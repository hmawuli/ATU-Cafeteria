import 'package:atu_cafeteria/core/network/api_client.dart';

class SmartCafeteriaRepository {
  final ApiClient api;
  SmartCafeteriaRepository(this.api);

  Future<Map<String, dynamic>> commandCenter() async => _map(await api.request('GET', 'admin/command-center'));
  Future<Map<String, dynamic>> securityAlerts() async => _map(await api.request('GET', 'admin/security-alerts'));
  Future<Map<String, dynamic>> recommendations() async => _map(await api.request('GET', 'student/recommendations'));
  Future<Map<String, dynamic>> queue(int orderId) async => _map(await api.request('GET', 'orders/$orderId/queue'));
  Future<Map<String, dynamic>> demandForecast() async => _map(await api.request('GET', 'vendor/demand-forecast'));
  Future<Map<String, dynamic>> wasteSummary() async => _map(await api.request('GET', 'vendor/waste/summary'));
  Future<Map<String, dynamic>> recordWaste({int? foodItemId, required int prepared, required int sold, required int wasted, String? reason}) async => _map(await api.request('POST', 'vendor/waste', body: {
    if (foodItemId != null) 'food_item_id': foodItemId,
    'prepared_quantity': prepared,
    'sold_quantity': sold,
    'wasted_quantity': wasted,
    if (reason != null && reason.trim().isNotEmpty) 'reason': reason.trim(),
  }));

  Map<String, dynamic> _map(dynamic value) => value is Map<String, dynamic> ? value : <String, dynamic>{'data': value};
}
