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
  List<FoodItem> _items = [];
  bool _loading = true;
  String? _error;

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
    setState(() { _loading = true; _error = null; });
    try {
      final data = await _api.get('/food-items');
      final raw = data is Map && data['food_items'] is List
          ? data['food_items']
          : data is Map && data['data'] is List
              ? data['data']
              : data;
      if (raw is List) {
        _items = raw.whereType<Map>().map((e) {
          return FoodItem.fromJson(Map<String, dynamic>.from(e));
        }).where((e) => e.isAvailable).toList();
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
    final query = _search.text.trim().toLowerCase();
    final visible = _items.where((item) {
      if (query.isEmpty) return true;
      return item.name.toLowerCase().contains(query) ||
          item.category.toLowerCase().contains(query) ||
          item.description.toLowerCase().contains(query);
    }).toList();

    return Scaffold(
      appBar: AppBar(
        title: const Text('ATU Cafeteria'),
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
          TextButton(
            onPressed: () => Navigator.pushNamed(context, '/login'),
            child: const Text('Sign in'),
          ),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: _loadMenu,
        child: ListView(
          padding: const EdgeInsets.fromLTRB(16, 20, 16, 32),
          children: [
            Card(
              child: Padding(
                padding: const EdgeInsets.all(22),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('Your digital cafeteria',
                      style: Theme.of(context).textTheme.headlineMedium?.copyWith(
                        fontWeight: FontWeight.w900)),
                    const SizedBox(height: 8),
                    const Text(
                      'Browse today’s meals, build your cart and order when you are ready. '
                      'Sign in is required only when you checkout.'),
                    const SizedBox(height: 16),
                    const Wrap(
                      spacing: 8, runSpacing: 8,
                      children: [
                        Chip(avatar: Icon(Icons.restaurant_menu, size: 18), label: Text('See meals')),
                        Chip(avatar: Icon(Icons.shopping_cart_outlined, size: 18), label: Text('Build your cart')),
                        Chip(avatar: Icon(Icons.flash_on_outlined, size: 18), label: Text('Order ahead')),
                      ],
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 16),
            TextField(
              controller: _search,
              onChanged: (_) => setState(() {}),
              decoration: InputDecoration(
                hintText: 'Search meals, categories...',
                prefixIcon: const Icon(Icons.search),
                suffixIcon: _search.text.isEmpty ? null : IconButton(
                  onPressed: () { _search.clear(); setState(() {}); },
                  icon: const Icon(Icons.clear),
                ),
              ),
            ),
            const SizedBox(height: 16),
            Row(children: [
              Text('Today’s menu',
                style: Theme.of(context).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w900)),
              const Spacer(),
              if (!_loading) Text('${visible.length} available'),
            ]),
            const SizedBox(height: 10),
            if (_loading)
              const Padding(padding: EdgeInsets.all(32), child: Center(child: CircularProgressIndicator()))
            else if (_error != null)
              Card(child: Padding(
                padding: const EdgeInsets.all(20),
                child: Column(children: [
                  const Icon(Icons.cloud_off, size: 42),
                  const SizedBox(height: 8),
                  Text(_error!, textAlign: TextAlign.center),
                  const SizedBox(height: 12),
                  FilledButton.icon(onPressed: _loadMenu, icon: const Icon(Icons.refresh), label: const Text('Try again')),
                ])))
            else if (visible.isEmpty)
              const Padding(padding: EdgeInsets.all(32), child: Center(child: Text('No available meals match your search.')))
            else
              ...visible.map((item) => Card(
                margin: const EdgeInsets.only(bottom: 10),
                child: ListTile(
                  contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                  leading: const CircleAvatar(child: Icon(Icons.restaurant_rounded)),
                  title: Text(item.name, style: const TextStyle(fontWeight: FontWeight.w800)),
                  subtitle: Text('${item.category}\n${item.description}', maxLines: 2, overflow: TextOverflow.ellipsis),
                  isThreeLine: true,
                  trailing: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Text('GH₵ ${item.price.toStringAsFixed(2)}', style: const TextStyle(fontWeight: FontWeight.w900)),
                      const SizedBox(height: 4),
                      FilledButton(onPressed: () => _add(item), child: const Text('Add')),
                    ],
                  ),
                ),
              )),
            const SizedBox(height: 10),
            Card(child: ListTile(
              leading: const Icon(Icons.lock_outline),
              title: const Text('Ready to order?', style: TextStyle(fontWeight: FontWeight.w800)),
              subtitle: const Text('Checkout requires a student account so your order, payment and rewards are linked to you.'),
              trailing: TextButton(onPressed: () => Navigator.pushNamed(context, '/login'), child: const Text('Sign in')),
            )),
          ],
        ),
      ),
    );
  }
}
