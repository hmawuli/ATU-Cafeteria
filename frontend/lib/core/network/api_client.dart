import 'dart:convert';
import 'dart:io';

import 'package:atu_cafeteria/core/config/app_config.dart';

class ApiException implements Exception {
  final int statusCode;
  final String message;
  final Map<String, dynamic> errors;
  const ApiException(this.statusCode, this.message, [this.errors = const {}]);
  @override String toString() => message;
}

class ApiClient {
  final HttpClient _client;
  String? token;
  ApiClient({HttpClient? client}) : _client = client ?? HttpClient();

  Future<dynamic> request(String method, String path, {Map<String, dynamic>? body}) async {
    final uri = Uri.parse('${AppConfig.normalizedApiBaseUrl}$path');
    final request = await _client.openUrl(method, uri);
    request.headers.contentType = ContentType.json;
    request.headers.set(HttpHeaders.acceptHeader, 'application/json');
    if (token != null && token!.isNotEmpty) request.headers.set(HttpHeaders.authorizationHeader, 'Bearer $token');
    if (body != null) request.write(jsonEncode(body));
    final response = await request.close();
    final text = await response.transform(utf8.decoder).join();
    dynamic decoded;
    if (text.isNotEmpty) { try { decoded = jsonDecode(text); } catch (_) { decoded = text; } }
    if (response.statusCode < 200 || response.statusCode >= 300) {
      final map = decoded is Map ? Map<String, dynamic>.from(decoded) : <String, dynamic>{};
      throw ApiException(response.statusCode, map['message']?.toString() ?? 'Request failed.', Map<String, dynamic>.from(map['errors'] ?? {}));
    }
    return decoded;
  }

  void close() => _client.close(force: true);
}
