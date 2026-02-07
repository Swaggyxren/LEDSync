import 'dart:convert';
import 'dart:typed_data';
import 'dart:ui';

import 'package:flutter/material.dart';
import 'package:installed_apps/installed_apps.dart';
import 'package:installed_apps/app_info.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'root_logic.dart';
import 'devices/device_config.dart';

// Cached app info model
class CachedAppInfo {
  final String name;
  final String packageName;
  final String? iconBase64;

  CachedAppInfo({
    required this.name,
    required this.packageName,
    this.iconBase64,
  });

  Map<String, dynamic> toJson() => {
        'name': name,
        'packageName': packageName,
        'iconBase64': iconBase64,
      };

  factory CachedAppInfo.fromJson(Map<String, dynamic> json) => CachedAppInfo(
        name: json['name'] as String,
        packageName: json['packageName'] as String,
        iconBase64: json['iconBase64'] as String?,
      );

  factory CachedAppInfo.fromAppInfo(AppInfo app) => CachedAppInfo(
        name: app.name,
        packageName: app.packageName,
        iconBase64: app.icon != null && app.icon!.isNotEmpty
            ? base64Encode(app.icon!)
            : null,
      );

  Uint8List? get iconBytes =>
      iconBase64 != null ? base64Decode(iconBase64!) : null;
}

class NotificationConfigScreen extends StatefulWidget {
  const NotificationConfigScreen({super.key});
  @override
  State<NotificationConfigScreen> createState() =>
      _NotificationConfigScreenState();
}

class _NotificationConfigScreenState extends State<NotificationConfigScreen> {
  static const _kNameMapKey = "notif_name_map";
  static const _kHexMapKey = "notif_hex_map";
  static const _kShowSystemKey = "notif_show_system_apps";
  
  // Cache keys
  static const _kCachedAppsKey = "cached_apps_list";
  static const _kCachedAppsSystemKey = "cached_apps_list_system";
  static const _kCacheTimestampKey = "cached_apps_timestamp";
  static const _kCacheTimestampSystemKey = "cached_apps_timestamp_system";

  late final Future<DeviceConfig?> _configFuture;
  
  bool _showSystemApps = false;
  bool _isReloading = false;

  List<CachedAppInfo> _cachedApps = [];
  Map<String, String> _savedMappings = {};
  Map<String, String> _workingMappings = {};
  bool _dirty = false;

  final TextEditingController _searchCtrl = TextEditingController();
  String _searchQuery = "";

  @override
  void initState() {
    super.initState();
    _configFuture = RootLogic.getConfig();
    _initPrefsAndLoad();
  }

  @override
  void dispose() {
    _searchCtrl.dispose();
    super.dispose();
  }

  Future<void> _initPrefsAndLoad() async {
    final sp = await SharedPreferences.getInstance();

    _showSystemApps = sp.getBool(_kShowSystemKey) ?? false;

    // Load saved mappings
    final raw = sp.getString(_kNameMapKey);
    Map<String, String> parsed = {};
    if (raw != null && raw.trim().isNotEmpty) {
      try {
        final decoded = (jsonDecode(raw) as Map).cast<String, dynamic>();
        parsed = decoded.map((k, v) => MapEntry(k.toString(), v.toString()));
      } catch (_) {
        parsed = {};
      }
    }

    // Load cached apps
    final cachedApps = await _loadCachedApps(_showSystemApps);
    
    if (!mounted) return;
    setState(() {
      _savedMappings = parsed;
      _workingMappings = Map.of(parsed);
      _cachedApps = cachedApps;
      _dirty = false;
    });

    // If cache is empty or old (>7 days), reload in background
    if (_cachedApps.isEmpty || await _isCacheStale(_showSystemApps)) {
      _reloadAppList(silent: true);
    }
  }

  Future<List<CachedAppInfo>> _loadCachedApps(bool includeSystem) async {
    final sp = await SharedPreferences.getInstance();
    final cacheKey = includeSystem ? _kCachedAppsSystemKey : _kCachedAppsKey;
    final cachedJson = sp.getString(cacheKey);
    
    if (cachedJson == null || cachedJson.trim().isEmpty) {
      return [];
    }

    try {
      final List<dynamic> decoded = jsonDecode(cachedJson);
      return decoded
          .map((e) => CachedAppInfo.fromJson(e as Map<String, dynamic>))
          .toList();
    } catch (_) {
      return [];
    }
  }

