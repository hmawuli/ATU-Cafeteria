import 'package:flutter/material.dart';
import '../../core/network/api_client.dart';

class CustomerAddressesScreen extends StatefulWidget {
  const CustomerAddressesScreen({super.key});
  @override
  State<CustomerAddressesScreen> createState() => _CustomerAddressesScreenState();
}

class _CustomerAddressesScreenState extends State<CustomerAddressesScreen> {
  final _api = ApiClient();
  bool _loading = true;
  List<Map<String, dynamic>> _items = [];

  @override
  void initState() { super.initState(); _load(); }

  Future<void> _load() async {
    try {
      final data = await _api.get('/customer/addresses');
      final raw = data is Map ? data['addresses'] : data;
      _items = raw is List ? raw.whereType<Map>().map((e) => Map<String, dynamic>.from(e)).toList() : [];
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

  Future<void> _add() async {
    final label = TextEditingController();
    final line1 = TextEditingController();
    final city = TextEditingController();
    final landmark = TextEditingController();
    var isDefault = _items.isEmpty;
    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (_, setDialog) => AlertDialog(
          title: const Text('Add saved address'),
          content: SingleChildScrollView(
            child: Column(children: [
              TextField(controller: label, decoration: const InputDecoration(labelText: 'Label')),
              TextField(controller: line1, decoration: const InputDecoration(labelText: 'Address line')),
              TextField(controller: city, decoration: const InputDecoration(labelText: 'City')),
              TextField(controller: landmark, decoration: const InputDecoration(labelText: 'Landmark')),
              SwitchListTile(
                value: isDefault,
                onChanged: (v) => setDialog(() => isDefault = v),
                title: const Text('Set as default'),
              ),
            ]),
          ),
          actions: [
            TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Cancel')),
            FilledButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('Save')),
          ],
        ),
      ),
    );
    if (ok != true) {
      label.dispose(); line1.dispose(); city.dispose(); landmark.dispose();
      return;
    }
    try {
      await _api.post('/customer/addresses',
        idempotencyKey: ApiClient.newIdempotencyKey(),
        body: {
          'label': label.text.trim(),
          'address_line1': line1.text.trim(),
          'city': city.text.trim().isEmpty ? null : city.text.trim(),
          'landmark': landmark.text.trim().isEmpty ? null : landmark.text.trim(),
          'is_default': isDefault,
        });
      await _load();
    } catch (e) {
      if (mounted) _message(e.toString());
    } finally {
      label.dispose(); line1.dispose(); city.dispose(); landmark.dispose();
    }
  }

  Future<void> _delete(int id) async {
    try { await _api.delete('/customer/addresses/' + id.toString()); await _load(); }
    catch (e) { if (mounted) _message(e.toString()); }
  }

  Future<void> _default(int id) async {
    try { await _api.post('/customer/addresses/' + id.toString() + '/default'); await _load(); }
    catch (e) { if (mounted) _message(e.toString()); }
  }

  @override
  void dispose() { _api.close(); super.dispose(); }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Saved addresses')),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: _add,
        icon: const Icon(Icons.add_location_alt_outlined),
        label: const Text('Add address'),
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : RefreshIndicator(
              onRefresh: _load,
              child: _items.isEmpty
                  ? ListView(children: const [
                      SizedBox(height: 160),
                      Icon(Icons.location_on_outlined, size: 64),
                      SizedBox(height: 12),
                      Center(child: Text('No saved addresses yet.')),
                    ])
                  : ListView.separated(
                      padding: const EdgeInsets.fromLTRB(16, 16, 16, 96),
                      itemCount: _items.length,
                      separatorBuilder: (_, __) => const SizedBox(height: 10),
                      itemBuilder: (_, i) {
                        final a = _items[i];
                        final id = int.tryParse((a['id'] ?? '').toString());
                        final isDefault = a['is_default'] == true;
                        final label = (a['label'] ?? 'Address').toString();
                        final title = isDefault ? label + ' • Default' : label;
                        final parts = [a['address_line1'], a['city'], a['landmark']]
                            .where((e) => e != null && e.toString().trim().isNotEmpty)
                            .map((e) => e.toString())
                            .join(', ');
                        return Card(
                          child: ListTile(
                            leading: CircleAvatar(child: Icon(isDefault ? Icons.home : Icons.location_on_outlined)),
                            title: Text(title, style: const TextStyle(fontWeight: FontWeight.w800)),
                            subtitle: Text(parts),
                            trailing: PopupMenuButton<String>(
                              onSelected: (v) {
                                if (id == null) return;
                                if (v == 'default') _default(id);
                                if (v == 'delete') _delete(id);
                              },
                              itemBuilder: (_) => [
                                if (!isDefault) const PopupMenuItem(value: 'default', child: Text('Make default')),
                                const PopupMenuItem(value: 'delete', child: Text('Remove')),
                              ],
                            ),
                          ),
                        );
                      },
                    ),
            ),
    );
  }
}
