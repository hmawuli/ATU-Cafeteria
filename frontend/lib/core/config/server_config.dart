import 'package:flutter/foundation.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'app_config.dart';

/// Runtime-overridable backend base URL.
///
/// Priority:
///   1. Manual override set from the app's "API server" settings
///      (persisted on the device) — e.g. a deployed Railway URL.
///   2. [AppConfig] default (loopback `http://127.0.0.1:8001` for USB).
///
/// This lets the same installed APK talk to a local backend over USB or to a
/// deployed server without rebuilding, by entering the address once in the app.
class ServerConfig {
  ServerConfig._();

  static const _storage = FlutterSecureStorage();
  static const _key = 'atu_server_base_url';
  static String? _manual;

  /// Active backend base URL with no trailing slash.
  static String get baseUrl {
    final manual = _manual?.trim() ?? '';
    final value = manual.isNotEmpty ? manual : AppConfig.backendBaseUrl;
    final normalized = value.endsWith('/')
        ? value.substring(0, value.length - 1)
        : value;

    if (kReleaseMode) {
      final uri = Uri.tryParse(normalized);
      if (uri == null || uri.scheme != 'https') {
        throw StateError(
          'Production builds require an HTTPS backend endpoint.',
        );
      }
    }

    return normalized;
  }

  /// URL shown in the settings dialog (the manual override is kept verbatim
  /// so the user can read it back; active value is [baseUrl]).
  static String get overrideUrl => _manual?.trim() ?? '';

  /// Whether a manual override is currently active.
  static bool get hasOverride => (_manual?.trim().isNotEmpty ?? false);

  /// Load the persisted override. Call once at startup before `runApp`.
  static Future<void> init() async {
    try {
      _manual = await _storage.read(key: _key);
    } catch (_) {
      _manual = null;
    }
  }

  /// Set (or clear, with `null`/empty) the manual override and persist it.
  static Future<void> setOverride(String? url) async {
    final value = url?.trim() ?? '';
    _manual = value.isEmpty ? null : value;
    try {
      if (_manual == null) {
        await _storage.delete(key: _key);
      } else {
        await _storage.write(key: _key, value: _manual);
      }
    } catch (_) {
      // Best-effort persistence; in-memory override still applies this run.
    }
  }
}