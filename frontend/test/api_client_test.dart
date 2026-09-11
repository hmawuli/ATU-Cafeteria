import 'package:flutter_test/flutter_test.dart';
import 'package:atu_cafeteria/core/config/app_config.dart';

void main() {
  test('API base URL is normalized', () {
    expect(AppConfig.normalizedApiBaseUrl.endsWith('/'), isTrue);
  });
}
