import 'package:flutter/material.dart';
import 'dart:ui';
import 'battery_config_screen.dart';
import 'notification_config_screen.dart';

class AppDrawerPopup {
  static void show(BuildContext context) {
    showGeneralDialog(
      context: context,
      barrierDismissible: true,
      barrierLabel: "Close",
      barrierColor: Colors.black.withAlpha(128),
      transitionDuration: const Duration(milliseconds: 300),
      pageBuilder: (context, anim1, anim2) {
        return BackdropFilter(
          filter: ImageFilter.blur(sigmaX: 10, sigmaY: 10),
          child: Center(
            child: Material(
              color: Colors.transparent,
              child: Container(
                // Compact height for just two main options
                height: MediaQuery.of(context).size.height * 0.35, 
                width: MediaQuery.of(context).size.width * 0.75,
                decoration: BoxDecoration(
                  color: Colors.white.withAlpha(240),
                  borderRadius: BorderRadius.circular(30),
                ),
                child: Column(
                  children: [
                    const SizedBox(height: 25),
                    const Text("LED Configuration", 
                      style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: Colors.black)),
                    const SizedBox(height: 20),
                    _buildTile(context, "App Alerts", Icons.notifications, const NotificationConfigScreen()),
                    _buildTile(context, "Battery Config", Icons.battery_charging_full, const BatteryConfigScreen()),
                    const Spacer(),
                    TextButton(onPressed: () => Navigator.pop(context), child: const Text("Close")),
                    const SizedBox(height: 10),
                  ],
                ),
              ),
            ),
          ),
        );
      },
    );
  }

  static Widget _buildTile(BuildContext context, String title, IconData icon, Widget screen) {
    return ListTile(
      leading: Icon(icon, color: Colors.blueAccent),
      title: Text(title, style: const TextStyle(color: Colors.black, fontWeight: FontWeight.w500)),
      onTap: () {
        Navigator.pop(context);
        Navigator.push(context, MaterialPageRoute(builder: (context) => screen));
      },
    );
  }
}