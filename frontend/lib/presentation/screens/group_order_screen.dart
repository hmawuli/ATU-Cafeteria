import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:atu_cafeteria/core/network/api_client.dart';
import 'package:atu_cafeteria/presentation/providers/cafeteria_provider.dart';

class GroupOrderScreen extends StatefulWidget {
  const GroupOrderScreen({super.key});
  @override
  State<GroupOrderScreen> createState() => _GroupOrderScreenState();
}

class _GroupOrderScreenState extends State<GroupOrderScreen> {
  final ApiClient _api = ApiClient();
  final _codeController = TextEditingController();
  String? _code;
  Map<String, dynamic>? _session;
  List<dynamic> _items = [];
  List<dynamic> _contributors = [];
  List<dynamic> _menu = [];
  bool _busy = false;
  String _paymentMode = 'HOST_PAYS';

  @override
  void dispose() {
    _api.close();
    _codeController.dispose();
    super.dispose();
  }

  Future<void> _createSession() async {
    final cafe = context.read<CafeteriaProvider>();
    final vendorIds = cafe.allFoodItems.map((e) => e.vendorId).toSet().toList();
    if (vendorIds.isEmpty) {
      _message('No cafeteria vendors are currently available.');
      return;
    }
    final selected = await showDialog<int>(
      context: context,
      builder: (ctx) {
        int value = vendorIds.first;
        return StatefulBuilder(
          builder: (ctx, setDialog) => AlertDialog(
            title: const Text('Start a group order'),
            content: DropdownButtonFormField<int>(
              initialValue: value,
              decoration: const InputDecoration(
                labelText: 'Cafeteria vendor',
                prefixIcon: Icon(Icons.storefront_outlined),
              ),
              items: vendorIds.map((id) {
                final vendor = cafe.allVendors.where((v) => v.id == id);
                final name = vendor.isEmpty ? 'Vendor #$id' : vendor.first.fullName;
                return DropdownMenuItem(value: id, child: Text(name));
              }).toList(),
              onChanged: (v) => setDialog(() => value = v ?? value),
            ),
          ),
        );
      },
    );
    if (selected == null) return;

    await _run(() async {
      final data = await _api.post('/student/group-order', body: {
        'vendor_id': selected,
        'payment_mode': _paymentMode,
        'duration_minutes': 30,
      });
      await _openSession(data['session_code']?.toString() ?? '');
      if (mounted) _message('Group order created. Share code ${_code ?? ''} with your friends.');
    });
  }

  Future<void> _joinSession() async {
    final code = _codeController.text.trim().toUpperCase();
    if (code.isEmpty) {
      _message('Enter the group order code first.');
      return;
    }
    await _openSession(code);
  }

  Future<void> _openSession(String code) async {
    if (code.isEmpty) return;
    await _run(() async {
      final data = await _api.get('/student/group-order/$code');
      final session = Map<String, dynamic>.from(data['session'] as Map);
      setState(() {
        _code = code;
        _session = session;
        _items = data['items'] is List ? List<dynamic>.from(data['items']) : [];
        _contributors = data['contributors'] is List ? List<dynamic>.from(data['contributors']) : [];
        _paymentMode = session['payment_mode']?.toString() ?? 'HOST_PAYS';
      });
      await _loadMenu(int.tryParse(session['vendor_id']?.toString() ?? ''));
    });
  }

  Future<void> _loadMenu(int? vendorId) async {
    if (vendorId == null) return;
    try {
      final data = await _api.get('/menu-items/vendor/$vendorId');
      final raw = data is List ? data : (data is Map ? data['menu_items'] : null);
      if (raw is List && mounted) setState(() => _menu = List<dynamic>.from(raw));
    } catch (_) {}
  }

