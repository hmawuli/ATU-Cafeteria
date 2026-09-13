import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:atu_cafeteria/presentation/providers/admin_state_provider.dart';
import 'package:atu_cafeteria/presentation/providers/cafeteria_provider.dart';
import 'package:atu_cafeteria/presentation/widgets/smart_command_center.dart';

class AdminDashboardScreen extends StatefulWidget {
  const AdminDashboardScreen({super.key});
  @override
  State<AdminDashboardScreen> createState() => _AdminDashboardScreenState();
}

class _AdminDashboardScreenState extends State<AdminDashboardScreen> {
  int index = 0;
  final titles = const [
    'Overview',
    'Users',
    'Vendors',
    'Orders',
    'Finance',
    'Audit',
    'Settings'
  ];

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      final cafe = context.read<CafeteriaProvider>();
      final admin = context.read<AdminStateProvider>();
      admin.setToken(cafe.authToken);
      final level = (cafe.currentUser?.adminLevel ?? '').toUpperCase();
      admin.loadAll(
          includeFinance: level == 'SUPER_ADMIN' || level == 'FINANCE_ADMIN',
          includeSettings: level == 'SUPER_ADMIN');
    });
  }

  @override
  Widget build(BuildContext context) {
    final state = context.watch<AdminStateProvider>();
    final cafe = context.read<CafeteriaProvider>();
    return Scaffold(
      appBar: AppBar(title: Text('ATU Cafeteria • ${titles[index]}'), actions: [
        IconButton(
            onPressed: () => Navigator.pushNamed(context, '/smart-insights'),
            icon: const Icon(Icons.insights)),
        IconButton(
            onPressed: () => Navigator.pushNamed(context, '/admin-security'),
            icon: const Icon(Icons.security)),
        IconButton(
            onPressed: () {
              final level = (cafe.currentUser?.adminLevel ?? '').toUpperCase();
              state.loadAll(
                  includeFinance:
                      level == 'SUPER_ADMIN' || level == 'FINANCE_ADMIN',
                  includeSettings: level == 'SUPER_ADMIN');
            },
            icon: const Icon(Icons.refresh)),
        IconButton(
            onPressed: () {
              cafe.logOut();
              Navigator.pushReplacementNamed(context, '/login');
            },
            icon: const Icon(Icons.logout))
      ]),
      body: state.loading && state.dashboardData.isEmpty
          ? const Center(child: CircularProgressIndicator())
          : state.error != null && state.dashboardData.isEmpty
              ? _error(state.error!, state.loadAll)
              : IndexedStack(index: index, children: [
                  _overview(state),
                  _users(state),
                  _vendors(state),
                  _orders(state),
                  _finance(state),
                  _audit(state),
                  _settings(state)
                ]),
      bottomNavigationBar: NavigationBar(
          selectedIndex: index,
          onDestinationSelected: (i) => setState(() => index = i),
          destinations: const [
            NavigationDestination(
                icon: Icon(Icons.dashboard), label: 'Overview'),
            NavigationDestination(icon: Icon(Icons.people), label: 'Users'),
            NavigationDestination(icon: Icon(Icons.store), label: 'Vendors'),
            NavigationDestination(
                icon: Icon(Icons.receipt_long), label: 'Orders'),
            NavigationDestination(
                icon: Icon(Icons.account_balance_wallet), label: 'Finance'),
            NavigationDestination(icon: Icon(Icons.fact_check), label: 'Audit'),
            NavigationDestination(icon: Icon(Icons.settings), label: 'Settings')
          ]),
    );
  }

  Widget _overview(AdminStateProvider s) {
    final d = s.dashboardData;
    final c = s.commandCenterData;
    return RefreshIndicator(
        onRefresh: s.loadAll,
        child: ListView(padding: const EdgeInsets.all(16), children: [
          Text('ATU Cafeteria Command Center',
              style: Theme.of(context)
                  .textTheme
                  .headlineSmall
                  ?.copyWith(fontWeight: FontWeight.bold)),
          const SizedBox(height: 4),
          const Text('Live operational, security and service indicators.'),
          const SizedBox(height: 16),
          Wrap(spacing: 12, runSpacing: 12, children: [
            SmartMetricCard(
                title: 'Active Orders',
                value: '${c['active_orders'] ?? d['pending_orders'] ?? 0}',
                icon: Icons.queue),
            SmartMetricCard(
                title: 'Sales Today',
                value: 'GHS ${(c['sales_today'] ?? d['sales_today'] ?? 0)}',
                icon: Icons.payments),
            SmartMetricCard(
                title: 'Average Wait',
                value: '${c['average_wait_minutes'] ?? 0} min',
                icon: Icons.timer),
            SmartMetricCard(
                title: 'Waste Rate',
                value: '${c['waste_rate_percent'] ?? 0}%',
                icon: Icons.eco),
            SmartMetricCard(
                title: 'Security Alerts',
                value:
                    '${c['open_security_alerts'] ?? s.securityAlerts.length}',
                icon: Icons.security),
            SmartMetricCard(
                title: 'System',
                value: '${c['system_status'] ?? 'HEALTHY'}',
                icon: Icons.monitor_heart),
          ]),
          const SizedBox(height: 16),
          Wrap(spacing: 12, runSpacing: 12, children: [
            _metric('Students', d['students']),
            _metric('Vendors', d['vendors']),
            _metric('Orders Today', d['orders_today']),
            _metric('Completed Orders', d['completed_orders']),
            _metric('Active Users', d['active_users'])
          ]),
          const SizedBox(height: 20),
          Card(
              child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text('Security & governance',
                            style: Theme.of(context)
                                .textTheme
                                .titleMedium
                                ?.copyWith(fontWeight: FontWeight.bold)),
                        const SizedBox(height: 8),
                        Text(
                            '${s.securityAlerts.length} unresolved security alerts. Administrative actions are audited and enforced server-side.')
                      ]))),
        ]));
  }

  Widget _metric(String label, dynamic value) => SizedBox(
      width: MediaQuery.sizeOf(context).width > 700 ? 210 : 160,
      child: Card(
          child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(label),
                    const SizedBox(height: 8),
                    Text('${value ?? 0}',
                        style: Theme.of(context)
                            .textTheme
                            .headlineSmall
                            ?.copyWith(fontWeight: FontWeight.bold))
                  ]))));
  Widget _users(AdminStateProvider s) => _list(
        s.users,
        (u) => ListTile(
          title: Text('${u['fullName'] ?? 'User'}'),
          subtitle: Text(
            '${u['username'] ?? ''} • ${u['role'] ?? ''} • ${u['account_status'] ?? 'ACTIVE'}'
            '${u['admin_level'] != null ? ' • ${u['admin_level']}' : ''}',
          ),
          trailing: PopupMenuButton<String>(
            onSelected: (v) {
              if (v.startsWith('LEVEL:')) {
                s.changeAdminLevel(u['id'], v.substring(6));
              } else {
                s.changeUserStatus(u['id'], v);
              }
            },
            itemBuilder: (_) => [
              const PopupMenuItem(value: 'ACTIVE', child: Text('Activate')),
              const PopupMenuItem(value: 'SUSPENDED', child: Text('Suspend')),
              const PopupMenuItem(value: 'DISABLED', child: Text('Disable')),
              if ((u['role'] ?? '').toString().toUpperCase() == 'ADMIN') ...[
                const PopupMenuDivider(),
                const PopupMenuItem(
                    value: 'LEVEL:SUPER_ADMIN', child: Text('Set Super Admin')),
                const PopupMenuItem(
                    value: 'LEVEL:CAFETERIA_ADMIN',
                    child: Text('Set Cafeteria Admin')),
                const PopupMenuItem(
                    value: 'LEVEL:FINANCE_ADMIN',
                    child: Text('Set Finance Admin')),
              ],
            ],
          ),
        ),
      );
  Widget _vendors(AdminStateProvider s) {
    return Stack(
      children: [
        _list(s.vendors, (v) {
          final u = v['user'] ?? {};
          return ListTile(
            leading: const CircleAvatar(child: Icon(Icons.storefront_rounded)),
            title: Text('${v['store_name'] ?? v['name'] ?? 'Vendor'}'),
            subtitle: Text(
                '${u['fullName'] ?? ''} • ${v['operational_status'] ?? 'UNKNOWN'}'),
            trailing: PopupMenuButton<String>(
              onSelected: (x) => s.changeVendorStatus(v['id'], x),
              itemBuilder: (_) => const [
                PopupMenuItem(
                    value: 'ACTIVE', child: Text('Approve / Activate')),
                PopupMenuItem(value: 'PENDING', child: Text('Set Pending')),
                PopupMenuItem(value: 'SUSPENDED', child: Text('Suspend')),
                PopupMenuItem(value: 'INACTIVE', child: Text('Deactivate')),
              ],
            ),
          );
        }),
        Positioned(
          right: 20,
          bottom: 20,
          child: FloatingActionButton.extended(
            onPressed: () => _showCreateVendorDialog(context, s),
            icon: const Icon(Icons.person_add_alt_1_rounded),
            label: const Text('Add Vendor'),
          ),
        ),
      ],
    );
  }

  Future<void> _showCreateVendorDialog(
      BuildContext context, AdminStateProvider state) async {
    final formKey = GlobalKey<FormState>();
    final name = TextEditingController();
    final email = TextEditingController();
    final password = TextEditingController();
    final store = TextEditingController();
    final location = TextEditingController();
    final contact = TextEditingController();
    try {
      await showDialog<void>(
        context: context,
        builder: (dialogContext) => AlertDialog(
          title: const Text('Create Vendor Account'),
          content: SizedBox(
            width: 480,
            child: Form(
              key: formKey,
              child: SingleChildScrollView(
                child: Column(mainAxisSize: MainAxisSize.min, children: [
                  const Text(
                      'Vendor accounts are provisioned by an administrator. The vendor will sign in with the email and password provided here.'),
                  const SizedBox(height: 16),
                  TextFormField(
                      controller: name,
                      decoration: const InputDecoration(
                          labelText: 'Vendor full name',
                          prefixIcon: Icon(Icons.person_outline)),
                      validator: (v) => v == null || v.trim().isEmpty
                          ? 'Enter the vendor name.'
                          : null),
                  const SizedBox(height: 12),
                  TextFormField(
                      controller: email,
                      keyboardType: TextInputType.emailAddress,
                      decoration: const InputDecoration(
                          labelText: 'Email address',
                          prefixIcon: Icon(Icons.email_outlined)),
                      validator: (v) {
                        final value = v?.trim() ?? '';
                        if (value.isEmpty) return 'Enter an email address.';
                        if (!RegExp(r'^[^@\s]+@[^@\s]+\.[^@\s]+$')
                            .hasMatch(value)) {
                          return 'Enter a valid email address.';
                        }
                        return null;
                      }),
                  const SizedBox(height: 12),
                  TextFormField(
                      controller: password,
                      obscureText: true,
                      decoration: const InputDecoration(
                          labelText: 'Temporary password',
                          prefixIcon: Icon(Icons.lock_outline)),
                      validator: (v) => v == null || v.length < 8
                          ? 'Use at least 8 characters.'
                          : null),
                  const SizedBox(height: 12),
                  TextFormField(
                      controller: store,
                      decoration: const InputDecoration(
                          labelText: 'Store / Booth name',
                          prefixIcon: Icon(Icons.store_outlined)),
                      validator: (v) => v == null || v.trim().isEmpty
                          ? 'Enter the store or booth name.'
                          : null),
                  const SizedBox(height: 12),
                  TextFormField(
                      controller: location,
                      decoration: const InputDecoration(
                          labelText: 'Location (optional)',
                          prefixIcon: Icon(Icons.location_on_outlined))),
                  const SizedBox(height: 12),
                  TextFormField(
                      controller: contact,
                      decoration: const InputDecoration(
                          labelText: 'Contact information (optional)',
                          prefixIcon: Icon(Icons.phone_outlined))),
                ]),
              ),
            ),
          ),
          actions: [
            TextButton(
                onPressed: () => Navigator.pop(dialogContext),
                child: const Text('Cancel')),
            FilledButton.icon(
                icon: const Icon(Icons.check_circle_outline),
                label: const Text('Create Vendor'),
                onPressed: () async {
                  if (!formKey.currentState!.validate()) return;
                  Navigator.pop(dialogContext);
                  final ok = await state.createVendor(
                      email: email.text.trim(),
                      password: password.text,
                      fullName: name.text.trim(),
                      storeName: store.text.trim(),
                      location: location.text.trim(),
                      contactEmail: email.text.trim(),
                      contactInfo: contact.text.trim());
                  if (!context.mounted) return;
                  ScaffoldMessenger.of(context).showSnackBar(SnackBar(
                      content: Text(ok
                          ? 'Vendor account created successfully.'
                          : (state.error ??
                              'Unable to create vendor account.'))));
                }),
          ],
        ),
      );
    } finally {
      name.dispose();
      email.dispose();
      password.dispose();
      store.dispose();
      location.dispose();
      contact.dispose();
    }
  }

  Widget _orders(AdminStateProvider s) => _list(
      s.orders,
      (o) => ListTile(
          title: Text('Order #${o['id']} • ${o['status'] ?? ''}'),
          subtitle: Text(
              '${o['user']?['fullName'] ?? 'Student'} → ${o['vendor']?['fullName'] ?? 'Vendor'}'),
          trailing: Text('GHS ${o['total_price'] ?? 0}')));
  Widget _finance(AdminStateProvider s) => ListView(
      padding: const EdgeInsets.all(16),
      children: s.financeData.entries
          .map((e) => Card(
              child: ListTile(
                  title: Text(_pretty(e.key)),
                  trailing: Text('GHS ${e.value}'))))
          .toList());
  Widget _audit(AdminStateProvider s) => _list(
      s.auditLogs,
      (a) => ListTile(
          title: Text(a['action'] ?? 'Action'),
          subtitle: Text(
              '${a['details'] ?? ''}\n${a['user']?['fullName'] ?? 'System'}')));
  Widget _settings(AdminStateProvider s) => _list(
      s.settings,
      (x) => ListTile(
          title: Text(x['key'] ?? ''),
          subtitle: Text(x['description'] ?? ''),
          trailing: Text(x['value'] ?? '')));
  Widget _list(List<dynamic> items, Widget Function(dynamic) builder) =>
      RefreshIndicator(
          onRefresh: context.read<AdminStateProvider>().loadAll,
          child: ListView.separated(
              padding: const EdgeInsets.all(12),
              itemCount: items.length,
              separatorBuilder: (_, __) => const Divider(height: 1),
              itemBuilder: (_, i) => Card(child: builder(items[i]))));
  Widget _error(String e, Future<void> Function() retry) => Center(
          child: Column(mainAxisSize: MainAxisSize.min, children: [
        const Icon(Icons.cloud_off, size: 48),
        const SizedBox(height: 12),
        Text(e, textAlign: TextAlign.center),
        const SizedBox(height: 12),
        ElevatedButton(onPressed: retry, child: const Text('Retry'))
      ]));
  String _pretty(String s) =>
      s.replaceAll('_', ' ').replaceFirst(s[0], s[0].toUpperCase());
}
