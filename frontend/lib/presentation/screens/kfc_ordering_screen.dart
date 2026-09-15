import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:atu_cafeteria/domain/models/models.dart';
import 'package:atu_cafeteria/presentation/providers/cart_provider.dart';
import 'package:atu_cafeteria/presentation/providers/cafeteria_provider.dart';
import 'package:atu_cafeteria/presentation/widgets/kfc_ordering_sections.dart';

class KfcOrderingScreen extends StatelessWidget {
  const KfcOrderingScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final cafe = context.watch<CafeteriaProvider>();
    final items = cafe.allFoodItems.where((item) => item.isAvailable).toList();

    return Scaffold(
      appBar: AppBar(
        title: const Text('ATU Cafeteria'),
        actions: [
          Consumer<CartProvider>(
            builder: (_, cart, __) => Badge(
              isLabelVisible: cart.itemCount > 0,
              label: Text(cart.itemCount.toString()),
              child: IconButton(
                tooltip: 'Cart',
                icon: const Icon(Icons.shopping_bag_outlined),
                onPressed: () => Navigator.pushNamed(context, '/cart'),
              ),
            ),
          ),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: cafe.refreshAllData,
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            KfcOrderingSections(
              items: items,
              onAdd: (FoodItem item) {
                context.read<CartProvider>().add(item);
                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(content: Text(item.name + ' added to your order')),
                );
              },
            ),
            const SizedBox(height: 22),
            FilledButton.icon(
              onPressed: () => Navigator.pushNamed(context, '/cart'),
              icon: const Icon(Icons.shopping_cart_checkout),
              label: const Text('Review order'),
            ),
          ],
        ),
      ),
    );
  }
}
