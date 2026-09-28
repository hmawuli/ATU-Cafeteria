import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';

import 'package:atu_cafeteria/main.dart' as app;

/// On-device smoke suite.
///
/// Run against a physical phone / emulator with the backend reachable:
///   flutter test integration_test -d <device> \
///     --dart-define=API_BASE_URL=https://<host>/api/
void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  testWidgets('application shell boots on device', (tester) async {
    await tester.pumpWidget(const app.ATUCafeteriaApp());
    await tester.pump();

    expect(find.byType(MaterialApp), findsOneWidget);
  });
}