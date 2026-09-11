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
  Widget _vendors(AdminStateProvider s) => _list(s.vendors, (v) {
        final u = v['user'] ?? {};
        return ListTile(
            title: Text('${v['store_name'] ?? v['name'] ?? 'Vendor'}'),
            subtitle: Text(
                '${u['fullName'] ?? ''} • ${v['operational_status'] ?? 'UNKNOWN'}'),
            trailing: PopupMenuButton<String>(
                onSelected: (x) => s.changeVendorStatus(v['id'], x),
                itemBuilder: (_) => const [
                      PopupMenuItem(
                          value: 'ACTIVE', child: Text('Approve / Activate')),
                      PopupMenuItem(value: 'PENDING', child: Text('Pending')),
                      PopupMenuItem(value: 'SUSPENDED', child: Text('Suspend')),
                      PopupMenuItem(
                          value: 'INACTIVE', child: Text('Deactivate'))
                    ]));
      });
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
