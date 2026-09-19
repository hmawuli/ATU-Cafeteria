import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:atu_cafeteria/domain/models/models.dart';
import 'package:atu_cafeteria/presentation/providers/cafeteria_provider.dart';
import 'package:atu_cafeteria/presentation/providers/cart_provider.dart';

/// Mobile-first student experience based on the supplied ATU Cafeteria
/// reference screens. The existing provider and routes remain the source of
/// truth for live data and actions.
class MobileStudentScreen extends StatefulWidget {
  const MobileStudentScreen({super.key});

  @override
  State<MobileStudentScreen> createState() => _MobileStudentScreenState();
}

class _MobileStudentScreenState extends State<MobileStudentScreen> {
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
      return const Scaffold(body: Center(child: Text('Please sign in again.')));
    }

    return Scaffold(
      backgroundColor: page,
      appBar: _appBar(context, provider),
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

  PreferredSizeWidget _appBar(
    BuildContext context,
    CafeteriaProvider provider,
  ) {
    return AppBar(
      backgroundColor: navy,
      foregroundColor: Colors.white,
      elevation: 0,
      titleSpacing: 16,
      title: Row(
        children: [
          Container(
            width: 38,
            height: 38,
            decoration: BoxDecoration(
              color: yellow,
              borderRadius: BorderRadius.circular(10),
            ),
            child: const Icon(
              Icons.restaurant_menu_rounded,
              color: navy,
              size: 24,
            ),
          ),
          const SizedBox(width: 10),
          const Text(
            'ATU CAFETERIA',
            style: TextStyle(fontSize: 18, fontWeight: FontWeight.w900),
          ),
        ],
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
        const SizedBox(width: 4),
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
        : foods.where((f) {
            return '${f.name} ${f.category} ${f.description}'
                .toLowerCase()
                .contains(query);
          }).toList();

    return RefreshIndicator(
      onRefresh: provider.refreshAllData,
      child: ListView(
        padding: const EdgeInsets.fromLTRB(16, 16, 16, 24),
        children: [
          _welcome(user),
          const SizedBox(height: 16),
          _search(),
          const SizedBox(height: 20),
          _header(
              'Featured Vendors', 'View All', () => setState(() => _tab = 1)),
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
            child: const Icon(
              Icons.school_rounded,
              color: navy,
              size: 34,
            ),
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
        result.add(User(
          id: food.vendorId,
          username: 'vendor_${food.vendorId}',
          role: 'VENDOR',
          fullName: 'Campus Vendor ${food.vendorId}',
          info: food.category,
        ));
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
          child:
              Text('Vendors will appear here when their menus are available.'),
        ),
      );
    }
    return SizedBox(
      height: 218,
      child: ListView.separated(
        scrollDirection: Axis.horizontal,
        itemCount: vendors.length > 5 ? 5 : vendors.length,
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
              Navigator.pushNamed(context, '/food-detail',
                  arguments: foods.first);
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
                        const Text(' 4.6',
                            style: TextStyle(fontWeight: FontWeight.w800)),
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
      (
        'Cart',
        Icons.shopping_cart_rounded,
        () => Navigator.pushNamed(context, '/cart')
      ),
      ('Settings', Icons.settings_rounded, () => setState(() => _tab = 3)),
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
                SizedBox(
                    width: 88, height: 76, child: _foodImage(item.imageUrl)),
                const SizedBox(width: 11),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        item.name,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(
                          color: navy,
                          fontWeight: FontWeight.w900,
                          fontSize: 15,
                        ),
                      ),
                      const SizedBox(height: 3),
                      Text(
                        item.description.isEmpty
                            ? item.category
                            : item.description,
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(color: muted, fontSize: 11),
                      ),
                      const SizedBox(height: 5),
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
                Container(
                  padding:
                      const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                  decoration: BoxDecoration(
                    color: yellow,
                    borderRadius: BorderRadius.circular(20),
                  ),
                  child: const Text(
                    '+ Add',
                    style: TextStyle(color: navy, fontWeight: FontWeight.w900),
                  ),
                ),
              ],
            ),
          ),
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
          'Vendors & Food',
          style:
              TextStyle(color: navy, fontSize: 27, fontWeight: FontWeight.w900),
        ),
        const SizedBox(height: 4),
        const Text(
          "Choose a campus vendor and explore today's menu.",
          style: TextStyle(color: muted),
        ),
        const SizedBox(height: 16),
        TextField(
          decoration: InputDecoration(
            hintText: 'Search vendors...',
            prefixIcon: const Icon(Icons.search, color: navy),
            filled: true,
            fillColor: Colors.white,
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(14),
              borderSide: const BorderSide(color: Color(0xFFD6E1EF)),
            ),
          ),
        ),
        const SizedBox(height: 16),
        ...vendors.map((vendor) => Padding(
              padding: const EdgeInsets.only(bottom: 12),
              child: _vendorListCard(vendor),
            )),
      ],
    );
  }

  Widget _vendorListCard(User vendor) {
    final foods = context
        .read<CafeteriaProvider>()
        .allFoodItems
        .where((f) => f.vendorId == vendor.id && f.isAvailable)
        .toList();
    return Material(
      color: Colors.white,
      borderRadius: BorderRadius.circular(16),
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: () {
          if (foods.isNotEmpty) {
            Navigator.pushNamed(context, '/food-detail',
                arguments: foods.first);
          }
        },
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            SizedBox(
                height: 145,
                width: double.infinity,
                child:
                    _foodImage(foods.isNotEmpty ? foods.first.imageUrl : '')),
            Padding(
              padding: const EdgeInsets.fromLTRB(14, 12, 14, 14),
              child: Row(
                children: [
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          vendor.fullName,
                          style: const TextStyle(
                              color: navy,
                              fontSize: 17,
                              fontWeight: FontWeight.w900),
                        ),
                        const SizedBox(height: 3),
                        Text(
                            vendor.info.isEmpty
                                ? 'Local Dishes & Snacks'
                                : vendor.info,
                            style: const TextStyle(color: muted)),
                        const SizedBox(height: 7),
                        const Row(children: [
                          Icon(Icons.star, color: yellow, size: 18),
                          Text(' 4.6',
                              style: TextStyle(fontWeight: FontWeight.w800))
                        ]),
                      ],
                    ),
                  ),
                  _openPill(vendor.isOpen),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _orders(CafeteriaProvider provider) {
    final orders = provider.customerOrders;
    return ListView(
      padding: const EdgeInsets.fromLTRB(16, 18, 16, 24),
      children: [
        const Text('My Orders',
            style: TextStyle(
                color: navy, fontSize: 27, fontWeight: FontWeight.w900)),
        const SizedBox(height: 14),
        if (orders.isEmpty)
          _panel(const Padding(
              padding: EdgeInsets.all(20),
              child: Text('You have no orders yet.')))
        else
          ...orders.take(8).map(_orderCard),
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
                          fontWeight: FontWeight.w900),
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
                    color: blue, fontSize: 16, fontWeight: FontWeight.w900),
              ),
              const SizedBox(height: 13),
              _progress(status),
              if (order.id != null) ...[
                const SizedBox(height: 10),
                Align(
                  alignment: Alignment.centerRight,
                  child: OutlinedButton.icon(
                    onPressed: () => Navigator.pushNamed(
                        context, '/order-tracking',
                        arguments: order.id),
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
          borderRadius: BorderRadius.circular(18)),
      child: Text(text,
          style: TextStyle(
              color: color, fontWeight: FontWeight.w900, fontSize: 11)),
    );
  }

  Widget _progress(String status) {
    const labels = ['Received', 'Preparing', 'Ready', 'Picked Up'];
    final normalized = status.toLowerCase();
    var active = 0;
    if (normalized.contains('prepar')) active = 1;
    if (normalized.contains('ready') || normalized.contains('delivery')) {
      active = 2;
    }
    if (normalized.contains('deliver') || normalized.contains('picked')) {
      active = 3;
    }
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
                        color: i <= active ? navy : const Color(0xFF9FB7D3),
                        width: 2),
                  ),
                  child: i < active
                      ? const Icon(Icons.check, size: 15, color: Colors.white)
                      : Center(
                          child: Text('${i + 1}',
                              style: TextStyle(
                                  fontSize: 11,
                                  color: i == active ? navy : muted,
                                  fontWeight: FontWeight.w800))),
                ),
                const SizedBox(height: 4),
                Text(labels[i],
                    textAlign: TextAlign.center,
                    style: const TextStyle(
                        fontSize: 9,
                        color: muted,
                        fontWeight: FontWeight.w700)),
              ],
            ),
          ),
          if (i < labels.length - 1)
            Expanded(
                child: Container(
                    height: 2,
                    color: i < active ? navy : const Color(0xFFB8C9DC))),
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
                  fontWeight: FontWeight.w900),
            ),
          ),
        ),
        const SizedBox(height: 10),
        Center(
          child: Text(user.fullName,
              style: const TextStyle(
                  color: navy, fontSize: 22, fontWeight: FontWeight.w900)),
        ),
        const SizedBox(height: 4),
        Center(
            child: Text(user.email ?? user.username,
                style: const TextStyle(color: muted))),
        const SizedBox(height: 14),
        Center(child: _status(user.accountStatus, const Color(0xFF20A45A))),
        const SizedBox(height: 18),
        _panel(
          Padding(
            padding: const EdgeInsets.all(18),
            child: Row(
              children: [
                const Icon(Icons.account_balance_wallet_outlined,
                    color: blue, size: 30),
                const SizedBox(width: 13),
                const Expanded(
                    child: Text('Wallet Balance',
                        style: TextStyle(
                            color: navy, fontWeight: FontWeight.w800))),
                Text('GH₵ ${user.balance.toStringAsFixed(2)}',
                    style: const TextStyle(
                        color: blue,
                        fontSize: 20,
                        fontWeight: FontWeight.w900)),
              ],
            ),
          ),
        ),
        const SizedBox(height: 14),
        _profileAction(Icons.settings_outlined, 'Settings', () {}),
        _profileAction(Icons.notifications_none_rounded, 'Notifications',
            () => Navigator.pushNamed(context, '/notifications')),
        _profileAction(Icons.logout_rounded, 'Sign out', () {
          provider.logOut();
          Navigator.pushReplacementNamed(context, '/login');
        }, danger: true),
      ],
    );
  }

  Widget _profileAction(IconData icon, String title, VoidCallback onTap,
      {bool danger = false}) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 10),
      child: Material(
        color: Colors.white,
        borderRadius: BorderRadius.circular(14),
        child: ListTile(
          onTap: onTap,
          leading: Icon(icon, color: danger ? Colors.redAccent : navy),
          title: Text(title,
              style: TextStyle(
                  color: danger ? Colors.redAccent : navy,
                  fontWeight: FontWeight.w800)),
          trailing: const Icon(Icons.chevron_right_rounded, color: muted),
          shape:
              RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
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

  Widget _emptyMenu(bool noData) {
    return _panel(
      Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          children: [
            const Icon(Icons.cloud_off_rounded, color: muted, size: 42),
            const SizedBox(height: 8),
            Text(
              noData
                  ? "Today's menu is not available yet."
                  : 'No meals match your search.',
              textAlign: TextAlign.center,
              style: const TextStyle(color: navy, fontWeight: FontWeight.w800),
            ),
            if (noData) ...[
              const SizedBox(height: 10),
              OutlinedButton.icon(
                onPressed: context.read<CafeteriaProvider>().refreshAllData,
                icon: const Icon(Icons.refresh),
                label: const Text('Try again'),
              ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _checkoutHint() {
    return Container(
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: const Color(0xFFEAF2FF),
        borderRadius: BorderRadius.circular(16),
      ),
      child: const Row(
        children: [
          Icon(Icons.lock_outline_rounded, color: blue, size: 30),
          SizedBox(width: 12),
          Expanded(
            child: Text(
              'Browse freely and build your cart. Sign in is required only when you are ready to checkout.',
              style: TextStyle(
                  color: navy, fontWeight: FontWeight.w700, height: 1.35),
            ),
          ),
        ],
      ),
    );
  }
}
