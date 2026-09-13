import 'package:flutter/material.dart';
import '../../core/network/api_client.dart';

class RegisterScreen extends StatefulWidget {
  const RegisterScreen({super.key});

  @override
  State<RegisterScreen> createState() => _RegisterScreenState();
}

class _RegisterScreenState extends State<RegisterScreen> {
  final _formKey = GlobalKey<FormState>();
  final _name = TextEditingController();
  final _email = TextEditingController();
  final _password = TextEditingController();
  final _confirm = TextEditingController();
  final _programme = TextEditingController();
  final _api = ApiClient();
  bool _busy = false;
  bool _obscure = true;

  @override
  void dispose() {
    _name.dispose();
    _email.dispose();
    _password.dispose();
    _confirm.dispose();
    _programme.dispose();
    _api.close();
    super.dispose();
  }

  Future<void> _register() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() => _busy = true);
    try {
      await _api.post('/student/register', body: {
        'fullName': _name.text.trim(),
        'email': _email.text.trim().toLowerCase(),
        'password': _password.text,
        'info': _programme.text.trim(),
      });
      if (!mounted) return;
      await showDialog<void>(
        context: context,
        builder: (ctx) => AlertDialog(
          icon: const Icon(Icons.check_circle_outline, size: 48),
          title: const Text('Account created'),
          content: const Text('Your student account has been created successfully. You can now sign in.'),
          actions: [FilledButton(onPressed: () => Navigator.pop(ctx), child: const Text('Continue'))],
        ),
      );
      if (mounted) Navigator.pushReplacementNamed(context, '/login');
    } on ApiException catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.message)));
    } catch (_) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Unable to create your account. Check your connection and try again.')),
      );
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Scaffold(
      appBar: AppBar(title: const Text('Create Student Account')),
      body: Container(
        decoration: BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
            colors: [scheme.primary.withOpacity(.06), scheme.surface, scheme.secondary.withOpacity(.05)],
          ),
        ),
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(24),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 520),
              child: Card(
                child: Padding(
                  padding: const EdgeInsets.all(28),
                  child: Form(
                    key: _formKey,
                    child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
                      Icon(Icons.restaurant_rounded, size: 52, color: scheme.secondary),
                      const SizedBox(height: 12),
                      Text('Welcome to ATU Cafeteria', textAlign: TextAlign.center,
                        style: Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w900)),
                      const SizedBox(height: 6),
                      const Text('Create a student account to browse meals and place orders.', textAlign: TextAlign.center),
                      const SizedBox(height: 26),
                      TextFormField(controller: _name, textInputAction: TextInputAction.next,
                        decoration: const InputDecoration(labelText: 'Full name', prefixIcon: Icon(Icons.person_outline)),
                        validator: (v) => v == null || v.trim().length < 2 ? 'Enter your full name.' : null),
                      const SizedBox(height: 14),
                      TextFormField(controller: _email, keyboardType: TextInputType.emailAddress, textInputAction: TextInputAction.next,
                        decoration: const InputDecoration(labelText: 'Email address', prefixIcon: Icon(Icons.email_outlined)),
                        validator: (v) {
                          final value = v?.trim() ?? '';
                          return RegExp(r'^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$').hasMatch(value) ? null : 'Enter a valid email address.';
                        }),
                      const SizedBox(height: 14),
                      TextFormField(controller: _password, obscureText: _obscure, textInputAction: TextInputAction.next,
                        decoration: InputDecoration(labelText: 'Password', helperText: 'At least 8 characters.', prefixIcon: const Icon(Icons.lock_outline),
                          suffixIcon: IconButton(onPressed: () => setState(() => _obscure = !_obscure), icon: Icon(_obscure ? Icons.visibility : Icons.visibility_off))),
                        validator: (v) => v == null || v.length < 8 ? 'Use at least 8 characters.' : null),
                      const SizedBox(height: 14),
                      TextFormField(controller: _confirm, obscureText: true, textInputAction: TextInputAction.next,
                        decoration: const InputDecoration(labelText: 'Confirm password', prefixIcon: Icon(Icons.lock_reset_outlined)),
                        validator: (v) => v != _password.text ? 'Passwords do not match.' : null),
                      const SizedBox(height: 14),
                      TextFormField(controller: _programme, maxLines: 2,
                        decoration: const InputDecoration(labelText: 'Department / Programme (optional)', prefixIcon: Icon(Icons.school_outlined))),
                      const SizedBox(height: 24),
                      SizedBox(height: 52, child: FilledButton.icon(
                        onPressed: _busy ? null : _register,
                        icon: _busy ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2)) : const Icon(Icons.person_add_alt_1),
                        label: Text(_busy ? 'Creating account…' : 'Create account'),
                      )),
                      const SizedBox(height: 8),
                      TextButton(onPressed: _busy ? null : () => Navigator.pushReplacementNamed(context, '/login'),
                        child: const Text('Already have an account? Sign in')),
                    ]),
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
