import 'dart:async';
import 'root_logic.dart';

enum LedPriority { notification, battery, idle }

class LedAutomator {
  static LedPriority _currentPriority = LedPriority.idle;
  static Timer? _cooldownTimer;

  static Future<void> playEffect({
    required String hex, 
    required LedPriority priority,
    Duration cooldown = const Duration(seconds: 5),
  }) async {
    // PRIORITY LOGIC:
    // Notifications (Priority 1) can override Battery (Priority 2).
    // Battery CANNOT override an active Notification cooldown.
    
    if (_currentPriority == LedPriority.notification && priority == LedPriority.battery) {
      // Ignore battery update if a notification just happened
      return;
    }

    // Set the new priority state
    _currentPriority = priority;

    // Send the command to the hardware
    await RootLogic.sendRawHex(hex);

    // Handle Cooldown
    _cooldownTimer?.cancel();
    _cooldownTimer = Timer(cooldown, () {
      _currentPriority = LedPriority.idle;
      // You can trigger a 'refresh' here to go back to battery state if needed
    });
  }
}