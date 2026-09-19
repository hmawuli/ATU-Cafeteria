/// Runtime configuration for the Flutter client.
///
/// The backend URL is resolved from the most specific override to the most
/// generic default, so a physical phone can connect without requiring a
/// dart-define on every launch:
///
///   1. `--dart-define=API_BASE_URL=...`   (highest priority, per-launch)
///   2. `staticApiHost`                    (phone → laptop LAN IP, set once)
///   3. `http://127.0.0.1:8001`            (web / desktop / USB `adb reverse`)
///
/// The provider builds API routes by appending /api/... to the backend root,
/// therefore this value must be the backend root, not the /api/ path.
///
/// Physical phone note: inside the app `127.0.0.1` means the phone itself,
/// never your computer. Use one of two modes:
///   * USB: run `scripts/connect_phone.sh` (uses `adb reverse`), or
///   * Wi-Fi: [staticApiHost] points to your computer's LAN IP and the backend
///     must bind to 0.0.0.0.
///
/// Android also requires cleartext HTTP for local development — see
/// `frontend/android/app/src/main/AndroidManifest.xml`.
///
/// Emulator note: an Android emulator reaches the host machine at
/// `http://10.0.2.2:8001` — pass that via `--dart-define` for emulators only.
class AppConfig {
  static const String _dartDefineUrl = String.fromEnvironment('API_BASE_URL');

  /// Computer LAN IP used by the physical Android phone over Wi-Fi.
  ///
  /// This is currently the laptop address reported by `hostname -I`.
  /// A per-launch `--dart-define=API_BASE_URL=...` still takes priority.
  static const String staticApiHost = '172.20.10.3';

  static const String _port = '8001';
  static const String _loopbackDefault = 'http://127.0.0.1:8001';

  static String get apiBaseUrl {
    if (_dartDefineUrl.trim().isNotEmpty) return _dartDefineUrl.trim();
    if (staticApiHost.trim().isNotEmpty) {
      return 'http://${staticApiHost.trim()}:$_port';
    }
    return _loopbackDefault;
  }

  static String get normalizedApiBaseUrl {
    final value = apiBaseUrl.trim();
    return value.endsWith('/') ? value.substring(0, value.length - 1) : value;
  }

  static String get backendBaseUrl => normalizedApiBaseUrl;

  const AppConfig._();
}
