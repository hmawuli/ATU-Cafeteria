import 'package:flutter/material.dart';
import '../../core/network/api_client.dart';

class CustomerDevicesScreen extends StatefulWidget {
  const CustomerDevicesScreen({super.key});
  @override
  State<CustomerDevicesScreen> createState() => _CustomerDevicesScreenState();
}

class _CustomerDevicesScreenState extends State<CustomerDevicesScreen> {
  final _api = ApiClient();
  bool _loading = true;
  List<Map<String, dynamic>> _items = [];

  @override
  void initState() { super.initState(); _load(); }

  Future<void> _load() async {
    try {
      final data = await _api.get('/customer/devices');
      final raw = data is Map ? data['devices'] : data;
      _items = raw is List ? raw.whereType<Map>().map(Map<String, dynamic>.from).toList() : [];
      if (mounted) setState(() {});
    } catch (e) {
      if (mounted) _message(e.toString());
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  void _message(String text) {
    ScaffoldMessenger.of(context)..hideCurrentSnackBar()..showSnackBar(SnackBar(content: Text(text)));
  }

  Future<void> _revoke(int id) async {
    try { await _api.delete('/customer/devices/$id'); await _load(); }
    catch (e) { if (mounted) _message(e.toString()); }
  }

  @override
  void dispose() { _api.close(); super.dispose(); }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Devices & sessions')),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : RefreshIndicator(
              onRefresh: _load,
              child: _items.isEmpty
                  ? ListView(children: const [
                      SizedBox(height: 160),
                      Icon(Icons.devices_outlined, size: 64),
                      SizedBox(height: 12),
                      Center(child: Text('No registered devices.')),
                    ])
                  : ListView.separated(
                      padding: const EdgeInsets.all(16),
                      itemCount: _items.length,
                      separatorBuilder: (_, __) => const SizedBox(height: 10),
                      itemBuilder: (_, i) {
                        final d = _items[i];
                        final id = int.tryParse((d['id'] ?? '').toString());
                        final platform = (d['platform'] ?? 'Device').toString();
                        final version = (d['app_version'] ?? 'Version not reported').toString();
                        final active = (d['last_seen_at'] ?? 'Not recorded').toString();
                        return Card(
                          child: ListTile(
                            leading: const CircleAvatar(child: Icon(Icons.phone_android_rounded)),
                            title: Text('$platform • $version', style: const TextStyle(fontWeight: FontWeight.w800)),
                            subtitle: Text('Last active: $active'),
                            trailing: id == null ? null : IconButton(
                              tooltip: 'Revoke',
                              onPressed: () => _revoke(id),
                              icon: const Icon(Icons.remove_circle_outline),
                            ),
                          ),
                        );
                      },
                    ),
            ),
    );
  }
}
