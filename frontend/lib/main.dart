import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:atu_cafeteria/core/theme/app_theme.dart';
import 'package:atu_cafeteria/presentation/providers/cafeteria_provider.dart';
import 'package:atu_cafeteria/presentation/providers/admin_state_provider.dart';
import 'package:atu_cafeteria/core/network/api_client.dart';
import 'package:atu_cafeteria/presentation/screens/admin_dashboard.dart';
import 'package:atu_cafeteria/presentation/screens/login_screen.dart';
import 'package:atu_cafeteria/presentation/screens/register_screen.dart';
import 'package:atu_cafeteria/presentation/screens/student_dashboard.dart';
import 'package:atu_cafeteria/presentation/screens/vendor_dashboard.dart';
import 'package:atu_cafeteria/presentation/screens/smart_insights_screen.dart';
import 'package:atu_cafeteria/presentation/screens/password_reset_screen.dart';
import 'package:atu_cafeteria/presentation/screens/email_verification_screen.dart';
import 'package:atu_cafeteria/presentation/screens/admin_security_screen.dart';
import 'package:atu_cafeteria/presentation/widgets/auth_gate.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const ATUCafeteriaApp());
}

class ATUCafeteriaApp extends StatelessWidget {
  const ATUCafeteriaApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MultiProvider(
      providers: [
        ChangeNotifierProvider(create: (_) => CafeteriaProvider()),
        ChangeNotifierProvider(create: (_) => AdminStateProvider(ApiClient())),
      ],
      child: MaterialApp(
        title: 'ATU Cafeteria',
        debugShowCheckedModeBanner: false,
        theme: AppTheme.light(),
        home: const AuthGate(),
        routes: {
          '/login': (_) => const LoginScreen(),
          '/register': (_) => const RegisterScreen(),
          '/student': (_) => const StudentDashboardScreen(),
          '/vendor': (_) => const VendorDashboardScreen(),
          '/admin': (_) => const AdminDashboardScreen(),
          '/smart-insights': (_) => const SmartInsightsScreen(),
          '/reset-password': (_) => const PasswordResetScreen(),
          '/verify-email': (_) => const EmailVerificationScreen(),
          '/admin-security': (_) => const AdminSecurityScreen(),
        },
      ),
    );
  }
}
