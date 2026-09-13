/// Domain models shared by the Flutter application.
///
/// These models are intentionally independent of widgets and networking so
/// the presentation layer can remain easy to test and maintain.
library;

class User {
  String? get emailAddress => email;

  final int? id;
  final String username;
  final String passwordHash;
  final String role;
  final String fullName;
  final String info;
  final String? studentStaffId;
  final String? telephone;
  final String? email;
  final String? department;
  final String? programOfStudy;
  final double balance;
  final bool isOpen;
  final String accountStatus;
  final String? adminLevel;
  final bool twoFactorEnabled;
  final DateTime? emailVerifiedAt;

  const User({
    this.id,
    required this.username,
    this.passwordHash = '',
    required this.role,
    required this.fullName,
    this.info = '',
    this.studentStaffId,
    this.telephone,
    this.email,
    this.department,
    this.programOfStudy,
    this.balance = 0.0,
    this.isOpen = true,
    this.accountStatus = 'ACTIVE',
    this.adminLevel,
    this.twoFactorEnabled = false,
    this.emailVerifiedAt,
  });

  factory User.fromMap(Map<String, dynamic> map) => User(
        id: _asInt(map['id']),
        username: _asString(map['username']),
        passwordHash: _asString(map['passwordHash'] ?? map['password_hash']),
        role: _asString(map['role'], fallback: 'STUDENT').toUpperCase(),
        fullName: _asString(map['fullName'] ?? map['full_name']),
        info: _asString(map['info']),
        studentStaffId:
            _asNullableString(map['student_staff_id'] ?? map['studentStaffId']),
        telephone: _asNullableString(map['telephone'] ?? map['phone_number']),
        email: _asNullableString(map['email']),
        department: _asNullableString(map['department']),
        programOfStudy:
            _asNullableString(map['program_of_study'] ?? map['programOfStudy']),
        balance: _asDouble(map['balance']),
        isOpen: _asBool(map['is_open'] ?? map['isOpen'], fallback: true),
        accountStatus: _asString(map['account_status'] ?? map['accountStatus'],
            fallback: 'ACTIVE'),
        adminLevel: _asNullableString(map['admin_level'] ?? map['adminLevel']),
        twoFactorEnabled:
            _asBool(map['two_factor_enabled'] ?? map['twoFactorEnabled']),
        emailVerifiedAt:
            _asDateTime(map['email_verified_at'] ?? map['emailVerifiedAt']),
      );

  factory User.fromJson(Map<String, dynamic> json) => User.fromMap(json);

  Map<String, dynamic> toMap() => {
        'id': id,
        'username': username,
        'passwordHash': passwordHash,
        'role': role,
        'fullName': fullName,
        'info': info,
        'student_staff_id': studentStaffId,
        'telephone': telephone,
        'email': email,
        'department': department,
        'program_of_study': programOfStudy,
        'balance': balance,
        'is_open': isOpen ? 1 : 0,
        'account_status': accountStatus,
        'admin_level': adminLevel,
        'two_factor_enabled': twoFactorEnabled ? 1 : 0,
        'email_verified_at': emailVerifiedAt?.toIso8601String(),
      };

  Map<String, dynamic> toJson() => toMap();

  User copyWith({
    int? id,
    String? username,
    String? passwordHash,
    String? role,
    String? fullName,
    String? info,
    String? studentStaffId,
    String? telephone,
    String? email,
    String? department,
    String? programOfStudy,
    double? balance,
    bool? isOpen,
    String? accountStatus,
    String? adminLevel,
    bool? twoFactorEnabled,
  }) =>
      User(
        id: id ?? this.id,
        username: username ?? this.username,
        passwordHash: passwordHash ?? this.passwordHash,
        role: role ?? this.role,
        fullName: fullName ?? this.fullName,
        info: info ?? this.info,
        studentStaffId: studentStaffId ?? this.studentStaffId,
        telephone: telephone ?? this.telephone,
        email: email ?? this.email,
        department: department ?? this.department,
        programOfStudy: programOfStudy ?? this.programOfStudy,
        balance: balance ?? this.balance,
        isOpen: isOpen ?? this.isOpen,
        accountStatus: accountStatus ?? this.accountStatus,
        adminLevel: adminLevel ?? this.adminLevel,
        twoFactorEnabled: twoFactorEnabled ?? this.twoFactorEnabled,
      );
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
  final int calories;
  final String allergens;

  const FoodItem({
    this.id,
    required this.vendorId,
    required this.name,
    required this.price,
    required this.category,
    required this.imageUrl,
    required this.description,
    this.isAvailable = true,
    this.calories = 250,
    this.allergens = 'None',
  });

  factory FoodItem.fromMap(Map<String, dynamic> map) => FoodItem(
        id: _asInt(map['id']),
        vendorId: _asInt(map['vendorId'] ?? map['vendor_id']) ?? 0,
        name: _asString(map['name']),
        price: _asDouble(map['price']),
        category: _asString(map['category'], fallback: 'General'),
        imageUrl: _asString(map['imageUrl'] ?? map['image_url']),
        description: _asString(map['description']),
        isAvailable:
            _asBool(map['isAvailable'] ?? map['is_available'], fallback: true),
        calories: _asInt(map['calories']) ?? 250,
        allergens: _asString(map['allergens'], fallback: 'None'),
      );

  factory FoodItem.fromJson(Map<String, dynamic> json) =>
      FoodItem.fromMap(json);

  Map<String, dynamic> toMap() => {
        'id': id,
        'vendorId': vendorId,
        'name': name,
        'price': price,
        'category': category,
        'imageUrl': imageUrl,
        'description': description,
        'isAvailable': isAvailable ? 1 : 0,
        'calories': calories,
        'allergens': allergens,
      };

