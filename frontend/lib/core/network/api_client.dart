import 'dart:convert';
import 'dart:io';

import 'package:atu_cafeteria/core/config/app_config.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

class ApiException implements Exception {
  final int statusCode;
  final String message;
  final Map<String, dynamic> errors;
  const ApiException(this.statusCode, this.message, [this.errors = const {}]);
  @override
  String toString() => message;
}

class ApiClient {
  final HttpClient _client;
  String? token;
  static const _tokenKey = 'atu_cafeteria_auth_token';
  static const FlutterSecureStorage _storage = FlutterSecureStorage();
  ApiClient({HttpClient? client}) : _client = client ?? HttpClient() {
    _client.connectionTimeout = const Duration(seconds: 12);
  }

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
    if (token == null || token!.isEmpty) {
      token = await _storage.read(key: _tokenKey);
    }
    final uri = Uri.parse('${AppConfig.normalizedApiBaseUrl}$path');
    final request = await _client.openUrl(method, uri);
    request.headers.contentType = ContentType.json;
    request.headers.set(HttpHeaders.acceptHeader, 'application/json');
    if (token != null && token!.isNotEmpty) {
      request.headers.set(HttpHeaders.authorizationHeader, 'Bearer $token');
    }
    if (body != null) request.write(jsonEncode(body));
    final response = await request.close().timeout(const Duration(seconds: 25));
    final text = await response.transform(utf8.decoder).join();
    dynamic decoded;
    if (text.isNotEmpty) {
      try {
        decoded = jsonDecode(text);
      } catch (_) {
        decoded = text;
      }
    }
    if (response.statusCode < 200 || response.statusCode >= 300) {
      final map = decoded is Map
          ? Map<String, dynamic>.from(decoded)
          : <String, dynamic>{};
      throw ApiException(
          response.statusCode,
          map['message']?.toString() ?? 'Request failed.',
          Map<String, dynamic>.from(map['errors'] ?? {}));
    }
    return decoded;
  }

  Future<dynamic> get(String path) => request('GET', path);
  Future<dynamic> post(String path, {Map<String, dynamic>? body}) =>
      request('POST', path, body: body);

  void close() => _client.close(force: true);
}
