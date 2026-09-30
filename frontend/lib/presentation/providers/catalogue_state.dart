import 'package:flutter/foundation.dart';

import 'package:atu_cafeteria/domain/models/models.dart';

/// Focused state for the live catalogue and the vendor's own menu.
///
/// Extracted from the monolith `CafeteriaProvider` (third split after Orders
/// and Wallet). I/O stays in the provider; the data + notifications live here.
class CatalogueState extends ChangeNotifier {
  List<FoodItem> _allFoodItems = [];

  List<FoodItem> get allFoodItems => _allFoodItems;

  List<FoodItem> _vendorFoodItems = [];

  List<FoodItem> get vendorFoodItems => _vendorFoodItems;

  void setAllFoodItems(List<FoodItem> items) {
    _allFoodItems = List<FoodItem>.from(items);
    notifyListeners();
  }

  void setVendorFoodItems(List<FoodItem> items) {
    _vendorFoodItems = List<FoodItem>.from(items);
    notifyListeners();
  }

  void clear() {
    _allFoodItems = [];
    _vendorFoodItems = [];
    notifyListeners();
  }
}