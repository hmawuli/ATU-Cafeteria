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
  String _category = 'All';

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

  String _imageFor(FoodItem item) {
    if (item.imageUrl.trim().isNotEmpty) return item.imageUrl;
    final category = item.category.toLowerCase();
    if (category.contains('drink') || category.contains('beverage')) {
      return 'https://images.unsplash.com/photo-1544145945-f90425340c7e?auto=format&fit=crop&w=900&q=82';
    }
    if (category.contains('snack') || category.contains('breakfast')) {
      return 'https://images.unsplash.com/photo-1509440159596-0249088772ff?auto=format&fit=crop&w=900&q=82';
    }
    if (category.contains('dessert')) {
      return 'https://images.unsplash.com/photo-1551024506-0bccd828d307?auto=format&fit=crop&w=900&q=82';
    }
    return 'https://images.unsplash.com/photo-1547592180-85f173990554?auto=format&fit=crop&w=900&q=82';
  }

  @override
  Widget build(BuildContext context) {
    final provider = context.watch<CafeteriaProvider>();
    final cart = context.watch<CartProvider>();
    final available =
        provider.allFoodItems.where((item) => item.isAvailable).toList();
    final categories = [
      'All',
      ...available
          .map((e) => e.category)
          .where((e) => e.trim().isNotEmpty)
          .toSet()
    ];
    final q = _query.trim().toLowerCase();
    final items = available.where((item) {
      final categoryMatch = _category == 'All' || item.category == _category;
      final searchMatch = q.isEmpty ||
          item.name.toLowerCase().contains(q) ||
          item.category.toLowerCase().contains(q);
      return categoryMatch && searchMatch;
    }).toList();

    return Scaffold(
      appBar: AppBar(
        title: const Text('ATU CAFETERIA • SELF-SERVICE'),
        actions: [
          Badge(
              isLabelVisible: cart.itemCount > 0,
              label: Text('${cart.itemCount}'),
              child: IconButton(
                  onPressed: () => Navigator.pushNamed(context, '/cart'),
                  icon: const Icon(Icons.shopping_cart_outlined, size: 30))),
          const SizedBox(width: 16),
        ],
      ),
      body: LayoutBuilder(builder: (context, constraints) {
        final compact = constraints.maxWidth < 850;
        return Column(children: [
          Container(
            width: double.infinity,
            margin: const EdgeInsets.fromLTRB(18, 18, 18, 10),
            padding: const EdgeInsets.all(22),
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(22),
              image: const DecorationImage(
                  image: NetworkImage(
                      'https://images.unsplash.com/photo-1515003197210-e0cd71810b5f?auto=format&fit=crop&w=1600&q=85'),
                  fit: BoxFit.cover),
              gradient: const LinearGradient(
                  colors: [Colors.black87, Colors.black38]),
            ),
            child: const Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('ORDER FRESH FOOD',
                      style: TextStyle(
                          color: Colors.white,
                          fontSize: 22,
                          fontWeight: FontWeight.w900)),
                  SizedBox(height: 5),
                  Text(
                      'Choose a meal, add it to your order and continue to payment.',
                      style: TextStyle(color: Colors.white70)),
                ]),
          ),
          Padding(
              padding: const EdgeInsets.fromLTRB(18, 4, 18, 8),
              child: Row(children: [
                Expanded(
                    child: TextField(
                        onChanged: (v) => setState(() => _query = v),
                        decoration: const InputDecoration(
                            hintText: 'Search meals, drinks and snacks...',
                            prefixIcon: Icon(Icons.search)))),
                const SizedBox(width: 10),
                DropdownButton<String>(
                    value: categories.contains(_category) ? _category : 'All',
                    items: categories
                        .map((c) => DropdownMenuItem(value: c, child: Text(c)))
                        .toList(),
                    onChanged: (v) => setState(() => _category = v ?? 'All')),
              ])),
          Expanded(
              child: compact
                  ? _MobileKiosk(items: items, imageFor: _imageFor)
                  : Row(children: [
                      Expanded(
                          child: items.isEmpty
                              ? const Center(
                                  child:
                                      Text('No meals are currently available.'))
                              : GridView.builder(
                                  padding:
                                      const EdgeInsets.fromLTRB(18, 8, 10, 24),
                                  gridDelegate:
                                      const SliverGridDelegateWithMaxCrossAxisExtent(
                                          maxCrossAxisExtent: 300,
                                          mainAxisExtent: 300,
                                          crossAxisSpacing: 16,
                                          mainAxisSpacing: 16),
                                  itemCount: items.length,
                                  itemBuilder: (_, i) => _MealCard(
                                      item: items[i],
                                      imageUrl: _imageFor(items[i])))),
                      SizedBox(width: 330, child: _OrderPanel(cart: cart)),
                    ])),
        ]);
      }),
    );
  }
}

