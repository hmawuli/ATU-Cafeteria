import 'package:flutter/material.dart';
import '../../core/network/api_client.dart';

class CustomerSupportScreen extends StatefulWidget {
  const CustomerSupportScreen({super.key});
  @override
  State<CustomerSupportScreen> createState() => _CustomerSupportScreenState();
}

class _CustomerSupportScreenState extends State<CustomerSupportScreen> {
  final _api = ApiClient();
  bool _loading = true;
  List<Map<String, dynamic>> _tickets = [];

  @override
  void initState() { super.initState(); _load(); }

  Future<void> _load() async {
    try {
      final data = await _api.get('/customer/support/tickets');
      final raw = data is Map ? data['tickets'] : data;
      final list = raw is Map && raw['data'] is List ? raw['data'] : raw;
      _tickets = list is List ? list.whereType<Map>().map(Map<String, dynamic>.from).toList() : [];
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

  Future<void> _newTicket() async {
    final subject = TextEditingController();
    final description = TextEditingController();
    var category = 'ORDER';
    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (_, setDialog) => AlertDialog(
          title: const Text('Contact support'),
          content: SingleChildScrollView(child: Column(children: [
            DropdownButtonFormField<String>(
              initialValue: category,
              items: const [
                DropdownMenuItem(value: 'ORDER', child: Text('Order')),
                DropdownMenuItem(value: 'PAYMENT', child: Text('Payment')),
                DropdownMenuItem(value: 'REFUND', child: Text('Refund')),
                DropdownMenuItem(value: 'ACCOUNT', child: Text('Account')),
                DropdownMenuItem(value: 'TECHNICAL', child: Text('Technical')),
                DropdownMenuItem(value: 'OTHER', child: Text('Other')),
              ],
              onChanged: (v) => setDialog(() => category = v ?? 'ORDER'),
              decoration: const InputDecoration(labelText: 'Category'),
            ),
            TextField(controller: subject, decoration: const InputDecoration(labelText: 'Subject')),
            TextField(controller: description, maxLines: 5, decoration: const InputDecoration(labelText: 'Describe the issue')),
          ])),
          actions: [
            TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Cancel')),
            FilledButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('Send')),
          ],
        ),
      ),
    );
    if (ok != true) { subject.dispose(); description.dispose(); return; }
    if (subject.text.trim().length < 3 || description.text.trim().length < 5) {
      _message('Please provide a subject and a clear description.');
      subject.dispose(); description.dispose(); return;
    }
    try {
      await _api.post('/customer/support/tickets',
        idempotencyKey: ApiClient.newIdempotencyKey(),
        body: {
          'category': category,
          'subject': subject.text.trim(),
          'description': description.text.trim(),
        });
      await _load();
    } catch (e) {
      if (mounted) _message(e.toString());
    } finally {
      subject.dispose(); description.dispose();
    }
  }

  @override
  void dispose() { _api.close(); super.dispose(); }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Customer support')),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: _newTicket,
        icon: const Icon(Icons.support_agent),
        label: const Text('New request'),
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : RefreshIndicator(
              onRefresh: _load,
              child: _tickets.isEmpty
                  ? ListView(children: const [
                      SizedBox(height: 160),
                      Icon(Icons.support_agent_outlined, size: 64),
                      SizedBox(height: 12),
                      Center(child: Text('No support requests yet.')),
                    ])
                  : ListView.separated(
                      padding: const EdgeInsets.fromLTRB(16, 16, 16, 96),
                      itemCount: _tickets.length,
                      separatorBuilder: (_, __) => const SizedBox(height: 10),
                      itemBuilder: (_, i) {
                        final t = _tickets[i];
                        final subject = (t['subject'] ?? 'Support request').toString();
                        final category = (t['category'] ?? 'OTHER').toString();
                        final priority = (t['priority'] ?? 'NORMAL').toString();
                        final description = (t['description'] ?? '').toString();
                        final status = (t['status'] ?? 'OPEN').toString();
                        return Card(
                          child: ListTile(
                            leading: const CircleAvatar(child: Icon(Icons.headset_mic_outlined)),
                            title: Text(subject, style: const TextStyle(fontWeight: FontWeight.w800)),
                            subtitle: Text('$category • $priority\n$description'),
                            isThreeLine: true,
                            trailing: Chip(label: Text(status)),
                          ),
                        );
                      },
                    ),
            ),
    );
  }
}
