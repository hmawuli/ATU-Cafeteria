class User {
  final int? id;
  final String username;
  final String passwordHash;
  final String role; // STUDENT, VENDOR, ADMIN
  final String fullName;
  final String info; // Student ID (e.g. ATU-2024-D45) or Vendor brand

  User({
    this.id,
    required this.username,
    required this.passwordHash,
    required this.role,
    required this.fullName,
    required this.info,
  });

  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'username': username,
      'passwordHash': passwordHash,
      'role': role,
      'fullName': fullName,
      'info': info,
    };
  }

  factory User.fromMap(Map<String, dynamic> map) {
    return User(
      id: map['id'],
      username: map['username'],
      passwordHash: map['passwordHash'],
      role: map['role'],
      fullName: map['fullName'],
      info: map['info'],
    );
  }

  User copyWith({
    int? id,
    String? username,
    String? passwordHash,
    String? role,
    String? fullName,
    String? info,
  }) {
    return User(
      id: id ?? this.id,
      username: username ?? this.username,
      passwordHash: passwordHash ?? this.passwordHash,
      role: role ?? this.role,
      fullName: fullName ?? this.fullName,
      info: info ?? this.info,
    );
  }
}

class FoodItem {
  final int? id;
  final int vendorId;
  final String name;
  final double price;
  final String category;
  final String imageUrl;
  final String description;
  final bool isAvailable;

  FoodItem({
    this.id,
    required this.vendorId,
    required this.name,
    required this.price,
    required this.category,
    required this.imageUrl,
    required this.description,
    this.isAvailable = true,
  });

  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'vendorId': vendorId,
      'name': name,
      'price': price,
      'category': category,
      'imageUrl': imageUrl,
      'description': description,
      'isAvailable': isAvailable ? 1 : 0,
    };
  }

  factory FoodItem.fromMap(Map<String, dynamic> map) {
    return FoodItem(
      id: map['id'],
      vendorId: map['vendorId'],
      name: map['name'],
      price: (map['price'] as num).toDouble(),
      category: map['category'],
      imageUrl: map['imageUrl'] ?? '',
      description: map['description'] ?? '',
      isAvailable: map['isAvailable'] == 1,
    );
  }

  FoodItem copyWith({bool? isAvailable}) {
    return FoodItem(
      id: id,
      vendorId: vendorId,
      name: name,
      price: price,
      category: category,
      imageUrl: imageUrl,
      description: description,
      isAvailable: isAvailable ?? this.isAvailable,
    );
  }
}

class Order {
  final int? id;
  final int customerId;
  final int vendorId;
  final int foodItemId;
  final String foodName;
  final int quantity;
  final double unitPrice;
  final double totalPrice;
  final int orderTimestamp; // epoch millis
  final String status; // PENDING, PREPARING, READY, COMPLETED, DECLINED
  final String pickupPin;

  Order({
    this.id,
    required this.customerId,
    required this.vendorId,
    required this.foodItemId,
    required this.foodName,
    required this.quantity,
    required this.unitPrice,
    required this.totalPrice,
    required this.orderTimestamp,
    this.status = 'PENDING',
    required this.pickupPin,
  });

  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'customerId': customerId,
      'vendorId': vendorId,
      'foodItemId': foodItemId,
      'foodName': foodName,
      'quantity': quantity,
      'unitPrice': unitPrice,
      'totalPrice': totalPrice,
      'orderTimestamp': orderTimestamp,
      'status': status,
      'pickupPin': pickupPin,
    };
  }

  factory Order.fromMap(Map<String, dynamic> map) {
    return Order(
      id: map['id'],
      customerId: map['customerId'],
      vendorId: map['vendorId'],
      foodItemId: map['foodItemId'],
      foodName: map['foodName'],
      quantity: map['quantity'],
      unitPrice: (map['unitPrice'] as num).toDouble(),
      totalPrice: (map['totalPrice'] as num).toDouble(),
      orderTimestamp: map['orderTimestamp'],
      status: map['status'] ?? 'PENDING',
      pickupPin: map['pickupPin'] ?? '',
    );
  }

  Order copyWith({String? status}) {
    return Order(
      id: id,
      customerId: customerId,
      vendorId: vendorId,
      foodItemId: foodItemId,
      foodName: foodName,
      quantity: quantity,
      unitPrice: unitPrice,
      totalPrice: totalPrice,
      orderTimestamp: orderTimestamp,
      status: status ?? this.status,
      pickupPin: pickupPin,
    );
  }
}

class Feedback {
  final int? id;
  final int orderId;
  final int vendorId;
  final int customerId;
  final int ratingFoodQuality;
  final int ratingCleanliness;
  final int ratingServiceSpeed;
  final int ratingPriceValue;
  final String comment;
  final int timestamp;

  Feedback({
    this.id,
    required this.orderId,
    required this.vendorId,
    required this.customerId,
    required this.ratingFoodQuality,
    required this.ratingCleanliness,
    required this.ratingServiceSpeed,
    required this.ratingPriceValue,
    required this.comment,
    required this.timestamp,
  });

  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'orderId': orderId,
      'vendorId': vendorId,
      'customerId': customerId,
      'ratingFoodQuality': ratingFoodQuality,
      'ratingCleanliness': ratingCleanliness,
      'ratingServiceSpeed': ratingServiceSpeed,
      'ratingPriceValue': ratingPriceValue,
      'comment': comment,
      'timestamp': timestamp,
    };
  }

  factory Feedback.fromMap(Map<String, dynamic> map) {
    return Feedback(
      id: map['id'],
      orderId: map['orderId'],
      vendorId: map['vendorId'],
      customerId: map['customerId'],
      ratingFoodQuality: map['ratingFoodQuality'],
      ratingCleanliness: map['ratingCleanliness'],
      ratingServiceSpeed: map['ratingServiceSpeed'],
      ratingPriceValue: map['ratingPriceValue'],
      comment: map['comment'] ?? '',
      timestamp: map['timestamp'],
    );
  }
}

class AuditLog {
  final int? id;
  final int timestamp;
  final int userId;
  final String action;
  final String details;

  AuditLog({
    this.id,
    required this.timestamp,
    required this.userId,
    required this.action,
    required this.details,
  });

  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'timestamp': timestamp,
      'userId': userId,
      'action': action,
      'details': details,
    };
  }

  factory AuditLog.fromMap(Map<String, dynamic> map) {
    return AuditLog(
      id: map['id'],
      timestamp: map['timestamp'],
      userId: map['userId'],
      action: map['action'],
      details: map['details'] ?? '',
    );
  }
}
