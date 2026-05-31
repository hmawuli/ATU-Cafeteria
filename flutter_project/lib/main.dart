import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'viewmodel/cafeteria_provider.dart';
import 'screens/login_screen.dart';
import 'screens/register_screen.dart';
import 'screens/student_dashboard.dart';
import 'screens/vendor_dashboard.dart';
import 'screens/admin_dashboard.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  
  final provider = CafeteriaProvider();
  await provider.initialize(); // Seeds SQLite db automatically

  runApp(
    ChangeNotifierProvider<CafeteriaProvider>.value(
      value: provider,
      child: const AtuCafeteriaApp(),
    ),
  );
}

class AtuCafeteriaApp extends StatelessWidget {
  const AtuCafeteriaApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'ATU Cafeteria',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        useMaterial3: true,
        colorScheme: ColorScheme.fromSeed(
          seedColor: const Color(0xFF0038A8), // ATU Sapphire Blue
          primary: const Color(0xFF0038A8),
          secondary: const Color(0xFFE5A93C), // ATU Golden Yellow
          background: const Color(0xFFF8FAFC),
          surface: Colors.white,
        ),
        cardTheme: const CardTheme(
          elevation: 2,
          margin: EdgeInsets.symmetric(vertical: 6, horizontal: 12),
        ),
        appBarTheme: const AppBarTheme(
          backgroundColor: Color(0xFF0038A8),
          foregroundColor: Colors.white,
          centerTitle: true,
          titleTextStyle: TextStyle(
            fontWeight: FontWeight.bold,
            fontSize: 20,
            letterSpacing: 1.1,
          ),
        ),
        elevatedButtonTheme: ElevatedButtonThemeData(
          style: ElevatedButton.styleFrom(
            minimumSize: const Size(double.infinity, 48),
            shape: RoundedCornerShape(12),
            backgroundColor: const Color(0xFF0038A8),
            foregroundColor: Colors.white,
          ),
        ),
      ),
      initialRoute: '/login',
      routes: {
        '/login': (context) => const LoginScreen(),
        '/register': (context) => const RegisterScreen(),
        '/student_home': (context) => const StudentDashboardScreen(),
        '/vendor_home': (context) => const VendorDashboardScreen(),
        '/admin_home': (context) => const AdminDashboardScreen(),
      },
    );
  }
}

// Global rounded corner shape helper
RoundedRectangleBorder RoundedCornerShape(double radius) {
  return RoundedRectangleBorder(
    borderRadius: BorderRadius.circular(radius),
  );
}
