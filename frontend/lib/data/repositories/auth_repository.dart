import 'package:atu_cafeteria/core/network/api_client.dart';
import 'package:atu_cafeteria/domain/models/models.dart';

class AuthRepository {
  final ApiClient client;
  AuthRepository(this.client);

  Future<(User user, String token)> login(String username, String pin) async {
    final data = await client.request('POST', 'login', body: {
      'username': username.trim(),
      'pin': pin,
    });
    final user = User.fromJson(Map<String, dynamic>.from(data['user'] ?? data));
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
}
