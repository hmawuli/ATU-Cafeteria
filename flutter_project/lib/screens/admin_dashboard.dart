import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:intl/intl.dart';
import '../models/models.dart';
import '../viewmodel/cafeteria_provider.dart';
import 'widgets/recharts_line_chart.dart';
import 'widgets/d3_sentiment_chart.dart';

class AdminDashboardScreen extends StatefulWidget {
  const AdminDashboardScreen({super.key});

  @override
  State<AdminDashboardScreen> createState() => _AdminDashboardScreenState();
}

class _AdminDashboardScreenState extends State<AdminDashboardScreen> {
  int _activeSubTab = 0; // 0: Compliance board, 1: Vendors Manager, 2: Audit Ledger Logs, 3: Academic Statistics

  @override
  Widget build(BuildContext context) {
    final provider = Provider.of<CafeteriaProvider>(context);

    return Scaffold(
      appBar: AppBar(
        title: const Text('ATU QA COMPLIANCE BOARD'),
        actions: [
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
        selectedIndex: _activeSubTab,
        onDestinationSelected: (value) => setState(() => _activeSubTab = value),
        destinations: const [
          NavigationDestination(icon: Icon(Icons.gavel), label: 'Compliance'),
          NavigationDestination(icon: Icon(Icons.storefront), label: 'Vendors Manager'),
          NavigationDestination(icon: Icon(Icons.verified_user), label: 'Audit Log Ledger'),
          NavigationDestination(icon: Icon(Icons.assessment), label: 'Statistics'),
        ],
      ),
      floatingActionButton: _activeSubTab == 1
          ? FloatingActionButton.extended(
              onPressed: () => _showAddVendorDialog(context, provider),
              icon: const Icon(Icons.add),
              label: const Text("ADD MERCHANT"),
            )
          : null,
      body: provider.isLoading
          ? const Center(child: CircularProgressIndicator())
          : SafeArea(
              child: IndexedStack(
                index: _activeSubTab,
                children: [
                  _buildComplianceBoard(context, provider),
                  _buildVendorCRUD(context, provider),
                  _buildAuditLedgerLogs(context, provider),
                  _buildAcademicStatistics(context, provider),
                ],
              ),
            ),
    );
  }

  // ==========================================
  // TAB 1: COMPLIANCE BOARD
  // ==========================================
  Widget _buildComplianceBoard(BuildContext context, CafeteriaProvider provider) {
    final vendors = provider.allVendors;

    if (vendors.isEmpty) {
      return const Center(child: Text("Registry contains zero food-booth merchant registrations."));
    }

    return ListView.builder(
      padding: const EdgeInsets.all(12),
      itemCount: vendors.length,
      itemBuilder: (context, index) {
        final vendor = vendors[index];
        final vendorFeedback = provider.allFeedback.where((f) => f.vendorId == vendor.id).toList();

        // Calculate average
        double avgQuality = 0;
        double avgClean = 0;
        double avgSpeed = 0;
        double avgPrice = 0;
        if (vendorFeedback.isNotEmpty) {
          avgQuality = vendorFeedback.map((r) => r.ratingFoodQuality).average();
          avgClean = vendorFeedback.map((r) => r.ratingCleanliness).average();
          avgSpeed = vendorFeedback.map((r) => r.ratingServiceSpeed).average();
          avgPrice = vendorFeedback.map((r) => r.ratingPriceValue).average();
        }
        double score = (avgQuality + avgClean + avgSpeed + avgPrice) / 4.0;
        final isExcellent = score >= 4.0;
        final isCritical = score > 0.0 && score < 3.0;

        return Card(
          elevation: 3,
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
          child: ExpansionTile(
            leading: CircleAvatar(
              backgroundColor: isExcellent 
                  ? Colors.green[100] 
                  : (isCritical ? Colors.red[100] : Colors.amber[100]),
              child: Icon(
                isExcellent 
                    ? Icons.verified 
                    : (isCritical ? Icons.report_problem : Icons.restaurant_menu),
                color: isExcellent 
                    ? Colors.green 
                    : (isCritical ? Colors.red : Colors.amber[800]),
              ),
            ),
            title: Text(
              vendor.fullName,
              style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
            ),
            subtitle: Text(
              "Licence Desk: ${vendor.info} | Overall: ${score > 0 ? '${score.toStringAsFixed(1)} / 5.0' : 'Unrated'}",
              style: const TextStyle(fontSize: 12),
            ),
            children: [
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 16.0, vertical: 12.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    const Divider(),
                    _barMetric("Dish Taste Evaluation", avgQuality),
                    _barMetric("Stall Sanitation & Waste Management", avgClean),
                    _barMetric("Counter Delivery Velocity", avgSpeed),
                    _barMetric("Value Benchmark Index", avgPrice),
                    const SizedBox(height: 16),

                    RechartsLineChart(
                      orders: provider.allOrders.where((o) => o.vendorId == vendor.id).toList(),
                      vendorId: vendor.id ?? 1,
                    ),
                    const SizedBox(height: 16),

                    const Divider(),
                    const Text(
                      "SCHOLAR REVIEW COMMENTS",
                      style: TextStyle(fontWeight: FontWeight.bold, fontSize: 13, color: Colors.blueGrey, letterSpacing: 0.5),
                    ),
                    const SizedBox(height: 8),
                    vendorFeedback.isEmpty
                        ? const Padding(
                            padding: EdgeInsets.symmetric(vertical: 8.0),
                            child: Text(
                              "No student review comments logged for this vendor.",
                              style: TextStyle(fontSize: 12, color: Colors.black54, fontStyle: FontStyle.italic),
                            ),
                          )
                        : ListView.builder(
                            shrinkWrap: true,
                            physics: const NeverScrollableScrollPhysics(),
                            itemCount: vendorFeedback.length,
                            itemBuilder: (context, fbIndex) {
                              final fb = vendorFeedback[fbIndex];
                              final score = (fb.ratingFoodQuality + fb.ratingCleanliness + fb.ratingServiceSpeed + fb.ratingPriceValue) / 4.0;
                              final dateStr = DateFormat('jm - d MMM yyyy').format(DateTime.fromMillisecondsSinceEpoch(fb.timestamp));

                              return Card(
                                elevation: 0.5,
                                color: Colors.grey[50],
                                margin: const EdgeInsets.only(bottom: 6),
                                shape: RoundedRectangleBorder(
                                  side: BorderSide(color: Colors.grey[200]!),
                                  borderRadius: BorderRadius.circular(8),
                                ),
                                child: ListTile(
                                  dense: true,
                                  leading: CircleAvatar(
                                    radius: 16,
                                    backgroundColor: Colors.amberAccent[100],
                                    child: Text(
                                      score.toStringAsFixed(1),
                                      style: const TextStyle(fontSize: 10, fontWeight: FontWeight.bold, color: Colors.black87),
                                    ),
                                  ),
                                  title: Text(
                                    fb.comment.isNotEmpty ? fb.comment : "No comments left.",
                                    style: const TextStyle(fontSize: 12, fontStyle: FontStyle.italic),
                                  ),
                                  subtitle: Text(
                                    "Posted: $dateStr",
                                    style: const TextStyle(fontSize: 9, color: Colors.grey),
                                  ),
                                  trailing: IconButton(
                                    icon: const Icon(Icons.delete_outline, color: Colors.redAccent, size: 18),
                                    tooltip: "Delete Review",
                                    onPressed: () => _confirmDeleteFeedback(context, provider, fb),
                                  ),
                                ),
                              );
                            },
                          ),
                    const Divider(),
                    const SizedBox(height: 12),

                    ElevatedButton.icon(
                      icon: provider.isAnalyzing
                          ? const SizedBox(width: 16, height: 16, child: CircularProgressIndicator(color: Colors.white, strokeWidth: 2))
                          : const Icon(Icons.analytics),
                      onPressed: provider.isAnalyzing
                          ? null
                          : () {
                              provider.runGeminiVendorAnalytics(vendor, vendorFeedback, provider.allOrders.where((o) => o.vendorId == vendor.id).length);
                            },
                      label: const Text("GENERATE QUALITY COMPLIANCE BULLETIN"),
                    ),

                    if (provider.isAnalyzing) ...[
                      const SizedBox(height: 12),
                      const LinearProgressIndicator(),
                    ],

                    if (provider.aiAnalysisText != null) ...[
                      const SizedBox(height: 16),
                      Container(
                        padding: const EdgeInsets.all(12),
                        decoration: BoxDecoration(
                          color: Colors.grey[100],
                          border: Border.all(color: Colors.grey[300]!),
                          borderRadius: BorderRadius.circular(8),
                        ),
                        child: Text(
                          provider.aiAnalysisText!,
                          style: const TextStyle(
                            fontFamily: 'monospace',
                            fontSize: 11,
                            height: 1.4,
                            color: Colors.black87,
                          ),
                        ),
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
  // TAB 2: VENDORS & USERS MANAGER (CRUD)
  // ==========================================
  Widget _buildVendorCRUD(BuildContext context, CafeteriaProvider provider) {
    return DefaultTabController(
      length: 2,
      child: Column(
        children: [
          TabBar(
            labelColor: Theme.of(context).colorScheme.primary,
            unselectedLabelColor: Colors.grey,
            indicatorColor: Theme.of(context).colorScheme.primary,
            tabs: const [
              Tab(icon: Icon(Icons.storefront), text: "Food Merchants"),
              Tab(icon: Icon(Icons.school), text: "Scholars (Students)"),
            ],
          ),
          Expanded(
            child: TabBarView(
              children: [
                _buildMerchantsList(context, provider),
                _buildScholarsList(context, provider),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildMerchantsList(BuildContext context, CafeteriaProvider provider) {
    final vendors = provider.allVendors;
    if (vendors.isEmpty) {
      return const Center(child: Text("No food merchants registered."));
    }
    return ListView.builder(
      padding: const EdgeInsets.all(12),
      itemCount: vendors.length,
      itemBuilder: (context, index) {
        final vendor = vendors[index];
        return Card(
          elevation: 2,
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
          child: Padding(
            padding: const EdgeInsets.symmetric(vertical: 12.0, horizontal: 16.0),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            vendor.fullName,
                            style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
                          ),
                          const SizedBox(height: 4),
                          Text("Brand: ${vendor.info}", style: TextStyle(color: Colors.grey[700], fontSize: 13)),
                          Text("Username: ${vendor.username}", style: TextStyle(color: Colors.grey[500], fontSize: 11)),
                        ],
                      ),
                    ),
                    Row(
                      children: [
                        IconButton(
                          icon: const Icon(Icons.edit, color: Colors.blue),
                          tooltip: "Edit Merchant",
                          onPressed: () => _showEditVendorDialog(context, provider, vendor),
                        ),
                        IconButton(
                          icon: const Icon(Icons.delete, color: Colors.red),
                          tooltip: "Decommission",
                          onPressed: () => _confirmDeleteVendor(context, provider, vendor),
                        ),
                      ],
                    ),
                  ],
                ),
                const Divider(),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    const Text(
                      "QA Simulation Portal",
                      style: TextStyle(fontWeight: FontWeight.bold, fontSize: 12, color: Colors.blueGrey),
                    ),
                    TextButton.icon(
                      style: TextButton.styleFrom(
                        backgroundColor: Theme.of(context).colorScheme.secondary.withOpacity(0.15),
                        foregroundColor: Theme.of(context).colorScheme.primary,
                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
                      ),
                      icon: const Icon(Icons.admin_panel_settings, size: 16),
                      label: const Text("SIMULATE BOOTH", style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold)),
                      onPressed: () {
                        provider.startImpersonation(vendor);
                        Navigator.pushReplacementNamed(context, '/vendor_home');
                      },
                    ),
                  ],
                ),
              ],
            ),
          ),
        );
      },
    );
  }

  Widget _buildScholarsList(BuildContext context, CafeteriaProvider provider) {
    return FutureBuilder<List<User>>(
      future: provider.getAllUsers(),
      builder: (context, snapshot) {
        if (snapshot.connectionState == ConnectionState.waiting) {
          return const Center(child: CircularProgressIndicator());
        }
        if (snapshot.hasError || !snapshot.hasData) {
          return const Center(child: Text("Failed to fetch students."));
        }
        final scholars = snapshot.data!.where((u) => u.role == 'STUDENT').toList();
        if (scholars.isEmpty) {
          return const Center(child: Text("No student active accounts."));
        }
        return ListView.builder(
          padding: const EdgeInsets.all(12),
          itemCount: scholars.length,
          itemBuilder: (context, index) {
            final student = scholars[index];
            return Card(
              elevation: 2,
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
              child: ListTile(
                leading: CircleAvatar(
                  backgroundColor: Theme.of(context).colorScheme.primary.withOpacity(0.12),
                  child: const Icon(Icons.school, color: Colors.blue),
                ),
                title: Text(student.fullName, style: const TextStyle(fontWeight: FontWeight.bold)),
                subtitle: Text("Scholar ID: ${student.info}\nUsername: ${student.username}", style: const TextStyle(fontSize: 12)),
                trailing: TextButton.icon(
                  style: TextButton.styleFrom(
                    backgroundColor: Colors.amber.withOpacity(0.15),
                    foregroundColor: const Color(0xFF8B5E00),
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
                  ),
                  icon: const Icon(Icons.admin_panel_settings, size: 16),
                  label: const Text("SIMULATE", style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold)),
                  onPressed: () {
                    provider.startImpersonation(student);
                    Navigator.pushReplacementNamed(context, '/student_home');
                  },
                ),
              ),
            );
          },
        );
      },
    );
  }

  void _showAddVendorDialog(BuildContext context, CafeteriaProvider provider) {
    final formKey = GlobalKey<FormState>();
    final usernameCtrl = TextEditingController();
    final pinCtrl = TextEditingController();
    final fullNameCtrl = TextEditingController();
    final brandCtrl = TextEditingController();

    showDialog(
      context: context,
      builder: (ctx) {
        return AlertDialog(
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
          title: const Text("REGISTER NEW WA-BOOTH"),
          content: SingleChildScrollView(
            child: Form(
              key: formKey,
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  TextFormField(
                    controller: usernameCtrl,
                    decoration: const InputDecoration(labelText: "Username", border: OutlineInputBorder()),
                    validator: (val) => val == null || val.trim().isEmpty ? "Enter unique username" : null,
                  ),
                  const SizedBox(height: 12),
                  TextFormField(
                    controller: pinCtrl,
                    obscureText: true,
                    keyboardType: TextInputType.number,
                    decoration: const InputDecoration(labelText: "Access PIN (4+ digits)", border: OutlineInputBorder()),
                    validator: (val) => val == null || val.length < 4 ? "PIN must be 4+ digits" : null,
                  ),
                  const SizedBox(height: 12),
                  TextFormField(
                    controller: fullNameCtrl,
                    decoration: const InputDecoration(labelText: "Vendor Name", border: OutlineInputBorder()),
                    validator: (val) => val == null || val.trim().isEmpty ? "Enter full name" : null,
                  ),
                  const SizedBox(height: 12),
                  TextFormField(
                    controller: brandCtrl,
                    decoration: const InputDecoration(labelText: "Merchant Brand / Booth Info", border: OutlineInputBorder()),
                    validator: (val) => val == null || val.trim().isEmpty ? "Enter brand summary" : null,
                  ),
                ],
              ),
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(ctx),
              child: const Text("CANCEL"),
            ),
            ElevatedButton(
              style: ElevatedButton.styleFrom(minimumSize: const Size(120, 40)),
              onPressed: () async {
                if (!formKey.currentState!.validate()) return;
                final success = await provider.addVendor(
                  username: usernameCtrl.text.trim(),
                  pinCode: pinCtrl.text.trim(),
                  fullName: fullNameCtrl.text.trim(),
                  info: brandCtrl.text.trim(),
                );
                if (mounted) {
                  Navigator.pop(ctx);
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(
                      content: Text(success ? "Vendor merchant launched successfully!" : "Failed to register vendor (username may be taken)"),
                      backgroundColor: success ? Colors.green : Colors.red,
                    ),
                  );
                }
              },
              child: const Text("LAUNCH"),
            )
          ],
        );
      },
    );
  }

  void _showEditVendorDialog(BuildContext context, CafeteriaProvider provider, User vendor) {
    final formKey = GlobalKey<FormState>();
    final fullNameCtrl = TextEditingController(text: vendor.fullName);
    final brandCtrl = TextEditingController(text: vendor.info);
    final pinCtrl = TextEditingController();

    showDialog(
      context: context,
      builder: (ctx) {
        return AlertDialog(
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
          title: const Text("EDIT MERCHANT DATA"),
          content: SingleChildScrollView(
            child: Form(
              key: formKey,
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text("Merchant ID: ${vendor.id}", style: const TextStyle(color: Colors.grey, fontSize: 12)),
                  const SizedBox(height: 12),
                  TextFormField(
                    controller: fullNameCtrl,
                    decoration: const InputDecoration(labelText: "Vendor Name", border: OutlineInputBorder()),
                    validator: (val) => val == null || val.trim().isEmpty ? "Name is required" : null,
                  ),
                  const SizedBox(height: 12),
                  TextFormField(
                    controller: brandCtrl,
                    decoration: const InputDecoration(labelText: "Brand Info / Booth", border: OutlineInputBorder()),
                    validator: (val) => val == null || val.trim().isEmpty ? "Brand is required" : null,
                  ),
                  const SizedBox(height: 12),
                  TextFormField(
                    controller: pinCtrl,
                    obscureText: true,
                    keyboardType: TextInputType.number,
                    decoration: const InputDecoration(
                      labelText: "New PIN (leave blank to keep current)",
                      border: OutlineInputBorder(),
                    ),
                    validator: (val) {
                      if (val != null && val.isNotEmpty && val.length < 4) {
                        return "PIN must be 4+ digits";
                      }
                      return null;
                    },
                  ),
                ],
              ),
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(ctx),
              child: const Text("CANCEL"),
            ),
            ElevatedButton(
              style: ElevatedButton.styleFrom(minimumSize: const Size(120, 40)),
              onPressed: () async {
                if (!formKey.currentState!.validate()) return;
                
                final updated = vendor.copyWith(
                  fullName: fullNameCtrl.text.trim(),
                  info: brandCtrl.text.trim(),
                );
                
                final success = await provider.updateVendor(updated, pinCtrl.text.isNotEmpty ? pinCtrl.text : null);
                if (mounted) {
                  Navigator.pop(ctx);
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(
                      content: Text(success ? "Merchant credentials updated." : "Failed to update vendor."),
                      backgroundColor: success ? Colors.green : Colors.red,
                    ),
                  );
                }
              },
              child: const Text("SAVE CHANGES"),
            )
          ],
        );
      },
    );
  }

  void _confirmDeleteVendor(BuildContext context, CafeteriaProvider provider, User vendor) {
    showDialog(
      context: context,
      builder: (ctx) {
        return AlertDialog(
          title: const Text("DECOMMISSION FOOD MERCHANT"),
          content: Text("Are you absolutely sure you want to delete ${vendor.fullName}? This will cascade delete their entire active menu, feedback log, and active deliveries!"),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(ctx),
              child: const Text("CANCEL"),
            ),
            ElevatedButton(
              style: ElevatedButton.styleFrom(backgroundColor: Colors.red, foregroundColor: Colors.white),
              onPressed: () async {
                final success = await provider.deleteVendor(vendor.id!);
                if (mounted) {
                  Navigator.pop(ctx);
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(
                      content: Text(success ? "Merchant deleted successfully!" : "Failed to delete merchant."),
                      backgroundColor: success ? Colors.green : Colors.red,
                    ),
                  );
                }
              },
              child: const Text("DELETE DESTRUCTIVELY"),
            ),
          ],
        );
      },
    );
  }

  void _confirmDeleteFeedback(BuildContext context, CafeteriaProvider provider, Feedback fb) {
    showDialog(
      context: context,
      builder: (ctx) {
        return AlertDialog(
          title: const Text("DELETE STUDENT REVIEW"),
          content: Text("Are you sure you want to delete this review comment?\n\n\"${fb.comment}\""),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(ctx),
              child: const Text("CANCEL"),
            ),
            ElevatedButton(
              style: ElevatedButton.styleFrom(backgroundColor: Colors.red, foregroundColor: Colors.white),
              onPressed: () async {
                final success = await provider.deleteFeedback(fb.id!);
                if (mounted) {
                  Navigator.pop(ctx);
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(
                      content: Text(success ? "Review comment deleted." : "Failed to delete review."),
                      backgroundColor: success ? Colors.green : Colors.red,
                    ),
                  );
                }
              },
              child: const Text("DELETE"),
            ),
          ],
        );
      },
    );
  }

  void _confirmDeleteAuditLog(BuildContext context, CafeteriaProvider provider, AuditLog log) {
    showDialog(
      context: context,
      builder: (ctx) {
        return AlertDialog(
          title: const Text("PURGE AUDIT LOG RECORD"),
          content: Text("Are you sure you want to permanently delete this audit log record?\n\nAction: ${log.action}\nDetails: ${log.details}"),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(ctx),
              child: const Text("CANCEL"),
            ),
            ElevatedButton(
              style: ElevatedButton.styleFrom(backgroundColor: Colors.red, foregroundColor: Colors.white),
              onPressed: () async {
                final success = await provider.deleteAuditLog(log.id!);
                if (mounted) {
                  Navigator.pop(ctx);
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(
                      content: Text(success ? "Audit record purged." : "Failed to purge record."),
                      backgroundColor: success ? Colors.green : Colors.red,
                    ),
                  );
                }
              },
              child: const Text("PURGE"),
            ),
          ],
        );
      },
    );
  }

  // ==========================================
  // TAB 3: AUDIT LEDGER LOGS
  // ==========================================
  Widget _buildAuditLedgerLogs(BuildContext context, CafeteriaProvider provider) {
    var logs = provider.auditLogs;

    if (logs.isEmpty) {
      return const Center(child: Text("Empty audit registry. System operating normally."));
    }

    return ListView.builder(
      padding: const EdgeInsets.all(12),
      itemCount: logs.length,
      itemBuilder: (context, index) {
        final log = logs[index];
        final timeStr = DateFormat('jm - d MMM yyyy').format(DateTime.fromMillisecondsSinceEpoch(log.timestamp));
        final isInit = log.action == 'SYSTEM_INIT';
        final isAuth = log.action.contains('LOGIN') || log.action.contains('REGISTRATION');
        final isFin = log.action.contains('WALLET');

        return Card(
          margin: const EdgeInsets.only(bottom: 8),
          color: isInit 
              ? Colors.green[50] 
              : (isFin ? Colors.blue[50] : Colors.white),
          child: ListTile(
            leading: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Icon(
                  isInit 
                      ? Icons.settings_power 
                      : (isFin ? Icons.monetization_on : (isAuth ? Icons.fingerprint : Icons.history)),
                  color: isInit 
                      ? Colors.green 
                      : (isFin ? Colors.blue : Colors.blueGrey),
                ),
              ],
            ),
            title: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(log.action, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13, letterSpacing: 0.5)),
                Text("UID: ${log.userId}", style: const TextStyle(color: Colors.grey, fontSize: 10)),
              ],
            ),
            subtitle: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const SizedBox(height: 4),
                Text(log.details, style: const TextStyle(fontSize: 11, color: Colors.black87)),
                const SizedBox(height: 4),
                Text(timeStr, style: const TextStyle(fontSize: 9, color: Colors.grey)),
              ],
            ),
            trailing: IconButton(
              icon: const Icon(Icons.delete_outline, color: Colors.redAccent),
              tooltip: "Purge Audit Log",
              onPressed: () => _confirmDeleteAuditLog(context, provider, log),
            ),
          ),
        );
      },
    );
  }

  // ==========================================
  // TAB 4: ACADEMIC STATISTICS
  // ==========================================
  Widget _buildAcademicStatistics(BuildContext context, CafeteriaProvider provider) {
    final foodCount = provider.allFoodItems.length;
    final totalOrders = provider.allOrders.length;
    final feedbackCount = provider.allFeedback.length;
    final vendorCount = provider.allVendors.length;

    return SingleChildScrollView(
      padding: const EdgeInsets.all(16.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          const Text(
            "ACCRA TECHNICAL UNIVERSITY FOOD QUALITY REPORT",
            textAlign: TextAlign.center,
            style: TextStyle(fontWeight: FontWeight.bold, fontSize: 12, color: Colors.grey, letterSpacing: 0.8),
          ),
          const SizedBox(height: 16),
          _statGridTile("Total Registered Merchants", vendorCount.toString(), Icons.storefront, Colors.purple),
          const SizedBox(height: 12),
          _statGridTile("Dishes Listed in Menu", foodCount.toString(), Icons.fastfood, Colors.amber),
          const SizedBox(height: 12),
          _statGridTile("Completed Orders Processed", totalOrders.toString(), Icons.receipt, Colors.green),
          const SizedBox(height: 12),
          _statGridTile("Complaints / Reviews Logged", feedbackCount.toString(), Icons.rate_review, Colors.blue),
          const SizedBox(height: 24),
          const Text(
            "CUSTOMER SATISFACTION SENTIMENT (D3.JS)",
            style: TextStyle(fontWeight: FontWeight.bold, fontSize: 11, color: Colors.grey, letterSpacing: 0.8),
          ),
          const SizedBox(height: 12),
          D3SentimentChart(feedbackList: provider.allFeedback),
          const SizedBox(height: 24),
          Card(
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
            child: const Padding(
              padding: EdgeInsets.all(16.0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text("Audit Verification Guidelines", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 14)),
                  SizedBox(height: 8),
                  Text("All vendors operating within Accra Technical University boundaries must maintain an overall compliance threshold score of 3.0 / 5.0. Scores dipping under this trigger automated QA Warnings in compliance board bulletins.",
                      style: TextStyle(fontSize: 11, color: Colors.black54, height: 1.4)),
                ],
              ),
            ),
          )
        ],
      ),
    );
  }

  Widget _statGridTile(String label, String value, IconData icon, Color color) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Row(
          children: [
            CircleAvatar(
              backgroundColor: color.withOpacity(0.12),
              child: Icon(icon, color: color),
            ),
            const SizedBox(width: 16),
            Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(label, style: const TextStyle(fontSize: 12, color: Colors.grey, fontWeight: FontWeight.bold)),
                const SizedBox(height: 4),
                Text(value, style: const TextStyle(fontSize: 22, fontWeight: FontWeight.bold)),
              ],
            )
          ],
        ),
      ),
    );
  }

  Widget _barMetric(String label, double val) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(label, style: const TextStyle(fontSize: 11, fontWeight: FontWeight.w600)),
              Text("${val.toStringAsFixed(1)} / 5.0", style: const TextStyle(fontSize: 11, fontWeight: FontWeight.bold, color: Colors.blueAccent)),
            ],
          ),
          const SizedBox(height: 4),
          ClipRRect(
            borderRadius: BorderRadius.circular(4),
            child: LinearProgressIndicator(
              value: val / 5.0,
              color: Colors.amber[700],
              minHeight: 6,
              backgroundColor: Colors.grey[200],
            ),
          )
        ],
      ),
    );
  }
}

// Extensions
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
