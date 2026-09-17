import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../core/network/api_client.dart';
import '../../domain/models/models.dart';
import '../providers/cart_provider.dart';

class PublicHomeScreen extends StatefulWidget {
  const PublicHomeScreen({super.key});
  @override
  State<PublicHomeScreen> createState() => _PublicHomeScreenState();
}

class _PublicHomeScreenState extends State<PublicHomeScreen> {
  final ApiClient _api = ApiClient();
  final TextEditingController _search = TextEditingController();
  final GlobalKey _menuKey = GlobalKey();
  List<FoodItem> _items = [];
  bool _loading = true;
  String? _error;
  String _category = 'All';

  @override
  void initState() {
    super.initState();
    _loadMenu();
  }

  @override
  void dispose() {
    _search.dispose();
    _api.close();
    super.dispose();
  }

  Future<void> _loadMenu() async {
    if (mounted) setState(() { _loading = true; _error = null; });
    try {
      final data = await _api.get('/food-items');
      final raw = data is Map && data['food_items'] is List
          ? data['food_items']
          : data is Map && data['data'] is List ? data['data'] : data;
      if (raw is List) {
        _items = raw.whereType<Map>().map((e) => FoodItem.fromJson(Map<String, dynamic>.from(e))).where((e) => e.isAvailable).toList();
      } else {
        _items = [];
      }
    } catch (_) {
      _error = 'We could not load today’s menu. Please try again.';
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  void _add(FoodItem item) {
    context.read<CartProvider>().add(item);
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(
      content: Text('${item.name} added to your cart.'),
      action: SnackBarAction(label: 'VIEW CART', onPressed: () => Navigator.pushNamed(context, '/cart')),
    ));
  }

  List<String> get _categories {
    final values = _items.map((item) => item.category.trim()).where((v) => v.isNotEmpty).toSet().toList()..sort();
    return ['All', ...values];
  }

  List<FoodItem> get _visibleItems {
    final query = _search.text.trim().toLowerCase();
    return _items.where((item) {
      final category = _category == 'All' || item.category == _category;
      final search = query.isEmpty || item.name.toLowerCase().contains(query) || item.category.toLowerCase().contains(query) || item.description.toLowerCase().contains(query);
      return category && search;
    }).toList();
  }

  String _imageFor(FoodItem item) {
    if (item.imageUrl.trim().isNotEmpty) return item.imageUrl;
    final category = item.category.toLowerCase();
    if (category.contains('drink') || category.contains('beverage')) return 'https://images.unsplash.com/photo-1544145945-f90425340c7e?auto=format&fit=crop&w=1000&q=85';
    if (category.contains('snack') || category.contains('breakfast')) return 'https://images.unsplash.com/photo-1509440159596-0249088772ff?auto=format&fit=crop&w=1000&q=85';
    if (category.contains('dessert')) return 'https://images.unsplash.com/photo-1551024506-0bccd828d307?auto=format&fit=crop&w=1000&q=85';
    return 'https://images.unsplash.com/photo-1547592180-85f173990554?auto=format&fit=crop&w=1000&q=85';
  }

  void _open(FoodItem item) => Navigator.pushNamed(context, '/food-detail', arguments: item);

  @override
  Widget build(BuildContext context) {
    final cart = context.watch<CartProvider>();
    final visible = _visibleItems;
    return Scaffold(
      appBar: AppBar(
        titleSpacing: 20,
        title: const Row(mainAxisSize: MainAxisSize.min, children: [Icon(Icons.restaurant_rounded), SizedBox(width: 10), Text('ATU Cafeteria')]),
        actions: [
          Badge(isLabelVisible: cart.itemCount > 0, label: Text('${cart.itemCount}'), child: IconButton(onPressed: () => Navigator.pushNamed(context, '/cart'), icon: const Icon(Icons.shopping_cart_outlined))),
          const SizedBox(width: 4),
          Padding(padding: const EdgeInsets.only(right: 14), child: TextButton.icon(onPressed: () => Navigator.pushNamed(context, '/login'), icon: const Icon(Icons.person_outline), label: const Text('Sign in'))),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: _loadMenu,
        child: LayoutBuilder(builder: (context, constraints) {
          final wide = constraints.maxWidth >= 900;
          final columns = constraints.maxWidth >= 1250 ? 4 : constraints.maxWidth >= 900 ? 3 : 1;
          return ListView(
            padding: EdgeInsets.fromLTRB(constraints.maxWidth >= 1200 ? 48 : 20, 24, constraints.maxWidth >= 1200 ? 48 : 20, 48),
            children: [
              Center(child: ConstrainedBox(constraints: const BoxConstraints(maxWidth: 1220), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                _Hero(
                  onBrowse: () {
                    final target = _menuKey.currentContext;
                    if (target != null) {
                      Scrollable.ensureVisible(target, duration: const Duration(milliseconds: 300), curve: Curves.easeInOut);
                    }
                  },
                  onCart: () => Navigator.pushNamed(context, '/cart'),
                ),
                const SizedBox(height: 24),
                TextField(controller: _search, onChanged: (_) => setState((){}), decoration: InputDecoration(hintText: 'Search meals, snacks and drinks...', prefixIcon: const Icon(Icons.search), suffixIcon: _search.text.isEmpty ? null : IconButton(onPressed: () { _search.clear(); setState(() {}); }, icon: const Icon(Icons.clear)))),
                const SizedBox(height: 14),
                SingleChildScrollView(scrollDirection: Axis.horizontal, child: Row(children: [for (final category in _categories) Padding(padding: const EdgeInsets.only(right: 8), child: ChoiceChip(selected: category == _category, label: Text(category), onSelected: (_) => setState(() => _category = category)))])),
                const SizedBox(height: 26),
                Row(
                  key: _menuKey,
                  children: [Expanded(child: Text('Today’s menu', style: Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w900))), if (!_loading) Text('${visible.length} ${visible.length == 1 ? 'meal' : 'meals'}')],
                ),
                const SizedBox(height: 14),
                if (_loading) const Padding(padding: EdgeInsets.all(48), child: Center(child: CircularProgressIndicator()))
                else if (_error != null) _ErrorCard(message: _error!, onRetry: _loadMenu)
                else if (visible.isEmpty) const _EmptyMenu()
                else GridView.builder(
                  shrinkWrap: true,
                  physics: const NeverScrollableScrollPhysics(),
                  itemCount: visible.length,
                  gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(crossAxisCount: columns, crossAxisSpacing: 16, mainAxisSpacing: 16, mainAxisExtent: wide ? 370 : 300),
                  itemBuilder: (_, index) => _MealCard(item: visible[index], imageUrl: _imageFor(visible[index]), onAdd: () => _add(visible[index]), onOpen: () => _open(visible[index])),
                ),
                const SizedBox(height: 24),
                Card(child: Padding(padding: const EdgeInsets.all(20), child: Row(children: [CircleAvatar(backgroundColor: Theme.of(context).colorScheme.primaryContainer, child: Icon(Icons.lock_outline, color: Theme.of(context).colorScheme.primary)), const SizedBox(width: 14), const Expanded(child: Text('Browse freely and build your cart. Sign in is required only when you are ready to checkout.', style: TextStyle(height: 1.4))), if (wide) TextButton(onPressed: () => Navigator.pushNamed(context, '/login'), child: const Text('Sign in'))]))),
              ])))
            ],
          );
        }),
      ),
    );
  }
}

