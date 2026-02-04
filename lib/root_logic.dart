import 'dart:io';
import 'package:device_info_plus/device_info_plus.dart';
import 'devices/device_config.dart';
import 'devices/lh8n_config.dart';

class RootManagerInfo {
  final String name;
  final String iconPath;
  RootManagerInfo(this.name, this.iconPath);
}

class RootLogic {
  static DeviceConfig? _currentConfig;

  static Future<DeviceConfig?> getConfig() async {
    if (_currentConfig != null) return _currentConfig;
    final deviceInfo = DeviceInfoPlugin();
    final androidInfo = await deviceInfo.androidInfo;
    
    // Specifically targeting the TECNO LH8n hardware
    if (androidInfo.model.contains("LH8n")) {
      _currentConfig = LH8nConfig();
      return _currentConfig;
    }
    return null; 
  }

  static Future<bool> isRooted() async {
    try {
      var result = await Process.run('su', ['-v']);
      return result.exitCode == 0;
    } catch (_) { return false; }
  }

  static Future<Map<String, dynamic>> getPhoneInfo() async {
    final deviceInfo = DeviceInfoPlugin();
    final androidInfo = await deviceInfo.androidInfo;
    var kernel = await _runSu("uname -r");
    
    String version = androidInfo.version.release;
    return {
      'model': "TECNO ${androidInfo.model}",
      'version': "Android $version",
      'kernel': kernel.stdout.toString().trim(),
    };
  }

  static Future<RootManagerInfo> detectManager() async {
    if ((await _runSu("ls /data/adb/ksu")).exitCode == 0) {
      return RootManagerInfo("KernelSU Next", "assets/kernelsu_next.png");
    }
    if ((await _runSu("ls /data/adb/apatch")).exitCode == 0) {
      return RootManagerInfo("APatch", "assets/apatch_icon.png");
    }
    var magiskCheck = await _runSu("magisk -v");
    if (magiskCheck.exitCode == 0) {
      return RootManagerInfo("Magisk", "assets/magisk.png");
    }
    return RootManagerInfo("Unknown", "");
  }

  static Future<void> initializeHardware() async {
    final cfg = await getConfig();
    if (cfg == null) return;
    await _runSu("echo 1 > ${cfg.awPath}/hwen");
    await _runSu("echo 255 > ${cfg.awPath}/brightness");
  }

  static Future<void> sendRawHex(String hex) async {
    final cfg = await getConfig();
    if (cfg == null) return;
    await _runSu("echo -n '$hex' > ${cfg.lbCmd}");
  }

  static Future<void> turnOffAll() async {
    final cfg = await getConfig();
    if (cfg == null) return;
    await sendRawHex("00 01 00 00 00 00"); 
    await _runSu("echo 0 > ${cfg.awPath}/hwen");
  }

  static Future<ProcessResult> _runSu(String cmd) async => 
      await Process.run('su', ['-c', cmd]);
}