  Future<void> _addItem(Map<String, dynamic> item) async {
    final id = int.tryParse((item['id'] ?? '').toString());
    if (id == null || _code == null) return;
    int quantity = 1;
    final notes = TextEditingController();
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setDialog) => AlertDialog(
          title: Text(item['name']?.toString() ?? item['food_name']?.toString() ?? 'Menu item'),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text('GH₵ ${_price(item).toStringAsFixed(2)}'),
              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  IconButton(
                    onPressed: quantity > 1 ? () => setDialog(() => quantity--) : null,
                    icon: const Icon(Icons.remove_circle_outline),
                  ),
                  Text(quantity.toString(), style: const TextStyle(fontWeight: FontWeight.w900, fontSize: 18)),
                  IconButton(
                    onPressed: () => setDialog(() => quantity++),
                    icon: const Icon(Icons.add_circle_outline),
                  ),
                ],
              ),
              TextField(
                controller: notes,
                maxLength: 150,
                decoration: const InputDecoration(
                  labelText: 'Special note (optional)',
                  prefixIcon: Icon(Icons.notes_outlined),
                ),
              ),
            ],
          ),
          actions: [
            TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Cancel')),
            FilledButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('Add to group')),
          ],
        ),
      ),
    );
    if (confirmed != true) {
      notes.dispose();
      return;
    }
    final note = notes.text.trim();
    notes.dispose();

    await _run(() async {
      await _api.post('/student/group-order/${_code!}/contribute', body: {
        'menu_item_id': id,
        'quantity': quantity,
        'custom_notes': note.isEmpty ? null : note,
      });
      await _openSession(_code!);
    });
  }

  Future<void> _checkout() async {
    if (_code == null) return;
    final points = TextEditingController();
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Checkout group order'),
        content: TextField(
          controller: points,
          keyboardType: TextInputType.number,
          decoration: const InputDecoration(
            labelText: 'Loyalty points to redeem',
            prefixIcon: Icon(Icons.stars_outlined),
          ),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Cancel')),
          FilledButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('Place group order')),
        ],
      ),
    );
    if (confirmed != true) {
      points.dispose();
      return;
    }
    final value = int.tryParse(points.text.trim()) ?? 0;
    points.dispose();

    await _run(() async {
      await _api.post('/student/group-order/${_code!}/checkout', body: {'points_to_redeem': value});
      if (mounted) {
        _message('Group order submitted successfully.');
        Navigator.pop(context);
      }
    });
  }

  double _price(dynamic item) => double.tryParse((item['price'] ?? 0).toString()) ?? 0;

  Future<void> _run(Future<void> Function() action) async {
    if (_busy) return;
    setState(() => _busy = true);
    try {
      await action();
    } on ApiException catch (e) {
      _message(e.message);
    } catch (_) {
      _message('Could not complete the request. Please try again.');
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  void _message(String message) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(message)));
  }

  @override
  Widget build(BuildContext context) {
    final open = _session?['status']?.toString().toUpperCase() == 'OPEN';
    return Scaffold(
      appBar: AppBar(
        title: const Text('Group Order'),
        actions: [
          if (_code != null)
            IconButton(
              tooltip: 'Refresh',
              onPressed: _busy ? null : () => _openSession(_code!),
              icon: const Icon(Icons.refresh_rounded),
            ),
        ],
      ),
      body: _session == null ? _landing() : _sessionView(open),
    );
  }

  Widget _landing() => ListView(
        padding: const EdgeInsets.all(20),
        children: [
          Card(
            child: Padding(
              padding: const EdgeInsets.all(22),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Icon(Icons.groups_rounded, size: 52, color: Theme.of(context).colorScheme.primary),
                  const SizedBox(height: 14),
                  Text('Order together', style: Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w900)),
                  const SizedBox(height: 8),
                  const Text('Create one shared order, invite your friends, and send everything to the same cafeteria vendor.'),
                  const SizedBox(height: 18),
                  const Text('Payment mode', style: TextStyle(fontWeight: FontWeight.w800)),
                  RadioGroup<String>(
                    groupValue: _paymentMode,
                    onChanged: (v) => setState(() => _paymentMode = v!),
                    child: const Column(
                      children: [
                        RadioListTile<String>(value: 'HOST_PAYS', title: Text('Host pays'), subtitle: Text('The person who starts the group order pays the total.')),
                        RadioListTile<String>(value: 'INDIVIDUAL', title: Text('Everyone pays their share'), subtitle: Text('Each contributor pays their own items.')),
                      ],
                    ),
                  ),
                  SizedBox(
                    width: double.infinity,
                    height: 50,
                    child: FilledButton.icon(
                      onPressed: _busy ? null : _createSession,
                      icon: const Icon(Icons.add_link_rounded),
                      label: const Text('Create group order'),
                    ),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 18),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(22),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text('Have an invitation code?', style: TextStyle(fontWeight: FontWeight.w900)),
                  const SizedBox(height: 10),
                  TextField(
                    controller: _codeController,
                    textCapitalization: TextCapitalization.characters,
                    decoration: const InputDecoration(labelText: 'Group code', hintText: 'GP-ABC123', prefixIcon: Icon(Icons.confirmation_number_outlined)),
                  ),
                  const SizedBox(height: 12),
                  SizedBox(
                    width: double.infinity,
                    height: 48,
                    child: OutlinedButton.icon(
                      onPressed: _busy ? null : _joinSession,
                      icon: const Icon(Icons.login_rounded),
                      label: const Text('Join group order'),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      );

  Widget _sessionView(bool open) {
    final total = double.tryParse((_session?['total_cost'] ?? 0).toString()) ?? 0;
    return RefreshIndicator(
      onRefresh: () => _openSession(_code!),
      child: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Card(
            child: Padding(
              padding: const EdgeInsets.all(18),
              child: Column(
                children: [
                  const Text('SHARE THIS CODE', style: TextStyle(fontWeight: FontWeight.w800, letterSpacing: 1.1)),
                  const SizedBox(height: 5),
                  SelectableText(_code ?? '', style: TextStyle(fontSize: 30, fontWeight: FontWeight.w900, color: Theme.of(context).colorScheme.primary)),
                  const SizedBox(height: 8),
                  Text('${_session?['vendor_name'] ?? 'Cafeteria'} • ${_session?['payment_mode'] == 'INDIVIDUAL' ? 'Split payment' : 'Host pays'}'),
                ],
              ),
            ),
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              Expanded(child: _stat('Items', (_session?['total_items'] ?? 0).toString(), Icons.shopping_bag_outlined)),
              const SizedBox(width: 10),
              Expanded(child: _stat('People', _contributors.length.toString(), Icons.people_outline)),
              const SizedBox(width: 10),
              Expanded(child: _stat('Total', 'GH₵ ${total.toStringAsFixed(2)}', Icons.payments_outlined)),
            ],
          ),
          const SizedBox(height: 18),
          if (_menu.isNotEmpty && open) ...[
            Text('Add food', style: Theme.of(context).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w900)),
            const SizedBox(height: 8),
            ..._menu.map((item) {
              final map = Map<String, dynamic>.from(item as Map);
              final name = map['name']?.toString() ?? map['food_name']?.toString() ?? 'Menu item';
              return Card(
                child: ListTile(
                  leading: CircleAvatar(child: Text(name.substring(0, 1).toUpperCase())),
                  title: Text(name),
                  subtitle: Text('GH₵ ${_price(map).toStringAsFixed(2)}'),
                  trailing: IconButton(
                    onPressed: _busy ? null : () => _addItem(map),
                    icon: const Icon(Icons.add_circle_rounded),
                  ),
                ),
              );
            }),
            const SizedBox(height: 12),
          ],
          Text('Shared items', style: Theme.of(context).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w900)),
          const SizedBox(height: 8),
          if (_items.isEmpty)
            const Card(child: Padding(padding: EdgeInsets.all(18), child: Text('No items yet. Add the first meal above.')))
          else
            ..._items.map((item) => ListTile(
                  leading: const Icon(Icons.fastfood_outlined),
                  title: Text(item['food_name']?.toString() ?? 'Meal'),
                  subtitle: Text('Added by ${item['added_by']?['name'] ?? 'student'} • Qty ${item['quantity'] ?? 1}'),
                  trailing: Text('GH₵ ${(double.tryParse((item['subtotal'] ?? 0).toString()) ?? 0).toStringAsFixed(2)}'),
                )),
          const SizedBox(height: 18),
          Text('Contributors', style: Theme.of(context).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w900)),
          const SizedBox(height: 8),
          ..._contributors.map((person) => ListTile(
                leading: const CircleAvatar(child: Icon(Icons.person_outline)),
                title: Text(person['username']?.toString() ?? 'Student'),
                subtitle: Text('${person['total_items'] ?? 0} items'),
                trailing: Text('GH₵ ${(double.tryParse((person['total_cost'] ?? 0).toString()) ?? 0).toStringAsFixed(2)}'),
              )),
          const SizedBox(height: 18),
          if (open)
            FilledButton.icon(
              onPressed: _busy ? null : _checkout,
              icon: const Icon(Icons.lock_open_rounded),
              label: Text('Checkout group • GH₵ ${total.toStringAsFixed(2)}'),
            )
          else
            const Card(child: Padding(padding: EdgeInsets.all(16), child: Text('This group order is locked or expired.'))),
        ],
      ),
    );
  }

  Widget _stat(String title, String value, IconData icon) => Card(
        child: Padding(
          padding: const EdgeInsets.symmetric(vertical: 14, horizontal: 8),
          child: Column(
            children: [
              Icon(icon, size: 22, color: Theme.of(context).colorScheme.primary),
              const SizedBox(height: 5),
              Text(value, style: const TextStyle(fontWeight: FontWeight.w900)),
              Text(title, style: Theme.of(context).textTheme.bodySmall),
            ],
          ),
        ),
      );
}
