import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/cafeteria_provider.dart';
import '../screens/login_screen.dart';

class AuthGate extends StatefulWidget {
  const AuthGate({super.key});
  @override
  State<AuthGate> createState() => _AuthGateState();
}

class _AuthGateState extends State<AuthGate> {
  bool ready = false;
  @override
  void initState() {
    super.initState();
    _restore();
  }

  Future<void> _restore() async {
    await context.read<CafeteriaProvider>().restoreSession();
    if (mounted) setState(() => ready = true);
  }

  @override
  Widget build(BuildContext context) {
    if (!ready) {
      return const Scaffold(body: Center(child: CircularProgressIndicator()));
    }
    final p = context.watch<CafeteriaProvider>();
    if (p.currentUser == null) return const LoginScreen();
    final role = p.currentUser!.role;
    if (role == 'ADMIN') return const _RoutePage(route: '/admin');
    if (role == 'VENDOR') return const _RoutePage(route: '/vendor');
    return const _RoutePage(route: '/student');
  }
}

class _RoutePage extends StatelessWidget {
  final String route;
  const _RoutePage({required this.route});
  @override
  Widget build(BuildContext context) {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (ModalRoute.of(context)?.settings.name != route) {
        Navigator.pushReplacementNamed(context, route);
      }
    });
    return const Scaffold(body: Center(child: CircularProgressIndicator()));
  }
}
