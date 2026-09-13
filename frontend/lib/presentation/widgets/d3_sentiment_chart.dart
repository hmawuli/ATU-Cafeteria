import 'dart:math' as math;
import 'package:flutter/material.dart';
import 'package:atu_cafeteria/domain/models/models.dart' as models;

/// Lightweight native Flutter sentiment chart.
///
/// This intentionally avoids WebView + D3.js so the Android application uses
/// less RAM, fewer native processes and no JavaScript engine for analytics.
class D3SentimentChart extends StatelessWidget {
  final List<models.Feedback> feedbackList;

  const D3SentimentChart({super.key, required this.feedbackList});

  @override
  Widget build(BuildContext context) {
    int positive = 0;
    int neutral = 0;
    int negative = 0;

    for (final feedback in feedbackList) {
      final average = (feedback.ratingFoodQuality +
              feedback.ratingCleanliness +
              feedback.ratingServiceSpeed +
              feedback.ratingPriceValue) /
          4.0;
      if (average >= 4.0) {
        positive++;
      } else if (average >= 3.0) {
        neutral++;
      } else {
        negative++;
      }
    }

    // Keep the existing presentation-friendly fallback when there is no data.
    if (feedbackList.isEmpty) {
      positive = 28;
      neutral = 12;
      negative = 5;
    }

    final total = positive + neutral + negative;

    return Container(
      height: 250,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: Colors.grey.withValues(alpha: 0.2)),
      ),
      child: Row(
        children: [
          Expanded(
            child: CustomPaint(
              painter: _SentimentPainter(
                positive: positive,
                neutral: neutral,
                negative: negative,
              ),
              child: const SizedBox.expand(),
            ),
          ),
          const SizedBox(width: 16),
          SizedBox(
            width: 120,
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text('FEEDBACK',
                    style: TextStyle(
                        fontSize: 11,
                        fontWeight: FontWeight.w700,
                        color: Colors.blueGrey)),
                const SizedBox(height: 4),
                Text('$total total',
                    style: const TextStyle(
                        fontSize: 18, fontWeight: FontWeight.w800)),
                const SizedBox(height: 16),
                _Legend('Positive', positive, Colors.green),
                _Legend('Neutral', neutral, Colors.amber.shade700),
                _Legend('Negative', negative, Colors.red),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _Legend extends StatelessWidget {
  final String label;
  final int count;
  final Color color;

  const _Legend(this.label, this.count, this.color);

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: Row(
        children: [
          Container(
              width: 10,
              height: 10,
              decoration: BoxDecoration(color: color, shape: BoxShape.circle)),
          const SizedBox(width: 8),
          Expanded(
              child: Text('$label ($count)',
                  style: const TextStyle(fontSize: 11))),
        ],
      ),
    );
  }
}

class _SentimentPainter extends CustomPainter {
  final int positive;
  final int neutral;
  final int negative;

  _SentimentPainter(
      {required this.positive, required this.neutral, required this.negative});

  @override
  void paint(Canvas canvas, Size size) {
    final values = [positive, neutral, negative];
    final colors = [Colors.green, Colors.amber.shade700, Colors.red];
    final total = values.fold<int>(0, (sum, value) => sum + value);
    if (total == 0) return;

    final center = Offset(size.width / 2, size.height / 2);
    final radius = math.min(size.width, size.height) / 2 - 8;
    final stroke = radius * 0.28;
    final rect = Rect.fromCircle(center: center, radius: radius);
    final paint = Paint()
      ..style = PaintingStyle.stroke
      ..strokeWidth = stroke;

    var start = -math.pi / 2;
    for (var i = 0; i < values.length; i++) {
      final sweep = values[i] / total * math.pi * 2;
      paint.color = colors[i];
      canvas.drawArc(rect, start, sweep, false, paint);
      start += sweep;
    }
  }

  @override
  bool shouldRepaint(covariant _SentimentPainter oldDelegate) =>
      oldDelegate.positive != positive ||
      oldDelegate.neutral != neutral ||
      oldDelegate.negative != negative;
}
