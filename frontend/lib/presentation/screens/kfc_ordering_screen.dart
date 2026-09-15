import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:atu_cafeteria/domain/models/models.dart';
import 'package:atu_cafeteria/presentation/providers/cart_provider.dart';
import 'package:atu_cafeteria/presentation/providers/cafeteria_provider.dart';
import 'package:atu_cafeteria/presentation/widgets/kfc_ordering_sections.dart';

class KfcOrderingScreen extends StatefulWidget {
  const KfcOrderingScreen({super.key});
  @override
  State<KfcOrderingScreen> createState() => _KfcOrderingScreenState();
}

class _KfcOrderingScreenState extends State<KfcOrderingScreen> {
  final _search = TextEditingController();
  String _query = '';
  String _category = 'All';

  @override
  void dispose() { _search.dispose(); super.dispose(); }

  @override
  Widget build(BuildContext context) {
    final cafe = context.watch<CafeteriaProvider>();
    final all = cafe.allFoodItems.where((i) => i.isAvailable).toList();
    final categories = <String>{'All', ...all.map((i) => i.category).where((v) => v.trim().isNotEmpty)}.toList();
    final q = _query.toLowerCase();
    final items = all.where((i) =>
      (_category == 'All' || i.category == _category) &&
      (q.isEmpty || i.name.toLowerCase().contains(q) || i.description.toLowerCase().contains(q) || i.category.toLowerCase().contains(q))
    ).toList();

    return Scaffold(
      appBar: AppBar(
        title: const Text('ATU Cafeteria'),
        actions: [
          Consumer<CartProvider>(builder: (_, cart, __) => Badge(
            isLabelVisible: cart.itemCount > 0,
            label: Text(cart.itemCount.toString()),
            child: IconButton(
              tooltip: 'Cart',
              icon: const Icon(Icons.shopping_bag_outlined),
              onPressed: () => Navigator.pushNamed(context, '/cart'),
            ),
          )),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: cafe.refreshAllData,
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            TextField(
              controller: _search,
              onChanged: (v) => setState(() => _query = v.trim()),
              decoration: InputDecoration(
                hintText: 'Search meals, snacks and drinks...',
                prefixIcon: const Icon(Icons.search_rounded),
                suffixIcon: _query.isEmpty ? null : IconButton(
                  onPressed: () { _search.clear(); setState(() => _query = ''); },
                  icon: const Icon(Icons.clear_rounded),
                ),
              ),
            ),
            const SizedBox(height: 10),
            SizedBox(
              height: 42,
              child: ListView.separated(
                scrollDirection: Axis.horizontal,
                itemCount: categories.length,
                separatorBuilder: (_, __) => const SizedBox(width: 8),
                itemBuilder: (_, i) => ChoiceChip(
                  label: Text(categories[i]),
                  selected: _category == categories[i],
                  onSelected: (_) => setState(() => _category = categories[i]),
                ),
              ),
            ),
            const SizedBox(height: 14),
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
