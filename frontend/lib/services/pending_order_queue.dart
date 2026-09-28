import 'dart:convert';

import 'package:atu_cafeteria/core/network/api_client.dart';
import 'package:atu_cafeteria/data/local/db_helper.dart';

/// Storage seam for the pending-order queue so the flush/reconcile logic can
/// be unit tested without a database engine.
abstract class PendingOrderStore {
  Future<List<Map<String, dynamic>>> allPendingOrders();

  Future<int> insertPendingOrder({
    required String idempotencyKey,
    required String payload,
    required int createdAt,
  });

  Future<void> deletePendingOrderByKey(String idempotencyKey);
}

/// Default store backed by [DbHelper] (sqflite).
class DbPendingOrderStore implements PendingOrderStore {
  final DbHelper db;

  DbPendingOrderStore(this.db);

  @override
  Future<List<Map<String, dynamic>>> allPendingOrders() =>
      db.allPendingOrders();

  @override
  Future<int> insertPendingOrder({
    required String idempotencyKey,
    required String payload,
    required int createdAt,
  }) =>
      db.insertPendingOrder(
        idempotencyKey: idempotencyKey,
        payload: payload,
        createdAt: createdAt,
      );

  @override
  Future<void> deletePendingOrderByKey(String idempotencyKey) =>
      db.deletePendingOrderByKey(idempotencyKey);
}

/// Offline order queue.
///
/// Orders composed while offline are persisted locally with their idempotency
/// key. [flush] delivers them through `POST /api/customer/orders` — which is
/// idempotency-protected — so a retry after reconnecting can never create a
/// duplicate charge server-side.
class PendingOrderQueue {
  final PendingOrderStore store;

  PendingOrderQueue(this.store);

  /// Persist an order payload for later delivery. Callers generate the
  /// idempotency key (e.g. [ApiClient.newIdempotencyKey]).
  Future<void> enqueue({
    required Map<String, dynamic> payload,
    required String idempotencyKey,
  }) {
    return store.insertPendingOrder(
      idempotencyKey: idempotencyKey,
      payload: jsonEncode(payload),
      createdAt: DateTime.now().millisecondsSinceEpoch,
    );
  }

  Future<int> pendingCount() async =>
      (await store.allPendingOrders()).length;

  /// Attempt delivery of every queued order.
  ///
  /// * 2xx/3xx → delivered and removed from the queue.
  /// * 4xx (validation) → dropped permanently (it cannot succeed later).
  /// * 5xx / 429 / network / timeout → kept for the next flush.
  ///
  /// Returns the number of orders delivered.
  Future<int> flush(ApiClient api, String? token) async {
    final rows = await store.allPendingOrders();
    var delivered = 0;

    for (final row in rows) {
      final key = row['idempotency_key']?.toString() ?? '';
      if (key.isEmpty) continue;

      final rawPayload = row['payload'];
      dynamic payload;
      if (rawPayload is String) {
        try {
          payload = jsonDecode(rawPayload);
        } catch (_) {
          payload = null;
        }
      }

      if (payload is! Map) {
        await store.deletePendingOrderByKey(key);
        continue;
      }

      try {
        await api.request(
          'POST',
          'customer/orders',
          body: Map<String, dynamic>.from(payload),
          idempotencyKey: key,
          token: token,
        );
        await store.deletePendingOrderByKey(key);
        delivered++;
      } on ApiException catch (e) {
        if (e.statusCode >= 400 && e.statusCode < 500) {
          // Permanent client error — dropping prevents a wedged queue.
          await store.deletePendingOrderByKey(key);
        }
      } catch (_) {
        // Transient failure — keep for the next flush.
      }
    }

    return delivered;
  }
}