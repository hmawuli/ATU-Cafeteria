import 'dart:math';
import 'package:path/path.dart';
import 'package:sqflite/sqflite.dart';

/// Small local API-shaped data layer used only by the phone-only defense build.
///
/// It deliberately mirrors the important Laravel concepts (users, food,
/// orders, wallet ledger and reviews) without requiring PHP, a server or a
/// network connection. The normal Flutter/Laravel path is unaffected.
class DefenseLocalApi {
  DefenseLocalApi._();
  static final DefenseLocalApi instance = DefenseLocalApi._();

  Database? _db;

  Future<Database> get database async {
    if (_db != null) return _db!;
    final root = await getDatabasesPath();
    _db = await openDatabase(
      join(root, 'atu_cafeteria_defense.db'),
      version: 1,
      onCreate: (db, version) async {
        await db.execute('''
          CREATE TABLE users (
            id INTEGER PRIMARY KEY,
            username TEXT NOT NULL UNIQUE,
            pin TEXT NOT NULL,
            role TEXT NOT NULL,
            full_name TEXT NOT NULL,
            info TEXT NOT NULL,
            balance REAL NOT NULL DEFAULT 0
          )
        ''');
        await db.execute('''
          CREATE TABLE food_items (
            id INTEGER PRIMARY KEY,
            vendor_id INTEGER NOT NULL,
            name TEXT NOT NULL,
            price REAL NOT NULL,
            category TEXT NOT NULL,
            description TEXT NOT NULL,
            available INTEGER NOT NULL DEFAULT 1
          )
        ''');
        await db.execute('''
          CREATE TABLE orders (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            customer_id INTEGER NOT NULL,
            vendor_id INTEGER NOT NULL,
            food_item_id INTEGER NOT NULL,
            food_name TEXT NOT NULL,
            quantity INTEGER NOT NULL,
            unit_price REAL NOT NULL,
            total_price REAL NOT NULL,
            status TEXT NOT NULL,
            pickup_pin TEXT NOT NULL,
            created_at INTEGER NOT NULL
          )
        ''');
        await db.execute('''
          CREATE TABLE wallet_ledger (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            user_id INTEGER NOT NULL,
            type TEXT NOT NULL,
            amount REAL NOT NULL,
            description TEXT NOT NULL,
            created_at INTEGER NOT NULL
          )
        ''');
        await db.execute('''
          CREATE TABLE reviews (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            order_id INTEGER NOT NULL,
            customer_id INTEGER NOT NULL,
            vendor_id INTEGER NOT NULL,
            rating INTEGER NOT NULL,
            comment TEXT NOT NULL,
            created_at INTEGER NOT NULL
          )
        ''');
        await db.execute('''
          CREATE TABLE audit_logs (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            user_id INTEGER,
            action TEXT NOT NULL,
            details TEXT NOT NULL,
            created_at INTEGER NOT NULL
          )
        ''');
      },
    );
    await _seed();
    return _db!;
  }

