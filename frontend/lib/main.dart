import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:atu_cafeteria/core/theme/app_theme.dart';
import 'package:atu_cafeteria/presentation/providers/cafeteria_provider.dart';
import 'package:atu_cafeteria/presentation/providers/admin_state_provider.dart';
import 'package:atu_cafeteria/presentation/providers/cart_provider.dart';
import 'package:atu_cafeteria/core/network/api_client.dart';
import 'presentation/screens/customer_vendor_menu_screen.dart';
import 'package:atu_cafeteria/presentation/screens/login_screen.dart';
import 'package:atu_cafeteria/presentation/screens/register_screen.dart';
import 'package:atu_cafeteria/presentation/screens/password_reset_screen.dart';
import 'package:atu_cafeteria/presentation/screens/email_verification_screen.dart';
import 'package:atu_cafeteria/presentation/screens/public_home_screen.dart';
import 'package:atu_cafeteria/presentation/screens/cart_screen.dart';
import 'package:atu_cafeteria/presentation/screens/checkout_screen.dart';
import 'package:atu_cafeteria/presentation/screens/admin_security_screen.dart';
import 'package:atu_cafeteria/presentation/screens/smart_insights_screen.dart';
import 'package:atu_cafeteria/presentation/screens/kiosk_screen.dart';
import 'package:atu_cafeteria/presentation/screens/vendor_dashboard.dart';
import 'package:atu_cafeteria/presentation/screens/vendor_finance_screen.dart';
import 'package:atu_cafeteria/presentation/screens/vendor_promotions_screen.dart';
import 'package:atu_cafeteria/presentation/screens/vendor_payout_account_screen.dart';
import 'package:atu_cafeteria/presentation/screens/vendor_order_display_screen.dart';
import 'package:atu_cafeteria/presentation/screens/vendor_order_workflow_screen.dart';
import 'package:atu_cafeteria/presentation/screens/kfc_ordering_screen.dart';
import 'package:atu_cafeteria/presentation/screens/order_tracking_screen.dart';
import 'package:atu_cafeteria/presentation/screens/group_order_screen.dart';
import 'package:atu_cafeteria/presentation/screens/food_detail_screen.dart';
import 'package:atu_cafeteria/presentation/screens/customer_home_screen.dart';
import 'package:atu_cafeteria/presentation/screens/customer_account_screen.dart';
import 'package:atu_cafeteria/presentation/screens/customer_addresses_screen.dart';
import 'package:atu_cafeteria/presentation/screens/customer_devices_screen.dart';
import 'package:atu_cafeteria/presentation/screens/customer_support_screen.dart';
import 'package:atu_cafeteria/presentation/screens/reference_admin_screen.dart';
import 'package:atu_cafeteria/presentation/screens/notifications_screen.dart';
import 'package:atu_cafeteria/presentation/screens/splash_screen.dart';
import 'package:atu_cafeteria/core/config/server_config.dart';
import 'package:atu_cafeteria/domain/models/models.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await ServerConfig.init();
  runApp(const ATUCafeteriaApp());
}

class ATUCafeteriaApp extends StatelessWidget {
  const ATUCafeteriaApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MultiProvider(
      providers: [
        Provider<ApiClient>(create: (_) => ApiClient()),
        ChangeNotifierProvider(create: (_) => CafeteriaProvider()),
        ChangeNotifierProvider(create: (_) => CartProvider()),
        ChangeNotifierProvider(
          create: (ctx) => AdminStateProvider(ctx.read<ApiClient>()),
        ),
      ],
      child: MaterialApp(
        title: 'ATU Cafeteria',
        debugShowCheckedModeBanner: false,
        theme: AppTheme.light(),
        darkTheme: AppTheme.dark(),
        themeMode: ThemeMode.system,
        home: const SplashScreen(),
        routes: {
          '/home': (_) => const PublicHomeScreen(),
          '/login': (_) => const LoginScreen(),
          '/register': (_) => const RegisterScreen(),
          '/customer': (_) => const CustomerHomeScreen(),
          '/customer-account': (_) => const CustomerAccountScreen(),
          '/customer-addresses': (_) => const CustomerAddressesScreen(),
          '/customer-devices': (_) => const CustomerDevicesScreen(),
          '/customer-support': (_) => const CustomerSupportScreen(),
          // Kept as a compatibility route for existing deep links. New
          // customer-facing flows should use /customer.
          '/student': (_) => const CustomerHomeScreen(),
          // Primary restaurant operations entry point.
          '/vendor': (_) => const VendorDashboardScreen(),
          '/vendor-dashboard': (_) => const VendorDashboardScreen(),

          // Dedicated operational modules.
          '/vendor-orders': (_) => const VendorOrderWorkflowScreen(),
          '/vendor-finance': (_) => const VendorFinanceScreen(),
          '/vendor-promotions': (_) => const VendorPromotionsScreen(),
          '/vendor-payout-account': (_) => const VendorPayoutAccountScreen(),
          '/admin': (_) => const ReferenceAdminScreen(),
          '/reset-password': (_) => const PasswordResetScreen(),
          '/verify-email': (_) => const EmailVerificationScreen(),
          '/admin-security': (_) => const AdminSecurityScreen(),
          '/smart-insights': (_) => const SmartInsightsScreen(),
          '/notifications': (_) => const NotificationsScreen(),
          '/cart': (_) => const CartScreen(),
          '/checkout': (_) => const CheckoutScreen(),
          '/kiosk': (_) => const KioskScreen(),
          '/vendor-display': (_) => const VendorOrderDisplayScreen(),
          '/kfc-ordering': (_) => const KfcOrderingScreen(),
          '/group-order': (_) => const GroupOrderScreen(),
          '/restaurant-menu': (context) {
            final vendor = ModalRoute.of(context)?.settings.arguments;
            return vendor is User
                ? CustomerVendorMenuScreen(vendor: vendor)
                : const Scaffold(
                    body: Center(
                      child: Text('Invalid restaurant.'),
                    ),
                  );
          },
          '/food-detail': (context) {
            final item = ModalRoute.of(context)?.settings.arguments;
            return item is FoodItem
                ? FoodDetailScreen(item: item)
                : const Scaffold(
                    body: Center(child: Text('Invalid food item.')));
          },
          '/order-tracking': (context) {
            final id = ModalRoute.of(context)?.settings.arguments;
            final orderId = id is int ? id : int.tryParse(id?.toString() ?? '');
            return orderId == null
                ? const Scaffold(body: Center(child: Text('Invalid order ID.')))
                : OrderTrackingScreen(orderId: orderId);
          },
        },
      ),
    );
  }
}
