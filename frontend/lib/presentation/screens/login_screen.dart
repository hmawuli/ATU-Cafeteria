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
  final password = TextEditingController();
  final _formKey = GlobalKey<FormState>();
  bool obscure = true;

  @override
  void dispose() { username.dispose(); password.dispose(); super.dispose(); }

  Future<void> login() async {
    if (!(_formKey.currentState?.validate() ?? false)) return;
    final provider = context.read<CafeteriaProvider>();
    final ok = await provider.loginUser(username.text.trim(), password.text);
    if (!mounted) return;
    if (provider.requiresTwoFactor) {
      Navigator.push(context, MaterialPageRoute(builder: (_) => TwoFactorScreen(username: username.text.trim())));
      return;
    }
    if (ok) {
      final role = provider.currentUser?.role;
      Navigator.pushReplacementNamed(context, role == 'ADMIN' ? '/admin' : role == 'VENDOR' ? '/vendor' : '/student');
    } else {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(provider.loginError ?? 'Unable to sign in.')));
    }
  }

  @override
  Widget build(BuildContext context) {
    final loading = context.watch<CafeteriaProvider>().isLoading;
    return Scaffold(
      body: Container(
        decoration: const BoxDecoration(
          gradient: LinearGradient(begin: Alignment.topCenter, end: Alignment.bottomCenter, colors: [Color(0xFF063B82), Color(0xFF0A4C9C), Color(0xFF052B63)]),
        ),
        child: SafeArea(child: Center(child: SingleChildScrollView(padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 28), child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 470),
          child: Card(shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(26)), child: Padding(padding: const EdgeInsets.fromLTRB(28, 30, 28, 25), child: Form(key: _formKey, child: Column(children: [
            Container(width: 82, height: 82, decoration: BoxDecoration(color: const Color(0xFFFFC400), borderRadius: BorderRadius.circular(22), boxShadow: const [BoxShadow(color: Color(0x33000000), blurRadius: 16, offset: Offset(0, 7))]), child: const Icon(Icons.restaurant_menu_rounded, size: 46, color: Color(0xFF063B82))),
            const SizedBox(height: 16),
            const Text('ATU CAFETERIA', style: TextStyle(color: Color(0xFF063B82), fontSize: 27, fontWeight: FontWeight.w900, letterSpacing: .5)),
            const SizedBox(height: 3),
            const Text('SMART CAMPUS FOOD PLATFORM', style: TextStyle(color: Color(0xFF4F6783), fontSize: 11, fontWeight: FontWeight.w800, letterSpacing: 1.2)),
            const SizedBox(height: 8),
            const Text('Secure access for students, vendors and administrators.', textAlign: TextAlign.center, style: TextStyle(color: Color(0xFF65788D), fontSize: 13)),
            const SizedBox(height: 27),
            TextFormField(controller: username, decoration: const InputDecoration(labelText: 'Username', prefixIcon: Icon(Icons.person_outline_rounded)), validator: (v) => v == null || v.trim().isEmpty ? 'Enter your username.' : null),
            const SizedBox(height: 14),
            TextFormField(controller: password, obscureText: obscure, onFieldSubmitted: (_) => loading ? null : login(), decoration: InputDecoration(labelText: 'PIN', prefixIcon: const Icon(Icons.lock_outline_rounded), suffixIcon: IconButton(icon: Icon(obscure ? Icons.visibility : Icons.visibility_off), onPressed: () => setState(() => obscure = !obscure))), validator: (v) => v == null || v.isEmpty ? 'Enter your PIN.' : null),
            Align(alignment: Alignment.centerRight, child: TextButton(onPressed: () => Navigator.pushNamed(context, '/reset-password'), child: const Text('Forgot password?'))),
            const SizedBox(height: 4),
            SizedBox(width: double.infinity, height: 52, child: ElevatedButton(onPressed: loading ? null : login, child: loading ? const SizedBox(width: 22, height: 22, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white)) : const Text('Sign In', style: TextStyle(fontSize: 16, fontWeight: FontWeight.w900)))),
            const SizedBox(height: 15),
            Row(children: [Expanded(child: Divider(color: Colors.grey.shade300)), const Padding(padding: EdgeInsets.symmetric(horizontal: 12), child: Text('or', style: TextStyle(color: Color(0xFF708196)))), Expanded(child: Divider(color: Colors.grey.shade300))]),
            const SizedBox(height: 10),
            TextButton(onPressed: () => Navigator.pushNamed(context, '/register'), child: const Text('Create an account', style: TextStyle(fontWeight: FontWeight.w800))),
            const SizedBox(height: 10),
            const Row(mainAxisAlignment: MainAxisAlignment.center, children: [Icon(Icons.verified_user_outlined, size: 15, color: Color(0xFF20B95A)), SizedBox(width: 6), Text('Secure ATU Cafeteria access', style: TextStyle(fontSize: 11, color: Color(0xFF65788D)))])
          ]))))
        ))),
      ),
    );
  }
}

class TwoFactorScreen extends StatefulWidget {
  final String username;
  const TwoFactorScreen({super.key, required this.username});
  @override State<TwoFactorScreen> createState() => _TwoFactorScreenState();
}
class _TwoFactorScreenState extends State<TwoFactorScreen> {
  final code = TextEditingController();
  @override void dispose() { code.dispose(); super.dispose(); }
  Future<void> verify() async {
    final provider = context.read<CafeteriaProvider>();
    final ok = await provider.verifyTwoFactor(widget.username, code.text);
    if (!mounted) return;
    if (ok) Navigator.pushReplacementNamed(context, '/admin');
    else ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(provider.loginError ?? 'Verification failed.')));
  }
  @override
  Widget build(BuildContext context) => Scaffold(appBar: AppBar(title: const Text('Security Verification')), body: Center(child: Padding(padding: const EdgeInsets.all(24), child: ConstrainedBox(constraints: const BoxConstraints(maxWidth: 460), child: Card(child: Padding(padding: const EdgeInsets.all(26), child: Column(mainAxisSize: MainAxisSize.min, children: [
    Container(padding: const EdgeInsets.all(18), decoration: BoxDecoration(color: const Color(0xFFFFC400).withValues(alpha: .15), shape: BoxShape.circle), child: const Icon(Icons.verified_user_rounded, size: 50, color: Color(0xFF063B82))),
    const SizedBox(height: 15), const Text('Two-factor authentication', style: TextStyle(fontSize: 21, fontWeight: FontWeight.w900, color: Color(0xFF0A2E68))), const SizedBox(height: 8), const Text('Enter the 6-digit verification code sent to your administrator email address.', textAlign: TextAlign.center), const SizedBox(height: 20),
    TextField(controller: code, keyboardType: TextInputType.number, maxLength: 6, decoration: const InputDecoration(labelText: 'Verification code')), const SizedBox(height: 4), SizedBox(width: double.infinity, height: 50, child: ElevatedButton(onPressed: verify, child: const Text('Verify & Continue')))
  ]))))));
}
