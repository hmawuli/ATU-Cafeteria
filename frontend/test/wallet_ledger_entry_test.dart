import 'package:flutter_test/flutter_test.dart';
import 'package:atu_cafeteria/domain/models/wallet_ledger_entry.dart';

void main() {
  group('WalletLedgerEntry', () {
    test('parses a credit DEPOSIT', () {
      final e = WalletLedgerEntry.fromJson({
        'id': 1,
        'type': 'DEPOSIT',
        'amount': 20.00,
        'reference': 'ATU-PAY-1',
        'created_at': '2026-10-01T08:00:00Z',
      });

      expect(e.isCredit, isTrue);
      expect(e.label, 'Top-up');
      expect(e.signedAmount, '+20.00');
      expect(e.day, '01-Oct');
    });

    test('parses a debit PAYMENT', () {
      final e = WalletLedgerEntry.fromJson({
        'type': 'PAYMENT',
        'amount': -15.50,
        'created_at': '2026-10-01T12:00:00Z',
      });

      expect(e.isCredit, isFalse);
      expect(e.signedAmount, '-15.50');
    });

    test('tolerates missing fields', () {
      final e = WalletLedgerEntry.fromJson({'type': null});

      expect(e.amount, 0);
      expect(e.day, '—');
      expect(e.label, 'UNKNOWN');
    });
  });
}