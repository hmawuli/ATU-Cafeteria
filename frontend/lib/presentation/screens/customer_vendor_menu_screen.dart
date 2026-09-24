import 'package:flutter/material.dart';
import 'package:atu_cafeteria/domain/models/models.dart';
import 'package:atu_cafeteria/presentation/providers/cafeteria_provider.dart';
import 'package:atu_cafeteria/presentation/providers/cart_provider.dart';
import 'package:provider/provider.dart';

/// Customer-facing restaurant menu.
///
/// This screen represents the menu of one restaurant/vendor and keeps the
/// customer inside a restaurant-specific ordering flow.
class CustomerVendorMenuScreen extends StatefulWidget {
  final User vendor;

  const CustomerVendorMenuScreen({
    super.key,
    required this.vendor,
  });

  @override
  State<CustomerVendorMenuScreen> createState() =>
      _CustomerVendorMenuScreenState();
}

class _CustomerVendorMenuScreenState extends State<CustomerVendorMenuScreen> {
  static const navy = Color(0xFF073B82);
  static const blue = Color(0xFF0D55B5);
  static const yellow = Color(0xFFFFC400);
  static const page = Color(0xFFF5F8FC);
  static const muted = Color(0xFF61738A);

  String _query = '';
  String _category = 'All';

  @override
  Widget build(BuildContext context) {
    final provider = context.watch<CafeteriaProvider>();

    final foods = provider.allFoodItems
        .where(
          (food) => food.vendorId == widget.vendor.id && food.isAvailable,
        )
        .toList();

    final categories = <String>{
      for (final food in foods)
        if (food.category.trim().isNotEmpty) food.category.trim(),
    }.toList();

    final query = _query.trim().toLowerCase();

    final filtered = foods.where((food) {
      final matchesCategory = _category == 'All' || food.category == _category;

      final matchesSearch = query.isEmpty ||
          '${food.name} ${food.category} ${food.description}'
              .toLowerCase()
              .contains(query);

      return matchesCategory && matchesSearch;
    }).toList();

    return Scaffold(
      backgroundColor: page,
      appBar: AppBar(
        backgroundColor: navy,
        foregroundColor: Colors.white,
        elevation: 0,
        title: const Text(
          'Restaurant Menu',
          style: TextStyle(fontWeight: FontWeight.w900),
        ),
        actions: [
          Consumer<CartProvider>(
            builder: (_, cart, __) => Badge(
              isLabelVisible: cart.itemCount > 0,
              label: Text('${cart.itemCount}'),
              child: IconButton(
                tooltip: 'Cart',
                onPressed: () => Navigator.pushNamed(context, '/cart'),
                icon: const Icon(Icons.shopping_cart_outlined),
              ),
            ),
          ),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: provider.refreshAllData,
        child: ListView(
          padding: const EdgeInsets.fromLTRB(16, 16, 16, 32),
          children: [
            _restaurantHeader(foods),
            const SizedBox(height: 18),
            _searchField(),
            const SizedBox(height: 14),
            if (categories.isNotEmpty) ...[
              SizedBox(
                height: 42,
                child: ListView.separated(
                  scrollDirection: Axis.horizontal,
                  itemCount: categories.length + 1,
                  separatorBuilder: (_, __) => const SizedBox(width: 8),
                  itemBuilder: (_, index) {
                    final category = index == 0 ? 'All' : categories[index - 1];

                    return ChoiceChip(
                      selected: _category == category,
                      label: Text(category),
                      avatar: const Icon(
                        Icons.restaurant_rounded,
                        size: 16,
                      ),
                      onSelected: (_) {
                        setState(() => _category = category);
                      },
                    );
                  },
                ),
              ),
              const SizedBox(height: 20),
            ],
            Row(
              children: [
                const Expanded(
                  child: Text(
                    'Menu',
                    style: TextStyle(
                      color: navy,
                      fontSize: 23,
                      fontWeight: FontWeight.w900,
                    ),
                  ),
                ),
                Text(
                  '${filtered.length} ${filtered.length == 1 ? 'item' : 'items'}',
                  style: const TextStyle(
                    color: muted,
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 10),
            if (filtered.isEmpty) _emptyState() else ...filtered.map(_foodCard),
          ],
        ),
      ),
    );
  }

  Widget _restaurantHeader(List<FoodItem> foods) {
    final firstImage = foods.isNotEmpty ? foods.first.imageUrl : '';

    return Container(
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(20),
        boxShadow: [
          BoxShadow(
            color: navy.withValues(alpha: .08),
            blurRadius: 18,
            offset: const Offset(0, 7),
          ),
        ],
      ),
      clipBehavior: Clip.antiAlias,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            height: 150,
            width: double.infinity,
            child: _foodImage(firstImage),
          ),
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 14, 16, 16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Expanded(
                      child: Text(
                        widget.vendor.fullName.isEmpty
                            ? 'Restaurant'
                            : widget.vendor.fullName,
                        style: const TextStyle(
                          color: navy,
                          fontSize: 21,
                          fontWeight: FontWeight.w900,
                        ),
                      ),
                    ),
                    _openPill(widget.vendor.isOpen),
                  ],
                ),
                const SizedBox(height: 7),
                const Row(
                  children: [
                    Icon(
                      Icons.star_rounded,
                      color: yellow,
                      size: 19,
                    ),
                    SizedBox(width: 4),
                    Text(
                      'Restaurant rating',
                      style: TextStyle(
                        color: muted,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 8),
                Text(
                  widget.vendor.info.isEmpty
                      ? 'Fresh meals and food & drinks.'
                      : widget.vendor.info,
                  style: const TextStyle(
                    color: muted,
                    height: 1.4,
                  ),
                ),
                const SizedBox(height: 12),
                Row(
                  children: [
                    _infoChip(
                      Icons.restaurant_menu_rounded,
                      '${foods.length} menu items',
                    ),
                    const SizedBox(width: 8),
                    _infoChip(
                      widget.vendor.isOpen
                          ? Icons.check_circle_outline_rounded
                          : Icons.schedule_rounded,
                      widget.vendor.isOpen ? 'Open now' : 'Currently closed',
                    ),
                  ],
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _searchField() {
    return TextField(
      onChanged: (value) => setState(() => _query = value),
      decoration: InputDecoration(
        hintText: 'Search this restaurant...',
        prefixIcon: const Icon(Icons.search_rounded),
        suffixIcon: _query.isEmpty
            ? null
            : IconButton(
                tooltip: 'Clear search',
                onPressed: () => setState(() => _query = ''),
                icon: const Icon(Icons.clear_rounded),
              ),
        filled: true,
        fillColor: Colors.white,
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(14),
          borderSide: BorderSide.none,
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(14),
          borderSide: BorderSide.none,
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(14),
          borderSide: const BorderSide(color: blue, width: 1.3),
        ),
      ),
    );
  }

  Widget _foodCard(FoodItem food) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: Material(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        clipBehavior: Clip.antiAlias,
        child: InkWell(
          onTap: () => Navigator.pushNamed(
            context,
            '/food-detail',
            arguments: food,
          ),
          child: Padding(
            padding: const EdgeInsets.all(10),
            child: Row(
              children: [
                ClipRRect(
                  borderRadius: BorderRadius.circular(12),
                  child: SizedBox(
                    width: 92,
                    height: 92,
                    child: _foodImage(food.imageUrl),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        food.name,
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(
                          color: navy,
                          fontSize: 16,
                          fontWeight: FontWeight.w900,
                        ),
                      ),
                      const SizedBox(height: 5),
                      if (food.category.trim().isNotEmpty)
                        Text(
                          food.category,
                          style: const TextStyle(
                            color: muted,
                            fontSize: 12,
                            fontWeight: FontWeight.w700,
                          ),
                        ),
                      const SizedBox(height: 6),
                      Text(
                        food.description,
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(
                          color: muted,
                          fontSize: 11,
                          height: 1.3,
                        ),
                      ),
                      const SizedBox(height: 8),
                      Text(
                        _price(food),
                        style: const TextStyle(
                          color: navy,
                          fontSize: 15,
                          fontWeight: FontWeight.w900,
                        ),
                      ),
                    ],
                  ),
                ),
                const Icon(
                  Icons.chevron_right_rounded,
                  color: muted,
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _emptyState() {
    return Container(
      padding: const EdgeInsets.all(28),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
      ),
      child: const Column(
        children: [
          Icon(
            Icons.restaurant_menu_outlined,
            size: 42,
            color: muted,
          ),
          SizedBox(height: 10),
          Text(
            'No meals found',
            style: TextStyle(
              color: navy,
              fontWeight: FontWeight.w900,
              fontSize: 17,
            ),
          ),
          SizedBox(height: 5),
          Text(
            'Try another search or menu category.',
            textAlign: TextAlign.center,
            style: TextStyle(color: muted),
          ),
        ],
      ),
    );
  }

  Widget _infoChip(IconData icon, String label) {
    return Container(
      padding: const EdgeInsets.symmetric(
        horizontal: 10,
        vertical: 7,
      ),
      decoration: BoxDecoration(
        color: page,
        borderRadius: BorderRadius.circular(20),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 15, color: blue),
          const SizedBox(width: 5),
          Text(
            label,
            style: const TextStyle(
              color: navy,
              fontSize: 11,
              fontWeight: FontWeight.w800,
            ),
          ),
        ],
      ),
    );
  }

  Widget _openPill(bool open) {
    return Container(
      padding: const EdgeInsets.symmetric(
        horizontal: 9,
        vertical: 5,
      ),
      decoration: BoxDecoration(
        color: open ? const Color(0xFF20B95A) : const Color(0xFFE8EEF5),
        borderRadius: BorderRadius.circular(20),
      ),
      child: Text(
        open ? 'OPEN' : 'CLOSED',
        style: TextStyle(
          color: open ? Colors.white : muted,
          fontSize: 10,
          fontWeight: FontWeight.w900,
        ),
      ),
    );
  }

  Widget _foodImage(String url) {
    if (url.trim().isEmpty) {
      return Container(
        color: page,
        alignment: Alignment.center,
        child: const Icon(
          Icons.restaurant_rounded,
          color: muted,
          size: 36,
        ),
      );
    }

    return Image.network(
      url,
      fit: BoxFit.cover,
      errorBuilder: (_, __, ___) => Container(
        color: page,
        alignment: Alignment.center,
        child: const Icon(
          Icons.restaurant_rounded,
          color: muted,
          size: 36,
        ),
      ),
    );
  }

  String _price(FoodItem food) {
    final value = food.price;
    return 'GH₵ ${value.toStringAsFixed(2)}';
  }
}
