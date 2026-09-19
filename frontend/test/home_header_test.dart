import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:atu_cafeteria/presentation/widgets/home_header.dart';

void main() {
  Widget wrap({required HomeHeader header}) =>
      MaterialApp(home: Scaffold(body: header));

  testWidgets('signed-out header shows wordmark, cart and Sign in pill',
      (tester) async {
    await tester.pumpWidget(wrap(
      header: HomeHeader(
        cartCount: 2,
        signedIn: false,
        onCart: () {},
        onSignIn: () {},
      ),
    ));

    expect(find.text('ATU CAFETERIA'), findsOneWidget);
    expect(find.text('Sign in'), findsOneWidget);
    expect(find.byIcon(Icons.shopping_cart_outlined), findsOneWidget);
    expect(find.byIcon(Icons.notifications_none_rounded), findsNothing);
  });

  testWidgets('signed-in header shows bell and avatar, hides Sign in',
      (tester) async {
    await tester.pumpWidget(wrap(
      header: HomeHeader(
        cartCount: 0,
        signedIn: true,
        profileInitials: 'DK',
        onCart: () {},
        onSignIn: () {},
      ),
    ));

    expect(find.text('Sign in'), findsNothing);
    expect(find.byIcon(Icons.notifications_none_rounded), findsOneWidget);
    expect(find.text('DK'), findsOneWidget);
  });

  testWidgets('cart badge hidden when empty, visible with a count',
      (tester) async {
    await tester.pumpWidget(wrap(
      header: HomeHeader(
        cartCount: 0,
        signedIn: false,
        onCart: () {},
        onSignIn: () {},
      ),
    ));
    expect(find.text('0'), findsNothing);

    await tester.pumpWidget(wrap(
      header: HomeHeader(
        cartCount: 3,
        signedIn: false,
        onCart: () {},
        onSignIn: () {},
      ),
    ));
    expect(find.text('3'), findsOneWidget);
  });

  testWidgets('cart tap fires callback', (tester) async {
    var tapped = false;
    await tester.pumpWidget(wrap(
      header: HomeHeader(
        cartCount: 1,
        signedIn: false,
        onCart: () => tapped = true,
        onSignIn: () {},
      ),
    ));

    await tester.tap(find.byIcon(Icons.shopping_cart_outlined));
    expect(tapped, isTrue);
  });
}
