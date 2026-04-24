import 'package:ledsync/models/devices/device_config.dart';

/// LH8n (TECNO POVA 5 PRO 5G) LED effect presets.
///
/// Command format is 6 ASCII bytes written to [lbCmd] as
/// `TYPE MODE R1 R2 R3 R4`. The mapping mirrors the stock Transsion
/// `TranLightsServiceExtImpl` service (see `/sys/led/led/tran_led_cmd`):
///
///   MODE 02 → battery/charge breath  (R1 = 00 low, 01 mid, 02 full)
///   MODE 03 → startup breath         (R1 = sub-effect 0..4)
///   MODE 04 → phone-call breath      (R1 = sub-effect 0..4)
///   MODE 05 01 → notification breath (R2 = sub-effect 0..4)
///   MODE 20 → music breath           (R1 = sub-effect 0..4)
///   MODE 30 → music / "gameCenter" breath variant
///
/// All of the above are `breath_flash` patterns in the OEM service — they
/// loop on the hardware until overwritten or [turnOffHex] is sent, so every
/// one of them belongs in [loopingPatterns].
class LH8nConfig implements DeviceConfig {
  @override
  String get deviceName => 'TECNO POVA 5 PRO 5G (LH8n)';
  @override
  String get awPath => '/sys/class/leds/aw22xxx_led';
  @override
  String get lbCmd => '/sys/led/led/tran_led_cmd';

  @override
  Map<String, String> get ledEffects => {
    // Phone-call breath (MODE 04, R1 = sub-effect)
    'Soft': '00 04 00 00 00 00',

    // Music "speed" / gameCenter breath variant (MODE 30)
    'Speed': '00 30 01 00 00 00',

    // Startup breath (MODE 03, R1 = sub-effect)
    'Illusion': '00 03 01 00 00 00',

    // Notification breath (MODE 05 01, R2 = sub-effect 0..4)
    'Pureness': '00 05 01 00 00 00',
    'StarRiver': '00 05 01 01 00 00',
    'Halo': '00 05 01 02 00 00',
    'Lightning': '00 05 01 03 00 00',
    'Rise': '00 05 01 04 00 00',

    // Music breath (MODE 20, R1 = sub-effect 0..4)
    'Mellow': '00 20 00 00 00 00',
    'Groove': '00 20 01 00 00 00',
    'Breathe': '00 20 02 00 00 00',
    'Party': '00 20 03 00 00 00',
    'Electric': '00 20 04 00 00 00',

    // Battery / charge breath (MODE 02, R1 = charge bucket)
    // These are the OEM's built-in battery presets (red/mid/full).
    'Low Battery': '00 02 00 00 00 00',
    'Charging': '00 02 01 00 00 00',
    'Charged': '00 02 02 00 00 00',
  };

  /// All MODE 02 / 03 / 04 / 05 / 20 / 30 presets on LH8n are breath
  /// patterns that loop indefinitely after a single hardware write. A
  /// per-app rule using any of these must stop the LED when the triggering
  /// notification is dismissed.
  @override
  Set<String> get loopingPatterns => const {
    // Phone-call breath
    'Soft',
    // Music "speed" / gameCenter
    'Speed',
    // Startup breath
    'Illusion',
    // Notification breath (all 5 sub-effects)
    'Pureness',
    'StarRiver',
    'Halo',
    'Lightning',
    'Rise',
    // Music breath (all 5 sub-effects)
    'Mellow',
    'Groove',
    'Breathe',
    'Party',
    'Electric',
    // Charge breath
    'Low Battery',
    'Charging',
    'Charged',
  };

  // Battery-threshold defaults now use the OEM's dedicated charge breath
  // patterns instead of reusing notification presets.
  @override
  String get defaultLowEffect => 'Low Battery';
  @override
  String get defaultCriticalEffect => 'Low Battery';
  @override
  String get defaultFullEffect => 'Charged';

  @override
  String get turnOffHex => '00 01 00 00 00 00';
}
