import 'package:flutter/foundation.dart';

import 'package:atu_cafeteria/core/network/api_client.dart';
import 'package:atu_cafeteria/domain/models/models.dart';

/// Focused state for order data (vendor + customer views).
///
/// Extracted from the monolith `CafeteriaProvider` so order flow has its own
/// ChangeNotifier, its own I/O and can be tested in isolation. The provider
/// delegates its public order API here and forwards notifications.
class OrdersState extends ChangeNotifier {
  List<Order> _vendorOrders = [];

  List<Order> get vendorOrders => _vendorOrders;

  List<Order> _customerOrders = [];

  List<Order> get customerOrders => _customerOrders;

  void setVendorOrders(List<Order> orders) {
    _vendorOrders = List<Order>.from(orders);
    notifyListeners();
  }

  void setCustomerOrders(List<Order> orders) {
    _customerOrders = List<Order>.from(orders);
    notifyListeners();
  }

  void clear() {
    _vendorOrders = [];
    _customerOrders = [];
    notifyListeners();
  }

  /// Refresh the vendor's orders from `GET /api/vendor/my-orders`.
  Future<List<Order>> refreshVendorOrders(ApiClient api, String? token) async {
    final value = await api
        .request('GET', 'vendor/my-orders', token: token)
        .timeout(const Duration(seconds: 8));

    if (value is List) {
      _vendorOrders = value
          .whereType<Map>()
          .map((e) => Order.fromJson(Map<String, dynamic>.from(e)))
          .toList();
      notifyListeners();
    }

    return _vendorOrders;
  }

  /// Verify a pickup PIN server-side and complete the order. Returns the
  /// server's success message. Orders are refreshed afterwards so every page
  /// watching [vendorOrders] reflects the completed state.
  Future<String> completePickup(
    ApiClient api,
    String? token, {
    required int orderId,
    required String pin,
  }) async {
    final decoded = await api
        .request(
          'POST',
          'orders/$orderId/verify-pickup',
          body: {'pickup_pin': pin},
          token: token,
        )
        .timeout(const Duration(seconds: 10));

    await refreshVendorOrders(api, token);

    return decoded is Map
        ? decoded['message']?.toString() ??
            'Pickup verified. Order completed.'
        : 'Pickup verified. Order completed.';
  }
}