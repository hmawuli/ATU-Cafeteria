import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:atu_cafeteria/domain/models/models.dart';
import 'package:atu_cafeteria/core/config/app_config.dart';

class ApiService {
  static String baseUrl = AppConfig.normalizedApiBaseUrl;
  static String? authToken;

  static Map<String, String> get _headers => {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
        if (authToken != null) 'Authorization': 'Bearer $authToken',
      };

  // 1. Authentication
  static Future<Map<String, dynamic>> login(String username, String password) async {
    try {
      final response = await http.post(
        Uri.parse('${baseUrl}login'),
        headers: _headers,
        body: jsonEncode({'username': username, 'pin': password}),
      );
      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        if (data['token'] != null) {
          authToken = data['token'];
        }
        return {'success': true, 'user': User.fromJson(data['user'])};
      }
      return {'success': false, 'message': 'Invalid credentials'};
    } catch (e) {
      return {'success': false, 'message': e.toString()};
    }
  }

  // 2. Fetch Food Items
  static Future<List<FoodItem>> getFoodItems() async {
    try {
      final response = await http.get(Uri.parse('${baseUrl}food-items'), headers: _headers);
      if (response.statusCode == 200) {
        final List<dynamic> data = jsonDecode(response.body);
        return data.map((item) => FoodItem.fromJson(item)).toList();
      }
    } catch (e) {
      // Fallback
    }
    return [];
  }

  // 3. Place Order
  static Future<Order?> placeOrder({
    required int customerId,
    required int vendorId,
    required int foodItemId,
    required String foodName,
    required int quantity,
    required double unitPrice,
    required double totalPrice,
    int pointsToRedeem = 0,
    String? estimatedPickupTime,
  }) async {
    try {
      final body = {
        'customer_id': customerId,
        'vendor_id': vendorId,
        'food_item_id': foodItemId,
        'food_name': foodName,
        'quantity': quantity,
        'unit_price': unitPrice,
        'total_price': totalPrice,
        'points_to_redeem': pointsToRedeem,
        'estimated_pickup_time': estimatedPickupTime,
      };

      final response = await http.post(
        Uri.parse('${baseUrl}orders'),
        headers: _headers,
        body: jsonEncode(body),
      );

      if (response.statusCode == 200 || response.statusCode == 201) {
        final data = jsonDecode(response.body);
        return Order.fromJson(data['order'] ?? data);
      }
    } catch (e) {
      // Fallback
    }
    return null;
  }

  // 4. Update Order Status
  static Future<bool> updateOrderStatus(int vendorId, int orderId, String newStatus, {String? estimatedTime}) async {
    try {
      final response = await http.put(
        Uri.parse('${baseUrl}vendor/$vendorId/orders/$orderId/status'),
        headers: _headers,
        body: jsonEncode({
          'status': newStatus,
          'estimated_pickup_time': estimatedTime,
        }),
      );
      return response.statusCode == 200;
    } catch (e) {
      return false;
    }
  }

  // 5. Verify Pickup PIN
  static Future<bool> verifyPickup(int vendorId, String pin) async {
    try {
      final response = await http.post(
        Uri.parse('${baseUrl}vendor/orders/verify-pickup'),
        headers: _headers,
        body: jsonEncode({
          'vendor_id': vendorId,
          'pickup_pin': pin,
        }),
      );
      return response.statusCode == 200;
    } catch (e) {
      return false;
    }
  }
}
