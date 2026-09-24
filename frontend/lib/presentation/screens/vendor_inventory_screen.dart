import 'package:flutter/material.dart';
import 'package:atu_cafeteria/core/network/api_client.dart';

/// Production inventory workspace backed by the vendor inventory APIs.
class VendorInventoryScreen extends StatefulWidget {
  const VendorInventoryScreen({super.key});

  @override
  State<VendorInventoryScreen> createState() => _VendorInventoryScreenState();
}

class _VendorInventoryScreenState extends State<VendorInventoryScreen> {
  final _api = ApiClient();
  final _search = TextEditingController();
  bool _loading = true;
  bool _adjusting = false;
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
    _search.dispose();
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
      final summary = data is Map && data['summary'] is Map
          ? Map<String, dynamic>.from(data['summary'])
          : <String, dynamic>{};
      final rawItems = data is Map && data['items'] is List
          ? data['items'] as List
          : const [];
      if (!mounted) return;
      setState(() {
        _summary = summary;
        _items = rawItems.whereType<Map>().map(Map<String, dynamic>.from).toList();
        _loading = false;
      });
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() {
        _error = e.message;
        _loading = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _error = 'Unable to load inventory information. Check your connection and try again.';
        _loading = false;
      });
    }
  }

  int _number(String key) => (_summary[key] as num?)?.toInt() ?? 0;

  List<Map<String, dynamic>> get _filteredItems {
    final query = _search.text.trim().toLowerCase();
    if (query.isEmpty) return _items;
    return _items.where((item) {
      final name = '${item['name'] ?? ''}'.toLowerCase();
      final status = '${item['status'] ?? ''}'.toLowerCase();
      return name.contains(query) || status.contains(query);
    }).toList();
  }

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

  Future<void> _adjustStock(Map<String, dynamic> item) async {
    final id = item['menu_item_id'] ?? item['id'];
    if (id == null) return;

    final quantity = TextEditingController();
    final reason = TextEditingController();
    String? error;

    try {
      final confirmed = await showDialog<bool>(
        context: context,
        builder: (dialogContext) => StatefulBuilder(
          builder: (dialogContext, setDialogState) => AlertDialog(
            title: const Text('Adjust Stock'),
            content: SingleChildScrollView(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(
                    '${item['name'] ?? 'Menu item'}  •  Current stock: ${item['stock'] ?? 'Untracked'}',
                    style: const TextStyle(fontWeight: FontWeight.w700),
                  ),
                  const SizedBox(height: 14),
                  TextField(
                    controller: quantity,
                    keyboardType: const TextInputType.numberWithOptions(signed: true),
                    decoration: const InputDecoration(
                      labelText: 'Quantity adjustment',
                      hintText: 'e.g. 10 or -2',
                      helperText: 'Positive adds stock; negative removes stock.',
                    ),
                  ),
                  const SizedBox(height: 10),
                  TextField(
                    controller: reason,
                    maxLength: 255,
                    decoration: const InputDecoration(labelText: 'Reason (optional)'),
                  ),
                  if (error != null) ...[
                    const SizedBox(height: 6),
                    Align(
                      alignment: Alignment.centerLeft,
                      child: Text(
                        error!,
                        style: TextStyle(color: Theme.of(dialogContext).colorScheme.error),
                      ),
                    ),
                  ],
                ],
              ),
            ),
            actions: [
              TextButton(
                onPressed: () => Navigator.pop(dialogContext, false),
                child: const Text('Cancel'),
              ),
              FilledButton(
                onPressed: () {
                  final value = int.tryParse(quantity.text.trim());
                  if (value == null || value == 0) {
                    setDialogState(() => error = 'Enter a non-zero whole number.');
                    return;
                  }
                  Navigator.pop(dialogContext, true);
                },
                child: const Text('Continue'),
              ),
            ],
          ),
        ),
      );

      if (confirmed != true || !mounted) return;
      final value = int.tryParse(quantity.text.trim());
      if (value == null || value == 0) return;

      setState(() => _adjusting = true);
      await _api.post(
        '/vendor/inventory/adjust',
        idempotencyKey: ApiClient.newIdempotencyKey(),
        body: {
          'menu_item_id': id,
          'quantity': value,
          if (reason.text.trim().isNotEmpty) 'reason': reason.text.trim(),
        },
      );
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Inventory updated successfully.')),
      );
      await _load();
    } on ApiException catch (e) {
      if (mounted) _show(e.message);
    } catch (_) {
      if (mounted) _show('Inventory could not be updated.');
    } finally {
      quantity.dispose();
      reason.dispose();
      if (mounted) setState(() => _adjusting = false);
    }
  }

  void _show(String message) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(message)));
  }

  @override
  Widget build(BuildContext context) {
    final items = _filteredItems;
    return Scaffold(
      appBar: AppBar(
        title: const Text('Inventory'),
        actions: [
          IconButton(
            onPressed: _loading ? null : _load,
            tooltip: 'Refresh inventory',
            icon: const Icon(Icons.refresh),
          ),
        ],
      ),
      body: _loading && _items.isEmpty
          ? const Center(child: CircularProgressIndicator())
          : _error != null && _items.isEmpty
              ? _ErrorState(message: _error!, onRetry: _load)
              : RefreshIndicator(
                  onRefresh: _load,
                  child: ListView(
                    physics: const AlwaysScrollableScrollPhysics(),
                    padding: const EdgeInsets.fromLTRB(16, 16, 16, 28),
                    children: [
                      if (_error != null) ...[
                        _MessageCard(message: _error!, onRetry: _load),
                        const SizedBox(height: 12),
                      ],
                      if (_adjusting) const LinearProgressIndicator(),
                      if (_adjusting) const SizedBox(height: 12),
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
                      TextField(
                        controller: _search,
                        onChanged: (_) => setState(() {}),
                        decoration: InputDecoration(
                          hintText: 'Search inventory',
                          prefixIcon: const Icon(Icons.search),
                          suffixIcon: _search.text.isEmpty
                              ? null
                              : IconButton(
                                  onPressed: () {
                                    _search.clear();
                                    setState(() {});
                                  },
                                  icon: const Icon(Icons.clear),
                                ),
                        ),
                      ),
                      const SizedBox(height: 12),
                      if (items.isEmpty)
                        const Card(
                          child: Padding(
                            padding: EdgeInsets.all(24),
                            child: Center(child: Text('No inventory records match your search.')),
                          ),
                        )
                      else
                        ...items.map((item) {
                          final status = '${item['status'] ?? 'UNTRACKED'}';
                          final stock = item['stock'];
                          final threshold = item['threshold'];
                          final available = item['available'] == true || item['available'] == 1;
                          final tracked = stock != null;
                          return Card(
                            margin: const EdgeInsets.only(bottom: 10),
                            child: ListTile(
                              contentPadding: const EdgeInsets.fromLTRB(14, 8, 8, 8),
                              leading: CircleAvatar(
                                child: Icon(
                                  status == 'OUT_OF_STOCK'
                                      ? Icons.remove_shopping_cart_outlined
                                      : Icons.inventory_2_outlined,
                                ),
                              ),
                              title: Text(
                                '${item['name'] ?? 'Item'}',
                                style: const TextStyle(fontWeight: FontWeight.w800),
                              ),
                              subtitle: Text(
                                tracked
                                    ? 'Stock: $stock  •  Low-stock threshold: $threshold'
                                    : 'Stock tracking not enabled',
                              ),
                              trailing: tracked
                                  ? IconButton(
                                      tooltip: 'Adjust stock',
                                      onPressed: _adjusting ? null : () => _adjustStock(item),
                                      icon: const Icon(Icons.tune_outlined),
                                    )
                                  : Text(
                                      available ? 'Available' : 'Unavailable',
                                      style: Theme.of(context).textTheme.bodySmall,
                                    ),
                              onTap: tracked ? () => _adjustStock(item) : null,
                            ),
                          );
                        }),
                      const SizedBox(height: 12),
                      Text(
                        'Inventory changes are server-authorized, idempotent, and recorded as inventory movements. Items without stock tracking remain untracked rather than being assigned an artificial quantity.',
                        style: Theme.of(context).textTheme.bodySmall,
                      ),
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

class _ErrorState extends StatelessWidget {
  final String message;
  final VoidCallback onRetry;
  const _ErrorState({required this.message, required this.onRetry});

  @override
  Widget build(BuildContext context) => Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Icon(Icons.cloud_off_outlined, size: 44),
              const SizedBox(height: 12),
              Text(message, textAlign: TextAlign.center),
              const SizedBox(height: 16),
              FilledButton.icon(onPressed: onRetry, icon: const Icon(Icons.refresh), label: const Text('Retry')),
            ],
          ),
        ),
      );
}

class _MessageCard extends StatelessWidget {
  final String message;
  final VoidCallback onRetry;
  const _MessageCard({required this.message, required this.onRetry});

  @override
  Widget build(BuildContext context) => Card(
        child: ListTile(
          leading: const Icon(Icons.warning_amber_rounded),
          title: Text(message),
          trailing: IconButton(onPressed: onRetry, icon: const Icon(Icons.refresh)),
        ),
      );
}