  Map<String, dynamic> toJson() => {
        'id': id,
        'vendor_id': vendorId,
        'name': name,
        'price': price,
        'category': category,
        'image_url': imageUrl,
        'description': description,
        'is_available': isAvailable,
        'calories': calories,
        'allergens': allergens,
      };

  FoodItem copyWith({bool? isAvailable}) => FoodItem(
        id: id,
        vendorId: vendorId,
        name: name,
        price: price,
        category: category,
        imageUrl: imageUrl,
        description: description,
        isAvailable: isAvailable ?? this.isAvailable,
        calories: calories,
        allergens: allergens,
      );
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
  final int orderTimestamp;
  String status;
  final String pickupPin;
  String? estimatedPickupTime;
  final int pointsRedeemed;
  final double discountApplied;

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
    this.estimatedPickupTime,
    this.pointsRedeemed = 0,
    this.discountApplied = 0.0,
  });

  factory Order.fromMap(Map<String, dynamic> map) => Order(
        id: _asInt(map['id']),
        customerId: _asInt(map['customerId'] ?? map['customer_id']) ?? 0,
        vendorId: _asInt(map['vendorId'] ?? map['vendor_id']) ?? 0,
        foodItemId: _asInt(map['foodItemId'] ?? map['food_item_id']) ?? 0,
        foodName:
            _asString(map['foodName'] ?? map['food_name'], fallback: 'Meal'),
        quantity: _asInt(map['quantity']) ?? 1,
        unitPrice: _asDouble(map['unitPrice'] ?? map['unit_price']),
        totalPrice: _asDouble(map['totalPrice'] ?? map['total_price']),
        orderTimestamp:
            _asInt(map['orderTimestamp'] ?? map['order_timestamp']) ??
                DateTime.now().millisecondsSinceEpoch,
        status:
            _asString(map['status'] ?? map['order_status'], fallback: 'PENDING')
                .toUpperCase(),
        pickupPin:
            _asString(map['pickupPin'] ?? map['pickup_pin'], fallback: '0000'),
        estimatedPickupTime: _asNullableString(
            map['estimatedPickupTime'] ?? map['estimated_pickup_time']),
        pointsRedeemed:
            _asInt(map['pointsRedeemed'] ?? map['points_redeemed']) ?? 0,
        discountApplied:
            _asDouble(map['discountApplied'] ?? map['discount_applied']),
      );

  factory Order.fromJson(Map<String, dynamic> json) => Order.fromMap(json);

  Map<String, dynamic> toMap() => {
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

  Map<String, dynamic> toJson() => {
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

  const Feedback({
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

  factory Feedback.fromMap(Map<String, dynamic> map) => Feedback(
        id: _asInt(map['id']),
        orderId: _asInt(map['orderId'] ?? map['order_id']) ?? 0,
        vendorId: _asInt(map['vendorId'] ?? map['vendor_id']) ?? 0,
        customerId: _asInt(map['customerId'] ?? map['customer_id']) ?? 0,
        ratingFoodQuality:
            _asInt(map['ratingFoodQuality'] ?? map['rating_food_quality']) ?? 0,
        ratingCleanliness:
            _asInt(map['ratingCleanliness'] ?? map['rating_cleanliness']) ?? 0,
        ratingServiceSpeed:
            _asInt(map['ratingServiceSpeed'] ?? map['rating_service_speed']) ??
                0,
        ratingPriceValue:
            _asInt(map['ratingPriceValue'] ?? map['rating_price_value']) ?? 0,
        comment: _asString(map['comment']),
        timestamp:
            _asInt(map['timestamp']) ?? DateTime.now().millisecondsSinceEpoch,
      );

  factory Feedback.fromJson(Map<String, dynamic> json) =>
      Feedback.fromMap(json);

  Map<String, dynamic> toMap() => {
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

  Map<String, dynamic> toJson() => toMap();
}

class AuditLog {
  final int? id;
  final int timestamp;
  final int userId;
  final String action;
  final String details;

  const AuditLog({
    this.id,
    required this.timestamp,
    required this.userId,
    required this.action,
    required this.details,
  });

  factory AuditLog.fromMap(Map<String, dynamic> map) => AuditLog(
        id: _asInt(map['id']),
        timestamp:
            _asInt(map['timestamp']) ?? DateTime.now().millisecondsSinceEpoch,
        userId: _asInt(map['userId'] ?? map['user_id']) ?? 0,
        action: _asString(map['action']),
        details: _asString(map['details']),
      );

  factory AuditLog.fromJson(Map<String, dynamic> json) =>
      AuditLog.fromMap(json);

  Map<String, dynamic> toMap() => {
        'id': id,
        'timestamp': timestamp,
        'userId': userId,
        'action': action,
        'details': details,
      };

  Map<String, dynamic> toJson() => toMap();
}

int? _asInt(dynamic value) {
  if (value == null) return null;
  if (value is int) return value;
  if (value is num) return value.toInt();
  return int.tryParse(value.toString());
}

double _asDouble(dynamic value) {
  if (value is num) return value.toDouble();
  return double.tryParse(value?.toString() ?? '') ?? 0.0;
}

DateTime? _asDateTime(dynamic value) {
  if (value == null) return null;
  return DateTime.tryParse(value.toString());
}

String _asString(dynamic value, {String fallback = ''}) =>
    value?.toString() ?? fallback;

String? _asNullableString(dynamic value) => value?.toString();

bool _asBool(dynamic value, {bool fallback = false}) {
  if (value is bool) return value;
  if (value is num) return value != 0;
  if (value is String) return value.toLowerCase() == 'true' || value == '1';
  return fallback;
}
