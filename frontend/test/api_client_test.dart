import 'package:flutter_test/flutter_test.dart';
import 'package:atu_cafeteria/core/config/app_config.dart';

void main() {
  test('API base URL is normalized (no trailing slash)', () {
    expect(AppConfig.normalizedApiBaseUrl.endsWith('/'), isFalse);
  });

  test('backendBaseUrl equals the normalized base', () {
    expect(AppConfig.backendBaseUrl, AppConfig.normalizedApiBaseUrl);
  });
}
