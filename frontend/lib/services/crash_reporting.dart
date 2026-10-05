import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_crashlytics/firebase_crashlytics.dart';
import 'package:flutter/foundation.dart';

/// Firebase Crashlytics integration.
///
/// Firebase values are supplied with `--dart-define` in production (identical
/// to FCM in [PushNotificationService]); nothing sensitive is committed. When
/// the app has not been configured for Firebase yet, crash reporting is
/// skipped without breaking startup.
class CrashReporting {
  CrashReporting._();

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

  static bool get isConfigured =>
      !kIsWeb &&
      _apiKey.isNotEmpty &&
      _appId.isNotEmpty &&
      _messagingSenderId.isNotEmpty &&
      _projectId.isNotEmpty;

  /// Initialise crash reporting and install global error handlers.
  ///
  /// Call once, after `WidgetsFlutterBinding.ensureInitialized()` and before
  /// `runApp`. Never blocks startup — any failure is swallowed.
  static Future<void> init() async {
    if (!isConfigured) {
      debugPrint('CrashReporting: Firebase not configured; skipping.');
      return;
    }

    try {
      if (Firebase.apps.isEmpty) {
        await Firebase.initializeApp(
          options: const FirebaseOptions(
            apiKey: _apiKey,
            appId: _appId,
            messagingSenderId: _messagingSenderId,
            projectId: _projectId,
          ),
        );
      }

      await FirebaseCrashlytics.instance
          .setCrashlyticsCollectionEnabled(kReleaseMode);

      FlutterError.onError = (details) {
        FirebaseCrashlytics.instance.recordFlutterFatalError(details);
        FlutterError.presentError(details);
      };

      PlatformDispatcher.instance.onError = (error, stack) {
        FirebaseCrashlytics.instance.recordError(error, stack, fatal: true);

        return true;
      };
    } catch (e, s) {
      debugPrint('CrashReporting: failed to initialise: $e\n$s');
    }
  }

  /// Record a non-fatal error that was handled (e.g. a best-effort network
  /// call that is allowed to fail) so it still surfaces in the console instead
  /// of being silently discarded. Never throws, even when unconfigured.
  static Future<void> recordNonFatal(
    Object error,
    StackTrace stack, {
    String? reason,
  }) async {
    if (!isConfigured) {
      debugPrint('CrashReporting: non-fatal${reason == null ? '' : ' ($reason)'}: $error');
      return;
    }

    try {
      await FirebaseCrashlytics.instance.recordError(
        error,
        stack,
        reason: reason,
        fatal: false,
      );
    } catch (_) {
      // Telemetry must never break the app.
    }
  }
}