import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../core/network/api_client.dart';
import '../../core/theme/app_theme.dart';
import '../../domain/models/models.dart';
import '../providers/cafeteria_provider.dart';
import '../providers/cart_provider.dart';
import '../widgets/reference_design.dart';
import 'vendor_finance_screen.dart';
import 'vendor_promotions_screen.dart';

class ReferenceVendorScreen extends StatefulWidget {
  const ReferenceVendorScreen({super.key});
  @override
  State<ReferenceVendorScreen> createState() => _ReferenceVendorScreenState();
}

class _ReferenceVendorScreenState extends State<ReferenceVendorScreen> {
  String page = 'Dashboard';
  String _kioskQuery = '';
  String _kioskCategory = 'All';
  bool _metricsRequested = false;
  Map<String, dynamic>? _inventorySummary;

  @override
  void initState() {
    super.initState();
    _loadInventorySummary();
  }

  Future<void> _loadInventorySummary() async {
    try {
      final response = await context.read<ApiClient>().get('/vendor/inventory/summary');
      if (!mounted || response is! Map || response['summary'] is! Map) return;
      setState(() => _inventorySummary = Map<String, dynamic>.from(response['summary']));
    } catch (_) {}
  }

  @override
  Widget build(BuildContext context) {
    final provider = context.watch<CafeteriaProvider>();
    return Scaffold(
      body: SafeArea(
        child: LayoutBuilder(
          builder: (context, constraints) {
            final wide = constraints.maxWidth >= 900;
            if (!wide) {
              return Column(
                children: [
                  _mobileHeader(provider),
                  Expanded(child: _content(provider)),
                  NavigationBar(
                    selectedIndex: _index,
                    onDestinationSelected: (i) =>
                        setState(() => page = _pages[i]),
                    destinations: const [
                      NavigationDestination(
                          icon: Icon(Icons.dashboard), label: 'Dashboard'),
                      NavigationDestination(
                          icon: Icon(Icons.restaurant_menu), label: 'Menu'),
                      NavigationDestination(
                          icon: Icon(Icons.receipt_long), label: 'Orders'),
                      NavigationDestination(
                          icon: Icon(Icons.point_of_sale_outlined),
                          label: 'Kiosk'),
                      NavigationDestination(
                          icon: Icon(Icons.insights), label: 'Performance'),
                      NavigationDestination(
                          icon: Icon(Icons.account_balance_wallet_outlined),
                          label: 'Finance'),
                      NavigationDestination(
                          icon: Icon(Icons.local_offer_outlined),
                          label: 'Promotions'),
                    ],
                  ),
                ],
              );
            }
            return Row(
              children: [
                Sidebar(
                  selected: page,
                  onSelected: (value) => setState(() => page = value),
                  onLogout: () {
                    provider.logOut();
                    Navigator.pushReplacementNamed(context, '/login');
                  },
                  vendor: true,
                ),
                Expanded(
                  child: Column(
                    children: [
                      Container(
                        height: 68,
                        padding: const EdgeInsets.symmetric(horizontal: 24),
                        decoration: const BoxDecoration(
                          color: Colors.white,
                          border: Border(
                              bottom: BorderSide(color: AppTheme.border)),
                        ),
                        child: Row(
                          children: [
                            Expanded(
                                child: Text(page,
                                    style: const TextStyle(
                                        fontSize: 22,
                                        fontWeight: FontWeight.w900,
                                        color: AppTheme.textDark))),
                            StatusPill(provider.currentUser?.isOpen == true
                                ? 'Open'
                                : 'Closed'),
                            const SizedBox(width: 12),
                            IconButton(
                                onPressed: provider.refreshAllData,
                                icon: const Icon(Icons.refresh)),
                          ],
                        ),
                      ),
                      Expanded(child: _content(provider)),
                    ],
                  ),
                ),
              ],
            );
          },
        ),
      ),
    );
  }

  static const _pages = [
    'Dashboard',
    'Menu Catalog',
    'Orders',
    'Kiosk',
    'Performance',
    'Finance',
    'Promotions'
  ];
  int get _index => _pages.indexOf(page).clamp(0, _pages.length - 1);

