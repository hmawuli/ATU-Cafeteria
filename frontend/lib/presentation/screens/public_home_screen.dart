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
    if (mounted) {
      setState(() {
        _loading = true;
        _error = null;
      });
    }

    try {
      final data = await _api.get('/food-items');
      final raw = data is Map && data['food_items'] is List
          ? data['food_items']
          : data is Map && data['data'] is List
              ? data['data']
              : data;

      if (raw is List) {
        _items = raw
            .whereType<Map>()
            .map((e) => FoodItem.fromJson(Map<String, dynamic>.from(e)))
            .where((e) => e.isAvailable)
            .toList();
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

  List<String> get _categories {
    final values = _items
        .map((item) => item.category.trim())
        .where((value) => value.isNotEmpty)
        .toSet()
        .toList()
      ..sort();
    return ['All', ...values];
  }

  List<FoodItem> _visibleItems() {
    final query = _search.text.trim().toLowerCase();
    return _items.where((item) {
      final matchesCategory =
          _category == 'All' || item.category == _category;
      final matchesQuery = query.isEmpty ||
          item.name.toLowerCase().contains(query) ||
          item.category.toLowerCase().contains(query) ||
          item.description.toLowerCase().contains(query);
      return matchesCategory && matchesQuery;
    }).toList();
  }

  @override
  Widget build(BuildContext context) {
    final cart = context.watch<CartProvider>();
    final visible = _visibleItems();

    return Scaffold(
      appBar: AppBar(
        titleSpacing: 20,
        title: const Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(Icons.restaurant_rounded),
            SizedBox(width: 10),
            Text('ATU Cafeteria'),
          ],
        ),
        actions: [
          Badge(
            isLabelVisible: cart.itemCount > 0,
            label: Text(cart.itemCount.toString()),
            child: IconButton(
              tooltip: 'View cart',
              onPressed: () => Navigator.pushNamed(context, '/cart'),
              icon: const Icon(Icons.shopping_cart_outlined),
            ),
          ),
          const SizedBox(width: 4),
          Padding(
            padding: const EdgeInsets.only(right: 14),
            child: TextButton.icon(
              onPressed: () => Navigator.pushNamed(context, '/login'),
              icon: const Icon(Icons.person_outline),
              label: const Text('Sign in'),
            ),
          ),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: _loadMenu,
        child: LayoutBuilder(
          builder: (context, constraints) {
            final wide = constraints.maxWidth >= 900;
            final horizontal = constraints.maxWidth >= 1200 ? 48.0 : 20.0;

            return ListView(
              padding: EdgeInsets.fromLTRB(horizontal, 24, horizontal, 48),
              children: [
                Center(
                  child: ConstrainedBox(
                    constraints: const BoxConstraints(maxWidth: 1180),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        _HeroCard(
                          onBrowse: () {
                            final target = _menuKey.currentContext;
                            if (target != null) {
                              Scrollable.ensureVisible(
                                target,
                                duration: const Duration(milliseconds: 350),
                              );
                            }
                          },
                          onCart: () => Navigator.pushNamed(context, '/cart'),
                        ),
                        const SizedBox(height: 22),
                        TextField(
                          controller: _search,
                          onChanged: (_) => setState(() {}),
                          decoration: InputDecoration(
                            hintText: 'Search meals, categories...',
                            prefixIcon: const Icon(Icons.search),
                            suffixIcon: _search.text.isEmpty
                                ? null
                                : IconButton(
                                    tooltip: 'Clear search',
                                    onPressed: () {
                                      _search.clear();
                                      setState(() {});
                                    },
                                    icon: const Icon(Icons.clear),
                                  ),
                          ),
                        ),
                        const SizedBox(height: 14),
                        _CategoryBar(
                          categories: _categories,
                          selected: _category,
                          onSelected: (value) =>
                              setState(() => _category = value),
                        ),
                        const SizedBox(height: 26),
                        Row(
                          key: _menuKey,
                          crossAxisAlignment: CrossAxisAlignment.end,
                          children: [
                            Expanded(
                              child: Text(
                                'Today’s menu',
                                style: Theme.of(context)
                                    .textTheme
                                    .headlineSmall
                                    ?.copyWith(fontWeight: FontWeight.w900),
                              ),
                            ),
                            if (!_loading)
                              Text(
                                visible.length.toString() +
                                    (visible.length == 1 ? ' meal' : ' meals'),
                              ),
                          ],
                        ),
                        const SizedBox(height: 12),
                        if (_loading)
                          const Padding(
                            padding: EdgeInsets.all(48),
                            child: Center(
                              child: CircularProgressIndicator(),
                            ),
                          )
                        else if (_error != null)
                          _ErrorCard(
                            message: _error!,
                            onRetry: _loadMenu,
                          )
                        else if (visible.isEmpty)
                          const _EmptyMenu()
                        else
                          GridView.builder(
                            shrinkWrap: true,
                            physics: const NeverScrollableScrollPhysics(),
                            itemCount: visible.length,
                            gridDelegate:
                                SliverGridDelegateWithFixedCrossAxisCount(
                              crossAxisCount: wide ? 3 : 1,
                              crossAxisSpacing: 16,
                              mainAxisSpacing: 16,
                              mainAxisExtent: wide ? 330 : 235,
                            ),
                            itemBuilder: (_, index) => _MealCard(
                              item: visible[index],
                              onAdd: () => _add(visible[index]),
                            ),
                          ),
                        const SizedBox(height: 24),
                        Card(
                          child: Padding(
                            padding: const EdgeInsets.all(20),
                            child: Row(
                              children: [
                                CircleAvatar(
                                  radius: 24,
                                  backgroundColor: Theme.of(context)
                                      .colorScheme
                                      .primaryContainer,
                                  child: Icon(
                                    Icons.lock_outline,
                                    color: Theme.of(context)
                                        .colorScheme
                                        .onPrimaryContainer,
                                  ),
                                ),
                                const SizedBox(width: 16),
                                const Expanded(
                                  child: Column(
                                    crossAxisAlignment:
                                        CrossAxisAlignment.start,
                                    children: [
                                      Text(
                                        'Ready to order?',
                                        style: TextStyle(
                                            fontWeight: FontWeight.w800),
                                      ),
                                      SizedBox(height: 4),
                                      Text(
                                        'You can browse and build your cart first. '
                                        'Sign in is required only when you checkout.',
                                      ),
                                    ],
                                  ),
                                ),
                                if (wide)
                                  TextButton(
                                    onPressed: () =>
                                        Navigator.pushNamed(context, '/login'),
                                    child: const Text('Sign in'),
                                  ),
                              ],
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ],
            );
          },
        ),
      ),
    );
  }
}

class _HeroCard extends StatelessWidget {
  final VoidCallback onBrowse;
  final VoidCallback onCart;

  const _HeroCard({
    required this.onBrowse,
    required this.onCart,
  });

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;

    return Card(
      clipBehavior: Clip.antiAlias,
      child: Container(
        padding: const EdgeInsets.all(28),
        decoration: BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: [
              scheme.primaryContainer,
              scheme.surface,
            ],
          ),
        ),
        child: LayoutBuilder(
          builder: (context, constraints) {
            final compact = constraints.maxWidth < 650;
            final content = Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Container(
                  padding:
                      const EdgeInsets.symmetric(horizontal: 12, vertical: 7),
                  decoration: BoxDecoration(
                    color: scheme.surface.withValues(alpha: 0.75),
                    borderRadius: BorderRadius.circular(30),
                  ),
                  child: const Text(
                    'ATU • CAMPUS DINING',
                    style: TextStyle(
                      fontWeight: FontWeight.w800,
                      letterSpacing: 0.8,
                      fontSize: 12,
                    ),
                  ),
                ),
                const SizedBox(height: 16),
                Text(
                  'Your digital cafeteria',
                  style: Theme.of(context)
                      .textTheme
                      .displaySmall
                      ?.copyWith(fontWeight: FontWeight.w900),
                ),
                const SizedBox(height: 10),
                ConstrainedBox(
                  constraints: const BoxConstraints(maxWidth: 650),
                  child: Text(
                    'Browse today’s meals, build your cart and order when you are ready. '
                    'No account is needed just to explore the menu.',
                    style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                          height: 1.45,
                        ),
                  ),
                ),
                const SizedBox(height: 20),
                Wrap(
                  spacing: 10,
                  runSpacing: 10,
                  children: [
                    FilledButton.icon(
                      onPressed: onBrowse,
                      icon: const Icon(Icons.restaurant_menu),
                      label: const Text('Browse meals'),
                    ),
                    OutlinedButton.icon(
                      onPressed: onCart,
                      icon: const Icon(Icons.shopping_cart_outlined),
                      label: const Text('View cart'),
                    ),
                  ],
                ),
              ],
            );

            if (compact) return content;

            return Row(
              children: [
                Expanded(child: content),
                const SizedBox(width: 24),
                Container(
                  width: 150,
                  height: 150,
                  decoration: BoxDecoration(
                    color: scheme.surface.withValues(alpha: 0.7),
                    shape: BoxShape.circle,
                  ),
                  child: Icon(
                    Icons.restaurant_rounded,
                    size: 72,
                    color: scheme.primary,
                  ),
                ),
              ],
            );
          },
        ),
      ),
    );
  }
}

