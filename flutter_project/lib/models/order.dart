class Order {
  final int id;
  final int customerId;
  final int vendorId;
  final int foodItemId;
  final String foodName;
  final int quantity;
  final double unitPrice;
  final double totalPrice;
  final int orderTimestamp;
  String status; // 'PENDING', 'PREPARING', 'READY', 'COMPLETED', 'DELIVERED'
  final String pickupPin;
  String? estimatedPickupTime;
  final int pointsRedeemed;
  final double discountApplied;

  Order({
    required this.id,
    required this.customerId,
    required this.vendorId,
    required this.foodItemId,
    required this.foodName,
    required this.quantity,
    required this.unitPrice,
    required this.totalPrice,
    required this.orderTimestamp,
    required this.status,
    required this.pickupPin,
    this.estimatedPickupTime,
    this.pointsRedeemed = 0,
    this.discountApplied = 0.0,
  });

  factory Order.fromJson(Map<String, dynamic> json) {
    return Order(
      id: json['id'] ?? 0,
      customerId: json['customer_id'] ?? json['customerId'] ?? 0,
      vendorId: json['vendor_id'] ?? json['vendorId'] ?? 0,
      foodItemId: json['food_item_id'] ?? json['foodItemId'] ?? 0,
      foodName: json['food_name'] ?? json['foodName'] ?? 'Meal Item',
      quantity: json['quantity'] ?? 1,
      unitPrice: (json['unit_price'] ?? json['unitPrice'] as num?)?.toDouble() ?? 0.0,
      totalPrice: (json['total_price'] ?? json['totalPrice'] as num?)?.toDouble() ?? 0.0,
      orderTimestamp: json['order_timestamp'] ?? json['orderTimestamp'] ?? DateTime.now().millisecondsSinceEpoch,
      status: (json['status'] ?? 'PENDING').toString().toUpperCase(),
      pickupPin: json['pickup_pin'] ?? json['pickupPin'] ?? '1234',
      estimatedPickupTime: json['estimated_pickup_time'] ?? json['estimatedPickupTime'],
      pointsRedeemed: json['points_redeemed'] ?? json['pointsRedeemed'] ?? 0,
      discountApplied: (json['discount_applied'] ?? json['discountApplied'] as num?)?.toDouble() ?? 0.0,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'customer_id': customerId,
      'vendor_id': vendorId,
      'food_item_id': foodItemId,
      'food_name': foodName,
      'quantity': quantity,
      'unit_price': unitPrice,
      'total_price': totalPrice,
      'order_timestamp': orderTimestamp,
      'status': status,
      'pickup_pin': pickupPin,
      'estimated_pickup_time': estimatedPickupTime,
      'points_redeemed': pointsRedeemed,
      'discount_applied': discountApplied,
    };
  }

  String get displayStatus {
    switch (status.toUpperCase()) {
      case 'PENDING':
      case 'ORDER_PLACED':
      case 'RECEIVED':
        return 'Received';
      case 'PREPARING':
        return 'Preparing';
      case 'READY':
      case 'READY_FOR_PICKUP':
      case 'OUT_FOR_DELIVERY':
        return 'Out for Delivery';
      case 'COMPLETED':
      case 'DELIVERED':
        return 'Delivered';
      default:
        return status;
    }
  }
}
