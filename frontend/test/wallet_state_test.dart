import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:atu_cafeteria/core/network/api_client.dart';
import 'package:atu_cafeteria/presentation/providers/wallet_state.dart';

void main() {
  group('WalletState (focused store)', () {
    test('refresh parses balance and ledger from GET /api/wallet', () async {
      final client = MockClient((request) async {
        expect(request.url.path, '/api/wallet');
        return http.Response(
          jsonEncode({
            'success': true,
            'balance': 42.5,
            'transactions': [
              {'id': 1, 'type': 'PAYMENT'},
              {'id': 2, 'type': 'DEPOSIT'},
            ],
          }),
          200,
          headers: {'content-type': 'application/json'},
        );
      });

      final state = WalletState();
      var notified = 0;
      state.addListener(() => notified++);

      await state.refresh(ApiClient(client: client), 't');

      expect(state.balance, 42.5);
      expect(state.transactions, hasLength(2));
      expect(notified, greaterThan(0));
    });

    test('applyLocalCredit mirrors a server-verified credit', () {
      final state = WalletState()..setBalance(10);
      state.applyLocalCredit(25);

      expect(state.balance, 35);
    });
  });
}