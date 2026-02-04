import 'package:battery_plus/battery_plus.dart';
import 'led_automator.dart';

class BatteryListener {
  static final Battery _battery = Battery();

  static void listen() {
    _battery.onBatteryStateChanged.listen((state) async {
      int level = await _battery.batteryLevel;
      
      // Example Logic: Low Battery (Priority 2)
      if (level <= 20) {
        // We fetch this hex based on the device later
        LedAutomator.playEffect(
          hex: "00 02 00 00 FF 00", // Placeholder hex
          priority: LedPriority.battery,
        );
      }
    });
  }
}