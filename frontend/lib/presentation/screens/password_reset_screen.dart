import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/cafeteria_provider.dart';

class PasswordResetScreen extends StatefulWidget {
  const PasswordResetScreen({super.key});
  @override
  State<PasswordResetScreen> createState() => _PasswordResetScreenState();
}

class _PasswordResetScreenState extends State<PasswordResetScreen> {
  final _emailController = TextEditingController();
  final _codeController = TextEditingController();
  final _passwordController = TextEditingController();
  bool _sent = false;
  bool _loading = false;
  bool _obscure = true;

  @override
  void dispose() {
    _emailController.dispose();
    _codeController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  Future<void> _requestCode() async {
    final email = _emailController.text.trim();
    if (!RegExp(r'^[^@\s]+@[^@\s]+\.[^@\s]+$').hasMatch(email)) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Enter a valid email address.')),
      );
      return;
    }
    setState(() => _loading = true);
    final provider = context.read<CafeteriaProvider>();
    final ok = await provider.requestPasswordReset(email);
    if (!mounted) return;
    setState(() { _sent = ok; _loading = false; });
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(ok
          ? 'If the account exists, a reset code has been sent.'
          : (provider.loginError ?? 'Request failed.'))),
    );
  }

  Future<void> _resetPassword() async {
    final email = _emailController.text.trim();
    final code = _codeController.text.trim();
    final password = _passwordController.text;
    if (!RegExp(r'^\d{6}$').hasMatch(code) || password.length < 8) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Enter a valid 6-digit code and a password of at least 8 characters.')),
      );
      return;
    }
    setState(() => _loading = true);
    final provider = context.read<CafeteriaProvider>();
    final ok = await provider.resetPassword(email, code, password);
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
                padding: const EdgeInsets.all(28),
                child: Column(
                  children: [
                    Container(
                      padding: const EdgeInsets.all(18),
                      decoration: BoxDecoration(
                        color: const Color(0xFFE8751A).withOpacity(.12),
                        shape: BoxShape.circle,
                      ),
                      child: const Icon(Icons.lock_reset_rounded, size: 52, color: Color(0xFFE8751A)),
                    ),
                    const SizedBox(height: 16),
                    const Text('Reset your password', style: TextStyle(fontSize: 24, fontWeight: FontWeight.w800)),
                    const SizedBox(height: 8),
                    const Text('Use the email associated with your ATU Cafeteria account.', textAlign: TextAlign.center),
                    const SizedBox(height: 24),
                    TextField(
                      controller: _emailController,
                      enabled: !_sent,
                      keyboardType: TextInputType.emailAddress,
                      decoration: const InputDecoration(labelText: 'Email address', prefixIcon: Icon(Icons.email_outlined), border: OutlineInputBorder()),
                    ),
                    if (_sent) ...[
                      const SizedBox(height: 14),
                      TextField(
                        controller: _codeController,
                        keyboardType: TextInputType.number,
                        maxLength: 6,
                        decoration: const InputDecoration(labelText: '6-digit reset code', prefixIcon: Icon(Icons.pin_outlined), border: OutlineInputBorder()),
                      ),
                      const SizedBox(height: 2),
                      TextField(
                        controller: _passwordController,
                        obscureText: _obscure,
                        decoration: InputDecoration(
                          labelText: 'New password',
                          prefixIcon: const Icon(Icons.lock_outline),
                          suffixIcon: IconButton(icon: Icon(_obscure ? Icons.visibility : Icons.visibility_off), onPressed: () => setState(() => _obscure = !_obscure)),
                          border: const OutlineInputBorder(),
                        ),
                      ),
                    ],
                    const SizedBox(height: 20),
                    SizedBox(
                      width: double.infinity,
                      height: 50,
                      child: FilledButton(
                        onPressed: _loading ? null : (_sent ? _resetPassword : _requestCode),
                        child: Text(_loading ? 'Please wait...' : (_sent ? 'Change Password' : 'Send Reset Code')),
                      ),
                    ),
                    if (_sent)
                      TextButton(
                        onPressed: _loading ? null : () => setState(() { _sent = false; _codeController.clear(); }),
                        child: const Text('Use a different email'),
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
