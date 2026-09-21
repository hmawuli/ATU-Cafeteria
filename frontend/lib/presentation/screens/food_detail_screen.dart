import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../domain/models/models.dart';
import '../providers/cart_provider.dart';

class FoodDetailScreen extends StatelessWidget {
  final FoodItem item;

  const FoodDetailScreen({super.key, required this.item});

  String _fallbackImage() {
    final category = item.category.toLowerCase();
    if (category.contains('drink') || category.contains('beverage')) {
      return 'https://images.unsplash.com/photo-1554866585-cd94860890b7?auto=format&fit=crop&w=1200&q=85';
    }
    if (category.contains('snack') || category.contains('breakfast')) {
      return 'https://images.unsplash.com/photo-1509440159596-0249088772ff?auto=format&fit=crop&w=1200&q=85';
    }
    if (category.contains('dessert')) {
      return 'https://images.unsplash.com/photo-1551024506-0bccd828d307?auto=format&fit=crop&w=1200&q=85';
    }
    return 'https://images.unsplash.com/photo-1540189549336-e6e99c3679fe?auto=format&fit=crop&w=1200&q=85';
  }

  void _addToCart(BuildContext context) {
    context.read<CartProvider>().add(item);
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text('${item.name} added to your cart.'),
        action: SnackBarAction(
          label: 'VIEW CART',
          onPressed: () => Navigator.pushNamed(context, '/cart'),
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final cart = context.watch<CartProvider>();
    final imageUrl =
        item.imageUrl.trim().isNotEmpty ? item.imageUrl : _fallbackImage();
    final scheme = Theme.of(context).colorScheme;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Food Details'),
        actions: [
          Badge(
            isLabelVisible: cart.itemCount > 0,
            label: Text('${cart.itemCount}'),
            child: IconButton(
              tooltip: 'View cart',
              onPressed: () => Navigator.pushNamed(context, '/cart'),
              icon: const Icon(Icons.shopping_cart_outlined),
            ),
          ),
          const SizedBox(width: 8),
        ],
      ),
      body: LayoutBuilder(
        builder: (context, constraints) {
          final wide = constraints.maxWidth >= 900;
          final image = ClipRRect(
            borderRadius: BorderRadius.circular(wide ? 28 : 0),
            child: AspectRatio(
              aspectRatio: wide ? 1.05 : 1.25,
              child: Image.network(
                imageUrl,
                fit: BoxFit.cover,
                loadingBuilder: (_, child, progress) => progress == null
                    ? child
                    : Container(
                        color: scheme.primaryContainer,
                        child: const Center(
                            child: CircularProgressIndicator(strokeWidth: 2))),
                errorBuilder: (_, __, ___) => Container(
                  color: scheme.primaryContainer,
                  child: Icon(Icons.restaurant_rounded,
                      size: 96, color: scheme.primary),
                ),
              ),
            ),
          );

          final details = Padding(
            padding: EdgeInsets.all(wide ? 34 : 24),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Container(
                  padding:
                      const EdgeInsets.symmetric(horizontal: 12, vertical: 7),
                  decoration: BoxDecoration(
                    color: scheme.primaryContainer,
                    borderRadius: BorderRadius.circular(20),
                  ),
                  child: Text(
                    item.category.isEmpty ? 'Today’s Special' : item.category,
                    style: TextStyle(
                        color: scheme.primary,
                        fontWeight: FontWeight.w800,
                        fontSize: 12),
                  ),
                ),
                const SizedBox(height: 18),
                Text(
                  item.name,
                  style: Theme.of(context)
                      .textTheme
                      .displaySmall
                      ?.copyWith(fontWeight: FontWeight.w900),
                ),
                const SizedBox(height: 12),
                Text(
                  'GH₵ ${item.price.toStringAsFixed(2)}',
                  style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                        color: scheme.primary,
                        fontWeight: FontWeight.w900,
                      ),
                ),
                const SizedBox(height: 20),
                Text(
                  item.description.isEmpty
                      ? 'Freshly prepared and available from the ATU cafeteria.'
                      : item.description,
                  style: Theme.of(context)
                      .textTheme
                      .bodyLarge
                      ?.copyWith(height: 1.55),
                ),
                const SizedBox(height: 28),
                Row(
                  children: [
                    Expanded(
                      child: FilledButton.icon(
                        onPressed:
                            item.isAvailable ? () => _addToCart(context) : null,
                        icon: const Icon(Icons.add_shopping_cart_outlined),
                        label: Text(
                            item.isAvailable ? 'Add to Cart' : 'Unavailable'),
                        style: FilledButton.styleFrom(
                            padding: const EdgeInsets.symmetric(vertical: 16)),
                      ),
                    ),
                    const SizedBox(width: 12),
                    OutlinedButton.icon(
                      onPressed: cart.isEmpty
                          ? null
                          : () => Navigator.pushNamed(context, '/cart'),
                      icon: const Icon(Icons.shopping_cart_outlined),
                      label: Text('${cart.itemCount}'),
                      style: OutlinedButton.styleFrom(
                          padding: const EdgeInsets.symmetric(
                              vertical: 16, horizontal: 18)),
                    ),
                  ],
                ),
                const SizedBox(height: 28),
                Card(
                  child: Padding(
                    padding: const EdgeInsets.all(18),
                    child: Row(
                      children: [
                        CircleAvatar(
                          backgroundColor: scheme.primaryContainer,
                          child: Icon(Icons.local_dining_outlined,
                              color: scheme.primary),
                        ),
                        const SizedBox(width: 14),
                        const Expanded(
                          child: Text(
                            'Campus dining made simple. Add your meal to the cart and continue to checkout when you are ready.',
                            style: TextStyle(height: 1.4),
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ],
            ),
          );

          return SingleChildScrollView(
            padding: EdgeInsets.all(wide ? 32 : 0),
            child: Center(
              child: ConstrainedBox(
                constraints: const BoxConstraints(maxWidth: 1180),
                child: wide
                    ? Card(
                        clipBehavior: Clip.antiAlias,
                        child: Row(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Expanded(child: image),
                            Expanded(child: details),
                          ],
                        ),
                      )
                    : Column(
                        children: [image, details],
                      ),
              ),
            ),
          );
        },
      ),
    );
  }
}
