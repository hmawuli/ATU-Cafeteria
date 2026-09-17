import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../core/theme/app_theme.dart';
import '../../domain/models/models.dart';
import '../providers/cafeteria_provider.dart';
import '../providers/cart_provider.dart';
import '../widgets/reference_design.dart';

class ReferenceVendorScreen extends StatefulWidget {
  const ReferenceVendorScreen({super.key});
  @override
  State<ReferenceVendorScreen> createState() => _ReferenceVendorScreenState();
}

class _ReferenceVendorScreenState extends State<ReferenceVendorScreen> {
  String page = 'Dashboard';
  String _kioskQuery = '';
  String _kioskCategory = 'All';

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
                    onDestinationSelected: (i) => setState(() => page = _pages[i]),
                    destinations: const [
                      NavigationDestination(icon: Icon(Icons.dashboard), label: 'Dashboard'),
                      NavigationDestination(icon: Icon(Icons.restaurant_menu), label: 'Menu'),
                      NavigationDestination(icon: Icon(Icons.receipt_long), label: 'Orders'),
                      NavigationDestination(icon: Icon(Icons.point_of_sale_outlined), label: 'Kiosk'),
                      NavigationDestination(icon: Icon(Icons.insights), label: 'Performance'),
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
                          border: Border(bottom: BorderSide(color: AppTheme.border)),
                        ),
                        child: Row(
                          children: [
                            Expanded(child: Text(page, style: const TextStyle(fontSize: 22, fontWeight: FontWeight.w900, color: AppTheme.textDark))),
                            StatusPill(provider.currentUser?.isOpen == true ? 'Open' : 'Closed'),
                            const SizedBox(width: 12),
                            IconButton(onPressed: provider.refreshAllData, icon: const Icon(Icons.refresh)),
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

  static const _pages = ['Dashboard', 'Menu Catalog', 'Orders', 'Kiosk', 'Performance'];
  int get _index => _pages.indexOf(page).clamp(0, _pages.length - 1);

  Widget _mobileHeader(CafeteriaProvider provider) => Container(
        height: 64,
        color: AppTheme.primary,
        padding: const EdgeInsets.symmetric(horizontal: 16),
        child: Row(
          children: [
            AtuBrand.title(compact: true),
            const Spacer(),
            IconButton(onPressed: provider.refreshAllData, icon: const Icon(Icons.refresh, color: Colors.white)),
            IconButton(onPressed: () { provider.logOut(); Navigator.pushReplacementNamed(context, '/login'); }, icon: const Icon(Icons.logout, color: Colors.white)),
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
      default:
        return _dashboard(provider);
    }
  }

  Widget _dashboard(CafeteriaProvider provider) {
    final orders = provider.vendorOrders;
    final active = orders.where((o) => !['COMPLETED', 'DELIVERED', 'CANCELLED', 'DECLINED'].contains(o.status.toUpperCase())).length;
    return ListView(
      padding: const EdgeInsets.all(24),
      children: [
        const Text('Vendor Dashboard', style: TextStyle(fontSize: 24, fontWeight: FontWeight.w900, color: AppTheme.textDark)),
        const Text('Manage your campus store and customer orders.'),
        const SizedBox(height: 18),
        Wrap(spacing: 12, runSpacing: 12, children: [
          MetricTile(label: 'Total Orders', value: '${orders.length}', icon: Icons.receipt_long),
          MetricTile(label: 'Active Orders', value: '$active', icon: Icons.timelapse),
          const MetricTile(label: 'Rating', value: '4.7 ★', icon: Icons.star),
          MetricTile(label: 'Menu Items', value: '${provider.vendorFoodItems.length}', icon: Icons.restaurant_menu),
        ]),
        const SizedBox(height: 20),
        ReferenceCard(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text('Recent Orders', style: TextStyle(fontSize: 18, fontWeight: FontWeight.w900, color: AppTheme.textDark)),
              const SizedBox(height: 12),
              if (orders.isEmpty) const Text('No customer orders yet.') else ...orders.take(5).map(_row),
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
              const Expanded(child: Text('Menu Catalog', style: TextStyle(fontSize: 24, fontWeight: FontWeight.w900, color: AppTheme.textDark))),
              FilledButton.icon(onPressed: () => _addFood(provider), icon: const Icon(Icons.add), label: const Text('Add Food')),
            ],
          ),
          const SizedBox(height: 14),
          if (provider.vendorFoodItems.isEmpty)
            const ReferenceCard(child: Text('No food items yet. Tap Add Food to create your first menu item.'))
          else
            ...provider.vendorFoodItems.map((food) => Padding(
                  padding: const EdgeInsets.only(bottom: 10),
                  child: ReferenceCard(
                    child: Row(
                      children: [
                        FoodImage(url: food.imageUrl),
                        const SizedBox(width: 14),
                        Expanded(
                          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                            Text(food.name, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w900, color: AppTheme.textDark)),
                            Text(food.category, style: const TextStyle(color: AppTheme.textMuted)),
                            const SizedBox(height: 4),
                            Text('GH₵ ${food.price.toStringAsFixed(2)}', style: const TextStyle(fontWeight: FontWeight.w900, color: AppTheme.primary)),
                          ]),
                        ),
                        StatusPill(food.isAvailable ? 'Available' : 'Unavailable'),
                      ],
                    ),
                  ),
                )),
        ],
      );

