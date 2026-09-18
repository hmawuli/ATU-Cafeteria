/// Runtime configuration for the Flutter client.
///
/// The provider builds API routes by appending /api/... to the backend root.
/// Therefore this value must be the backend root, not the /api/ path.
///
/// Web/local development defaults to the Laravel server on this machine.
/// For an Android emulator use:
/// flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8000
/// For a physical device use your computer's LAN address instead.
class AppConfig {
  static const String apiBaseUrl = String.fromEnvironment(
    'API_BASE_URL',
    defaultValue: 'http://127.0.0.1:8000',
  );

  static String get normalizedApiBaseUrl {
    final value = apiBaseUrl.trim();
    return value.endsWith('/')
        ? value.substring(0, value.length - 1)
        : value;
  }

  static String get backendBaseUrl => normalizedApiBaseUrl;

  const AppConfig._();
}
