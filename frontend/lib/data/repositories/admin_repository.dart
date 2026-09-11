import 'package:atu_cafeteria/core/network/api_client.dart';

class AdminRepository {
  final ApiClient api;
  AdminRepository(this.api);

  Future<Map<String, dynamic>> dashboard() async => _map(await api.request('GET', 'admin/dashboard'));
  Future<Map<String, dynamic>> users({String? role, String? status, String? search}) async => _map(await api.request('GET', _query('admin/users', {'role': role, 'status': status, 'search': search})));
  Future<Map<String, dynamic>> vendors() async => _map(await api.request('GET', 'admin/vendors'));
  Future<Map<String, dynamic>> createVendor({required String email, required String password, required String fullName, required String storeName, String? location, String? contactEmail, String? contactInfo}) async => _map(await api.request('POST', 'admin/vendors', body: {'email': email, 'password': password, 'fullName': fullName, 'storeName': storeName, 'location': location, 'contactEmail': contactEmail, 'contactInfo': contactInfo}));
  Future<Map<String, dynamic>> orders({String? status}) async => _map(await api.request('GET', _query('admin/orders', {'status': status})));
  Future<Map<String, dynamic>> finance() async => _map(await api.request('GET', 'admin/finance/summary'));
  Future<Map<String, dynamic>> auditLogs() async => _map(await api.request('GET', 'admin/audit-logs'));
  Future<Map<String, dynamic>> settings() async => _map(await api.request('GET', 'admin/settings'));
  Future<Map<String, dynamic>> updateUserStatus(int id, String status) async => _map(await api.request('PATCH', 'admin/users/$id/status', body: {'account_status': status}));
  Future<Map<String, dynamic>> updateVendorStatus(int id, String status) async => _map(await api.request('PATCH', 'admin/vendors/$id/status', body: {'operational_status': status}));
  Future<Map<String, dynamic>> updateAdminLevel(int id, String level) async => _map(await api.request('PATCH', 'admin/users/$id/admin-level', body: {'admin_level': level}));
  Future<Map<String, dynamic>> updateSetting(String key, String value) async => _map(await api.request('PUT', 'admin/settings/${Uri.encodeComponent(key)}', body: {'value': value}));
  Future<Map<String, dynamic>> walletAdjustment(int id, double amount, String type, String reason) async => _map(await api.request('POST', 'admin/finance/users/$id/wallet-adjustment', body: {'amount': amount, 'type': type, 'reason': reason}));

  Map<String, dynamic> _map(dynamic value) => value is Map<String, dynamic> ? value : <String, dynamic>{'data': value};
  String _query(String path, Map<String, String?> values) {
    final entries = values.entries.where((e) => e.value != null && e.value!.trim().isNotEmpty).map((e) => '${Uri.encodeQueryComponent(e.key)}=${Uri.encodeQueryComponent(e.value!)}');
    final q = entries.join('&');
    return q.isEmpty ? path : '$path?$q';
  }
}
