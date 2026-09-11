import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:atu_cafeteria/core/network/api_client.dart';
import '../providers/cafeteria_provider.dart';

class AdminSecurityScreen extends StatefulWidget {
  const AdminSecurityScreen({super.key});

  @override
  State<AdminSecurityScreen> createState() => _AdminSecurityScreenState();
}

class _AdminSecurityScreenState extends State<AdminSecurityScreen> {
  final TextEditingController _codeController = TextEditingController();
  bool _requested = false;
  bool _loading = false;

  @override
  void dispose() {
    _codeController.dispose();
    super.dispose();
  }

  Future<Map<String, dynamic>> _call(
    String method,
    String path,
    Map<String, dynamic> body,
  ) async {
    final provider = context.read<CafeteriaProvider>();
    final client = ApiClient()..token = provider.authToken;
    try {
      final result = await client.request(method, path, body: body);
      if (result is Map<String, dynamic>) return result;
      if (result is Map) return Map<String, dynamic>.from(result);
      return <String, dynamic>{'data': result};
    } finally {
      client.close();
    }
  }

  Future<void> _requestCode() async {
    setState(() => _loading = true);
    try {
      final result = await _call('POST', 'admin/security/2fa/request-enable', {});
      if (!mounted) return;
      setState(() => _requested = true);
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(result['message']?.toString() ?? 'Code sent.')),
      );
    } catch (error) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(error.toString())),
        );
      }
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _enableTwoFactor() async {
    final code = _codeController.text.trim();
    if (code.length != 6) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Enter the 6-digit verification code.')),
      );
      return;
    }

    setState(() => _loading = true);
    try {
      final result = await _call(
        'POST',
        'admin/security/2fa/enable',
        {'code': code},
      );
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(result['message']?.toString() ?? 'Two-factor enabled.')),
      );
      Navigator.pop(context);
    } catch (error) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(error.toString())),
        );
      }
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _showSecurityActivity() async {
    setState(() => _loading = true);
    try {
      final rows = await context.read<CafeteriaProvider>().securityActivity();
      if (!mounted) return;
      await showModalBottomSheet<void>(
        context: context,
        builder: (sheetContext) => SafeArea(
          child: ListView(
            padding: const EdgeInsets.all(16),
            children: [
              const Text(
                'Security activity',
                style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 12),
              if (rows.isEmpty)
                const ListTile(title: Text('No security events found.'))
              else
                ...rows.map(
                  (row) => ListTile(
                    leading: const Icon(Icons.shield_outlined),
                    title: Text(row['action']?.toString() ?? 'Security event'),
                    subtitle: Text(row['details']?.toString() ?? ''),
                  ),
                ),
            ],
          ),
        ),
      );
    } catch (error) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(error.toString())),
        );
      }
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final user = context.watch<CafeteriaProvider>().currentUser;
    final enabled = user?.twoFactorEnabled == true;

    return Scaffold(
      appBar: AppBar(title: const Text('Admin Security')),
      body: ListView(
        padding: const EdgeInsets.all(20),
        children: [
          Card(
            child: ListTile(
              leading: const Icon(Icons.verified_user),
              title: const Text('Two-factor authentication'),
              subtitle: Text(enabled ? 'Enabled' : 'Not enabled'),
              trailing: enabled ? const Icon(Icons.check_circle) : null,
            ),
          ),
          const SizedBox(height: 16),
          if (!enabled) ...[
            const Text(
              'Protect administrator accounts with a one-time verification code sent to the configured administrator email address.',
            ),
            const SizedBox(height: 16),
            if (_requested)
              TextField(
                controller: _codeController,
                keyboardType: TextInputType.number,
                maxLength: 6,
                decoration: const InputDecoration(
                  labelText: '6-digit verification code',
                  border: OutlineInputBorder(),
                ),
              ),
            const SizedBox(height: 8),
            SizedBox(
              height: 48,
              child: ElevatedButton(
                onPressed: _loading ? null : (_requested ? _enableTwoFactor : _requestCode),
                child: Text(
                  _loading
                      ? 'Please wait...'
                      : (_requested ? 'Enable 2FA' : 'Send verification code'),
                ),
              ),
            ),
          ] else ...[
            const Text('Two-factor authentication is protecting this administrator account.'),
            const SizedBox(height: 24),
            SizedBox(
              height: 48,
              child: OutlinedButton.icon(
                onPressed: _loading ? null : _showSecurityActivity,
                icon: const Icon(Icons.history),
                label: const Text('View security activity'),
              ),
            ),
          ],
        ],
      ),
    );
  }
}
