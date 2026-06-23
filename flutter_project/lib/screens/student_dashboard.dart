import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:intl/intl.dart';
import '../models/models.dart' as models;
import '../viewmodel/cafeteria_provider.dart';

class StudentDashboardScreen extends StatefulWidget {
  const StudentDashboardScreen({super.key});

  @override
  State<StudentDashboardScreen> createState() => _StudentDashboardScreenState();
}

class _StudentDashboardScreenState extends State<StudentDashboardScreen> {
  int _activeTab = 0; // 0: Browse Food, 1: Track Orders, 2: Smart Wallet & ID
  String _selectedCategory = 'All';

  final List<String> _categories = ['All', 'Lunch Specials', 'Traditional', 'Drinks', 'Snacks'];

  @override
  Widget build(BuildContext context) {
    final provider = Provider.of<CafeteriaProvider>(context);
    final user = provider.currentUser;

    if (user == null) {
      return const Scaffold(body: Center(child: Text("Access session expired. Please re-authenticate.")));
    }

    return Scaffold(
      appBar: AppBar(
        title: Text(provider.isAdminActing ? 'SIMULATION: ${user.fullName}' : 'STUDENT CENTRAL HUB'),
        leading: provider.isAdminActing
            ? IconButton(
                icon: const Icon(Icons.admin_panel_settings, color: Colors.amber),
                tooltip: "Return to Admin Console",
                onPressed: () {
                  provider.stopImpersonation();
                  Navigator.pushReplacementNamed(context, '/admin_home');
                },
              )
            : null,
        actions: [
          if (provider.isAdminActing)
            Padding(
              padding: const EdgeInsets.only(right: 8.0),
              child: ElevatedButton.icon(
                style: ElevatedButton.styleFrom(
                  backgroundColor: Colors.amber[800],
                  foregroundColor: Colors.white,
                  padding: const EdgeInsets.symmetric(horizontal: 12),
                  minimumSize: const Size(64, 36),
                ),
                icon: const Icon(Icons.exit_to_app, size: 16),
                label: const Text("EXIT", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 12)),
                onPressed: () {
                  provider.stopImpersonation();
                  Navigator.pushReplacementNamed(context, '/admin_home');
                },
              ),
            )
          else
            IconButton(
              icon: const Icon(Icons.logout),
              tooltip: "Logout Securely",
              onPressed: () {
                provider.logOut();
                Navigator.pushReplacementNamed(context, '/login');
              },
            )
        ],
      ),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _activeTab,
        onDestinationSelected: (value) => setState(() => _activeTab = value),
        destinations: const [
          NavigationDestination(icon: Icon(Icons.restaurant_menu), label: 'Browse Food'),
          NavigationDestination(icon: Icon(Icons.receipt_long), label: 'Track Orders'),
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
      list = list.where((item) => item.category == _selectedCategory).toList();
    }

    return Column(
      children: [
        // Announcement bar
        Container(
          width: double.infinity,
          color: Theme.of(context).colorScheme.secondary.withOpacity(0.15),
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
          child: Row(
            children: [
              Icon(Icons.campaign, color: Theme.of(context).colorScheme.primary),
              const SizedBox(width: 12),
              Expanded(
                child: Text(
                  provider.vendorAnnouncement,
                  style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 12, color: Colors.black87),
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
              color: Colors.red[50]?.withOpacity(0.4) ?? Colors.amber[50]?.withOpacity(0.4),
              borderRadius: BorderRadius.circular(10),
              border: Border.all(color: Colors.red[100] ?? Colors.amber[100]!),
            ),
            child: Row(
              children: [
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                  decoration: BoxDecoration(
                    color: Colors.redAccent,
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
                          title: Row(
                            children: const [
                              Icon(Icons.radar, color: Colors.blueAccent),
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
                                leading: const Icon(Icons.info_outline, size: 18),
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
                        color: Colors.blueAccent,
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
                    selectedColor: Theme.of(context).colorScheme.primary.withOpacity(0.2),
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
              ? const Center(child: Text("No meals available in this category."))
              : ListView.builder(
                  padding: const EdgeInsets.all(12),
                  itemCount: list.length,
                  itemBuilder: (context, index) {
                    final item = list[index];
                    final vendor = provider.allVendors.firstWhere(
                      (v) => v.id == item.vendorId,
                      orElse: () => models.User(username: 'unknown', passwordHash: '', role: 'VENDOR', fullName: 'ATU Kitchen', info: 'Registered Chef'),
                    );

                    return Card(
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                      clipBehavior: Clip.antiAlias,
                      child: Padding(
                        padding: const EdgeInsets.all(12.0),
                        child: Row(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Container(
                              width: 80,
                              height: 80,
                              decoration: BoxDecoration(
                                color: Theme.of(context).colorScheme.primary.withOpacity(0.08),
                                borderRadius: BorderRadius.circular(8),
                              ),
                              child: Icon(
                                _categoryIcon(item.category),
                                size: 36,
                                color: Theme.of(context).colorScheme.primary,
                              ),
                            ),
                            const SizedBox(width: 16),
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(
                                    item.name,
                                    style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
                                  ),
                                  const SizedBox(height: 4),
                                  Text(
                                    "Vendor: ${vendor.fullName} (${vendor.info})",
                                    style: TextStyle(color: Colors.grey[600], fontSize: 12),
                                  ),
                                  const SizedBox(height: 4),
                                  Text(
                                    item.description,
                                    maxLines: 2,
                                    overflow: TextOverflow.ellipsis,
                                    style: TextStyle(color: Colors.grey[500], fontSize: 11),
                                  ),
                                  const SizedBox(height: 8),
                                  Row(
                                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                    children: [
                                      Text(
                                        "GH₵ ${item.price.toStringAsFixed(2)}",
                                        style: TextStyle(
                                          fontWeight: FontWeight.w900,
                                          fontSize: 15,
                                          color: Theme.of(context).colorScheme.primary,
                                        ),
                                      ),
                                      ElevatedButton.icon(
                                        onPressed: () => _showOrderSheet(context, item, provider),
                                        icon: const Icon(Icons.add_shopping_cart, size: 16),
                                        label: const Text("ORDER"),
                                        style: ElevatedButton.styleFrom(
                                          minimumSize: const Size(80, 32),
                                          padding: const EdgeInsets.symmetric(horizontal: 12),
                                        ),
                                      ),
                                    ],
                                  )
                                ],
                              ),
                            )
                          ],
                        ),
                      ),
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

    if (list.isEmpty) {
      return const Center(child: Text("You have not initialized any orders yet."));
    }

    return ListView.builder(
      padding: const EdgeInsets.all(12),
      itemCount: list.length,
      itemBuilder: (context, index) {
        final order = list[index];
        final timeStr = DateFormat('h:mm a - d MMM').format(DateTime.fromMillisecondsSinceEpoch(order.orderTimestamp));
        final orderColor = _statusColor(order.status);

        return Card(
          elevation: 2,
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
          child: ExpansionTile(
            title: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Expanded(
                  child: Text(
                    order.foodName,
                    style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 15),
                  ),
                ),
                Container(
                  padding: const EdgeInsets.symmetric(vertical: 4, horizontal: 8),
                  decoration: BoxDecoration(color: orderColor.withOpacity(0.12), borderRadius: BorderRadius.circular(8)),
                  child: Text(
                    order.status,
                    style: TextStyle(color: orderColor, fontWeight: FontWeight.bold, fontSize: 11),
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
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        const Text(
                          "Authentication Token / PIN:",
                          style: TextStyle(fontWeight: FontWeight.w600, fontSize: 13),
                        ),
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 6),
                          decoration: BoxDecoration(
                            color: Colors.grey[200],
                            borderRadius: BorderRadius.circular(6),
                            border: Border.all(color: Colors.grey[400]!),
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
                      style: TextStyle(color: Colors.grey[500], fontSize: 11),
                    ),
                    const SizedBox(height: 16),
                    if (order.status == 'COMPLETED') ...[
                      ElevatedButton.icon(
                        icon: const Icon(Icons.rate_review),
                        label: const Text("POST RATING & FEEDBACK AUDIT"),
                        onPressed: () => _showFeedbackDialog(context, order, provider),
                      )
                    ]
                  ],
                ),
              )
            ],
          ),
        );
      },
    );
  }

  // ==========================================
  // TAB 3: SMART WALLET & CAMPUS IDENTITY CARD
  // ==========================================
  Widget _buildWalletAndId(BuildContext context, CafeteriaProvider provider, models.User user) {
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
                BoxShadow(color: Colors.blue.withOpacity(0.3), blurRadius: 8, offset: const Offset(0, 4))
              ]
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    const Text(
                      "ACCRA TECHNICAL UNIVERSITY",
                      style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, letterSpacing: 0.8, fontSize: 11),
                    ),
                    Icon(Icons.nfc, color: Theme.of(context).colorScheme.secondary, size: 24)
                  ],
                ),
                const SizedBox(height: 24),
                Row(
                  children: [
                    CircleAvatar(
                      radius: 28,
                      backgroundColor: Colors.white24,
                      child: Icon(Icons.school, color: Theme.of(context).colorScheme.secondary, size: 30),
                    ),
                    const SizedBox(width: 16),
                    Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          user.fullName,
                          style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 16),
                        ),
                        const SizedBox(height: 4),
                        Text(
                          "REF ID: ${user.info}",
                          style: const TextStyle(color: Colors.white70, fontSize: 12),
                        ),
                        const SizedBox(height: 4),
                        const Text(
                          "CLASSIFICATION: SCHOLAR / STUDENT",
                          style: TextStyle(color: Colors.white54, fontSize: 10, fontWeight: FontWeight.bold),
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
                    const Text("NFC SECURED CASH LESS PASS", style: TextStyle(color: Colors.white60, fontSize: 9)),
                    Icon(Icons.qr_code, color: Colors.white.withOpacity(0.8), size: 28)
                  ],
                )
              ],
            ),
          ),
          const SizedBox(height: 20),

          // Digital Wallet Balance Card
          Card(
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
            child: Padding(
              padding: const EdgeInsets.all(20.0),
              child: Column(
                children: [
                  const Text("ATU WALLET ACCOUNT BALANCE", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 12, letterSpacing: 1, color: Colors.black54)),
                  const SizedBox(height: 8),
                  Text(
                    "GH₵ ${provider.studentWalletBalance.toStringAsFixed(2)}",
                    style: TextStyle(fontSize: 32, fontWeight: FontWeight.w900, color: Theme.of(context).colorScheme.primary),
                  ),
                  const SizedBox(height: 16),
                  ElevatedButton.icon(
                    icon: const Icon(Icons.add_card),
                    label: const Text("TOP-UP VIA MOMO GATEWAY"),
                    style: ElevatedButton.styleFrom(backgroundColor: Theme.of(context).colorScheme.primary),
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
              child: Text("Wallet & Session History logs", style: TextStyle(fontWeight: FontWeight.bold, color: Colors.black87, fontSize: 14)),
            ),
          ),
          const SizedBox(height: 8),
          ListView.builder(
            shrinkWrap: true,
            physics: const NeverScrollableScrollPhysics(),
            itemCount: provider.auditLogs.where((l) => l.userId == user.id).length,
            itemBuilder: (context, idx) {
              final studentLogs = provider.auditLogs.where((l) => l.userId == user.id).toList();
              final log = studentLogs[idx];
              final dateStr = DateFormat('jm - d MMM').format(DateTime.fromMillisecondsSinceEpoch(log.timestamp));

              return ListTile(
                leading: const Icon(Icons.history_toggle_off, color: Colors.blueAccent),
                title: Text(log.action, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13)),
                subtitle: Text(log.details, style: const TextStyle(fontSize: 11)),
                trailing: Text(dateStr, style: const TextStyle(fontSize: 10, color: Colors.grey)),
              );
            },
          )
        ],
      ),
    );
  }

  // Helper popup sheets
  void _showOrderSheet(BuildContext context, models.FoodItem food, CafeteriaProvider provider) {
    int qty = 1;
    bool payWallet = false;

    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      builder: (context) {
        return StatefulBuilder(
          builder: (context, setSheetState) {
            final double priceSum = food.price * qty;
            final hasSufficientWallet = provider.studentWalletBalance >= priceSum;

            return Padding(
              padding: EdgeInsets.only(
                top: 20,
                left: 20,
                right: 20,
                bottom: MediaQuery.of(context).viewInsets.bottom + 24,
              ),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text("ORDER COMPILATION", style: TextStyle(fontWeight: FontWeight.bold, color: Theme.of(context).colorScheme.primary, fontSize: 13, letterSpacing: 0.8)),
                  const SizedBox(height: 8),
                  Text(food.name, style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold)),
                  Text("Category: ${food.category}", style: TextStyle(color: Colors.grey[600], fontSize: 12)),
                  const SizedBox(height: 16),
                  Text("Quantity Selection:", style: TextStyle(color: Colors.grey[700], fontWeight: FontWeight.bold, fontSize: 13)),
                  const SizedBox(height: 8),
                  Row(
                    children: [
                      IconButton(
                        icon: const Icon(Icons.remove_circle_outline),
                        onPressed: qty > 1 ? () => setSheetState(() => qty--) : null,
                      ),
                      Text("$qty", style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
                      IconButton(
                        icon: const Icon(Icons.add_circle_outline),
                        onPressed: () => setSheetState(() => qty++),
                      ),
                    ],
                  ),
                  const SizedBox(height: 16),
                  SwitchListTile(
                    title: const Text("Pay using Digital Wallet", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 14)),
                    subtitle: Text("Available: GH₵ ${provider.studentWalletBalance.toStringAsFixed(2)}", style: const TextStyle(fontSize: 11)),
                    value: payWallet,
                    onChanged: (val) {
                      setSheetState(() => payWallet = val);
                    },
                  ),
                  const SizedBox(height: 16),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      const Text("Grand Total:", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
                      Text(
                        "GH₵ ${priceSum.toStringAsFixed(2)}",
                        style: TextStyle(fontWeight: FontWeight.w900, color: Theme.of(context).colorScheme.primary, fontSize: 20),
                      ),
                    ],
                  ),
                  const SizedBox(height: 20),
                  ElevatedButton(
                    onPressed: (payWallet && !hasSufficientWallet)
                        ? null
                        : () async {
                            final success = await provider.placeOrder(food, qty, payWallet);
                            if (mounted) Navigator.pop(context);

                            ScaffoldMessenger.of(context).showSnackBar(
                              SnackBar(
                                content: Text(success ? "Order registered dynamically!" : "Failed. Insufficient wallet balance."),
                                backgroundColor: success ? Colors.green : Colors.red,
                              ),
                            );
                          },
                    child: Text(
                      (payWallet && !hasSufficientWallet) ? "INSUFFICIENT WALLET BALANCE" : "COMPILE ORDER NOW",
                      style: const TextStyle(fontWeight: FontWeight.bold),
                    ),
                  )
                ],
              ),
            );
          },
        );
      },
    );
  }

  void _showFeedbackDialog(BuildContext context, models.Order order, CafeteriaProvider provider) {
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
              title: const Text("POST PERFORMANCE RATINGS", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 17)),
              content: SingleChildScrollView(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    const Text("ATU requires student reviews to secure hygiene and compliance audits.", style: TextStyle(fontSize: 12, color: Colors.black54)),
                    const SizedBox(height: 12),
                    _ratingRow("Food Taste & Quality", qualityVal, (v) => setDialogState(() => qualityVal = v)),
                    _ratingRow("Booth Cleanliness", cleanlinessVal, (v) => setDialogState(() => cleanlinessVal = v)),
                    _ratingRow("Service Velocity / Speed", speedVal, (v) => setDialogState(() => speedVal = v)),
                    _ratingRow("Price Value Fairness", pricingVal, (v) => setDialogState(() => pricingVal = v)),
                    const SizedBox(height: 12),
                    TextField(
                      controller: commentController,
                      decoration: const InputDecoration(labelText: "Review Comments", border: OutlineInputBorder()),
                      maxLines: 2,
                    ),
                  ],
                ),
              ),
              actions: [
                TextButton(onPressed: () => Navigator.pop(context), child: const Text("CANCEL")),
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
    final phoneController = TextEditingController();

    showDialog(
      context: context,
      builder: (context) {
        return AlertDialog(
          title: const Text("MoMo Digital Wallet Recharge", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextField(
                controller: amountController,
                keyboardType: TextInputType.number,
                decoration: const InputDecoration(labelText: "Top-Up Amount (GH₵)", border: OutlineInputBorder()),
              ),
              const SizedBox(height: 12),
              TextField(
                controller: phoneController,
                keyboardType: TextInputType.phone,
                decoration: const InputDecoration(labelText: "Mobile Money Phone Number", border: OutlineInputBorder()),
              ),
            ],
          ),
          actions: [
            TextButton(onPressed: () => Navigator.pop(context), child: const Text("CANCEL")),
            ElevatedButton(
              onPressed: () {
                final amt = double.tryParse(amountController.text) ?? 0.0;
                if (amt > 0) {
                  provider.rechargeWallet(amt);
                  Navigator.pop(context);
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(content: Text("Credited GH₵ ${amt.toStringAsFixed(2)} securely via Mobile Money gateway.")),
                  );
                }
              },
              child: const Text("AUTHENTICATE TOP UP"),
            )
          ],
        );
      },
    );
  }

  Widget _ratingRow(String label, int current, Function(int) onSelect) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(label, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 12)),
        Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: List.generate(5, (index) {
            final starVal = index + 1;
            return IconButton(
              icon: Icon(starVal <= current ? Icons.star : Icons.star_border, color: const Color(0xFFE5A93C)),
              onPressed: () => onSelect(starVal),
            );
          }),
        )
      ],
    );
  }

  IconData _categoryIcon(String category) {
    switch (category) {
      case 'Drinks':
        return Icons.local_drink;
      case 'Snacks':
        return Icons.cookie;
      case 'Traditional':
        return Icons.rice_bowl;
      default:
        return Icons.restaurant;
    }
  }

  Color _statusColor(String status) {
    switch (status) {
      case 'Order Placed':
      case 'PENDING':
        return Colors.green;
      case 'Preparing':
      case 'PREPARING':
        return Colors.blue;
      case 'Out for Delivery':
      case 'OUT_FOR_DELIVERY':
      case 'READY':
        return Colors.orange;
      case 'Delivered':
      case 'COMPLETED':
        return Colors.teal;
      case 'DECLINED':
      case 'Declined':
        return Colors.red;
      default:
        return Colors.grey;
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
                style: const TextStyle(color: Colors.red, fontWeight: FontWeight.bold, fontSize: 13),
              ),
            ),
          ],
        ),
      );
    }

    final stages = [
      {'label': 'Received', 'icon': Icons.assignment_turned_in},
      {'label': 'Preparing', 'icon': Icons.soup_kitchen},
      {'label': 'Out for Delivery', 'icon': Icons.delivery_dining},
      {'label': 'Delivered', 'icon': Icons.check_circle},
    ];

    return Container(
      margin: const EdgeInsets.symmetric(vertical: 8),
      padding: const EdgeInsets.symmetric(vertical: 10, horizontal: 8),
      decoration: BoxDecoration(
        color: Colors.blue[50]?.withOpacity(0.3),
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: Colors.blue[100]!),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: const [
              Icon(Icons.radar, color: Colors.blueAccent, size: 14),
              SizedBox(width: 6),
              Text(
                "REAL-TIME ORDER TRACKING STREAMS",
                style: TextStyle(
                  fontWeight: FontWeight.bold,
                  fontSize: 10,
                  color: Colors.blueAccent,
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
                  ? Colors.blueAccent 
                  : (isPassed ? Colors.green : Colors.grey[400]!);

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
                                : (idx <= currentStage ? Colors.green : Colors.grey[300]),
                          ),
                        ),
                        Container(
                          padding: const EdgeInsets.all(6),
                          decoration: BoxDecoration(
                            color: isActive ? Colors.blue[100] : (isPassed ? Colors.green[50] : Colors.grey[100]),
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
                                : (idx < currentStage ? Colors.green : Colors.grey[300]),
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
                        fontWeight: isActive ? FontWeight.bold : FontWeight.normal,
                        color: isActive ? Colors.blueAccent : (isPassed ? Colors.green : Colors.grey[600]),
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
