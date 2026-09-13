/// Runtime configuration for the Flutter client.
///
/// Web/local development defaults to the Laravel server on this machine.
/// For an Android emulator use:
/// flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8001/api/
/// For a physical device use your computer's LAN address instead.
class AppConfig {
  static const String apiBaseUrl = String.fromEnvironment(
    'API_BASE_URL',
    defaultValue: 'http://127.0.0.1:8001/api/',
  );

  static String get normalizedApiBaseUrl {
    final value = apiBaseUrl.trim();
    return value.endsWith('/') ? value : '$value/';
  }

  static String get backendBaseUrl =>
      normalizedApiBaseUrl.replaceFirst(RegExp(r'/api(?:/v1)?/?$'), '');

  const AppConfig._();
}
