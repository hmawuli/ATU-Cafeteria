import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:atu_cafeteria/core/network/api_client.dart';
import 'package:atu_cafeteria/presentation/providers/cafeteria_provider.dart';

/// Primary production vendor command centre.
///
/// This screen intentionally composes the existing operational modules instead
/// of duplicating their business logic. Live overview figures come from the
/// vendor APIs, while Orders/Menu/Inventory/Finance/Promotions/Payouts remain
/// dedicated workflows.
class VendorOperationsDashboardScreen extends StatefulWidget {
  const VendorOperationsDashboardScreen({super.key});

  @override
  State<VendorOperationsDashboardScreen> createState() => _VendorOperationsDashboardScreenState();
}

class _VendorOperationsDashboardScreenState extends State<VendorOperationsDashboardScreen> {
  final _api = ApiClient();
  bool _loading = true;
  bool _storeOpen = true;
  String? _error;
  List<Map<String, dynamic>> _orders = [];
  Map<String, dynamic> _inventory = {};
  Map<String, dynamic> _finance = {};

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
      final results = await Future.wait<dynamic>([
        _api.get('/vendor/orders'),
        _api.get('/vendor/inventory/summary'),
        _api.get('/vendor/finance?days=30'),
        _api.get('/me'),
      ]);

      final rawOrders = results[0] is List ? results[0] as List : const [];
      final inventory = results[1] is Map && results[1]['summary'] is Map
          ? Map<String, dynamic>.from(results[1]['summary'])
          : <String, dynamic>{};
      final finance = results[2] is Map && results[2]['summary'] is Map
          ? Map<String, dynamic>.from(results[2]['summary'])
          : <String, dynamic>{};
      final me = results[3] is Map && results[3]['user'] is Map
          ? Map<String, dynamic>.from(results[3]['user'])
          : <String, dynamic>{};

