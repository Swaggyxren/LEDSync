import 'root_logic.dart';
import 'devices/device_config.dart';
import 'led_automator.dart';

class NotificationListener {
  static Map<String, String> savedMappings = {}; 

  static void onPackageReceived(String packageName) async {
    String? patternName = savedMappings[packageName];
    
    if (patternName != null) {
      // FIX: Changed to DeviceConfig? and added 'await' 
      // This matches the type returned by RootLogic.getConfig()
      final DeviceConfig? config = await RootLogic.getConfig();

      // FIX: Check if config is not null before accessing ledEffects
      if (config != null) {
        String? hex = config.ledEffects[patternName];

        if (hex != null) {
          LedAutomator.playEffect(
            hex: hex,
            priority: LedPriority.notification,
            cooldown: const Duration(seconds: 8),
          );
        }
      }
    }
  }
}