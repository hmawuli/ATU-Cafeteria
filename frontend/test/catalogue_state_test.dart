import 'package:flutter_test/flutter_test.dart';
import 'package:atu_cafeteria/presentation/providers/catalogue_state.dart';
import 'package:atu_cafeteria/domain/models/models.dart';

void main() {
  group('CatalogueState (focused store)', () {
    test('setAllFoodItems stores and notifies', () {
      final state = CatalogueState();
      var notified = 0;
      state.addListener(() => notified++);

      state.setAllFoodItems([
        const FoodItem(
            id: 1,
            vendorId: 20,
            name: 'Jollof',
            price: 30,
            category: 'Dishes',
            description: '',
            imageUrl: ''),
      ]);

      expect(state.allFoodItems, hasLength(1));
      expect(state.allFoodItems.first.name, 'Jollof');
      expect(notified, greaterThan(0));
    });

    test('setVendorFoodItems and clear behave', () {
      final state = CatalogueState();
      state.setVendorFoodItems([
        const FoodItem(
            id: 2,
            vendorId: 20,
            name: 'Waakye',
            price: 25,
            category: 'Dishes',
            description: '',
            imageUrl: ''),
      ]);
      expect(state.vendorFoodItems, hasLength(1));

      state.clear();
      expect(state.allFoodItems, isEmpty);
      expect(state.vendorFoodItems, isEmpty);
    });
  });
}