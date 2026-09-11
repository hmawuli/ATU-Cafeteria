import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/cart_provider.dart';

class CartScreen extends StatelessWidget {
  const CartScreen({super.key});
  @override
  Widget build(BuildContext context) {
    final cart = context.watch<CartProvider>();
    return Scaffold(
      appBar: AppBar(title: const Text('Your Cart')),
      body: cart.isEmpty ? const Center(child: Column(mainAxisSize: MainAxisSize.min, children: [
        Icon(Icons.shopping_cart_outlined, size: 64), SizedBox(height: 12),
        Text('Your cart is empty', style: TextStyle(fontSize: 20, fontWeight: FontWeight.w800)),
        SizedBox(height: 6), Text('Add meals from the menu to get started.'),
      ])) : Column(children: [
        Expanded(child: ListView.separated(
          padding: const EdgeInsets.all(16), itemCount: cart.lines.length,
          separatorBuilder: (_, __) => const SizedBox(height: 10),
          itemBuilder: (_, i) {
            final line = cart.lines[i];
            return Card(child: ListTile(
              leading: const CircleAvatar(child: Icon(Icons.restaurant_rounded)),
              title: Text(line.item.name, style: const TextStyle(fontWeight: FontWeight.w800)),
              subtitle: Text('GH₵ ${line.item.price.toStringAsFixed(2)} each'),
              trailing: SizedBox(width: 130, child: Row(mainAxisAlignment: MainAxisAlignment.end, children: [
                IconButton(onPressed: () => cart.remove(line.item.id!), icon: const Icon(Icons.remove_circle_outline)),
                Text('${line.quantity}', style: const TextStyle(fontWeight: FontWeight.w800)),
                IconButton(onPressed: () => cart.add(line.item), icon: const Icon(Icons.add_circle_outline)),
              ])),
            );
          },
        )),
        SafeArea(child: Card(margin: const EdgeInsets.all(16), child: Padding(padding: const EdgeInsets.all(18), child: Column(children: [
          Row(children: [const Text('Subtotal'), const Spacer(), Text('GH₵ ${cart.subtotal.toStringAsFixed(2)}', style: const TextStyle(fontWeight: FontWeight.w900))]),
          const SizedBox(height: 14),
          SizedBox(width: double.infinity, child: FilledButton.icon(
            onPressed: () => Navigator.pushNamed(context, '/checkout'),
            icon: const Icon(Icons.lock_outline), label: const Text('Proceed to Checkout'),
          )),
        ]))),
      ]),
    );
  }
}