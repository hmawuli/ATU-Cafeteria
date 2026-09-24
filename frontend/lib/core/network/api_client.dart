import 'dart:convert';
import 'dart:math';
import 'package:flutter/foundation.dart';
import 'package:http/http.dart' as http;
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:atu_cafeteria/core/config/server_config.dart';
import 'package:atu_cafeteria/core/network/infinityfree_challenge_solver.dart';

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

  /// Send an API request.
  ///
  /// Critical financial/order mutations can provide an Idempotency-Key. The
  /// same key may safely be retried after a timeout without creating a second
  /// server-side transaction. Existing callers remain fully compatible when
  /// the key is omitted.
  Future<dynamic> request(String method, String path,
      {Map<String, dynamic>? body, String? idempotencyKey}) async {
    final storedToken = await _storage.read(key: _tokenKey);
    final effectiveToken =
        (storedToken != null && storedToken.isNotEmpty) ? storedToken : token;

    final uri = Uri.parse(
        '${ServerConfig.baseUrl}/api/${path.replaceFirst(RegExp(r'^/'), '')}');

    if (kReleaseMode && uri.scheme != 'https') {
      throw const ApiException(
        0,
        'Production builds require a secure HTTPS API endpoint.',
      );
    }
    final headers = <String, String>{
      'Accept': 'application/json',
      'Content-Type': 'application/json',
    };
    if (effectiveToken != null && effectiveToken.isNotEmpty) {
      headers['Authorization'] = 'Bearer $effectiveToken';
    }
    if (idempotencyKey != null && idempotencyKey.trim().isNotEmpty) {
      headers['Idempotency-Key'] = idempotencyKey.trim();
    }

    final encodedBody = body == null ? null : jsonEncode(body);
    final response =
        await _performWithBrowserCheck(method, uri, headers, encodedBody);

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

  /// Creates a cryptographically strong-enough client request key for a
  /// single user action. Store and reuse the returned value when retrying the
  /// exact same operation; do not generate a new key for a retry.
  static String newIdempotencyKey() {
    final random = Random.secure();
    final bytes = List<int>.generate(24, (_) => random.nextInt(256));
    return base64UrlEncode(bytes).replaceAll('=', '');
  }

  Future<http.Response> _performWithBrowserCheck(
      String method, Uri uri, Map<String, String> headers, String? encodedBody) {
    Future<http.Response> perform() {
      final requestHeaders = Map<String, String>.from(headers);
      final cached = InfinityFreeChallengeSolver.cookies;
      if (cached != null) requestHeaders['Cookie'] = '__test=$cached';
      return _send(method, uri, requestHeaders, encodedBody);
    }

    return perform().then((response) {
      final contentType = response.headers['content-type'] ?? '';
      if (!InfinityFreeChallengeSolver.isChallenge(contentType, response.body)) {
        return response;
      }
      final cookie = InfinityFreeChallengeSolver.solveFromHtml(response.body);
      if (cookie == null) return response;
      return perform();
    });
  }

  Future<http.Response> _send(
      String method, Uri uri, Map<String, String> headers, String? encodedBody) {
    switch (method.toUpperCase()) {
      case 'GET':
        return _client
            .get(uri, headers: headers)
            .timeout(const Duration(seconds: 25));
      case 'POST':
        return _client
            .post(uri, headers: headers, body: encodedBody)
            .timeout(const Duration(seconds: 25));
      case 'PUT':
        return _client
            .put(uri, headers: headers, body: encodedBody)
            .timeout(const Duration(seconds: 25));
      case 'PATCH':
        return _client
            .patch(uri, headers: headers, body: encodedBody)
            .timeout(const Duration(seconds: 25));
      case 'DELETE':
        return _client
            .delete(uri, headers: headers, body: encodedBody)
            .timeout(const Duration(seconds: 25));
      default:
        throw ArgumentError('Unsupported HTTP method: $method');
    }
  }

  Future<dynamic> get(String path) => request('GET', path);
  Future<dynamic> post(String path,
          {Map<String, dynamic>? body, String? idempotencyKey}) =>
      request('POST', path, body: body, idempotencyKey: idempotencyKey);
  Future<dynamic> put(String path,
          {Map<String, dynamic>? body, String? idempotencyKey}) =>
      request('PUT', path, body: body, idempotencyKey: idempotencyKey);
  Future<dynamic> patch(String path,
          {Map<String, dynamic>? body, String? idempotencyKey}) =>
      request('PATCH', path, body: body, idempotencyKey: idempotencyKey);
  Future<dynamic> delete(String path,
          {Map<String, dynamic>? body, String? idempotencyKey}) =>
      request('DELETE', path, body: body, idempotencyKey: idempotencyKey);

  void close() => _client.close();
}
