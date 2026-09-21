import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:atu_cafeteria/core/config/server_config.dart';

class ApiException implements Exception {
  final int statusCode;
  final String message;
  final Map<String, dynamic> errors;
  const ApiException(this.statusCode, this.message, [this.errors = const {}]);
  @override
  String toString() => message;
}

class ApiClient {
  final http.Client _client;
  String? token;

  // Keep this key identical to SecureSessionStore so login sessions are
  // available to every API request, including checkout.
  static const _tokenKey = 'atu_auth_token';
  static const FlutterSecureStorage _storage = FlutterSecureStorage();

  ApiClient({http.Client? client}) : _client = client ?? http.Client();

  Future<void> setToken(String value) async {
    token = value;
    await _storage.write(key: _tokenKey, value: value);
  }

  Future<void> loadToken() async {
    token = await _storage.read(key: _tokenKey);
  }

  Future<void> clearToken() async {
    token = null;
    await _storage.delete(key: _tokenKey);
  }

  Future<dynamic> request(String method, String path,
      {Map<String, dynamic>? body}) async {
    // The token is session scoped. Read secure storage on every request so a
    // single shared ApiClient can never serve a stale token after a logout /
    // login as another user. The in-memory [token] is only a fallback (e.g.
    // unit tests that inject a client without secure storage).
    final storedToken = await _storage.read(key: _tokenKey);
    final effectiveToken =
        (storedToken != null && storedToken.isNotEmpty) ? storedToken : token;

    final uri = Uri.parse(
        '${ServerConfig.baseUrl}/api/${path.replaceFirst(RegExp(r'^/'), '')}');
    final headers = <String, String>{
      'Accept': 'application/json',
      'Content-Type': 'application/json',
    };
    if (effectiveToken != null && effectiveToken.isNotEmpty) {
      headers['Authorization'] = 'Bearer $effectiveToken';
    }

    late http.Response response;
    final encodedBody = body == null ? null : jsonEncode(body);
    switch (method.toUpperCase()) {
      case 'GET':
        response = await _client
            .get(uri, headers: headers)
            .timeout(const Duration(seconds: 25));
        break;
      case 'POST':
        response = await _client
            .post(uri, headers: headers, body: encodedBody)
            .timeout(const Duration(seconds: 25));
        break;
      case 'PUT':
        response = await _client
            .put(uri, headers: headers, body: encodedBody)
            .timeout(const Duration(seconds: 25));
        break;
      case 'PATCH':
        response = await _client
            .patch(uri, headers: headers, body: encodedBody)
            .timeout(const Duration(seconds: 25));
        break;
      case 'DELETE':
        response = await _client
            .delete(uri, headers: headers, body: encodedBody)
            .timeout(const Duration(seconds: 25));
        break;
      default:
        throw ArgumentError('Unsupported HTTP method: $method');
    }

    dynamic decoded;
    if (response.body.isNotEmpty) {
      try {
        decoded = jsonDecode(response.body);
      } catch (_) {
        decoded = response.body;
      }
    }

    if (response.statusCode < 200 || response.statusCode >= 300) {
      final map = decoded is Map
          ? Map<String, dynamic>.from(decoded)
          : <String, dynamic>{};
      final rawErrors = map['errors'];
      final errors = rawErrors is Map
          ? Map<String, dynamic>.from(rawErrors)
          : <String, dynamic>{};
      throw ApiException(
        response.statusCode,
        map['message']?.toString() ?? 'Request failed. Please try again.',
        errors,
      );
    }
    return decoded;
  }

  Future<dynamic> get(String path) => request('GET', path);
  Future<dynamic> post(String path, {Map<String, dynamic>? body}) =>
      request('POST', path, body: body);
  Future<dynamic> put(String path, {Map<String, dynamic>? body}) =>
      request('PUT', path, body: body);
  Future<dynamic> patch(String path, {Map<String, dynamic>? body}) =>
      request('PATCH', path, body: body);
  Future<dynamic> delete(String path, {Map<String, dynamic>? body}) =>
      request('DELETE', path, body: body);

  void close() => _client.close();
}