  Widget _orders(CafeteriaProvider provider) => ListView(
        padding: const EdgeInsets.all(24),
        children: [
          const Text('Orders', style: TextStyle(fontSize: 24, fontWeight: FontWeight.w900, color: AppTheme.textDark)),
          const SizedBox(height: 14),
          ReferenceCard(
            child: Column(
              children: provider.vendorOrders.isEmpty
                  ? [const Padding(padding: EdgeInsets.all(20), child: Text('No customer orders yet.'))]
                  : provider.vendorOrders.map(_row).toList(),
            ),
          ),
        ],
      );

  Widget _row(Order order) => Padding(
        padding: const EdgeInsets.symmetric(vertical: 9),
        child: Row(
          children: [
            SizedBox(width: 90, child: Text('#ATU-${order.id ?? 0}', style: const TextStyle(fontWeight: FontWeight.w900))),
            Expanded(child: Text('${order.foodName} × ${order.quantity}')),
            Text('GH₵ ${order.totalPrice.toStringAsFixed(2)}', style: const TextStyle(fontWeight: FontWeight.w800)),
            const SizedBox(width: 10),
            StatusPill(order.displayStatus),
            const SizedBox(width: 8),
            _nextAction(order),
          ],
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
          await context.read<CafeteriaProvider>().updateOrderStatus(order.id!, next!);
          if (mounted) setState(() {});
        },
        child: Text(
          next == 'COMPLETED' ? 'Complete' : next == 'READY' ? 'Mark Ready' : 'Preparing',
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
    final source = provider.vendorFoodItems.where((item) => item.isAvailable).toList();
    final categories = <String>{'All', ...source.map((e) => e.category)}.toList();
    final query = _kioskQuery.trim().toLowerCase();
    final items = source.where((item) {
      final matchesCategory = _kioskCategory == 'All' || item.category == _kioskCategory;
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
            gradient: LinearGradient(
              colors: [AppTheme.primaryDark, AppTheme.primary],
            ),
            borderRadius: BorderRadius.circular(18),
          ),
          child: Row(
            children: [
              Container(
                width: 54,
                height: 54,
                decoration: BoxDecoration(color: AppTheme.accent, borderRadius: BorderRadius.circular(14)),
                child: const Icon(Icons.point_of_sale_outlined, color: AppTheme.primaryDark, size: 30),
              ),
              const SizedBox(width: 16),
              const Expanded(
                child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                  Text('WALK-IN CUSTOMER KIOSK', style: TextStyle(color: Colors.white, fontSize: 20, fontWeight: FontWeight.w900)),
                  SizedBox(height: 4),
                  Text('Let customers browse this vendor, build an order and proceed to checkout.', style: TextStyle(color: Colors.white70)),
                ]),
              ),
              Badge(
                isLabelVisible: cart.itemCount > 0,
                label: Text('${cart.itemCount}'),
                child: IconButton(
                  tooltip: 'Open cart',
                  onPressed: () => Navigator.pushNamed(context, '/cart'),
                  icon: const Icon(Icons.shopping_cart_outlined, color: Colors.white, size: 30),
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
                value: categories.contains(_kioskCategory) ? _kioskCategory : 'All',
                items: categories.map((category) => DropdownMenuItem(value: category, child: Text(category))).toList(),
                onChanged: (value) => setState(() => _kioskCategory = value ?? 'All'),
              ),
            ],
          ),
        ),
        Expanded(
          child: Row(
            children: [
              Expanded(
                child: items.isEmpty
                    ? const Center(child: Text('No available food items match this search.'))
                    : GridView.builder(
                        padding: const EdgeInsets.fromLTRB(24, 8, 12, 24),
                        gridDelegate: const SliverGridDelegateWithMaxCrossAxisExtent(
                          maxCrossAxisExtent: 270,
                          mainAxisExtent: 245,
                          crossAxisSpacing: 14,
                          mainAxisSpacing: 14,
                        ),
                        itemCount: items.length,
                        itemBuilder: (_, index) => _KioskFoodCard(item: items[index]),
                      ),
              ),
              SizedBox(
                width: 300,
                child: ReferenceCard(
                  padding: const EdgeInsets.all(18),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      Row(children: [
                        const Expanded(child: Text('CURRENT ORDER', style: TextStyle(fontSize: 17, fontWeight: FontWeight.w900, color: AppTheme.textDark))),
                        Text('${cart.itemCount} items', style: const TextStyle(color: AppTheme.textMuted, fontSize: 11)),
                      ]),
                      const Divider(height: 24),
                      Expanded(
                        child: cart.isEmpty
                            ? const Center(child: Text('Tap a food item to add it to the customer order.', textAlign: TextAlign.center))
                            : ListView(
                                children: cart.lines.map((line) => ListTile(
                                  contentPadding: EdgeInsets.zero,
                                  title: Text(line.item.name, maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontWeight: FontWeight.w800)),
                                  subtitle: Text('Qty ${line.quantity}'),
                                  trailing: Text('GH₵ ${line.total.toStringAsFixed(2)}', style: const TextStyle(fontWeight: FontWeight.w800)),
                                )).toList(),
                              ),
                      ),
                      const Divider(height: 24),
                      Row(children: [
                        const Text('TOTAL', style: TextStyle(fontWeight: FontWeight.w800)),
                        const Spacer(),
                        Text('GH₵ ${cart.subtotal.toStringAsFixed(2)}', style: const TextStyle(fontSize: 19, fontWeight: FontWeight.w900, color: AppTheme.primary)),
                      ]),
                      const SizedBox(height: 12),
                      FilledButton.icon(
                        onPressed: cart.isEmpty ? null : () => Navigator.pushNamed(context, '/checkout'),
                        icon: const Icon(Icons.arrow_forward_rounded),
                        label: const Text('PROCEED TO CHECKOUT'),
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
          ),
        ),
      ],
    );
  }

  Widget _performance(CafeteriaProvider provider) {
    final revenue = provider.vendorOrders.fold<double>(0, (sum, order) => sum + order.totalPrice);
    final completed = provider.vendorOrders.where((o) => o.status.toUpperCase() == 'COMPLETED').length;
    return ListView(
      padding: const EdgeInsets.all(24),
      children: [
        const Text('Performance', style: TextStyle(fontSize: 24, fontWeight: FontWeight.w900, color: AppTheme.textDark)),
        const SizedBox(height: 14),
        Wrap(spacing: 12, runSpacing: 12, children: [
          MetricTile(label: 'Revenue', value: 'GH₵ ${revenue.toStringAsFixed(2)}', icon: Icons.payments),
          MetricTile(label: 'Completed', value: '$completed', icon: Icons.check_circle),
          const MetricTile(label: 'Prep Speed', value: '12.5 min', icon: Icons.timer),
          const MetricTile(label: 'Rating', value: '4.7 ★', icon: Icons.star),
        ]),
        const SizedBox(height: 20),
        const ReferenceCard(child: Center(child: Padding(padding: EdgeInsets.all(35), child: Icon(Icons.show_chart, size: 80, color: AppTheme.primary)))),
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
                TextField(controller: name, decoration: const InputDecoration(labelText: 'Food name')),
                const SizedBox(height: 10),
                TextField(controller: price, keyboardType: const TextInputType.numberWithOptions(decimal: true), decoration: const InputDecoration(labelText: 'Price (GH₵)')),
                const SizedBox(height: 10),
                DropdownButtonFormField<String>(
                  initialValue: category,
                  items: const ['Lunch Specials', 'Traditional', 'Drinks', 'Snacks'].map((value) => DropdownMenuItem(value: value, child: Text(value))).toList(),
                  onChanged: (value) => setDialogState(() => category = value ?? category),
                  decoration: const InputDecoration(labelText: 'Category'),
                ),
                const SizedBox(height: 10),
                TextField(controller: description, maxLines: 2, decoration: const InputDecoration(labelText: 'Description')),
              ],
            ),
          ),
          actions: [
            TextButton(onPressed: () => Navigator.pop(dialogContext, false), child: const Text('Cancel')),
            FilledButton(
              onPressed: () async {
                final value = double.tryParse(price.text.trim());
                if (name.text.trim().isEmpty || value == null || value <= 0) return;
                final success = await provider.addVendorFoodItem(name.text.trim(), value, category, description.text.trim());
                if (dialogContext.mounted) Navigator.pop(dialogContext, success);
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
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Food item added successfully.')));
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
            Expanded(child: FoodImage(url: item.imageUrl, width: double.infinity, height: 120)),
            Padding(
              padding: const EdgeInsets.fromLTRB(14, 10, 14, 12),
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Text(item.name, maxLines: 2, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 15, fontWeight: FontWeight.w900, color: AppTheme.textDark)),
                const SizedBox(height: 3),
                Text(item.category, style: const TextStyle(fontSize: 11, color: AppTheme.textMuted)),
                const SizedBox(height: 8),
                Row(children: [
                  Text('GH₵ ${item.price.toStringAsFixed(2)}', style: const TextStyle(fontWeight: FontWeight.w900, color: AppTheme.primary)),
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
