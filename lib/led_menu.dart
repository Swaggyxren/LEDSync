import 'package:flutter/material.dart';
import 'root_logic.dart';
import 'devices/device_config.dart';

class LedMenu extends StatefulWidget {
  const LedMenu({super.key});
  @override
  State<LedMenu> createState() => _LedMenuState();
}

class _LedMenuState extends State<LedMenu> {
  final ScrollController _scrollController = ScrollController();
  List<String> logs = [];
  DeviceConfig? config;
  bool isReady = false;

  @override
  void initState() {
    super.initState();
    _initLab();
  }

  void _addLog(String msg) {
    if (!mounted) return;
    setState(() => logs.add("> $msg"));
    Future.delayed(const Duration(milliseconds: 50), () {
      if (_scrollController.hasClients) _scrollController.jumpTo(_scrollController.position.maxScrollExtent);
    });
  }

  Future<void> _initLab() async {
    config = await RootLogic.getConfig();
    if (await RootLogic.isRooted()) {
      await RootLogic.initializeHardware();
      _addLog("Hardware Nodes Initialized.");
      setState(() => isReady = true);
    } else {
      _addLog("Critical: No Root Access.");
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(20),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  IconButton(icon: const Icon(Icons.arrow_back_ios), onPressed: () => Navigator.pop(context)),
                  const Text("LED Hardware Lab", style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold)),
                ],
              ),
              const SizedBox(height: 15),
              // LOG BOX
              Container(
                height: 120, width: double.infinity,
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(color: Colors.black, borderRadius: BorderRadius.circular(12)),
                child: ListView.builder(
                  controller: _scrollController,
                  itemCount: logs.length,
                  itemBuilder: (context, i) => Text(logs[i], style: const TextStyle(color: Colors.greenAccent, fontSize: 11, fontFamily: 'monospace')),
                ),
              ),
              const SizedBox(height: 25),
              const Text("Injected Effects", style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
              const SizedBox(height: 15),
              if (isReady && config != null) 
                Expanded(
                  child: SingleChildScrollView(
                    child: Wrap(
                      spacing: 10, runSpacing: 10,
                      children: config!.ledEffects.entries.map((e) => ActionChip(
                        avatar: const Icon(Icons.lightbulb, size: 16),
                        label: Text(e.key),
                        onPressed: () {
                          RootLogic.sendRawHex(e.value);
                          _addLog("Injected: ${e.key}");
                        },
                      )).toList(),
                    ),
                  ),
                ),
              SizedBox(
                width: double.infinity, height: 55,
                child: ElevatedButton(
                  style: ElevatedButton.styleFrom(backgroundColor: Colors.red[700], foregroundColor: Colors.white, shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(15))),
                  onPressed: () {
                    RootLogic.turnOffAll();
                    _addLog("Emergency Stop Sent.");
                  },
                  child: const Text("EMERGENCY KILL", style: TextStyle(fontWeight: FontWeight.bold)),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}