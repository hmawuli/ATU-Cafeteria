import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:atu_cafeteria/core/network/api_client.dart';
import 'package:atu_cafeteria/domain/models/models.dart';
import 'package:atu_cafeteria/presentation/providers/orders_state.dart';

void main() {
  group('OrdersState (focused store)', () {
    test('refreshVendorOrders parses the order list and notifies', () async {
      final client = MockClient((request) async {
        expect(request.url.path, '/api/vendor/my-orders');
        return http.Response(
          jsonEncode([
            {'id': 1, 'status': 'READY', 'total_price': 35.0},
            {'id': 2, 'status': 'PREPARING', 'total_price': 20.0},
          ]),
          200,
          headers: {'content-type': 'application/json'},
        );
      });

      final state = OrdersState();
      var notified = 0;
      state.addListener(() => notified++);

      final orders =
          await state.refreshVendorOrders(ApiClient(client: client), 't');

      expect(orders, hasLength(2));
      expect(orders.first.id, 1);
      expect(state.vendorOrders, hasLength(2));
      expect(notified, greaterThan(0));
    });

    test('completePickup returns the message and refreshes orders', () async {
      final client = MockClient((request) async {
        if (request.url.path.endsWith('/verify-pickup')) {
          return http.Response(
            jsonEncode({'success': true, 'message': 'Pickup verified.'}),
            200,
            headers: {'content-type': 'application/json'},
          );
        }
        // Follow-up refresh of vendor orders.
        return http.Response(jsonEncode([]), 200,
            headers: {'content-type': 'application/json'});
      });

      final state = OrdersState();

      final message = await state.completePickup(
        ApiClient(client: client),
        't',
        orderId: 7,
        pin: '1234',
      );

      expect(message, 'Pickup verified.');
    });

    test('clear resets both lists', () {
      final state = OrdersState();
      state.setVendorOrders([Order.fromJson({
        'id': 1,
        'vendorId': 2,
        'name': 'Jollof',
        'price': 35.0,
        'category': 'Dishes',
      })]);
      state.setCustomerOrders([Order.fromJson({
        'id': 3,
        'vendorId': 2,
        'name': 'Waakye',
        'price': 30.0,
        'category': 'Dishes',
      })]);

      state.clear();

      expect(state.vendorOrders, isEmpty);
      expect(state.customerOrders, isEmpty);
    });
  });
}