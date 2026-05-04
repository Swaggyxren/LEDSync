import 'dart:io';
import 'package:device_info_plus/device_info_plus.dart';
import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:ledsync/models/devices/device_config.dart';
import 'package:ledsync/models/devices/lh8n_config.dart';

class RootLogic {
  /// In-memory mirror of the persisted master toggle. Defaults to `true`
  /// for fresh installs; replaced by [loadMasterEnabled] on app start so
  /// callers reading this field synchronously after that point get the
  /// real value. The Kotlin notification service reads the same key
  /// (`flutter.master_enabled`) directly from SharedPreferences so the
  /// toggle covers both code paths.
  static const _kMasterEnabledKey = 'master_enabled';
  static bool masterEnabled = true;
  static bool? _cachedRooted;

  /// Hydrate [masterEnabled] from persisted prefs. Safe to call repeatedly;
  /// no-op once the value has been loaded.
  static Future<void> loadMasterEnabled() async {
    final prefs = await SharedPreferences.getInstance();
    masterEnabled = prefs.getBool(_kMasterEnabledKey) ?? true;
  }

  /// Set + persist the master toggle. When disabled, [sendRawHex] short-
  /// circuits and the Kotlin service skips notifications too. We also
  /// soft-stop the LED on disable so a currently-running pattern doesn't
  /// keep breathing forever.
  static Future<void> setMasterEnabled(bool value) async {
    masterEnabled = value;
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool(_kMasterEnabledKey, value);
    if (!value) {
      await turnOffAll();
    }
  }

  // ── Active config — user-chosen, persisted ─────────────────────────────
  static DeviceConfig? _currentConfig;

  static final List<DeviceConfig> allConfigs = [
    LH8nConfig(),
    // Add future device configs here
  ];

  static const _kConfigPrefKey = 'selected_device_config';

  /// Returns the active config. Loads from prefs on first call.
  /// Defaults to LH8nConfig if nothing is saved yet — never returns null.
  static Future<DeviceConfig> getConfig() async {
    if (_currentConfig != null) return _currentConfig!;

    final prefs = await SharedPreferences.getInstance();
    final saved = prefs.getString(_kConfigPrefKey);

    _currentConfig = allConfigs.firstWhere(
      (c) => c.deviceName == saved,
      orElse: () => LH8nConfig(),
    );
    return _currentConfig!;
  }

  /// Sync getter for use after getConfig() has been awaited at least once.
  static DeviceConfig? get activeConfig => _currentConfig;

  /// Set and persist the user's chosen config.
  static Future<void> setConfig(DeviceConfig cfg) async {
    _currentConfig = cfg;
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_kConfigPrefKey, cfg.deviceName);
  }

  static Future<bool> isRooted() async {
    if (_cachedRooted != null) return _cachedRooted!;
    try {
      final r = await Process.run('su', ['-v']);
      _cachedRooted = r.exitCode == 0;
    } catch (_) {
      _cachedRooted = false;
    }
    return _cachedRooted!;
  }

  static Future<Map<String, dynamic>> getPhoneInfo() async {
    final results = await Future.wait([
      DeviceInfoPlugin().androidInfo,
      Process.run('su', ['-c', 'uname -r']),
    ]);

    final androidInfo = results[0] as AndroidDeviceInfo;
    final kernelResult = results[1] as ProcessResult;
    final cfg = await getConfig();

    return {
      'model': cfg.deviceName,
      'version': 'Android ${androidInfo.version.release}',
      'kernel': kernelResult.stdout.toString().trim(),
    };
  }

  /// Mirrors Transsion `TranLightsServiceExtImpl.isLightActive`: the aw22xxx
  /// controller only accepts `tran_led_cmd` effect writes after a one-time
  /// prime (`hwen=1` + `brightness=255` + `00 00 00 00 00 00`). Re-priming on
  /// every notification is expensive (~5 `su` execs) and slows LED reaction
  /// by a couple hundred ms on LH8n, so we cache the primed state and only
  /// re-run the sequence when explicitly invalidated (soft turn-off,
  /// emergency revive, or `force: true`).
  static bool _isLightActive = false;

  /// Force the next [ensureLedEnabled] call to re-prime the controller.
  /// Exposed for callers that know the hardware was reset out-of-band.
  static void markLedInactive() => _isLightActive = false;

  static Future<void> initializeHardware() => ensureLedEnabled(force: true);

  static Future<void> ensureLedEnabled({bool force = false}) async {
    if (_isLightActive && !force) return;
    final cfg = await getConfig();
    await _runSu(
      'echo 1 > ${cfg.awPath}/hwen; '
      'echo c > ${cfg.awPath}/imax 2>/dev/null || true; '
      'echo 255 > ${cfg.awPath}/brightness; '
      'echo none > ${cfg.awPath}/trigger 2>/dev/null || true; '
      "echo -n '00 00 00 00 00 00' > ${cfg.lbCmd}",
    );
    _isLightActive = true;
  }

  static Future<void> sendRawHex(String hex) async {
    if (!masterEnabled) return;
    final cfg = await getConfig();
    // Prime the controller on the first write (or after a kill). Subsequent
    // writes are a single `su` exec thanks to the cached `_isLightActive`
    // flag — matching the stock Transsion service's own fast-path.
    await ensureLedEnabled();
    await _runSu("echo -n '$hex' > ${cfg.lbCmd}");
  }

  /// Soft turn-off: sends only the OEM's `turnOffHex` to `tran_led_cmd`.
  /// `hwen` and `brightness` stay at their primed values so the next
  /// [sendRawHex] doesn't have to power-cycle the aw22xxx controller
  /// (a power-cycle costs ~200–400 ms on LH8n). Use
  /// [emergencyKillAndRevive] if you need a full hardware reset.
  static Future<void> turnOffAll() async {
    final cfg = await getConfig();
    await _runSu("echo -n '${cfg.turnOffHex}' > ${cfg.lbCmd}");
    _isLightActive = false;
  }

  /// Hard reset: drop `hwen` + `brightness` to force the aw22xxx controller
  /// to fully power-cycle, wait [offTime], then re-prime. Use this when the
  /// LED is stuck in a bad state and a soft turn-off isn't enough.
  static Future<void> emergencyKillAndRevive({
    Duration offTime = const Duration(milliseconds: 250),
  }) async {
    final cfg = await getConfig();
    await _runSu(
      "echo -n '${cfg.turnOffHex}' > ${cfg.lbCmd}; "
      'echo 0 > ${cfg.awPath}/brightness; '
      'echo 0 > ${cfg.awPath}/hwen',
    );
    _isLightActive = false;
    await Future.delayed(offTime);
    await ensureLedEnabled(force: true);
  }

  static Future<ProcessResult> _runSu(String cmd) async {
    final result = await Process.run('su', ['-c', cmd]);
    if (result.exitCode != 0) {
      debugPrint('[RootLogic] su failed (exit ${result.exitCode}): $cmd');
    }
    return result;
  }
}