class _CategoryBar extends StatelessWidget {
  final List<String> categories;
  final String selected;
  final ValueChanged<String> onSelected;

  const _CategoryBar({
    required this.categories,
    required this.selected,
    required this.onSelected,
  });

  @override
  Widget build(BuildContext context) {
    return SingleChildScrollView(
      scrollDirection: Axis.horizontal,
      child: Row(
        children: [
          for (final category in categories) ...[
            ChoiceChip(
              selected: category == selected,
              label: Text(category),
              onSelected: (_) => onSelected(category),
            ),
            const SizedBox(width: 8),
          ],
        ],
      ),
    );
  }
}

class _MealCard extends StatelessWidget {
  final FoodItem item;
  final VoidCallback onAdd;

  const _MealCard({
    required this.item,
    required this.onAdd,
  });

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final hasImage = item.imageUrl.trim().isNotEmpty;

    return Card(
      clipBehavior: Clip.antiAlias,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            height: 128,
            width: double.infinity,
            child: hasImage
                ? Image.network(
                    item.imageUrl,
                    fit: BoxFit.cover,
                    errorBuilder: (_, __, ___) => _ImagePlaceholder(
                      color: scheme.primaryContainer,
                    ),
                  )
                : _ImagePlaceholder(
                    color: scheme.primaryContainer,
                  ),
          ),
          Expanded(
            child: Padding(
              padding: const EdgeInsets.fromLTRB(16, 14, 16, 14),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    item.category.isEmpty ? 'Today’s special' : item.category,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: TextStyle(
                      color: scheme.primary,
                      fontWeight: FontWeight.w700,
                      fontSize: 12,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    item.name,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(
                      fontWeight: FontWeight.w900,
                      fontSize: 17,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Expanded(
                    child: Text(
                      item.description.isEmpty
                          ? 'Freshly prepared and available today.'
                          : item.description,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: Theme.of(context).textTheme.bodySmall?.copyWith(
                            height: 1.35,
                          ),
                    ),
                  ),
                  Row(
                    children: [
                      Text(
                        'GH₵ ${item.price.toStringAsFixed(2)}',
                        style: TextStyle(
                          fontSize: 17,
                          fontWeight: FontWeight.w900,
                          color: scheme.primary,
                        ),
                      ),
                      const Spacer(),
                      FilledButton.icon(
                        onPressed: onAdd,
                        icon: const Icon(Icons.add, size: 18),
                        label: const Text('Add'),
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _ImagePlaceholder extends StatelessWidget {
  final Color color;

  const _ImagePlaceholder({required this.color});

  @override
  Widget build(BuildContext context) {
    return Container(
      color: color,
      child: Center(
        child: Icon(
          Icons.restaurant_rounded,
          size: 48,
          color: Theme.of(context).colorScheme.primary,
        ),
      ),
    );
  }
}

class _ErrorCard extends StatelessWidget {
  final String message;
  final VoidCallback onRetry;

  const _ErrorCard({
    required this.message,
    required this.onRetry,
  });

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(28),
        child: Column(
          children: [
            const Icon(Icons.cloud_off, size: 44),
            const SizedBox(height: 10),
            Text(message, textAlign: TextAlign.center),
            const SizedBox(height: 14),
            FilledButton.icon(
              onPressed: onRetry,
              icon: const Icon(Icons.refresh),
              label: const Text('Try again'),
            ),
          ],
        ),
      ),
    );
  }
}

class _EmptyMenu extends StatelessWidget {
  const _EmptyMenu();

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(36),
        child: Column(
          children: [
            const Icon(Icons.search_off_rounded, size: 48),
            const SizedBox(height: 10),
            Text(
              'No available meals found',
              style: Theme.of(context).textTheme.titleMedium?.copyWith(
                    fontWeight: FontWeight.w800,
                  ),
            ),
            const SizedBox(height: 5),
            const Text(
              'Try another search or category. Pull down to refresh the menu.',
              textAlign: TextAlign.center,
            ),
          ],
        ),
      ),
    );
  }
}
