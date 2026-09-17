import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../core/theme/app_theme.dart';
import '../../domain/models/models.dart';
import '../providers/cafeteria_provider.dart';
import '../providers/cart_provider.dart';
import '../widgets/reference_design.dart';

class ReferenceStudentScreen extends StatefulWidget {
  const ReferenceStudentScreen({super.key});
  @override
  State<ReferenceStudentScreen> createState() => _ReferenceStudentScreenState();
}

class _ReferenceStudentScreenState extends State<ReferenceStudentScreen> {
  int _tab = 0;
  String _query = '';
  String _category = 'All';

  @override
  Widget build(BuildContext context) {
    final provider = context.watch<CafeteriaProvider>();
    final user = provider.currentUser;
    if (user == null) {
      return const Scaffold(body: Center(child: Text('Please sign in again.')));
    }

    return Scaffold(
      body: SafeArea(
        child: LayoutBuilder(
          builder: (context, constraints) {
            final wide = constraints.maxWidth >= 850;
            return Column(
              children: [
                _topBar(context, provider),
                Expanded(
                  child: IndexedStack(
                    index: _tab,
                    children: [
                      _home(provider, user, wide),
                      _vendors(provider, wide),
                      _orders(provider),
                      _profile(provider, user),
                    ],
                  ),
                ),
                if (!wide)
                  NavigationBar(
                    selectedIndex: _tab,
                    onDestinationSelected: (value) => setState(() => _tab = value),
                    destinations: const [
                      NavigationDestination(icon: Icon(Icons.home_outlined), selectedIcon: Icon(Icons.home), label: 'Home'),
                      NavigationDestination(icon: Icon(Icons.storefront_outlined), selectedIcon: Icon(Icons.storefront), label: 'Vendors'),
                      NavigationDestination(icon: Icon(Icons.receipt_long_outlined), selectedIcon: Icon(Icons.receipt_long), label: 'My Orders'),
                      NavigationDestination(icon: Icon(Icons.person_outline), selectedIcon: Icon(Icons.person), label: 'Profile'),
                    ],
                  ),
              ],
            );
          },
        ),
      ),
    );
  }

  Widget _topBar(BuildContext context, CafeteriaProvider provider) {
    return Container(
      height: 66,
      color: AppTheme.primary,
      padding: const EdgeInsets.symmetric(horizontal: 18),
      child: Row(
        children: [
          AtuBrand.title(compact: true),
          const Spacer(),
          Consumer<CartProvider>(
            builder: (_, cart, __) => Badge(
              isLabelVisible: cart.itemCount > 0,
              label: Text('${cart.itemCount}'),
              child: IconButton(
                tooltip: 'Cart',
                onPressed: () => Navigator.pushNamed(context, '/cart'),
                icon: const Icon(Icons.shopping_cart_outlined, color: Colors.white),
              ),
            ),
          ),
          IconButton(
            tooltip: 'Sign out',
            onPressed: () {
              provider.logOut();
              Navigator.pushReplacementNamed(context, '/login');
            },
            icon: const Icon(Icons.logout, color: Colors.white),
          ),
        ],
      ),
    );
  }