class _Hero extends StatelessWidget {
  final VoidCallback onBrowse;
  final VoidCallback onCart;
  const _Hero({required this.onBrowse, required this.onCart});
  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Card(
      clipBehavior: Clip.antiAlias,
      child: Container(
        constraints: const BoxConstraints(minHeight: 280),
        decoration: const BoxDecoration(image: DecorationImage(image: NetworkImage('https://images.unsplash.com/photo-1515003197210-e0cd71810b5f?auto=format&fit=crop&w=1800&q=88'), fit: BoxFit.cover)),
        child: Container(
          padding: const EdgeInsets.all(32),
          decoration: BoxDecoration(gradient: LinearGradient(begin: Alignment.centerLeft, end: Alignment.centerRight, colors: [Colors.black.withValues(alpha: .78), Colors.black.withValues(alpha: .28), Colors.transparent])),
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, mainAxisAlignment: MainAxisAlignment.center, children: [
            Container(padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 7), decoration: BoxDecoration(color: scheme.primary, borderRadius: BorderRadius.circular(20)), child: const Text('ATU • CAMPUS DINING', style: TextStyle(color: Colors.white, fontWeight: FontWeight.w800, letterSpacing: .7, fontSize: 12))),
            const SizedBox(height: 14),
            const Text('Fresh food.\nMade for campus.', style: TextStyle(color: Colors.white, fontSize: 38, height: 1.02, fontWeight: FontWeight.w900)),
            const SizedBox(height: 12),
            const SizedBox(width: 560, child: Text('Discover today’s meals, choose your favourite and order from the ATU cafeteria.', style: TextStyle(color: Colors.white70, fontSize: 16, height: 1.4))),
            const SizedBox(height: 22),
            Wrap(spacing: 10, runSpacing: 10, children: [FilledButton.icon(onPressed: onBrowse, icon: const Icon(Icons.restaurant_menu), label: const Text('Browse meals')), OutlinedButton.icon(onPressed: onCart, icon: const Icon(Icons.shopping_cart_outlined, color: Colors.white), label: const Text('View cart', style: TextStyle(color: Colors.white)))]),
          ]),
        ),
      ),
    );
  }
}

