import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'providers/auth_provider.dart';
import 'providers/cafeteria_provider.dart';
import 'screens/login_screen.dart';
import 'viewmodel/cafeteria_provider.dart' as admin_data;

void main() {
  runApp(const ATUCafeteriaApp());
}

class ATUCafeteriaApp extends StatelessWidget {
  const ATUCafeteriaApp({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return MultiProvider(
      providers: [
        ChangeNotifierProvider(create: (_) => AuthProvider()),
        ChangeNotifierProvider(create: (_) => CafeteriaProvider()),
        // Admin dashboard uses the richer local/remote data provider.
        ChangeNotifierProvider(create: (_) => admin_data.CafeteriaProvider()),
      ],
      child: MaterialApp(
        title: 'ATU Cafeteria',
        debugShowCheckedModeBanner: false,
        theme: ThemeData(
          useMaterial3: true,
          colorSchemeSeed: Colors.deepOrange,
          scaffoldBackgroundColor: const Color(0xFFF9F9F9),
        ),
        home: const LoginScreen(),
      ),
    );
  }
}
