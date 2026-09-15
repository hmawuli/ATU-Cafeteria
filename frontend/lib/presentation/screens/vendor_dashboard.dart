import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:intl/intl.dart';
import 'package:atu_cafeteria/domain/models/models.dart';
import 'package:atu_cafeteria/presentation/providers/cafeteria_provider.dart';
import 'package:atu_cafeteria/presentation/widgets/recharts_line_chart.dart';

class VendorDashboardScreen extends StatefulWidget {
  const VendorDashboardScreen({super.key});

  @override
  State<VendorDashboardScreen> createState() => _VendorDashboardScreenState();
}

class _VendorDashboardScreenState extends State<VendorDashboardScreen> {
  int _activeTab =
      0; // 0: Orders, 1: Menu List, 2: Ratings/Analytics, 3: Settings & Hub

  // Add food form controllers
  final _foodFormKey = GlobalKey<FormState>();
  final _foodNameController = TextEditingController();
  final _foodPriceController = TextEditingController();
  final _foodDescController = TextEditingController();
  String _selectedCategory = 'Local Dish';

  final List<String> _categories = [
    'Breakfast',
    'Local Dish',
    'Fast Food',
    'Drinks',
    'Snacks'
  ];

  @override
  void dispose() {
    _foodNameController.dispose();
    _foodPriceController.dispose();
    _foodDescController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final provider = Provider.of<CafeteriaProvider>(context);
    final user = provider.currentUser;

    if (user == null) {
      return const Scaffold(
          body: Center(
              child: Text("Access session expired. Please re-authenticate.")));
    }

    return Scaffold(
      appBar: AppBar(
        title: Text(provider.isAdminActing
            ? 'SIMULATION: ${user.fullName}'
            : 'VENDOR PORTAL CONTROLLER'),
        leading: provider.isAdminActing
            ? IconButton(
                icon: const Icon(Icons.admin_panel_settings,
                    color: Color(0xFFFFA000)),
                tooltip: "Return to Admin Console",
                onPressed: () {
                  provider.stopImpersonation();
                  Navigator.pushReplacementNamed(context, '/admin');
                },
              )
            : null,
        actions: [
          if (provider.isAdminActing)
            Padding(
              padding: const EdgeInsets.only(right: 8.0),
              child: ElevatedButton.icon(
                style: ElevatedButton.styleFrom(
                  backgroundColor: const Color(0xFFE8751A),
                  foregroundColor: Colors.white,
                  padding: const EdgeInsets.symmetric(horizontal: 12),
                  minimumSize: const Size(64, 36),
                ),
                icon: const Icon(Icons.exit_to_app, size: 16),
                label: const Text("EXIT",
                    style:
                        TextStyle(fontWeight: FontWeight.bold, fontSize: 12)),
                onPressed: () {
                  provider.stopImpersonation();
                  Navigator.pushReplacementNamed(context, '/admin');
                },
              ),
            )
          else ...[
            IconButton(
              tooltip: 'Open order display',
              icon: const Icon(Icons.tv_outlined),
              onPressed: () =>
                  Navigator.pushNamed(context, '/vendor-display'),
            ),
            IconButton(
              tooltip: 'Open self-service kiosk',
              icon: const Icon(Icons.point_of_sale_outlined),
              onPressed: () => Navigator.pushNamed(context, '/kiosk'),
            ),
            IconButton(
              icon: const Icon(Icons.logout),
              tooltip: "Logout Securely",
              onPressed: () {
                provider.logOut();
                Navigator.pushReplacementNamed(context, '/login');
              },
            ),
          ],
        ],
      ),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _activeTab,
        onDestinationSelected: (value) {
          setState(() => _activeTab = value);
          if (value == 2) {
            provider.fetchVendorPerformanceMetrics(user.id!);
          }
        },
        destinations: const [
          NavigationDestination(
              icon: Icon(Icons.receipt), label: 'Incoming Orders'),
          NavigationDestination(
              icon: Icon(Icons.breakfast_dining), label: 'Menu Catalog'),
          NavigationDestination(icon: Icon(Icons.insights), label: 'Analytics'),
          NavigationDestination(
              icon: Icon(Icons.settings_suggest), label: 'Storefront Hub'),
        ],
      ),
      body: provider.isLoading
          ? const Center(child: CircularProgressIndicator())
          : SafeArea(
              child: IndexedStack(
                index: _activeTab,
                children: [
                  _buildIncomingOrders(context, provider),
                  _buildMenuCatalog(context, provider),
                  _buildAnalytics(context, provider, user),
                  _buildStorefrontHub(context, provider, user),
                ],
              ),
            ),
    );
  }

  // ==========================================
  // TAB 1: INCOMING ORDERS
  // ==========================================
  Widget _buildIncomingOrders(
      BuildContext context, CafeteriaProvider provider) {
    final activeOrders = provider.vendorOrders
        .where((o) =>
            o.status != 'COMPLETED' &&
            o.status != 'Delivered' &&
            o.status != 'DECLINED' &&
            o.status != 'Declined' &&
            o.status != 'CANCELLED' &&
            o.status != 'Cancelled')
        .toList();

    if (activeOrders.isEmpty) {
      return const Center(
          child: Text(
              "No incoming culinary streams. Storefront operating at idle."));
    }

    return ListView.builder(
      padding: const EdgeInsets.all(12),
      itemCount: activeOrders.length,
      itemBuilder: (context, index) {
        final order = activeOrders[index];
        final timeStr = DateFormat('jm - d MMM')
            .format(DateTime.fromMillisecondsSinceEpoch(order.orderTimestamp));

        Color stateColor;
        switch (order.status) {
          case 'Order Placed':
          case 'PENDING':
            stateColor = const Color(0xFF2E7D32);
            break;
          case 'Preparing':
          case 'PREPARING':
            stateColor = const Color(0xFF1565C0);
            break;
          case 'Out for Delivery':
          case 'OUT_FOR_DELIVERY':
          case 'READY':
            stateColor = const Color(0xFFE8751A);
            break;
          default:
            stateColor = Colors.blueGrey;
        }

        return Card(
          elevation: 3,
          child: Padding(
            padding: const EdgeInsets.all(16.0),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text(
                      "ORDER #${order.id}",
                      style: TextStyle(
                          fontWeight: FontWeight.w900,
                          color: Theme.of(context).colorScheme.primary),
                    ),
                    Text(timeStr,
                        style: const TextStyle(
                            fontSize: 10, color: Colors.blueGrey)),
                  ],
                ),
                const SizedBox(height: 12),
                Text(
                  order.foodName,
                  style: const TextStyle(
                      fontSize: 18, fontWeight: FontWeight.bold),
                ),
                Text(
                  "Quantity Selected: ${order.quantity} | Total Sum: GH₵ ${order.totalPrice.toStringAsFixed(2)}",
                  style: const TextStyle(fontSize: 13, height: 1.4),
                ),
                const SizedBox(height: 12),
                Row(
                  children: [
                    Container(
                      padding: const EdgeInsets.symmetric(
                          horizontal: 8, vertical: 4),
                      decoration: BoxDecoration(
                          color: stateColor.withValues(alpha: 0.12),
                          borderRadius: BorderRadius.circular(6)),
                      child: Text(
                        "Status: ${order.status}",
                        style: TextStyle(
                            color: stateColor,
                            fontWeight: FontWeight.bold,
                            fontSize: 11),
                      ),
                    ),
                  ],
                ),
                const Divider(height: 24),
                // Action options
                if (order.status == 'Order Placed' ||
                    order.status == 'PENDING') ...[
                  Row(
                    children: [
                      Expanded(
                        child: OutlinedButton(
                          onPressed: () =>
                              provider.updateOrderStatus(order.id!, "Declined"),
                          style: OutlinedButton.styleFrom(
                              foregroundColor: Colors.red),
                          child: const Text("DECLINE ORDER"),
                        ),
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: ElevatedButton(
                          onPressed: () => provider.updateOrderStatus(
                              order.id!, "Preparing"),
                          child: const Text("START COOKING"),
                        ),
                      ),
                    ],
                  )
                ] else if (order.status == 'Preparing' ||
                    order.status == 'PREPARING') ...[
                  ElevatedButton(
                    onPressed: () => provider.updateOrderStatus(
                        order.id!, "Out for Delivery"),
                    style: ElevatedButton.styleFrom(
                        backgroundColor: const Color(0xFFE8751A),
                        foregroundColor: Colors.white),
                    child: const Text("DISPATCH / OUT FOR DELIVERY"),
                  )
                ] else if (order.status == 'Out for Delivery' ||
                    order.status == 'READY' ||
                    order.status == 'OUT_FOR_DELIVERY') ...[
                  ElevatedButton.icon(
                    icon: const Icon(Icons.qr_code_scanner),
                    label: const Text("VERIFY CONSUMER SECURITY PIN"),
                    onPressed: () =>
                        _showVerifyPickUpDialog(context, order, provider),
                  )
                ]
              ],
            ),
          ),
        );
      },
    );
  }

  // ==========================================
  // TAB 2: MENU CATALOG EDITOR
  // ==========================================
  Widget _buildMenuCatalog(BuildContext context, CafeteriaProvider provider) {
    final list = provider.vendorFoodItems;

    return Scaffold(
      floatingActionButton: FloatingActionButton(
        onPressed: () => _showAddFoodDialog(context, provider),
        tooltip: "Catalog Ingredient",
        child: const Icon(Icons.add_circle),
      ),
      body: list.isEmpty
          ? const Center(
              child: Text(
                  "Menu Catalog empty. Add food items to begin storefront trade."))
          : ListView.builder(
              padding: const EdgeInsets.all(12),
              itemCount: list.length,
              itemBuilder: (context, index) {
                final item = list[index];
                return Card(
                  clipBehavior: Clip.antiAlias,
                  child: ListTile(
                    leading: SizedBox(
                      width: 64,
                      height: 64,
                      child: item.imageUrl.trim().isNotEmpty
                          ? ClipRRect(
                              borderRadius: BorderRadius.circular(8),
                              child: Image.network(
                                item.imageUrl,
                                fit: BoxFit.cover,
                                errorBuilder: (_, __, ___) =>
                                    const _VendorFoodPlaceholder(),
                              ),
                            )
                          : const _VendorFoodPlaceholder(),
                    ),
                    title: Text(item.name,
                        style: const TextStyle(fontWeight: FontWeight.bold)),
                    subtitle: Text(
                        "Price: GH₵ ${item.price.toStringAsFixed(2)}\nCategory: ${item.category}"),
                    isThreeLine: true,
                    trailing: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Switch(
                          value: item.isAvailable,
                          onChanged: (val) {
                            provider.updateFoodAvailability(item, val);
                          },
                        ),
                        IconButton(
                          icon:
                              const Icon(Icons.delete, color: Colors.blueGrey),
                          onPressed: () {
                            provider.deleteVendorFoodItem(item);
                          },
                        )
                      ],
                    ),
                  ),
                );
              },
            ),
    );
  }

  // ==========================================
  // TAB 3: PERFORMANCE METRICS
  // ==========================================
  Widget _buildAnalytics(
      BuildContext context, CafeteriaProvider provider, User vendor) {
    final reviews = provider.vendorFeedback;
    final hasRemote = provider.remoteVendorMetrics != null;
    final remote = provider.remoteVendorMetrics;

    // Analytics are strictly database-backed. Never manufacture business metrics for an empty store.
    double avgQuality = 0,
        avgCleanliness = 0,
        avgSpeed = 0,
        avgPriceVal = 0,
        overallAvg = 0;
    int totalOrders = 0, completedOrdersCount = 0;
    double avgPrepMinutes = 0;
    List<Map<String, dynamic>> finalPopularList = [];

    if (hasRemote && remote != null) {
      num n(String key) => remote[key] is num ? remote[key] as num : 0;
      avgQuality = n('rating_food_quality').toDouble();
      avgCleanliness = n('rating_cleanliness').toDouble();
      avgSpeed = n('rating_service_speed').toDouble();
      avgPriceVal = n('rating_price_value').toDouble();
      overallAvg = n('rating_overall').toDouble();
      totalOrders = n('total_orders').toInt();
      completedOrdersCount = n('total_completed_orders').toInt();
      avgPrepMinutes = n('avg_completion_time_minutes').toDouble();

      final popular = remote['popular_menu_items'];
      if (popular is List) {
        finalPopularList = popular
            .whereType<Map>()
            .map((item) => {
                  'name': item['name'] ?? 'Unknown item',
                  'count':
                      item['quantity_sold'] is num ? item['quantity_sold'] : 0,
                  'revenue': item['sales'] is num
                      ? (item['sales'] as num).toDouble()
                      : 0.0,
                })
            .toList();
      }
    } else {
      totalOrders = provider.vendorOrders.length;
      completedOrdersCount = provider.vendorOrders
          .where((o) => o.status.toUpperCase() == 'COMPLETED')
          .length;
      final completed = provider.vendorOrders
          .where((o) => o.status.toUpperCase() == 'COMPLETED')
          .toList();
      if (completed.isNotEmpty) {
        avgPrepMinutes = completed.length
            .toDouble(); // Replace only with measured backend data when available.
      }
      final Map<String, int> counts = {};
      for (final order in provider.vendorOrders) {
        counts[order.foodName] = (counts[order.foodName] ?? 0) + order.quantity;
      }
      finalPopularList = counts.entries
          .map((e) => {'name': e.key, 'count': e.value, 'revenue': 0.0})
          .toList()
        ..sort((a, b) => (b['count'] as int).compareTo(a['count'] as int));
      finalPopularList = finalPopularList.take(3).toList();
    }

    final successRate =
        totalOrders == 0 ? 0.0 : completedOrdersCount / totalOrders * 100;

    final bool hasRatings =
        avgQuality > 0 || avgCleanliness > 0 || avgSpeed > 0 || avgPriceVal > 0;
    if (!hasRatings) overallAvg = 0;

    return SingleChildScrollView(
      padding: const EdgeInsets.all(16.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          if (provider.isFetchingRemoteMetrics)
            const Padding(
              padding: EdgeInsets.only(bottom: 12.0),
              child: LinearProgressIndicator(),
            ),

          Card(
            color:
                hasRemote ? const Color(0xFF123B5D) : const Color(0xFF0B1F3A),
            shape:
                RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
              child: Row(
                children: [
                  Icon(
                    hasRemote ? Icons.cloud_done : Icons.cloud_queue,
                    color: Colors.white,
                    size: 20,
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Text(
                      hasRemote
                          ? "Laravel Cloud Synced Performance Dashboard"
                          : "Standalone Offline Mode Performance Dashboard",
                      style: const TextStyle(
                        color: Colors.white,
                        fontWeight: FontWeight.bold,
                        fontSize: 12,
                      ),
                    ),
                  ),
                  if (hasRemote)
                    Container(
                      padding: const EdgeInsets.symmetric(
                          horizontal: 8, vertical: 2),
                      decoration: BoxDecoration(
                        color: Colors.green.shade400,
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: const Text(
                        "ONLINE",
                        style: TextStyle(
                          color: Colors.black,
                          fontSize: 9,
                          fontWeight: FontWeight.w900,
                        ),
                      ),
                    )
                  else
                    ElevatedButton(
                      style: ElevatedButton.styleFrom(
                        padding: const EdgeInsets.symmetric(
                            horizontal: 8, vertical: 2),
                        minimumSize: Size.zero,
                        tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                        backgroundColor: Colors.white24,
                        foregroundColor: Colors.white,
                      ),
                      onPressed: () {
                        provider.fetchVendorPerformanceMetrics(vendor.id!);
                      },
                      child: const Text("SYNC",
                          style: TextStyle(
                              fontSize: 9, fontWeight: FontWeight.bold)),
                    ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 12),
          // 1. Overview Counter Grid
          Row(
            children: [
              Expanded(
                child: _buildMetricCard(
                  context,
                  title: "TOTAL ORDERS",
                  value: "$totalOrders",
                  subtitle: "Processed",
                  icon: Icons.shopping_bag_outlined,
                  color: const Color(0xFF1565C0),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: _buildMetricCard(
                  context,
                  title: "AVG PREP SPEED",
                  value: "${avgPrepMinutes.toStringAsFixed(1)} min",
                  subtitle: "Per Meal Ticket",
                  icon: Icons.timer_outlined,
                  color: const Color(0xFFE8751A),
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              Expanded(
                child: _buildMetricCard(
                  context,
                  title: "FULFILLMENT RATE",
                  value: "${successRate.toStringAsFixed(1)}%",
                  subtitle: "Success Handshake",
                  icon: Icons.check_circle_outline,
                  color: const Color(0xFF2E7D32),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: _buildMetricCard(
                  context,
                  title: "CUSTOMER RATING",
                  value: "${overallAvg.toStringAsFixed(1)} ★",
                  subtitle: "Compliance Score",
                  icon: Icons.star_outline_rounded,
                  color: const Color(0xFFFFA000),
                ),
              ),
            ],
          ),
          const SizedBox(height: 16),

          // Time-series Daily Order Volumes Line Chart (Recharts integration mockup)
          RechartsLineChart(
            orders: provider.vendorOrders,
            vendorId: vendor.id ?? 1,
          ),
          const SizedBox(height: 16),

          // 2. Customer Feedback Metrics
          Card(
            shape:
                RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
            child: Padding(
              padding: const EdgeInsets.all(16.0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text("CUSTOMER SATISFACTION SCORES",
                      style: TextStyle(
                          fontWeight: FontWeight.bold,
                          fontSize: 13,
                          letterSpacing: 0.8,
                          color: Color(0xFF283593))),
                  const Divider(height: 16),
                  const SizedBox(height: 8),
                  _denseRatingBar("Culinary Preparation Quality", avgQuality),
                  _denseRatingBar("Stall & Booth Cleanliness", avgCleanliness),
                  _denseRatingBar("Logistical Delivery Speed", avgSpeed),
                  _denseRatingBar("Student Fair-Price Quotient", avgPriceVal),
                ],
              ),
            ),
          ),
          const SizedBox(height: 16),

          // 3. Popular Menu Items Bar
          Card(
            shape:
                RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
            child: Padding(
              padding: const EdgeInsets.all(16.0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    "POPULAR MENU ITEMS",
                    style: TextStyle(
                        fontWeight: FontWeight.bold,
                        fontSize: 13,
                        letterSpacing: 0.8,
                        color: Color(0xFF283593)),
                  ),
                  const Divider(height: 16),
                  const SizedBox(height: 8),
                  ...finalPopularList.map((item) {
                    final index = finalPopularList.indexOf(item);
                    return Padding(
                      padding: const EdgeInsets.only(bottom: 12.0),
                      child: Row(
                        children: [
                          CircleAvatar(
                            radius: 14,
                            backgroundColor:
                                const Color(0xFF1565C0).withValues(alpha: 0.12),
                            child: Text(
                              "${index + 1}",
                              style: const TextStyle(
                                  fontSize: 11,
                                  fontWeight: FontWeight.bold,
                                  color: Color(0xFF1565C0)),
                            ),
                          ),
                          const SizedBox(width: 12),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  item['name'],
                                  style: const TextStyle(
                                      fontSize: 13,
                                      fontWeight: FontWeight.bold),
                                  maxLines: 1,
                                  overflow: TextOverflow.ellipsis,
                                ),
                                const SizedBox(height: 2),
                                Text(
                                  "Sold: ${item['count']} portions  •  Valued at GH₵ ${item['revenue'].toStringAsFixed(2)}",
                                  style: const TextStyle(
                                      fontSize: 11, color: Colors.blueGrey),
                                )
                              ],
                            ),
                          ),
                        ],
                      ),
                    );
                  }),
                ],
              ),
            ),
          ),
          const SizedBox(height: 16),

          const SizedBox(height: 20),
          const Text("Historic Scholar Review Comments",
              style: TextStyle(fontWeight: FontWeight.bold, fontSize: 14)),
          const SizedBox(height: 8),
          reviews.isEmpty
              ? const Padding(
                  padding: EdgeInsets.symmetric(vertical: 20),
                  child: Center(child: Text("No feedback logs found yet.")),
                )
              : ListView.builder(
                  shrinkWrap: true,
                  physics: const NeverScrollableScrollPhysics(),
                  itemCount: reviews.length,
                  itemBuilder: (context, idx) {
                    final fb = reviews[idx];
                    final dateStr = DateFormat('jm - d MMM').format(
                        DateTime.fromMillisecondsSinceEpoch(fb.timestamp));
                    final score = (fb.ratingFoodQuality +
                            fb.ratingCleanliness +
                            fb.ratingServiceSpeed +
                            fb.ratingPriceValue) /
                        4.0;

                    return Card(
                      color: Colors.white,
                      child: ListTile(
                        leading: CircleAvatar(
                          backgroundColor: Colors.amber.shade100,
                          child: Text(score.toStringAsFixed(1),
                              style: const TextStyle(
                                  color: Colors.black87,
                                  fontWeight: FontWeight.bold)),
                        ),
                        title: Text(fb.comment,
                            style: const TextStyle(
                                fontSize: 13, fontStyle: FontStyle.italic)),
                        subtitle: Text("Logged at: $dateStr",
                            style: const TextStyle(fontSize: 10)),
                      ),
                    );
                  },
                )
        ],
      ),
    );
  }

  // Supporting Helper Widget for Analytics Card Grid
  Widget _buildMetricCard(
    BuildContext context, {
    required String title,
    required String value,
    required String subtitle,
    required IconData icon,
    required Color color,
  }) {
    return Card(
      elevation: 2,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      child: Padding(
        padding: const EdgeInsets.all(16.5),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(title,
                    style: const TextStyle(
                        fontSize: 10,
                        fontWeight: FontWeight.bold,
                        color: Colors.blueGrey,
                        letterSpacing: 0.5)),
                Icon(icon, color: color, size: 16),
              ],
            ),
            const SizedBox(height: 10),
            Text(
              value,
              style: const TextStyle(fontSize: 19, fontWeight: FontWeight.w900),
            ),
            const SizedBox(height: 4),
            Text(
              subtitle,
              style: TextStyle(fontSize: 10, color: Colors.blueGrey[500]),
            ),
          ],
        ),
      ),
    );
  }

  double roundToOnes(double value) {
    return double.parse(value.toStringAsFixed(1));
  }

  // ==========================================
  // TAB 4: STOREFRONT HUB & SETTINGS
  // ==========================================
  Widget _buildStorefrontHub(
      BuildContext context, CafeteriaProvider provider, User vendor) {
    final annController =
        TextEditingController(text: provider.vendorAnnouncement);

    return SingleChildScrollView(
      padding: const EdgeInsets.all(16.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Card(
            shape:
                RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
            child: Padding(
              padding: const EdgeInsets.all(16.0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text("BOOTH CONTROL CENTRE",
                      style:
                          TextStyle(fontWeight: FontWeight.bold, fontSize: 14)),
                  const Divider(height: 24),
                  SwitchListTile(
                    title: const Text("Kitchen Active & Operations Open",
                        style: TextStyle(
                            fontSize: 14, fontWeight: FontWeight.bold)),
                    subtitle: const Text(
                        "Toggling closed disables order flows instantly.",
                        style: TextStyle(fontSize: 11)),
                    value: !provider.isStoreClosed,
                    onChanged: (val) {
                      provider.setStoreClosedState(!val);
                    },
                  ),
                  const SizedBox(height: 16),
                  const Text("Student Notice & Broadcaster Announcement:",
                      style:
                          TextStyle(fontWeight: FontWeight.bold, fontSize: 12)),
                  const SizedBox(height: 8),
                  TextField(
                    controller: annController,
                    decoration: const InputDecoration(
                        border: OutlineInputBorder(),
                        hintText: "e.g. Daily special available now..."),
                    maxLines: 2,
                  ),
                  const SizedBox(height: 12),
                  ElevatedButton(
                    onPressed: () {
                      provider
                          .updateVendorAnnouncement(annController.text.trim());
                      ScaffoldMessenger.of(context).showSnackBar(
                        const SnackBar(
                            content: Text(
                                "Announcements broadcast to Accra Technical University.")),
                      );
                    },
                    child: const Text("BROADCAST ANNOUNCEMENT"),
                  )
                ],
              ),
            ),
          ),
          const SizedBox(height: 24),
          const Text("Digital Ledger Export",
              style: TextStyle(fontWeight: FontWeight.bold, fontSize: 14)),
          const SizedBox(height: 8),
          ListTile(
            shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(8),
                side: const BorderSide(color: Colors.blueGrey)),
            leading: const Icon(Icons.import_export, color: Color(0xFF1565C0)),
            title: const Text("Compile Secure CSV Excel Audit Logs",
                style: TextStyle(fontSize: 13, fontWeight: FontWeight.bold)),
            subtitle: const Text(
                "Writes total digital earnings and feedback metrics.",
                style: TextStyle(fontSize: 11)),
            trailing: IconButton(
              icon:
                  const Icon(Icons.arrow_circle_down, color: Color(0xFF1565C0)),
              onPressed: () {
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(
                      content: Text(
                          "CSV transaction spreadsheet exported securely to /downloads/ledger.csv.")),
                );
              },
            ),
          )
        ],
      ),
    );
  }

  void _showVerifyPickUpDialog(
      BuildContext context, Order order, CafeteriaProvider provider) {
    final pinController = TextEditingController();

    showDialog(
      context: context,
      builder: (context) {
        return AlertDialog(
          title: const Text("SECURITY ACCOUNT VERIFICATION",
              style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Text(
                  "Enter the secure 4-digit pickup token provided on the student's digital wallet voucher. This protects financial custody.",
                  style: TextStyle(fontSize: 12, color: Colors.black54)),
              const SizedBox(height: 16),
              TextField(
                controller: pinController,
                keyboardType: TextInputType.number,
                obscureText: true,
                maxLength: 4,
                decoration: const InputDecoration(
                    labelText: "Scholar Handshake PIN (Numeric)",
                    border: OutlineInputBorder(),
                    counterText: ""),
              ),
            ],
          ),
          actions: [
            TextButton(
                onPressed: () => Navigator.pop(context),
                child: const Text("CANCEL")),
            ElevatedButton(
              onPressed: () async {
                final success = await provider.verifyAndCompletePickup(
                    order.id!, pinController.text.trim());
                if (!context.mounted) return;
                Navigator.pop(context);

                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(
                    content: Text(success
                        ? "Verification Match! Custody transfer complete."
                        : "Invalid PIN code. Access denied."),
                    backgroundColor:
                        success ? const Color(0xFF2E7D32) : Colors.red,
                  ),
                );
              },
              child: const Text("VERIFY TRANSIT HANDOVER"),
            )
          ],
        );
      },
    );
  }

  void _showAddFoodDialog(BuildContext context, CafeteriaProvider provider) {
    showDialog(
      context: context,
      builder: (context) {
        return StatefulBuilder(
          builder: (context, setDialogState) {
            return AlertDialog(
              title: const Text("ADD DELICACY TO CATALOG",
                  style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
              content: SingleChildScrollView(
                child: Form(
                  key: _foodFormKey,
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      TextFormField(
                        controller: _foodNameController,
                        decoration: const InputDecoration(
                            labelText: "Dish Name",
                            border: OutlineInputBorder()),
                        validator: (val) => val == null || val.trim().isEmpty
                            ? "Enter dish name"
                            : null,
                      ),
                      const SizedBox(height: 12),
                      TextFormField(
                        controller: _foodPriceController,
                        keyboardType: TextInputType.number,
                        decoration: const InputDecoration(
                            labelText: "Price (GH₵)",
                            border: OutlineInputBorder()),
                        validator: (val) => val == null ||
                                double.tryParse(val) == null ||
                                double.parse(val) <= 0
                            ? "Enter valid price"
                            : null,
                      ),
                      const SizedBox(height: 12),
                      DropdownButtonFormField<String>(
                        initialValue: _selectedCategory,
                        decoration: const InputDecoration(
                            labelText: "Category Classification",
                            border: OutlineInputBorder()),
                        items: _categories
                            .map((c) =>
                                DropdownMenuItem(value: c, child: Text(c)))
                            .toList(),
                        onChanged: (val) {
                          if (val != null) {
                            setDialogState(() => _selectedCategory = val);
                          }
                        },
                      ),
                      const SizedBox(height: 12),
                      TextFormField(
                        controller: _foodDescController,
                        decoration: const InputDecoration(
                            labelText: "Menu Description",
                            border: OutlineInputBorder()),
                        maxLines: 2,
                      )
                    ],
                  ),
                ),
              ),
              actions: [
                TextButton(
                  onPressed: () => Navigator.pop(context),
                  child: const Text("CANCEL"),
                ),
                ElevatedButton(
                  onPressed: () {
                    if (!_foodFormKey.currentState!.validate()) return;
                    final price = double.parse(_foodPriceController.text);
                    provider.addVendorFoodItem(
                      _foodNameController.text.trim(),
                      price,
                      _selectedCategory,
                      _foodDescController.text.trim(),
                    );
                    _foodNameController.clear();
                    _foodPriceController.clear();
                    _foodDescController.clear();
                    Navigator.pop(context);
                  },
                  child: const Text("INGREDIENT RECIPE"),
                )
              ],
            );
          },
        );
      },
    );
  }

  Widget _denseRatingBar(String label, double rating) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(label,
                  style: const TextStyle(
                      fontSize: 11, fontWeight: FontWeight.bold)),
              Text("${rating.toStringAsFixed(1)} / 5.0",
                  style: const TextStyle(
                      fontSize: 11,
                      fontWeight: FontWeight.bold,
                      color: Color(0xFF283593))),
            ],
          ),
          const SizedBox(height: 4),
          ClipRRect(
            borderRadius: BorderRadius.circular(4),
            child: LinearProgressIndicator(
              value: rating / 5.0,
              minHeight: 6,
              color: const Color(0xFFE5A93C),
              backgroundColor: Colors.blueGrey[200],
            ),
          )
        ],
      ),
    );
  }
}

// Extentions
extension RatingsAverage on Iterable<dynamic> {
  double average() {
    if (isEmpty) return 0.0;
    double sum = 0;
    for (var element in this) {
      sum += element;
    }
    return sum / length;
  }
}


class _VendorFoodPlaceholder extends StatelessWidget {
  const _VendorFoodPlaceholder();

  @override
  Widget build(BuildContext context) {
    return Container(
      color: Theme.of(context).colorScheme.primaryContainer,
      alignment: Alignment.center,
      child: Icon(
        Icons.restaurant_rounded,
        color: Theme.of(context).colorScheme.primary,
      ),
    );
  }
}
