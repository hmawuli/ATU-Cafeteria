import 'dart:convert';
import 'dart:io';
import 'dart:math';

import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

import '../core/network/api_client.dart';

/// Lightweight FCM registration for authenticated customers.
///
/// Firebase values are supplied with --dart-define in production. Nothing
/// sensitive is committed to the repository. When the app has not been
/// configured for FCM yet, registration is skipped without breaking login.
class PushNotificationService {
  PushNotificationService._();

  static const FlutterSecureStorage _storage = FlutterSecureStorage();
  static const String _deviceIdKey = 'atu_device_id';
  static const String _appVersion =
      String.fromEnvironment('ATU_APP_VERSION', defaultValue: '1.1.0');

  static const String _apiKey =
      String.fromEnvironment('ATU_FIREBASE_API_KEY', defaultValue: '');
  static const String _appId =
      String.fromEnvironment('ATU_FIREBASE_APP_ID', defaultValue: '');
  static const String _messagingSenderId = String.fromEnvironment(
    'ATU_FIREBASE_MESSAGING_SENDER_ID',
    defaultValue: '',
  );
  static const String _projectId =
      String.fromEnvironment('ATU_FIREBASE_PROJECT_ID', defaultValue: '');
  static const String _storageBucket =
      String.fromEnvironment('ATU_FIREBASE_STORAGE_BUCKET', defaultValue: '');

  static bool get isConfigured =>
      _apiKey.isNotEmpty &&
      _appId.isNotEmpty &&
      _messagingSenderId.isNotEmpty &&
      _projectId.isNotEmpty;

  static bool _listenerAttached = false;
  static bool _tokenRefreshAttached = false;

  static Future<void> syncRegisteredDevice() async {
    if (kIsWeb || (!Platform.isAndroid && !Platform.isIOS) || !isConfigured) {
      return;
    }

    try {
      if (Firebase.apps.isEmpty) {
        await Firebase.initializeApp(
          options: FirebaseOptions(
            apiKey: _apiKey,
            appId: _appId,
            messagingSenderId: _messagingSenderId,
            projectId: _projectId,
            storageBucket: _storageBucket.isEmpty ? null : _storageBucket,
          ),
        );
      }

      final messaging = FirebaseMessaging.instance;
      final settings = await messaging.requestPermission(
        alert: true,
        badge: true,
        sound: true,
        provisional: false,
      );

      if (settings.authorizationStatus == AuthorizationStatus.denied) {
        return;
      }

      final token = await messaging.getToken();
      if (token == null || token.isEmpty) return;

      final deviceId = await _deviceId();
      final platform = Platform.isIOS ? 'ios' : 'android';

      final api = ApiClient();
      try {
        await api.post(
          '/customer/devices',
          body: {
            'device_id': deviceId,
            'platform': platform,
            'push_token': token,
            'app_version': _appVersion,
          },
        );
      } finally {
        api.close();
      }

      if (!_listenerAttached) {
        _listenerAttached = true;
        FirebaseMessaging.onMessage.listen((message) {
          debugPrint(
            'ATU Cafeteria push: ' +
                (message.notification?.title ?? 'Notification'),
          );
        });
      }

      if (!_tokenRefreshAttached) {
        _tokenRefreshAttached = true;
        messaging.onTokenRefresh.listen((token) async {
          try {
            final deviceId = await _deviceId();
            final api = ApiClient();
            try {
              await api.post(
                '/customer/devices',
                body: {
                  'device_id': deviceId,
                  'platform': Platform.isIOS ? 'ios' : 'android',
                  'push_token': token,
                  'app_version': _appVersion,
                },
              );
            } finally {
              api.close();
            }
          } catch (e) {
            debugPrint('FCM token refresh sync skipped: ' + e.toString());
          }
        });
      }
    } catch (e) {
      debugPrint('FCM registration skipped: ' + e.toString());
    }
  }

  static Future<void> revokeRegisteredDevice() async {
    if (kIsWeb || (!Platform.isAndroid && !Platform.isIOS)) return;

    try {
      final existing = await _storage.read(key: _deviceIdKey);
      if (existing == null || existing.isEmpty) return;

      final api = ApiClient();
      try {
        final response = await api.get('/customer/devices');
        final raw = response is Map ? response['devices'] : response;
        if (raw is! List) return;

        for (final entry in raw.whereType<Map>()) {
          if (entry['device_id']?.toString() != existing) continue;
          final id = entry['id']?.toString();
          if (id == null || id.isEmpty) continue;
          await api.delete('/customer/devices/' + id);
          break;
        }
      } finally {
        api.close();
      }
    } catch (e) {
      debugPrint('FCM device revoke skipped: ' + e.toString());
    }
  }

  static Future<String> _deviceId() async {
    final existing = await _storage.read(key: _deviceIdKey);
    if (existing != null && existing.isNotEmpty) return existing;

    final random = Random.secure();
    final bytes = List<int>.generate(18, (_) => random.nextInt(256));
    final value = base64UrlEncode(bytes).replaceAll('=', '');

    await _storage.write(key: _deviceIdKey, value: value);
    return value;
  }
}