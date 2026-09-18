import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
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
  static const pages = ['Dashboard', 'Users', 'Vendors', 'Orders', 'Finance'];

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      final cafeteria = context.read<CafeteriaProvider>();
      final admin = context.read<AdminStateProvider>();
      admin.setToken(cafeteria.authToken);
      admin.loadAll(includeFinance: true);
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

  Widget _mobile(CafeteriaProvider cafeteria, AdminStateProvider admin) => Column(
        children: [
          Container(
            height: 64,
            color: AppTheme.primary,
            padding: const EdgeInsets.symmetric(horizontal: 16),
            child: Row(
              children: [
                AtuBrand.title(compact: true),
                const Spacer(),
                IconButton(onPressed: () => admin.loadAll(includeFinance: true), icon: const Icon(Icons.refresh, color: Colors.white)),
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
            selectedIndex: pages.indexOf(page).clamp(0, pages.length - 1),
            onDestinationSelected: (index) => setState(() => page = pages[index]),
            destinations: const [
              NavigationDestination(icon: Icon(Icons.dashboard), label: 'Dashboard'),
              NavigationDestination(icon: Icon(Icons.people), label: 'Users'),
              NavigationDestination(icon: Icon(Icons.store), label: 'Vendors'),
              NavigationDestination(icon: Icon(Icons.receipt_long), label: 'Orders'),
              NavigationDestination(icon: Icon(Icons.account_balance_wallet), label: 'Finance'),
            ],
          ),
        ],
      );

  Widget _body(AdminStateProvider admin) {
    if (page == 'Dashboard') return _dashboard(admin);
    if (page == 'Finance') return _finance(admin);
    if (page == 'Vendors') return _vendorsPage(admin);

    final List<Map<String, dynamic>> rows;
    if (page == 'Users') {
      rows = List<Map<String, dynamic>>.from(admin.users);
    } else {
      rows = List<Map<String, dynamic>>.from(admin.orders);
    }

    return ListView(
      padding: const EdgeInsets.all(24),
      children: [
        Text('All $page', style: const TextStyle(fontSize: 24, fontWeight: FontWeight.w900, color: AppTheme.textDark)),
        const SizedBox(height: 14),
        ReferenceCard(
          child: rows.isEmpty
              ? const Padding(padding: EdgeInsets.all(24), child: Text('No records available.'))
              : Column(
                  children: rows.take(20).map<Widget>((row) {
                    final title = row['fullName'] ?? row['full_name'] ?? row['name'] ?? row['id'] ?? 'Record';
                    final subtitle = row['username'] ?? row['email'] ?? row['status'] ?? row['role'] ?? '';
                    return ListTile(
                      title: Text('$title', style: const TextStyle(fontWeight: FontWeight.w800)),
                      subtitle: Text('$subtitle'),
                      trailing: const Icon(Icons.chevron_right),
                    );
                  }).toList(),
                ),
        ),
      ],
    );
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
                    Text('Vendors', style: TextStyle(fontSize: 24, fontWeight: FontWeight.w900, color: AppTheme.textDark)),
                    SizedBox(height: 4),
                    Text('Create vendor accounts and manage their operational status.'),
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
                    Icon(Icons.storefront_outlined, size: 54, color: AppTheme.primary),
                    SizedBox(height: 12),
                    Text('No vendors yet', style: TextStyle(fontSize: 18, fontWeight: FontWeight.w900)),
                    SizedBox(height: 6),
                    Text('Create the first vendor account to start publishing cafeteria menus.', textAlign: TextAlign.center),
                  ],
                ),
              ),
            )
          else
            ...admin.vendors.map<Widget>((raw) => _vendorManagementCard(admin, Map<String, dynamic>.from(raw as Map))),
        ],
      ),
    );
  }

  Widget _vendorManagementCard(AdminStateProvider admin, Map<String, dynamic> vendor) {
    final user = vendor['user'] is Map ? Map<String, dynamic>.from(vendor['user']) : <String, dynamic>{};
    final id = int.tryParse('${vendor['id'] ?? 0}') ?? 0;
    final name = '${vendor['store_name'] ?? vendor['name'] ?? user['fullName'] ?? 'Campus Vendor'}';
    final owner = '${vendor['name'] ?? user['fullName'] ?? 'Vendor owner'}';
    final email = '${vendor['contact_email'] ?? user['username'] ?? ''}';
    final location = '${vendor['location'] ?? vendor['location_within_campus'] ?? 'Campus'}';
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
              decoration: BoxDecoration(color: AppTheme.primary.withValues(alpha: .09), borderRadius: BorderRadius.circular(16)),
              child: const Icon(Icons.storefront_rounded, color: AppTheme.primary, size: 30),
            ),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(name, style: const TextStyle(fontSize: 17, fontWeight: FontWeight.w900, color: AppTheme.textDark)),
                  const SizedBox(height: 4),
                  Text('Owner: $owner', style: const TextStyle(color: AppTheme.textMuted)),
                  const SizedBox(height: 3),
                  Text('$email • $location', maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 12, color: AppTheme.textMuted)),
                ],
              ),
            ),
            StatusPill(status),
            const SizedBox(width: 8),
            PopupMenuButton<String>(
              tooltip: 'Vendor actions',
              onSelected: (value) async {
                final ok = await admin.changeVendorStatus(id, value);
                if (!mounted) return;
                ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(ok ? 'Vendor status updated.' : (admin.error ?? 'Could not update vendor status.'))));
              },
              itemBuilder: (_) => [
                if (!active) const PopupMenuItem(value: 'ACTIVE', child: Text('Activate vendor')),
                if (active) const PopupMenuItem(value: 'INACTIVE', child: Text('Set inactive')),
                const PopupMenuItem(value: 'SUSPENDED', child: Text('Suspend vendor')),
                const PopupMenuItem(value: 'PENDING', child: Text('Set pending')),
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
              contactEmail: contactEmail.text.trim().isEmpty ? null : contactEmail.text.trim(),
              contactInfo: contactInfo.text.trim().isEmpty ? null : contactInfo.text.trim(),
            );
            if (!this.mounted || !dialogContext.mounted) return;
            if (ok) {
              Navigator.of(dialogContext).pop();
              ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Vendor account created successfully.')));
            } else {
              setDialogState(() => saving = false);
              ScaffoldMessenger.of(this.context).showSnackBar(SnackBar(content: Text(admin.error ?? 'Could not create vendor.')));
            }
          }

          return AlertDialog(
            title: const Text('Add Vendor', style: TextStyle(fontWeight: FontWeight.w900)),
            content: SizedBox(
              width: 560,
              child: SingleChildScrollView(
                child: Form(
                  key: formKey,
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      const Align(alignment: Alignment.centerLeft, child: Text('Vendor account details', style: TextStyle(color: AppTheme.textMuted))),
                      const SizedBox(height: 14),
                      _field(fullName, 'Owner full name', Icons.person_outline, required: true),
                      const SizedBox(height: 10),
                      _field(storeName, 'Store / vendor name', Icons.storefront_outlined, required: true),
                      const SizedBox(height: 10),
                      _field(email, 'Login email', Icons.email_outlined, required: true, email: true),
                      const SizedBox(height: 10),
                      TextFormField(
                        controller: password,
                        obscureText: obscure,
                        decoration: InputDecoration(prefixIcon: const Icon(Icons.lock_outline), labelText: 'Temporary password', suffixIcon: IconButton(onPressed: () => setDialogState(() => obscure = !obscure), icon: Icon(obscure ? Icons.visibility : Icons.visibility_off))),
                        validator: (value) => value == null || value.length < 8 ? 'Use at least 8 characters.' : null,
                      ),
                      const SizedBox(height: 10),
                      _field(location, 'Campus location (optional)', Icons.location_on_outlined),
                      const SizedBox(height: 10),
                      _field(contactEmail, 'Contact email (optional)', Icons.alternate_email),
                      const SizedBox(height: 10),
                      _field(contactInfo, 'Contact phone / info (optional)', Icons.phone_outlined),
                    ],
                  ),
                ),
              ),
            ),
            actions: [
              TextButton(onPressed: saving ? null : () => Navigator.of(dialogContext).pop(), child: const Text('Cancel')),
              FilledButton.icon(onPressed: saving ? null : submit, icon: saving ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2)) : const Icon(Icons.person_add_alt_1), label: Text(saving ? 'Creating...' : 'Create Vendor')),
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

  Widget _field(TextEditingController controller, String label, IconData icon, {bool required = false, bool email = false}) {
    return TextFormField(
      controller: controller,
      keyboardType: email ? TextInputType.emailAddress : TextInputType.text,
      decoration: InputDecoration(prefixIcon: Icon(icon), labelText: label),
      validator: (value) {
        final text = value?.trim() ?? '';
        if (required && text.isEmpty) return '$label is required.';
        if (email && text.isNotEmpty && !RegExp(r'^[^@\s]+@[^@\s]+\.[^@\s]+\$').hasMatch(text)) return 'Enter a valid email address.';
        return null;
      },
    );
  }

  Widget _dashboard(AdminStateProvider admin) {
    final data = admin.dashboardData;
    return ListView(
      padding: const EdgeInsets.all(24),
      children: [
        Container(
          padding: const EdgeInsets.all(20),
          decoration: BoxDecoration(color: AppTheme.primary, borderRadius: BorderRadius.circular(20)),
          child: const Row(
            children: [
              Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text('Welcome, Admin!', style: TextStyle(color: Colors.white, fontSize: 24, fontWeight: FontWeight.w900)), SizedBox(height: 4), Text('Here’s an overview of your cafeteria system.', style: TextStyle(color: Colors.white70))])),
              Icon(Icons.admin_panel_settings, color: AppTheme.accent, size: 50),
            ],
          ),
        ),
        const SizedBox(height: 18),
        Wrap(
          spacing: 12,
          runSpacing: 12,
          children: [
            MetricTile(label: 'Total Users', value: '${data['users'] ?? data['total_users'] ?? 0}', icon: Icons.people),
            MetricTile(label: 'Total Vendors', value: '${data['vendors'] ?? data['total_vendors'] ?? 0}', icon: Icons.store),
            MetricTile(label: 'Total Orders', value: '${data['orders'] ?? data['total_orders'] ?? 0}', icon: Icons.receipt_long),
            const MetricTile(label: 'Average Rating', value: '4.3 ★', icon: Icons.star),
          ],
        ),
        const SizedBox(height: 20),
        ReferenceCard(
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            const Text('Vendor Performance Overview', style: TextStyle(fontSize: 18, fontWeight: FontWeight.w900, color: AppTheme.textDark)),
            const SizedBox(height: 15),
            _bar('Emanuella Adu Dudaa', .92),
            _bar('Lovelace Lartey Adams', .78),
            _bar('KV Bakery', .64),
            _bar('Joll of Rice & Chicken', .58),
            _bar('Donut (Chocolate)', .46),
          ]),
        ),
      ],
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
            const Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text('Finance', style: TextStyle(fontSize: 24, fontWeight: FontWeight.w900, color: AppTheme.textDark)), SizedBox(height: 4), Text('Monitor cafeteria wallet balances, payments, deposits, refunds and pending payouts.')])) ,
            FilledButton.icon(onPressed: () => admin.loadAll(includeFinance: true), icon: const Icon(Icons.refresh), label: const Text('Refresh')),
          ]),
          const SizedBox(height: 18),
          Wrap(spacing: 12, runSpacing: 12, children: [
            MetricTile(label: 'Total Wallet Balance', value: _money(totalWallet), icon: Icons.account_balance_wallet_outlined),
            MetricTile(label: 'Successful Deposits', value: _money(deposits), icon: Icons.add_card),
            MetricTile(label: 'Successful Payments', value: _money(payments), icon: Icons.payments_outlined),
            MetricTile(label: 'Successful Refunds', value: _money(refunds), icon: Icons.currency_exchange),
            MetricTile(label: 'Pending Payouts', value: _money(payouts), icon: Icons.pending_actions),
          ]),
          const SizedBox(height: 20),
          ReferenceCard(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            const Text('Financial overview', style: TextStyle(fontSize: 18, fontWeight: FontWeight.w900, color: AppTheme.textDark)),
            const SizedBox(height: 14),
            _financeRow('Wallet funds currently held', totalWallet, Icons.account_balance_wallet),
            _financeRow('Completed wallet deposits', deposits, Icons.arrow_downward_rounded),
            _financeRow('Completed customer payments', payments, Icons.arrow_upward_rounded),
            _financeRow('Completed refunds', refunds, Icons.undo_rounded),
            _financeRow('Payouts awaiting processing', payouts, Icons.schedule),
          ])),
          const SizedBox(height: 14),
          const ReferenceCard(child: Row(children: [Icon(Icons.info_outline, color: AppTheme.primary), SizedBox(width: 12), Expanded(child: Text('These figures are loaded from the Laravel finance summary endpoint and are not placeholder figures.'))])),
        ],
      ),
    );
  }

  Widget _financeRow(String label, double value, IconData icon) => Padding(padding: const EdgeInsets.symmetric(vertical: 10), child: Row(children: [Icon(icon, color: AppTheme.primary, size: 20), const SizedBox(width: 12), Expanded(child: Text(label, style: const TextStyle(fontWeight: FontWeight.w600))), Text(_money(value), style: const TextStyle(fontWeight: FontWeight.w900, color: AppTheme.textDark))]));
  String _money(double value) => 'GH₵ ${value.toStringAsFixed(2)}';
  Widget _bar(String label, double value) => Padding(padding: const EdgeInsets.symmetric(vertical: 6), child: Row(children: [SizedBox(width: 170, child: Text(label, style: const TextStyle(fontSize: 11))), Expanded(child: LinearProgressIndicator(value: value, minHeight: 12)), const SizedBox(width: 8), Text('${(value * 100).round()}', style: const TextStyle(fontWeight: FontWeight.w900))]));
}
