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
  static const pages = ['Dashboard', 'Users', 'Vendors', 'Orders'];

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      final cafeteria = context.read<CafeteriaProvider>();
      final admin = context.read<AdminStateProvider>();
      admin.setToken(cafeteria.authToken);
      admin.loadAll();
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
                IconButton(onPressed: admin.loadAll, icon: const Icon(Icons.refresh, color: Colors.white)),
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
            ],
          ),
        ],
      );

  Widget _body(AdminStateProvider admin) {
    if (page == 'Dashboard') return _dashboard(admin);

    final List<Map<String, dynamic>> rows;
    if (page == 'Users') {
      rows = List<Map<String, dynamic>>.from(admin.users);
    } else if (page == 'Vendors') {
      rows = List<Map<String, dynamic>>.from(admin.vendors);
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
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('Welcome, Admin!', style: TextStyle(color: Colors.white, fontSize: 24, fontWeight: FontWeight.w900)),
                    SizedBox(height: 4),
                    Text('Here’s an overview of your cafeteria system.', style: TextStyle(color: Colors.white70)),
                  ],
                ),
              ),
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
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text('Vendor Performance Overview', style: TextStyle(fontSize: 18, fontWeight: FontWeight.w900, color: AppTheme.textDark)),
              const SizedBox(height: 15),
              _bar('Emanuella Adu Dudaa', .92),
              _bar('Lovelace Lartey Adams', .78),
              _bar('KV Bakery', .64),
              _bar('Joll of Rice & Chicken', .58),
              _bar('Donut (Chocolate)', .46),
            ],
          ),
        ),
      ],
    );
  }

  Widget _bar(String label, double value) => Padding(
        padding: const EdgeInsets.symmetric(vertical: 6),
        child: Row(
          children: [
            SizedBox(width: 170, child: Text(label, style: const TextStyle(fontSize: 11))),
            Expanded(child: LinearProgressIndicator(value: value, minHeight: 12)),
            const SizedBox(width: 8),
            Text('${(value * 100).round()}', style: const TextStyle(fontWeight: FontWeight.w900)),
          ],
        ),
      );
}