  Widget _home(CafeteriaProvider provider, User user, bool wide) {
    final available = provider.allFoodItems.where((item) => item.isAvailable).toList();
    final categories = <String>{
      'All',
      ...available.map((item) => item.category.trim()).where((item) => item.isNotEmpty),
    }.toList();
    categories.sort((a, b) => a == 'All' ? -1 : b == 'All' ? 1 : a.compareTo(b));

    final query = _query.trim().toLowerCase();
    final items = available.where((item) {
      final categoryMatch = _category == 'All' || item.category == _category;
      final searchMatch = query.isEmpty || '${item.name} ${item.category} ${item.description}'.toLowerCase().contains(query);
      return categoryMatch && searchMatch;
    }).toList();

    final popular = [...available]..sort((a, b) => a.price.compareTo(b.price));
    final vendors = _vendorDirectory(provider, available);

    return RefreshIndicator(
      onRefresh: provider.refreshAllData,
      child: ListView(
        padding: EdgeInsets.fromLTRB(wide ? 28 : 16, 18, wide ? 28 : 16, 34),
        children: [
          _welcomeBanner(user),
          const SizedBox(height: 16),
          _searchBox(),
          const SizedBox(height: 20),
          _sectionHeader('Featured Vendors', 'View All', () => setState(() => _tab = 1)),
          const SizedBox(height: 10),
          _featuredVendors(vendors, wide),
          const SizedBox(height: 22),
          _sectionHeader('Browse by Category', null, null),
          const SizedBox(height: 10),
          _categoryChips(categories),
          const SizedBox(height: 22),
          _sectionHeader('Today’s Menu', '${items.length} ${items.length == 1 ? 'meal' : 'meals'}', null),
          const SizedBox(height: 12),
          if (items.isEmpty)
            _emptyMenu(provider.allFoodItems.isEmpty)
          else
            _mealGrid(items, wide),
          if (popular.length >= 3) ...[
            const SizedBox(height: 26),
            _sectionHeader('Popular on Campus', 'View Menu', () => setState(() => _category = 'All')),
            const SizedBox(height: 10),
            _popularRow(popular.take(3).toList()),
          ],
          const SizedBox(height: 26),
          _quickActions(),
          const SizedBox(height: 20),
          _checkoutNotice(),
        ],
      ),
    );
  }

