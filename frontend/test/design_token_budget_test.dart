import 'dart:io';

import 'package:flutter_test/flutter_test.dart';

/// Professional guard: the presentation layer must use the design tokens in
/// `core/theme/app_theme.dart` instead of hardcoded colours, so the UI stays
/// consistent and dark-mode-safe.
///
/// This is a *ratchet*: the budget is a ceiling that must only go down as
/// screens are migrated to [AppTheme]. If a change pushes it up, migrate the
/// new colour to a token instead of raising the number.
void main() {
  test('hardcoded presentation colours stay within the token budget', () {
    const budget = 82;

    final directory = Directory('lib/presentation');
    expect(directory.existsSync(), isTrue,
        reason: 'Run from the frontend package root (flutter test).');

    final pattern = RegExp(r'Color\(0x[0-9A-Fa-f]{6,8}\)');
    var count = 0;
    final offenders = <String, int>{};

    for (final entity in directory.listSync(recursive: true)) {
      if (entity is! File || !entity.path.endsWith('.dart')) continue;
      final hits = pattern.allMatches(entity.readAsStringSync()).length;
      if (hits > 0) {
        offenders[entity.path] = hits;
        count += hits;
      }
    }

    final worst = (offenders.entries.toList()
          ..sort((a, b) => b.value.compareTo(a.value)))
        .take(5)
        .map((e) => '${e.key}: ${e.value}')
        .join(', ');

    expect(
      count,
      lessThanOrEqualTo(budget),
      reason: 'Found $count hardcoded colours (budget $budget). '
          'Use AppTheme tokens instead. Worst files — $worst.',
    );
  });
}