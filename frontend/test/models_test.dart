import 'package:flutter_test/flutter_test.dart';
import 'package:atu_cafeteria/domain/models/models.dart';

void main() {
  group('Domain models', () {
    test('User supports JSON and local-map representations', () {
      final user = User.fromJson({
        'id': 10,
        'username': 'vendor',
        'role': 'vendor',
        'full_name': 'Test Vendor',
        'info': 'Kitchen',
        'balance': 25.5,
        'is_open': true,
      });

      expect(user.id, 10);
      expect(user.role, 'VENDOR');
      expect(user.fullName, 'Test Vendor');
      expect(user.balance, 25.5);
      expect(user.isOpen, isTrue);
    });

    test('Order normalizes API status values', () {
      final order = Order.fromJson({
        'id': 42,
        'customer_id': 1,
        'vendor_id': 2,
        'food_item_id': 3,
        'food_name': 'Jollof',
        'quantity': 2,
        'unit_price': 20,
        'total_price': 40,
        'status': 'preparing',
        'pickup_pin': '1234',
      });

      expect(order.status, 'PREPARING');
      expect(order.totalPrice, 40);
      expect(order.displayStatus, 'Preparing');
    });
  });
}
