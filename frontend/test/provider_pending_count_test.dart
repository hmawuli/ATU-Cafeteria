import 'package:flutter_test/flutter_test.dart';

import 'package:atu_cafeteria/presentation/providers/cafeteria_provider.dart';
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

void main() {
  group('CafeteriaProvider pending-order awareness', () {
    test('reports how many orders are queued for offline delivery', () async {
      final store = FakePendingOrderStore();
      final queue = PendingOrderQueue(store);
      final provider = CafeteriaProvider(orderQueue: queue);
      addTearDown(provider.dispose);

      expect(provider.pendingOrderCount, 0);

      await queue.enqueue(
        payload: {
          'vendor_id': 20,
          'menu_item_id': 3,
          'food_name': 'Jollof',
          'quantity': 1,
          'unit_price': 35.0,
          'total_price': 35.0,
        },
        idempotencyKey: 'queued-key',
      );

      await provider.refreshPendingOrderCount();

      expect(provider.pendingOrderCount, 1);
    });

    test('count drops back to zero after delivery', () async {
      final store = FakePendingOrderStore();
      final queue = PendingOrderQueue(store);
      final provider = CafeteriaProvider(orderQueue: queue);
      addTearDown(provider.dispose);

      await queue.enqueue(
        payload: {
          'vendor_id': 20,
          'menu_item_id': 3,
          'food_name': 'Waakye',
          'quantity': 1,
          'unit_price': 30.0,
          'total_price': 30.0,
        },
        idempotencyKey: 'queued-key-2',
      );
      await provider.refreshPendingOrderCount();
      expect(provider.pendingOrderCount, 1);

      await store.deletePendingOrderByKey('queued-key-2');
      await provider.refreshPendingOrderCount();
      expect(provider.pendingOrderCount, 0);
    });
  });
}