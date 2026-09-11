import 'dart:convert';
import 'package:http/http.dart' as http;
import '../models/user.dart';
import '../models/food_item.dart';
import '../models/order.dart';

class ApiService {
  static String baseUrl = 'http://10.0.2.2:8000/api/';
  static String? authToken;
  static Map<String, String> get _headers => {'Content-Type': 'application/json', 'Accept': 'application/json', if (authToken != null) 'Authorization': 'Bearer $authToken'};

  static Future<Map<String, dynamic>> login(String username, String pin) async {
    try {
      final response = await http.post(Uri.parse('${baseUrl}login'), headers: _headers, body: jsonEncode({'username': username.trim(), 'pin': pin}));
      final data = jsonDecode(response.body) as Map<String, dynamic>;
      if (response.statusCode == 200 && data['success'] == true && data['token'] != null && data['user'] != null) {
        authToken = data['token'] as String;
        return {'success': true, 'user': User.fromJson(data['user'] as Map<String, dynamic>)};
      }
      return {'success': false, 'message': data['message'] ?? 'Authentication failed.'};
    } catch (_) { return {'success': false, 'message': 'Unable to reach authentication server.'}; }
  }

  static Future<void> logout() async { try { if (authToken != null) await http.post(Uri.parse('${baseUrl}logout'), headers: _headers); } finally { authToken = null; } }

  static Future<List<FoodItem>> getFoodItems() async { try { final r = await http.get(Uri.parse('${baseUrl}food-items'), headers: _headers); if (r.statusCode == 200) { final data = jsonDecode(r.body) as List; return data.map((x) => FoodItem.fromJson(x)).toList(); } } catch (_) {} return []; }

  static Future<Order?> placeOrder({required int customerId, required int vendorId, required int foodItemId, required String foodName, required int quantity, required double unitPrice, required double totalPrice, int pointsToRedeem = 0, String? estimatedPickupTime}) async {
    try { final r = await http.post(Uri.parse('${baseUrl}orders'), headers: _headers, body: jsonEncode({'customer_id': customerId, 'vendor_id': vendorId, 'food_item_id': foodItemId, 'food_name': foodName, 'quantity': quantity, 'unit_price': unitPrice, 'total_price': totalPrice, 'points_to_redeem': pointsToRedeem, 'estimated_pickup_time': estimatedPickupTime})); if (r.statusCode == 200 || r.statusCode == 201) { final d = jsonDecode(r.body); return Order.fromJson(d['order'] ?? d); } } catch (_) {} return null;
  }

  static Future<bool> updateOrderStatus(int vendorId, int orderId, String newStatus, {String? estimatedTime}) async { try { final r = await http.put(Uri.parse('${baseUrl}vendor/$vendorId/orders/$orderId/status'), headers: _headers, body: jsonEncode({'status': newStatus, 'estimated_pickup_time': estimatedTime})); return r.statusCode == 200; } catch (_) { return false; } }
  static Future<bool> verifyPickup(int vendorId, String pin) async { try { final r = await http.post(Uri.parse('${baseUrl}vendor/orders/verify-pickup'), headers: _headers, body: jsonEncode({'vendor_id': vendorId, 'pickup_pin': pin})); return r.statusCode == 200; } catch (_) { return false; } }

  static Future<Map<String, dynamic>?> vendorOverview() async => _getMap('vendor/overview');
  static Future<List<dynamic>> vendorWorkers() async { final d = await _getMap('vendor/workers'); return List<dynamic>.from(d?['workers'] ?? []); }
  static Future<List<dynamic>> vendorReviews() async { final d = await _getMap('vendor/reviews'); return List<dynamic>.from(d?['reviews'] ?? []); }

  static Future<Map<String, dynamic>?> createWorker({required String fullName, required String username, required String pin, String position = 'Staff', String? phone}) async => _postMap('vendor/workers', {'full_name': fullName, 'username': username, 'pin': pin, 'position': position, 'phone': phone});
  static Future<Map<String, dynamic>?> updateWorker(int id, Map<String, dynamic> data) async => _patchMap('vendor/workers/$id', data);
  static Future<bool> deleteWorker(int id) async => _delete('vendor/workers/$id');
  static Future<Map<String, dynamic>?> createShift({required int workerId, required String startTime, required String endTime, String shiftName = 'Regular', String? shiftDate}) async => _postMap('vendor/shifts', {'worker_id': workerId, 'start_time': startTime, 'end_time': endTime, 'shift_name': shiftName, 'shift_date': shiftDate});
  static Future<Map<String, dynamic>?> updateShift(int id, Map<String, dynamic> data) async => _patchMap('vendor/shifts/$id', data);
  static Future<bool> deleteShift(int id) async => _delete('vendor/shifts/$id');

  static Future<Map<String, dynamic>?> adminOverview() async => _getMap('admin/overview');
  static Future<Map<String, dynamic>?> adminCreateVendor({required String fullName, required String username, required String pin, String? info}) async => _postMap('admin/vendors', {'fullName': fullName, 'username': username, 'pin': pin, 'info': info});

  static Future<Map<String, dynamic>?> _getMap(String path) async { try { final r = await http.get(Uri.parse('${baseUrl}$path'), headers: _headers); if (r.statusCode >= 200 && r.statusCode < 300) return jsonDecode(r.body) as Map<String, dynamic>; } catch (_) {} return null; }
  static Future<Map<String, dynamic>?> _postMap(String path, Map<String, dynamic> body) async { try { final r = await http.post(Uri.parse('${baseUrl}$path'), headers: _headers, body: jsonEncode(body)); if (r.statusCode >= 200 && r.statusCode < 300) return jsonDecode(r.body) as Map<String, dynamic>; } catch (_) {} return null; }
  static Future<Map<String, dynamic>?> _patchMap(String path, Map<String, dynamic> body) async { try { final r = await http.patch(Uri.parse('${baseUrl}$path'), headers: _headers, body: jsonEncode(body)); if (r.statusCode >= 200 && r.statusCode < 300) return jsonDecode(r.body) as Map<String, dynamic>; } catch (_) {} return null; }
  static Future<bool> _delete(String path) async { try { final r = await http.delete(Uri.parse('${baseUrl}$path'), headers: _headers); return r.statusCode >= 200 && r.statusCode < 300; } catch (_) { return false; } }
}
