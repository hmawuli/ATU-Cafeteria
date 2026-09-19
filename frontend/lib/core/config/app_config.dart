/// Runtime configuration for the Flutter client.
///
/// The backend URL is resolved from the most specific override to the most
/// generic default, so a physical phone never needs the IP retyped for every
/// launch:
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
///   * USB:  run `scripts/connect_phone.sh` (uses `adb reverse` — no IP
///           needed, works over the cable regardless of Wi-Fi changes), or
///   * Wi-Fi: set [staticApiHost] to your computer's LAN IP
///            (`hostname -I`) once, and start the backend with
///            `scripts/serve_backend.sh` so it binds 0.0.0.0.
/// Android also requires cleartext HTTP for local development — see
/// `frontend/android/app/src/main/AndroidManifest.xml`.
///
/// Emulator note: an Android emulator reaches the host machine at
/// `http://10.0.2.2:8001` — pass that via `--dart-define` for emulators only.
class AppConfig {
  static const String _dartDefineUrl = String.fromEnvironment('API_BASE_URL');

  /// The ONLY value you should need to edit by hand: your computer's LAN IP
  /// (for example `'192.168.1.50'`) when running on a physical phone over
  /// Wi-Fi. Leave this empty (`''`) when using USB (`adb reverse`), an
  /// emulator, or web/desktop.
  static const String staticApiHost = '';

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