  Future<void> _saveCachedApps(List<CachedAppInfo> apps, bool includeSystem) async {
    final sp = await SharedPreferences.getInstance();
    final cacheKey = includeSystem ? _kCachedAppsSystemKey : _kCachedAppsKey;
    final timestampKey = includeSystem ? _kCacheTimestampSystemKey : _kCacheTimestampKey;
    
    final jsonData = jsonEncode(apps.map((e) => e.toJson()).toList());
    await sp.setString(cacheKey, jsonData);
    await sp.setInt(timestampKey, DateTime.now().millisecondsSinceEpoch);
  }

  Future<bool> _isCacheStale(bool includeSystem) async {
    final sp = await SharedPreferences.getInstance();
    final timestampKey = includeSystem ? _kCacheTimestampSystemKey : _kCacheTimestampKey;
    final timestamp = sp.getInt(timestampKey);
    
    if (timestamp == null) return true;
    
    final cacheDate = DateTime.fromMillisecondsSinceEpoch(timestamp);
    final now = DateTime.now();
    final difference = now.difference(cacheDate);
    
    // Cache is stale if older than 7 days
    return difference.inDays > 7;
  }

  Future<void> _reloadAppList({bool silent = false}) async {
    if (_isReloading) return;

    if (!silent) {
      setState(() => _isReloading = true);
    }

    try {
      final apps = await InstalledApps.getInstalledApps(
        excludeSystemApps: !_showSystemApps,
        excludeNonLaunchableApps: false,
        withIcon: true,
      );

      final cachedApps = apps.map((e) => CachedAppInfo.fromAppInfo(e)).toList();
      
      // Save to cache
      await _saveCachedApps(cachedApps, _showSystemApps);

      if (!mounted) return;
      setState(() {
        _cachedApps = cachedApps;
        _isReloading = false;
      });

      if (!silent) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text("Loaded ${cachedApps.length} apps"),
            backgroundColor: const Color(0xFF1A2942),
            duration: const Duration(seconds: 2),
          ),
        );
      }
    } catch (e) {
      if (!mounted) return;
      setState(() => _isReloading = false);
      
      if (!silent) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text("Failed to load apps: $e"),
            backgroundColor: Colors.red[700],
          ),
        );
      }
    }
  }

  bool _mapsEqual(Map<String, String> a, Map<String, String> b) {
    if (a.length != b.length) return false;
    for (final e in a.entries) {
      if (b[e.key] != e.value) return false;
    }
    return true;
  }

  Future<void> _toggleSystemApps(bool value) async {
    final sp = await SharedPreferences.getInstance();
    await sp.setBool(_kShowSystemKey, value);

    setState(() => _showSystemApps = value);
    
    // Load cached apps for the new setting
    final cachedApps = await _loadCachedApps(_showSystemApps);
    
    if (!mounted) return;
    setState(() => _cachedApps = cachedApps);

    // If cache is empty or stale, reload
    if (_cachedApps.isEmpty || await _isCacheStale(_showSystemApps)) {
      _reloadAppList(silent: true);
    }
  }

  Future<void> _openSearchDialog() async {
    final result = await showDialog<String>(
      context: context,
      builder: (ctx) {
        _searchCtrl.text = _searchQuery;
        return AlertDialog(
          backgroundColor: const Color(0xFF1A2942),
          title: const Text(
            "Search apps",
            style: TextStyle(color: Color(0xFFD4DCE6)),
          ),
          content: TextField(
            controller: _searchCtrl,
            autofocus: true,
            style: const TextStyle(color: Color(0xFFD4DCE6)),
            decoration: const InputDecoration(
              hintText: "Name or package…",
              hintStyle: TextStyle(color: Color(0xFF8899AA)),
              prefixIcon: Icon(Icons.search, color: Color(0xFF8899AA)),
              enabledBorder: UnderlineInputBorder(
                borderSide: BorderSide(color: Color(0xFF8899AA)),
              ),
              focusedBorder: UnderlineInputBorder(
                borderSide: BorderSide(color: Color(0xFF5B9FED)),
              ),
            ),
            onSubmitted: (v) => Navigator.pop(ctx, v.trim()),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(ctx, null),
              child: const Text(
                "Cancel",
                style: TextStyle(color: Color(0xFF8899AA)),
              ),
            ),
            TextButton(
              onPressed: () => Navigator.pop(ctx, ""),
              child: const Text(
                "Clear",
                style: TextStyle(color: Color(0xFF8899AA)),
              ),
            ),
            TextButton(
              onPressed: () => Navigator.pop(ctx, _searchCtrl.text.trim()),
              child: const Text(
                "Apply",
                style: TextStyle(color: Color(0xFF5B9FED)),
              ),
            ),
          ],
        );
      },
    );

    if (!mounted || result == null) return;
    setState(() => _searchQuery = result);
  }

  Future<void> _save(DeviceConfig config) async {
    final sp = await SharedPreferences.getInstance();

    await sp.setString(_kNameMapKey, jsonEncode(_workingMappings));

    final effects = config.ledEffects;
    final hexMap = <String, String>{};
    for (final e in _workingMappings.entries) {
      final hex = effects[e.value];
      if (hex != null && hex.trim().isNotEmpty) {
        hexMap[e.key] = hex;
      }
    }
    await sp.setString(_kHexMapKey, jsonEncode(hexMap));

    if (!mounted) return;
    setState(() {
      _savedMappings = Map.of(_workingMappings);
      _dirty = false;
    });

    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(
        content: Text("Saved app LED patterns"),
        backgroundColor: Color(0xFF1A2942),
      ),
    );
  }

  Widget _buildLeadingIcon(Uint8List? iconBytes) {
    if (iconBytes == null || iconBytes.isEmpty) {
      return const Icon(Icons.android, color: Color(0xFF8899AA));
    }
    return ClipRRect(
      borderRadius: BorderRadius.circular(8),
      child: Image.memory(
        iconBytes,
        width: 40,
        height: 40,
        errorBuilder: (_, __, ___) => const Icon(
          Icons.android,
          color: Color(0xFF8899AA),
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Container(
        decoration: const BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: [
              Color(0xFF0A1628),
              Color(0xFF1A2942),
              Color(0xFF0F1D2F),
              Color(0xFF0A1628),
            ],
            stops: [0.0, 0.3, 0.7, 1.0],
          ),
        ),
        child: Column(
          children: [
            AppBar(
              centerTitle: false,
              titleSpacing: 0,
              title: const Text(
                "App LED Sync",
                style: TextStyle(
                  fontWeight: FontWeight.bold,
                  fontSize: 16,
                  color: Color(0xFFD4DCE6),
                ),
              ),
              backgroundColor: Colors.transparent,
              elevation: 0,
              iconTheme: const IconThemeData(color: Color(0xFFB8C5D6)),
              actions: [
                Row(
                  children: [
                    const Text(
                      "System",
                      style: TextStyle(
                        fontSize: 13,
                        color: Color(0xFF8899AA),
                      ),
                    ),
                    Switch(
                      value: _showSystemApps,
                      onChanged: _toggleSystemApps,
                      activeThumbColor: const Color(0xFF5B9FED),
                      activeTrackColor:
                          const Color(0xFF5B9FED).withValues(alpha: 0.3),
                    ),
                    IconButton(
                      tooltip: "Reload app list",
                      icon: _isReloading
                          ? const SizedBox(
                              width: 20,
                              height: 20,
                              child: CircularProgressIndicator(
                                strokeWidth: 2,
                                color: Color(0xFF5B9FED),
                              ),
                            )
                          : const Icon(Icons.refresh, color: Color(0xFFB8C5D6)),
                      onPressed: _isReloading ? null : () => _reloadAppList(),
                    ),
                    IconButton(
                      tooltip: "Search apps",
                      icon: const Icon(Icons.search, color: Color(0xFFB8C5D6)),
                      onPressed: _openSearchDialog,
                    ),
                    const SizedBox(width: 6),
                  ],
                ),
              ],
            ),
            Expanded(
              child: FutureBuilder<DeviceConfig?>(
                future: _configFuture,
                builder: (context, configSnapshot) {
                  if (configSnapshot.connectionState == ConnectionState.waiting) {
                    return const Center(
                      child: CircularProgressIndicator(color: Color(0xFF5B9FED)),
                    );
                  }
                  if (configSnapshot.hasError) {
                    return Center(
                      child: Text(
                        "Config error: ${configSnapshot.error}",
                        style: const TextStyle(color: Color(0xFF8899AA)),
                      ),
                    );
                  }

                  final config = configSnapshot.data;
                  if (config == null) {
                    return const Center(
                      child: Text(
                        "Device not supported for LED sync",
                        style: TextStyle(color: Color(0xFF8899AA)),
                      ),
                    );
                  }

                  final effects = config.ledEffects;

                  // If still loading initial apps, show loading
                  if (_cachedApps.isEmpty && !_isReloading) {
                    return const Center(
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          CircularProgressIndicator(color: Color(0xFF5B9FED)),
                          SizedBox(height: 16),
                          Text(
                            "Loading apps...",
                            style: TextStyle(color: Color(0xFF8899AA)),
                          ),
                        ],
                      ),
                    );
                  }

                  final apps = List<CachedAppInfo>.from(_cachedApps);

                  final q = _searchQuery.trim().toLowerCase();
                  if (q.isNotEmpty) {
                    apps.removeWhere((a) {
                      final name = a.name.toLowerCase();
                      final pkg = a.packageName.toLowerCase();
                      return !(name.contains(q) || pkg.contains(q));
                    });
                  }

                  apps.sort((a, b) {
                    final aHas = _workingMappings.containsKey(a.packageName);
                    final bHas = _workingMappings.containsKey(b.packageName);
                    if (aHas != bHas) return aHas ? -1 : 1;
                    return a.name.toLowerCase().compareTo(b.name.toLowerCase());
                  });

                  return ListView.builder(
                    padding: const EdgeInsets.all(10),
                    itemCount: apps.length,
                    itemBuilder: (context, index) {
                      final app = apps[index];
                      final selected = _workingMappings[app.packageName];
                      final hasPattern =
                          selected != null && selected.trim().isNotEmpty;

                      return ClipRRect(
                        borderRadius: BorderRadius.circular(12),
                        child: BackdropFilter(
                          filter: ImageFilter.blur(sigmaX: 10, sigmaY: 10),
                          child: Card(
                            elevation: 0,
                            color: hasPattern
                                ? const Color(0xFF5B9FED).withValues(alpha: 0.2)
                                : const Color(0xFF1A2942).withValues(alpha: 0.6),
                            margin: const EdgeInsets.symmetric(vertical: 4),
                            shape: RoundedRectangleBorder(
                              borderRadius: BorderRadius.circular(12),
                              side: BorderSide(
                                color: hasPattern
                                    ? const Color(0xFF5B9FED)
                                        .withValues(alpha: 0.3)
                                    : Colors.white.withValues(alpha: 0.1),
                                width: 1,
                              ),
                            ),
                            child: ListTile(
                              leading: _buildLeadingIcon(app.iconBytes),
                              title: Text(
                                app.name,
                                style: TextStyle(
                                  fontWeight: FontWeight.w600,
                                  color: hasPattern
                                      ? const Color(0xFF5B9FED)
                                      : const Color(0xFFD4DCE6),
                                ),
                              ),
                              subtitle: Text(
                                app.packageName,
                                style: const TextStyle(
                                  fontSize: 10,
                                  color: Color(0xFF8899AA),
                                ),
                              ),
                              trailing: DropdownButton<String>(
                                value: selected,
                                hint: const Text(
                                  "None",
                                  style: TextStyle(
                                    fontSize: 12,
                                    color: Color(0xFF8899AA),
                                  ),
                                ),
                                underline: const SizedBox(),
                                dropdownColor: const Color(0xFF1A2942),
                                style: const TextStyle(
                                  color: Color(0xFFD4DCE6),
                                ),
                                items: effects.keys
                                    .map((name) => DropdownMenuItem(
                                          value: name,
                                          child: Text(
                                            name,
                                            style: const TextStyle(fontSize: 14),
                                          ),
                                        ))
                                    .toList(),
                                onChanged: (val) {
                                  setState(() {
                                    if (val == null) {
                                      _workingMappings.remove(app.packageName);
                                    } else {
                                      _workingMappings[app.packageName] = val;
                                    }
                                    _dirty = !_mapsEqual(
                                        _workingMappings, _savedMappings);
                                  });
                                },
                              ),
                              onLongPress: () {
                                setState(() {
                                  _workingMappings.remove(app.packageName);
                                  _dirty = !_mapsEqual(
                                      _workingMappings, _savedMappings);
                                });
                              },
                            ),
                          ),
                        ),
                      );
                    },
                  );
                },
              ),
            ),
          ],
        ),
      ),
      floatingActionButtonLocation: FloatingActionButtonLocation.centerFloat,
      floatingActionButton: FutureBuilder<DeviceConfig?>(
        future: _configFuture,
        builder: (context, snap) {
          final cfg = snap.data;
          if (!_dirty || cfg == null) return const SizedBox.shrink();

          return FloatingActionButton.extended(
            onPressed: () => _save(cfg),
            backgroundColor: const Color(0xFF5B9FED),
            icon: const Icon(Icons.save),
            label: const Text("Save"),
          );
        },
      ),
    );
  }
}