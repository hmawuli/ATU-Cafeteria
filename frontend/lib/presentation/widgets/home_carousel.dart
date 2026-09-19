import 'dart:async';
import 'package:flutter/material.dart';
import 'package:atu_cafeteria/core/theme/app_theme.dart';

/// A single promotional card shown inside [HomeCarousel].
class HomeSlide {
  const HomeSlide({
    required this.badge,
    required this.title,
    required this.subtitle,
    required this.imageUrl,
    required this.ctaLabel,
    required this.onCta,
    this.secondaryLabel,
    this.onSecondary,
  });

  final String badge;
  final String title;
  final String subtitle;
  final String imageUrl;
  final String ctaLabel;
  final VoidCallback onCta;
  final String? secondaryLabel;
  final VoidCallback? onSecondary;
}

/// Mobile-first, horizontally scrollable promo carousel.
///
/// Swipeable [PageView] (one slide per page) with pagination dots, optional
/// auto-advance that pauses while the user is dragging, and tappable dots.
/// Bounded height so it never takes over the screen: give it a fixed height
/// via [height] and it lays out safely inside any scrollable parent.
class HomeCarousel extends StatefulWidget {
  const HomeCarousel({
    super.key,
    required this.slides,
    this.height = 188,
    this.autoPlay = true,
    this.interval = const Duration(seconds: 4),
  });

  final List<HomeSlide> slides;
  final double height;
  final bool autoPlay;
  final Duration interval;

  @override
  State<HomeCarousel> createState() => _HomeCarouselState();
}

class _HomeCarouselState extends State<HomeCarousel> {
  late final PageController _controller;
  Timer? _timer;
  int _current = 0;
  bool _paused = false;

  @override
  void initState() {
    super.initState();
    _controller = PageController();
    if (widget.autoPlay && widget.slides.length > 1) _startTimer();
  }

  @override
  void dispose() {
    _timer?.cancel();
    _controller.dispose();
    super.dispose();
  }

  void _startTimer() {
    _timer?.cancel();
    _timer = Timer.periodic(widget.interval, (_) => _advance());
  }

  void _advance() {
    if (_paused || widget.slides.length < 2 || !_controller.hasClients) return;
    final next = (_current + 1) % widget.slides.length;
    _controller.animateToPage(
      next,
      duration: const Duration(milliseconds: 400),
      curve: Curves.easeInOut,
    );
  }

  void _pause() {
    if (!_paused) {
      _paused = true;
      _timer?.cancel();
    }
  }

  void _resume() {
    if (_paused) {
      _paused = false;
      if (widget.autoPlay && widget.slides.length > 1) _startTimer();
    }
  }

  void _goTo(int index) {
    _current = index;
    _controller.animateToPage(
      index,
      duration: const Duration(milliseconds: 400),
      curve: Curves.easeInOut,
    );
  }

  @override
  Widget build(BuildContext context) {
    if (widget.slides.isEmpty) return const SizedBox.shrink();

    return SizedBox(
      height: widget.height,
      width: double.infinity,
      child: Stack(
        children: [
          Positioned.fill(
            child: ClipRRect(
              borderRadius: BorderRadius.circular(18),
              child: Listener(
                onPointerDown: (_) => _pause(),
                onPointerUp: (_) => _resume(),
                onPointerCancel: (_) => _resume(),
                child: PageView.builder(
                  controller: _controller,
                  onPageChanged: (index) => setState(() => _current = index),
                  itemCount: widget.slides.length,
                  itemBuilder: (_, index) => _SlideCard(
                    slide: widget.slides[index],
                  ),
                ),
              ),
            ),
          ),
          if (widget.slides.length > 1)
            Positioned(
              left: 0,
              right: 0,
              bottom: 10,
              child: Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  for (var i = 0; i < widget.slides.length; i++)
                    Padding(
                      padding: const EdgeInsets.symmetric(horizontal: 3),
                      child: GestureDetector(
                        onTap: () => _goTo(i),
                        child: AnimatedContainer(
                          duration: const Duration(milliseconds: 250),
                          curve: Curves.easeOut,
                          width: i == _current ? 18 : 6,
                          height: 6,
                          decoration: BoxDecoration(
                            color: i == _current
                                ? AppTheme.accent
                                : Colors.white.withValues(alpha: 0.55),
                            borderRadius: BorderRadius.circular(3),
                          ),
                        ),
                      ),
                    ),
                ],
              ),
            ),
        ],
      ),
    );
  }
}

class _SlideCard extends StatelessWidget {
  const _SlideCard({required this.slide});

  final HomeSlide slide;

  @override
  Widget build(BuildContext context) {
    return DecoratedBox(
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(18),
        image: DecorationImage(
          image: NetworkImage(slide.imageUrl),
          fit: BoxFit.cover,
          onError: (_, __) {},
        ),
      ),
      child: DecoratedBox(
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(18),
          gradient: const LinearGradient(
            begin: Alignment.centerLeft,
            end: Alignment.centerRight,
            colors: [
              Color(0xEB052B63),
              Color(0x8C052B63),
              Colors.transparent,
            ],
          ),
        ),
        child: Padding(
          padding: const EdgeInsets.fromLTRB(18, 14, 18, 18),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Container(
                padding:
                    const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                decoration: BoxDecoration(
                  color: Colors.white.withValues(alpha: 0.18),
                  borderRadius: BorderRadius.circular(20),
                  border: Border.all(
                    color: Colors.white.withValues(alpha: 0.35),
                  ),
                ),
                child: Text(
                  slide.badge.toUpperCase(),
                  style: const TextStyle(
                    color: Colors.white,
                    fontSize: 10,
                    fontWeight: FontWeight.w800,
                    letterSpacing: 0.8,
                  ),
                ),
              ),
              const SizedBox(height: 10),
              Text(
                slide.title,
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
                style: const TextStyle(
                  color: Colors.white,
                  fontSize: 21,
                  height: 1.1,
                  fontWeight: FontWeight.w900,
                ),
              ),
              const SizedBox(height: 5),
              Text(
                slide.subtitle,
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
                style: TextStyle(
                  color: Colors.white.withValues(alpha: 0.85),
                  fontSize: 12,
                  height: 1.35,
                ),
              ),
              const Spacer(),
              Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  FilledButton(
                    onPressed: slide.onCta,
                    style: FilledButton.styleFrom(
                      backgroundColor: AppTheme.accent,
                      foregroundColor: AppTheme.primaryDark,
                      minimumSize: Size.zero,
                      padding: const EdgeInsets.symmetric(
                        horizontal: 16,
                        vertical: 9,
                      ),
                      shape: const StadiumBorder(),
                      textStyle: const TextStyle(
                        fontWeight: FontWeight.w800,
                        fontSize: 12,
                      ),
                    ),
                    child: Text(slide.ctaLabel),
                  ),
                  if (slide.secondaryLabel != null &&
                      slide.onSecondary != null) ...[
                    const SizedBox(width: 8),
                    OutlinedButton(
                      onPressed: slide.onSecondary,
                      style: OutlinedButton.styleFrom(
                        foregroundColor: Colors.white,
                        minimumSize: Size.zero,
                        padding: const EdgeInsets.symmetric(
                          horizontal: 14,
                          vertical: 9,
                        ),
                        side: BorderSide(
                          color: Colors.white.withValues(alpha: 0.7),
                        ),
                        shape: const StadiumBorder(),
                        textStyle: const TextStyle(
                          fontWeight: FontWeight.w700,
                          fontSize: 12,
                        ),
                      ),
                      child: Text(slide.secondaryLabel!),
                    ),
                  ],
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}
