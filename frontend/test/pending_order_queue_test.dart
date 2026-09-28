import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:atu_cafeteria/core/network/api_client.dart';
import 'package:atu_cafeteria/services/pending_order_queue.dart';

class FakePendingOrderStore implements PendingOrderStore {
  final List<Map<String, dynamic>> rows = [];

  @override
  Future<List<Map<String, dynamic>>> allPendingOrders() async =>
      List<Map<String, dynamic>>.from(rows);

  @override
  Future<int> insertPendingOrder({
    required String idempotencyKey,
    required String payload,
    required int createdAt,
  }) async {
    rows.add({
      'idempotency_key': idempotencyKey,
      'payload': payload,
      'created_at': createdAt,
    });
    return rows.length;
  }

  @override
  Future<void> deletePendingOrderByKey(String idempotencyKey) async {
    rows.removeWhere((r) => r['idempotency_key'] == idempotencyKey);
  }
}

Map<String, dynamic> orderPayload() => {
      'vendor_id': 20,
      'menu_item_id': 3,
      'food_name': 'Jollof Rice',
      'quantity': 2,
      'unit_price': 35.0,
      'total_price': 70.0,
    };

void main() {
  group('PendingOrderQueue', () {
    test('enqueues an order and reports the pending count', () async {
      final store = FakePendingOrderStore();
      final queue = PendingOrderQueue(store);

      await queue.enqueue(
        payload: orderPayload(),
        idempotencyKey: 'key-1',
      );

      expect(await queue.pendingCount(), 1);
    });

    test('flush delivers success orders and removes them', () async {
      final store = FakePendingOrderStore();
      final queue = PendingOrderQueue(store);
      await queue.enqueue(payload: orderPayload(), idempotencyKey: 'key-ok');

      final api = ApiClient(
        client: MockClient((request) async {
          expect(request.headers['Idempotency-Key'], 'key-ok');
          return http.Response(
            jsonEncode({'success': true, 'order': {'id': 1}}),
            201,
            headers: {'content-type': 'application/json'},
          );
        }),
      );

      expect(await queue.flush(api, 'token'), 1);
      expect(await queue.pendingCount(), 0);
    });

    test('flush drops a 4xx validation error permanently', () async {
      final store = FakePendingOrderStore();
      final queue = PendingOrderQueue(store);
      await queue.enqueue(payload: orderPayload(), idempotencyKey: 'key-422');

      final api = ApiClient(
        client: MockClient((request) async {
          return http.Response(
            jsonEncode({'message': 'Invalid order.'}),
            422,
            headers: {'content-type': 'application/json'},
          );
        }),
      );

      expect(await queue.flush(api, 'token'), 0);
      expect(await queue.pendingCount(), 0);
    });

    test('flush keeps a 5xx so it can retry later', () async {
      final store = FakePendingOrderStore();
      final queue = PendingOrderQueue(store);
      await queue.enqueue(payload: orderPayload(), idempotencyKey: 'key-500');

      final api = ApiClient(
        client: MockClient((request) async {
          return http.Response('Server Error', 500);
        }),
      );

      expect(await queue.flush(api, 'token'), 0);
      expect(await queue.pendingCount(), 1);
    });

    test('flush keeps an order after a network failure', () async {
      final store = FakePendingOrderStore();
      final queue = PendingOrderQueue(store);
      await queue.enqueue(payload: orderPayload(), idempotencyKey: 'key-net');

      final api = ApiClient(
        client: MockClient((request) async {
          throw http.ClientException('connection refused');
        }),
      );

      expect(await queue.flush(api, 'token'), 0);
      expect(await queue.pendingCount(), 1);
    });
  });
}