  Widget _mobileHeader(CafeteriaProvider provider) => Container(
        height: 64,
        color: AppTheme.primary,
        padding: const EdgeInsets.symmetric(horizontal: 16),
        child: Row(
          children: [
            AtuBrand.title(compact: true),
            const Spacer(),
            IconButton(
                onPressed: provider.refreshAllData,
                icon: const Icon(Icons.refresh, color: Colors.white)),
            IconButton(
                onPressed: () {
                  provider.logOut();
                  Navigator.pushReplacementNamed(context, '/login');
                },
                icon: const Icon(Icons.logout, color: Colors.white)),
          ],
        ),
      );

  Widget _content(CafeteriaProvider provider) {
    switch (page) {
      case 'Menu Catalog':
        return _menu(provider);
      case 'Orders':
        return _orders(provider);
      case 'Kiosk':
        return _kiosk(provider);
      case 'Performance':
        return _performance(provider);
      case 'Finance':
        return const VendorFinanceScreen();
      case 'Promotions':
        return const VendorPromotionsScreen();
      default:
        return _dashboard(provider);
    }
  }

  Widget _dashboard(CafeteriaProvider provider) {
    final orders = provider.vendorOrders;
    final active = orders
        .where((o) => !['COMPLETED', 'DELIVERED', 'CANCELLED', 'DECLINED']
            .contains(o.status.toUpperCase()))
        .length;
    return ListView(
      padding: const EdgeInsets.all(24),
      children: [
        const Text('Vendor Dashboard',
            style: TextStyle(
                fontSize: 24,
                fontWeight: FontWeight.w900,
                color: AppTheme.textDark)),
        const Text('Manage your campus store and customer orders.'),
        const SizedBox(height: 18),
        if (_inventorySummary != null) ...[
          ReferenceCard(
            child: Row(
              children: [
                const Icon(Icons.inventory_2_outlined, color: AppTheme.primary),
                const SizedBox(width: 12),
                const Expanded(
                  child: Text('Inventory health', style: TextStyle(fontWeight: FontWeight.w900, color: AppTheme.textDark)),
                ),
                Text('Low ${_inventorySummary!['low_stock'] ?? 0}  •  Out ${_inventorySummary!['out_of_stock'] ?? 0}', style: const TextStyle(fontWeight: FontWeight.w800, color: AppTheme.textMuted)),
                const SizedBox(width: 8),
                IconButton(tooltip: 'Refresh inventory', onPressed: _loadInventorySummary, icon: const Icon(Icons.refresh, size: 18)),
              ],
            ),
          ),
          const SizedBox(height: 12),
        ],
        Wrap(spacing: 12, runSpacing: 12, children: [
          MetricTile(
              label: 'Total Orders',
              value: '${orders.length}',
              icon: Icons.receipt_long),
          MetricTile(
              label: 'Active Orders', value: '$active', icon: Icons.timelapse),
          MetricTile(
              label: 'Rating',
              value: '${provider.getAverageRating(provider.vendorFeedback).toStringAsFixed(1)} ★',
              icon: Icons.star),
          MetricTile(
              label: 'Menu Items',
              value: '${provider.vendorFoodItems.length}',
              icon: Icons.restaurant_menu),
        ]),
        const SizedBox(height: 20),
        ReferenceCard(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text('Recent Orders',
                  style: TextStyle(
                      fontSize: 18,
                      fontWeight: FontWeight.w900,
                      color: AppTheme.textDark)),
              const SizedBox(height: 12),
              if (orders.isEmpty)
                const Text('No customer orders yet.')
              else
                ...orders.take(5).map(_row),
            ],
          ),
        ),
      ],
    );
  }

  Widget _menu(CafeteriaProvider provider) => ListView(
        padding: const EdgeInsets.all(24),
        children: [
          Row(
            children: [
              const Expanded(
                  child: Text('Menu Catalog',
                      style: TextStyle(
                          fontSize: 24,
                          fontWeight: FontWeight.w900,
                          color: AppTheme.textDark))),
              FilledButton.icon(
                  onPressed: () => _addFood(provider),
                  icon: const Icon(Icons.add),
                  label: const Text('Add Food')),
            ],
          ),
          const SizedBox(height: 14),
          if (provider.vendorFoodItems.isEmpty)
            const ReferenceCard(
                child: Text(
                    'No food items yet. Tap Add Food to create your first menu item.'))
          else
            ...provider.vendorFoodItems.map((food) => Padding(
                  padding: const EdgeInsets.only(bottom: 10),
                  child: ReferenceCard(
                    child: Row(
                      children: [
                        FoodImage(url: food.imageUrl),
                        const SizedBox(width: 14),
                        Expanded(
                          child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(food.name,
                                    style: const TextStyle(
                                        fontSize: 16,
                                        fontWeight: FontWeight.w900,
                                        color: AppTheme.textDark)),
                                Text(food.category,
                                    style: const TextStyle(
                                        color: AppTheme.textMuted)),
                                const SizedBox(height: 4),
                                Text('GH₵ ${food.price.toStringAsFixed(2)}',
                                    style: const TextStyle(
                                        fontWeight: FontWeight.w900,
                                        color: AppTheme.primary)),
                              ]),
                        ),
                        StatusPill(
                            food.isAvailable ? 'Available' : 'Unavailable'),
                      ],
                    ),
                  ),
                )),
        ],
      );

  Widget _orders(CafeteriaProvider provider) => ListView(
        padding: const EdgeInsets.all(24),
        children: [
          const Text('Orders',
              style: TextStyle(
                  fontSize: 24,
                  fontWeight: FontWeight.w900,
                  color: AppTheme.textDark)),
          const SizedBox(height: 14),
          ReferenceCard(
            child: Column(
              children: provider.vendorOrders.isEmpty
                  ? [
                      const Padding(
                          padding: EdgeInsets.all(20),
                          child: Text('No customer orders yet.'))
                    ]
                  : provider.vendorOrders.map(_row).toList(),
            ),
          ),
        ],
      );

  Widget _row(Order order) => Padding(
        padding: const EdgeInsets.symmetric(vertical: 9),
        child: LayoutBuilder(
          builder: (context, constraints) {
            final wide = constraints.maxWidth >= 640;
            final id = SizedBox(
                width: 90,
                child: Text('#ATU-${order.id ?? 0}',
                    style: const TextStyle(fontWeight: FontWeight.w900)));
            final name = Expanded(
                child: Text('${order.foodName} × ${order.quantity}'));
            final price = Text(
                'GH₵ ${order.totalPrice.toStringAsFixed(2)}',
                style: const TextStyle(fontWeight: FontWeight.w800));
            final pill = StatusPill(order.displayStatus);
            final action = _nextAction(order);
            if (wide) {
              return Row(
                children: [
                  id,
                  name,
                  price,
                  const SizedBox(width: 10),
                  pill,
                  const SizedBox(width: 8),
                  action,
                ],
              );
            }
            // Narrow layout: split across two lines so the row never overflows.
            return Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(children: [id, name]),
                const SizedBox(height: 6),
                Row(children: [
                  pill,
                  const SizedBox(width: 8),
                  price,
                  const Spacer(),
                  action,
                ]),
              ],
            );
          },
        ),
      );

  Widget _nextAction(Order order) {
    final status = order.status.toUpperCase();
    String? next;
    if (status == 'PENDING' || status == 'ORDER_PLACED') {
      next = 'PREPARING';
    } else if (status == 'PREPARING') {
      next = 'READY';
    } else if (status == 'READY' || status == 'READY_FOR_PICKUP') {
      next = 'COMPLETED';
    }
    if (next == null || order.id == null) return const SizedBox(width: 105);

    return SizedBox(
      width: 105,
      child: FilledButton(
        onPressed: () async {
          await context
              .read<CafeteriaProvider>()
              .updateOrderStatus(order.id!, next!);
          if (mounted) setState(() {});
        },
        child: Text(
          next == 'COMPLETED'
              ? 'Complete'
              : next == 'READY'
                  ? 'Mark Ready'
                  : 'Preparing',
          style: const TextStyle(fontSize: 9),
        ),
      ),
    );
  }

  // ============================================================
  // WALK-IN CUSTOMER KIOSK
  // ============================================================
  Widget _kiosk(CafeteriaProvider provider) {
    final cart = context.watch<CartProvider>();
    final source =
        provider.vendorFoodItems.where((item) => item.isAvailable).toList();
    final categories =
        <String>{'All', ...source.map((e) => e.category)}.toList();
    final query = _kioskQuery.trim().toLowerCase();
    final items = source.where((item) {
      final matchesCategory =
          _kioskCategory == 'All' || item.category == _kioskCategory;
      final matchesSearch = query.isEmpty ||
          item.name.toLowerCase().contains(query) ||
          item.category.toLowerCase().contains(query);
      return matchesCategory && matchesSearch;
    }).toList();

    return Column(
      children: [
        Container(
          margin: const EdgeInsets.fromLTRB(24, 18, 24, 0),
          padding: const EdgeInsets.all(20),
          decoration: BoxDecoration(
            gradient: const LinearGradient(
              colors: [AppTheme.primaryDark, AppTheme.primary],
            ),
            borderRadius: BorderRadius.circular(18),
          ),
          child: Row(
            children: [
              Container(
                width: 54,
                height: 54,
                decoration: BoxDecoration(
                    color: AppTheme.accent,
                    borderRadius: BorderRadius.circular(14)),
                child: const Icon(Icons.point_of_sale_outlined,
                    color: AppTheme.primaryDark, size: 30),
              ),
              const SizedBox(width: 16),
              const Expanded(
                child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text('WALK-IN CUSTOMER KIOSK',
                          style: TextStyle(
                              color: Colors.white,
                              fontSize: 20,
                              fontWeight: FontWeight.w900)),
                      SizedBox(height: 4),
                      Text(
                          'Let customers browse this vendor, build an order and proceed to checkout.',
                          style: TextStyle(color: Colors.white70)),
                    ]),
              ),
              Badge(
                isLabelVisible: cart.itemCount > 0,
                label: Text('${cart.itemCount}'),
                child: IconButton(
                  tooltip: 'Open cart',
                  onPressed: () => Navigator.pushNamed(context, '/cart'),
                  icon: const Icon(Icons.shopping_cart_outlined,
                      color: Colors.white, size: 30),
                ),
              ),
            ],
          ),
        ),
        Padding(
          padding: const EdgeInsets.fromLTRB(24, 14, 24, 8),
          child: Row(
            children: [
              Expanded(
                child: TextField(
                  onChanged: (value) => setState(() => _kioskQuery = value),
                  decoration: const InputDecoration(
                    hintText: 'Search this vendor menu...',
                    prefixIcon: Icon(Icons.search),
                  ),
                ),
              ),
              const SizedBox(width: 12),
              DropdownButton<String>(
                value: categories.contains(_kioskCategory)
                    ? _kioskCategory
                    : 'All',
                items: categories
                    .map((category) => DropdownMenuItem(
                        value: category, child: Text(category)))
                    .toList(),
                onChanged: (value) =>
                    setState(() => _kioskCategory = value ?? 'All'),
              ),
            ],
          ),
        ),
        Expanded(
          child: LayoutBuilder(
            builder: (context, kc) {
              final showPanel = kc.maxWidth >= 820;
              return Row(
                children: [
                  Expanded(
                    child: items.isEmpty
                    ? const Center(
                        child:
                            Text('No available food items match this search.'))
                    : GridView.builder(
                        padding: const EdgeInsets.fromLTRB(24, 8, 12, 24),
                        gridDelegate:
                            const SliverGridDelegateWithMaxCrossAxisExtent(
                          maxCrossAxisExtent: 270,
                          mainAxisExtent: 245,
                          crossAxisSpacing: 14,
                          mainAxisSpacing: 14,
                        ),
                        itemCount: items.length,
                        itemBuilder: (_, index) =>
                            _KioskFoodCard(item: items[index]),
                      ),
              ),
              if (showPanel)
                SizedBox(
                width: 300,
                child: ReferenceCard(
                  padding: const EdgeInsets.all(18),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      Row(children: [
                        const Expanded(
                            child: Text('CURRENT ORDER',
                                style: TextStyle(
                                    fontSize: 17,
                                    fontWeight: FontWeight.w900,
                                    color: AppTheme.textDark))),
                        Text('${cart.itemCount} items',
                            style: const TextStyle(
                                color: AppTheme.textMuted, fontSize: 11)),
                      ]),
                      const Divider(height: 24),
                      Expanded(
                        child: cart.isEmpty
                            ? const Center(
                                child: Text(
                                    'Tap a food item to add it to the customer order.',
                                    textAlign: TextAlign.center))
                            : ListView(
                                children: cart.lines
                                    .map((line) => ListTile(
                                          contentPadding: EdgeInsets.zero,
                                          title: Text(line.item.name,
                                              maxLines: 1,
                                              overflow: TextOverflow.ellipsis,
                                              style: const TextStyle(
                                                  fontWeight: FontWeight.w800)),
                                          subtitle:
                                              Text('Qty ${line.quantity}'),
                                          trailing: Text(
                                              'GH₵ ${line.total.toStringAsFixed(2)}',
                                              style: const TextStyle(
                                                  fontWeight: FontWeight.w800)),
                                        ))
                                    .toList(),
                              ),
                      ),
                      const Divider(height: 24),
                      Row(children: [
                        const Text('TOTAL',
                            style: TextStyle(fontWeight: FontWeight.w800)),
                        const Spacer(),
                        Text('GH₵ ${cart.subtotal.toStringAsFixed(2)}',
                            style: const TextStyle(
                                fontSize: 19,
                                fontWeight: FontWeight.w900,
                                color: AppTheme.primary)),
                      ]),
                      const SizedBox(height: 12),
                      FilledButton.icon(
                        onPressed: cart.isEmpty ? null : () => _completeWalkInSale(cart),
                        icon: const Icon(Icons.point_of_sale_outlined),
                        label: const Text('COMPLETE WALK-IN SALE'),
                      ),
                      const SizedBox(height: 6),
                      TextButton(
                        onPressed: cart.isEmpty ? null : cart.clear,
                        child: const Text('Clear order'),
                      ),
                    ],
                  ),
                ),
              ),
            ],
              );
            },
          ),
        ),
      ],
    );
  }

  Future<void> _completeWalkInSale(CartProvider cart) async {
    final customerName = TextEditingController();
    final customerPhone = TextEditingController();
    String paymentMethod = 'CASH';

    final confirmed = await showDialog<bool>(
      context: context,
      builder: (dialogContext) => StatefulBuilder(
        builder: (dialogContext, setDialogState) => AlertDialog(
          title: const Text('Complete Walk-In Sale'),
          content: SizedBox(
            width: 460,
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Text('Total: GH₵ ${cart.subtotal.toStringAsFixed(2)}', style: const TextStyle(fontWeight: FontWeight.w900)),
                const SizedBox(height: 14),
                TextField(
                  controller: customerName,
                  decoration: const InputDecoration(
                    labelText: 'Customer name (optional)',
                    prefixIcon: Icon(Icons.person_outline),
                  ),
                ),
                const SizedBox(height: 10),
                TextField(
                  controller: customerPhone,
                  keyboardType: TextInputType.phone,
                  decoration: const InputDecoration(
                    labelText: 'Phone number (optional)',
                    prefixIcon: Icon(Icons.phone_outlined),
                  ),
                ),
                const SizedBox(height: 10),
                DropdownButtonFormField<String>(
                  initialValue: paymentMethod,
                  items: const [
                    DropdownMenuItem(value: 'CASH', child: Text('Cash')),
                    DropdownMenuItem(value: 'MOMO', child: Text('Mobile Money')),
                    DropdownMenuItem(value: 'CARD', child: Text('Card')),
                  ],
                  onChanged: (value) => setDialogState(() => paymentMethod = value ?? 'CASH'),
                  decoration: const InputDecoration(
                    labelText: 'Payment method',
                    prefixIcon: Icon(Icons.payments_outlined),
                  ),
                ),
                const SizedBox(height: 8),
                const Align(
                  alignment: Alignment.centerLeft,
                  child: Text(
                    'The sale will appear in Orders and Finance as a Kiosk transaction.',
                    style: TextStyle(fontSize: 11, color: AppTheme.textMuted),
                  ),
                ),
              ],
            ),
          ),
          actions: [
            TextButton(onPressed: () => Navigator.pop(dialogContext, false), child: const Text('Cancel')),
            FilledButton(
              onPressed: () async {
                try {
                  await context.read<ApiClient>().post(
                    '/vendor/kiosk/orders',
                    idempotencyKey: ApiClient.newIdempotencyKey(),
                    body: {
                      'items': cart.toCheckoutPayload(),
                      if (customerName.text.trim().isNotEmpty) 'customer_name': customerName.text.trim(),
                      if (customerPhone.text.trim().isNotEmpty) 'customer_phone': customerPhone.text.trim(),
                      'payment_method': paymentMethod,
                    },
                  );
                  if (dialogContext.mounted) Navigator.pop(dialogContext, true);
                } catch (e) {
                  if (dialogContext.mounted) {
                    ScaffoldMessenger.of(dialogContext).showSnackBar(
                      SnackBar(content: Text(e.toString().replaceFirst('Exception: ', ''))),
                    );
                  }
                }
              },
              child: const Text('Record Sale'),
            ),
          ],
        ),
      ),
    );

    customerName.dispose();
    customerPhone.dispose();

    if (confirmed == true && mounted) {
      cart.clear();
      await context.read<CafeteriaProvider>().refreshAllData();
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Walk-in sale recorded successfully.')),
      );
    }
  }

  Widget _performance(CafeteriaProvider provider) {
    final vendorId = provider.currentUser?.id;
    if (!_metricsRequested) {
      _metricsRequested = true;
      if (vendorId != null) {
        WidgetsBinding.instance.addPostFrameCallback((_) {
          if (mounted) provider.fetchVendorPerformanceMetrics(vendorId);
        });
      }
    }

    final metrics = provider.remoteVendorMetrics ?? const <String, dynamic>{};
    final revenue = metrics['total_sales'] is num
        ? (metrics['total_sales'] as num).toDouble()
        : provider.vendorOrders.fold<double>(
            0, (sum, order) => sum + order.totalPrice);
    final completed = metrics['total_completed_orders'] is num
        ? (metrics['total_completed_orders'] as num).toInt()
        : provider.vendorOrders
            .where((o) => o.status.toUpperCase() == 'COMPLETED')
            .length;
    final rating = metrics['rating_overall']?.toString() ?? '—';
    final prepSpeed = metrics['avg_completion_time_display']?.toString() ??
        metrics['average_delivery_time_display']?.toString() ??
        '—';
    final fulfillment = metrics['order_fulfillment_rate']?.toString() ?? '—';
    final popular =
        metrics['popular_menu_items'] is List ? metrics['popular_menu_items'] : const [];

    final byDate = List<Map<String, dynamic>>.from(
        provider.remoteRechartsData ?? const []);
    final maxSales = byDate.fold<double>(0, (m, d) {
      final s = double.tryParse((d['sales'] ?? 0).toString()) ?? 0;
      return s > m ? s : m;
    });

    // Today's revenue and orders from the vendor daily-revenue endpoint.
    final now = DateTime.now();
    final todayKey = '${now.year.toString().padLeft(4, '0')}-'
        '${now.month.toString().padLeft(2, '0')}-'
        '${now.day.toString().padLeft(2, '0')}';
    Map<String, dynamic>? todayRow;
    for (final day in provider.remoteDailyRevenue ?? const []) {
      if (day is Map && (day['date']?.toString() ?? '') == todayKey) {
        todayRow = Map<String, dynamic>.from(day);
        break;
      }
    }
    final todayRevenue = todayRow == null
        ? 0.0
        : double.tryParse((todayRow['revenue'] ?? 0).toString()) ?? 0;
    final todayOrders = todayRow == null
        ? 0
        : int.tryParse((todayRow['orders_count'] ?? 0).toString()) ?? 0;

    return ListView(
      padding: const EdgeInsets.all(24),
      children: [
        const Text('Performance',
            style: TextStyle(
                fontSize: 24,
                fontWeight: FontWeight.w900,
                color: AppTheme.textDark)),
        const SizedBox(height: 14),
        Wrap(spacing: 12, runSpacing: 12, children: [
          MetricTile(
              label: 'Revenue',
              value: 'GH₵ ${revenue.toStringAsFixed(2)}',
              icon: Icons.payments),
          MetricTile(
              label: "Today's Revenue",
              value: 'GH₵ ${todayRevenue.toStringAsFixed(2)}',
              icon: Icons.today),
          MetricTile(
              label: "Today's Orders",
              value: '$todayOrders',
              icon: Icons.receipt_long),
          MetricTile(
              label: 'Completed', value: '$completed', icon: Icons.check_circle),
          MetricTile(
              label: 'Prep Speed', value: prepSpeed, icon: Icons.timer),
          MetricTile(
              label: 'Rating', value: '$rating ★', icon: Icons.star),
          MetricTile(
              label: 'Fulfilment', value: '$fulfillment%', icon: Icons.task_alt),
          MetricTile(
              label: 'Menu Items',
              value: '${provider.vendorFoodItems.length}',
              icon: Icons.restaurant_menu),
        ]),
        const SizedBox(height: 20),
        ReferenceCard(
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            const Text('Daily Sales Trend',
                style: TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.w900,
                    color: AppTheme.textDark)),
            const SizedBox(height: 6),
            const Text('Live sales from the analytics endpoint.',
                style: TextStyle(color: AppTheme.textMuted, fontSize: 11)),
            const SizedBox(height: 14),
            if (byDate.isEmpty)
              const Padding(
                padding: EdgeInsets.symmetric(vertical: 18),
                child: Row(children: [
                  Icon(Icons.show_chart, color: AppTheme.primary),
                  SizedBox(width: 12),
                  Expanded(
                      child: Text(
                          'No sales data yet. Place and complete orders to see trends.')),
                ]),
              )
            else
              ...byDate.take(14).map((day) {
                final s =
                    double.tryParse((day['sales'] ?? 0).toString()) ?? 0;
                final label = day['date']?.toString() ?? '';
                return Padding(
                  padding: const EdgeInsets.symmetric(vertical: 5),
                  child: Row(children: [
                    SizedBox(
                        width: 90,
                        child: Text(label,
                            style: const TextStyle(fontSize: 11))),
                    Expanded(
                        child: LinearProgressIndicator(
                            value: maxSales <= 0 ? 0 : (s / maxSales).clamp(0, 1),
                            minHeight: 12,
                            backgroundColor: AppTheme.primary.withValues(alpha: .1))),
                    const SizedBox(width: 8),
                    SizedBox(
                        width: 80,
                        child: Text('GH₵${s.toStringAsFixed(2)}',
                            textAlign: TextAlign.end,
                            style: const TextStyle(
                                fontWeight: FontWeight.w800, fontSize: 11))),
                  ]),
                );
              }),
          ]),
        ),
        const SizedBox(height: 16),
        if (popular.isNotEmpty)
          ReferenceCard(
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              const Text('Popular Menu Items',
                  style: TextStyle(
                      fontSize: 18,
                      fontWeight: FontWeight.w900,
                      color: AppTheme.textDark)),
              const SizedBox(height: 12),
              ...List<Map<String, dynamic>>.from(popular).map((item) {
                final name = item['name']?.toString() ?? 'Item';
                final qty = item['quantity_sold']?.toString() ?? '0';
                final sales = double.tryParse((item['sales'] ?? 0).toString());
                return Padding(
                  padding: const EdgeInsets.symmetric(vertical: 6),
                  child: Row(children: [
                    const Icon(Icons.restaurant_menu,
                        color: AppTheme.primary, size: 18),
                    const SizedBox(width: 10),
                    Expanded(
                        child: Text(name,
                            style: const TextStyle(fontWeight: FontWeight.w700))),
                    Text('×$qty  ·  GH₵${(sales ?? 0).toStringAsFixed(2)}',
                        style: const TextStyle(
                            color: AppTheme.textMuted, fontSize: 12)),
                  ]),
                );
              }),
            ]),
          ),
      ],
    );
  }

  Future<void> _addFood(CafeteriaProvider provider) async {
    final name = TextEditingController();
    final price = TextEditingController();
    final description = TextEditingController();
    String category = 'Lunch Specials';

    final result = await showDialog<bool>(
      context: context,
      builder: (dialogContext) => StatefulBuilder(
        builder: (dialogContext, setDialogState) => AlertDialog(
          title: const Text('Add Food Item'),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                TextField(
                    controller: name,
                    decoration: const InputDecoration(labelText: 'Food name')),
                const SizedBox(height: 10),
                TextField(
                    controller: price,
                    keyboardType:
                        const TextInputType.numberWithOptions(decimal: true),
                    decoration:
                        const InputDecoration(labelText: 'Price (GH₵)')),
                const SizedBox(height: 10),
                DropdownButtonFormField<String>(
                  initialValue: category,
                  items: const [
                    'Lunch Specials',
                    'Traditional',
                    'Drinks',
                    'Snacks'
                  ]
                      .map((value) =>
                          DropdownMenuItem(value: value, child: Text(value)))
                      .toList(),
                  onChanged: (value) =>
                      setDialogState(() => category = value ?? category),
                  decoration: const InputDecoration(labelText: 'Category'),
                ),
                const SizedBox(height: 10),
                TextField(
                    controller: description,
                    maxLines: 2,
                    decoration:
                        const InputDecoration(labelText: 'Description')),
              ],
            ),
          ),
          actions: [
            TextButton(
                onPressed: () => Navigator.pop(dialogContext, false),
                child: const Text('Cancel')),
            FilledButton(
              onPressed: () async {
                final value = double.tryParse(price.text.trim());
                if (name.text.trim().isEmpty || value == null || value <= 0) {
                  return;
                }
                final error = await provider.addVendorFoodItem(
                  name.text.trim(),
                  value,
                  category,
                  description.text.trim(),
                );

                if (!dialogContext.mounted) return;

                if (error != null) {
                  ScaffoldMessenger.of(dialogContext).showSnackBar(
                    SnackBar(
                      content: Text(error),
                      backgroundColor: Colors.red,
                    ),
                  );
                  return;
                }

                Navigator.pop(dialogContext, true);
              },
              child: const Text('Add Food'),
            ),
          ],
        ),
      ),
    );

    name.dispose();
    price.dispose();
    description.dispose();

    if (result == true && mounted) {
      await provider.refreshAllData();
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Food item added successfully.')));
    }
  }
}