      if (!mounted) return;
      setState(() {
        _orders = rawOrders.whereType<Map>().map(Map<String, dynamic>.from).toList();
        _inventory = inventory;
        _finance = finance;
        _storeOpen = me['is_open'] != false && me['is_open'] != 0;
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
        _error = 'Some restaurant data could not be loaded. Please refresh and try again.';
        _loading = false;
      });
    }
  }

  String _status(Map<String, dynamic> order) =>
      '${order['status'] ?? order['order_status'] ?? 'PENDING'}'.toUpperCase();

  int _count(Iterable<Map<String, dynamic>> source, Set<String> statuses) =>
      source.where((o) => statuses.contains(_status(o))).length;

  double _money(dynamic value) => value is num ? value.toDouble() : double.tryParse('$value') ?? 0;

  String _ghs(dynamic value) => 'GH₵ ${_money(value).toStringAsFixed(2)}';

  Future<void> _toggleStore(bool open) async {
    final previous = _storeOpen;
    setState(() => _storeOpen = open);
    try {
      final response = await _api.patch('/vendor/status', body: {'is_open': open}, idempotencyKey: ApiClient.newIdempotencyKey());
      final serverValue = response is Map ? response['is_open'] : null;
      if (mounted && serverValue is bool) setState(() => _storeOpen = serverValue);
      if (mounted) {
        context.read<CafeteriaProvider>().setStoreClosedState(!open);
      }
    } catch (e) {
      if (!mounted) return;
      setState(() => _storeOpen = previous);
      final message = e is ApiException ? e.message : 'Restaurant status could not be updated.';
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(message)));
    }
  }

  void _go(String route) => Navigator.pushNamed(context, route).then((_) => _load());

  @override
  Widget build(BuildContext context) {
    final provider = context.watch<CafeteriaProvider>();
    final user = provider.currentUser;
    if (user == null) {
      return const Scaffold(body: Center(child: Text('Access session expired. Please sign in again.')));
    }

    final newOrders = _count(_orders, {'PENDING', 'ORDER_PLACED', 'RECEIVED'});
    final preparing = _count(_orders, {'PREPARING'});
    final ready = _count(_orders, {'READY', 'READY_FOR_PICKUP'});
    final completed = _count(_orders, {'COMPLETED', 'DELIVERED'});

    return Scaffold(
      appBar: AppBar(
        title: const Text('Restaurant Operations'),
        actions: [
          IconButton(onPressed: _load, tooltip: 'Refresh', icon: const Icon(Icons.refresh)),
          IconButton(
            onPressed: () {
              provider.logOut();
              Navigator.pushReplacementNamed(context, '/login');
            },
            tooltip: 'Sign out',
            icon: const Icon(Icons.logout),
          ),
        ],
      ),
      body: _loading && _orders.isEmpty
          ? const Center(child: CircularProgressIndicator())
          : RefreshIndicator(
              onRefresh: _load,
              child: ListView(
                physics: const AlwaysScrollableScrollPhysics(),
                padding: const EdgeInsets.fromLTRB(16, 18, 16, 30),
                children: [
                  if (_error != null) ...[
                    _messageCard(_error!, Icons.warning_amber_rounded),
                    const SizedBox(height: 12),
                  ],
                  _restaurantHeader(user.fullName),
                  const SizedBox(height: 16),
                  _sectionTitle('Today at a glance', 'Live operational indicators'),
                  const SizedBox(height: 10),
                  GridView.count(
                    crossAxisCount: 2,
                    shrinkWrap: true,
                    physics: const NeverScrollableScrollPhysics(),
                    mainAxisSpacing: 10,
                    crossAxisSpacing: 10,
                    childAspectRatio: 1.55,
                    children: [
                      _metric('New Orders', '$newOrders', Icons.notifications_active_outlined, () => _go('/vendor-orders')),
                      _metric('Preparing', '$preparing', Icons.soup_kitchen_outlined, () => _go('/vendor-orders')),
                      _metric('Ready', '$ready', Icons.shopping_bag_outlined, () => _go('/vendor-orders')),
                      _metric('Completed', '$completed', Icons.check_circle_outline, () => _go('/vendor-orders')),
                    ],
                  ),
                  const SizedBox(height: 18),
                  _sectionTitle('Order pipeline', 'Current restaurant workload'),
                  const SizedBox(height: 10),
                  Card(
                    child: Padding(
                      padding: const EdgeInsets.all(16),
                      child: Row(
                        children: [
                          _pipeline('New', newOrders),
                          const Icon(Icons.chevron_right),
                          _pipeline('Preparing', preparing),
                          const Icon(Icons.chevron_right),
                          _pipeline('Ready', ready),
                        ],
                      ),
                    ),
                  ),
                  const SizedBox(height: 18),
                  _sectionTitle('Business snapshot', 'Database-backed restaurant performance'),
                  const SizedBox(height: 10),
                  Card(
                    child: Column(
                      children: [
                        ListTile(leading: const Icon(Icons.payments_outlined), title: const Text('Net food sales • 30 days'), trailing: Text(_ghs(_finance['net_food_sales']), style: const TextStyle(fontWeight: FontWeight.w900))),
                        const Divider(height: 1),
                        ListTile(leading: const Icon(Icons.receipt_long_outlined), title: const Text('Completed orders • 30 days'), trailing: Text('${_finance['completed_orders'] ?? 0}', style: const TextStyle(fontWeight: FontWeight.w900))),
                        const Divider(height: 1),
                        ListTile(leading: const Icon(Icons.inventory_2_outlined), title: const Text('Low-stock items'), trailing: Text('${_inventory['low_stock'] ?? 0}', style: const TextStyle(fontWeight: FontWeight.w900))),
                        const Divider(height: 1),
                        ListTile(leading: const Icon(Icons.remove_shopping_cart_outlined), title: const Text('Out of stock'), trailing: Text('${_inventory['out_of_stock'] ?? 0}', style: const TextStyle(fontWeight: FontWeight.w900))),
                      ],
                    ),
                  ),
                  const SizedBox(height: 18),
                  _sectionTitle('Restaurant controls', 'Manage the customer-facing operation'),
                  const SizedBox(height: 10),
                  Card(
                    child: SwitchListTile.adaptive(
                      secondary: Icon(_storeOpen ? Icons.storefront : Icons.storefront_outlined),
                      title: Text(_storeOpen ? 'Restaurant Open' : 'Restaurant Closed', style: const TextStyle(fontWeight: FontWeight.w800)),
                      subtitle: Text(_storeOpen ? 'Customers can place new orders.' : 'New customer orders are currently paused.'),
                      value: _storeOpen,
                      onChanged: _toggleStore,
                    ),
                  ),
                  const SizedBox(height: 18),
                  _sectionTitle('Operations', 'Open the dedicated management workspace'),
                  const SizedBox(height: 10),
                  _actionGrid(),
                  const SizedBox(height: 18),
                  _sectionTitle('Production tools', 'Keep the kitchen and service points connected'),
                  const SizedBox(height: 10),
                  Row(
                    children: [
                      Expanded(child: _action('Kitchen Display', Icons.tv_outlined, () => _go('/vendor-display'))),
                      const SizedBox(width: 10),
                      Expanded(child: _action('Kiosk', Icons.point_of_sale_outlined, () => _go('/kiosk'))),
                    ],
                  ),
                ],
              ),
            ),
    );
  }

  Widget _restaurantHeader(String name) => Card(
        child: Padding(
          padding: const EdgeInsets.all(18),
          child: Row(
            children: [
              const CircleAvatar(radius: 29, child: Icon(Icons.restaurant_rounded, size: 29)),
              const SizedBox(width: 14),
              Expanded(
                child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                  Text(name, maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 20, fontWeight: FontWeight.w900)),
                  const SizedBox(height: 4),
                  const Text('Restaurant command centre'),
                ]),
              ),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                decoration: BoxDecoration(borderRadius: BorderRadius.circular(30), color: (_storeOpen ? Colors.green : Colors.red).withValues(alpha: .10)),
                child: Text(_storeOpen ? 'OPEN' : 'CLOSED', style: TextStyle(fontSize: 11, fontWeight: FontWeight.w900, color: _storeOpen ? Colors.green.shade700 : Colors.red.shade700)),
              ),
            ],
          ),
        ),
      );

  Widget _sectionTitle(String title, String subtitle) => Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Text(title, style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w900)),
        const SizedBox(height: 3),
        Text(subtitle, style: Theme.of(context).textTheme.bodySmall),
      ]);

  Widget _metric(String title, String value, IconData icon, VoidCallback onTap) => Card(
        clipBehavior: Clip.antiAlias,
        child: InkWell(
          onTap: onTap,
          child: Padding(
            padding: const EdgeInsets.all(14),
            child: Row(children: [
              CircleAvatar(child: Icon(icon, size: 19)),
              const SizedBox(width: 10),
              Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, mainAxisAlignment: MainAxisAlignment.center, children: [
                Text(value, style: const TextStyle(fontSize: 22, fontWeight: FontWeight.w900)),
                const SizedBox(height: 2),
                Text(title, style: const TextStyle(fontSize: 11, fontWeight: FontWeight.w700)),
              ])),
            ]),
          ),
        ),
      );

  Widget _pipeline(String label, int count) => Expanded(child: Column(children: [Text('$count', style: const TextStyle(fontSize: 21, fontWeight: FontWeight.w900)), const SizedBox(height: 3), Text(label, textAlign: TextAlign.center, style: const TextStyle(fontSize: 10))]));

  Widget _actionGrid() => GridView.count(
        crossAxisCount: 2,
        shrinkWrap: true,
        physics: const NeverScrollableScrollPhysics(),
        mainAxisSpacing: 10,
        crossAxisSpacing: 10,
        childAspectRatio: 2.2,
        children: [
          _action('Orders', Icons.receipt_long_outlined, () => _go('/vendor-orders')),
          _action('Menu Management', Icons.restaurant_menu_outlined, () => _go('/vendor-menu')),
          _action('Inventory', Icons.inventory_2_outlined, () => _go('/vendor-inventory')),
          _action('Finance', Icons.account_balance_wallet_outlined, () => _go('/vendor-finance')),
          _action('Promotions', Icons.local_offer_outlined, () => _go('/vendor-promotions')),
          _action('Payout Account', Icons.payments_outlined, () => _go('/vendor-payout-account')),
        ],
      );

  Widget _action(String label, IconData icon, VoidCallback onTap) => Card(
        clipBehavior: Clip.antiAlias,
        child: InkWell(onTap: onTap, child: Padding(padding: const EdgeInsets.symmetric(horizontal: 12), child: Row(children: [CircleAvatar(radius: 18, child: Icon(icon, size: 18)), const SizedBox(width: 10), Expanded(child: Text(label, style: const TextStyle(fontWeight: FontWeight.w800, fontSize: 12)))]))),
      );

  Widget _messageCard(String message, IconData icon) => Card(child: ListTile(leading: Icon(icon), title: Text(message), trailing: IconButton(onPressed: _load, icon: const Icon(Icons.refresh))));
}
