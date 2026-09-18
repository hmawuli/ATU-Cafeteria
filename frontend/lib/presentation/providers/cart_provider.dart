import 'package:flutter/foundation.dart';
import '../../domain/models/models.dart';

class CartLine {
  final FoodItem item;
  int quantity;
  CartLine({required this.item, this.quantity = 1});
  double get total => item.price * quantity;
}

class CartProvider extends ChangeNotifier {
  final Map<int, CartLine> _lines = {};
  List<CartLine> get lines => _lines.values.toList(growable: false);
  int get itemCount =>
      _lines.values.fold(0, (sum, line) => sum + line.quantity);
  double get subtotal => _lines.values.fold(0, (sum, line) => sum + line.total);
  bool get isEmpty => _lines.isEmpty;
  List<Map<String, dynamic>> toCheckoutPayload() => _lines.values
      .map((line) => {'food_item_id': line.item.id, 'quantity': line.quantity})
      .toList();

  void add(FoodItem item) {
    final id = item.id;
    if (id == null || !item.isAvailable) return;
    final line = _lines[id];
    if (line == null) {
      _lines[id] = CartLine(item: item);
    } else {
      line.quantity++;
    }
    notifyListeners();
  }

  void remove(int id) {
    final line = _lines[id];
    if (line == null) return;
    if (line.quantity > 1) {
      line.quantity--;
    } else {
      _lines.remove(id);
    }
    notifyListeners();
  }

  void setQuantity(int id, int quantity) {
    if (quantity <= 0) {
      _lines.remove(id);
    } else if (_lines.containsKey(id)) {
      _lines[id]!.quantity = quantity;
    }
    notifyListeners();
  }

  void clear() {
    _lines.clear();
    notifyListeners();
  }
}
