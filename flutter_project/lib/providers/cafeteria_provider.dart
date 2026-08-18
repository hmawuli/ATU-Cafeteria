import 'package:flutter/foundation.dart';
import '../models/food_item.dart';
import '../models/order.dart';
import '../services/api_service.dart';
import '../services/websocket_service.dart';

class CafeteriaProvider with ChangeNotifier {
  List<FoodItem> _foodItems = [
    FoodItem(
      id: 1,
      vendorId: 201,
      name: 'Spiced Jollof Rice with Chicken',
      price: 35.00,
      category: 'Main Dish',
      imageUrl: 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c',
      description: 'Fragrant seasoned rice served with tender grilled chicken and shito pepper.',
      calories: 450,
      allergens: 'None',
    ),
    FoodItem(
      id: 2,
      vendorId: 201,
      name: 'Waakye Deluxe with Egg & Wele',
      price: 30.00,
      category: 'Main Dish',
      imageUrl: 'https://images.unsplash.com/photo-1565299624946-b28f40a0ae38',
      description: 'Authentic Ghanaian rice and beans with spaghetti, gari, and rich stew.',
      calories: 520,
      allergens: 'Egg',
    ),
    FoodItem(
      id: 3,
      vendorId: 201,
      name: 'Fried Plantain with Red Red Beans',
      price: 25.00,
      category: 'Traditional',
      imageUrl: 'https://images.unsplash.com/photo-1540420773420-3366772f4999',
      description: 'Sweet golden fried plantains served with rich palm-nut black-eyed bean stew.',
      calories: 380,
      allergens: 'None',
    ),
    FoodItem(
      id: 4,
      vendorId: 201,
      name: 'Chilled Sobolo (Hibiscus Juice)',
      price: 10.00,
      category: 'Drinks',
      imageUrl: 'https://images.unsplash.com/photo-1513558161293-cdaf765ed2fd',
      description: 'Refreshing spiced ginger hibiscus drink brewed fresh daily.',
      calories: 90,
      allergens: 'None',
    ),
  ];

  List<Order> _orders = [];
  Map<int, int> _cart = {}; // foodItemId -> quantity

  List<FoodItem> get foodItems => _foodItems;
  List<Order> get orders => _orders;
  Map<int, int> get cart => _cart;

  CafeteriaProvider() {
    // Listen for WebSocket order updates
    WebSocketService().orderStream.listen((updatedOrder) {
      final index = _orders.indexWhere((o) => o.id == updatedOrder.id);
      if (index >= 0) {
        _orders[index] = updatedOrder;
      } else {
        _orders.insert(0, updatedOrder);
      }
      notifyListeners();
    });
  }

  void addToCart(int foodItemId) {
    _cart[foodItemId] = (_cart[foodItemId] ?? 0) + 1;
    notifyListeners();
  }

  void removeFromCart(int foodItemId) {
    if (_cart.containsKey(foodItemId)) {
      if (_cart[foodItemId]! > 1) {
        _cart[foodItemId] = _cart[foodItemId]! - 1;
      } else {
        _cart.remove(foodItemId);
      }
      notifyListeners();
    }
  }

  void clearCart() {
    _cart.clear();
    notifyListeners();
  }

  double get cartTotal {
    double total = 0;
    _cart.forEach((foodId, qty) {
      final item = _foodItems.firstWhere((f) => f.id == foodId, orElse: () => _foodItems.first);
      total += item.price * qty;
    });
    return total;
  }

  Future<bool> checkoutCart(int customerId) async {
    if (_cart.isEmpty) return false;

    for (var entry in _cart.entries) {
      final item = _foodItems.firstWhere((f) => f.id == entry.key);
      final newOrder = Order(
        id: DateTime.now().millisecondsSinceEpoch % 10000,
        customerId: customerId,
        vendorId: item.vendorId,
        foodItemId: item.id,
        foodName: item.name,
        quantity: entry.value,
        unitPrice: item.price,
        totalPrice: item.price * entry.value,
        orderTimestamp: DateTime.now().millisecondsSinceEpoch,
        status: 'PENDING',
        pickupPin: (1000 + (entry.key * 7)).toString(),
        estimatedPickupTime: '15-20 mins',
      );

      _orders.insert(0, newOrder);
      WebSocketService().broadcastNewOrder(newOrder);
    }

    clearCart();
    notifyListeners();
    return true;
  }

  void updateOrderStatus(int orderId, String newStatus) {
    final index = _orders.indexWhere((o) => o.id == orderId);
    if (index >= 0) {
      _orders[index].status = newStatus;
      WebSocketService().emitOrderStatusUpdate(_orders[index]);
      notifyListeners();
    }
  }
}
