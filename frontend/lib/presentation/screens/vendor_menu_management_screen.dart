import 'package:flutter/material.dart';
import 'package:atu_cafeteria/core/network/api_client.dart';

/// Production menu-management workspace backed directly by the vendor CRUD API.
class VendorMenuManagementScreen extends StatefulWidget {
  const VendorMenuManagementScreen({super.key});

  @override
  State<VendorMenuManagementScreen> createState() => _VendorMenuManagementScreenState();
}

class _VendorMenuManagementScreenState extends State<VendorMenuManagementScreen> {
  final _api = ApiClient();
  final _search = TextEditingController();
  List<Map<String, dynamic>> _items = [];
  bool _loading = true;
  String? _error;

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
      final data = await _api.get('/vendor/menu-items');
      final raw = data is Map ? data['menu_items'] : null;
      final list = raw is List ? raw : const [];
      if (!mounted) return;
      setState(() {
        _items = list.whereType<Map>().map(Map<String, dynamic>.from).toList();
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
        _error = 'Unable to load the restaurant menu. Check your connection and try again.';
        _loading = false;
      });
    }
  }

  List<Map<String, dynamic>> get _filtered {
    final query = _search.text.trim().toLowerCase();
    if (query.isEmpty) return _items;
    return _items.where((item) {
      final name = '${item['name'] ?? item['food_name'] ?? ''}'.toLowerCase();
      final category = '${item['category'] ?? ''}'.toLowerCase();
      return name.contains(query) || category.contains(query);
    }).toList();
  }

  Future<void> _save({Map<String, dynamic>? item}) async {
    final name = TextEditingController(text: '${item?['name'] ?? item?['food_name'] ?? ''}');
    final price = TextEditingController(text: item == null ? '' : '${item['price'] ?? ''}');
    final description = TextEditingController(text: '${item?['description'] ?? ''}');
    final category = TextEditingController(text: '${item?['category'] ?? 'General'}');
    bool available = item == null ? true : item['is_available'] != false && item['is_available'] != 0;
    String? error;

    try {
      final result = await showDialog<bool>(
        context: context,
        builder: (context) => StatefulBuilder(
          builder: (context, setDialogState) => AlertDialog(
            title: Text(item == null ? 'Add Menu Item' : 'Edit Menu Item'),
            content: SingleChildScrollView(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  TextField(controller: name, decoration: const InputDecoration(labelText: 'Menu item name')),
                  const SizedBox(height: 10),
                  TextField(controller: price, keyboardType: const TextInputType.numberWithOptions(decimal: true), decoration: const InputDecoration(labelText: 'Price (GH₵)')),
                  const SizedBox(height: 10),
                  TextField(controller: category, decoration: const InputDecoration(labelText: 'Category')),
                  const SizedBox(height: 10),
                  TextField(controller: description, maxLines: 3, decoration: const InputDecoration(labelText: 'Description')),
                  const SizedBox(height: 6),
                  SwitchListTile.adaptive(
                    contentPadding: EdgeInsets.zero,
                    title: const Text('Available for ordering'),
                    value: available,
                    onChanged: (value) => setDialogState(() => available = value),
                  ),
                  if (error != null)
                    Align(
                      alignment: Alignment.centerLeft,
                      child: Text(error!, style: TextStyle(color: Theme.of(context).colorScheme.error)),
                    ),
                ],
              ),
            ),
            actions: [
              TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Cancel')),
              FilledButton(
                onPressed: () async {
                  final parsed = double.tryParse(price.text.trim());
                  if (name.text.trim().isEmpty || parsed == null || parsed < 0) {
                    setDialogState(() => error = 'Enter a valid name and price.');
                    return;
                  }
                  try {
                    final body = {
                      'food_name': name.text.trim(),
                      'price': parsed,
                      'description': description.text.trim(),
                      'category': category.text.trim().isEmpty ? 'General' : category.text.trim(),
                      'is_available': available,
                    };
                    if (item == null) {
                      await _api.post('/vendor/menu-items', body: body, idempotencyKey: ApiClient.newIdempotencyKey());
                    } else {
                      await _api.put('/vendor/menu-items/${item['id']}', body: body);
                    }
                    if (context.mounted) Navigator.pop(context, true);
                  } on ApiException catch (e) {
                    setDialogState(() => error = e.message);
                  } catch (_) {
                    setDialogState(() => error = 'The menu item could not be saved.');
                  }
                },
                child: const Text('Save'),
              ),
            ],
          ),
        ),
      );
      if (result == true) await _load();
    } finally {
      name.dispose();
      price.dispose();
      description.dispose();
      category.dispose();
    }
  }

  Future<void> _toggle(Map<String, dynamic> item, bool value) async {
    final id = item['id'];
    if (id == null) return;
    try {
      await _api.put('/vendor/menu-items/$id', body: {'is_available': value});
      await _load();
    } on ApiException catch (e) {
      _show(e.message);
    } catch (_) {
      _show('Availability could not be updated.');
    }
  }

  Future<void> _delete(Map<String, dynamic> item) async {
    final id = item['id'];
    if (id == null) return;
    final name = '${item['name'] ?? item['food_name'] ?? 'this menu item'}';
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Remove menu item?'),
        content: Text('Remove "$name" from your restaurant menu?'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Cancel')),
          FilledButton(onPressed: () => Navigator.pop(context, true), child: const Text('Remove')),
        ],
      ),
    );
    if (confirmed != true) return;
    try {
      await _api.delete('/vendor/menu-items/$id');
      await _load();
    } on ApiException catch (e) {
      _show(e.message);
    } catch (_) {
      _show('Menu item could not be removed.');
    }
  }

  void _show(String message) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(message)));
  }

  @override
  Widget build(BuildContext context) {
    final filtered = _filtered;
    final available = _items.where((e) => e['is_available'] == true || e['is_available'] == 1).length;
    return Scaffold(
      appBar: AppBar(
        title: const Text('Menu Management'),
        actions: [
          IconButton(onPressed: _load, tooltip: 'Refresh menu', icon: const Icon(Icons.refresh)),
        ],
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: _save,
        icon: const Icon(Icons.add),
        label: const Text('Add Item'),
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
              ? _ErrorState(message: _error!, onRetry: _load)
              : RefreshIndicator(
                  onRefresh: _load,
                  child: ListView(
                    padding: const EdgeInsets.fromLTRB(16, 16, 16, 96),
                    children: [
                      Card(
                        child: Padding(
                          padding: const EdgeInsets.all(16),
                          child: Row(
                            children: [
                              Expanded(child: _Metric(label: 'Menu Items', value: '${_items.length}')),
                              Expanded(child: _Metric(label: 'Available', value: '$available')),
                              Expanded(child: _Metric(label: 'Unavailable', value: '${_items.length - available}')),
                            ],
                          ),
                        ),
                      ),
                      const SizedBox(height: 14),
                      TextField(
                        controller: _search,
                        onChanged: (_) => setState(() {}),
                        decoration: InputDecoration(
                          hintText: 'Search menu items or categories',
                          prefixIcon: const Icon(Icons.search),
                          suffixIcon: _search.text.isEmpty ? null : IconButton(onPressed: () { _search.clear(); setState(() {}); }, icon: const Icon(Icons.clear)),
                          border: const OutlineInputBorder(),
                        ),
                      ),
                      const SizedBox(height: 14),
                      if (filtered.isEmpty)
                        const Padding(
                          padding: EdgeInsets.symmetric(vertical: 60),
                          child: Center(child: Text('No menu items match your search.')),
                        )
                      else
                        ...filtered.map((item) {
                          final isAvailable = item['is_available'] == true || item['is_available'] == 1;
                          final itemName = '${item['name'] ?? item['food_name'] ?? 'Menu item'}';
                          final category = '${item['category'] ?? 'General'}';
                          final price = double.tryParse('${item['price'] ?? 0}') ?? 0;
                          return Card(
                            margin: const EdgeInsets.only(bottom: 10),
                            child: ListTile(
                              contentPadding: const EdgeInsets.fromLTRB(16, 8, 8, 8),
                              leading: CircleAvatar(child: Icon(isAvailable ? Icons.restaurant : Icons.visibility_off_outlined)),
                              title: Text(itemName, style: const TextStyle(fontWeight: FontWeight.w800)),
                              subtitle: Text('$category  •  GH₵ ${price.toStringAsFixed(2)}\n${item['description'] ?? ''}'),
                              isThreeLine: true,
                              trailing: Wrap(
                                spacing: 2,
                                crossAxisAlignment: WrapCrossAlignment.center,
                                children: [
                                  Switch(value: isAvailable, onChanged: (value) => _toggle(item, value)),
                                  IconButton(tooltip: 'Edit', onPressed: () => _save(item: item), icon: const Icon(Icons.edit_outlined)),
                                  IconButton(tooltip: 'Remove', onPressed: () => _delete(item), icon: const Icon(Icons.delete_outline)),
                                ],
                              ),
                            ),
                          );
                        }),
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
          Text(value, style: const TextStyle(fontSize: 22, fontWeight: FontWeight.w900)),
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
