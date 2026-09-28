import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:atu_cafeteria/core/network/api_client.dart';
import 'package:atu_cafeteria/generated/atu_api.dart';

void main() {
  group('AtuApi (generated from OpenAPI)', () {
    test('me() returns a typed user from the server envelope', () async {
      final client = MockClient((request) async {
        return http.Response(
          jsonEncode({
            'success': true,
            'user': {
              'id': 7,
              'username': 'ama',
              'role': 'STUDENT',
              'fullName': 'Ama Mensah',
              'balance': 25.5,
              'loyalty_points': 120,
              'is_open': false,
            },
          }),
          200,
          headers: {'content-type': 'application/json'},
        );
      });

      final api = AtuApi(ApiClient(client: client));
      final user = await api.me(token: 't');

      expect(user.id, 7);
      expect(user.balance, 25.5);
      expect(user.is_open, isFalse);
    });

    test('wallet() returns balance and transaction ledger', () async {
      final client = MockClient((request) async {
        return http.Response(
          jsonEncode({
            'success': true,
            'balance': 40.0,
            'transactions': [
              {'id': 1, 'type': 'PAYMENT'},
              {'id': 2, 'type': 'DEPOSIT'},
            ],
          }),
          200,
          headers: {'content-type': 'application/json'},
        );
      });

      final api = AtuApi(ApiClient(client: client));
      final wallet = await api.wallet(token: 't');

      expect(wallet.balance, 40.0);
      expect(wallet.transactions, hasLength(2));
    });

    test('strict models reject a type mismatch', () {
      expect(
        () => ApiUser.fromJson({
          'id': 'not-an-int',
          'username': 'x',
        }),
        throwsA(isA<FormatException>()),
      );
    });

    test('login response parses token and user', () {
      final resp = ApiLoginResponse.fromJson({
        'success': true,
        'token': 'abc',
        'requires_2fa': false,
        'user': {'id': 3, 'username': 'student'},
      });

      expect(resp.token, 'abc');
      expect(resp.user?.id, 3);
      expect(resp.requires_2fa, isFalse);
    });
  });
}