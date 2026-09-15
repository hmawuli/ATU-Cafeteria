import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/cart_provider.dart';

class CartScreen extends StatelessWidget {
  const CartScreen({super.key});

  Future<void> _confirmClear(BuildContext context, CartProvider cart) async {
    final clear = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Clear cart?'),
        content: const Text('All items will be removed from your current order.'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Keep items')),
          FilledButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('Clear cart')),
        ],
      ),
    );
    if (clear == true) cart.clear();
  }

  @override
  Widget build(BuildContext context) {
    final cart = context.watch<CartProvider>();
    return Scaffold(
      appBar: AppBar(
        title: const Text('Your Cart'),
        actions: [
          if (!cart.isEmpty)
            IconButton(
              tooltip: 'Clear cart',
              icon: const Icon(Icons.delete_sweep_outlined),
              onPressed: () => _confirmClear(context, cart),
            ),
        ],
      ),
      body: cart.isEmpty
          ? const Center(
              child: Column(mainAxisSize: MainAxisSize.min, children: [
                Icon(Icons.shopping_bag_outlined, size: 64),
                SizedBox(height: 12),
                Text('Your cart is empty',
                    style: TextStyle(fontSize: 20, fontWeight: FontWeight.w800)),
                SizedBox(height: 6),
                Text('Add meals from the menu to get started.'),
              ]),
            )
          : Column(
              children: [
                Expanded(
                  child: ListView.separated(
                    padding: const EdgeInsets.fromLTRB(16, 16, 16, 8),
                    itemCount: cart.lines.length,
                    separatorBuilder: (_, __) => const SizedBox(height: 10),
                    itemBuilder: (_, i) {
                      final line = cart.lines[i];
                      return Card(
                        clipBehavior: Clip.antiAlias,
                        child: Padding(
                          padding: const EdgeInsets.all(10),
                          child: Row(
                            children: [
                              SizedBox(
                                width: 76,
                                height: 76,
                                child: ClipRRect(
                                  borderRadius: BorderRadius.circular(12),
                                  child: line.item.imageUrl.trim().isEmpty
                                      ? Container(
                                          color: Theme.of(context).colorScheme.surfaceContainerHighest,
                                          child: const Icon(Icons.restaurant_rounded, size: 34),
                                        )
                                      : Image.network(
                                          line.item.imageUrl,
                                          fit: BoxFit.cover,
                                          errorBuilder: (_, __, ___) => Container(
                                            color: Theme.of(context).colorScheme.surfaceContainerHighest,
                                            child: const Icon(Icons.restaurant_rounded, size: 34),
                                          ),
                                        ),
                                ),
                              ),
                              const SizedBox(width: 12),
                              Expanded(
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Text(line.item.name,
                                        maxLines: 2,
                                        overflow: TextOverflow.ellipsis,
                                        style: const TextStyle(fontWeight: FontWeight.w800)),
                                    const SizedBox(height: 4),
                                    Text('GH₵ ${line.item.price.toStringAsFixed(2)} each'),
                                    const SizedBox(height: 6),
                                    Text('GH₵ ${line.total.toStringAsFixed(2)}',
                                        style: TextStyle(
                                          fontWeight: FontWeight.w900,
                                          color: Theme.of(context).colorScheme.primary,
                                        )),
                                  ],
                                ),
                              ),
                              const SizedBox(width: 8),
                              DecoratedBox(
                                decoration: BoxDecoration(
                                  border: Border.all(color: Theme.of(context).colorScheme.outlineVariant),
                                  borderRadius: BorderRadius.circular(14),
                                ),
                                child: Row(
                                  mainAxisSize: MainAxisSize.min,
                                  children: [
                                    IconButton(
                                      tooltip: 'Decrease quantity',
                                      visualDensity: VisualDensity.compact,
                                      onPressed: () => cart.remove(line.item.id!),
                                      icon: const Icon(Icons.remove_rounded),
                                    ),
                                    Text('${line.quantity}',
                                        style: const TextStyle(fontWeight: FontWeight.w900)),
                                    IconButton(
                                      tooltip: 'Increase quantity',
                                      visualDensity: VisualDensity.compact,
                                      onPressed: () => cart.add(line.item),
                                      icon: const Icon(Icons.add_rounded),
                                    ),
                                  ],
                                ),
                              ),
                            ],
                          ),
                        ),
                      );
                    },
                  ),
                ),
                SafeArea(
                  child: Card(
                    margin: const EdgeInsets.fromLTRB(16, 8, 16, 16),
                    child: Padding(
                      padding: const EdgeInsets.all(18),
                      child: Column(
                        children: [
                          Row(children: [
                            Text('${cart.itemCount} ${cart.itemCount == 1 ? 'item' : 'items'}'),
                            const Spacer(),
                            Text('GH₵ ${cart.subtotal.toStringAsFixed(2)}',
                                style: const TextStyle(fontWeight: FontWeight.w900)),
                          ]),
                          const SizedBox(height: 8),
                          const Row(children: [
                            Icon(Icons.lock_outline, size: 16),
                            SizedBox(width: 6),
                            Expanded(child: Text('Secure checkout • Pickup or scheduled collection')),
                          ]),
                          const SizedBox(height: 14),
                          SizedBox(
                            width: double.infinity,
                            child: FilledButton.icon(
                              onPressed: () => Navigator.pushNamed(context, '/checkout'),
                              icon: const Icon(Icons.shopping_cart_checkout),
                              label: const Text('Proceed to Checkout'),
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
              ],
            ),
    );
  }
}
