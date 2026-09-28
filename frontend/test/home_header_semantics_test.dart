import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:atu_cafeteria/presentation/widgets/home_header.dart';

void main() {
  Widget wrap(HomeHeader header) =>
      MaterialApp(home: Scaffold(body: header));

  testWidgets('home header exposes a labelled logo to screen readers',
      (tester) async {
    await tester.pumpWidget(wrap(
      HomeHeader(
        cartCount: 0,
        signedIn: false,
        onCart: () {},
        onSignIn: () {},
      ),
    ));

    expect(find.bySemanticsLabel('ATU Cafeteria logo'), findsOneWidget);
  });

  testWidgets('cart and notifications remain tappable with tooltips',
      (tester) async {
    await tester.pumpWidget(wrap(
      HomeHeader(
        cartCount: 3,
        signedIn: true,
        onCart: () {},
        onBell: () {},
        onProfile: () {},
        onSignIn: () {},
      ),
    ));

    expect(find.byTooltip('View cart'), findsOneWidget);
    expect(find.byTooltip('Notifications'), findsOneWidget);
  });
}