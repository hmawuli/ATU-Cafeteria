import 'package:flutter_test/flutter_test.dart';
import 'package:atu_cafeteria/presentation/providers/cart_provider.dart';
import 'package:atu_cafeteria/domain/models/models.dart';

FoodItem meal(int id, {bool available = true}) => FoodItem(
      id: id,
      vendorId: 20,
      name: 'Jollof #$id',
      price: 30.0,
      category: 'Dishes',
      description: '',
      imageUrl: '',
      isAvailable: available,
    );

void main() {
  group('CartProvider', () {
    test('add builds lines and counts quantity', () {
      final cart = CartProvider();
      cart.add(meal(1));
      cart.add(meal(1));
      cart.add(meal(2));

      expect(cart.itemCount, 3);
      expect(cart.lines, hasLength(2));
      expect(cart.subtotal, closeTo(90.0, 0.001));
    });

    test('ignores items with no id or unavailable meals', () {
      final cart = CartProvider();
      const noId = FoodItem(
        id: null,
        vendorId: 20,
        name: 'No id',
        price: 5.0,
        category: 'Dishes',
        description: '',
        imageUrl: '',
      );
      cart.add(noId);
      cart.add(meal(3, available: false));

      expect(cart.isEmpty, isTrue);
    });

    test('checkout payload sends menu_item_id (production checkout schema)', () {
      final cart = CartProvider();
      cart.add(meal(101));
      cart.add(meal(101));
      cart.add(meal(202));

      final payload = cart.toCheckoutPayload();

      expect(payload, hasLength(2));
      expect(payload.first['menu_item_id'], 101);
      expect(payload.first['quantity'], 2);
      expect(payload.last['menu_item_id'], 202);
      // Must NOT send the legacy food_item_id for menu items.
      expect(payload.first.containsKey('food_item_id'), isFalse);
    });

    test('setQuantity and remove behave', () {
      final cart = CartProvider();
      cart.add(meal(1));
      cart.add(meal(1));
      cart.setQuantity(1, 5);
      expect(cart.itemCount, 5);

      cart.remove(1);
      expect(cart.itemCount, 4);

      cart.setQuantity(1, 0);
      expect(cart.isEmpty, isTrue);
    });
  });
}