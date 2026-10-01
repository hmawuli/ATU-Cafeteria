import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:atu_cafeteria/main.dart' as app;

void main() {
  testWidgets('unregistered navigation shows the friendly NotFound page',
      (tester) async {
    await tester.pumpWidget(
      MaterialApp(
        onUnknownRoute: app.appUnknownRoute,
        home: Builder(
          builder: (context) => Scaffold(
            body: Center(
              child: ElevatedButton(
                onPressed: () =>
                    Navigator.of(context).pushNamed('/does-not-exist'),
                child: const Text('go'),
              ),
            ),
          ),
        ),
      ),
    );

    await tester.tap(find.text('go'));
    await tester.pumpAndSettle();

    expect(find.text('Page not found'), findsOneWidget);
    expect(find.text('Return home'), findsOneWidget);
  });
}