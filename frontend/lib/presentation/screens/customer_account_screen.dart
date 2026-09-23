import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../core/network/api_client.dart';
import '../providers/cafeteria_provider.dart';

class CustomerAccountScreen extends StatelessWidget {
  const CustomerAccountScreen({super.key});

  Future<void> _close(BuildContext context) async {
    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Close customer account?'),
        content: const Text('Your account will be closed and active sessions revoked. Order and financial history may be retained for audit and reconciliation.'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Keep account')),
          FilledButton(
            onPressed: () => Navigator.pop(ctx, true),
            style: FilledButton.styleFrom(backgroundColor: Colors.redAccent),
            child: const Text('Close account'),
          ),
        ],
      ),
    );
    if (ok != true || !context.mounted) return;
    final api = ApiClient();
    try {
      await api.delete('/customer/account', idempotencyKey: ApiClient.newIdempotencyKey());
      if (!context.mounted) return;
      context.read<CafeteriaProvider>().logOut();
      Navigator.pushNamedAndRemoveUntil(context, '/login', (_) => false);
    } catch (e) {
      if (context.mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.toString())));
    } finally {
      api.close();
    }
  }

  @override
  Widget build(BuildContext context) {
    final user = context.watch<CafeteriaProvider>().currentUser;
    return Scaffold(
      appBar: AppBar(title: const Text('Account & privacy')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Card(
            child: ListTile(
              leading: const CircleAvatar(child: Icon(Icons.person_outline)),
              title: Text(user?.fullName ?? 'Customer', style: const TextStyle(fontWeight: FontWeight.w900)),
              subtitle: Text(user?.email ?? user?.username ?? ''),
            ),
          ),
          const SizedBox(height: 12),
          _item(context, Icons.location_on_outlined, 'Saved addresses', 'Manage saved customer addresses', '/customer-addresses'),
          _item(context, Icons.devices_outlined, 'Devices & sessions', 'Review and revoke registered devices', '/customer-devices'),
          _item(context, Icons.support_agent_outlined, 'Support', 'Create and track support requests', '/customer-support'),
          const SizedBox(height: 24),
          OutlinedButton.icon(
            onPressed: () => _close(context),
            icon: const Icon(Icons.delete_outline),
            label: const Text('Close customer account'),
            style: OutlinedButton.styleFrom(foregroundColor: Colors.redAccent),
          ),
        ],
      ),
    );
  }

  Widget _item(BuildContext context, IconData icon, String title, String subtitle, String route) {
    return Card(
      child: ListTile(
        leading: Icon(icon),
        title: Text(title, style: const TextStyle(fontWeight: FontWeight.w800)),
        subtitle: Text(subtitle),
        trailing: const Icon(Icons.chevron_right_rounded),
        onTap: () => Navigator.pushNamed(context, route),
      ),
    );
  }
}
