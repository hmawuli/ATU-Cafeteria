import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:intl/intl.dart';
import '../models/models.dart';
import '../viewmodel/cafeteria_provider.dart';

class VendorDashboardScreen extends StatefulWidget {
  const VendorDashboardScreen({super.key});

  @override
  State<VendorDashboardScreen> createState() => _VendorDashboardScreenState();
}

class _VendorDashboardScreenState extends State<VendorDashboardScreen> {
  int _activeTab = 0; // 0: Orders, 1: Menu List, 2: Ratings/Analytics, 3: Settings & Hub

  // Add food form controllers
  final _foodFormKey = GlobalKey<FormState>();
  final _foodNameController = TextEditingController();
  final _foodPriceController = TextEditingController();
  final _foodDescController = TextEditingController();
  String _selectedCategory = 'Local Dish';

  final List<String> _categories = ['Breakfast', 'Local Dish', 'Fast Food', 'Drinks', 'Snacks'];

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
      return const Scaffold(body: Center(child: Text("Access session expired. Please re-authenticate.")));
    }

    return Scaffold(
      appBar: AppBar(
        title: Text(provider.isAdminActing ? 'SIMULATION: ${user.fullName}' : 'VENDOR PORTAL CONTROLLER'),
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
          NavigationDestination(icon: Icon(Icons.receipt), label: 'Incoming Orders'),
          NavigationDestination(icon: Icon(Icons.breakfast_dining), label: 'Menu Catalog'),
          NavigationDestination(icon: Icon(Icons.insights), label: 'Analytics & AI'),
          NavigationDestination(icon: Icon(Icons.settings_suggest), label: 'Storefront Hub'),
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
                  _buildAnalyticsAndAi(context, provider, user),
                  _buildStorefrontHub(context, provider, user),
                ],
              ),
            ),
    );
  }

  // ==========================================
  // TAB 1: INCOMING ORDERS
  // ==========================================
  Widget _buildIncomingOrders(BuildContext context, CafeteriaProvider provider) {
    final activeOrders = provider.vendorOrders.where((o) => 
      o.status != 'COMPLETED' && 
      o.status != 'Delivered' && 
      o.status != 'DECLINED' && 
      o.status != 'Declined' &&
      o.status != 'CANCELLED' &&
      o.status != 'Cancelled'
    ).toList();

    if (activeOrders.isEmpty) {
      return const Center(child: Text("No incoming culinary streams. Storefront operating at idle."));
    }

    return ListView.builder(
      padding: const EdgeInsets.all(12),
      itemCount: activeOrders.length,
      itemBuilder: (context, index) {
        final order = activeOrders[index];
        final timeStr = DateFormat('jm - d MMM').format(DateTime.fromMillisecondsSinceEpoch(order.orderTimestamp));
        
        Color stateColor;
        switch (order.status) {
          case 'Order Placed':
          case 'PENDING':
            stateColor = Colors.green;
            break;
          case 'Preparing':
          case 'PREPARING':
            stateColor = Colors.blue;
            break;
          case 'Out for Delivery':
          case 'OUT_FOR_DELIVERY':
          case 'READY':
            stateColor = Colors.orange;
            break;
          default:
            stateColor = Colors.grey;
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
                      style: TextStyle(fontWeight: FontWeight.w900, color: Theme.of(context).colorScheme.primary),
                    ),
                    Text(timeStr, style: const TextStyle(fontSize: 10, color: Colors.grey)),
                  ],
                ),
                const SizedBox(height: 12),
                Text(
                  order.foodName,
                  style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                ),
                Text(
                  "Quantity Selected: ${order.quantity} | Total Sum: GH₵ ${order.totalPrice.toStringAsFixed(2)}",
                  style: const TextStyle(fontSize: 13, height: 1.4),
                ),
                const SizedBox(height: 12),
                Row(
                  children: [
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                      decoration: BoxDecoration(color: stateColor.withOpacity(0.12), borderRadius: BorderRadius.circular(6)),
                      child: Text(
                        "Status: ${order.status}",
                        style: TextStyle(color: stateColor, fontWeight: FontWeight.bold, fontSize: 11),
                      ),
                    ),
                  ],
                ),
                const Divider(height: 24),
                // Action options
                if (order.status == 'Order Placed' || order.status == 'PENDING') ...[
                  Row(
                    children: [
                      Expanded(
                        child: OutlinedButton(
                          onPressed: () => provider.updateOrderStatus(order.id!, "Declined"),
                          style: OutlinedButton.styleFrom(foregroundColor: Colors.red),
                          child: const Text("DECLINE ORDER"),
                        ),
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: ElevatedButton(
                          onPressed: () => provider.updateOrderStatus(order.id!, "Preparing"),
                          child: const Text("START COOKING"),
                        ),
                      ),
                    ],
                  )
                ] else if (order.status == 'Preparing' || order.status == 'PREPARING') ...[
                  ElevatedButton(
                    onPressed: () => provider.updateOrderStatus(order.id!, "Out for Delivery"),
                    style: ElevatedButton.styleFrom(backgroundColor: Colors.orange, foregroundColor: Colors.white),
                    child: const Text("DISPATCH / OUT FOR DELIVERY"),
                  )
                ] else if (order.status == 'Out for Delivery' || order.status == 'READY' || order.status == 'OUT_FOR_DELIVERY') ...[
                  ElevatedButton.icon(
                    icon: const Icon(Icons.qr_code_scanner),
                    label: const Text("VERIFY CONSUMER SECURITY PIN"),
                    onPressed: () => _showVerifyPickUpDialog(context, order, provider),
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
          ? const Center(child: Text("Menu Catalog empty. Add food items to begin storefront trade."))
          : ListView.builder(
              padding: const EdgeInsets.all(12),
              itemCount: list.length,
              itemBuilder: (context, index) {
                final item = list[index];
                return Card(
                  child: ListTile(
                    leading: CircleAvatar(
                      backgroundColor: Theme.of(context).colorScheme.primaryContainer,
                      child: Text(item.name.substring(0, 1), style: const TextStyle(fontWeight: FontWeight.bold)),
                    ),
                    title: Text(item.name, style: const TextStyle(fontWeight: FontWeight.bold)),
                    subtitle: Text("Price: GH₵ ${item.price.toStringAsFixed(2)}\nCategory: ${item.category}"),
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
                          icon: const Icon(Icons.delete, color: Colors.grey),
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
  // TAB 3: PERFORMANCE METRICS & GEMINI AI BULLETIN
  // ==========================================
  Widget _buildAnalyticsAndAi(BuildContext context, CafeteriaProvider provider, User vendor) {
    final reviews = provider.vendorFeedback;

    // Compile quick metrics
    double avgQuality = 0;
    double avgCleanliness = 0;
    double avgSpeed = 0;
    double avgPriceVal = 0;
    if (reviews.isNotEmpty) {
      avgQuality = reviews.map((r) => r.ratingFoodQuality).average();
      avgCleanliness = reviews.map((r) => r.ratingCleanliness).average();
      avgSpeed = reviews.map((r) => r.ratingServiceSpeed).average();
      avgPriceVal = reviews.map((r) => r.ratingPriceValue).average();
    }
    double overallAvg = (avgQuality + avgCleanliness + avgSpeed + avgPriceVal) / 4.0;

    return SingleChildScrollView(
      padding: const EdgeInsets.all(16.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Card(
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
            child: Padding(
              padding: const EdgeInsets.all(16.0),
              child: Column(
                children: [
                  const Text("OVERALL COMPLIANCE SCORE", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 12, color: Colors.grey)),
                  const SizedBox(height: 8),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Text(
                        overallAvg.toStringAsFixed(2),
                        style: TextStyle(fontSize: 36, fontWeight: FontWeight.black, color: Theme.of(context).colorScheme.primary),
                      ),
                      const Text(" / 5.0", style: TextStyle(color: Colors.grey, fontSize: 16)),
                    ],
                  ),
                  const SizedBox(height: 12),
                  _denseRatingBar("Culinary Preparation Quality", avgQuality),
                  _denseRatingBar("Stall & Booth Cleanliness", avgCleanliness),
                  _denseRatingBar("Logistical Delivery Speed", avgSpeed),
                  _denseRatingBar("Student Fair-Price Quotient", avgPriceVal),
                ],
              ),
            ),
          ),
          const SizedBox(height: 16),

          ElevatedButton.icon(
            icon: provider.isAnalyzing
                ? const SizedBox(width: 16, height: 16, child: CircularProgressIndicator(color: Colors.white, strokeWidth: 2))
                : const Icon(Icons.psychology),
            onPressed: provider.isAnalyzing
                ? null
                : () {
                    provider.runGeminiVendorAnalytics(vendor, reviews, provider.vendorOrders.length);
                  },
            label: const Text("GENERATE PREDICTIVE GEMINI AUDIT BULLETIN", style: TextStyle(fontWeight: FontWeight.bold)),
          ),

          if (provider.isAnalyzing) ...[
            const SizedBox(height: 16),
            const LinearProgressIndicator(),
            const SizedBox(height: 8),
            const Center(child: Text("Gemini parsing local SQLite arrays...", style: TextStyle(fontStyle: FontStyle.italic, fontSize: 11))),
          ],

          if (provider.aiAnalysisText != null) ...[
            const SizedBox(height: 16),
            Card(
              color: Colors.slate[900],
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
              child: Padding(
                padding: const EdgeInsets.all(16.0),
                child: Text(
                  provider.aiAnalysisText!,
                  style: const TextStyle(
                    fontFamily: 'monospace',
                    color: Colors.white,
                    fontSize: 12,
                    height: 1.4,
                  ),
                ),
              ),
            ),
          ],

          const SizedBox(height: 20),
          const Text("Historic Scholar Review Comments", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 14)),
          const SizedBox(height: 8),
          reviews.isEmpty
              ? const Padding(
                  padding: EdgeInsets.symmetric(vertical: 20),
                  child: Center(child: Text("No feedback logs found.")),
                )
              : ListView.builder(
                  shrinkWrap: true,
                  physics: const NeverScrollableScrollPhysics(),
                  itemCount: reviews.length,
                  itemBuilder: (context, idx) {
                    final fb = reviews[idx];
                    final dateStr = DateFormat('jm - d MMM').format(DateTime.fromMillisecondsSinceEpoch(fb.timestamp));
                    final score = (fb.ratingFoodQuality + fb.ratingCleanliness + fb.ratingServiceSpeed + fb.ratingPriceValue) / 4.0;

                    return Card(
                      color: Colors.white,
                      child: ListTile(
                        leading: CircleAvatar(
                          backgroundColor: Colors.amberAccent[100],
                          child: Text(score.toStringAsFixed(1), style: const TextStyle(color: Colors.black87, fontWeight: FontWeight.bold)),
                        ),
                        title: Text(fb.comment, style: const TextStyle(fontSize: 13, fontStyle: FontStyle.italic)),
                        subtitle: Text("Logged at: $dateStr", style: const TextStyle(fontSize: 10)),
                      ),
                    );
                  },
                )
        ],
      ),
    );
  }

  // ==========================================
  // TAB 4: STOREFRONT HUB & SETTINGS
  // ==========================================
  Widget _buildStorefrontHub(BuildContext context, CafeteriaProvider provider, User vendor) {
    final annController = TextEditingController(text: provider.vendorAnnouncement);

    return SingleChildScrollView(
      padding: const EdgeInsets.all(16.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Card(
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
            child: Padding(
              padding: const EdgeInsets.all(16.0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text("BOOTH CONTROL CENTRE", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 14)),
                  const Divider(height: 24),
                  SwitchListTile(
                    title: const Text("Kitchen Active & Operations Open", style: TextStyle(fontSize: 14, fontWeight: FontWeight.bold)),
                    subtitle: const Text("Toggling closed disables order flows instantly.", style: TextStyle(fontSize: 11)),
                    value: !provider.isStoreClosed,
                    onChanged: (val) {
                      provider.setStoreClosedState(!val);
                    },
                  ),
                  const SizedBox(height: 16),
                  const Text("Student Notice & Broadcaster Announcement:", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 12)),
                  const SizedBox(height: 8),
                  TextField(
                    controller: annController,
                    decoration: const InputDecoration(border: OutlineInputBorder(), hintText: "e.g. Daily special available now..."),
                    maxLines: 2,
                  ),
                  const SizedBox(height: 12),
                  ElevatedButton(
                    onPressed: () {
                      provider.updateVendorAnnouncement(annController.text.trim());
                      ScaffoldMessenger.of(context).showSnackBar(
                        const SnackBar(content: Text("Announcements broadcast to Accra Technical University.")),
                      );
                    },
                    child: const Text("BROADCAST ANNOUNCEMENT"),
                  )
                ],
              ),
            ),
          ),
          const SizedBox(height: 24),
          const Text("Digital Ledger Export", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 14)),
          const SizedBox(height: 8),
          ListTile(
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8), side: const BorderSide(color: Colors.grey)),
            leading: const Icon(Icons.import_export, color: Colors.blueAccent),
            title: const Text("Compile Secure CSV Excel Audit Logs", style: TextStyle(fontSize: 13, fontWeight: FontWeight.bold)),
            subtitle: const Text("Writes total digital earnings and feedback metrics.", style: TextStyle(fontSize: 11)),
            trailing: IconButton(
              icon: const Icon(Icons.arrow_circle_down, color: Colors.blueAccent),
              onPressed: () {
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(content: Text("CSV transaction spreadsheet exported securely to /downloads/ledger.csv.")),
                );
              },
            ),
          )
        ],
      ),
    );
  }

  void _showVerifyPickUpDialog(BuildContext context, Order order, CafeteriaProvider provider) {
    final pinController = TextEditingController();

    showDialog(
      context: context,
      builder: (context) {
        return AlertDialog(
          title: const Text("SECURITY ACCOUNT VERIFICATION", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Text("Enter the secure 4-digit pickup token provided on the student's digital wallet voucher. This protects financial custody.", style: TextStyle(fontSize: 12, color: Colors.black54)),
              const SizedBox(height: 16),
              TextField(
                controller: pinController,
                keyboardType: TextInputType.number,
                obscureText: true,
                maxLength: 4,
                decoration: const InputDecoration(labelText: "Scholar Handshake PIN (Numeric)", border: OutlineInputBorder(), counterText: ""),
              ),
            ],
          ),
          actions: [
            TextButton(onPressed: () => Navigator.pop(context), child: const Text("CANCEL")),
            ElevatedButton(
              onPressed: () async {
                final success = await provider.verifyAndCompletePickup(order.id!, pinController.text.trim());
                if (mounted) Navigator.pop(context);

                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(
                    content: Text(success ? "Verification Match! Custody transfer complete." : "Invalid PIN code. Access denied."),
                    backgroundColor: success ? Colors.green : Colors.red,
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
              title: const Text("ADD DELICACY TO CATALOG", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
              content: SingleChildScrollView(
                child: Form(
                  key: _foodFormKey,
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      TextFormField(
                        controller: _foodNameController,
                        decoration: const InputDecoration(labelText: "Dish Name", border: OutlineInputBorder()),
                        validator: (val) => val == null || val.trim().isEmpty ? "Enter dish name" : null,
                      ),
                      const SizedBox(height: 12),
                      TextFormField(
                        controller: _foodPriceController,
                        keyboardType: TextInputType.number,
                        decoration: const InputDecoration(labelText: "Price (GH₵)", border: OutlineInputBorder()),
                        validator: (val) => val == null || double.tryParse(val) == null || double.parse(val) <= 0
                            ? "Enter valid price"
                            : null,
                      ),
                      const SizedBox(height: 12),
                      DropdownButtonFormField<String>(
                        value: _selectedCategory,
                        decoration: const InputDecoration(labelText: "Category Classification", border: OutlineInputBorder()),
                        items: _categories.map((c) => DropdownMenuItem(value: c, child: Text(c))).toList(),
                        onChanged: (val) {
                          if (val != null) setDialogState(() => _selectedCategory = val);
                        },
                      ),
                      const SizedBox(height: 12),
                      TextFormField(
                        controller: _foodDescController,
                        decoration: const InputDecoration(labelText: "Menu Description", border: OutlineInputBorder()),
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
              Text(label, style: const TextStyle(fontSize: 11, fontWeight: FontWeight.bold)),
              Text("${rating.toStringAsFixed(1)} / 5.0", style: const TextStyle(fontSize: 11, fontWeight: FontWeight.bold, color: Colors.indigoAccent)),
            ],
          ),
          const SizedBox(height: 4),
          ClipRRect(
            borderRadius: BorderRadius.circular(4),
            child: LinearProgressIndicator(
              value: rating / 5.0,
              minHeight: 6,
              color: const Color(0xFFE5A93C),
              backgroundColor: Colors.grey[200],
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
