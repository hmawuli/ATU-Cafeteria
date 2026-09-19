import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import '../../domain/models/models.dart';

class CartLine {
  final FoodItem item;
  int quantity;
  CartLine({required this.item, this.quantity = 1});
  double get total => item.price * quantity;
}

class CartProvider extends ChangeNotifier {
  static const _storageKey = 'atu_cart';
  static const _storage = FlutterSecureStorage();

  final Map<int, CartLine> _lines = {};
  Future<void>? _restoreFuture;

  CartProvider() {
    _restoreFuture = _restore();
  }

  List<CartLine> get lines => _lines.values.toList(growable: false);
  int get itemCount =>
      _lines.values.fold(0, (sum, line) => sum + line.quantity);
  double get subtotal => _lines.values.fold(0, (sum, line) => sum + line.total);
  bool get isEmpty => _lines.isEmpty;

  List<Map<String, dynamic>> toCheckoutPayload() => _lines.values
      .map((line) => {'food_item_id': line.item.id, 'quantity': line.quantity})
      .toList();

  Future<void> restore() => _restoreFuture ??= _restore();

  Future<void> _restore() async {
    try {
      final raw = await _storage.read(key: _storageKey);
      if (raw == null || raw.isEmpty) return;

      final decoded = jsonDecode(raw);
      if (decoded is! List) return;

      for (final entry in decoded) {
        if (entry is! Map) continue;
        final itemRaw = entry['item'];
        if (itemRaw is! Map) continue;

        final item = FoodItem.fromJson(Map<String, dynamic>.from(itemRaw));
        final id = item.id;
        final quantity = int.tryParse(entry['quantity']?.toString() ?? '') ?? 0;
        if (id == null || quantity <= 0) continue;

        // Merge rather than replace anything the user may have added while
        // the persisted cart is being restored.
        if (!_lines.containsKey(id)) {
          _lines[id] = CartLine(item: item, quantity: quantity);
        }
      }

      if (_lines.isNotEmpty) notifyListeners();
    } catch (e) {
      debugPrint('Cart restore skipped: $e');
    }
  }

  Future<void> _persist() async {
    try {
      final payload = _lines.values
          .map((line) => {
                'item': line.item.toJson(),
                'quantity': line.quantity,
              })
          .toList();
      await _storage.write(key: _storageKey, value: jsonEncode(payload));
    } catch (e) {
      debugPrint('Cart persistence skipped: $e');
    }
  }

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
    _persist();
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
    _persist();
  }

  void setQuantity(int id, int quantity) {
    if (quantity <= 0) {
      _lines.remove(id);
    } else if (_lines.containsKey(id)) {
      _lines[id]!.quantity = quantity;
    }
    notifyListeners();
    _persist();
  }

  void clear() {
    _lines.clear();
    notifyListeners();
    _storage.delete(key: _storageKey).catchError((_) {});
  }
}
