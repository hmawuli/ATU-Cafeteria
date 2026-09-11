import 'dart:async';
import 'package:sqflite/sqflite.dart';
import 'package:path/path.dart';
import 'package:atu_cafeteria/domain/models/models.dart';

class DbHelper {
  static final DbHelper instance = DbHelper._init();
  static Database? _database;

  DbHelper._init();

  Future<Database> get database async {
    if (_database != null) return _database!;
    _database = await _initDB('atu_cafeteria.db');
    return _database!;
  }

  Future<Database> _initDB(String filePath) async {
    final dbPath = await getDatabasesPath();
    final path = join(dbPath, filePath);

    return await openDatabase(
      path,
      version: 2,
      onCreate: _createDB,
      onUpgrade: _upgradeDB,
    );
  }

  Future<void> _upgradeDB(Database db, int oldVersion, int newVersion) async {
    if (oldVersion < 2) {
      // Keep upgrades additive so existing local demo data is preserved.
      await db.execute("ALTER TABLE food_items ADD COLUMN calories INTEGER NOT NULL DEFAULT 250");
      await db.execute("ALTER TABLE food_items ADD COLUMN allergens TEXT NOT NULL DEFAULT 'None'");
    }
  }

  Future _createDB(Database db, int version) async {
    // 1. Users Table
    await db.execute('''
      CREATE TABLE users (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        username TEXT NOT NULL UNIQUE,
        passwordHash TEXT NOT NULL,
        role TEXT NOT NULL,
        fullName TEXT NOT NULL,
        info TEXT NOT NULL
      )
    ''');

    // 2. FoodItems Table
    await db.execute('''
      CREATE TABLE food_items (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        vendorId INTEGER NOT NULL,
        name TEXT NOT NULL,
        price REAL NOT NULL,
        category TEXT NOT NULL,
        imageUrl TEXT NOT NULL,
        description TEXT NOT NULL,
        isAvailable INTEGER NOT NULL DEFAULT 1,
        calories INTEGER NOT NULL DEFAULT 250,
        allergens TEXT NOT NULL DEFAULT 'None',
        FOREIGN KEY (vendorId) REFERENCES users (id) ON DELETE CASCADE
      )
    ''');

    // 3. Orders Table
    await db.execute('''
      CREATE TABLE orders (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        customerId INTEGER NOT NULL,
        vendorId INTEGER NOT NULL,
        foodItemId INTEGER NOT NULL,
        foodName TEXT NOT NULL,
        quantity INTEGER NOT NULL,
        unitPrice REAL NOT NULL,
        totalPrice REAL NOT NULL,
        orderTimestamp INTEGER NOT NULL,
        status TEXT NOT NULL DEFAULT 'PENDING',
        pickupPin TEXT NOT NULL,
        FOREIGN KEY (customerId) REFERENCES users (id) ON DELETE CASCADE
      )
    ''');

    // 4. Feedback Table
    await db.execute('''
      CREATE TABLE feedback (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        orderId INTEGER NOT NULL,
        vendorId INTEGER NOT NULL,
        customerId INTEGER NOT NULL,
        ratingFoodQuality INTEGER NOT NULL,
        ratingCleanliness INTEGER NOT NULL,
        ratingServiceSpeed INTEGER NOT NULL,
        ratingPriceValue INTEGER NOT NULL,
        comment TEXT NOT NULL,
        timestamp INTEGER NOT NULL,
        FOREIGN KEY (orderId) REFERENCES orders (id) ON DELETE CASCADE
      )
    ''');

    // 5. Audit Logs Table
    await db.execute('''
      CREATE TABLE audit_logs (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        timestamp INTEGER NOT NULL,
        userId INTEGER NOT NULL,
        action TEXT NOT NULL,
        details TEXT NOT NULL
      )
    ''');

    // Indexes for fast lookups
    await db.execute('CREATE INDEX idx_users_username ON users(username)');
    await db.execute('CREATE INDEX idx_food_vendor ON food_items(vendorId)');
    await db.execute('CREATE INDEX idx_orders_customer ON orders(customerId)');
    await db.execute('CREATE INDEX idx_orders_vendor ON orders(vendorId)');
    await db.execute('CREATE INDEX idx_feedback_vendor ON feedback(vendorId)');
  }

  // ==========================================
  // USER OPERATIONS
  // ==========================================
  Future<int> insertUser(User user) async {
    final db = await instance.database;
    return await db.insert('users', {
      'id': user.id,
      'username': user.username,
      'passwordHash': user.passwordHash,
      'role': user.role,
      'fullName': user.fullName,
      'info': user.info,
    });
  }

  Future<User?> getUserByUsername(String username) async {
    final db = await instance.database;
    final maps = await db.query(
      'users',
      where: 'username = ?',
      whereArgs: [username],
    );

    if (maps.isNotEmpty) {
      return User.fromMap(maps.first);
    }
    return null;
  }

  Future<List<User>> getAllVendors() async {
    final db = await instance.database;
    final result = await db.query('users', where: "role = 'VENDOR'");
    return result.map(User.fromMap).toList();
  }

  Future<int> updateUser(User user) async {
    final db = await instance.database;
    return await db.update(
      'users',
      {
        'username': user.username,
        'passwordHash': user.passwordHash,
        'role': user.role,
        'fullName': user.fullName,
        'info': user.info,
      },
      where: 'id = ?',
      whereArgs: [user.id],
    );
  }

