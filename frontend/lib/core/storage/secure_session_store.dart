import 'package:flutter_secure_storage/flutter_secure_storage.dart';

class SecureSessionStore {
  static const _tokenKey = 'atu_auth_token';
  static const _usernameKey = 'atu_auth_username';
  static const _savedAtKey = 'atu_auth_saved_at';
  static const _storage = FlutterSecureStorage();

  const SecureSessionStore._();
  static Future<void> save(
      {required String token, required String username}) async {
    await _storage.write(key: _tokenKey, value: token);
    await _storage.write(key: _usernameKey, value: username);
    await _storage.write(
        key: _savedAtKey, value: DateTime.now().toUtc().toIso8601String());
  }

  static Future<String?> token() => _storage.read(key: _tokenKey);
  static Future<String?> username() => _storage.read(key: _usernameKey);
  static Future<void> clear() async {
    await _storage.delete(key: _tokenKey);
    await _storage.delete(key: _usernameKey);
    await _storage.delete(key: _savedAtKey);
  }
}
