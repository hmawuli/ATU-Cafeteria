import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../core/theme/app_theme.dart';
import '../../domain/models/models.dart';
import '../providers/cafeteria_provider.dart';
import '../providers/cart_provider.dart';
import '../widgets/reference_design.dart';

class ReferenceStudentScreen extends StatefulWidget {
  const ReferenceStudentScreen({super.key});
  @override
  State<ReferenceStudentScreen> createState() => _ReferenceStudentScreenState();
}

class _ReferenceStudentScreenState extends State<ReferenceStudentScreen> {
  int _tab = 0;
  String _query = '';

  @override
  Widget build(BuildContext context) {
    final provider = context.watch<CafeteriaProvider>();
    final user = provider.currentUser;
    if (user == null) {
      return const Scaffold(body: Center(child: Text('Please sign in again.')));
    }

    return Scaffold(
      body: SafeArea(
        child: LayoutBuilder(
          builder: (context, constraints) {
            final wide = constraints.maxWidth >= 850;
            return Column(
              children: [
                Container(
                  height: 66,
                  color: AppTheme.primary,
                  padding: const EdgeInsets.symmetric(horizontal: 18),
                  child: Row(
                    children: [
                      AtuBrand.title(compact: true),
                      const Spacer(),
                      Consumer<CartProvider>(
                        builder: (_, cart, __) => Badge(
                          isLabelVisible: cart.itemCount > 0,
                          label: Text('${cart.itemCount}'),
                          child: IconButton(
                            onPressed: () => Navigator.pushNamed(context, '/cart'),
                            icon: const Icon(Icons.shopping_cart_outlined, color: Colors.white),
                          ),
                        ),
                      ),
                      IconButton(
                        onPressed: () {
                          provider.logOut();
                          Navigator.pushReplacementNamed(context, '/login');
                        },
                        icon: const Icon(Icons.logout, color: Colors.white),
                      ),
                    ],
                  ),
                ),
                Expanded(
                  child: IndexedStack(
                    index: _tab,
                    children: [
                      _home(provider),
                      _vendors(provider),
                      _orders(provider),
                      _profile(provider, user),
                    ],
                  ),
                ),
                if (!wide)
                  NavigationBar(
                    selectedIndex: _tab,
                    onDestinationSelected: (value) => setState(() => _tab = value),
                    destinations: const [
                      NavigationDestination(icon: Icon(Icons.home_outlined), label: 'Home'),
                      NavigationDestination(icon: Icon(Icons.storefront_outlined), label: 'Vendors'),
                      NavigationDestination(icon: Icon(Icons.receipt_long_outlined), label: 'My Orders'),
                      NavigationDestination(icon: Icon(Icons.person_outline), label: 'Profile'),
                    ],
                  ),
              ],
            );
          },
        ),
      ),
    );
  }

