import 'dart:async';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../domain/models/models.dart';
import '../providers/cafeteria_provider.dart';
import '../providers/cart_provider.dart';

class KioskScreen extends StatefulWidget {
  const KioskScreen({super.key});
  @override
  State<KioskScreen> createState() => _KioskScreenState();
}

class _KioskScreenState extends State<KioskScreen> {
  Timer? _refresh;
  String _query = '';

  @override
  void initState() {
    super.initState();
    _refresh = Timer.periodic(const Duration(seconds: 20), (_) {
      if (mounted) context.read<CafeteriaProvider>().refreshAllData();
    });
  }

  @override
  void dispose() {
    _refresh?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final provider = context.watch<CafeteriaProvider>();
    final cart = context.watch<CartProvider>();
    final items = provider.foodItems.where((item) {
      final q = _query.trim().toLowerCase();
      return q.isEmpty ||
          item.name.toLowerCase().contains(q) ||
          item.category.toLowerCase().contains(q);
    }).toList();

    return Scaffold(
      appBar: AppBar(
        title: const Text('ATU CAFETERIA • SELF-SERVICE'),
        actions: [
          Badge(
            isLabelVisible: cart.itemCount > 0,
            label: Text('${cart.itemCount}'),
            child: IconButton(
              icon: const Icon(Icons.shopping_cart_outlined, size: 30),
              tooltip: 'View order',
              onPressed: () => Navigator.pushNamed(context, '/cart'),
            ),
          ),
          const SizedBox(width: 16),
        ],
      ),
      body: Row(
        children: [
          Expanded(
            child: Column(
              children: [
                Padding(
                  padding: const EdgeInsets.fromLTRB(24, 20, 24, 12),
                  child: TextField(
                    onChanged: (v) => setState(() => _query = v),
                    decoration: const InputDecoration(
                      hintText: 'Search meals, drinks and snacks...',
                      prefixIcon: Icon(Icons.search),
                    ),
                  ),
                ),
                Expanded(
                  child: items.isEmpty
                      ? const Center(child: Text('No meals are currently available.'))
                      : GridView.builder(
                          padding: const EdgeInsets.all(24),
                          gridDelegate: const SliverGridDelegateWithMaxCrossAxisExtent(
                            maxCrossAxisExtent: 310,
                            mainAxisExtent: 230,
                            crossAxisSpacing: 18,
                            mainAxisSpacing: 18,
                          ),
                          itemCount: items.length,
                          itemBuilder: (_, i) => _MealCard(item: items[i]),
                        ),
                ),
              ],
            ),
          ),
          SizedBox(
            width: 330,
            child: Card(
              margin: const EdgeInsets.all(18),
              child: Padding(
                padding: const EdgeInsets.all(20),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    Text('YOUR ORDER',
                        style: Theme.of(context).textTheme.titleLarge?.copyWith(
                              fontWeight: FontWeight.w900,
                            )),
                    const Divider(height: 28),
                    Expanded(
                      child: cart.isEmpty
                          ? const Center(child: Text('Tap a meal to add it here.'))
                          : ListView(
                              children: cart.lines.map((line) => ListTile(
                                dense: true,
                                title: Text(line.item.name),
                                subtitle: Text('Qty: ${line.quantity}'),
                                trailing: Text(
                                  'GH₵ ${line.total.toStringAsFixed(2)}',
                                  style: const TextStyle(fontWeight: FontWeight.w800),
                                ),
                              )).toList(),
                            ),
                    ),
                    Text('TOTAL  GH₵ ${cart.subtotal.toStringAsFixed(2)}',
                        style: const TextStyle(fontSize: 19, fontWeight: FontWeight.w900)),
                    const SizedBox(height: 12),
                    FilledButton(
                      onPressed: cart.isEmpty
                          ? null
                          : () => Navigator.pushNamed(context, '/checkout'),
                      child: const Padding(
                        padding: EdgeInsets.symmetric(vertical: 14),
                        child: Text('PROCEED TO PAYMENT'),
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

class _MealCard extends StatelessWidget {
  final FoodItem item;
  const _MealCard({required this.item});

  @override
  Widget build(BuildContext context) {
    final cart = context.read<CartProvider>();
    return Card(
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: () => cart.add(item),
        child: Padding(
          padding: const EdgeInsets.all(18),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Expanded(
                child: Icon(Icons.restaurant_rounded,
                    size: 58,
                    color: Theme.of(context).colorScheme.primary),
              ),
              Text(item.name,
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w900)),
              const SizedBox(height: 4),
              Text(item.category),
              const SizedBox(height: 8),
              Row(
                children: [
                  Text('GH₵ ${item.price.toStringAsFixed(2)}',
                      style: const TextStyle(fontWeight: FontWeight.w900)),
                  const Spacer(),
                  const Icon(Icons.add_circle),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}
