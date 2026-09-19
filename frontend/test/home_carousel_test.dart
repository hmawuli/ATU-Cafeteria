import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:atu_cafeteria/presentation/widgets/home_carousel.dart';

const _slides = [
  HomeSlide(
    badge: 'Slide One',
    title: 'First slide title',
    subtitle: 'First subtitle',
    imageUrl: 'https://example.com/one.jpg',
    ctaLabel: 'Go one',
    onCta: _noop,
  ),
  HomeSlide(
    badge: 'Slide Two',
    title: 'Second slide title',
    subtitle: 'Second subtitle',
    imageUrl: 'https://example.com/two.jpg',
    ctaLabel: 'Go two',
    onCta: _noop,
  ),
  HomeSlide(
    badge: 'Slide Three',
    title: 'Third slide title',
    subtitle: 'Third subtitle',
    imageUrl: 'https://example.com/three.jpg',
    ctaLabel: 'Go three',
    onCta: _noop,
  ),
];

void _noop() {}

Widget _wrap({HomeCarousel? carousel}) => MaterialApp(
      home: Scaffold(
        body: Center(
          child: SizedBox(
            width: 360,
            child: carousel ??
                const HomeCarousel(slides: _slides, autoPlay: false),
          ),
        ),
      ),
    );

void main() {
  testWidgets('renders all three slides with three pagination dots',
      (tester) async {
    await tester.pumpWidget(_wrap());

    expect(find.text('First slide title'), findsOneWidget);
    // One AnimatedContainer per dot.
    expect(find.byType(AnimatedContainer), findsNWidgets(3));
  });

  testWidgets('auto-play advances to the next slide', (tester) async {
    await tester.pumpWidget(_wrap(
      carousel: const HomeCarousel(slides: _slides, autoPlay: true),
    ));

    expect(find.text('First slide title'), findsOneWidget);

    // Trigger the periodic timer, then let the page animation complete.
    await tester.pump(const Duration(seconds: 4));
    await tester.pump(const Duration(milliseconds: 450));

    expect(find.text('Second slide title'), findsOneWidget);
  });

  testWidgets('swiping left moves to the next slide', (tester) async {
    await tester.pumpWidget(
        _wrap(carousel: const HomeCarousel(slides: _slides, autoPlay: false)));

    await tester.drag(find.byType(PageView), const Offset(-360, 0));
    await tester.pumpAndSettle();

    expect(find.text('Second slide title'), findsOneWidget);
  });

  testWidgets('tapping a dot jumps to that slide', (tester) async {
    await tester.pumpWidget(
        _wrap(carousel: const HomeCarousel(slides: _slides, autoPlay: false)));

    // Third dot → third slide.
    await tester.tap(find.byType(AnimatedContainer).at(2));
    await tester.pumpAndSettle();

    expect(find.text('Third slide title'), findsOneWidget);
  });

  testWidgets('CTA button fires its slide action', (tester) async {
    var tapped = false;
    final custom = [
      HomeSlide(
        badge: 'One',
        title: 'Single',
        subtitle: 'sub',
        imageUrl: 'https://example.com/one.jpg',
        ctaLabel: 'Tap me',
        onCta: () => tapped = true,
      ),
    ];
    await tester.pumpWidget(
        _wrap(carousel: HomeCarousel(slides: custom, autoPlay: false)));

    await tester.tap(find.text('Tap me'));
    expect(tapped, isTrue);
  });
}