  Widget _home(CafeteriaProvider provider) {
    final items = provider.allFoodItems.where((item) {
      if (!item.isAvailable) return false;
      final q = _query.toLowerCase().trim();
      return q.isEmpty || '${item.name} ${item.category} ${item.description}'.toLowerCase().contains(q);
    }).toList();

    return RefreshIndicator(
      onRefresh: provider.refreshAllData,
      child: ListView(
        padding: const EdgeInsets.all(18),
        children: [
          Container(
            padding: const EdgeInsets.all(20),
            decoration: BoxDecoration(
              gradient: const LinearGradient(colors: [AppTheme.primary, Color(0xFF0B56A5)]),
              borderRadius: BorderRadius.circular(22),
            ),
            child: Row(
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Text('Good Morning,', style: TextStyle(color: Colors.white70, fontSize: 17)),
                      const SizedBox(height: 3),
                      Text('${provider.currentUser?.fullName ?? 'Student'}! 👋', style: const TextStyle(color: Colors.white, fontSize: 27, fontWeight: FontWeight.w900)),
                      const SizedBox(height: 5),
                      const Text('Great food fuels great minds.', style: TextStyle(color: Colors.white70)),
                    ],
                  ),
                ),
                const Icon(Icons.restaurant, color: AppTheme.accent, size: 62),
              ],
            ),
          ),
          const SizedBox(height: 14),
          TextField(
            decoration: const InputDecoration(prefixIcon: Icon(Icons.search), hintText: 'Search for food, vendors or meals...'),
            onChanged: (value) => setState(() => _query = value),
          ),
          const SizedBox(height: 20),
          Row(
            children: [
              const Expanded(child: Text('Featured Vendors', style: TextStyle(fontSize: 20, fontWeight: FontWeight.w900, color: AppTheme.textDark))),
              TextButton(onPressed: () => setState(() => _tab = 1), child: const Text('View All')),
            ],
          ),
          const SizedBox(height: 8),
          SizedBox(height: 185, child: ListView(scrollDirection: Axis.horizontal, children: _vendorCards(provider))),
          const SizedBox(height: 22),
          const Text('Today’s Menu', style: TextStyle(fontSize: 20, fontWeight: FontWeight.w900, color: AppTheme.textDark)),
          const SizedBox(height: 10),
          if (items.isEmpty)
            const ReferenceCard(child: Text('No meals are available right now.'))
          else
            ...items.take(8).map((item) => _meal(item)),
        ],
      ),
    );
  }

  List<Widget> _vendorCards(CafeteriaProvider provider) {
    final ids = <int>[];
    for (final item in provider.allFoodItems) {
      if (!ids.contains(item.vendorId)) ids.add(item.vendorId);
    }
    if (ids.isEmpty) return [];

    return List.generate(ids.take(3).length, (index) {
      final foods = provider.allFoodItems.where((f) => f.vendorId == ids[index] && f.isAvailable).toList();
      final image = foods.isNotEmpty ? foods.first.imageUrl : '';
      return Container(
        width: 210,
        margin: const EdgeInsets.only(right: 12),
        child: ReferenceCard(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              FoodImage(url: image, width: double.infinity, height: 82),
              const SizedBox(height: 8),
              Text('Campus Vendor ${index + 1}', maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontWeight: FontWeight.w900, color: AppTheme.textDark)),
              Text(foods.isNotEmpty ? foods.first.category : 'Campus Food', style: const TextStyle(fontSize: 11, color: AppTheme.textMuted)),
              const Spacer(),
              Row(children: [const Icon(Icons.star, color: AppTheme.accent, size: 17), const Text(' 4.6', style: TextStyle(fontWeight: FontWeight.w800)), const Spacer(), StatusPill('Open')]),
            ],
          ),
        ),
      );
    });
  }

  Widget _vendors(CafeteriaProvider provider) => ListView(
        padding: const EdgeInsets.all(18),
        children: [
          const Text('Vendors & Food', style: TextStyle(fontSize: 24, fontWeight: FontWeight.w900, color: AppTheme.textDark)),
          const SizedBox(height: 4),
          const Text('Choose a campus vendor and explore today’s menu.', style: TextStyle(color: AppTheme.textMuted)),
          const SizedBox(height: 14),
          TextField(decoration: const InputDecoration(prefixIcon: Icon(Icons.search), hintText: 'Search vendors...')),
          const SizedBox(height: 12),
          ..._vendorCards(provider).map((widget) => Padding(padding: const EdgeInsets.only(bottom: 10), child: widget)),
        ],
      );

  Widget _meal(FoodItem item) => ReferenceCard(
        child: Row(
          children: [
            FoodImage(url: item.imageUrl, width: 100, height: 86),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(item.name, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w900, color: AppTheme.textDark)),
                  const SizedBox(height: 3),
                  Text(item.description, maxLines: 2, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 11, color: AppTheme.textMuted)),
                  const SizedBox(height: 7),
                  Text('GH₵ ${item.price.toStringAsFixed(2)}', style: const TextStyle(fontSize: 17, fontWeight: FontWeight.w900, color: AppTheme.primary)),
                ],
              ),
            ),
            FilledButton(
              onPressed: () {
                context.read<CartProvider>().add(item);
                ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('${item.name} added to cart')));
              },
              child: const Text('+ Add'),
            ),
          ],
        ),
      );

  Widget _orders(CafeteriaProvider provider) => ListView(
        padding: const EdgeInsets.all(18),
        children: [
          Row(children: [const Expanded(child: Text('My Orders', style: TextStyle(fontSize: 24, fontWeight: FontWeight.w900, color: AppTheme.textDark))), IconButton(onPressed: provider.refreshAllData, icon: const Icon(Icons.refresh))]),
          const SizedBox(height: 8),
          if (provider.customerOrders.isEmpty)
            const ReferenceCard(child: Column(children: [Icon(Icons.receipt_long, size: 48, color: AppTheme.primary), SizedBox(height: 8), Text('No orders yet.'), SizedBox(height: 8), Text('Browse the menu and place your first order.', style: TextStyle(color: AppTheme.textMuted))]))
          else
            ...provider.customerOrders.map(_orderCard),
        ],
      );

  Widget _orderCard(Order order) => ReferenceCard(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(children: [Expanded(child: Text('Order #ATU-${order.id ?? 0}', style: const TextStyle(fontSize: 17, fontWeight: FontWeight.w900, color: AppTheme.textDark))), StatusPill(order.displayStatus)]),
            const SizedBox(height: 6),
            Text('${order.foodName} • Qty: ${order.quantity} • GH₵ ${order.totalPrice.toStringAsFixed(2)}', style: const TextStyle(color: AppTheme.textMuted)),
            const SizedBox(height: 16),
            StepTrack(status: order.status),
            const SizedBox(height: 12),
            Align(
              alignment: Alignment.centerRight,
              child: OutlinedButton.icon(
                onPressed: order.id == null ? null : () => Navigator.pushNamed(context, '/order-tracking', arguments: order.id),
                icon: const Icon(Icons.track_changes),
                label: const Text('Track Order'),
              ),
            ),
          ],
        ),
      );

  Widget _profile(CafeteriaProvider provider, User user) => ListView(
        padding: const EdgeInsets.all(18),
        children: [
          ReferenceCard(
            child: Row(
              children: [
                CircleAvatar(radius: 34, backgroundColor: AppTheme.primary, child: Text(user.fullName.isEmpty ? 'S' : user.fullName[0].toUpperCase(), style: const TextStyle(color: Colors.white, fontSize: 25, fontWeight: FontWeight.w900))),
                const SizedBox(width: 14),
                Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text(user.fullName, style: const TextStyle(fontSize: 20, fontWeight: FontWeight.w900, color: AppTheme.textDark)), Text(user.email ?? user.username, style: const TextStyle(color: AppTheme.textMuted)), const SizedBox(height: 5), StatusPill(user.accountStatus)])),
              ],
            ),
          ),
          const SizedBox(height: 12),
          MetricTile(label: 'Wallet Balance', value: 'GH₵ ${user.balance.toStringAsFixed(2)}', icon: Icons.account_balance_wallet_outlined),
          const SizedBox(height: 12),
          ReferenceCard(
            child: Column(
              children: [
                ListTile(leading: const Icon(Icons.settings_outlined), title: const Text('Settings'), trailing: const Icon(Icons.chevron_right), onTap: () {}),
                const Divider(),
                ListTile(leading: const Icon(Icons.logout, color: Colors.red), title: const Text('Sign out'), onTap: () { provider.logOut(); Navigator.pushReplacementNamed(context, '/login'); }),
              ],
            ),
          ),
        ],
      );
}
