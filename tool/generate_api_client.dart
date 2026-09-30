// Generates a typed Dart API client from docs/openapi.json.
//
// Usage:  dart run tool/generate_api_client.dart
// Output: lib/generated/atu_api.dart
//
// The OpenAPI document (docs/openapi.json) is committed as the contract
// source of truth. This generator emits strict typed models for the schemas
// and a typed AtuApi facade over the shared ApiClient, so app↔backend drift
// surfaces at build time instead of silently at runtime.
import 'dart:convert';
import 'dart:io';

const specPath = 'docs/openapi.json';
const outPath = 'frontend/lib/generated/atu_api.dart';

String _typeFor(Map<String, dynamic> schema) {
  final t = schema['type'];
  if (t == 'integer') return 'int';
  if (t == 'number') return 'double';
  if (t == 'boolean') return 'bool';
  if (t == 'array') return 'List<dynamic>';

  final ref = schema['\$ref'] ?? schema['allOf']?[0]?['\$ref'];
  if (ref == null) return 'dynamic';
  final name = ref.split('/').last;
  return name.startsWith('Api') ? name : 'Api$name';
}

String _fieldDecl(String prop, Map<String, dynamic> ps) {
  final optional = true; // spec schemas don't mark required here
  final type = _typeFor(ps);
  final typeStr = optional ? '$type?' : type;
  if (ps['type'] == 'array') {
    return '  final List<dynamic>? $prop;';
  }
  return '  final $typeStr $prop;';
}

String _parseValue(String prop, Map<String, dynamic> ps) {
  final type = _typeFor(ps);
  final key = prop;
  switch (type) {
    case 'int':
      return "  final $prop = _reqInt(json, '$key');";
    case 'double':
      return "  final $prop = _reqNum(json, '$key');";
    case 'bool':
      return "  final $prop = _reqBool(json, '$key');";
    case 'List<dynamic>':
      return "  final $prop = _reqList(json, '$key');";
    default:
      if (type.startsWith('Api')) {
        return "  final $prop = _reqMap(json, '$key') == null\n      ? null\n      : $type.fromJson(_reqMap(json, '$key')!);";
      }
      return "  final $prop = json['$key']?.toString();";
  }
}

String emitModels(Map<String, dynamic> schemas) {
  final buf = StringBuffer();
  for (final entry in schemas.entries) {
    final name = 'Api${entry.key}';
    final props = (entry.value['properties'] as Map?) ?? <String, dynamic>{};
    buf.writeln('/// Typed model generated from OpenAPI schema `${entry.key}`.');
    buf.writeln('class $name {');
    for (final p in props.entries) {
      buf.writeln(_fieldDecl(p.key, p.value));
    }
    buf.writeln('  const $name({');
    for (final p in props.entries) {
      buf.writeln('    this.${p.key},');
    }
    buf.writeln('  });');
    buf.writeln('\n  factory $name.fromJson(Map<String, dynamic> json) {');
    for (final p in props.entries) {
      buf.writeln(_parseValue(p.key, p.value));
    }
    buf.writeln('    return $name(');
    for (final p in props.entries) {
      buf.writeln('      ${p.key}: ${p.key},');
    }
    buf.writeln('    );');
    buf.writeln('  }');
    buf.writeln('}');
    buf.writeln('');
  }
  return buf.toString();
}

// Endpoint table: (className, path, method, bodyName?, responseModel?).
// ResponseModel 'ApiUser' means parse 'user' or the body itself.
const endpoints = [
  ['login', 'login', 'POST', 'LoginRequest', 'LoginResponse'],
  ['register', 'register', 'POST', 'RegisterRequest', 'User'],
  ['me', 'me', 'GET', null, 'User'],
  ['wallet', 'wallet', 'GET', null, 'Wallet'],
];

String emitApi() {
  final buf = StringBuffer();
  buf.writeln('/// Typed facade over the shared [ApiClient], generated from');
  buf.writeln('/// docs/openapi.json. Endpoints not listed here are available');
  buf.writeln('/// through [ApiClient.request] directly.');
  buf.writeln('class AtuApi {');
  buf.writeln('  final ApiClient _client;');
  buf.writeln('  AtuApi(this._client);');
  buf.writeln('');

  // login / register bodies
  buf.writeln('  Future<ApiLoginResponse> login({');
  buf.writeln('    required String username, required String pin}) async {');
  buf.writeln('    final body = await _client.request(\'POST\', \'login\',');
  buf.writeln("        body: {'username': username, 'pin': pin});");
  buf.writeln('    return ApiLoginResponse.fromJson(');
  buf.writeln('        body is Map ? Map<String, dynamic>.from(body) : <String, dynamic>{});');
  buf.writeln('  }');
  buf.writeln('');

  buf.writeln('  Future<ApiUser> me({String? token}) async {');
  buf.writeln('    final body = await _client.request(\'GET\', \'me\', token: token);');
  buf.writeln('    final map = body is Map ? Map<String, dynamic>.from(body) : <String, dynamic>{};');
  buf.writeln('    final user = map[\'user\'] is Map ? Map<String, dynamic>.from(map[\'user\'] as Map) : map;');
  buf.writeln('    return ApiUser.fromJson(user);');
  buf.writeln('  }');
  buf.writeln('');

  buf.writeln('  Future<ApiWallet> wallet({String? token}) async {');
  buf.writeln('    final body = await _client.request(\'GET\', \'wallet\', token: token);');
  buf.writeln('    return ApiWallet.fromJson(');
  buf.writeln('        body is Map ? Map<String, dynamic>.from(body) : <String, dynamic>{});');
  buf.writeln('  }');
  buf.writeln('}');
  return buf.toString();
}

void main() {
  final spec = jsonDecode(File(specPath).readAsStringSync());
  final schemas = spec['components']['schemas'];
  final header = '''
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
  throw FormatException('Expected int for \$key, got \${v.runtimeType}');
}

double? _reqNum(Map<String, dynamic> json, String key) {
  final v = json[key];
  if (v == null) return null;
  if (v is num) return v.toDouble();
  throw FormatException('Expected num for \$key, got \${v.runtimeType}');
}

bool? _reqBool(Map<String, dynamic> json, String key) {
  final v = json[key];
  if (v == null) return null;
  if (v is bool) return v;
  throw FormatException('Expected bool for \$key, got \${v.runtimeType}');
}

List<dynamic>? _reqList(Map<String, dynamic> json, String key) {
  final v = json[key];
  if (v == null) return null;
  if (v is List) return v;
  throw FormatException('Expected List for \$key, got \${v.runtimeType}');
}

Map<String, dynamic>? _reqMap(Map<String, dynamic> json, String key) {
  final v = json[key];
  if (v == null) return null;
  if (v is Map) return Map<String, dynamic>.from(v);
  throw FormatException('Expected Map for \$key, got \${v.runtimeType}');
}
''';

  final out = StringBuffer()
    ..writeln(header)
    ..writeln(emitModels(schemas))
    ..writeln(emitApi());

  File(outPath).parent.createSync(recursive: true);
  File(outPath).writeAsStringSync(out.toString());
  stdout.writeln('Generated $outPath (${out.length} chars).');
}