  Widget _welcomeBanner(User user) {
    return Container(
      padding: const EdgeInsets.all(22),
      decoration: BoxDecoration(
        gradient: const LinearGradient(colors: [AppTheme.primary, Color(0xFF0B56A5)]),
        borderRadius: BorderRadius.circular(24),
        boxShadow: [BoxShadow(color: AppTheme.primary.withValues(alpha: .18), blurRadius: 18, offset: const Offset(0, 8))],
      ),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text('Good Morning,', style: TextStyle(color: Colors.white70, fontSize: 16)),
                const SizedBox(height: 4),
                Text('${user.fullName}! 👋', maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(color: Colors.white, fontSize: 27, fontWeight: FontWeight.w900)),
                const SizedBox(height: 6),
                const Text('Great food fuels great minds.', style: TextStyle(color: Colors.white70)),
              ],
            ),
          ),
          Container(
            width: 72,
            height: 72,
            decoration: BoxDecoration(color: Colors.white.withValues(alpha: .12), shape: BoxShape.circle),
            child: const Icon(Icons.restaurant_rounded, color: AppTheme.accent, size: 42),
          ),
        ],
      ),
    );
  }

  Widget _searchBox() {
    return TextField(
      decoration: InputDecoration(
        prefixIcon: const Icon(Icons.search),
        hintText: 'Search for food, vendors or meals...',
        suffixIcon: _query.isEmpty
            ? null
            : IconButton(onPressed: () => setState(() => _query = ''), icon: const Icon(Icons.clear)),
      ),
      onChanged: (value) => setState(() => _query = value),
    );
  }

  Widget _sectionHeader(String title, String? action, VoidCallback? onAction) {
    return Row(
      children: [
        Expanded(child: Text(title, style: const TextStyle(fontSize: 20, fontWeight: FontWeight.w900, color: AppTheme.textDark))),
        if (action != null && onAction != null) TextButton(onPressed: onAction, child: Text(action)),
        if (action != null && onAction == null) Text(action, style: const TextStyle(color: AppTheme.textMuted)),
      ],
    );
  }

  List<User> _vendorDirectory(CafeteriaProvider provider, List<FoodItem> foods) {
    final result = provider.allVendors.where((vendor) => vendor.role.toUpperCase() == 'VENDOR' && vendor.isOpen).toList();
    final ids = result.map((vendor) => vendor.id).whereType<int>().toSet();
    for (final food in foods) {
      if (!ids.contains(food.vendorId)) {
        result.add(User(id: food.vendorId, username: 'vendor_${food.vendorId}', role: 'VENDOR', fullName: 'Campus Vendor ${food.vendorId}', info: food.category));
        ids.add(food.vendorId);
      }
    }
    return result;
  }

  Widget _featuredVendors(List<User> vendors, bool wide) {
    if (vendors.isEmpty) {
      return const ReferenceCard(child: Padding(padding: EdgeInsets.all(10), child: Text('Featured vendors will appear here when vendors publish their menus.')));
    }

    return SizedBox(
      height: wide ? 205 : 190,
      child: ListView.separated(
        scrollDirection: Axis.horizontal,
        itemCount: vendors.length.clamp(0, 6),
        separatorBuilder: (_, __) => const SizedBox(width: 12),
        itemBuilder: (_, index) => _vendorCard(vendors[index]),
      ),
    );
  }

  Widget _vendorCard(User vendor) {
    final foods = context.read<CafeteriaProvider>().allFoodItems.where((item) => item.vendorId == vendor.id && item.isAvailable).toList();
    final imageUrl = foods.isNotEmpty ? _imageFor(foods.first) : '';
    return SizedBox(
      width: 245,
      child: ReferenceCard(
        padding: EdgeInsets.zero,
        child: InkWell(
          borderRadius: BorderRadius.circular(16),
          onTap: () => setState(() => _tab = 1),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              FoodImage(url: imageUrl, width: double.infinity, height: 105),
              Padding(
                padding: const EdgeInsets.fromLTRB(14, 10, 14, 12),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(children: [Expanded(child: Text(vendor.fullName.isEmpty ? 'Campus Vendor' : vendor.fullName, maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontWeight: FontWeight.w900, color: AppTheme.textDark))), const Icon(Icons.verified, size: 16, color: AppTheme.primary)]),
                    const SizedBox(height: 4),
                    Text(vendor.info.isEmpty ? (foods.isNotEmpty ? foods.first.category : 'Campus Food') : vendor.info, maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 11, color: AppTheme.textMuted)),
                    const SizedBox(height: 9),
                    Row(children: [const Icon(Icons.star, color: AppTheme.accent, size: 16), const Text(' 4.6', style: TextStyle(fontWeight: FontWeight.w800)), const Spacer(), StatusPill(vendor.isOpen ? 'Open' : 'Closed')]),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _categoryChips(List<String> categories) {
    return SingleChildScrollView(
      scrollDirection: Axis.horizontal,
      child: Row(
        children: [
          for (final category in categories)
            Padding(
              padding: const EdgeInsets.only(right: 8),
              child: ChoiceChip(
                selected: category == _category,
                label: Text(category),
                onSelected: (_) => setState(() => _category = category),
              ),
            ),
        ],
      ),
    );
  }

  Widget _mealGrid(List<FoodItem> items, bool wide) {
    final count = wide ? 3 : 1;
    if (!wide) return Column(children: items.take(8).map(_meal).toList());
    return GridView.builder(
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      itemCount: items.take(9).length,
      gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(crossAxisCount: 3, crossAxisSpacing: 14, mainAxisSpacing: 14, mainAxisExtent: 320),
      itemBuilder: (_, index) => _mealTile(items[index]),
    );
  }

  Widget _mealTile(FoodItem item) {
    final scheme = Theme.of(context).colorScheme;
    return Card(
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: () => Navigator.pushNamed(context, '/food-detail', arguments: item),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Stack(children: [
              SizedBox(width: double.infinity, height: 158, child: Image.network(_imageFor(item), fit: BoxFit.cover, errorBuilder: (_, __, ___) => Container(color: scheme.primaryContainer, child: Icon(Icons.restaurant_rounded, size: 52, color: scheme.primary)))),
              Positioned(top: 10, left: 10, child: Container(padding: const EdgeInsets.symmetric(horizontal: 9, vertical: 5), decoration: BoxDecoration(color: Colors.white.withValues(alpha: .93), borderRadius: BorderRadius.circular(20)), child: Text(item.category.isEmpty ? 'Meal' : item.category, style: TextStyle(color: scheme.primary, fontSize: 10, fontWeight: FontWeight.w800)))),
            ]),
            Expanded(child: Padding(padding: const EdgeInsets.fromLTRB(14, 11, 14, 12), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Text(item.name, maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w900, color: AppTheme.textDark)),
              const SizedBox(height: 4),
              Expanded(child: Text(item.description.isEmpty ? 'Freshly prepared and available today.' : item.description, maxLines: 2, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 11, color: AppTheme.textMuted, height: 1.3))),
              Row(children: [Text('GH₵ ${item.price.toStringAsFixed(2)}', style: TextStyle(fontSize: 16, fontWeight: FontWeight.w900, color: scheme.primary)), const Spacer(), FilledButton(onPressed: () => _addToCart(item), child: const Text('Add'))]),
            ]))),
          ],
        ),
      ),
    );
  }

  Widget _meal(FoodItem item) {
    return Padding(padding: const EdgeInsets.only(bottom: 10), child: ReferenceCard(child: Row(children: [FoodImage(url: _imageFor(item), width: 105, height: 92), const SizedBox(width: 12), Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text(item.name, maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w900, color: AppTheme.textDark)), const SizedBox(height: 4), Text(item.description.isEmpty ? 'Freshly prepared and available today.' : item.description, maxLines: 2, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 11, color: AppTheme.textMuted)), const SizedBox(height: 7), Text('GH₵ ${item.price.toStringAsFixed(2)}', style: const TextStyle(fontSize: 17, fontWeight: FontWeight.w900, color: AppTheme.primary))])), const SizedBox(width: 8), FilledButton(onPressed: () => _addToCart(item), child: const Text('+ Add'))] )));
  }

  void _addToCart(FoodItem item) {
    context.read<CartProvider>().add(item);
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('${item.name} added to cart'), action: SnackBarAction(label: 'VIEW CART', onPressed: () => Navigator.pushNamed(context, '/cart'))));
  }

  String _imageFor(FoodItem item) {
    if (item.imageUrl.trim().isNotEmpty) return item.imageUrl;
    final category = item.category.toLowerCase();
    if (category.contains('drink') || category.contains('beverage')) return 'https://images.unsplash.com/photo-1544145945-f90425340c7e?auto=format&fit=crop&w=1000&q=85';
    if (category.contains('snack') || category.contains('breakfast')) return 'https://images.unsplash.com/photo-1509440159596-0249088772ff?auto=format&fit=crop&w=1000&q=85';
    if (category.contains('dessert')) return 'https://images.unsplash.com/photo-1551024506-0bccd828d307?auto=format&fit=crop&w=1000&q=85';
    return 'https://images.unsplash.com/photo-1547592180-85f173990554?auto=format&fit=crop&w=1000&q=85';
  }

  Widget _emptyMenu(bool noData) {
    return ReferenceCard(
      child: Padding(
        padding: const EdgeInsets.all(28),
        child: Column(children: [
          Icon(noData ? Icons.cloud_off_rounded : Icons.search_off_rounded, size: 48, color: AppTheme.primary),
          const SizedBox(height: 10),
          Text(noData ? 'The menu is not loaded yet' : 'No meals match your search', style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w900, color: AppTheme.textDark)),
          const SizedBox(height: 5),
          Text(noData ? 'Pull down to refresh and load the latest cafeteria menu.' : 'Try another search term or choose All categories.', textAlign: TextAlign.center, style: const TextStyle(color: AppTheme.textMuted)),
        ]),
      ),
    );
  }

  Widget _popularRow(List<FoodItem> items) {
    return SizedBox(height: 150, child: ListView.separated(scrollDirection: Axis.horizontal, itemCount: items.length, separatorBuilder: (_, __) => const SizedBox(width: 12), itemBuilder: (_, index) {
      final item = items[index];
      return SizedBox(width: 235, child: ReferenceCard(padding: EdgeInsets.zero, child: InkWell(onTap: () => Navigator.pushNamed(context, '/food-detail', arguments: item), child: Row(children: [FoodImage(url: _imageFor(item), width: 94, height: 150), Expanded(child: Padding(padding: const EdgeInsets.all(12), child: Column(crossAxisAlignment: CrossAxisAlignment.start, mainAxisAlignment: MainAxisAlignment.center, children: [Text(item.name, maxLines: 2, overflow: TextOverflow.ellipsis, style: const TextStyle(fontWeight: FontWeight.w900, color: AppTheme.textDark)), const SizedBox(height: 7), Text('GH₵ ${item.price.toStringAsFixed(2)}', style: const TextStyle(fontWeight: FontWeight.w900, color: AppTheme.primary)), const SizedBox(height: 8), const Row(children: [Icon(Icons.local_fire_department, size: 16, color: AppTheme.accent), SizedBox(width: 4), Text('Popular', style: TextStyle(fontSize: 11, fontWeight: FontWeight.w700))])]))]))));
    }));
  }

  Widget _quickActions() {
    return Row(children: [
      Expanded(child: _actionCard(Icons.shopping_cart_outlined, 'My Cart', 'Review your order', () => Navigator.pushNamed(context, '/cart'))),
      const SizedBox(width: 10),
      Expanded(child: _actionCard(Icons.receipt_long_outlined, 'My Orders', 'Track your meals', () => setState(() => _tab = 2))),
      const SizedBox(width: 10),
      Expanded(child: _actionCard(Icons.storefront_outlined, 'Vendors', 'Explore campus', () => setState(() => _tab = 1))),
    ]);
  }

  Widget _actionCard(IconData icon, String title, String subtitle, VoidCallback onTap) {
    return Card(child: InkWell(onTap: onTap, borderRadius: BorderRadius.circular(16), child: Padding(padding: const EdgeInsets.all(14), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Icon(icon, color: AppTheme.primary), const SizedBox(height: 9), Text(title, style: const TextStyle(fontWeight: FontWeight.w900, color: AppTheme.textDark)), const SizedBox(height: 2), Text(subtitle, style: const TextStyle(fontSize: 10, color: AppTheme.textMuted))]))));
  }

  Widget _checkoutNotice() {
    return Card(color: AppTheme.primary.withValues(alpha: .06), child: const Padding(padding: EdgeInsets.all(16), child: Row(children: [Icon(Icons.lock_outline, color: AppTheme.primary), SizedBox(width: 12), Expanded(child: Text('Browse meals freely. Your account is only needed when you are ready to complete checkout.', style: TextStyle(color: AppTheme.textDark, height: 1.35)))])));
  }

  Widget _vendors(CafeteriaProvider provider, bool wide) {
    final vendors = _vendorDirectory(provider, provider.allFoodItems.where((item) => item.isAvailable).toList());
    final q = _query.trim().toLowerCase();
    final filtered = vendors.where((vendor) => q.isEmpty || '${vendor.fullName} ${vendor.info}'.toLowerCase().contains(q)).toList();
    return ListView(padding: EdgeInsets.all(wide ? 28 : 18), children: [
      const Text('Vendors & Food', style: TextStyle(fontSize: 25, fontWeight: FontWeight.w900, color: AppTheme.textDark)),
      const SizedBox(height: 4),
      const Text('Choose a campus vendor and explore today’s menu.', style: TextStyle(color: AppTheme.textMuted)),
      const SizedBox(height: 16),
      TextField(decoration: const InputDecoration(prefixIcon: Icon(Icons.search), hintText: 'Search vendors...'), onChanged: (value) => setState(() => _query = value)),
      const SizedBox(height: 16),
      if (filtered.isEmpty) const ReferenceCard(child: Text('No vendors are available right now.')) else ...filtered.map((vendor) => Padding(padding: const EdgeInsets.only(bottom: 12), child: _vendorCard(vendor))),
    ]);
  }

  Widget _orders(CafeteriaProvider provider) => RefreshIndicator(
        onRefresh: provider.refreshAllData,
        child: ListView(padding: const EdgeInsets.all(18), children: [
          Row(children: [const Expanded(child: Text('My Orders', style: TextStyle(fontSize: 25, fontWeight: FontWeight.w900, color: AppTheme.textDark))), IconButton(onPressed: provider.refreshAllData, icon: const Icon(Icons.refresh))]),
          const SizedBox(height: 8),
          if (provider.customerOrders.isEmpty)
            const ReferenceCard(child: Padding(padding: EdgeInsets.all(16), child: Column(children: [Icon(Icons.receipt_long, size: 48, color: AppTheme.primary), SizedBox(height: 8), Text('No orders yet.', style: TextStyle(fontWeight: FontWeight.w800)), SizedBox(height: 6), Text('Browse the menu and place your first order.', style: TextStyle(color: AppTheme.textMuted))])))
          else
            ...provider.customerOrders.map(_orderCard),
        ],
      );

  Widget _orderCard(Order order) => Padding(padding: const EdgeInsets.only(bottom: 10), child: ReferenceCard(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Row(children: [Expanded(child: Text('Order #ATU-${order.id ?? 0}', style: const TextStyle(fontSize: 17, fontWeight: FontWeight.w900, color: AppTheme.textDark))), StatusPill(order.displayStatus)]), const SizedBox(height: 6), Text('${order.foodName} • Qty: ${order.quantity} • GH₵ ${order.totalPrice.toStringAsFixed(2)}', style: const TextStyle(color: AppTheme.textMuted)), const SizedBox(height: 16), StepTrack(status: order.status), const SizedBox(height: 12), Align(alignment: Alignment.centerRight, child: OutlinedButton.icon(onPressed: order.id == null ? null : () => Navigator.pushNamed(context, '/order-tracking', arguments: order.id), icon: const Icon(Icons.track_changes), label: const Text('Track Order')))])));

  Widget _profile(CafeteriaProvider provider, User user) => ListView(padding: const EdgeInsets.all(18), children: [
        ReferenceCard(child: Row(children: [CircleAvatar(radius: 34, backgroundColor: AppTheme.primary, child: Text(user.fullName.isEmpty ? 'S' : user.fullName[0].toUpperCase(), style: const TextStyle(color: Colors.white, fontSize: 25, fontWeight: FontWeight.w900))), const SizedBox(width: 14), Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text(user.fullName, style: const TextStyle(fontSize: 20, fontWeight: FontWeight.w900, color: AppTheme.textDark)), Text(user.email ?? user.username, style: const TextStyle(color: AppTheme.textMuted)), const SizedBox(height: 5), StatusPill(user.accountStatus)]))])),
        const SizedBox(height: 12),
        MetricTile(label: 'Wallet Balance', value: 'GH₵ ${user.balance.toStringAsFixed(2)}', icon: Icons.account_balance_wallet_outlined),
        const SizedBox(height: 12),
        ReferenceCard(child: Column(children: [ListTile(leading: const Icon(Icons.settings_outlined), title: const Text('Settings'), trailing: const Icon(Icons.chevron_right), onTap: () {}), const Divider(), ListTile(leading: const Icon(Icons.logout, color: Colors.red), title: const Text('Sign out'), onTap: () { provider.logOut(); Navigator.pushReplacementNamed(context, '/login'); })])),
      ]);
}