  Future<void> _seed() async {
    final db = _db!;
    final count = Sqflite.firstIntValue(
          await db.rawQuery('SELECT COUNT(*) FROM users'),
        ) ??
        0;
    if (count > 0) return;

    await db.transaction((tx) async {
      await tx.insert('users', {
        'id': 1,
        'username': 'student',
        'pin': '1234',
        'role': 'STUDENT',
        'full_name': 'Daniel Mensah',
        'info': 'ATU-2026-D45',
        'balance': 120.00,
      });
      await tx.insert('users', {
        'id': 2,
        'username': 'student2',
        'pin': '1234',
        'role': 'STUDENT',
        'full_name': 'Abena Osei',
        'info': 'ATU-2026-S12',
        'balance': 85.00,
      });
      await tx.insert('users', {
        'id': 10,
        'username': 'maryjoint',
        'pin': '1111',
        'role': 'VENDOR',
        'full_name': 'Mary Joint',
        'info': 'Auntie Mary Special',
        'balance': 240.00,
      });
      await tx.insert('users', {
        'id': 11,
        'username': 'atkitch',
        'pin': '2222',
        'role': 'VENDOR',
        'full_name': 'Kofi Local Kitchen',
        'info': 'ATU Local Hub',
        'balance': 315.00,
      });
      await tx.insert('users', {
        'id': 99,
        'username': 'admin',
        'pin': 'admin123',
        'role': 'ADMIN',
        'full_name': 'ATU System Administrator',
        'info': 'System Administration',
        'balance': 0,
      });

      final foods = [
        [1, 10, 'ATU Chicken Jollof Rice', 25.0, 'Lunch Specials', 'Jollof rice, seasoned chicken, salad and shito.'],
        [2, 10, 'Zesty Ginger Sobolo', 10.0, 'Drinks', 'Chilled hibiscus drink brewed with fresh ginger.'],
        [3, 11, 'Waakye Supreme', 30.0, 'Traditional', 'Waakye served with egg, gari, stew and shito.'],
        [4, 11, 'Fufu & Goat Light Soup', 35.0, 'Traditional', 'Fresh fufu with aromatic goat light soup.'],
        [5, 10, 'Savoury Meat Pie', 15.0, 'Snacks', 'Flaky pastry filled with seasoned minced beef.'],
      ];
      for (final f in foods) {
        await tx.insert('food_items', {
          'id': f[0],
          'vendor_id': f[1],
          'name': f[2],
          'price': f[3],
          'category': f[4],
          'description': f[5],
          'available': 1,
        });
      }

      await tx.insert('orders', {
        'customer_id': 1,
        'vendor_id': 10,
        'food_item_id': 1,
        'food_name': 'ATU Chicken Jollof Rice',
        'quantity': 1,
        'unit_price': 25.0,
        'total_price': 25.0,
        'status': 'COMPLETED',
        'pickup_pin': '4444',
        'created_at': DateTime.now().millisecondsSinceEpoch - 86400000,
      });
      await tx.insert('audit_logs', {
        'user_id': 99,
        'action': 'SYSTEM_INIT',
        'details': 'Phone-only defense database initialized.',
        'created_at': DateTime.now().millisecondsSinceEpoch,
      });
    });
  }

  Future<Map<String, dynamic>?> login(String username, String pin) async {
    final db = await database;
    final rows = await db.query(
      'users',
      where: 'username = ? AND pin = ?',
      whereArgs: [username.trim(), pin.trim()],
      limit: 1,
    );
    if (rows.isEmpty) return null;
    return Map<String, dynamic>.from(rows.first);
  }

  Future<List<Map<String, dynamic>>> users({String? role}) async {
    final db = await database;
    return db.query('users',
        where: role == null ? null : 'role = ?',
        whereArgs: role == null ? null : [role],
        orderBy: 'full_name ASC');
  }

  Future<List<Map<String, dynamic>>> foods() async {
    final db = await database;
    return db.query('food_items', where: 'available = 1', orderBy: 'id ASC');
  }

  Future<List<Map<String, dynamic>>> ordersFor(int userId, String role) async {
    final db = await database;
    final where = role == 'STUDENT' ? 'customer_id = ?' : role == 'VENDOR' ? 'vendor_id = ?' : null;
    return db.query('orders', where: where, whereArgs: where == null ? null : [userId], orderBy: 'created_at DESC');
  }

  Future<double> balance(int userId) async {
    final db = await database;
    final rows = await db.query('users', columns: ['balance'], where: 'id = ?', whereArgs: [userId], limit: 1);
    return rows.isEmpty ? 0 : ((rows.first['balance'] as num?)?.toDouble() ?? 0);
  }

