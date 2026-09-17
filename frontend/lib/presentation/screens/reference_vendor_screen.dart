import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../core/theme/app_theme.dart';
import '../../domain/models/models.dart';
import '../providers/cafeteria_provider.dart';
import '../widgets/reference_design.dart';

class ReferenceVendorScreen extends StatefulWidget {
  const ReferenceVendorScreen({super.key});
  @override
  State<ReferenceVendorScreen> createState() => _ReferenceVendorScreenState();
}

class _ReferenceVendorScreenState extends State<ReferenceVendorScreen> {
  String page = 'Dashboard';

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

  static const _pages = ['Dashboard', 'Menu Catalog', 'Orders', 'Performance'];
  int get _index => _pages.indexOf(page).clamp(0, 3);

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
