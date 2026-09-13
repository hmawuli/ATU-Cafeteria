import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/cafeteria_provider.dart';

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});
  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final username = TextEditingController();
  final _formKey = GlobalKey<FormState>();
  final password = TextEditingController();
  bool obscure = true;

  @override
  void dispose() {
    username.dispose();
    password.dispose();
    super.dispose();
  }

  Future<void> login() async {
    final provider = context.read<CafeteriaProvider>();
    final ok = await provider.loginUser(username.text, password.text);
    if (!mounted) return;

    if (provider.requiresTwoFactor) {
      Navigator.push(
        context,
        MaterialPageRoute(
          builder: (_) => TwoFactorScreen(username: username.text.trim()),
        ),
      );
      return;
    }

    if (ok) {
      final role = provider.currentUser?.role;
      Navigator.pushReplacementNamed(
        context,
        role == 'ADMIN'
            ? '/admin'
            : role == 'VENDOR'
                ? '/vendor'
                : '/student',
      );
    } else {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(provider.loginError ?? 'Unable to sign in.')),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final loading = context.watch<CafeteriaProvider>().isLoading;
    return Scaffold(
      body: Container(
        decoration: BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
            colors: [
            const Color(0xFF0B1F3A),
            const Color(0xFF123B5D),
            const Color(0xFFE8751A),
          ],
          ),
        ),
        child: SafeArea(
          child: Center(
            child: SingleChildScrollView(
              padding: const EdgeInsets.all(24),
              child: ConstrainedBox(
                constraints: const BoxConstraints(maxWidth: 460),
                child: Card(
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(24),
                  ),
                  child: Padding(
                    padding: const EdgeInsets.all(28),
                    child: Form(key: _formKey, child: Column(
                      children: [
                        Container(
                          width: 82,
                          height: 82,
                          decoration: BoxDecoration(
                            gradient: const LinearGradient(
                              colors: [Color(0xFFE8751A), Color(0xFFFFB347)],
                              begin: Alignment.topLeft,
                              end: Alignment.bottomRight,
                            ),
                            borderRadius: BorderRadius.circular(24),
                            boxShadow: const [
                              BoxShadow(
                                blurRadius: 18,
                                offset: Offset(0, 8),
                                color: Color(0x33000000),
                              ),
                            ],
                          ),
                          child: const Icon(
                            Icons.restaurant_rounded,
                            size: 44,
                            color: Colors.white,
                          ),
                        ),
                        const SizedBox(height: 12),
                        const Text(
                          'ATU CAFETERIA',
                          style: TextStyle(
                            fontSize: 28,
                            fontWeight: FontWeight.w800,
                            letterSpacing: 1.2,
                          ),
                        ),
                        Text(
                          'SMART CAMPUS FOOD PLATFORM',
                          style: TextStyle(
                            color: Colors.blueGrey.shade600,
                            fontSize: 12,
                            fontWeight: FontWeight.w700,
                            letterSpacing: 1.5,
                          ),
                        ),
                        const SizedBox(height: 6),
                        const Text(
                          'Secure access for students, vendors and administrators.',
                          textAlign: TextAlign.center,
                        ),
                        const SizedBox(height: 28),
                        TextFormField(
                          controller: username,
                          autofocus: true,
                          decoration: const InputDecoration(
                            labelText: 'Username / Student ID',
                            prefixIcon: Icon(Icons.badge_outlined),
                            border: OutlineInputBorder(),
                          ),
                          validator: (v) => v == null || v.trim().isEmpty ? 'Enter your username or student ID.' : null,
                        ),
                        const SizedBox(height: 16),
                        TextFormField(
                          controller: password,
                          obscureText: obscure,
                          decoration: InputDecoration(
                            labelText: 'PIN',
                            prefixIcon: const Icon(Icons.lock),
                            suffixIcon: IconButton(
                              icon: Icon(
                                obscure ? Icons.visibility : Icons.visibility_off,
                              ),
                              onPressed: () => setState(() => obscure = !obscure),
                            ),
                            border: const OutlineInputBorder(),
                          ),
                        ),
                        Align(
                          alignment: Alignment.centerRight,
                          child: TextButton(
                            onPressed: () => Navigator.pushNamed(
                              context,
                              '/reset-password',
                            ),
                            child: const Text('Forgot password?'),
                          ),
                        ),
                        const SizedBox(height: 8),
                        SizedBox(
                          width: double.infinity,
                          height: 50,
                          child: ElevatedButton(
                            onPressed: loading ? null : () { if (_formKey.currentState?.validate() ?? false) login(); },
                            child: loading
                                ? const SizedBox(
                                    width: 22,
                                    height: 22,
                                    child: CircularProgressIndicator(),
                                  )
                                : const Text(
                                    'Sign In',
                                    style: TextStyle(fontWeight: FontWeight.bold),
                                  ),
                          ),
                        ),
                        const SizedBox(height: 12),
                        TextButton(
                          onPressed: () => Navigator.pushNamed(context, '/register'),
                          child: const Text('Create an account'),
                        ),
                      ],
                    )),
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
  final code = TextEditingController();

  @override
  void dispose() {
    code.dispose();
    super.dispose();
  }

  Future<void> verify() async {
    final provider = context.read<CafeteriaProvider>();
    final ok = await provider.verifyTwoFactor(widget.username, code.text);
    if (!mounted) return;
    if (ok) {
      Navigator.pushReplacementNamed(context, '/admin');
    } else {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(provider.loginError ?? 'Verification failed.')),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Security Verification'),
        backgroundColor: const Color(0xFF0B1F3A),
        foregroundColor: Colors.white,
      ),
      body: Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 460),
            child: Card(
              child: Padding(
                padding: const EdgeInsets.all(24),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Container(
                      padding: const EdgeInsets.all(18),
                      decoration: BoxDecoration(
                        color: const Color(0xFFE8751A).withOpacity(.12),
                        shape: BoxShape.circle,
                      ),
                      child: const Icon(
                        Icons.verified_user_rounded,
                        size: 52,
                        color: Color(0xFFE8751A),
                      ),
                    ),
                    const SizedBox(height: 16),
                    const Text(
                      'Two-factor authentication',
                      style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold),
                    ),
                    const SizedBox(height: 8),
                    const Text(
                      'Enter the 6-digit verification code sent to your administrator email address.',
                      textAlign: TextAlign.center,
                    ),
                    const SizedBox(height: 20),
                    TextField(
                      controller: code,
                      keyboardType: TextInputType.number,
                      maxLength: 6,
                      decoration: const InputDecoration(
                        labelText: 'Verification code',
                        border: OutlineInputBorder(),
                      ),
                    ),
                    SizedBox(
                      width: double.infinity,
                      height: 48,
                      child: ElevatedButton(
                        onPressed: verify,
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
}
