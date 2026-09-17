import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:atu_cafeteria/presentation/providers/cafeteria_provider.dart';
import 'package:atu_cafeteria/presentation/providers/cart_provider.dart';
import 'package:atu_cafeteria/presentation/widgets/atu_ui.dart';

class ReferenceStudentHomeScreen extends StatelessWidget {
  const ReferenceStudentHomeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final provider = context.watch<CafeteriaProvider>();
    final foods = provider.allFoodItems.where((f) => f.isAvailable).take(8).toList();
    return Scaffold(
      backgroundColor: AtuUi.surface,
      body: SafeArea(
        child: Column(
          children: [
            AtuUi.brandHeader(actions: [
              Consumer<CartProvider>(
                builder: (_, cart, __) => Badge(
                  isLabelVisible: cart.itemCount > 0,
                  label: Text('${cart.itemCount}'),
                  child: IconButton(
                    color: Colors.white,
                    onPressed: () => Navigator.pushNamed(context, '/cart'),
                    icon: const Icon(Icons.shopping_cart_outlined),
                  ),
                ),
              ),
              IconButton(
                color: Colors.white,
                onPressed: () => Navigator.pushNamed(context, '/login'),
                icon: const Icon(Icons.account_circle_outlined),
              ),
            ]),
            Expanded(
              child: ListView(
                children: [
                  Container(
                    margin: const EdgeInsets.all(16),
                    padding: const EdgeInsets.all(20),
                    decoration: BoxDecoration(color: AtuUi.navy, borderRadius: BorderRadius.circular(18)),
                    child: const Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                      Text('Good Morning, Student! 👋', style: TextStyle(color: Colors.white, fontSize: 25, fontWeight: FontWeight.w900)),
                      SizedBox(height: 6),
                      Text('Great food fuels great minds.', style: TextStyle(color: Colors.white70, fontSize: 14)),
                    ]),
                  ),
                  const AtuSearchBar(hint: 'Search for food, vendors or meals...'),
                  AtuUi.sectionTitle('Featured Vendors'),
                  SizedBox(
                    height: 175,
                    child: ListView.separated(
                      padding: const EdgeInsets.symmetric(horizontal: 16),
                      scrollDirection: Axis.horizontal,
                      itemCount: foods.take(3).length,
                      separatorBuilder: (_, __) => const SizedBox(width: 12),
                      itemBuilder: (_, i) {
                        final food = foods[i];
                        return Container(
                          width: 185,
                          padding: const EdgeInsets.all(10),
                          decoration: AtuUi.card(),
                          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                            Expanded(child: Container(decoration: BoxDecoration(color: const Color(0xFFEAF1FA), borderRadius: BorderRadius.circular(12)), child: const Center(child: Icon(Icons.restaurant, color: AtuUi.navy, size: 45)))),
                            const SizedBox(height: 7),
                            Text(food.name, maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(color: AtuUi.navy, fontWeight: FontWeight.w900)),
                            Text(food.category, maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 11)),
                            Row(children: [const Icon(Icons.star, color: AtuUi.gold, size: 16), const SizedBox(width: 4), const Text('4.7', style: TextStyle(fontWeight: FontWeight.w800)), const Spacer(), AtuUi.status('Open')]),
                          ]),
                        );
                      },
                    ),
                  ),
                  AtuUi.sectionTitle('Quick Actions'),
                  Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 16),
                    child: Row(children: [
                      Expanded(child: _quick(context, Icons.storefront, 'All Vendors', '/student')),
                      const SizedBox(width: 8),
                      Expanded(child: _quick(context, Icons.receipt_long, 'My Orders', '/student')),
                      const SizedBox(width: 8),
                      Expanded(child: _quick(context, Icons.shopping_cart, 'Cart', '/cart')),
                      const SizedBox(width: 8),
                      Expanded(child: _quick(context, Icons.settings, 'Settings', '/student')),
                    ]),
                  ),
                  AtuUi.sectionTitle('Popular Meals'),
                  ...foods.map((food) => Container(
                    margin: const EdgeInsets.fromLTRB(16, 0, 16, 10),
                    padding: const EdgeInsets.all(12),
                    decoration: AtuUi.card(),
                    child: Row(children: [
                      Container(width: 76, height: 70, decoration: BoxDecoration(color: const Color(0xFFEAF1FA), borderRadius: BorderRadius.circular(10)), child: const Icon(Icons.restaurant_menu, color: AtuUi.navy, size: 32)),
                      const SizedBox(width: 12),
                      Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text(food.name, style: const TextStyle(color: AtuUi.navy, fontSize: 16, fontWeight: FontWeight.w900)), Text(food.description, maxLines: 2, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 12)), Text('GH₵ ${food.price.toStringAsFixed(2)}', style: const TextStyle(color: AtuUi.navy, fontSize: 16, fontWeight: FontWeight.w900))])),
                      ElevatedButton(onPressed: () { context.read<CartProvider>().add(food); ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('${food.name} added to cart'))); }, style: ElevatedButton.styleFrom(backgroundColor: AtuUi.gold, foregroundColor: AtuUi.navy), child: const Text('+ Add')),
                    ]),
                  )),
                ],
              ),
            ),
          ],
        ),
      ),
      bottomNavigationBar: NavigationBar(
        selectedIndex: 0,
        onDestinationSelected: (i) {
          if (i == 2) Navigator.pushNamed(context, '/cart');
          if (i == 3) Navigator.pushNamed(context, '/login');
        },
        destinations: const [
          NavigationDestination(icon: Icon(Icons.home_outlined), label: 'Home'),
          NavigationDestination(icon: Icon(Icons.store_outlined), label: 'Vendors'),
          NavigationDestination(icon: Icon(Icons.shopping_cart_outlined), label: 'Cart'),
          NavigationDestination(icon: Icon(Icons.person_outline), label: 'Profile'),
        ],
      ),
    );
  }

  Widget _quick(BuildContext context, IconData icon, String label, String route) => InkWell(
    onTap: () => Navigator.pushNamed(context, route),
    child: Container(height: 78, padding: const EdgeInsets.all(8), decoration: AtuUi.card(), child: Column(mainAxisAlignment: MainAxisAlignment.center, children: [Icon(icon, color: AtuUi.navy), const SizedBox(height: 5), Text(label, textAlign: TextAlign.center, style: const TextStyle(color: AtuUi.navy, fontSize: 10, fontWeight: FontWeight.w800))])),
  );
}