class _MealCard extends StatelessWidget {
  final FoodItem item;
  final String imageUrl;
  final VoidCallback onAdd;
  final VoidCallback onOpen;
  const _MealCard({required this.item, required this.imageUrl, required this.onAdd, required this.onOpen});
  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Card(clipBehavior: Clip.antiAlias, child: InkWell(onTap: onOpen, child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      Stack(children: [
        SizedBox(width: double.infinity, height: 175, child: Image.network(imageUrl, fit: BoxFit.cover, errorBuilder: (_, __, ___) => Container(color: scheme.primaryContainer, child: Icon(Icons.restaurant_rounded, size: 56, color: scheme.primary)))),
        Positioned(top: 12, left: 12, child: Container(padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6), decoration: BoxDecoration(color: Colors.white.withValues(alpha: .92), borderRadius: BorderRadius.circular(20)), child: Text(item.category.isEmpty ? 'Special' : item.category, style: TextStyle(color: scheme.primary, fontWeight: FontWeight.w800, fontSize: 11)))),
      ]),
      Expanded(child: Padding(padding: const EdgeInsets.fromLTRB(16, 14, 16, 14), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Text(item.name, maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w900)),
        const SizedBox(height: 5),
        Expanded(child: Text(item.description.isEmpty ? 'Freshly prepared and available today.' : item.description, maxLines: 3, overflow: TextOverflow.ellipsis, style: Theme.of(context).textTheme.bodySmall?.copyWith(height: 1.35))),
        Row(children: [Text('GH₵ ${item.price.toStringAsFixed(2)}', style: TextStyle(fontSize: 17, fontWeight: FontWeight.w900, color: scheme.primary)), const Spacer(), FilledButton.icon(onPressed: onAdd, icon: const Icon(Icons.add, size: 18), label: const Text('Add'))]),
      ])))
    ])));
  }
}

class _ErrorCard extends StatelessWidget {
  final String message;
  final VoidCallback onRetry;
  const _ErrorCard({required this.message, required this.onRetry});
  @override
  Widget build(BuildContext context) => Card(child: Padding(padding: const EdgeInsets.all(28), child: Column(children: [const Icon(Icons.cloud_off, size: 44), const SizedBox(height: 10), Text(message, textAlign: TextAlign.center), const SizedBox(height: 14), FilledButton.icon(onPressed: onRetry, icon: const Icon(Icons.refresh), label: const Text('Try again'))])));
}

class _EmptyMenu extends StatelessWidget {
  const _EmptyMenu();
  @override
  Widget build(BuildContext context) => Card(child: Padding(padding: const EdgeInsets.all(36), child: Column(children: [const Icon(Icons.restaurant_menu, size: 50), const SizedBox(height: 10), Text('No available meals found', style: Theme.of(context).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w800)), const SizedBox(height: 5), const Text('Try another category or refresh the menu.', textAlign: TextAlign.center)])));
}
