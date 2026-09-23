import 'package:atu_cafeteria/core/network/api_client.dart';
import 'package:atu_cafeteria/domain/models/models.dart';

class AuthRepository {
  final ApiClient client;
  AuthRepository(this.client);

  Future<(User user, String token)> login(String username, String pin) async {
    final data = await client.request('POST', 'login', body: {'username': username.trim(), 'pin': pin});
    final user = User.fromJson(Map<String, dynamic>.from(data['user'] ?? data));
    final token = data['token']?.toString() ?? '';
    client.token = token;
    return (user, token);
  }

  /// Restaurant-domain customer login. Legacy student login remains supported.
  Future<(User user, String token)> customerLogin(String email, String password) async {
    final data = await client.request('POST', 'customer/login', body: {
      'email': email.trim().toLowerCase(),
      'password': password,
    });
    final user = User.fromJson(Map<String, dynamic>.from(data['customer'] ?? data['user'] ?? data));
    final token = data['token']?.toString() ?? '';
    client.token = token;
    return (user, token);
  }

  Future<(User user, String token)> register({
    required String username,
    required String pin,
    required String role,
    required String fullName,
    String? info,
    String? email,
  }) async {
    final body = <String, dynamic>{
      'username': username.trim(),
      'pin': pin,
      'role': role,
      'fullName': fullName.trim(),
      'info': info,
      if (email != null && email.trim().isNotEmpty) 'email': email.trim(),
    };
    final data = await client.request('POST', 'register', body: body);
    final user = User.fromJson(Map<String, dynamic>.from(data['user'] ?? data));
    final token = data['token']?.toString() ?? '';
    client.token = token;
    return (user, token);
  }

  /// Restaurant-domain customer registration.
  Future<(User user, String token)> customerRegister({
    required String name,
    required String email,
    required String password,
    String? phone,
  }) async {
    final data = await client.request('POST', 'customer/register', body: {
      'name': name.trim(),
      'email': email.trim().toLowerCase(),
      'password': password,
      if (phone != null && phone.trim().isNotEmpty) 'phone': phone.trim(),
    });
    final user = User.fromJson(Map<String, dynamic>.from(data['customer'] ?? data['user'] ?? data));
    final token = data['token']?.toString() ?? '';
    client.token = token;
    return (user, token);
  }
}
