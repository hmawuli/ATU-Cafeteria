import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:atu_cafeteria/presentation/providers/cafeteria_provider.dart';
import 'package:atu_cafeteria/presentation/providers/cart_provider.dart';
import 'package:atu_cafeteria/presentation/widgets/atu_ui.dart';

/// Reference-style student landing page. Kept as a focused presentation layer
/// while the existing authenticated student dashboard continues to provide
/// the complete live workflow.
class ReferenceStudentHomeScreen extends StatelessWidget {
  const ReferenceStudentHomeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final provider = context.watch<CafeteriaProvider>();
    final foods = provider.allFoodItems.where((f) => f.isAvailable).take(8).toList();
    return Scaffold(
      backgroundColor: AtuUi.surface,
      body: SafeArea(child: Column(children: [
        AtuUi.brandHeader(actions: [
          Consumer<CartProvider>(builder: (_, cart, __) => Badge(isLabelVisible: cart.itemCount > 0, label: Text('${cart.itemCount}'), child: IconButton(color: Colors.white, onPressed: () => Navigator.pushNamed(context, '/cart'), icon: const Icon(Icons.shopping_cart_outlined)))),
          IconButton(color: Colors.white, onPressed: () => Navigator.pushNamed(context, '/login'), icon: const Icon(Icons.account_circle_outlined)),
        ]),
        Expanded(child: ListView(children: [
          Container(margin: const EdgeInsets.all(16), padding: const EdgeInsets.all(20), decoration: BoxDecoration(color: AtuUi.navy, borderRadius: BorderRadius.circular(18)), child: const Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text('Good Morning, Student! 👋', style: TextStyle(color: Colors.white, fontSize: 25, fontWeight: FontWeight.w900)), SizedBox(height: 6), Text('Great food fuels great minds.', style: TextStyle(color: Colors.white70, fontSize: 14))])),
          const AtuSearchBar(hint: 'Search for food, vendors or meals...'),
          AtuUi.sectionTitle('Featured Vendors', action: 'View All'),
          SizedBox(height: 175, child: ListView.separated(padding: const EdgeInsets.symmetric(horizontal: 16), scrollDirection: Axis.horizontal, itemCount: foods.take(3).length, separatorBuilder: (_, __) => const SizedBox(width: 12), itemBuilder: (_, i) { final f = foods[i]; return Container(width: 185, padding: const EdgeInsets.all(10), decoration: AtuUi.card(), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Expanded(child: Container(decoration: BoxDecoration(color: const Color(0xFFEAF1FA), borderRadius: BorderRadius.circular(12)), child: const Center(child: Icon(Icons.restaurant, color: AtuUi.navy, size: 45)))), const SizedBox(height: 7), Text(f.name, maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(color: AtuUi.navy, fontWeight: FontWeight.w900)), Text(f.category, maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 11)), Row(children: [const Icon(Icons.star, color: AtuUi.gold, size: 16), const SizedBox(width: 4), Text('4.7', style: const TextStyle(fontWeight: FontWeight.w800)), const Spacer(), AtuUi.status('Open')]) ])); })),
          AtuUi.sectionTitle('Quick Actions'),
          Padding(padding: const EdgeInsets.symmetric(horizontal: 16), child: Row(children: [Expanded(child: _quick(context, Icons.storefront, 'All Vendors', '/login')), const SizedBox(width: 8), Expanded(child: _quick(context, Icons.receipt_long, 'My Orders', '/student')), const SizedBox(width: 8), Expanded(child: _quick(context, Icons.shopping_cart, 'Cart', '/cart')), const SizedBox(width: 8), Expanded(child: _quick(context, Icons.settings, 'Settings', '/login'))])),
          AtuUi.sectionTitle('Popular Meals'),
          ...foods.map((f) => Container(margin: const EdgeInsets.fromLTRB(16, 0, 16, 10), padding: const EdgeInsets.all(12), decoration: AtuUi.card(), child: Row(children: [Container(width: 76, height: 70, decoration: BoxDecoration(color: const Color(0xFFEAF1FA), borderRadius: BorderRadius.circular(10)), child: const Icon(Icons.restaurant_menu, color: AtuUi.navy, size: 32)), const SizedBox(width: 12), Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text(f.name, style: const TextStyle(color: AtuUi.navy, fontSize: 16, fontWeight: FontWeight.w900)), Text(f.description, maxLines: 2, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 12)), Text('GH₵ ${f.price.toStringAsFixed(2)}', style: const TextStyle(color: AtuUi.navy, fontSize: 16, fontWeight: FontWeight.w900))])), ElevatedButton(onPressed: () { context.read<CartProvider>().add(f); ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('${f.name} added to cart'))); }, style: ElevatedButton.styleFrom(backgroundColor: AtuUi.gold, foregroundColor: AtuUi.navy), child: const Text('+ Add'))])),
        ])),
      ]),
      bottomNavigationBar: NavigationBar(selectedIndex: 0, onDestinationSelected: (i) { if (i == 2) Navigator.pushNamed(context, '/cart'); else if (i == 3) Navigator.pushNamed(context, '/login'); }, destinations: const [NavigationDestination(icon: Icon(Icons.home_outlined), label: 'Home'), NavigationDestination(icon: Icon(Icons.store_outlined), label: 'Vendors'), NavigationDestination(icon: Icon(Icons.shopping_cart_outlined), label: 'Cart'), NavigationDestination(icon: Icon(Icons.person_outline), label: 'Profile')]),
    );
  }
  Widget _quick(BuildContext context, IconData icon, String label, String route) => InkWell(onTap: () => Navigator.pushNamed(context, route), child: Container(height: 78, padding: const EdgeInsets.all(8), decoration: AtuUi.card(), child: Column(mainAxisAlignment: MainAxisAlignment.center, children: [Icon(icon, color: AtuUi.navy), const SizedBox(height: 5), Text(label, textAlign: TextAlign.center, style: const TextStyle(color: AtuUi.navy, fontSize: 10, fontWeight: FontWeight.w800))])));
}
