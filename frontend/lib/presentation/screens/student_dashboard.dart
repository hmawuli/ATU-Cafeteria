import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:intl/intl.dart';
import 'package:atu_cafeteria/domain/models/models.dart' as models;
import 'package:atu_cafeteria/presentation/providers/cafeteria_provider.dart';
import 'package:atu_cafeteria/presentation/providers/cart_provider.dart';
import 'package:atu_cafeteria/presentation/widgets/professional_widgets.dart';

class StudentDashboardScreen extends StatefulWidget {
  const StudentDashboardScreen({super.key});

  @override
  State<StudentDashboardScreen> createState() => _StudentDashboardScreenState();
}

class _StudentDashboardScreenState extends State<StudentDashboardScreen> {
  int _activeTab = 0; // 0: Browse Food, 1: Track Orders, 2: Smart Wallet & ID
  String _selectedCategory = 'All';
  final _searchController = TextEditingController();
  String _searchQuery = '';

  final List<String> _categories = [
    'All',
    'Lunch Specials',
    'Traditional',
    'Drinks',
    'Snacks'
  ];

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
            : 'STUDENT CENTRAL HUB'),
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
            Consumer<CartProvider>(
              builder: (context, cart, _) => Badge(
                isLabelVisible: cart.itemCount > 0,
                label: Text('${cart.itemCount}'),
                child: IconButton(
                  icon: const Icon(Icons.shopping_cart_outlined),
                  tooltip: 'View cart',
                  onPressed: () => Navigator.pushNamed(context, '/cart'),
                ),
              ),
            ),
            IconButton(
              icon: const Icon(Icons.logout),
              tooltip: "Logout Securely",
              onPressed: () {
                provider.logOut();
                Navigator.pushReplacementNamed(context, '/login');
              },
            ),
          ]
        ],
      ),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _activeTab,
        onDestinationSelected: (value) => setState(() => _activeTab = value),
        destinations: const [
          NavigationDestination(
              icon: Icon(Icons.restaurant_menu), label: 'Browse Food'),
          NavigationDestination(
              icon: Icon(Icons.receipt_long), label: 'Track Orders'),
          NavigationDestination(icon: Icon(Icons.wallet), label: 'Wallet & ID'),
        ],
      ),
      body: provider.isLoading
          ? const Center(child: CircularProgressIndicator())
          : SafeArea(
              child: IndexedStack(
                index: _activeTab,
                children: [
                  _buildBrowseMenu(context, provider),
                  _buildTrackOrders(context, provider),
                  _buildWalletAndId(context, provider, user),
                ],
              ),
            ),
    );
  }

  // ==========================================
  // TAB 1: BROWSE MENU
  // ==========================================
  Widget _buildBrowseMenu(BuildContext context, CafeteriaProvider provider) {
    var list = provider.allFoodItems.where((item) => item.isAvailable).toList();
    if (_selectedCategory != 'All') {
      // The API uses descriptive category names while the student UI uses
      // short, friendly labels. Match both forms so categories never appear
      // empty simply because their display names differ.
      final normalizedCategory = _selectedCategory.toLowerCase().trim();
      list = list.where((item) {
        final category = item.category.toLowerCase().trim();
        switch (normalizedCategory) {
          case 'lunch specials':
            return category == 'lunch specials' ||
                category.contains('lunch') ||
                category.contains('main dish');
          case 'traditional':
            return category == 'traditional' ||
                category.contains('traditional');
          case 'drinks':
            return category == 'drinks' ||
                category.contains('beverage') ||
                category.contains('drink');
          case 'snacks':
            return category == 'snacks' ||
                category.contains('snack') ||
                category.contains('pastr') ||
                category.contains('fast food');
          default:
            return category == normalizedCategory;
        }
      }).toList();
    }
    if (_searchQuery.isNotEmpty) {
      final q = _searchQuery.toLowerCase();
      list = list
          .where((item) =>
              item.name.toLowerCase().contains(q) ||
              item.description.toLowerCase().contains(q) ||
              item.category.toLowerCase().contains(q))
          .toList();
    }

    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(12, 14, 12, 4),
          child: Row(children: [
            Expanded(
                child: Text('Good food, right on campus.',
                    style: Theme.of(context)
                        .textTheme
                        .headlineSmall
                        ?.copyWith(fontWeight: FontWeight.w900))),
            IconButton(
                onPressed: () => provider.refreshAllData(),
                icon: const Icon(Icons.refresh_rounded),
                tooltip: 'Refresh'),
          ]),
        ),
        Padding(
          padding: const EdgeInsets.fromLTRB(12, 8, 12, 4),
          child: AppSearchField(
              controller: _searchController,
              hint: 'Search meals, categories...',
              onChanged: (v) => setState(() => _searchQuery = v.trim())),
        ),
        // Announcement bar
        Container(
          width: double.infinity,
          color:
              Theme.of(context).colorScheme.secondary.withValues(alpha: 0.15),
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
          child: Row(
            children: [
              Icon(Icons.campaign,
                  color: Theme.of(context).colorScheme.primary),
              const SizedBox(width: 12),
              Expanded(
                child: Text(
                  provider.vendorAnnouncement,
                  style: const TextStyle(
                      fontWeight: FontWeight.w600,
                      fontSize: 12,
                      color: Colors.black87),
                ),
              ),
            ],
          ),
        ),

        // Live stock alerts feed widget
        if (provider.liveAlerts.isNotEmpty)
          Container(
            margin: const EdgeInsets.only(left: 12, right: 12, top: 10),
            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
            decoration: BoxDecoration(
              color: Colors.red.shade50.withValues(alpha: 0.4),
              borderRadius: BorderRadius.circular(10),
              border: Border.all(color: Colors.red.shade100),
            ),
            child: Row(
              children: [
                Container(
                  padding:
                      const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                  decoration: BoxDecoration(
                    color: const Color(0xFFC62828),
                    borderRadius: BorderRadius.circular(4),
                  ),
                  child: const Text(
                    "LIVE ALERT",
                    style: TextStyle(
                      color: Colors.white,
                      fontSize: 8,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(
                    provider.liveAlerts.first,
                    style: const TextStyle(
                      fontSize: 11,
                      fontWeight: FontWeight.w600,
                      color: Colors.black87,
                    ),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                ),
                if (provider.liveAlerts.length > 1) ...[
                  const SizedBox(width: 4),
                  InkWell(
                    onTap: () {
                      showDialog(
                        context: context,
                        builder: (context) => AlertDialog(
                          title: const Row(
                            children: [
                              Icon(Icons.radar, color: Color(0xFF1565C0)),
                              SizedBox(width: 8),
                              Text("Real-Time Stock Stream"),
                            ],
                          ),
                          content: SizedBox(
                            width: double.maxFinite,
                            child: ListView.builder(
                              shrinkWrap: true,
                              itemCount: provider.liveAlerts.length,
                              itemBuilder: (context, idx) => ListTile(
                                leading:
                                    const Icon(Icons.info_outline, size: 18),
                                title: Text(
                                  provider.liveAlerts[idx],
                                  style: const TextStyle(fontSize: 13),
                                ),
                              ),
                            ),
                          ),
                          actions: [
                            TextButton(
                              onPressed: () => Navigator.pop(context),
                              child: const Text("CLOSE"),
                            )
                          ],
                        ),
                      );
                    },
                    child: Text(
                      "+${provider.liveAlerts.length - 1} more",
                      style: const TextStyle(
                        fontSize: 9,
                        color: Color(0xFF1565C0),
                        fontWeight: FontWeight.bold,
                        decoration: TextDecoration.underline,
                      ),
                    ),
                  )
                ]
              ],
            ),
          ),

        // Categories selector
        Padding(
          padding: const EdgeInsets.symmetric(vertical: 12.0),
          child: SizedBox(
            height: 40,
            child: ListView.builder(
              scrollDirection: Axis.horizontal,
              itemCount: _categories.length,
              padding: const EdgeInsets.symmetric(horizontal: 12),
              itemBuilder: (context, index) {
                final cat = _categories[index];
                final isSelected = cat == _selectedCategory;
                return Padding(
                  padding: const EdgeInsets.only(right: 8.0),
                  child: FilterChip(
                    selected: isSelected,
                    label: Text(cat),
                    selectedColor: Theme.of(context)
                        .colorScheme
                        .primary
                        .withValues(alpha: 0.2),
                    checkmarkColor: Theme.of(context).colorScheme.primary,
                    onSelected: (val) {
                      setState(() => _selectedCategory = cat);
                    },
                  ),
                );
              },
            ),
          ),
        ),

        // Food Grid / List
        Expanded(
          child: list.isEmpty
              ? AppEmptyState(
                  icon: Icons.restaurant_rounded,
                  title: "No meals found",
                  message: _searchQuery.isEmpty
                      ? "There are no meals available in this category right now."
                      : "Try another meal name or category.")
              : ListView.builder(
                  padding: const EdgeInsets.all(12),
                  itemCount: list.length,
                  itemBuilder: (context, index) {
                    final item = list[index];

                    return FoodCard(
                      name: item.name,
                      description: item.description,
                      category: item.category,
                      imageUrl: item.imageUrl,
                      price: item.price,
                      available: item.isAvailable,
                      onAdd: () {
                        context.read<CartProvider>().add(item);
                        ScaffoldMessenger.of(context).showSnackBar(
                          SnackBar(content: Text('${item.name} added to cart')),
                        );
                      },
                    );
                  },
                ),
        )
      ],
    );
  }

  // ==========================================
  // TAB 2: TRACK ACTIVE ORDERS
  // ==========================================
  Widget _buildTrackOrders(BuildContext context, CafeteriaProvider provider) {
    final list = provider.customerOrders;
    final isOffline = provider.isOrderCacheOffline;

    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(12.0, 12.0, 12.0, 4.0),
          child: Card(
            color:
                isOffline ? const Color(0xFF0B1F3A) : const Color(0xFF123B5D),
            shape:
                RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
              child: Row(
                children: [
                  Icon(
                    isOffline ? Icons.wifi_off : Icons.cloud_done,
                    color: Colors.white,
                    size: 20,
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Text(
                      isOffline
                          ? "Campus network unstable. Displaying offline cached order history."
                          : "Orders synchronized securely with ATU Cloud.",
                      style: const TextStyle(
                        color: Colors.white,
                        fontWeight: FontWeight.bold,
                        fontSize: 12,
                      ),
                    ),
                  ),
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
                      provider.refreshAllData();
                    },
                    child: const Text("RETRY",
                        style: TextStyle(
                            fontSize: 9, fontWeight: FontWeight.bold)),
                  ),
                ],
              ),
            ),
          ),
        ),
        Expanded(
          child: list.isEmpty
              ? const Center(
                  child: Text("You have not initialized any orders yet."))
              : ListView.builder(
                  padding:
                      const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                  itemCount: list.length,
                  itemBuilder: (context, index) {
                    final order = list[index];
                    final timeStr = DateFormat('h:mm a - d MMM').format(
                        DateTime.fromMillisecondsSinceEpoch(
                            order.orderTimestamp));
                    final orderColor = _statusColor(order.status);

                    return Card(
                      elevation: 2,
                      shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(12)),
                      child: ExpansionTile(
                        title: Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            Expanded(
                              child: Text(
                                order.foodName,
                                style: const TextStyle(
                                    fontWeight: FontWeight.bold, fontSize: 15),
                              ),
                            ),
                            Container(
                              padding: const EdgeInsets.symmetric(
                                  vertical: 4, horizontal: 8),
                              decoration: BoxDecoration(
                                  color: orderColor.withValues(alpha: 0.12),
                                  borderRadius: BorderRadius.circular(8)),
                              child: Text(
                                order.status,
                                style: TextStyle(
                                    color: orderColor,
                                    fontWeight: FontWeight.bold,
                                    fontSize: 11),
                              ),
                            )
                          ],
                        ),
                        subtitle: Text(
                          "Qty: ${order.quantity} | Total: GH₵ ${order.totalPrice.toStringAsFixed(2)}\n$timeStr",
                          style: const TextStyle(fontSize: 12, height: 1.4),
                        ),
                        children: [
                          Padding(
                            padding: const EdgeInsets.all(16.0),
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                const Divider(),
                                _buildTrackingStepper(order.status),
                                const SizedBox(height: 8),
                                Row(
                                  mainAxisAlignment:
                                      MainAxisAlignment.spaceBetween,
                                  children: [
                                    const Text(
                                      "Authentication Token / PIN:",
                                      style: TextStyle(
                                          fontWeight: FontWeight.w600,
                                          fontSize: 13),
                                    ),
                                    Container(
                                      padding: const EdgeInsets.symmetric(
                                          horizontal: 14, vertical: 6),
                                      decoration: BoxDecoration(
                                        color: Colors.blueGrey[200],
                                        borderRadius: BorderRadius.circular(6),
                                        border: Border.all(
                                            color: Colors.blueGrey[400]!),
                                      ),
                                      child: Text(
                                        order.pickupPin,
                                        style: const TextStyle(
                                          letterSpacing: 2,
                                          fontWeight: FontWeight.bold,
                                          fontSize: 16,
                                          fontFamily: 'monospace',
                                        ),
                                      ),
                                    )
                                  ],
                                ),
                                const SizedBox(height: 12),
                                Text(
                                  "Provide this secure 4-digit token to the cook upon receiving custody of order to validate pickup.",
                                  style: TextStyle(
                                      color: Colors.blueGrey[500],
                                      fontSize: 11),
                                ),
                                const SizedBox(height: 16),
                                if (order.status == 'COMPLETED') ...[
                                  ElevatedButton.icon(
                                    icon: const Icon(Icons.rate_review),
                                    label: const Text(
                                        "POST RATING & FEEDBACK AUDIT"),
                                    onPressed: () => _showFeedbackDialog(
                                        context, order, provider),
                                  )
                                ]
                              ],
                            ),
                          )
                        ],
                      ),
                    );
                  },
                ),
        ),
      ],
    );
  }

  // ==========================================
  // TAB 3: SMART WALLET & CAMPUS IDENTITY CARD
  // ==========================================
  Widget _buildWalletAndId(
      BuildContext context, CafeteriaProvider provider, models.User user) {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(16.0),
      child: Column(
        children: [
          // Simulated Ghana Academic Student NFC ID Card
          Container(
            width: double.infinity,
            padding: const EdgeInsets.all(20),
            decoration: BoxDecoration(
                gradient: const LinearGradient(
                  colors: [Color(0xFF0038A8), Color(0xFF1E56C5)],
                  begin: Alignment.topLeft,
                  end: Alignment.bottomRight,
                ),
                borderRadius: BorderRadius.circular(16),
                boxShadow: [
                  BoxShadow(
                      color: const Color(0xFF1565C0).withValues(alpha: 0.3),
                      blurRadius: 8,
                      offset: const Offset(0, 4))
                ]),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    const Text(
                      "ACCRA TECHNICAL UNIVERSITY",
                      style: TextStyle(
                          color: Colors.white,
                          fontWeight: FontWeight.bold,
                          letterSpacing: 0.8,
                          fontSize: 11),
                    ),
                    Icon(Icons.nfc,
                        color: Theme.of(context).colorScheme.secondary,
                        size: 24)
                  ],
                ),
                const SizedBox(height: 24),
                Row(
                  children: [
                    CircleAvatar(
                      radius: 28,
                      backgroundColor: Colors.white24,
                      child: Icon(Icons.school,
                          color: Theme.of(context).colorScheme.secondary,
                          size: 30),
                    ),
                    const SizedBox(width: 16),
                    Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          user.fullName,
                          style: const TextStyle(
                              color: Colors.white,
                              fontWeight: FontWeight.bold,
                              fontSize: 16),
                        ),
                        const SizedBox(height: 4),
                        Text(
                          "REF ID: ${user.info}",
                          style: const TextStyle(
                              color: Colors.white70, fontSize: 12),
                        ),
                        const SizedBox(height: 4),
                        const Text(
                          "CLASSIFICATION: SCHOLAR / STUDENT",
                          style: TextStyle(
                              color: Colors.white54,
                              fontSize: 10,
                              fontWeight: FontWeight.bold),
                        ),
                      ],
                    )
                  ],
                ),
                const SizedBox(height: 20),
                const Divider(color: Colors.white24),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    const Text("NFC SECURED CASH LESS PASS",
                        style: TextStyle(color: Colors.white60, fontSize: 9)),
                    Icon(Icons.qr_code,
                        color: Colors.white.withValues(alpha: 0.8), size: 28)
                  ],
                )
              ],
            ),
          ),
          const SizedBox(height: 20),

          // Digital Wallet Balance Card
          Card(
            shape:
                RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
            child: Padding(
              padding: const EdgeInsets.all(20.0),
              child: Column(
                children: [
                  const Text("ATU WALLET ACCOUNT BALANCE",
                      style: TextStyle(
                          fontWeight: FontWeight.bold,
                          fontSize: 12,
                          letterSpacing: 1,
                          color: Colors.black54)),
                  const SizedBox(height: 8),
                  Text(
                    "GH₵ ${provider.studentWalletBalance.toStringAsFixed(2)}",
                    style: TextStyle(
                        fontSize: 32,
                        fontWeight: FontWeight.w900,
                        color: Theme.of(context).colorScheme.primary),
                  ),
                  const SizedBox(height: 16),
                  ElevatedButton.icon(
                    icon: const Icon(Icons.add_card),
                    label: const Text("TOP-UP VIA MOMO GATEWAY"),
                    style: ElevatedButton.styleFrom(
                        backgroundColor: Theme.of(context).colorScheme.primary),
                    onPressed: () => _showTopUpDialog(context, provider),
                  )
                ],
              ),
            ),
          ),

          const SizedBox(height: 16),
          // Audit Log / Wallet History
          const Padding(
            padding: EdgeInsets.symmetric(horizontal: 4.0),
            child: Align(
              alignment: Alignment.centerLeft,
              child: Text("Wallet & Session History logs",
                  style: TextStyle(
                      fontWeight: FontWeight.bold,
                      color: Colors.black87,
                      fontSize: 14)),
            ),
          ),
          const SizedBox(height: 8),
          ListView.builder(
            shrinkWrap: true,
            physics: const NeverScrollableScrollPhysics(),
            itemCount:
                provider.auditLogs.where((l) => l.userId == user.id).length,
            itemBuilder: (context, idx) {
              final studentLogs =
                  provider.auditLogs.where((l) => l.userId == user.id).toList();
              final log = studentLogs[idx];
              final dateStr = DateFormat('jm - d MMM')
                  .format(DateTime.fromMillisecondsSinceEpoch(log.timestamp));

              return ListTile(
                leading: const Icon(Icons.history_toggle_off,
                    color: Color(0xFF1565C0)),
                title: Text(log.action,
                    style: const TextStyle(
                        fontWeight: FontWeight.bold, fontSize: 13)),
                subtitle:
                    Text(log.details, style: const TextStyle(fontSize: 11)),
                trailing: Text(dateStr,
                    style:
                        const TextStyle(fontSize: 10, color: Colors.blueGrey)),
              );
            },
          )
        ],
      ),
    );
  }

  void _showFeedbackDialog(
      BuildContext context, models.Order order, CafeteriaProvider provider) {
    int qualityVal = 5;
    int cleanlinessVal = 5;
    int speedVal = 5;
    int pricingVal = 5;
    final commentController = TextEditingController();

    showDialog(
      context: context,
      builder: (context) {
        return StatefulBuilder(
          builder: (context, setDialogState) {
            return AlertDialog(
              title: const Text("POST PERFORMANCE RATINGS",
                  style: TextStyle(fontWeight: FontWeight.bold, fontSize: 17)),
              content: SingleChildScrollView(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    const Text(
                        "ATU requires student reviews to secure hygiene and compliance audits.",
                        style: TextStyle(fontSize: 12, color: Colors.black54)),
                    const SizedBox(height: 12),
                    _ratingRow("Food Taste & Quality", qualityVal,
                        (v) => setDialogState(() => qualityVal = v)),
                    _ratingRow("Booth Cleanliness", cleanlinessVal,
                        (v) => setDialogState(() => cleanlinessVal = v)),
                    _ratingRow("Service Velocity / Speed", speedVal,
                        (v) => setDialogState(() => speedVal = v)),
                    _ratingRow("Price Value Fairness", pricingVal,
                        (v) => setDialogState(() => pricingVal = v)),
                    const SizedBox(height: 12),
                    TextField(
                      controller: commentController,
                      decoration: const InputDecoration(
                          labelText: "Review Comments",
                          border: OutlineInputBorder()),
                      maxLines: 2,
                    ),
                  ],
                ),
              ),
              actions: [
                TextButton(
                    onPressed: () => Navigator.pop(context),
                    child: const Text("CANCEL")),
                ElevatedButton(
                  onPressed: () async {
                    await provider.submitOrderFeedback(
                      comment: commentController.text.trim(),
                      cleanliness: cleanlinessVal,
                      orderId: order.id!,
                      quality: qualityVal,
                      speed: speedVal,
                      value: pricingVal,
                      vendorId: order.vendorId,
                    );
                    if (context.mounted) Navigator.pop(context);
                  },
                  child: const Text("SUBMIT REVIEW"),
                )
              ],
            );
          },
        );
      },
    );
  }

  void _showTopUpDialog(BuildContext context, CafeteriaProvider provider) {
    final amountController = TextEditingController();
    final phoneController = TextEditingController(text: "055-123-4567");
    bool isLoading = false;
    String? errorMsg;

    showDialog(
      context: context,
      builder: (context) {
        return StatefulBuilder(
          builder: (context, setDialogState) {
            return AlertDialog(
              title: Row(
                children: [
                  Container(
                    padding: const EdgeInsets.all(6),
                    decoration: BoxDecoration(
                      color: const Color(0xFF1565C0).withValues(alpha: 0.1),
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: const Icon(Icons.account_balance_wallet,
                        color: Color(0xFF1565C0), size: 20),
                  ),
                  const SizedBox(width: 12),
                  const Text("MoMo / Card Wallet Top-Up",
                      style:
                          TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
                ],
              ),
              content: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  const Text(
                    "Load digital funds securely to your student cafeteria wallet via Paystack gateway.",
                    style: TextStyle(fontSize: 11, color: Colors.blueGrey),
                  ),
                  const SizedBox(height: 16),
                  TextField(
                    controller: amountController,
                    keyboardType:
                        const TextInputType.numberWithOptions(decimal: true),
                    decoration: const InputDecoration(
                      labelText: "Top-Up Amount (GH₵)",
                      prefixText: "GH₵ ",
                      border: OutlineInputBorder(),
                    ),
                  ),
                  const SizedBox(height: 12),
                  TextField(
                    controller: phoneController,
                    keyboardType: TextInputType.phone,
                    decoration: const InputDecoration(
                      labelText: "Mobile Money Phone Number",
                      prefixIcon: Icon(Icons.phone_android, size: 16),
                      border: OutlineInputBorder(),
                    ),
                  ),
                  if (errorMsg case final message?) ...[
                    const SizedBox(height: 8),
                    Text(message,
                        style: const TextStyle(
                            color: Colors.red,
                            fontSize: 11,
                            fontWeight: FontWeight.bold)),
                  ]
                ],
              ),
              actions: [
                TextButton(
                  onPressed: isLoading ? null : () => Navigator.pop(context),
                  child: const Text("CANCEL"),
                ),
                ElevatedButton(
                  onPressed: isLoading
                      ? null
                      : () async {
                          final amt =
                              double.tryParse(amountController.text) ?? 0.0;
                          if (amt < 1.0) {
                            setDialogState(() {
                              errorMsg = "Minimum top-up is GH₵ 1.00";
                            });
                            return;
                          }

                          setDialogState(() {
                            isLoading = true;
                            errorMsg = null;
                          });

                          // Initialize Paystack with Laravel
                          final user = provider.currentUser;
                          final email = user != null
                              ? "${user.username}@atu.edu.gh"
                              : "student@atu.edu.gh";
                          final paystackInit =
                              await provider.initializePaystackPayment(
                            amount: amt,
                            email: email,
                            purpose: 'WALLET_TOPUP',
                          );
                          if (!context.mounted) return;

                          setDialogState(() {
                            isLoading = false;
                          });

                          if (paystackInit != null) {
                            // Close current dialog and show Paystack Checkout simulator
                            Navigator.pop(context);
                            _showPaystackSimulator(
                                context, provider, paystackInit, amt);
                          } else {
                            setDialogState(() {
                              errorMsg =
                                  "Payment gateway offline. Utilizing standalone mock bypass.";
                            });

                            // Fallback mock top-up if backend is not running
                            Future.delayed(const Duration(seconds: 1), () {
                              if (!context.mounted) return;
                              provider.rechargeWallet(amt);
                              Navigator.pop(context);
                              ScaffoldMessenger.of(context).showSnackBar(
                                SnackBar(
                                    content: Text(
                                        "Mock credited GH₵ ${amt.toStringAsFixed(2)} to wallet successfully.")),
                              );
                            });
                          }
                        },
                  child: isLoading
                      ? const SizedBox(
                          width: 16,
                          height: 16,
                          child: CircularProgressIndicator(strokeWidth: 2))
                      : const Text("INITIATE CHECKOUT"),
                ),
              ],
            );
          },
        );
      },
    );
  }

  // Beautiful Paystack Payment Gateway Simulator View
  void _showPaystackSimulator(
    BuildContext context,
    CafeteriaProvider provider,
    Map<String, dynamic> paystackData,
    double amount,
  ) {
    final stepWrapper = {'step': 0};
    String otpInput = "";
    String verifyStatus = "Verifying transaction with Laravel...";

    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (context) {
        return StatefulBuilder(
          builder: (context, setSimState) {
            final step = stepWrapper['step']!;

            return Dialog(
              shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(16)),
              child: AnimatedContainer(
                duration: const Duration(milliseconds: 300),
                width: 320,
                padding: const EdgeInsets.all(20),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    // Paystack Secure Branding
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Row(
                          children: [
                            Container(
                              width: 10,
                              height: 10,
                              decoration: const BoxDecoration(
                                color: Color(0xFF09A5DB), // Paystack Teal
                                shape: BoxShape.circle,
                              ),
                            ),
                            const SizedBox(width: 8),
                            const Text(
                              "paystack",
                              style: TextStyle(
                                  fontWeight: FontWeight.bold,
                                  fontSize: 18,
                                  color: Colors.black87),
                            ),
                          ],
                        ),
                        Container(
                          padding: const EdgeInsets.symmetric(
                              horizontal: 8, vertical: 4),
                          decoration: BoxDecoration(
                            color:
                                const Color(0xFFFFA000).withValues(alpha: 0.12),
                            borderRadius: BorderRadius.circular(6),
                          ),
                          child: const Row(
                            children: [
                              Icon(Icons.lock,
                                  color: Color(0xFFFFA000), size: 10),
                              SizedBox(width: 4),
                              Text("TEST GATEWAY",
                                  style: TextStyle(
                                      color: Color(0xFFFFA000),
                                      fontSize: 8,
                                      fontWeight: FontWeight.bold)),
                            ],
                          ),
                        ),
                      ],
                    ),
                    const Divider(height: 24),

                    // STEP 0: CARD / MOMO INPUTS
                    if (step == 0) ...[
                      Text(
                        "PAYMENT TO: Accra Tech Cafeteria",
                        style: TextStyle(
                            color: Colors.blueGrey[600],
                            fontSize: 10,
                            fontWeight: FontWeight.bold,
                            letterSpacing: 0.5),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        "GH₵ ${amount.toStringAsFixed(2)}",
                        style: const TextStyle(
                            fontSize: 24,
                            fontWeight: FontWeight.bold,
                            color: Colors.black87),
                      ),
                      const SizedBox(height: 16),
                      Text(
                        "Reference: ${paystackData['reference']}",
                        style: TextStyle(
                            fontSize: 9,
                            fontFamily: 'monospace',
                            color: Colors.blueGrey[500]),
                      ),
                      const SizedBox(height: 16),

                      // Payment Methods Selection
                      Container(
                        padding: const EdgeInsets.symmetric(
                            vertical: 8, horizontal: 12),
                        decoration: BoxDecoration(
                          border: Border.all(color: Colors.blueGrey[300]!),
                          borderRadius: BorderRadius.circular(8),
                        ),
                        child: const Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            Row(
                              children: [
                                Icon(Icons.phone_android,
                                    color: Color(0xFF00796B), size: 18),
                                SizedBox(width: 8),
                                Text("Mobile Money (MTN/Telecel/AT)",
                                    style: TextStyle(
                                        fontSize: 11,
                                        fontWeight: FontWeight.bold)),
                              ],
                            ),
                            Icon(Icons.check_circle,
                                color: Color(0xFF00796B), size: 16),
                          ],
                        ),
                      ),
                      const SizedBox(height: 12),

                      TextField(
                        controller: TextEditingController(
                            text: provider.currentUser?.info ?? "0551234567"),
                        enabled: false,
                        decoration: const InputDecoration(
                          labelText: "MoMo Account Number",
                          border: OutlineInputBorder(),
                        ),
                      ),
                      const SizedBox(height: 20),

                      ElevatedButton(
                        style: ElevatedButton.styleFrom(
                          backgroundColor: const Color(
                              0xFF3AC5A0), // Paystack Bright Teal Button
                          foregroundColor: Colors.white,
                          padding: const EdgeInsets.symmetric(vertical: 14),
                          shape: RoundedRectangleBorder(
                              borderRadius: BorderRadius.circular(8)),
                        ),
                        onPressed: () {
                          setSimState(() {
                            stepWrapper['step'] =
                                1; // transition to OTP Verification
                          });
                        },
                        child: const Text("PAY WITH MOBILE MONEY",
                            style: TextStyle(fontWeight: FontWeight.bold)),
                      ),
                    ],

                    // STEP 1: OTP SIMULATOR
                    if (step == 1) ...[
                      const Icon(Icons.security,
                          color: Color(0xFF09A5DB), size: 48),
                      const SizedBox(height: 16),
                      const Center(
                        child: Text(
                          "Verify MoMo Transaction",
                          style: TextStyle(
                              fontWeight: FontWeight.bold, fontSize: 16),
                        ),
                      ),
                      const SizedBox(height: 8),
                      const Center(
                        child: Text(
                          "A push notification OTP was simulated to your handset. Please type '1234' below to authorize the withdrawal request.",
                          textAlign: TextAlign.center,
                          style:
                              TextStyle(fontSize: 11, color: Colors.blueGrey),
                        ),
                      ),
                      const SizedBox(height: 16),
                      TextField(
                        keyboardType: TextInputType.number,
                        textAlign: TextAlign.center,
                        obscureText: true,
                        maxLength: 4,
                        style: const TextStyle(
                            fontSize: 20,
                            fontWeight: FontWeight.bold,
                            letterSpacing: 8),
                        decoration: const InputDecoration(
                          counterText: "",
                          border: OutlineInputBorder(),
                          hintText: "••••",
                        ),
                        onChanged: (val) {
                          otpInput = val;
                        },
                      ),
                      const SizedBox(height: 20),
                      ElevatedButton(
                        style: ElevatedButton.styleFrom(
                          backgroundColor: const Color(0xFF00796B),
                          padding: const EdgeInsets.symmetric(vertical: 14),
                        ),
                        onPressed: () {
                          if (otpInput == "1234") {
                            setSimState(() {
                              stepWrapper['step'] =
                                  2; // transition to Backend Verification
                            });
                            _performBackendVerification(
                                context,
                                provider,
                                paystackData['reference'],
                                amount,
                                setSimState,
                                stepWrapper);
                          } else {
                            ScaffoldMessenger.of(context).showSnackBar(
                              const SnackBar(
                                  content: Text(
                                      "Incorrect OTP pin. Please input '1234' to authorize.")),
                            );
                          }
                        },
                        child: const Text("AUTHORIZE PAYMENT",
                            style: TextStyle(fontWeight: FontWeight.bold)),
                      ),
                    ],

                    // STEP 2: BACKEND LARAVEL VERIFICATION
                    if (step == 2) ...[
                      const SizedBox(height: 24),
                      const Center(
                          child: CircularProgressIndicator(
                              color: Color(0xFF09A5DB))),
                      const SizedBox(height: 24),
                      Center(
                        child: Text(
                          verifyStatus,
                          style: const TextStyle(
                              fontWeight: FontWeight.bold,
                              fontSize: 13,
                              color: Colors.black54),
                        ),
                      ),
                      const SizedBox(height: 8),
                      const Center(
                        child: Text(
                          "Synchronizing digital signatures with Accra Tech ledger and writing security audit trails...",
                          textAlign: TextAlign.center,
                          style: TextStyle(
                              fontSize: 10,
                              color: Colors.blueGrey,
                              fontStyle: FontStyle.italic),
                        ),
                      ),
                    ],

                    // STEP 3: TRANSACTION SUCCESS
                    if (step == 3) ...[
                      const Icon(Icons.check_circle_rounded,
                          color: Color(0xFF2E7D32), size: 64),
                      const SizedBox(height: 16),
                      const Center(
                        child: Text(
                          "TRANSACTION SUCCESSFUL",
                          style: TextStyle(
                              fontWeight: FontWeight.bold,
                              fontSize: 16,
                              color: Color(0xFF2E7D32)),
                        ),
                      ),
                      const SizedBox(height: 8),
                      Center(
                        child: Text(
                          "Credited GH₵ ${amount.toStringAsFixed(2)} to student wallet securely.",
                          textAlign: TextAlign.center,
                          style: const TextStyle(fontSize: 12),
                        ),
                      ),
                      const SizedBox(height: 24),
                      ElevatedButton(
                        style: ElevatedButton.styleFrom(
                            backgroundColor: const Color(0xFF2E7D32)),
                        onPressed: () {
                          Navigator.pop(context);
                        },
                        child: const Text("DONE",
                            style: TextStyle(fontWeight: FontWeight.bold)),
                      ),
                    ],
                  ],
                ),
              ),
            );
          },
        );
      },
    );
  }

  // Triggers the Laravel API verify request
  void _performBackendVerification(
    BuildContext context,
    CafeteriaProvider provider,
    String reference,
    double amount,
    StateSetter setSimState,
    Map<String, int> stepWrapper,
  ) async {
    final success = await provider.verifyPaystackPayment(
      reference: reference,
      amount: amount,
      purpose: 'WALLET_TOPUP',
    );
    if (!context.mounted) return;

    if (success) {
      setSimState(() {
        stepWrapper['step'] = 3; // transition to SUCCESS Step
      });
    } else {
      Navigator.pop(context);
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
            content: Text(
                "Laravel payment verification failed. Standalone bypass triggered.")),
      );
      // fallback
      provider.rechargeWallet(amount);
    }
  }

  Widget _ratingRow(String label, int current, Function(int) onSelect) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(label,
            style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 12)),
        Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: List.generate(5, (index) {
            final starVal = index + 1;
            return IconButton(
              icon: Icon(starVal <= current ? Icons.star : Icons.star_border,
                  color: const Color(0xFFE5A93C)),
              onPressed: () => onSelect(starVal),
            );
          }),
        )
      ],
    );
  }

  Color _statusColor(String status) {
    switch (status) {
      case 'Order Placed':
      case 'PENDING':
        return const Color(0xFF2E7D32);
      case 'Preparing':
      case 'PREPARING':
        return const Color(0xFF1565C0);
      case 'Out for Delivery':
      case 'OUT_FOR_DELIVERY':
      case 'READY':
        return const Color(0xFFE8751A);
      case 'Delivered':
      case 'COMPLETED':
        return const Color(0xFF00796B);
      case 'DECLINED':
      case 'Declined':
        return Colors.red;
      default:
        return Colors.blueGrey;
    }
  }

  int _getStageIndex(String status) {
    switch (status) {
      case 'Order Placed':
      case 'PENDING':
        return 0;
      case 'Preparing':
      case 'PREPARING':
        return 1;
      case 'Out for Delivery':
      case 'OUT_FOR_DELIVERY':
      case 'READY':
        return 2;
      case 'Delivered':
      case 'COMPLETED':
        return 3;
      default:
        return -1; // Cancellation or declination
    }
  }

  Widget _buildTrackingStepper(String status) {
    final currentStage = _getStageIndex(status);

    if (currentStage == -1) {
      return Container(
        margin: const EdgeInsets.symmetric(vertical: 8),
        padding: const EdgeInsets.all(10),
        decoration: BoxDecoration(
          color: Colors.red[50],
          borderRadius: BorderRadius.circular(8),
          border: Border.all(color: Colors.red[200]!),
        ),
        child: Row(
          children: [
            const Icon(Icons.cancel, color: Colors.red, size: 18),
            const SizedBox(width: 8),
            Expanded(
              child: Text(
                "Order is currently: $status.",
                style: const TextStyle(
                    color: Colors.red,
                    fontWeight: FontWeight.bold,
                    fontSize: 13),
              ),
            ),
          ],
        ),
      );
    }

    final stages = [
      {'label': 'Order Received', 'icon': Icons.assignment_turned_in},
      {'label': 'Preparing', 'icon': Icons.soup_kitchen},
      {'label': 'Ready for Pickup/Delivery', 'icon': Icons.delivery_dining},
      {'label': 'Delivered', 'icon': Icons.check_circle},
    ];

    return Container(
      margin: const EdgeInsets.symmetric(vertical: 8),
      padding: const EdgeInsets.symmetric(vertical: 10, horizontal: 8),
      decoration: BoxDecoration(
        color: Colors.blue.shade50.withValues(alpha: 0.3),
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: Colors.blue.shade100),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Row(
            children: [
              Icon(Icons.radar, color: Color(0xFF1565C0), size: 14),
              SizedBox(width: 6),
              Text(
                "REAL-TIME ORDER TRACKING STREAMS",
                style: TextStyle(
                  fontWeight: FontWeight.bold,
                  fontSize: 10,
                  color: Color(0xFF1565C0),
                  letterSpacing: 0.8,
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: List.generate(stages.length, (idx) {
              final stage = stages[idx];
              final isPassed = idx <= currentStage;
              final isActive = idx == currentStage;
              final label = stage['label'] as String;
              final icon = stage['icon'] as IconData;

              final Color color = isActive
                  ? const Color(0xFF1565C0)
                  : (isPassed
                      ? const Color(0xFF2E7D32)
                      : Colors.blueGrey[400]!);

              return Expanded(
                child: Column(
                  children: [
                    Row(
                      children: [
                        Expanded(
                          child: Container(
                            height: 2,
                            color: idx == 0
                                ? Colors.transparent
                                : (idx <= currentStage
                                    ? const Color(0xFF2E7D32)
                                    : Colors.blueGrey[300]),
                          ),
                        ),
                        Container(
                          padding: const EdgeInsets.all(6),
                          decoration: BoxDecoration(
                            color: isActive
                                ? Colors.blue.shade100
                                : (isPassed
                                    ? Colors.green.shade50
                                    : Colors.blueGrey[100]),
                            shape: BoxShape.circle,
                            border: Border.all(color: color, width: 2),
                          ),
                          child: Icon(icon, color: color, size: 14),
                        ),
                        Expanded(
                          child: Container(
                            height: 2,
                            color: idx == stages.length - 1
                                ? Colors.transparent
                                : (idx < currentStage
                                    ? const Color(0xFF2E7D32)
                                    : Colors.blueGrey[300]),
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 6),
                    Text(
                      label,
                      textAlign: TextAlign.center,
                      style: TextStyle(
                        fontSize: 9,
                        fontWeight:
                            isActive ? FontWeight.bold : FontWeight.normal,
                        color: isActive
                            ? const Color(0xFF1565C0)
                            : (isPassed
                                ? const Color(0xFF2E7D32)
                                : Colors.blueGrey[600]),
                      ),
                    ),
                  ],
                ),
              );
            }),
          ),
        ],
      ),
    );
  }
}