class _MobileKiosk extends StatelessWidget {
  final List<FoodItem> items;
  final String Function(FoodItem) imageFor;
  const _MobileKiosk({required this.items, required this.imageFor});
  @override
  Widget build(BuildContext context) {
    final cart = context.watch<CartProvider>();
    return ListView(
        padding: const EdgeInsets.fromLTRB(18, 8, 18, 24),
        children: [
          ...items.map((item) => Padding(
              padding: const EdgeInsets.only(bottom: 12),
              child: _MealCard(item: item, imageUrl: imageFor(item)))),
          const SizedBox(height: 8),
          _OrderPanel(cart: cart),
        ]);
  }
}

class _MealCard extends StatelessWidget {
  final FoodItem item;
  final String imageUrl;
  const _MealCard({required this.item, required this.imageUrl});
  @override
  Widget build(BuildContext context) {
    final cart = context.read<CartProvider>();
    final scheme = Theme.of(context).colorScheme;
    return Card(
        clipBehavior: Clip.antiAlias,
        child: InkWell(
            onTap: () {
              cart.add(item);
              ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(content: Text('${item.name} added to order')));
            },
            child:
                Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              SizedBox(
                  width: double.infinity,
                  height: 150,
                  child: Image.network(imageUrl,
                      fit: BoxFit.cover,
                      errorBuilder: (_, __, ___) => Container(
                          color: scheme.primaryContainer,
                          child: Icon(Icons.restaurant_rounded,
                              size: 56, color: scheme.primary)))),
              Expanded(
                  child: Padding(
                      padding: const EdgeInsets.fromLTRB(15, 12, 15, 12),
                      child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                                item.category.isEmpty
                                    ? 'Special'
                                    : item.category,
                                style: TextStyle(
                                    color: scheme.primary,
                                    fontWeight: FontWeight.w800,
                                    fontSize: 11)),
                            const SizedBox(height: 4),
                            Text(item.name,
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                                style: const TextStyle(
                                    fontSize: 18, fontWeight: FontWeight.w900)),
                            const Spacer(),
                            Row(children: [
                              Text('GH₵ ${item.price.toStringAsFixed(2)}',
                                  style: TextStyle(
                                      color: scheme.primary,
                                      fontWeight: FontWeight.w900,
                                      fontSize: 16)),
                              const Spacer(),
                              const Icon(Icons.add_circle, size: 28)
                            ]),
                          ]))),
            ])));
  }
}

class _OrderPanel extends StatelessWidget {
  final CartProvider cart;

  const _OrderPanel({required this.cart});

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: const EdgeInsets.all(18),
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Row(
              children: [
                const Expanded(
                  child: Text(
                    'YOUR ORDER',
                    style: TextStyle(
                      fontSize: 20,
                      fontWeight: FontWeight.w900,
                    ),
                  ),
                ),
                Text(
                  '${cart.itemCount} items',
                  style: const TextStyle(color: Colors.grey),
                ),
              ],
            ),
            const Divider(height: 28),
            Expanded(
              child: cart.isEmpty
                  ? const Center(
                      child: Text(
                        'Tap a meal to add it here.',
                        textAlign: TextAlign.center,
                      ),
                    )
                  : ListView(
                      children: [
                        for (final line in cart.lines)
                          ListTile(
                            contentPadding: EdgeInsets.zero,
                            title: Text(
                              line.item.name,
                              style: const TextStyle(
                                fontWeight: FontWeight.w800,
                              ),
                            ),
                            subtitle: Text(
                              'Qty: ${line.quantity}',
                            ),
                            trailing: Text(
                              'GH₵ ${line.total.toStringAsFixed(2)}',
                              style: const TextStyle(
                                fontWeight: FontWeight.w800,
                              ),
                            ),
                          ),
                      ],
                    ),
            ),
            const Divider(height: 24),
            Row(
              children: [
                const Text(
                  'TOTAL',
                  style: TextStyle(fontWeight: FontWeight.w800),
                ),
                const Spacer(),
                Text(
                  'GH₵ ${cart.subtotal.toStringAsFixed(2)}',
                  style: const TextStyle(
                    fontSize: 20,
                    fontWeight: FontWeight.w900,
                  ),
                ),
              ],
            ),
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
    );
  }
}
