import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:atu_cafeteria/domain/models/models.dart';
import 'package:atu_cafeteria/presentation/providers/cafeteria_provider.dart';
import 'package:atu_cafeteria/presentation/providers/cart_provider.dart';

/// Mobile-first customer experience for the ATU Cafeteria application.
class RestaurantCustomerHomeScreen extends StatefulWidget {
  const RestaurantCustomerHomeScreen({super.key});

  @override
  State<RestaurantCustomerHomeScreen> createState() => _RestaurantCustomerHomeScreenState();
}

class _RestaurantCustomerHomeScreenState extends State<RestaurantCustomerHomeScreen> {
  static const navy = Color(0xFF073B82);
  static const blue = Color(0xFF0D55B5);
  static const yellow = Color(0xFFFFC400);
  static const page = Color(0xFFF5F8FC);
  static const muted = Color(0xFF61738A);

  int _tab = 0;
  String _query = '';

  @override
  Widget build(BuildContext context) {
    final provider = context.watch<CafeteriaProvider>();
    final user = provider.currentUser;
    if (user == null) {
      return Scaffold(
        backgroundColor: page,
        body: Center(
          child: Padding(
            padding: const EdgeInsets.all(28),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                const Icon(Icons.lock_outline_rounded,
                    size: 64, color: navy),
                const SizedBox(height: 16),
                const Text(
                  'Please sign in to continue.',
                  textAlign: TextAlign.center,
                  style: TextStyle(fontSize: 18, fontWeight: FontWeight.w900),
                ),
                const SizedBox(height: 6),
                const Text(
                  'Your session has ended. Sign in again to keep ordering.',
                  textAlign: TextAlign.center,
                  style: TextStyle(color: muted),
                ),
                const SizedBox(height: 22),
                FilledButton.icon(
                  onPressed: () =>
                      Navigator.pushReplacementNamed(context, '/login'),
                  icon: const Icon(Icons.login_rounded),
                  label: const Text('Sign in'),
                ),
              ],
            ),
          ),
        ),
      );
    }

    return Scaffold(
      backgroundColor: page,
      appBar: _appBar(context),
      body: IndexedStack(
        index: _tab,
        children: [
          _home(provider, user),
          _vendors(provider),
          _orders(provider),
          _profile(provider, user),
        ],
      ),
      bottomNavigationBar: _bottomNavigation(),
    );
  }

  PreferredSizeWidget _appBar(BuildContext context) {
    return AppBar(
      backgroundColor: navy,
      foregroundColor: Colors.white,
      elevation: 0,
      titleSpacing: 16,
      title: GestureDetector(
        onTap: () => setState(() => _tab = 0),
        child: Row(
          children: [
            Container(
              width: 38,
              height: 38,
              decoration: BoxDecoration(
                color: yellow,
                borderRadius: BorderRadius.circular(10),
              ),
              child: const Icon(Icons.restaurant_menu_rounded, color: navy),
            ),
            const SizedBox(width: 10),
            const Text(
              'ATU CAFETERIA',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.w900),
            ),
          ],
        ),
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
        IconButton(
          tooltip: 'Notifications',
          onPressed: () => Navigator.pushNamed(context, '/notifications'),
          icon: const Icon(Icons.notifications_none_rounded),
        ),
      ],
    );
  }

  Widget _bottomNavigation() {
    return NavigationBarTheme(
      data: NavigationBarThemeData(
        backgroundColor: navy,
        indicatorColor: const Color(0xFF4B8DE0),
        labelTextStyle: WidgetStateProperty.resolveWith(
          (states) => TextStyle(
            color: Colors.white,
            fontSize: 12,
            fontWeight: states.contains(WidgetState.selected)
                ? FontWeight.w900
                : FontWeight.w600,
          ),
        ),
        iconTheme: WidgetStateProperty.resolveWith(
          (states) => IconThemeData(
            color: states.contains(WidgetState.selected)
                ? Colors.white
                : Colors.white70,
            size: 24,
          ),
        ),
      ),
      child: NavigationBar(
        height: 72,
        selectedIndex: _tab,
        onDestinationSelected: (value) => setState(() => _tab = value),
        destinations: const [
          NavigationDestination(
            icon: Icon(Icons.home_outlined),
            selectedIcon: Icon(Icons.home),
            label: 'Home',
          ),
          NavigationDestination(
            icon: Icon(Icons.storefront_outlined),
            selectedIcon: Icon(Icons.storefront),
            label: 'Vendors',
          ),
          NavigationDestination(
            icon: Icon(Icons.receipt_long_outlined),
            selectedIcon: Icon(Icons.receipt_long),
            label: 'My Orders',
          ),
          NavigationDestination(
            icon: Icon(Icons.person_outline),
            selectedIcon: Icon(Icons.person),
            label: 'Profile',
          ),
        ],
      ),
    );
  }

  Widget _home(CafeteriaProvider provider, User user) {
    final foods = provider.allFoodItems.where((f) => f.isAvailable).toList();
    final vendors = _vendorsFrom(provider, foods);
    final query = _query.trim().toLowerCase();
    final filtered = query.isEmpty
        ? foods
        : foods
            .where((f) =>
                '${f.name} ${f.category} ${f.description}'
                    .toLowerCase()
                    .contains(query))
            .toList();

    return RefreshIndicator(
      onRefresh: provider.refreshAllData,
      child: ListView(
        padding: const EdgeInsets.fromLTRB(16, 16, 16, 24),
        children: [
          _welcome(user),
          const SizedBox(height: 16),
          _search(),
          const SizedBox(height: 20),
          _header('Featured Vendors', 'View All', () => setState(() => _tab = 1)),
          const SizedBox(height: 10),
          _featuredVendors(vendors),
          const SizedBox(height: 20),
          _header('Quick Actions', null, null),
          const SizedBox(height: 10),
          _quickActions(),
          const SizedBox(height: 20),
          _header(
            "Today's Menu",
            '${filtered.length} ${filtered.length == 1 ? 'meal' : 'meals'}',
            null,
          ),
          const SizedBox(height: 10),
          if (filtered.isEmpty)
            _emptyMenu(provider.allFoodItems.isEmpty)
          else
            ...filtered.take(6).map(_mealRow),
          const SizedBox(height: 16),
          _checkoutHint(),
        ],
      ),
    );
  }

  Widget _welcome(User user) {
    final firstName = user.fullName.trim().split(RegExp(r'\s+')).first;
    return Container(
      padding: const EdgeInsets.fromLTRB(18, 18, 18, 16),
      decoration: BoxDecoration(
        gradient: const LinearGradient(colors: [navy, blue]),
        borderRadius: BorderRadius.circular(18),
      ),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  'Good Morning,',
                  style: TextStyle(color: Colors.white70, fontSize: 15),
                ),
                const SizedBox(height: 2),
                Text(
                  '$firstName! 👋',
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(
                    color: Colors.white,
                    fontSize: 25,
                    fontWeight: FontWeight.w900,
                  ),
                ),
                const SizedBox(height: 4),
                const Text(
                  'Great food fuels great minds.',
                  style: TextStyle(color: Colors.white70),
                ),
              ],
            ),
          ),
          Container(
            width: 66,
            height: 66,
            decoration: const BoxDecoration(
              color: yellow,
              shape: BoxShape.circle,
            ),
            child: const Icon(Icons.restaurant_rounded, color: navy, size: 34),
          ),
        ],
      ),
    );
  }

  Widget _search() {
    return TextField(
      onChanged: (value) => setState(() => _query = value),
      decoration: InputDecoration(
        hintText: 'Search for food, vendors or meals...',
        prefixIcon: const Icon(Icons.search_rounded, color: navy),
        suffixIcon: _query.isEmpty
            ? null
            : IconButton(
                onPressed: () => setState(() => _query = ''),
                icon: const Icon(Icons.clear),
              ),
        filled: true,
        fillColor: Colors.white,
        contentPadding: const EdgeInsets.symmetric(vertical: 15),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(14),
          borderSide: const BorderSide(color: Color(0xFFD6E1EF)),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(14),
          borderSide: const BorderSide(color: Color(0xFFD6E1EF)),
        ),
      ),
    );
  }

  Widget _header(String title, String? action, VoidCallback? onTap) {
    return Row(
      children: [
        Expanded(
          child: Text(
            title,
            style: const TextStyle(
              color: navy,
              fontSize: 20,
              fontWeight: FontWeight.w900,
            ),
          ),
        ),
        if (action != null && onTap != null)
          TextButton(
            onPressed: onTap,
            child: const Text(
              'View All',
              style: TextStyle(color: blue, fontWeight: FontWeight.w800),
            ),
          ),
        if (action != null && onTap == null)
          Text(
            action,
            style: const TextStyle(color: muted, fontWeight: FontWeight.w600),
          ),
      ],
    );
  }

  List<User> _vendorsFrom(CafeteriaProvider provider, List<FoodItem> foods) {
    final result = provider.allVendors
        .where((v) => v.role.toUpperCase() == 'VENDOR')
        .toList();
    final ids = result.map((v) => v.id).whereType<int>().toSet();
    for (final food in foods) {
      if (!ids.contains(food.vendorId)) {
        result.add(
          User(
            id: food.vendorId,
            username: 'vendor_${food.vendorId}',
            role: 'VENDOR',
            fullName: 'Campus Vendor ${food.vendorId}',
            info: food.category,
          ),
        );
        ids.add(food.vendorId);
      }
    }
    return result;
  }

  Widget _featuredVendors(List<User> vendors) {
    if (vendors.isEmpty) {
      return _panel(
        const Padding(
          padding: EdgeInsets.all(16),
          child: Text('Vendors will appear here when their menus are available.'),
        ),
      );
    }
    final count = vendors.length > 5 ? 5 : vendors.length;
    return SizedBox(
      height: 218,
      child: ListView.separated(
        scrollDirection: Axis.horizontal,
        itemCount: count,
        separatorBuilder: (_, __) => const SizedBox(width: 10),
        itemBuilder: (_, index) => _vendorCard(vendors[index]),
      ),
    );
  }

  Widget _vendorCard(User vendor) {
    final provider = context.read<CafeteriaProvider>();
    final foods = provider.allFoodItems
        .where((f) => f.vendorId == vendor.id && f.isAvailable)
        .toList();
    final image = foods.isNotEmpty ? foods.first.imageUrl : '';

    return SizedBox(
      width: 172,
      child: Material(
        color: Colors.white,
        borderRadius: BorderRadius.circular(14),
        clipBehavior: Clip.antiAlias,
        child: InkWell(
          onTap: () {
            if (foods.isNotEmpty) {
              Navigator.pushNamed(
                context,
                '/food-detail',
                arguments: foods.first,
              );
            } else {
              setState(() => _tab = 1);
            }
          },
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              SizedBox(
                height: 112,
                width: double.infinity,
                child: _foodImage(image),
              ),
              Padding(
                padding: const EdgeInsets.fromLTRB(10, 8, 10, 9),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      vendor.fullName.isEmpty
                          ? 'Campus Vendor'
                          : vendor.fullName,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                        color: navy,
                        fontWeight: FontWeight.w900,
                      ),
                    ),
                    const SizedBox(height: 3),
                    Text(
                      vendor.info.isEmpty ? 'Campus Food' : vendor.info,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(fontSize: 11, color: muted),
                    ),
                    const SizedBox(height: 7),
                    Row(
                      children: [
                        const Icon(Icons.star, color: yellow, size: 17),
                        const Text(
                          ' 4.6',
                          style: TextStyle(fontWeight: FontWeight.w800),
                        ),
                        const Spacer(),
                        _openPill(vendor.isOpen),
                      ],
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _openPill(bool open) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 9, vertical: 5),
      decoration: BoxDecoration(
        color: open ? const Color(0xFF20B95A) : const Color(0xFFE8EEF5),
        borderRadius: BorderRadius.circular(20),
      ),
      child: Text(
        open ? 'Open' : 'Closed',
        style: TextStyle(
          color: open ? Colors.white : muted,
          fontSize: 10,
          fontWeight: FontWeight.w900,
        ),
      ),
    );
  }

  Widget _quickActions() {
    final actions = [
      ('All Vendors', Icons.storefront_rounded, () => setState(() => _tab = 1)),
      ('My Orders', Icons.receipt_long_rounded, () => setState(() => _tab = 2)),
      ('Cart', Icons.shopping_cart_rounded, () => Navigator.pushNamed(context, '/cart')),
      ('Wallet', Icons.account_balance_wallet_rounded, () => setState(() => _tab = 3)),
    ];

    return Row(
      children: actions.map((item) {
        return Expanded(
          child: Padding(
            padding: const EdgeInsets.only(right: 7),
            child: InkWell(
              onTap: item.$3,
              borderRadius: BorderRadius.circular(12),
              child: Container(
                height: 78,
                padding: const EdgeInsets.all(8),
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(color: const Color(0xFFD8E3F0)),
                ),
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Icon(item.$2, color: navy, size: 25),
                    const SizedBox(height: 5),
                    Text(
                      item.$1,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                        color: navy,
                        fontSize: 10,
                        fontWeight: FontWeight.w800,
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
        );
      }).toList(),
    );
  }

  Widget _mealRow(FoodItem item) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 9),
      child: Material(
        color: Colors.white,
        borderRadius: BorderRadius.circular(13),
        child: InkWell(
          borderRadius: BorderRadius.circular(13),
          onTap: () => Navigator.pushNamed(
            context,
            '/food-detail',
            arguments: item,
          ),
          child: Padding(
            padding: const EdgeInsets.all(9),
            child: Row(
              children: [
                ClipRRect(
                  borderRadius: BorderRadius.circular(10),
                  child: SizedBox(
                    width: 74,
                    height: 74,
                    child: _foodImage(item.imageUrl),
                  ),
                ),
                const SizedBox(width: 11),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        item.name,
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(
                          color: navy,
                          fontWeight: FontWeight.w900,
                        ),
                      ),
                      const SizedBox(height: 5),
                      Text(
                        item.category,
                        style: const TextStyle(color: muted, fontSize: 11),
                      ),
                      const SizedBox(height: 6),
                      Text(
                        'GH₵ ${item.price.toStringAsFixed(2)}',
                        style: const TextStyle(
                          color: blue,
                          fontSize: 15,
                          fontWeight: FontWeight.w900,
                        ),
                      ),
                    ],
                  ),
                ),
                const Icon(Icons.chevron_right_rounded, color: muted),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _checkoutHint() {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: const Color(0xFFEAF2FC),
        borderRadius: BorderRadius.circular(14),
      ),
      child: const Row(
        children: [
          Icon(Icons.info_outline_rounded, color: blue),
          SizedBox(width: 10),
          Expanded(
            child: Text(
              'Add your favourite meal to the cart and checkout securely from your wallet.',
            ),
          ),
        ],
      ),
    );
  }

  Widget _emptyMenu(bool noData) {
    return _panel(
      Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          children: [
            Icon(
              noData
                  ? Icons.restaurant_menu_outlined
                  : Icons.search_off_rounded,
              size: 38,
              color: muted,
            ),
            const SizedBox(height: 8),
            Text(
              noData
                  ? 'No meals are available right now.'
                  : 'No meals match your search.',
              textAlign: TextAlign.center,
            ),
          ],
        ),
      ),
    );
  }

  Widget _vendors(CafeteriaProvider provider) {
    final foods = provider.allFoodItems.where((f) => f.isAvailable).toList();
    final vendors = _vendorsFrom(provider, foods);
    return ListView(
      padding: const EdgeInsets.fromLTRB(16, 18, 16, 24),
      children: [
        const Text(
          'Campus Vendors',
          style: TextStyle(color: navy, fontSize: 27, fontWeight: FontWeight.w900),
        ),
        const SizedBox(height: 14),
        ...vendors.map(_vendorListCard),
      ],
    );
  }

  Widget _vendorListCard(User vendor) {
    final provider = context.read<CafeteriaProvider>();
    final foods = provider.allFoodItems
        .where((f) => f.vendorId == vendor.id && f.isAvailable)
        .toList();

    return Padding(
      padding: const EdgeInsets.only(bottom: 10),
      child: Material(
        color: Colors.white,
        borderRadius: BorderRadius.circular(15),
        child: ListTile(
          contentPadding: const EdgeInsets.all(12),
          leading: ClipRRect(
            borderRadius: BorderRadius.circular(10),
            child: SizedBox(
              width: 58,
              height: 58,
              child: _foodImage(foods.isNotEmpty ? foods.first.imageUrl : ''),
            ),
          ),
          title: Text(
            vendor.fullName.isEmpty ? 'Campus Vendor' : vendor.fullName,
            style: const TextStyle(color: navy, fontWeight: FontWeight.w900),
          ),
          subtitle: Text(
            '${foods.length} available menu item${foods.length == 1 ? '' : 's'}',
            style: const TextStyle(color: muted),
          ),
          trailing: _openPill(vendor.isOpen),
          onTap: foods.isEmpty
              ? null
              : () => Navigator.pushNamed(
                    context,
                    '/food-detail',
                    arguments: foods.first,
                  ),
        ),
      ),
    );
  }

  Widget _orders(CafeteriaProvider provider) {
    final orders = provider.customerOrders;
    return ListView(
      padding: const EdgeInsets.fromLTRB(16, 18, 16, 24),
      children: [
        const Text(
          'My Orders',
          style: TextStyle(color: navy, fontSize: 27, fontWeight: FontWeight.w900),
        ),
        const SizedBox(height: 14),
        if (orders.isEmpty)
          _panel(
            const Padding(
              padding: EdgeInsets.all(20),
              child: Text('You have no orders yet.'),
            ),
          )
        else
          ...orders.take(8).map(_orderCard),
        const SizedBox(height: 14),
        _panel(
          Padding(
            padding: const EdgeInsets.all(14),
            child: Row(
              children: [
                const Icon(Icons.account_balance_wallet_outlined, color: blue),
                const SizedBox(width: 10),
                Expanded(
                  child: Text(
                    'Wallet balance: GH₵ ${provider.customerWalletBalance.toStringAsFixed(2)}',
                    style: const TextStyle(color: navy, fontWeight: FontWeight.w800),
                  ),
                ),
                TextButton(
                  onPressed: () => setState(() => _tab = 3),
                  child: const Text('Top Up'),
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }

  Widget _orderCard(Order order) {
    final status = order.displayStatus;
    final color = status == 'Delivered'
        ? const Color(0xFF178A49)
        : status == 'Preparing'
            ? const Color(0xFFE39A00)
            : blue;

    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: Material(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        child: Padding(
          padding: const EdgeInsets.fromLTRB(14, 14, 14, 16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Expanded(
                    child: Text(
                      'Order #ATU-${order.id ?? '----'}',
                      style: const TextStyle(
                        color: navy,
                        fontSize: 18,
                        fontWeight: FontWeight.w900,
                      ),
                    ),
                  ),
                  _status(status, color),
                ],
              ),
              const SizedBox(height: 7),
              Text(
                '${order.foodName} • Qty: ${order.quantity}',
                style: const TextStyle(color: muted, fontSize: 14),
              ),
              const SizedBox(height: 5),
              Text(
                'GH₵ ${order.totalPrice.toStringAsFixed(2)}',
                style: const TextStyle(
                  color: blue,
                  fontSize: 16,
                  fontWeight: FontWeight.w900,
                ),
              ),
              const SizedBox(height: 13),
              _progress(status),
              if (order.id != null) ...[
                const SizedBox(height: 10),
                Align(
                  alignment: Alignment.centerRight,
                  child: OutlinedButton.icon(
                    onPressed: () => Navigator.pushNamed(
                      context,
                      '/order-tracking',
                      arguments: order.id,
                    ),
                    icon: const Icon(Icons.track_changes_rounded),
                    label: const Text('Track Order'),
                  ),
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }

  Widget _status(String text, Color color) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 11, vertical: 7),
      decoration: BoxDecoration(
        color: color.withValues(alpha: .12),
        borderRadius: BorderRadius.circular(18),
      ),
      child: Text(
        text,
        style: TextStyle(color: color, fontWeight: FontWeight.w900, fontSize: 11),
      ),
    );
  }

  Color _statusColor(String status) {
    switch (status.toUpperCase()) {
      case 'ACTIVE':
        return const Color(0xFF20A45A);
      case 'SUSPENDED':
        return const Color(0xFFE67E22);
      case 'DISABLED':
        return const Color(0xFFD64545);
      default:
        return const Color(0xFF20A45A);
    }
  }

  Future<void> _showAbout(BuildContext context) {
    return showDialog<void>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        icon: Container(
          width: 72,
          height: 72,
          decoration: BoxDecoration(
            color: yellow,
            borderRadius: BorderRadius.circular(18),
          ),
          child: const Icon(Icons.restaurant_menu_rounded, color: navy, size: 38),
        ),
        title: const Text('ATU Cafeteria',
            textAlign: TextAlign.center,
            style: TextStyle(fontWeight: FontWeight.w900)),
        content: const Text(
          'ATU Cafeteria Food Ordering & Live Order Tracking Application\n\n'
          'v1.1.0\n'
          'Accra Technical University',
          textAlign: TextAlign.center,
        ),
        actions: [
          FilledButton(
            onPressed: () => Navigator.pop(dialogContext),
            child: const Text('Close'),
          ),
        ],
      ),
    );
  }

  Widget _progress(String status) {
    const labels = ['Received', 'Preparing', 'Ready', 'Picked Up'];
    final normalized = status.toLowerCase();
    var active = 0;
    if (normalized.contains('prepar')) active = 1;
    if (normalized.contains('ready') || normalized.contains('delivery')) active = 2;
    if (normalized.contains('deliver') || normalized.contains('picked')) active = 3;
    if (normalized.contains('completed')) active = 3;

    return Row(
      children: [
        for (var i = 0; i < labels.length; i++) ...[
          Expanded(
            child: Column(
              children: [
                Container(
                  width: 24,
                  height: 24,
                  decoration: BoxDecoration(
                    shape: BoxShape.circle,
                    color: i <= active
                        ? (i == active ? yellow : navy)
                        : Colors.white,
                    border: Border.all(
                      color: i <= active
                          ? navy
                          : const Color(0xFF9FB7D3),
                      width: 2,
                    ),
                  ),
                  child: i < active
                      ? const Icon(Icons.check, size: 15, color: Colors.white)
                      : Center(
                          child: Text(
                            '${i + 1}',
                            style: TextStyle(
                              fontSize: 11,
                              color: i == active ? navy : muted,
                              fontWeight: FontWeight.w800,
                            ),
                          ),
                        ),
                ),
                const SizedBox(height: 4),
                Text(
                  labels[i],
                  textAlign: TextAlign.center,
                  style: const TextStyle(
                    fontSize: 9,
                    color: muted,
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ],
            ),
          ),
          if (i < labels.length - 1)
            Expanded(
              child: Container(
                height: 2,
                color: i < active ? navy : const Color(0xFFB8C9DC),
              ),
            ),
        ],
      ],
    );
  }

  Widget _profile(CafeteriaProvider provider, User user) {
    return ListView(
      padding: const EdgeInsets.fromLTRB(16, 20, 16, 24),
      children: [
        Center(
          child: CircleAvatar(
            radius: 38,
            backgroundColor: blue,
            child: Text(
              user.fullName.isEmpty ? 'S' : user.fullName[0].toUpperCase(),
              style: const TextStyle(
                color: Colors.white,
                fontSize: 30,
                fontWeight: FontWeight.w900,
              ),
            ),
          ),
        ),
        const SizedBox(height: 10),
        Center(
          child: Text(
            user.fullName,
            style: const TextStyle(
              color: navy,
              fontSize: 22,
              fontWeight: FontWeight.w900,
            ),
          ),
        ),
        const SizedBox(height: 4),
        Center(
          child: Text(
            user.email ?? user.username,
            style: const TextStyle(color: muted),
          ),
        ),
        const SizedBox(height: 14),
        Center(child: _status(user.accountStatus, _statusColor(user.accountStatus))),
        const SizedBox(height: 18),
        _walletCard(context, provider),
        const SizedBox(height: 14),
        _profileAction(Icons.info_outline_rounded, 'About this app',
            () => _showAbout(context)),
        _profileAction(
          Icons.notifications_none_rounded,
          'Notifications',
          () => Navigator.pushNamed(context, '/notifications'),
        ),
        _profileAction(
          Icons.logout_rounded,
          'Sign out',
          () {
            provider.logOut();
            Navigator.pushReplacementNamed(context, '/login');
          },
          danger: true,
        ),
      ],
    );
  }

  Widget _walletCard(BuildContext context, CafeteriaProvider provider) {
    return _panel(
      Padding(
        padding: const EdgeInsets.fromLTRB(18, 17, 18, 16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Row(
              children: [
                const Icon(
                  Icons.account_balance_wallet_outlined,
                  color: blue,
                  size: 30,
                ),
                const SizedBox(width: 13),
                const Expanded(
                  child: Text(
                    'Wallet Balance',
                    style: TextStyle(color: navy, fontWeight: FontWeight.w800),
                  ),
                ),
                Text(
                  'GH₵ ${provider.customerWalletBalance.toStringAsFixed(2)}',
                  style: const TextStyle(
                    color: blue,
                    fontSize: 20,
                    fontWeight: FontWeight.w900,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 14),
            SizedBox(
              height: 48,
              child: ElevatedButton.icon(
                onPressed: () => _showTopUpDialog(context, provider),
                icon: const Icon(Icons.add_card_rounded),
                label: const Text(
                  'TOP UP WALLET',
                  style: TextStyle(fontWeight: FontWeight.w900),
                ),
              ),
            ),
            const SizedBox(height: 7),
            const Text(
              'Add funds securely with Mobile Money or Card.',
              textAlign: TextAlign.center,
              style: TextStyle(color: muted, fontSize: 11),
            ),
          ],
        ),
      ),
    );
  }

  Widget _profileAction(
    IconData icon,
    String title,
    VoidCallback onTap, {
    bool danger = false,
  }) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 10),
      child: Material(
        color: Colors.white,
        borderRadius: BorderRadius.circular(14),
        child: ListTile(
          onTap: onTap,
          leading: Icon(icon, color: danger ? Colors.redAccent : navy),
          title: Text(
            title,
            style: TextStyle(
              color: danger ? Colors.redAccent : navy,
              fontWeight: FontWeight.w800,
            ),
          ),
          trailing: const Icon(Icons.chevron_right_rounded, color: muted),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(14),
          ),
        ),
      ),
    );
  }

  void _showTopUpDialog(BuildContext context, CafeteriaProvider provider) {
    final amountController = TextEditingController();
    bool isLoading = false;
    String? errorMsg;

    showDialog<void>(
      context: context,
      builder: (dialogContext) => StatefulBuilder(
        builder: (context, setDialogState) => AlertDialog(
          title: const Row(
            children: [
              Icon(Icons.account_balance_wallet_rounded, color: blue),
              SizedBox(width: 10),
              Expanded(child: Text('Top Up Wallet')),
            ],
          ),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                const Text(
                  'Add money to your ATU Cafeteria wallet using the secure payment gateway.',
                  style: TextStyle(color: muted, fontSize: 12),
                ),
                const SizedBox(height: 16),
                TextField(
                  controller: amountController,
                  autofocus: true,
                  keyboardType: const TextInputType.numberWithOptions(decimal: true),
                  decoration: const InputDecoration(
                    labelText: 'Amount',
                    prefixText: 'GH₵ ',
                    hintText: 'e.g. 50.00',
                  ),
                ),
                if (errorMsg != null) ...[
                  const SizedBox(height: 8),
                  Text(
                    errorMsg!,
                    style: const TextStyle(
                      color: Colors.red,
                      fontSize: 12,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                ],
              ],
            ),
          ),
          actions: [
            TextButton(
              onPressed: isLoading ? null : () => Navigator.pop(context),
              child: const Text('CANCEL'),
            ),
            ElevatedButton.icon(
              onPressed: isLoading
                  ? null
                  : () async {
                      final amount =
                          double.tryParse(amountController.text.trim()) ?? 0;
                      if (amount < 1) {
                        setDialogState(
                          () => errorMsg = 'Enter at least GH₵ 1.00.',
                        );
                        return;
                      }

                      setDialogState(() {
                        isLoading = true;
                        errorMsg = null;
                      });

                      final user = provider.currentUser;
                      final email = user?.email?.trim().isNotEmpty == true
                          ? user!.email!
                          : '${user?.username ?? 'customer'}@atu.edu.gh';
                      final payment = await provider.initializePaystackPayment(
                        amount: amount,
                        email: email,
                        purpose: 'WALLET_TOPUP',
                      );

                      if (!context.mounted) return;
                      if (payment == null) {
                        setDialogState(() {
                          isLoading = false;
                          errorMsg =
                              'Unable to start wallet top-up. Please check the cafeteria server and try again.';
                        });
                        return;
                      }

                      Navigator.pop(context);
                      _showPaymentConfirmation(
                        context,
                        provider,
                        payment,
                        amount,
                      );
                    },
              icon: isLoading
                  ? const SizedBox(
                      width: 16,
                      height: 16,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    )
                  : const Icon(Icons.arrow_forward_rounded),
              label: Text(isLoading ? 'STARTING...' : 'CONTINUE'),
            ),
          ],
        ),
      ),
    );
  }

  void _showPaymentConfirmation(
    BuildContext context,
    CafeteriaProvider provider,
    Map<String, dynamic> payment,
    double amount,
  ) {
    var isVerifying = false;
    String message = 'Confirm the payment after completing the Paystack checkout.';

    showDialog<void>(
      context: context,
      barrierDismissible: false,
      builder: (dialogContext) => StatefulBuilder(
        builder: (context, setState) => AlertDialog(
          title: const Row(
            children: [
              Icon(Icons.verified_user_rounded, color: blue),
              SizedBox(width: 10),
              Expanded(child: Text('Confirm Wallet Top-Up')),
            ],
          ),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                'Amount: GH₵ ${amount.toStringAsFixed(2)}',
                style: const TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.w900,
                  color: blue,
                ),
              ),
              const SizedBox(height: 10),
              const Text(
                'Complete the payment in the Paystack checkout, then tap Verify Payment.',
                style: TextStyle(color: muted, fontSize: 12),
              ),
              const SizedBox(height: 10),
              Text(
                message,
                style: const TextStyle(color: navy, fontWeight: FontWeight.w700),
              ),
            ],
          ),
          actions: [
            TextButton(
              onPressed: isVerifying ? null : () => Navigator.pop(context),
              child: const Text('CLOSE'),
            ),
            ElevatedButton(
              onPressed: isVerifying
                  ? null
                  : () async {
                      final reference = payment['reference']?.toString();
                      if (reference == null || reference.isEmpty) {
                        setState(() => message = 'Payment reference was not returned.');
                        return;
                      }

                      setState(() {
                        isVerifying = true;
                        message = 'Verifying payment with Laravel...';
                      });

                      final ok = await provider.verifyPaystackPayment(
                        reference: reference,
                        amount: amount,
                        purpose: 'WALLET_TOPUP',
                      );

                      if (!context.mounted) return;
                      setState(() {
                        isVerifying = false;
                        message = ok
                            ? 'Wallet credited successfully.'
                            : 'Payment verification failed. No wallet funds were added.';
                      });

                      if (ok) {
                        await provider.refreshAllData();
                      }
                    },
              child: isVerifying
                  ? const SizedBox(
                      width: 18,
                      height: 18,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    )
                  : const Text('VERIFY PAYMENT'),
            ),
          ],
        ),
      ),
    );
  }

  Widget _foodImage(String url) {
    if (url.trim().isEmpty) {
      return Container(
        color: const Color(0xFFE8EEF6),
        child: const Icon(Icons.restaurant_rounded, color: navy, size: 38),
      );
    }
    return Image.network(
      url,
      fit: BoxFit.cover,
      errorBuilder: (_, __, ___) => Container(
        color: const Color(0xFFE8EEF6),
        child: const Icon(Icons.restaurant_rounded, color: navy, size: 38),
      ),
    );
  }

  Widget _panel(Widget child) {
    return Container(
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: const Color(0xFFD8E3F0)),
      ),
      child: child,
    );
  }
}
