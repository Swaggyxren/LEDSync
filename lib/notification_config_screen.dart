import 'package:flutter/material.dart';
import 'package:installed_apps/installed_apps.dart'; 
import 'package:installed_apps/app_info.dart';       
import 'root_logic.dart';
import 'devices/device_config.dart';

class NotificationConfigScreen extends StatefulWidget {
  const NotificationConfigScreen({super.key});
  @override
  State<NotificationConfigScreen> createState() => _NotificationConfigScreenState();
}

class _NotificationConfigScreenState extends State<NotificationConfigScreen> {
  Map<String, String> appMappings = {};

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      appBar: AppBar(
        title: const Text("App LED Sync", style: TextStyle(fontWeight: FontWeight.bold)),
        backgroundColor: Colors.white,
        elevation: 0,
      ),
      // FIX: Changed <DeviceConfig> to <DeviceConfig?> to match RootLogic
      body: FutureBuilder<DeviceConfig?>(
        future: RootLogic.getConfig(), 
        builder: (context, configSnapshot) {
          if (configSnapshot.connectionState == ConnectionState.waiting) {
            return const Center(child: CircularProgressIndicator());
          }

          // Handle null config for unsupported devices
          final config = configSnapshot.data;
          if (config == null) {
            return const Center(child: Text("Device not supported for LED sync"));
          }

          final effects = config.ledEffects;

          return FutureBuilder<List<AppInfo>>(
            future: InstalledApps.getInstalledApps(true, true), 
            builder: (context, appSnapshot) {
              if (!appSnapshot.hasData) return const Center(child: CircularProgressIndicator());

              final apps = appSnapshot.data!;

              return ListView.builder(
                padding: const EdgeInsets.all(10),
                itemCount: apps.length,
                itemBuilder: (context, index) {
                  final app = apps[index];

                  return Card(
                    elevation: 0,
                    color: Colors.grey[50],
                    margin: const EdgeInsets.symmetric(vertical: 4),
                    child: ListTile(
                      leading: app.icon != null 
                          ? ClipRRect(
                              borderRadius: BorderRadius.circular(8),
                              child: Image.memory(app.icon!, width: 40),
                            )
                          : const Icon(Icons.android),
                      title: Text(app.name ?? "Unknown App", style: const TextStyle(fontWeight: FontWeight.w500)),
                      subtitle: Text(app.packageName, style: const TextStyle(fontSize: 10)),
                      trailing: DropdownButton<String>(
                        value: appMappings[app.packageName],
                        hint: const Text("None", style: TextStyle(fontSize: 12)),
                        underline: const SizedBox(), // Removes the messy default line
                        items: effects.keys.map((name) => DropdownMenuItem(
                          value: name, 
                          child: Text(name, style: const TextStyle(fontSize: 14))
                        )).toList(),
                        onChanged: (val) {
                          if (val != null) {
                            setState(() => appMappings[app.packageName] = val);
                          }
                        },
                      ),
                    ),
                  );
                },
              );
            },
          );
        },
      ),
    );
  }
}