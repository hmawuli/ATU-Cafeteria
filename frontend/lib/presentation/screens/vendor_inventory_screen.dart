import 'package:flutter/material.dart';
import 'package:atu_cafeteria/core/network/api_client.dart';

/// Lightweight inventory visibility screen backed by the vendor inventory API.
class VendorInventoryScreen extends StatefulWidget {
  const VendorInventoryScreen({super.key});

  @override
  State<VendorInventoryScreen> createState() => _VendorInventoryScreenState();
}

class _VendorInventoryScreenState extends State<VendorInventoryScreen> {
  final _api = ApiClient();
  bool _loading = true;
  String? _error;
  Map<String, dynamic> _summary = {};
  List<Map<String, dynamic>> _items = [];

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _api.close();
    super.dispose();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final data = await _api.get('/vendor/inventory/summary');
      final summary = data is Map && data['summary'] is Map ? Map<String, dynamic>.from(data['summary']) : <String, dynamic>{};
      final rawItems = data is Map && data['items'] is List ? data['items'] as List : const [];
      if (!mounted) return;
      setState(() {
        _summary = summary;
        _items = rawItems.whereType<Map>().map(Map<String, dynamic>.from).toList();
        _loading = false;
      });
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() { _error = e.message; _loading = false; });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _error = 'Unable to load inventory information. Check your connection and try again.';
        _loading = false;
      });
    }
  }

  int _number(String key) => (_summary[key] as num?)?.toInt() ?? 0;

  Color _statusColor(BuildContext context, String status) {
    switch (status.toUpperCase()) {
      case 'OUT_OF_STOCK':
        return Theme.of(context).colorScheme.error;
      case 'LOW_STOCK':
        return Colors.orange.shade800;
      case 'HEALTHY':
      case 'AVAILABLE':
        return Colors.green.shade700;
      default:
        return Theme.of(context).colorScheme.outline;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Inventory'),
        actions: [IconButton(onPressed: _load, tooltip: 'Refresh inventory', icon: const Icon(Icons.refresh))],
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
              ? Center(
                  child: Padding(
                    padding: const EdgeInsets.all(24),
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        const Icon(Icons.inventory_2_outlined, size: 44),
                        const SizedBox(height: 12),
                        Text(_error!, textAlign: TextAlign.center),
                        const SizedBox(height: 16),
                        FilledButton.icon(onPressed: _load, icon: const Icon(Icons.refresh), label: const Text('Retry')),
                      ],
                    ),
                  ),
                )
              : RefreshIndicator(
                  onRefresh: _load,
                  child: ListView(
                    padding: const EdgeInsets.fromLTRB(16, 16, 16, 28),
                    children: [
                      Card(
                        child: Padding(
                          padding: const EdgeInsets.all(16),
                          child: Row(
                            children: [
                              Expanded(child: _Metric(label: 'Total', value: '${_number('total_items')}')),
                              Expanded(child: _Metric(label: 'Tracked', value: '${_number('tracked_items')}')),
                              Expanded(child: _Metric(label: 'Low Stock', value: '${_number('low_stock')}')),
                              Expanded(child: _Metric(label: 'Out', value: '${_number('out_of_stock')}')),
                            ],
                          ),
                        ),
                      ),
                      const SizedBox(height: 18),
                      const Text('Stock & Availability', style: TextStyle(fontSize: 18, fontWeight: FontWeight.w900)),
                      const SizedBox(height: 10),
                      if (_items.isEmpty)
                        const Card(child: Padding(padding: EdgeInsets.all(24), child: Center(child: Text('No inventory records are available yet.'))))
                      else
                        ..._items.map((item) {
                          final status = '${item['status'] ?? 'UNTRACKED'}';
                          final stock = item['stock'];
                          final threshold = item['threshold'];
                          final available = item['available'] == true || item['available'] == 1;
                          return Card(
                            margin: const EdgeInsets.only(bottom: 10),
                            child: ListTile(
                              leading: CircleAvatar(child: Icon(status == 'OUT_OF_STOCK' ? Icons.remove_shopping_cart_outlined : Icons.inventory_2_outlined)),
                              title: Text('${item['name'] ?? 'Item'}', style: const TextStyle(fontWeight: FontWeight.w800)),
                              subtitle: Text(stock == null ? 'Stock tracking not enabled' : 'Stock: $stock  •  Low-stock threshold: $threshold'),
                              trailing: Column(
                                mainAxisAlignment: MainAxisAlignment.center,
                                crossAxisAlignment: CrossAxisAlignment.end,
                                children: [
                                  Text(status.replaceAll('_', ' '), style: TextStyle(fontSize: 10, fontWeight: FontWeight.w900, color: _statusColor(context, status))),
                                  const SizedBox(height: 4),
                                  Text(available ? 'Available' : 'Unavailable', style: Theme.of(context).textTheme.bodySmall),
                                ],
                              ),
                            ),
                          );
                        }),
                      const SizedBox(height: 12),
                      Text('Inventory data is supplied by the restaurant inventory service. Items without stock tracking are shown as untracked rather than assigned an artificial quantity.', style: Theme.of(context).textTheme.bodySmall),
                    ],
                  ),
                ),
    );
  }
}

class _Metric extends StatelessWidget {
  final String label;
  final String value;
  const _Metric({required this.label, required this.value});

  @override
  Widget build(BuildContext context) => Column(
        children: [
          Text(value, style: const TextStyle(fontSize: 21, fontWeight: FontWeight.w900)),
          const SizedBox(height: 3),
          Text(label, textAlign: TextAlign.center, style: Theme.of(context).textTheme.bodySmall),
        ],
      );
}