  Future<int> deleteUser(int id) async {
    final db = await instance.database;
    await db.delete('food_items', where: 'vendorId = ?', whereArgs: [id]);
    await db.delete('orders', where: 'vendorId = ?', whereArgs: [id]);
    await db.delete('feedback', where: 'vendorId = ?', whereArgs: [id]);
    return await db.delete(
      'users',
      where: 'id = ?',
      whereArgs: [id],
    );
  }

  Future<int> getUserCount() async {
    final db = await instance.database;
    final result = await db.rawQuery('SELECT COUNT(*) FROM users');
    return Sqflite.firstIntValue(result) ?? 0;
  }

  // ==========================================
  // FOOD OPERATIONS
  // ==========================================
  Future<int> insertFoodItem(FoodItem item) async {
    final db = await instance.database;
    return await db.insert('food_items', {
      'id': item.id,
      'vendorId': item.vendorId,
      'name': item.name,
      'price': item.price,
      'category': item.category,
      'imageUrl': item.imageUrl,
      'description': item.description,
      'isAvailable': item.isAvailable ? 1 : 0,
    });
  }

  Future<List<FoodItem>> getAllFoodItems() async {
    final db = await instance.database;
    final result = await db.query('food_items');
    return result.map(FoodItem.fromMap).toList();
  }

  Future<List<FoodItem>> getFoodItemsByVendor(int vendorId) async {
    final db = await instance.database;
    final result = await db.query(
      'food_items',
      where: 'vendorId = ?',
      whereArgs: [vendorId],
    );
    return result.map(FoodItem.fromMap).toList();
  }

  Future<int> updateFoodItem(FoodItem item) async {
    final db = await instance.database;
    return await db.update(
      'food_items',
      {
        'vendorId': item.vendorId,
        'name': item.name,
        'price': item.price,
        'category': item.category,
        'imageUrl': item.imageUrl,
        'description': item.description,
        'isAvailable': item.isAvailable ? 1 : 0,
      },
      where: 'id = ?',
      whereArgs: [item.id],
    );
  }

  Future<int> deleteFoodItem(int id) async {
    final db = await instance.database;
    return await db.delete(
      'food_items',
      where: 'id = ?',
      whereArgs: [id],
    );
  }

  // ==========================================
  // ORDER OPERATIONS
  // ==========================================
  Future<int> insertOrder(Order order) async {
    final db = await instance.database;
    return await db.insert(
      'orders',
      order.toMap(),
      conflictAlgorithm: ConflictAlgorithm.replace,
    );
  }

  Future<List<Order>> getAllOrders() async {
    final db = await instance.database;
    final result = await db.query('orders', orderBy: 'orderTimestamp DESC');
    return result.map(Order.fromMap).toList();
  }

  Future<List<Order>> getOrdersForCustomer(int customerId) async {
    final db = await instance.database;
    final result = await db.query(
      'orders',
      where: 'customerId = ?',
      whereArgs: [customerId],
      orderBy: 'orderTimestamp DESC',
    );
    return result.map(Order.fromMap).toList();
  }

  Future<List<Order>> getOrdersForVendor(int vendorId) async {
    final db = await instance.database;
    final result = await db.query(
      'orders',
      where: 'vendorId = ?',
      whereArgs: [vendorId],
      orderBy: 'orderTimestamp DESC',
    );
    return result.map(Order.fromMap).toList();
  }

  Future<int> updateOrderStatus(int orderId, String status) async {
    final db = await instance.database;
    return await db.update(
      'orders',
      {'status': status},
      where: 'id = ?',
      whereArgs: [orderId],
    );
  }

  // ==========================================
  // FEEDBACK OPERATIONS
  // ==========================================
  Future<int> insertFeedback(Feedback feedback) async {
    final db = await instance.database;
    return await db.insert(
      'feedback',
      feedback.toMap(),
      conflictAlgorithm: ConflictAlgorithm.replace,
    );
  }

  Future<List<Feedback>> getAllFeedback() async {
    final db = await instance.database;
    final result = await db.query('feedback', orderBy: 'timestamp DESC');
    return result.map(Feedback.fromMap).toList();
  }

  Future<List<Feedback>> getFeedbackForVendor(int vendorId) async {
    final db = await instance.database;
    final result = await db.query(
      'feedback',
      where: 'vendorId = ?',
      whereArgs: [vendorId],
      orderBy: 'timestamp DESC',
    );
    return result.map(Feedback.fromMap).toList();
  }

  Future<int> deleteFeedback(int id) async {
    final db = await instance.database;
    return await db.delete(
      'feedback',
      where: 'id = ?',
      whereArgs: [id],
    );
  }

  // ==========================================
  // AUDIT LOG OPERATIONS
  // ==========================================
  Future<int> insertAuditLog(AuditLog log) async {
    final db = await instance.database;
    return await db.insert(
      'audit_logs',
      log.toMap(),
      conflictAlgorithm: ConflictAlgorithm.replace,
    );
  }

  Future<List<AuditLog>> getAllLogs() async {
    final db = await instance.database;
    final result = await db.query('audit_logs', orderBy: 'timestamp DESC');
    return result.map(AuditLog.fromMap).toList();
  }

  Future<int> deleteAuditLog(int id) async {
    final db = await instance.database;
    return await db.delete(
      'audit_logs',
      where: 'id = ?',
      whereArgs: [id],
    );
  }

  Future close() async {
    final db = await instance.database;
    db.close();
  }
}
