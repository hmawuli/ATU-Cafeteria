import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../domain/models/models.dart';
import '../providers/cafeteria_provider.dart';
import '../providers/cart_provider.dart';

class StudentPortalScreen extends StatefulWidget {
  const StudentPortalScreen({super.key});

  @override
  State<StudentPortalScreen> createState() => _StudentPortalScreenState();
}

class _StudentPortalScreenState extends State<StudentPortalScreen> {
  int _tab = 0;
  String _query = '';

  static const navy = Color(0xFF063B82);
  static const gold = Color(0xFFFFC400);
  static const green = Color(0xFF20B95A);

  @override
  Widget build(BuildContext context) {
    final provider = context.watch<CafeteriaProvider>();
    final user = provider.currentUser;
    if (user == null) {
      return const Scaffold(body: Center(child: Text('Session expired. Please sign in again.')));
    }

    return Scaffold(
      backgroundColor: const Color(0xFFF5F8FC),
      appBar: AppBar(
        backgroundColor: navy,
        title: const Row(children: [
          Icon(Icons.restaurant, color: gold),
          SizedBox(width: 10),
          Text('ATU CAFETERIA', style: TextStyle(fontWeight: FontWeight.w900)),
        ]),
        actions: [
          IconButton(onPressed: () {}, icon: const Icon(Icons.notifications_none_rounded)),
          IconButton(
            onPressed: () => _showProfile(context, user.fullName),
            icon: const CircleAvatar(
              radius: 15,
              backgroundColor: Colors.white,
              child: Icon(Icons.person, color: navy, size: 19),
            ),
          ),
          const SizedBox(width: 8),
        ],
      ),
      body: IndexedStack(
        index: _tab,
        children: [
          _home(context, provider, user.fullName),
          _vendors(context, provider),
          _orders(context, provider),
          _profile(context, provider, user.fullName),
        ],
      ),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _tab,
        onDestinationSelected: (v) => setState(() => _tab = v),
        destinations: const [
          NavigationDestination(icon: Icon(Icons.home_outlined), selectedIcon: Icon(Icons.home), label: 'Home'),
          NavigationDestination(icon: Icon(Icons.storefront_outlined), selectedIcon: Icon(Icons.storefront), label: 'Vendors'),
          NavigationDestination(icon: Icon(Icons.shopping_cart_outlined), selectedIcon: Icon(Icons.shopping_cart), label: 'Cart'),
          NavigationDestination(icon: Icon(Icons.person_outline), selectedIcon: Icon(Icons.person), label: 'Profile'),
        ],
      ),
    );
  }

  Widget _home(BuildContext context, CafeteriaProvider provider, String name) {
    final items = provider.allFoodItems.where((e) => e.isAvailable).toList();
    final filtered = _query.trim().isEmpty
        ? items
        : items.where((e) => '${e.name} ${e.category} ${e.description}'.toLowerCase().contains(_query.toLowerCase())).toList();

    return RefreshIndicator(
      onRefresh: provider.refreshAllData,
      child: ListView(
        padding: const EdgeInsets.fromLTRB(18, 18, 18, 28),
        children: [
          _welcomeCard(name),
          const SizedBox(height: 18),
          TextField(
            onChanged: (v) => setState(() => _query = v),
            decoration: const InputDecoration(
              hintText: 'Search for food, vendors or meals...',
              prefixIcon: Icon(Icons.search, color: navy, size: 30),
            ),
          ),
          const SizedBox(height: 24),
          _sectionTitle('Featured Vendors', 'View All', () => setState(() => _tab = 1)),
          const SizedBox(height: 12),
          SizedBox(
            height: 225,
            child: ListView.separated(
              scrollDirection: Axis.horizontal,
              itemCount: items.take(3).length,
              separatorBuilder: (_, __) => const SizedBox(width: 12),
              itemBuilder: (_, i) => _vendorCard(items[i], i),
            ),
          ),
          const SizedBox(height: 24),
          const Text('Quick Actions', style: TextStyle(fontSize: 22, fontWeight: FontWeight.w900, color: navy)),
          const SizedBox(height: 12),
          Row(children: [
            Expanded(child: _quickAction(Icons.storefront, 'All Vendors', () => setState(() => _tab = 1))),
            const SizedBox(width: 10),
            Expanded(child: _quickAction(Icons.receipt_long, 'My Orders', () => setState(() => _tab = 2))),
            const SizedBox(width: 10),
            Expanded(child: _quickAction(Icons.shopping_cart, 'Cart', () => Navigator.pushNamed(context, '/cart'))),
            const SizedBox(width: 10),
            Expanded(child: _quickAction(Icons.settings, 'Settings', () => setState(() => _tab = 3))),
          ]),
          const SizedBox(height: 26),
          const Text('Popular Today', style: TextStyle(fontSize: 22, fontWeight: FontWeight.w900, color: navy)),
          const SizedBox(height: 12),
          if (filtered.isEmpty)
            const _EmptyPanel(message: 'No meals are available right now.')
          else
            ...filtered.take(6).map((item) => _mealRow(context, item)),
        ],
      ),
    );
  }

  Widget _welcomeCard(String name) => Container(
        padding: const EdgeInsets.all(22),
        decoration: BoxDecoration(
          gradient: const LinearGradient(colors: [navy, Color(0xFF0B56A5)]),
          borderRadius: BorderRadius.circular(22),
        ),
        child: Row(children: [
          Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Text('Good Morning,', style: TextStyle(color: Colors.white.withValues(alpha: .9), fontSize: 18)),
            const SizedBox(height: 2),
            Text('${name.split(' ').first}!', style: const TextStyle(color: Colors.white, fontSize: 29, fontWeight: FontWeight.w900)),
            const SizedBox(height: 7),
            const Text('Great food fuels great minds.', style: TextStyle(color: Colors.white, fontSize: 14)),
          ])),
          Container(width: 76, height: 76, decoration: BoxDecoration(color: gold, borderRadius: BorderRadius.circular(22)), child: const Icon(Icons.restaurant, color: navy, size: 42)),
        ]),
      );

  Widget _vendors(BuildContext context, CafeteriaProvider provider) {
    final items = provider.allFoodItems.where((e) => e.isAvailable).toList();
    const names = ['Emmanuella Adu Dudaa', 'Lovelace Lartey Adams', 'KV Bakery', 'Joll of Rice & Chicken'];
    const cats = ['Local Dishes & Snacks', 'Rice Dishes & Drinks', 'Pastries & Bakes', 'Main Meals'];
    return ListView(
      padding: const EdgeInsets.fromLTRB(18, 18, 18, 28),
      children: [
        const Text('Vendors & Food', style: TextStyle(color: navy, fontSize: 28, fontWeight: FontWeight.w900)),
        const SizedBox(height: 6),
        const Text('Discover meals from trusted campus vendors.'),
        const SizedBox(height: 16),
        TextField(decoration: const InputDecoration(hintText: 'Search vendors...', prefixIcon: Icon(Icons.search))),
        const SizedBox(height: 18),
        for (int i = 0; i < names.length; i++)
          _vendorListCard(context, names[i], cats[i], items.isNotEmpty ? items[i % items.length] : null),
        const SizedBox(height: 12),
        Container(
          padding: const EdgeInsets.all(20),
          decoration: BoxDecoration(color: const Color(0xFFFFF3B8), borderRadius: BorderRadius.circular(18)),
          child: const Row(children: [Icon(Icons.restaurant, color: navy, size: 38), SizedBox(width: 14), Expanded(child: Text('Support our campus vendors — eat local, eat fresh!', style: TextStyle(color: navy, fontSize: 17, fontWeight: FontWeight.w800)))]),
        ),
      ],
    );
  }

  Widget _orders(BuildContext context, CafeteriaProvider provider) {
    final orders = provider.customerOrders;
    return ListView(
      padding: const EdgeInsets.fromLTRB(18, 18, 18, 28),
      children: [
        const Text('My Orders', style: TextStyle(color: navy, fontSize: 28, fontWeight: FontWeight.w900)),
        const SizedBox(height: 16),
        if (orders.isEmpty)
          _EmptyPanel(message: 'Your orders will appear here after checkout.', action: FilledButton(onPressed: () => setState(() => _tab = 0), child: const Text('Browse Food')))
        else
          ...orders.map((order) => Card(
                margin: const EdgeInsets.only(bottom: 14),
                child: InkWell(
                  borderRadius: BorderRadius.circular(18),
                  onTap: order.id == null ? null : () => Navigator.pushNamed(context, '/order-tracking', arguments: order.id),
                  child: Padding(
                    padding: const EdgeInsets.all(18),
                    child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                      Row(children: [
                        Expanded(child: Text('Order #ATU-${order.id ?? ''}', style: const TextStyle(color: navy, fontSize: 20, fontWeight: FontWeight.w900))),
                        _statusPill(order.status),
                      ]),
                      const SizedBox(height: 10),
                      Text(order.foodName, style: const TextStyle(fontSize: 17, fontWeight: FontWeight.w800)),
                      Text('Qty: ${order.quantity}  •  GH₵ ${order.totalPrice.toStringAsFixed(2)}'),
                      const SizedBox(height: 16),
                      _stepper(order.status),
                    ]),
                  ),
                ),
              )),
      ],
    );
  }

  Widget _profile(BuildContext context, CafeteriaProvider provider, String name) => ListView(
        padding: const EdgeInsets.all(18),
        children: [
          const Text('Profile', style: TextStyle(color: navy, fontSize: 28, fontWeight: FontWeight.w900)),
          const SizedBox(height: 18),
          Card(child: Padding(padding: const EdgeInsets.all(22), child: Column(children: [
            const CircleAvatar(radius: 42, backgroundColor: navy, child: Icon(Icons.person, color: Colors.white, size: 45)),
            const SizedBox(height: 12), Text(name, style: const TextStyle(color: navy, fontSize: 20, fontWeight: FontWeight.w900)),
            const SizedBox(height: 4), const Text('ATU Student'),
          ]))),
          const SizedBox(height: 14),
          _settingsTile(Icons.notifications_none, 'Notifications', 'Order updates and alerts', () {}),
          _settingsTile(Icons.lock_outline, 'Security', 'Protect your account', () {}),
          _settingsTile(Icons.help_outline, 'Help & Support', 'Get assistance', () {}),
          _settingsTile(Icons.logout, 'Sign Out', 'End your current session', () { provider.logOut(); Navigator.pushReplacementNamed(context, '/login'); }),
        ],
      );

  Widget _sectionTitle(String title, String action, VoidCallback onTap) => Row(children: [Expanded(child: Text(title, style: const TextStyle(color: navy, fontSize: 22, fontWeight: FontWeight.w900))), TextButton(onPressed: onTap, child: Text(action, style: const TextStyle(color: navy, fontWeight: FontWeight.w800)))]);

  Widget _vendorCard(FoodItem item, int index) {
    final names = ['Emmanuella Adu Dudaa', 'Lovelace Lartey Adams', 'KV Bakery'];
    final cats = ['Local Dishes & Snacks', 'Rice Dishes & Drinks', 'Pastries & Bakes'];
    return Container(width: 185, decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(18), border: Border.all(color: const Color(0xFFE1E8F0)), boxShadow: const [BoxShadow(color: Color(0x10000000), blurRadius: 8, offset: Offset(0, 3))]), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      _image(item, height: 105),
      Padding(padding: const EdgeInsets.fromLTRB(12, 10, 12, 10), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text(names[index], maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(color: navy, fontWeight: FontWeight.w900)), const SizedBox(height: 3), Text(cats[index], maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 12)), const SizedBox(height: 7), Row(children: [const Icon(Icons.star, color: gold, size: 18), const Text(' 4.6', style: TextStyle(fontWeight: FontWeight.w800)), const Spacer(), Container(padding: const EdgeInsets.symmetric(horizontal: 9, vertical: 5), decoration: BoxDecoration(color: green, borderRadius: BorderRadius.circular(16)), child: const Text('Open', style: TextStyle(color: Colors.white, fontWeight: FontWeight.w700)))])]))
    ]));
  }

  Widget _vendorListCard(BuildContext context, String name, String category, FoodItem? item) => Card(margin: const EdgeInsets.only(bottom: 14), child: InkWell(onTap: () {}, borderRadius: BorderRadius.circular(18), child: Padding(padding: const EdgeInsets.all(12), child: Row(children: [_image(item, width: 110, height: 100), const SizedBox(width: 14), Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text(name, style: const TextStyle(color: navy, fontSize: 18, fontWeight: FontWeight.w900)), const SizedBox(height: 5), Text(category, style: const TextStyle(color: navy)), const SizedBox(height: 12), const Row(children: [Icon(Icons.star, color: gold, size: 20), Text(' 4.6', style: TextStyle(fontWeight: FontWeight.w800)), SizedBox(width: 18), _OpenPill()])])), const Icon(Icons.chevron_right, color: navy, size: 30)])));

  Widget _mealRow(BuildContext context, FoodItem item) => Card(margin: const EdgeInsets.only(bottom: 10), child: Padding(padding: const EdgeInsets.all(10), child: Row(children: [_image(item, width: 92, height: 82), const SizedBox(width: 12), Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text(item.name, style: const TextStyle(color: navy, fontSize: 16, fontWeight: FontWeight.w900)), const SizedBox(height: 4), Text(item.description, maxLines: 2, overflow: TextOverflow.ellipsis), const SizedBox(height: 5), Text('GH₵ ${item.price.toStringAsFixed(2)}', style: const TextStyle(color: navy, fontSize: 16, fontWeight: FontWeight.w900))])), IconButton(onPressed: () { context.read<CartProvider>().add(item); ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Added to cart'))); }, icon: const Icon(Icons.add_circle, color: gold, size: 34))]));

  Widget _quickAction(IconData icon, String label, VoidCallback onTap) => InkWell(onTap: onTap, borderRadius: BorderRadius.circular(16), child: Container(height: 92, decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(16), border: Border.all(color: const Color(0xFFD8E2EE))), child: Column(mainAxisAlignment: MainAxisAlignment.center, children: [Icon(icon, color: navy, size: 27), const SizedBox(height: 7), Text(label, textAlign: TextAlign.center, style: const TextStyle(color: navy, fontSize: 11, fontWeight: FontWeight.w700))])));

  Widget _settingsTile(IconData icon, String title, String subtitle, VoidCallback onTap) => Card(margin: const EdgeInsets.only(bottom: 10), child: ListTile(leading: CircleAvatar(backgroundColor: navy.withValues(alpha: .1), child: Icon(icon, color: navy)), title: Text(title, style: const TextStyle(fontWeight: FontWeight.w800)), subtitle: Text(subtitle), trailing: const Icon(Icons.chevron_right), onTap: onTap));

  Widget _statusPill(String status) { final s = status.toUpperCase(); final color = s.contains('READY') ? green : s.contains('PREPAR') ? navy : gold; return Container(padding: const EdgeInsets.symmetric(horizontal: 11, vertical: 7), decoration: BoxDecoration(color: color.withValues(alpha: .15), borderRadius: BorderRadius.circular(20)), child: Text(status, style: TextStyle(color: color == gold ? navy : color, fontWeight: FontWeight.w800, fontSize: 11))); }

  Widget _stepper(String status) { final s = status.toUpperCase(); final active = s.contains('COMPLETED') || s.contains('DELIVERED') ? 3 : s.contains('READY') ? 2 : s.contains('PREPAR') ? 1 : 0; final labels = ['Received', 'Preparing', 'Ready', 'Picked Up']; return Row(children: [for (int i = 0; i < 4; i++) Expanded(child: Column(children: [Row(children: [if (i > 0) Expanded(child: Container(height: 3, color: i <= active ? navy : const Color(0xFFD6E0EA))), Container(width: 25, height: 25, decoration: BoxDecoration(shape: BoxShape.circle, color: i <= active ? (i == active ? gold : navy) : Colors.white, border: Border.all(color: i <= active ? navy : const Color(0xFF9DB2C9), width: 2)), child: i <= active ? Icon(i == active ? Icons.circle : Icons.check, size: 12, color: i == active ? navy : Colors.white) : null), if (i < 3) Expanded(child: Container(height: 3, color: i < active ? navy : const Color(0xFFD6E0EA)))]), const SizedBox(height: 5), Text(labels[i], style: const TextStyle(fontSize: 10, fontWeight: FontWeight.w700, color: navy))]))]); }

  Widget _image(FoodItem? item, {double? width, double? height}) { final url = item?.imageUrl.trim() ?? ''; final child = url.isEmpty ? const ColoredBox(color: Color(0xFFE9F0F7), child: Center(child: Icon(Icons.restaurant, color: navy, size: 34))) : Image.network(url, fit: BoxFit.cover, errorBuilder: (_, __, ___) => const ColoredBox(color: Color(0xFFE9F0F7), child: Center(child: Icon(Icons.restaurant, color: navy, size: 34)))); return ClipRRect(borderRadius: BorderRadius.circular(14), child: SizedBox(width: width, height: height, child: child)); }

  void _showProfile(BuildContext context, String name) => showModalBottomSheet(context: context, builder: (_) => SafeArea(child: Padding(padding: const EdgeInsets.all(24), child: Column(mainAxisSize: MainAxisSize.min, children: [const CircleAvatar(radius: 32, backgroundColor: navy, child: Icon(Icons.person, color: Colors.white, size: 34)), const SizedBox(height: 10), Text(name, style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w800)), const SizedBox(height: 6), const Text('ATU Student'), const SizedBox(height: 14)]))));
}

class _OpenPill extends StatelessWidget { const _OpenPill(); @override Widget build(BuildContext context) => Container(padding: const EdgeInsets.symmetric(horizontal: 9, vertical: 5), decoration: BoxDecoration(color: const Color(0xFF20B95A), borderRadius: BorderRadius.circular(16)), child: const Text('Open', style: TextStyle(color: Colors.white, fontWeight: FontWeight.w700, fontSize: 11))); }
class _EmptyPanel extends StatelessWidget { final String message; final Widget? action; const _EmptyPanel({required this.message, this.action}); @override Widget build(BuildContext context) => Card(child: Padding(padding: const EdgeInsets.all(28), child: Column(children: [const Icon(Icons.restaurant_menu, color: Color(0xFF063B82), size: 52), const SizedBox(height: 10), Text(message, textAlign: TextAlign.center), if (action != null) ...[const SizedBox(height: 14), action!]]))); }
