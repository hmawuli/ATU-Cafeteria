// GENERATED FILE — DO NOT EDIT BY HAND.
// Regenerate with: dart run tool/generate_api_client.dart
// Contract source: docs/openapi.json
//
// ignore_for_file: unnecessary_question_mark, non_constant_identifier_names

import 'package:atu_cafeteria/core/network/api_client.dart';

int? _reqInt(Map<String, dynamic> json, String key) {
  final v = json[key];
  if (v == null) return null;
  if (v is int) return v;
  if (v is num) return v.toInt();
  throw FormatException('Expected int for $key, got ${v.runtimeType}');
}

double? _reqNum(Map<String, dynamic> json, String key) {
  final v = json[key];
  if (v == null) return null;
  if (v is num) return v.toDouble();
  throw FormatException('Expected num for $key, got ${v.runtimeType}');
}

bool? _reqBool(Map<String, dynamic> json, String key) {
  final v = json[key];
  if (v == null) return null;
  if (v is bool) return v;
  throw FormatException('Expected bool for $key, got ${v.runtimeType}');
}

List<dynamic>? _reqList(Map<String, dynamic> json, String key) {
  final v = json[key];
  if (v == null) return null;
  if (v is List) return v;
  throw FormatException('Expected List for $key, got ${v.runtimeType}');
}

Map<String, dynamic>? _reqMap(Map<String, dynamic> json, String key) {
  final v = json[key];
  if (v == null) return null;
  if (v is Map) return Map<String, dynamic>.from(v);
  throw FormatException('Expected Map for $key, got ${v.runtimeType}');
}

/// Typed model generated from OpenAPI schema `LoginRequest`.
class ApiLoginRequest {
  final dynamic? username;
  final dynamic? pin;
  const ApiLoginRequest({
    this.username,
    this.pin,
  });

  factory ApiLoginRequest.fromJson(Map<String, dynamic> json) {
    final username = json['username']?.toString();
    final pin = json['pin']?.toString();
    return ApiLoginRequest(
      username: username,
      pin: pin,
    );
  }
}

/// Typed model generated from OpenAPI schema `RegisterRequest`.
class ApiRegisterRequest {
  final dynamic? email;
  final dynamic? password;
  final dynamic? role;
  final dynamic? fullName;
  const ApiRegisterRequest({
    this.email,
    this.password,
    this.role,
    this.fullName,
  });

  factory ApiRegisterRequest.fromJson(Map<String, dynamic> json) {
    final email = json['email']?.toString();
    final password = json['password']?.toString();
    final role = json['role']?.toString();
    final fullName = json['fullName']?.toString();
    return ApiRegisterRequest(
      email: email,
      password: password,
      role: role,
      fullName: fullName,
    );
  }
}

/// Typed model generated from OpenAPI schema `LoginResponse`.
class ApiLoginResponse {
  final bool? success;
  final dynamic? token;
  final ApiUser? user;
  final bool? requires_2fa;
  const ApiLoginResponse({
    this.success,
    this.token,
    this.user,
    this.requires_2fa,
  });

  factory ApiLoginResponse.fromJson(Map<String, dynamic> json) {
    final success = _reqBool(json, 'success');
    final token = json['token']?.toString();
    final user = _reqMap(json, 'user') == null
        ? null
        : ApiUser.fromJson(_reqMap(json, 'user')!);
    final requires_2fa = _reqBool(json, 'requires_2fa');
    return ApiLoginResponse(
      success: success,
      token: token,
      user: user,
      requires_2fa: requires_2fa,
    );
  }
}

/// Typed model generated from OpenAPI schema `User`.
class ApiUser {
  final int? id;
  final dynamic? username;
  final dynamic? role;
  final dynamic? fullName;
  final double? balance;
  final int? loyalty_points;
  final bool? is_open;
  const ApiUser({
    this.id,
    this.username,
    this.role,
    this.fullName,
    this.balance,
    this.loyalty_points,
    this.is_open,
  });

  factory ApiUser.fromJson(Map<String, dynamic> json) {
    final id = _reqInt(json, 'id');
    final username = json['username']?.toString();
    final role = json['role']?.toString();
    final fullName = json['fullName']?.toString();
    final balance = _reqNum(json, 'balance');
    final loyalty_points = _reqInt(json, 'loyalty_points');
    final is_open = _reqBool(json, 'is_open');
    return ApiUser(
      id: id,
      username: username,
      role: role,
      fullName: fullName,
      balance: balance,
      loyalty_points: loyalty_points,
      is_open: is_open,
    );
  }
}

/// Typed model generated from OpenAPI schema `Wallet`.
class ApiWallet {
  final bool? success;
  final double? balance;
  final List<dynamic>? transactions;
  const ApiWallet({
    this.success,
    this.balance,
    this.transactions,
  });

  factory ApiWallet.fromJson(Map<String, dynamic> json) {
    final success = _reqBool(json, 'success');
    final balance = _reqNum(json, 'balance');
    final transactions = _reqList(json, 'transactions');
    return ApiWallet(
      success: success,
      balance: balance,
      transactions: transactions,
    );
  }
}

/// Typed model generated from OpenAPI schema `Error`.
class ApiError {
  final bool? success;
  final dynamic? message;
  final dynamic? errors;
  const ApiError({
    this.success,
    this.message,
    this.errors,
  });

  factory ApiError.fromJson(Map<String, dynamic> json) {
    final success = _reqBool(json, 'success');
    final message = json['message']?.toString();
    final errors = json['errors']?.toString();
    return ApiError(
      success: success,
      message: message,
      errors: errors,
    );
  }
}

/// Typed facade over the shared [ApiClient], generated from
/// docs/openapi.json. Endpoints not listed here are available
/// through [ApiClient.request] directly.
class AtuApi {
  final ApiClient _client;
  AtuApi(this._client);

  Future<ApiLoginResponse> login(
      {required String username, required String pin}) async {
    final body = await _client
        .request('POST', 'login', body: {'username': username, 'pin': pin});
    return ApiLoginResponse.fromJson(
        body is Map ? Map<String, dynamic>.from(body) : <String, dynamic>{});
  }

  Future<ApiUser> me({String? token}) async {
    final body = await _client.request('GET', 'me', token: token);
    final map =
        body is Map ? Map<String, dynamic>.from(body) : <String, dynamic>{};
    final user = map['user'] is Map
        ? Map<String, dynamic>.from(map['user'] as Map)
        : map;
    return ApiUser.fromJson(user);
  }

  Future<ApiWallet> wallet({String? token}) async {
    final body = await _client.request('GET', 'wallet', token: token);
    return ApiWallet.fromJson(
        body is Map ? Map<String, dynamic>.from(body) : <String, dynamic>{});
  }
}
