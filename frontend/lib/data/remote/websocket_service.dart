import 'dart:async';
import 'package:atu_cafeteria/domain/models/models.dart';

/// Handles real-time order notifications and state streaming across Student and Vendor roles.
class WebSocketService {
  static final WebSocketService _instance = WebSocketService._internal();
  factory WebSocketService() => _instance;
  WebSocketService._internal();

  final _orderController = StreamController<Order>.broadcast();
  Stream<Order> get orderStream => _orderController.stream;

  final _notificationController =
      StreamController<Map<String, dynamic>>.broadcast();
  Stream<Map<String, dynamic>> get notificationStream =>
      _notificationController.stream;

  void emitOrderStatusUpdate(Order updatedOrder) {
    _orderController.add(updatedOrder);
    _notificationController.add({
      'order_id': updatedOrder.id,
      'status': updatedOrder.status,
      'message':
          'Order #${updatedOrder.id} status changed to ${updatedOrder.displayStatus}',
      'timestamp': DateTime.now().millisecondsSinceEpoch,
    });
  }

  void broadcastNewOrder(Order newOrder) {
    _orderController.add(newOrder);
  }

  void dispose() {
    _orderController.close();
    _notificationController.close();
  }
}
