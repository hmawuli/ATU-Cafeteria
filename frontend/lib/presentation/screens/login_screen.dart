import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/cafeteria_provider.dart';

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final _formKey = GlobalKey<FormState>();
  final _username = TextEditingController();
  final _pin = TextEditingController();
  bool _obscure = true;

  @override
  void dispose() {
    _username.dispose();
    _pin.dispose();
    super.dispose();
  }

  Future<void> _login() async {
    if (!(_formKey.currentState?.validate() ?? false)) return;
    final provider = context.read<CafeteriaProvider>();
    final ok = await provider.loginUser(_username.text.trim(), _pin.text);
    if (!mounted) return;

    if (provider.requiresTwoFactor) {
      Navigator.push(
        context,
        MaterialPageRoute(
          builder: (_) => TwoFactorScreen(username: _username.text.trim()),
        ),
      );
      return;
    }

    if (!ok) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(provider.loginError ?? 'Unable to sign in.')),
      );
      return;
    }

    final role = provider.currentUser?.role.toUpperCase();
    final route = role == 'ADMIN'
        ? '/admin'
        : role == 'VENDOR'
            ? '/vendor'
            : '/student';
    Navigator.pushReplacementNamed(context, route);
  }

  @override
  Widget build(BuildContext context) {
    final loading = context.watch<CafeteriaProvider>().isLoading;

    return Scaffold(
      body: Container(
        decoration: const BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
            colors: [Color(0xFF063B82), Color(0xFF0A4C9C), Color(0xFF052B63)],
          ),
        ),
        child: SafeArea(
          child: Center(
            child: SingleChildScrollView(
              padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 28),
              child: ConstrainedBox(
                constraints: const BoxConstraints(maxWidth: 470),
                child: Card(
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(26),
                  ),
                  child: Padding(
                    padding: const EdgeInsets.fromLTRB(28, 30, 28, 25),
                    child: Form(
                      key: _formKey,
                      child: Column(
                        children: [
                          Container(
                            width: 82,
                            height: 82,
                            decoration: BoxDecoration(
                              color: const Color(0xFFFFC400),
                              borderRadius: BorderRadius.circular(22),
                            ),
                            child: const Icon(
                              Icons.restaurant_menu_rounded,
                              size: 46,
                              color: Color(0xFF063B82),
                            ),
                          ),
                          const SizedBox(height: 16),
                          const Text(
                            'ATU CAFETERIA',
                            style: TextStyle(
                              color: Color(0xFF063B82),
                              fontSize: 27,
                              fontWeight: FontWeight.w900,
                            ),
                          ),
                          const SizedBox(height: 3),
                          const Text(
                            'SMART CAMPUS FOOD PLATFORM',
                            style: TextStyle(
                              color: Color(0xFF4F6783),
                              fontSize: 11,
                              fontWeight: FontWeight.w800,
                              letterSpacing: 1.2,
                            ),
                          ),
                          const SizedBox(height: 8),
                          const Text(
                            'Secure access for students, vendors and administrators.',
                            textAlign: TextAlign.center,
                            style: TextStyle(
                              color: Color(0xFF65788D),
                              fontSize: 13,
                            ),
                          ),
                          const SizedBox(height: 27),
                          TextFormField(
                            controller: _username,
                            textInputAction: TextInputAction.next,
                            decoration: const InputDecoration(
                              labelText: 'Username',
                              prefixIcon: Icon(Icons.person_outline_rounded),
                            ),
                            validator: (value) =>
                                value == null || value.trim().isEmpty
                                    ? 'Enter your username.'
                                    : null,
                          ),
                          const SizedBox(height: 14),
                          TextFormField(
                            controller: _pin,
                            obscureText: _obscure,
                            keyboardType: TextInputType.number,
                            onFieldSubmitted: (_) => loading ? null : _login(),
                            decoration: InputDecoration(
                              labelText: 'PIN',
                              prefixIcon:
                                  const Icon(Icons.lock_outline_rounded),
                              suffixIcon: IconButton(
                                icon: Icon(
                                  _obscure
                                      ? Icons.visibility
                                      : Icons.visibility_off,
                                ),
                                onPressed: () =>
                                    setState(() => _obscure = !_obscure),
                              ),
                            ),
                            validator: (value) => value == null || value.isEmpty
                                ? 'Enter your PIN.'
                                : null,
                          ),
                          Align(
                            alignment: Alignment.centerRight,
                            child: TextButton(
                              onPressed: () => Navigator.pushNamed(
                                  context, '/reset-password'),
                              child: const Text('Forgot password?'),
                            ),
                          ),
                          const SizedBox(height: 4),
                          SizedBox(
                            width: double.infinity,
                            height: 52,
                            child: ElevatedButton(
                              onPressed: loading ? null : _login,
                              child: loading
                                  ? const SizedBox(
                                      width: 22,
                                      height: 22,
                                      child: CircularProgressIndicator(
                                          strokeWidth: 2),
                                    )
                                  : const Text(
                                      'Sign In',
                                      style: TextStyle(
                                        fontSize: 16,
                                        fontWeight: FontWeight.w900,
                                      ),
                                    ),
                            ),
                          ),
                          const SizedBox(height: 15),
                          Row(
                            children: [
                              Expanded(
                                  child: Divider(color: Colors.grey.shade300)),
                              const Padding(
                                padding: EdgeInsets.symmetric(horizontal: 12),
                                child: Text('or'),
                              ),
                              Expanded(
                                  child: Divider(color: Colors.grey.shade300)),
                            ],
                          ),
                          const SizedBox(height: 10),
                          TextButton(
                            onPressed: () =>
                                Navigator.pushNamed(context, '/register'),
                            child: const Text(
                              'Create an account',
                              style: TextStyle(fontWeight: FontWeight.w800),
                            ),
                          ),
                          const SizedBox(height: 10),
                          const Row(
                            mainAxisAlignment: MainAxisAlignment.center,
                            children: [
                              Icon(Icons.verified_user_outlined,
                                  size: 15, color: Color(0xFF20B95A)),
                              SizedBox(width: 6),
                              Text('Secure ATU Cafeteria access',
                                  style: TextStyle(fontSize: 11)),
                            ],
                          ),
                        ],
                      ),
                    ),
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

class TwoFactorScreen extends StatefulWidget {
  final String username;
  const TwoFactorScreen({super.key, required this.username});

  @override
  State<TwoFactorScreen> createState() => _TwoFactorScreenState();
}

class _TwoFactorScreenState extends State<TwoFactorScreen> {
  final _code = TextEditingController();

  @override
  void dispose() {
    _code.dispose();
    super.dispose();
  }

  Future<void> _verify() async {
    final provider = context.read<CafeteriaProvider>();
    final ok =
        await provider.verifyTwoFactor(widget.username, _code.text.trim());
    if (!mounted) return;
    if (ok) {
      final role = provider.currentUser?.role.toUpperCase();
      final route = role == 'ADMIN'
          ? '/admin'
          : role == 'VENDOR'
              ? '/vendor'
              : '/student';
      if (!mounted) return;
      Navigator.pushReplacementNamed(context, route);
    } else {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(provider.loginError ?? 'Verification failed.')),
      );
    }
  }

  @override
  Widget build(BuildContext context) => Scaffold(
        appBar: AppBar(title: const Text('Security Verification')),
        body: Center(
          child: Padding(
            padding: const EdgeInsets.all(24),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 460),
              child: Card(
                child: Padding(
                  padding: const EdgeInsets.all(26),
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      const Icon(Icons.verified_user_rounded, size: 50),
                      const SizedBox(height: 15),
                      const Text(
                        'Two-factor authentication',
                        style: TextStyle(
                            fontSize: 21, fontWeight: FontWeight.w900),
                      ),
                      const SizedBox(height: 8),
                      const Text(
                        'Enter the 6-digit verification code sent to your administrator email address.',
                        textAlign: TextAlign.center,
                      ),
                      const SizedBox(height: 20),
                      TextField(
                        controller: _code,
                        keyboardType: TextInputType.number,
                        maxLength: 6,
                        decoration: const InputDecoration(
                            labelText: 'Verification code'),
                      ),
                      const SizedBox(height: 4),
                      SizedBox(
                        width: double.infinity,
                        height: 50,
                        child: ElevatedButton(
                          onPressed: _verify,
                          child: const Text('Verify & Continue'),
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