  Future<int> placeOrder({required int customerId, required int foodId, required int quantity}) async {
    final db = await database;
    return db.transaction((tx) async {
      final foods = await tx.query('food_items', where: 'id = ? AND available = 1', whereArgs: [foodId], limit: 1);
      if (foods.isEmpty) throw StateError('Food item is unavailable.');
      final food = foods.first;
      final price = (food['price'] as num).toDouble();
      final total = price * quantity;
      final users = await tx.query('users', where: 'id = ?', whereArgs: [customerId], limit: 1);
      if (users.isEmpty) throw StateError('Student account not found.');
      final current = (users.first['balance'] as num).toDouble();
      if (current < total) throw StateError('Insufficient wallet balance.');
      await tx.update('users', {'balance': current - total}, where: 'id = ?', whereArgs: [customerId]);
      await tx.insert('wallet_ledger', {
        'user_id': customerId,
        'type': 'DEBIT',
        'amount': total,
        'description': 'Cafeteria checkout for ${food['name']}',
        'created_at': DateTime.now().millisecondsSinceEpoch,
      });
      final pin = (1000 + Random().nextInt(9000)).toString();
      final orderId = await tx.insert('orders', {
        'customer_id': customerId,
        'vendor_id': food['vendor_id'],
        'food_item_id': foodId,
        'food_name': food['name'],
        'quantity': quantity,
        'unit_price': price,
        'total_price': total,
        'status': 'ORDER_PLACED',
        'pickup_pin': pin,
        'created_at': DateTime.now().millisecondsSinceEpoch,
      });
      await tx.insert('audit_logs', {
        'user_id': customerId,
        'action': 'ORDER_CREATED',
        'details': 'Order #$orderId created for ${food['name']} x$quantity.',
        'created_at': DateTime.now().millisecondsSinceEpoch,
      });
      return orderId;
    });
  }

  Future<void> updateOrderStatus(int orderId, String status, int actorId) async {
    final db = await database;
    await db.transaction((tx) async {
      final rows = await tx.query('orders', where: 'id = ?', whereArgs: [orderId], limit: 1);
      if (rows.isEmpty) throw StateError('Order not found.');
      final old = rows.first['status'].toString();
      final allowed = <String, Set<String>>{
        'ORDER_PLACED': {'PREPARING', 'CANCELLED'},
        'PREPARING': {'READY_FOR_PICKUP', 'CANCELLED'},
        'READY_FOR_PICKUP': {'COMPLETED'},
        'CANCELLED': {},
        'COMPLETED': {},
      };
      if (old != status && !(allowed[old]?.contains(status) ?? false)) {
        throw StateError('Invalid order transition: $old → $status');
      }
      await tx.update('orders', {'status': status}, where: 'id = ?', whereArgs: [orderId]);
      await tx.insert('audit_logs', {
        'user_id': actorId,
        'action': 'ORDER_STATUS_CHANGED',
        'details': 'Order #$orderId changed from $old to $status.',
        'created_at': DateTime.now().millisecondsSinceEpoch,
      });
    });
  }

  Future<void> addReview({required int orderId, required int customerId, required int rating, required String comment}) async {
    final db = await database;
    final rows = await db.query('orders', where: 'id = ? AND customer_id = ? AND status = ?', whereArgs: [orderId, customerId, 'COMPLETED'], limit: 1);
    if (rows.isEmpty) throw StateError('Only completed orders can be reviewed.');
    await db.insert('reviews', {
      'order_id': orderId,
      'customer_id': customerId,
      'vendor_id': rows.first['vendor_id'],
      'rating': rating,
      'comment': comment.trim(),
      'created_at': DateTime.now().millisecondsSinceEpoch,
    });
  }

  Future<List<Map<String, dynamic>>> reviews() async {
    final db = await database;
    return db.rawQuery('SELECT reviews.*, users.full_name AS customer_name FROM reviews LEFT JOIN users ON users.id = reviews.customer_id ORDER BY reviews.created_at DESC');
  }

  Future<List<Map<String, dynamic>>> auditLogs() async {
    final db = await database;
    return db.query('audit_logs', orderBy: 'created_at DESC', limit: 50);
  }

  Future<void> resetDefenseData() async {
    final db = await database;
    await db.delete('users');
    await db.delete('food_items');
    await db.delete('orders');
    await db.delete('wallet_ledger');
    await db.delete('reviews');
    await db.delete('audit_logs');
    await _seed();
  }
}
