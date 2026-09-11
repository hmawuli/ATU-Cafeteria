import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/cafeteria_provider.dart';

class PasswordResetScreen extends StatefulWidget {
  const PasswordResetScreen({super.key});

  @override
  State<PasswordResetScreen> createState() => _PasswordResetScreenState();
}

class _PasswordResetScreenState extends State<PasswordResetScreen> {
  final TextEditingController _usernameController = TextEditingController();
  final TextEditingController _codeController = TextEditingController();
  final TextEditingController _passwordController = TextEditingController();
  bool _sent = false;
  bool _loading = false;

  @override
  void dispose() {
    _usernameController.dispose();
    _codeController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  Future<void> _requestCode() async {
    final username = _usernameController.text.trim();
    if (username.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Enter your username or student ID.')),
      );
      return;
    }

    setState(() => _loading = true);
    final provider = context.read<CafeteriaProvider>();
    final ok = await provider.requestPasswordReset(username);
    if (!mounted) return;
    setState(() {
      _sent = ok;
      _loading = false;
    });
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(
          ok
              ? 'If the account exists, a reset code has been sent.'
              : (provider.loginError ?? 'Request failed.'),
        ),
      ),
    );
  }

  Future<void> _resetPassword() async {
    final username = _usernameController.text.trim();
    final code = _codeController.text.trim();
    final password = _passwordController.text;

    if (code.length != 6 || password.length < 4) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Enter a valid 6-digit code and a password/PIN of at least 4 characters.')),
      );
      return;
    }

    setState(() => _loading = true);
    final provider = context.read<CafeteriaProvider>();
    final ok = await provider.resetPassword(username, code, password);
    if (!mounted) return;
    setState(() => _loading = false);

    if (ok) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Password changed successfully.')),
      );
      Navigator.pop(context);
    } else {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(provider.loginError ?? 'Reset failed.')),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Reset Password')),
      body: Center(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 460),
            child: Card(
              child: Padding(
                padding: const EdgeInsets.all(24),
                child: Column(
                  children: [
                    const Icon(Icons.lock_reset, size: 60),
                    const SizedBox(height: 12),
                    const Text(
                      'Reset your password',
                      style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold),
                    ),
                    const SizedBox(height: 20),
                    TextField(
                      controller: _usernameController,
                      enabled: !_sent,
                      decoration: const InputDecoration(
                        labelText: 'Username / Student ID',
                        border: OutlineInputBorder(),
                      ),
                    ),
                    const SizedBox(height: 12),
                    if (_sent) ...[
                      TextField(
                        controller: _codeController,
                        keyboardType: TextInputType.number,
                        maxLength: 6,
                        decoration: const InputDecoration(
                          labelText: '6-digit code',
                          border: OutlineInputBorder(),
                        ),
                      ),
                      const SizedBox(height: 12),
                      TextField(
                        controller: _passwordController,
                        obscureText: true,
                        decoration: const InputDecoration(
                          labelText: 'New password / PIN',
                          border: OutlineInputBorder(),
                        ),
                      ),
                      const SizedBox(height: 12),
                      SizedBox(
                        width: double.infinity,
                        height: 48,
                        child: ElevatedButton(
                          onPressed: _loading ? null : _resetPassword,
                          child: Text(_loading ? 'Please wait...' : 'Change Password'),
                        ),
                      ),
                    ] else
                      SizedBox(
                        width: double.infinity,
                        height: 48,
                        child: ElevatedButton(
                          onPressed: _loading ? null : _requestCode,
                          child: Text(_loading ? 'Please wait...' : 'Send Reset Code'),
                        ),
                      ),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
