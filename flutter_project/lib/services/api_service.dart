import 'dart:convert';
import 'package:http/http.dart' as http;
import '../models/user.dart';
import '../models/food_item.dart';
import '../models/order.dart';

class ApiService {
  static String baseUrl = 'http://10.0.2.2:8000/api/';
  static String? authToken;

  static Map<String, String> get _headers => {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
        if (authToken != null) 'Authorization': 'Bearer $authToken',
      };

  /// Authenticate against the Laravel API.
  /// The PIN is sent only over the configured transport and is hashed server-side.
  static Future<Map<String, dynamic>> login(String username, String pin) async {
    try {
      final response = await http.post(
        Uri.parse('${baseUrl}login'),
        headers: _headers,
        body: jsonEncode({'username': username.trim(), 'pin': pin}),
      );

      final data = jsonDecode(response.body) as Map<String, dynamic>;
      if (response.statusCode == 200 && data['success'] == true && data['token'] != null && data['user'] != null) {
        authToken = data['token'] as String;
        return {'success': true, 'user': User.fromJson(data['user'] as Map<String, dynamic>)};
      }

      return {
        'success': false,
        'message': data['message'] ?? 'Authentication failed.',
      };
    } catch (e) {
      return {'success': false, 'message': 'Unable to reach authentication server.'};
    }
  }

  static Future<void> logout() async {
    try {
      if (authToken != null) {
        await http.post(Uri.parse('${baseUrl}logout'), headers: _headers);
      }
    } finally {
      authToken = null;
    }
  }

  static Future<List<FoodItem>> getFoodItems() async {
    try {
      final response = await http.get(Uri.parse('${baseUrl}food-items'), headers: _headers);
      if (response.statusCode == 200) {
        final List<dynamic> data = jsonDecode(response.body);
        return data.map((item) => FoodItem.fromJson(item)).toList();
      }
    } catch (_) {}
    return [];
  }

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
      final response = await http.post(Uri.parse('${baseUrl}orders'), headers: _headers, body: jsonEncode(body));
      if (response.statusCode == 200 || response.statusCode == 201) {
        final data = jsonDecode(response.body);
        return Order.fromJson(data['order'] ?? data);
      }
    } catch (_) {}
    return null;
  }

  static Future<bool> updateOrderStatus(int vendorId, int orderId, String newStatus, {String? estimatedTime}) async {
    try {
      final response = await http.put(
        Uri.parse('${baseUrl}vendor/$vendorId/orders/$orderId/status'),
        headers: _headers,
        body: jsonEncode({'status': newStatus, 'estimated_pickup_time': estimatedTime}),
      );
      return response.statusCode == 200;
    } catch (_) {
      return false;
    }
  }

  static Future<bool> verifyPickup(int vendorId, String pin) async {
    try {
      final response = await http.post(
        Uri.parse('${baseUrl}vendor/orders/verify-pickup'),
        headers: _headers,
        body: jsonEncode({'vendor_id': vendorId, 'pickup_pin': pin}),
      );
      return response.statusCode == 200;
    } catch (_) {
      return false;
    }
  }
}