class _KioskFoodCard extends StatelessWidget {
  final FoodItem item;
  const _KioskFoodCard({required this.item});

  @override
  Widget build(BuildContext context) {
    final cart = context.read<CartProvider>();
    return ReferenceCard(
      padding: EdgeInsets.zero,
      child: InkWell(
        borderRadius: BorderRadius.circular(16),
        onTap: () => cart.add(item),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Expanded(
                child: FoodImage(
                    url: item.imageUrl, width: double.infinity, height: 120)),
            Padding(
              padding: const EdgeInsets.fromLTRB(14, 10, 14, 12),
              child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(item.name,
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(
                            fontSize: 15,
                            fontWeight: FontWeight.w900,
                            color: AppTheme.textDark)),
                    const SizedBox(height: 3),
                    Text(item.category,
                        style: const TextStyle(
                            fontSize: 11, color: AppTheme.textMuted)),
                    const SizedBox(height: 8),
                    Row(children: [
                      Text('GH₵ ${item.price.toStringAsFixed(2)}',
                          style: const TextStyle(
                              fontWeight: FontWeight.w900,
                              color: AppTheme.primary)),
                      const Spacer(),
                      const Icon(Icons.add_circle, color: AppTheme.primary),
                    ]),
                  ]),
            ),
          ],
        ),
      ),
    );
  }
}
