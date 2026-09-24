import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../core/network/api_responses.dart';
import '../../core/theme/app_theme.dart';
import '../providers/admin_state_provider.dart';
import '../providers/cafeteria_provider.dart';
import '../widgets/reference_design.dart';

class ReferenceAdminScreen extends StatefulWidget {
  const ReferenceAdminScreen({super.key});
  @override
  State<ReferenceAdminScreen> createState() => _ReferenceAdminScreenState();
}

class _ReferenceAdminScreenState extends State<ReferenceAdminScreen> {
  String page = 'Dashboard';
  static const mobilePages = [
    'Dashboard',
    'Users',
    'Vendors',
    'Orders',
    'Finance',
  ];

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      final cafeteria = context.read<CafeteriaProvider>();
      final admin = context.read<AdminStateProvider>();
      admin.setToken(cafeteria.authToken);
      admin.loadAll(includeFinance: true, includeSettings: true);
    });
  }

  @override
  Widget build(BuildContext context) {
    final cafeteria = context.watch<CafeteriaProvider>();
    final admin = context.watch<AdminStateProvider>();
    return Scaffold(
      body: SafeArea(
        child: LayoutBuilder(
          builder: (context, constraints) {
            if (constraints.maxWidth < 900) return _mobile(cafeteria, admin);
            return Row(
              children: [
                Sidebar(
                  selected: page,
                  onSelected: (value) => setState(() => page = value),
                  onLogout: () {
                    cafeteria.logOut();
                    Navigator.pushReplacementNamed(context, '/login');
                  },
                ),
                Expanded(child: _body(admin)),
              ],
            );
          },
        ),
      ),
    );
  }

  Widget _mobile(CafeteriaProvider cafeteria, AdminStateProvider admin) =>
      Column(
        children: [
          Container(
            height: 64,
            color: AppTheme.primary,
            padding: const EdgeInsets.symmetric(horizontal: 16),
            child: Row(
              children: [
                AtuBrand.title(compact: true),
                const Spacer(),
                IconButton(
                    onPressed: () =>
                        admin.loadAll(includeFinance: true, includeSettings: true),
                    icon: const Icon(Icons.refresh, color: Colors.white)),
                IconButton(
                  onPressed: () {
                    cafeteria.logOut();
                    Navigator.pushReplacementNamed(context, '/login');
                  },
                  icon: const Icon(Icons.logout, color: Colors.white),
                ),
              ],
            ),
          ),
          Expanded(child: _body(admin)),
          NavigationBar(
            selectedIndex:
                mobilePages.indexOf(page).clamp(0, mobilePages.length - 1),
            onDestinationSelected: (index) =>
                setState(() => page = mobilePages[index]),
            destinations: const [
              NavigationDestination(
                  icon: Icon(Icons.dashboard), label: 'Dashboard'),
              NavigationDestination(icon: Icon(Icons.people), label: 'Users'),
              NavigationDestination(icon: Icon(Icons.store), label: 'Vendors'),
              NavigationDestination(
                  icon: Icon(Icons.receipt_long), label: 'Orders'),
              NavigationDestination(
                  icon: Icon(Icons.account_balance_wallet), label: 'Finance'),
            ],
          ),
        ],
      );

  Widget _body(AdminStateProvider admin) {
    // Standard loading and error states while the initial admin data loads.
    if (admin.loading && admin.dashboardData.isEmpty) {
      return const Center(child: CircularProgressIndicator());
    }
    if (!admin.loading && admin.error != null && admin.dashboardData.isEmpty) {
      return ListView(
        padding: const EdgeInsets.all(24),
        children: [
          ReferenceCard(
            child: Padding(
              padding: const EdgeInsets.all(24),
              child: Column(
                children: [
                  const Icon(Icons.cloud_off, size: 54, color: AppTheme.primary),
                  const SizedBox(height: 12),
                  const Text('Unable to load the admin dashboard',
                      style: TextStyle(
                          fontSize: 18, fontWeight: FontWeight.w900)),
                  const SizedBox(height: 6),
                  Text(admin.error!,
                      textAlign: TextAlign.center,
                      style: const TextStyle(color: AppTheme.textMuted)),
                  const SizedBox(height: 18),
                  FilledButton.icon(
                    onPressed: () => admin.loadAll(
                        includeFinance: true, includeSettings: true),
                    icon: const Icon(Icons.refresh),
                    label: const Text('Retry'),
                  ),
                ],
              ),
            ),
          ),
        ],
      );
    }

    if (page == 'Dashboard') return _dashboard(admin);
    if (page == 'Finance') return _finance(admin);
    if (page == 'Vendors') return _vendorsPage(admin);
    if (page == 'Command Center') return _commandCenter(admin);
    if (page == 'Security Alerts') return _securityAlerts(admin);
    if (page == 'Settings') return _settings(admin);

    final List<Map<String, dynamic>> rows;
    if (page == 'Users') {
      rows = List<Map<String, dynamic>>.from(admin.users);
    } else {
      rows = List<Map<String, dynamic>>.from(admin.orders);
    }

    return ListView(
      padding: const EdgeInsets.all(24),
      children: [
        Text('All $page',
            style: const TextStyle(
                fontSize: 24,
                fontWeight: FontWeight.w900,
                color: AppTheme.textDark)),
        const SizedBox(height: 14),
        ReferenceCard(
          child: rows.isEmpty
              ? const Padding(
                  padding: EdgeInsets.all(24),
                  child: Text('No records available.'))
              : Column(
                  children: rows.take(20).toList().asMap().entries.map(
                      (entry) {
                    final row = entry.value;
                    final title = row['fullName'] ??
                        row['full_name'] ??
                        row['name'] ??
                        row['id'] ??
                        'Record';
                    final subtitle = row['username'] ??
                        row['email'] ??
                        row['status'] ??
                        row['role'] ??
                        '';
                    return Column(
                      children: [
                        if (entry.key > 0) const Divider(height: 1),
                        ListTile(
                          onTap: () => _showRecordDetails(row),
                          title: Text('$title',
                              style: const TextStyle(
                                  fontWeight: FontWeight.w800)),
                          subtitle: Text('$subtitle'),
                          trailing: const Icon(Icons.chevron_right),
                        ),
                      ],
                    );
                  }).toList(),
                ),
        ),
      ],
    );
  }

  /// Displays a professional detail sheet for a clicked Users/Orders row.
  void _showRecordDetails(Map<String, dynamic> row) {
    final title = row['fullName'] ??
        row['full_name'] ??
        row['name'] ??
        row['type'] ??
        (row['id'] != null ? 'Record #${row['id']}' : 'Record');
    final fields = <String, String>{};
    row.forEach((key, value) {
      final rendered = _detailValue(value);
      if (rendered.isEmpty) return;
      if (_detailLabel(key) == 'ID' && rendered == '$title') return;
      fields[_detailLabel(key)] = rendered;
    });
    if (fields.isEmpty) {
      fields['Details'] = 'No additional details are available for this record.';
    }

    showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      builder: (sheetContext) => SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.fromLTRB(24, 12, 24, 24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(children: [
                const Icon(Icons.person_pin_outlined,
                    color: AppTheme.primary, size: 28),
                const SizedBox(width: 10),
                Expanded(
                  child: Text('$title',
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                          fontSize: 20, fontWeight: FontWeight.w900)),
                ),
                IconButton(
                    onPressed: () => Navigator.pop(sheetContext),
                    icon: const Icon(Icons.close)),
              ]),
              const SizedBox(height: 16),
              ...fields.entries.map(
                (field) => Padding(
                  padding: const EdgeInsets.only(bottom: 12),
                  child: Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      SizedBox(
                        width: 140,
                        child: Text(
                          field.key,
                          style: const TextStyle(
                              color: AppTheme.textMuted,
                              fontWeight: FontWeight.w600),
                        ),
                      ),
                      Expanded(
                          child:
                              Text(field.value, textAlign: TextAlign.start)),
                    ],
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  String _detailValue(dynamic value) {
    if (value == null) return '';
    if (value is Map) {
      return value['fullName']?.toString() ??
          value['full_name']?.toString() ??
          value['name']?.toString() ??
          value['username']?.toString() ??
          value['store_name']?.toString() ??
          '';
    }
    final text = value.toString().trim();
    if (text.isEmpty) return '';
    final parsed = DateTime.tryParse(text);
    if (parsed != null &&
        text.contains('T') &&
        (text.contains('-') || text.contains(':'))) {
      return '${parsed.day.toString().padLeft(2, '0')}-${parsed.month.toString().padLeft(2, '0')}-${parsed.year} '
          '${parsed.hour.toString().padLeft(2, '0')}:${parsed.minute.toString().padLeft(2, '0')}';
    }
    return text;
  }

  String _detailLabel(String key) {
    const overrides = {
      'fullName': 'Name',
      'full_name': 'Name',
      'username': 'Username',
      'email': 'Email',
      'contact_email': 'Contact email',
      'account_status': 'Status',
      'admin_level': 'Admin level',
      'operational_status': 'Operational status',
      'total_price': 'Total (GH₵)',
      'vendor_id': 'Vendor ID',
      'user_id': 'User ID',
      'created_at': 'Created',
      'updated_at': 'Updated',
      'two_factor_enabled': '2FA enabled',
      'profile_completed': 'Profile completed',
    };
    if (overrides.containsKey(key)) return overrides[key]!;
    return key
        .replaceAll('_', ' ')
        .replaceFirst(key[0], key[0].toUpperCase());
  }

  Widget _vendorsPage(AdminStateProvider admin) {
    return RefreshIndicator(
      onRefresh: () => admin.loadAll(includeFinance: true),
      child: ListView(
        padding: const EdgeInsets.all(24),
        children: [
          Row(
            children: [
              const Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('Vendors',
                        style: TextStyle(
                            fontSize: 24,
                            fontWeight: FontWeight.w900,
                            color: AppTheme.textDark)),
                    SizedBox(height: 4),
                    Text(
                        'Create vendor accounts and manage their operational status.'),
                  ],
                ),
              ),
              FilledButton.icon(
                onPressed: () => _showCreateVendorDialog(admin),
                icon: const Icon(Icons.add_business),
                label: const Text('Add Vendor'),
              ),
            ],
          ),
          const SizedBox(height: 18),
          if (admin.vendors.isEmpty)
            const ReferenceCard(
              child: Padding(
                padding: EdgeInsets.all(32),
                child: Column(
                  children: [
                    Icon(Icons.storefront_outlined,
                        size: 54, color: AppTheme.primary),
                    SizedBox(height: 12),
                    Text('No vendors yet',
                        style: TextStyle(
                            fontSize: 18, fontWeight: FontWeight.w900)),
                    SizedBox(height: 6),
                    Text(
                        'Create the first vendor account to start publishing cafeteria menus.',
                        textAlign: TextAlign.center),
                  ],
                ),
              ),
            )
          else
            ...admin.vendors.map<Widget>((raw) => _vendorManagementCard(
                admin, Map<String, dynamic>.from(raw as Map))),
        ],
      ),
    );
  }

  Widget _vendorManagementCard(
      AdminStateProvider admin, Map<String, dynamic> vendor) {
    final user = vendor['user'] is Map
        ? Map<String, dynamic>.from(vendor['user'])
        : <String, dynamic>{};
    final id = int.tryParse('${vendor['id'] ?? 0}') ?? 0;
    final name =
        '${vendor['store_name'] ?? vendor['name'] ?? user['fullName'] ?? 'Campus Vendor'}';
    final owner = '${vendor['name'] ?? user['fullName'] ?? 'Vendor owner'}';
    final email = '${vendor['contact_email'] ?? user['username'] ?? ''}';
    final location =
        '${vendor['location'] ?? vendor['location_within_campus'] ?? 'Campus'}';
    final status = '${vendor['operational_status'] ?? 'ACTIVE'}'.toUpperCase();
    final active = status == 'ACTIVE';

    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: ReferenceCard(
        child: Row(
          children: [
            Container(
              width: 58,
              height: 58,
              decoration: BoxDecoration(
                  color: AppTheme.primary.withValues(alpha: .09),
                  borderRadius: BorderRadius.circular(16)),
              child: const Icon(Icons.storefront_rounded,
                  color: AppTheme.primary, size: 30),
            ),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(name,
                      style: const TextStyle(
                          fontSize: 17,
                          fontWeight: FontWeight.w900,
                          color: AppTheme.textDark)),
                  const SizedBox(height: 4),
                  Text('Owner: $owner',
                      style: const TextStyle(color: AppTheme.textMuted)),
                  const SizedBox(height: 3),
                  Text('$email • $location',
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                          fontSize: 12, color: AppTheme.textMuted)),
                ],
              ),
            ),
            StatusPill(status),
            const SizedBox(width: 8),
            PopupMenuButton<String>(
              tooltip: 'Vendor actions',
              onSelected: (value) async {
                if (!admin.busy) {
                  final ok = await admin.changeVendorStatus(id, value);
                  if (!mounted) return;
                  showAppMessage(
                    context,
                    message: ok
                        ? (admin.actionMessage ?? 'Vendor status updated.')
                        : (admin.error ?? 'Could not update vendor status.'),
                    isError: !ok,
                  );
                }
              },
              itemBuilder: (_) => [
                if (!active)
                  const PopupMenuItem(
                      value: 'ACTIVE', child: Text('Activate vendor')),
                if (active)
                  const PopupMenuItem(
                      value: 'INACTIVE', child: Text('Set inactive')),
                const PopupMenuItem(
                    value: 'SUSPENDED', child: Text('Suspend vendor')),
                const PopupMenuItem(
                    value: 'PENDING', child: Text('Set pending')),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Future<void> _showCreateVendorDialog(AdminStateProvider admin) async {
    final formKey = GlobalKey<FormState>();
    final fullName = TextEditingController();
    final storeName = TextEditingController();
    final email = TextEditingController();
    final password = TextEditingController();
    final location = TextEditingController();
    final contactEmail = TextEditingController();
    final contactInfo = TextEditingController();
    bool obscure = true;
    bool saving = false;

    await showDialog<void>(
      context: context,
      barrierDismissible: false,
      builder: (dialogContext) => StatefulBuilder(
        builder: (context, setDialogState) {
          Future<void> submit() async {
            if (!formKey.currentState!.validate()) return;
            setDialogState(() => saving = true);
            final ok = await admin.createVendor(
              email: email.text.trim(),
              password: password.text,
              fullName: fullName.text.trim(),
              storeName: storeName.text.trim(),
              location: location.text.trim(),
              contactEmail: contactEmail.text.trim().isEmpty
                  ? null
                  : contactEmail.text.trim(),
              contactInfo: contactInfo.text.trim().isEmpty
                  ? null
                  : contactInfo.text.trim(),
            );
            if (!mounted || !dialogContext.mounted) return;
            if (ok) {
              Navigator.of(dialogContext).pop();
              showAppMessage(
                this.context,
                message:
                    admin.actionMessage ?? 'Vendor account created successfully.',
              );
            } else {
              setDialogState(() => saving = false);
              showAppMessage(
                this.context,
                message: admin.error ?? 'Could not create vendor.',
                isError: true,
              );
            }
          }

          return AlertDialog(
            title: const Text('Add Vendor',
                style: TextStyle(fontWeight: FontWeight.w900)),
            content: SizedBox(
              width: 560,
              child: SingleChildScrollView(
                child: Form(
                  key: formKey,
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      const Align(
                          alignment: Alignment.centerLeft,
                          child: Text('Vendor account details',
                              style: TextStyle(color: AppTheme.textMuted))),
                      const SizedBox(height: 14),
                      _field(fullName, 'Owner full name', Icons.person_outline,
                          required: true),
                      const SizedBox(height: 10),
                      _field(storeName, 'Store / vendor name',
                          Icons.storefront_outlined,
                          required: true),
                      const SizedBox(height: 10),
                      _field(email, 'Login email', Icons.email_outlined,
                          required: true, email: true),
                      const SizedBox(height: 10),
                      TextFormField(
                        controller: password,
                        obscureText: obscure,
                        decoration: InputDecoration(
                            prefixIcon: const Icon(Icons.lock_outline),
                            labelText: 'Temporary password',
                            suffixIcon: IconButton(
                                onPressed: () =>
                                    setDialogState(() => obscure = !obscure),
                                icon: Icon(obscure
                                    ? Icons.visibility
                                    : Icons.visibility_off))),
                        validator: (value) => value == null || value.length < 8
                            ? 'Use at least 8 characters.'
                            : null,
                      ),
                      const SizedBox(height: 10),
                      _field(location, 'Campus location (optional)',
                          Icons.location_on_outlined),
                      const SizedBox(height: 10),
                      _field(contactEmail, 'Contact email (optional)',
                          Icons.alternate_email),
                      const SizedBox(height: 10),
                      _field(contactInfo, 'Contact phone / info (optional)',
                          Icons.phone_outlined),
                    ],
                  ),
                ),
              ),
            ),
            actions: [
              TextButton(
                  onPressed:
                      saving ? null : () => Navigator.of(dialogContext).pop(),
                  child: const Text('Cancel')),
              FilledButton.icon(
                  onPressed: saving ? null : submit,
                  icon: saving
                      ? const SizedBox(
                          width: 18,
                          height: 18,
                          child: CircularProgressIndicator(strokeWidth: 2))
                      : const Icon(Icons.person_add_alt_1),
                  label: Text(saving ? 'Creating...' : 'Create Vendor')),
            ],
          );
        },
      ),
    );

    fullName.dispose();
    storeName.dispose();
    email.dispose();
    password.dispose();
    location.dispose();
    contactEmail.dispose();
    contactInfo.dispose();
  }

  Widget _field(TextEditingController controller, String label, IconData icon,
      {bool required = false, bool email = false}) {
    return TextFormField(
      controller: controller,
      keyboardType: email ? TextInputType.emailAddress : TextInputType.text,
      decoration: InputDecoration(prefixIcon: Icon(icon), labelText: label),
      validator: (value) {
        final text = value?.trim() ?? '';
        if (required && text.isEmpty) return '$label is required.';
        if (email &&
            text.isNotEmpty &&
            !RegExp(r'^[^@\s]+@[^@\s]+\.[^@\s]+$').hasMatch(text)) {
          return 'Enter a valid email address.';
        }
        return null;
      },
    );
  }

  Widget _dashboard(AdminStateProvider admin) {
    final data = admin.dashboardData;
    // Vendor performance bars are computed from the live orders loaded by the
    // admin API — never hard-coded.
    final vendorStats = _aggregateVendorStats(admin.orders);
    final vendorRows = vendorStats.entries.toList()
      ..sort((a, b) => (b.value['revenue'] as double)
          .compareTo(a.value['revenue'] as double));
    final maxRevenue =
        vendorRows.isEmpty ? 1.0 : (vendorRows.first.value['revenue'] as double);
    final topVendors = vendorRows.take(5).toList();

    return ListView(
      padding: const EdgeInsets.all(24),
      children: [
        Container(
          padding: const EdgeInsets.all(20),
          decoration: BoxDecoration(
              color: AppTheme.primary, borderRadius: BorderRadius.circular(20)),
          child: const Row(
            children: [
              Expanded(
                  child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                    Text('Welcome, Admin!',
                        style: TextStyle(
                            color: Colors.white,
                            fontSize: 24,
                            fontWeight: FontWeight.w900)),
                    SizedBox(height: 4),
                    Text('Here’s an overview of your cafeteria system.',
                        style: TextStyle(color: Colors.white70))
                  ])),
              Icon(Icons.admin_panel_settings,
                  color: AppTheme.accent, size: 50),
            ],
          ),
        ),
        const SizedBox(height: 18),
        Wrap(
          spacing: 12,
          runSpacing: 12,
          children: [
            MetricTile(
                label: 'Total Users',
                value: '${data['total_users'] ?? data['students'] ?? 0}',
                icon: Icons.people),
            MetricTile(
                label: 'Total Vendors',
                value: '${data['vendors'] ?? data['total_vendors'] ?? 0}',
                icon: Icons.store),
            MetricTile(
                label: 'Total Orders',
                value: '${data['total_orders'] ?? data['orders'] ?? 0}',
                icon: Icons.receipt_long),
            MetricTile(
                label: 'Orders Today',
                value: '${data['orders_today'] ?? 0}',
                icon: Icons.today),
            MetricTile(
                label: 'Pending Orders',
                value: '${data['pending_orders'] ?? 0}',
                icon: Icons.hourglass_top),
            MetricTile(
                label: 'Sales Today (GH¢)',
                value: (data['sales_today'] ?? 0).toStringAsFixed(2),
                icon: Icons.trending_up),
            MetricTile(
                label: 'Average Rating',
                value: '${data['average_rating'] ?? '—'} ★',
                icon: Icons.star),
            MetricTile(
                label: 'Wallet Balance (GH¢)',
                value: (data['wallet_balance'] ?? 0).toStringAsFixed(2),
                icon: Icons.account_balance_wallet),
          ],
        ),
        const SizedBox(height: 20),
        ReferenceCard(
          child:
              Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            const Text('Vendor Performance Overview',
                style: TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.w900,
                    color: AppTheme.textDark)),
            const SizedBox(height: 6),
            const Text('Revenue from live order data, top 5 vendors.',
                style: TextStyle(color: AppTheme.textMuted, fontSize: 11)),
            const SizedBox(height: 15),
            if (topVendors.isEmpty)
              const Padding(
                padding: EdgeInsets.symmetric(vertical: 16),
                child: Text('No order data yet.'),
              )
            else
              ...topVendors.map((entry) {
                final revenue = entry.value['revenue'] as double;
                return _bar('${entry.key} · ${entry.value['orders']} orders',
                    maxRevenue <= 0 ? 0 : (revenue / maxRevenue).clamp(0, 1),
                    trailing: 'GH¢${revenue.toStringAsFixed(2)}');
              }),
          ]),
        ),
      ],
    );
  }

  Map<String, Map<String, dynamic>> _aggregateVendorStats(
      List<dynamic> orders) {
    final stats = <String, Map<String, dynamic>>{};
    for (final order in orders) {
      if (order is! Map) continue;
      final vendor = order['vendor'];
      final name = vendor is Map
          ? (vendor['fullName']?.toString() ??
              vendor['name']?.toString() ??
              'Vendor ${order['vendor_id'] ?? ''}')
          : (order['vendor_name']?.toString() ??
              'Vendor ${order['vendor_id'] ?? ''}');
      final total =
          double.tryParse((order['total_price'] ?? order['totalPrice'] ?? 0).toString()) ??
              0;
      final entry = stats.putIfAbsent(
          name, () => <String, dynamic>{'orders': 0, 'revenue': 0.0});
      entry['orders'] = (entry['orders'] as int) + 1;
      entry['revenue'] = (entry['revenue'] as double) + total;
    }
    return stats;
  }

  Widget _settlementRow(
    BuildContext context,
    AdminStateProvider admin,
    dynamic raw,
  ) {
    final settlement = raw is Map
        ? Map<String, dynamic>.from(raw)
        : <String, dynamic>{};
    final id = int.tryParse(settlement['id']?.toString() ?? '') ?? 0;
    final status = (settlement['status']?.toString() ?? 'UNKNOWN').toUpperCase();
    final amount = double.tryParse(settlement['net_amount']?.toString() ?? '') ?? 0;
    final vendor = settlement['vendor'];
    final vendorName = vendor is Map
        ? (vendor['fullName'] ?? vendor['full_name'] ?? vendor['name'] ?? 'Vendor').toString()
        : 'Vendor #${settlement['vendor_id']?.toString() ?? '—'}';

    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(vendorName, style: const TextStyle(fontWeight: FontWeight.w800)),
                Text(
                  'Settlement #$id  •  GH₵ ${amount.toStringAsFixed(2)}',
                  style: const TextStyle(fontSize: 11, color: AppTheme.textMuted),
                ),
              ],
            ),
          ),
          StatusPill(status),
          const SizedBox(width: 8),
          if ((status == 'PENDING' || status == 'FAILED') && id > 0)
            TextButton(
              onPressed: admin.busy
                  ? null
                  : () async {
                      final ok = await admin.payoutSettlement(id);
                      if (!context.mounted) return;
                      ScaffoldMessenger.of(context).showSnackBar(
                        SnackBar(content: Text(admin.actionMessage ?? (ok ? 'Payout queued.' : 'Payout failed.'))),
                      );
                    },
              child: const Text('Pay'),
            ),
          if (status == 'PROCESSING' && settlement['transfer_code'] != null && id > 0)
            TextButton(
              onPressed: admin.busy ? null : () => _authorizeSettlement(context, admin, id),
              child: const Text('Authorize'),
            ),
        ],
      ),
    );
  }

  Future<void> _authorizeSettlement(
    BuildContext context,
    AdminStateProvider admin,
    int id,
  ) async {
    final otp = TextEditingController();
    final value = await showDialog<String>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: const Text('Authorize Vendor Payout'),
        content: TextField(
          controller: otp,
          keyboardType: TextInputType.number,
          obscureText: true,
          decoration: const InputDecoration(labelText: 'Paystack transfer OTP'),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(dialogContext), child: const Text('Cancel')),
          FilledButton(
            onPressed: () => Navigator.pop(dialogContext, otp.text.trim()),
            child: const Text('Authorize'),
          ),
        ],
      ),
    );
    otp.dispose();

    if (value == null || value.isEmpty || !context.mounted) return;
    final ok = await admin.finalizeSettlementPayout(id, value);
    if (!context.mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(admin.actionMessage ?? (ok ? 'Payout authorization submitted.' : 'Authorization failed.'))),
    );
  }

  Widget _finance(AdminStateProvider admin) {
    final data = admin.financeData;
    double amount(String key) {
      final value = data[key];
      if (value is num) return value.toDouble();
      return double.tryParse(value?.toString() ?? '') ?? 0;
    }

    final totalWallet = amount('total_wallet_balance');
    final deposits = amount('successful_deposits');
    final payments = amount('successful_payments');
    final refunds = amount('successful_refunds');
    final payouts = amount('pending_payouts');

    return RefreshIndicator(
      onRefresh: () => admin.loadAll(includeFinance: true),
      child: ListView(
        padding: const EdgeInsets.all(24),
        children: [
          Row(children: [
            const Expanded(
                child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                  Text('Finance',
                      style: TextStyle(
                          fontSize: 24,
                          fontWeight: FontWeight.w900,
                          color: AppTheme.textDark)),
                  SizedBox(height: 4),
                  Text(
                      'Monitor cafeteria wallet balances, payments, deposits, refunds and pending payouts.')
                ])),
            FilledButton.icon(
                onPressed: () => admin.loadAll(includeFinance: true),
                icon: const Icon(Icons.refresh),
                label: const Text('Refresh')),
          ]),
          if (admin.financeError != null) ...[
            const SizedBox(height: 18),
            ReferenceCard(
              child: Row(
                children: [
                  const Icon(Icons.lock_outline, color: AppTheme.textMuted),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Text(
                      admin.financeError!,
                      style: const TextStyle(color: AppTheme.textMuted),
                    ),
                  ),
                ],
              ),
            ),
          ],
          const SizedBox(height: 18),
          Wrap(spacing: 12, runSpacing: 12, children: [
            MetricTile(
                label: 'Total Wallet Balance',
                value: _money(totalWallet),
                icon: Icons.account_balance_wallet_outlined),
            MetricTile(
                label: 'Successful Deposits',
                value: _money(deposits),
                icon: Icons.add_card),
            MetricTile(
                label: 'Successful Payments',
                value: _money(payments),
                icon: Icons.payments_outlined),
            MetricTile(
                label: 'Successful Refunds',
                value: _money(refunds),
                icon: Icons.currency_exchange),
            MetricTile(
                label: 'Pending Payouts',
                value: _money(payouts),
                icon: Icons.pending_actions),
          ]),
          const SizedBox(height: 20),
          ReferenceCard(
              child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                const Text('Financial overview',
                    style: TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.w900,
                        color: AppTheme.textDark)),
                const SizedBox(height: 14),
                _financeRow('Wallet funds currently held', totalWallet,
                    Icons.account_balance_wallet),
                _financeRow('Completed wallet deposits', deposits,
                    Icons.arrow_downward_rounded),
                _financeRow('Completed customer payments', payments,
                    Icons.arrow_upward_rounded),
                _financeRow('Completed refunds', refunds, Icons.undo_rounded),
                _financeRow(
                    'Payouts awaiting processing', payouts, Icons.schedule),
              ])),
          const SizedBox(height: 14),
          if (admin.settlementsError != null)
            ReferenceCard(
              child: Row(
                children: [
                  const Icon(Icons.lock_outline, color: AppTheme.textMuted),
                  const SizedBox(width: 10),
                  Expanded(child: Text(admin.settlementsError!, style: const TextStyle(color: AppTheme.textMuted))),
                ],
              ),
            )
          else
            ReferenceCard(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    'Vendor Settlements',
                  style: TextStyle(fontSize: 18, fontWeight: FontWeight.w900, color: AppTheme.textDark),
                ),
                const SizedBox(height: 8),
                if (admin.settlements.isEmpty)
                  const Text('No settlement records yet.', style: TextStyle(color: AppTheme.textMuted))
                else
                  ...admin.settlements.take(10).map((settlement) => _settlementRow(context, admin, settlement)),
                ],
              ),
            ),
          const SizedBox(height: 14),
          const ReferenceCard(
              child: Row(children: [
            Icon(Icons.info_outline, color: AppTheme.primary),
            SizedBox(width: 12),
            Expanded(
                child: Text(
                    'These figures are loaded from the Laravel finance summary endpoint and are not placeholder figures.'))
          ])),
        ],
      ),
    );
  }

  Widget _financeRow(String label, double value, IconData icon) => Padding(
      padding: const EdgeInsets.symmetric(vertical: 10),
      child: Row(children: [
        Icon(icon, color: AppTheme.primary, size: 20),
        const SizedBox(width: 12),
        Expanded(
            child: Text(label,
                style: const TextStyle(fontWeight: FontWeight.w600))),
        Text(_money(value),
            style: const TextStyle(
                fontWeight: FontWeight.w900, color: AppTheme.textDark))
      ]));
  String _money(double value) => 'GH₵ ${value.toStringAsFixed(2)}';
  Widget _bar(String label, double value, {String? trailing}) => Padding(
      padding: const EdgeInsets.symmetric(vertical: 6),
      child: Row(children: [
        SizedBox(
            width: 220,
            child: Text(label,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: const TextStyle(fontSize: 11))),
        Expanded(child: LinearProgressIndicator(value: value, minHeight: 12)),
        const SizedBox(width: 8),
        SizedBox(
            width: 78,
            child: Text(
                trailing ?? '${(value * 100).round()}',
                textAlign: TextAlign.end,
                style: const TextStyle(fontWeight: FontWeight.w900))),
      ]));
  Widget _commandCenter(AdminStateProvider admin) {
    final d = admin.commandCenterData;
    String value(String key) => d[key]?.toString() ?? '—';
    return RefreshIndicator(
      onRefresh: () => admin.loadAll(includeFinance: true, includeSettings: true),
      child: ListView(
        padding: const EdgeInsets.all(24),
        children: [
          const Text('Command Center',
              style: TextStyle(
                  fontSize: 24,
                  fontWeight: FontWeight.w900,
                  color: AppTheme.textDark)),
          const SizedBox(height: 6),
          const Text('Live status of cafeteria operations.',
              style: TextStyle(color: AppTheme.textMuted, fontSize: 12)),
          const SizedBox(height: 18),
          Wrap(
            spacing: 12,
            runSpacing: 12,
            children: [
              MetricTile(
                  label: 'Active Orders',
                  value: value('active_orders'),
                  icon: Icons.receipt_long),
              MetricTile(
                  label: 'Completed Today',
                  value: value('completed_orders_today'),
                  icon: Icons.task_alt),
              MetricTile(
                  label: 'Sales Today (GH¢)',
                  value: value('sales_today'),
                  icon: Icons.trending_up),
              MetricTile(
                  label: 'Avg Wait (min)',
                  value: value('average_wait_minutes'),
                  icon: Icons.timer_outlined),
              MetricTile(
                  label: 'Open Security Alerts',
                  value: value('open_security_alerts'),
                  icon: Icons.shield_outlined),
              MetricTile(
                  label: 'Food Waste Rate (%)',
                  value: value('waste_rate_percent'),
                  icon: Icons.recycling),
              MetricTile(
                  label: 'System Status',
                  value: value('system_status'),
                  icon: Icons.health_and_safety),
            ],
          ),
          const SizedBox(height: 20),
          const ReferenceCard(
            child: ListTile(
              leading: Icon(Icons.tips_and_updates_outlined,
                  color: AppTheme.primary),
              title: Text('What is this?',
                  style: TextStyle(fontWeight: FontWeight.w800)),
              subtitle: Text(
                  'The command centre aggregates live queue, waste and security signals across all vendors. Figures are fetched from the Laravel smart-cafeteria endpoints.'),
            ),
          ),
        ],
      ),
    );
  }

  Widget _securityAlerts(AdminStateProvider admin) {
    final alerts = List<Map<String, dynamic>>.from(admin.securityAlerts);
    return RefreshIndicator(
      onRefresh: () => admin.loadAll(includeFinance: true),
      child: ListView(
        padding: const EdgeInsets.all(24),
        children: [
          const Text('Security Alerts',
              style: TextStyle(
                  fontSize: 24,
                  fontWeight: FontWeight.w900,
                  color: AppTheme.textDark)),
          const SizedBox(height: 6),
          const Text('Review and resolve security signals.',
              style: TextStyle(color: AppTheme.textMuted, fontSize: 12)),
          const SizedBox(height: 18),
          if (alerts.isEmpty)
            const ReferenceCard(
              child: Padding(
                padding: EdgeInsets.all(24),
                child: Row(children: [
                  Icon(Icons.shield_outlined, color: AppTheme.primary),
                  SizedBox(width: 12),
                  Expanded(
                      child: Text('No open security alerts. All clear!')),
                ]),
              ),
            )
          else
            ...alerts.map((alert) {
              final severity = (alert['severity']?.toString() ?? 'LOW')
                  .toUpperCase();
              final severityColor = severity == 'HIGH'
                  ? const Color(0xFFD32F2F)
                  : severity == 'MEDIUM'
                      ? const Color(0xFFF57C00)
                      : const Color(0xFF388E3C);
              final user = alert['user'] is Map
                  ? (alert['user']['fullName'] ??
                      alert['user']['username'] ??
                      'Unknown user')
                  : 'Unknown user';
              return Padding(
                padding: const EdgeInsets.only(bottom: 12),
                child: ReferenceCard(
                  child: Padding(
                    padding: const EdgeInsets.all(16),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(children: [
                          Container(
                            padding: const EdgeInsets.symmetric(
                                horizontal: 10, vertical: 4),
                            decoration: BoxDecoration(
                              color: severityColor.withValues(alpha: .12),
                              borderRadius: BorderRadius.circular(999),
                            ),
                            child: Text(severity,
                                style: TextStyle(
                                    color: severityColor,
                                    fontWeight: FontWeight.w900,
                                    fontSize: 11)),
                          ),
                          const SizedBox(width: 10),
                          Expanded(
                            child: Text(
                                alert['type']?.toString() ?? 'SECURITY_ALERT',
                                style: const TextStyle(
                                    fontWeight: FontWeight.w800,
                                    fontSize: 12)),
                          ),
                        ]),
                        const SizedBox(height: 10),
                        Text(alert['message']?.toString() ?? 'Alert message.'),
                        const SizedBox(height: 8),
                        Text('By: $user · ${alert['occurred_at'] ?? ''}',
                            style: const TextStyle(
                                color: AppTheme.textMuted, fontSize: 11)),
                        const SizedBox(height: 12),
                        Align(
                          alignment: Alignment.centerRight,
                          child: OutlinedButton.icon(
                            onPressed: () async {
                              if (admin.busy) return;
                              final ok = await admin.resolveSecurityAlert(
                                  (alert['id'] as num?)?.toInt() ?? 0);
                              if (!mounted) return;
                              showAppMessage(
                                context,
                                message: ok
                                    ? (admin.actionMessage ?? 'Alert resolved.')
                                    : (admin.error ?? 'Could not resolve alert.'),
                                isError: !ok,
                              );
                            },
                            icon: const Icon(Icons.check_circle_outline, size: 18),
                            label: const Text('Resolve'),
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              );
            }),
        ],
      ),
    );
  }

  Widget _settings(AdminStateProvider admin) {
    final settings = List<Map<String, dynamic>>.from(admin.settings);
    return RefreshIndicator(
      onRefresh: () => admin.loadAll(includeFinance: true, includeSettings: true),
      child: ListView(
        padding: const EdgeInsets.all(24),
        children: [
          const Text('System Settings',
              style: TextStyle(
                  fontSize: 24,
                  fontWeight: FontWeight.w900,
                  color: AppTheme.textDark)),
          const SizedBox(height: 6),
          const Text('Cafeteria-wide configuration values.',
              style: TextStyle(color: AppTheme.textMuted, fontSize: 12)),
          const SizedBox(height: 18),
          if (settings.isEmpty)
            const ReferenceCard(
              child: Padding(
                padding: EdgeInsets.all(24),
                child: Row(
                  children: [
                    Icon(Icons.info_outline, color: AppTheme.primary),
                    SizedBox(width: 12),
                    Expanded(
                        child: Text(
                            'No system settings are configured, or your admin level does not have permission to view settings.')),
                  ],
                ),
              ),
            )
          else
            ...settings.map((setting) {
              final key = setting['key']?.toString() ?? 'key';
              final value = setting['value']?.toString() ?? '';
              final description =
                  setting['description']?.toString() ?? '';
              return Padding(
                padding: const EdgeInsets.only(bottom: 10),
                child: ReferenceCard(
                  child: ListTile(
                    leading: const Icon(Icons.tune, color: AppTheme.primary),
                    title: Text(key,
                        style: const TextStyle(fontWeight: FontWeight.w800)),
                    subtitle: Text(
                        description.isEmpty ? value : '$description\n$value'),
                    trailing: Text(value,
                        style: const TextStyle(
                            fontWeight: FontWeight.w900,
                            color: AppTheme.textDark)),
                  ),
                ),
              );
            }),
        ],
      ),
    );
  }
}
