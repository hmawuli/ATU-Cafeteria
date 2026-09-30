import 'package:flutter_test/flutter_test.dart';
import 'package:atu_cafeteria/core/money.dart';

void main() {
  group('money helpers', () {
    test('isUnderfunded is float-safe', () {
      expect(isUnderfunded(0, 15), isTrue);
      expect(isUnderfunded(15, 15), isFalse);
      expect(isUnderfunded(15.000000001, 15), isFalse);
      expect(isUnderfunded(20, 15), isFalse);
    });

    test('fundingShortage returns the exact missing amount', () {
      expect(fundingShortage(0, 15), 15);
      expect(fundingShortage(7.5, 15), 7.5);
      expect(fundingShortage(20, 15), 0);
    });
  });
}