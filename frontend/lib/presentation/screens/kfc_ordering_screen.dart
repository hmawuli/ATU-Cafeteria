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

  Future<void> _showMealDetails(FoodItem item) async {
    if (!item.isAvailable) return;
    int quantity = 1;
    final add = await showModalBottomSheet<bool>(
      context: context,
      isScrollControlled: true,
      showDragHandle: true,
      builder: (sheetContext) => StatefulBuilder(
        builder: (context, setSheetState) {
          final scheme = Theme.of(context).colorScheme;
          return SafeArea(
            child: Padding(
              padding: EdgeInsets.fromLTRB(20, 0, 20, 20 + MediaQuery.of(context).viewInsets.bottom),
              child: SingleChildScrollView(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    ClipRRect(
                      borderRadius: BorderRadius.circular(20),
                      child: SizedBox(
                        width: double.infinity,
                        height: 220,
                        child: item.imageUrl.trim().isEmpty
                            ? Container(color: scheme.primaryContainer, child: Icon(Icons.restaurant_rounded, size: 80, color: scheme.primary))
                            : Image.network(item.imageUrl, fit: BoxFit.cover,
                                errorBuilder: (_, __, ___) => Container(
                                  color: scheme.primaryContainer,
                                  child: Icon(Icons.restaurant_rounded, size: 80, color: scheme.primary),
                                )),
                      ),
                    ),
                    const SizedBox(height: 18),
                    Text(item.category, style: TextStyle(color: scheme.primary, fontWeight: FontWeight.w800)),
                    const SizedBox(height: 4),
                    Text(item.name, style: Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w900)),
                    const SizedBox(height: 8),
                    Text(item.description.isEmpty ? 'Freshly prepared and available for campus ordering.' : item.description,
                        style: Theme.of(context).textTheme.bodyLarge?.copyWith(height: 1.45)),
                    const SizedBox(height: 14),
                    Wrap(spacing: 8, runSpacing: 8, children: [
                      Chip(avatar: const Icon(Icons.local_fire_department, size: 18), label: Text('${item.calories} kcal')),
                      Chip(avatar: const Icon(Icons.info_outline, size: 18), label: Text('Allergens: ${item.allergens}')),
                    ]),
                    const SizedBox(height: 18),
                    Row(children: [
                      Text('GH₵ ${item.price.toStringAsFixed(2)}',
                          style: TextStyle(fontSize: 22, fontWeight: FontWeight.w900, color: scheme.primary)),
                      const Spacer(),
                      DecoratedBox(
                        decoration: BoxDecoration(border: Border.all(color: scheme.outlineVariant), borderRadius: BorderRadius.circular(14)),
                        child: Row(children: [
                          IconButton(onPressed: quantity > 1 ? () => setSheetState(() => quantity--) : null, icon: const Icon(Icons.remove_rounded)),
                          SizedBox(width: 30, child: Text('${quantity}', textAlign: TextAlign.center, style: const TextStyle(fontWeight: FontWeight.w900, fontSize: 17))),
                          IconButton(onPressed: () => setSheetState(() => quantity++), icon: const Icon(Icons.add_rounded)),
                        ]),
                      ),
                    ]),
                    const SizedBox(height: 18),
                    SizedBox(
                      width: double.infinity,
                      height: 52,
                      child: FilledButton.icon(
                        onPressed: () => Navigator.pop(sheetContext, true),
                        icon: const Icon(Icons.add_shopping_cart),
                        label: Text('Add ${quantity} ${quantity == 1 ? 'item' : 'items'} • GH₵ ${(item.price * quantity).toStringAsFixed(2)}'),
                      ),
                    ),
                  ],
                ),
              ),
            ),
          );
        },
      ),
    );
    if (add == true && mounted) {
      final cart = context.read<CartProvider>();
      for (var i = 0; i < quantity; i++) {
        cart.add(item);
      }
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('${item.name} added to your order.'),
          action: SnackBarAction(label: 'VIEW CART', onPressed: () => Navigator.pushNamed(context, '/cart')),
        ),
      );
    }
  }

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

    final cart = context.watch<CartProvider>();

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
      bottomNavigationBar: cart.isEmpty
          ? null
          : SafeArea(
              child: Padding(
                padding: const EdgeInsets.fromLTRB(12, 6, 12, 10),
                child: Material(
                  elevation: 8,
                  borderRadius: BorderRadius.circular(16),
                  color: Theme.of(context).colorScheme.primary,
                  child: InkWell(
                    borderRadius: BorderRadius.circular(16),
                    onTap: () => Navigator.pushNamed(context, '/cart'),
                    child: Padding(
                      padding: const EdgeInsets.symmetric(
                          horizontal: 16, vertical: 13),
                      child: Row(
                        children: [
                          Badge(
                            label: Text('${cart.itemCount}'),
                            child: const Icon(
                              Icons.shopping_bag_outlined,
                              color: Colors.white,
                            ),
                          ),
                          const SizedBox(width: 12),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              mainAxisSize: MainAxisSize.min,
                              children: [
                                const Text(
                                  'YOUR ORDER',
                                  style: TextStyle(
                                    color: Colors.white70,
                                    fontSize: 10,
                                    fontWeight: FontWeight.w800,
                                    letterSpacing: 0.8,
                                  ),
                                ),
                                Text(
                                  '${cart.itemCount} ${cart.itemCount == 1 ? 'item' : 'items'} • GH₵ ${cart.subtotal.toStringAsFixed(2)}',
                                  style: const TextStyle(
                                    color: Colors.white,
                                    fontSize: 15,
                                    fontWeight: FontWeight.w900,
                                  ),
                                ),
                              ],
                            ),
                          ),
                          const Text(
                            'VIEW CART',
                            style: TextStyle(
                              color: Colors.white,
                              fontWeight: FontWeight.w900,
                              fontSize: 12,
                            ),
                          ),
                          const SizedBox(width: 4),
                          const Icon(
                            Icons.arrow_forward_rounded,
                            color: Colors.white,
                            size: 20,
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
              ),
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
              onAdd: _showMealDetails,
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
