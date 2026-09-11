/// Runtime configuration for the Flutter client.
///
/// Override the API endpoint without editing source code:
/// flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8000/api/
class AppConfig {
  static const String apiBaseUrl = String.fromEnvironment(
    'API_BASE_URL',
    defaultValue: 'http://10.0.2.2:8000/api/',
  );

  static String get normalizedApiBaseUrl {
    final value = apiBaseUrl.trim();
    return value.endsWith('/') ? value : '$value/';
  }

  static String get backendBaseUrl =>
      normalizedApiBaseUrl.replaceFirst(RegExp(r'/api(?:/v1)?/?$'), '');

  const AppConfig._();
}
