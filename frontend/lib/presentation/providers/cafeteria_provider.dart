import 'dart:convert';
import 'dart:io';
import 'dart:async';
import 'package:flutter/foundation.dart';
import 'package:http/http.dart' as http;
import 'package:atu_cafeteria/domain/models/models.dart';
import 'package:atu_cafeteria/data/local/db_helper.dart';
import 'package:atu_cafeteria/core/config/server_config.dart';
import 'package:atu_cafeteria/core/storage/secure_session_store.dart';

class CafeteriaProvider extends ChangeNotifier {
  final DbHelper _db = DbHelper.instance;
  Timer? _readyPollingTimer;
  final Set<int> _announcedReadyOrders = <int>{};

  @override
  void dispose() {
    _readyPollingTimer?.cancel();
    _readyPollingTimer = null;
    _vendorOrderPollingTimer?.cancel();
    _vendorOrderPollingTimer = null;
    super.dispose();
  }

  // Real-time State Streams
  User? _currentUser;
  User? get currentUser => _currentUser;

  String? _authToken;
  String? get authToken => _authToken;

  bool _isOrderCacheOffline = false;
  bool get isOrderCacheOffline => _isOrderCacheOffline;

  // Remote Synchronization & Performance Metrics
  Map<String, dynamic>? _remoteVendorMetrics;
  Map<String, dynamic>? get remoteVendorMetrics => _remoteVendorMetrics;

  List<dynamic>? _remoteRechartsData;
  List<dynamic>? get remoteRechartsData => _remoteRechartsData;

  List<dynamic>? _remoteDailyRevenue;
  List<dynamic>? get remoteDailyRevenue => _remoteDailyRevenue;

  bool _isFetchingRemoteMetrics = false;
  bool get isFetchingRemoteMetrics => _isFetchingRemoteMetrics;

  bool _isLoading = false;
  bool get isLoading => _isLoading;

  String? _loginError;
  String? get loginError => _loginError;
  bool _requiresTwoFactor = false;
  bool get requiresTwoFactor => _requiresTwoFactor;

  bool _registrationSuccess = false;
  bool get registrationSuccess => _registrationSuccess;

  /// The wallet balance is the server's authoritative `users.balance`,
  /// mirrored onto [currentUser]. Every page reads this single value so a
  /// checkout, wallet top-up or admin adjustment is immediately reflected
  /// across the app instead of drifting from a hard-coded local figure.
  double get customerWalletBalance => _currentUser?.balance ?? 0;

  /// Backward-compatible alias for legacy widgets during the domain migration.
  double get studentWalletBalance => customerWalletBalance;

  String _vendorAnnouncement =
      "All meals prepared in alignment with Accra hygiene standards. Dine safe, study hard!";
  String get vendorAnnouncement => _vendorAnnouncement;

  bool _isStoreClosed = false;
  bool get isStoreClosed => _isStoreClosed;

  final List<String> _liveAlerts = [
    "🟢 Welcome to ATU Cafeteria. All storefront culinary channels are online."
  ];
  List<String> get liveAlerts => _liveAlerts;

  // List Cache / Reactive Repositories
  List<User> _allVendors = [];
  List<User> get allVendors => _allVendors;

  List<FoodItem> _allFoodItems = [];
  List<FoodItem> get allFoodItems => _allFoodItems;

  List<Order> _allOrders = [];
  List<Order> get allOrders => _allOrders;

  List<Feedback> _allFeedback = [];
  List<Feedback> get allFeedback => _allFeedback;

  List<AuditLog> _auditLogs = [];
  List<AuditLog> get auditLogs => _auditLogs;

  // User-specific Lists
  List<Order> _customerOrders = [];
  List<Order> get customerOrders => _customerOrders;

  List<Map<String, dynamic>> _purchasedVendors = [];
  List<Map<String, dynamic>> get purchasedVendors => _purchasedVendors;

  List<Order> _vendorOrders = [];
  List<Order> get vendorOrders => _vendorOrders;

  List<FoodItem> _vendorFoodItems = [];
  List<FoodItem> get vendorFoodItems => _vendorFoodItems;

  List<Feedback> _vendorFeedback = [];
  List<Feedback> get vendorFeedback => _vendorFeedback;

  // Simple string hash function matching sha256 conceptually
  String _localCacheCredentialHash(String pin) {
    // Generate a secure lookup hash safely without external dependency errors
    var bytes = utf8.encode(pin);
    var hash = 0;
    for (var byte in bytes) {
      hash = (31 * hash + byte) & 0xFFFFFFFF;
    }
    return hash.toRadixString(16);
  }

  // ==========================================
  // INITIALIZATION AND DB DATABASE SEEDING
  // ==========================================
  Future<void> initialize() async {
    _isLoading = true;
    notifyListeners();

    await _seedDatabaseIfEmpty();
    await refreshAllData();

    _isLoading = false;
    notifyListeners();
  }

  Future<List<Order>> fetchAndCacheStudentOrders(int studentId) async {
    try {
      final url = Uri.parse("$_laravelBaseUrl/api/orders/customer/$studentId");
      final response = await http.get(
        url,
        headers: {
          'Accept': 'application/json',
          if (_authToken != null) 'Authorization': 'Bearer $_authToken',
        },
      ).timeout(const Duration(seconds: 8));

      if (response.statusCode != 200 || response.body.isEmpty) {
        _isOrderCacheOffline = true;
        return [];
      }

      final decoded = jsonDecode(response.body);
      if (decoded is! List) {
        _isOrderCacheOffline = true;
        return [];
      }

      final remoteOrders = <Order>[];
      for (final raw in decoded) {
        if (raw is! Map) continue;
        try {
          final order = Order.fromJson(Map<String, dynamic>.from(raw));
          remoteOrders.add(order);

          // SQLite is only an offline cache. A cache write must never prevent
          // a successfully fetched server order from reaching the UI.
          try {
            await _db.insertOrder(order);
          } catch (e) {
            debugPrint('Student order cache update skipped: $e');
          }
        } catch (e) {
          debugPrint('Skipping malformed remote student order: $e');
        }
      }

      _isOrderCacheOffline = false;
      return remoteOrders;
    } catch (e) {
      debugPrint('Student order sync skipped; using SQLite cache: $e');
      _isOrderCacheOffline = true;
      return [];
    }
  }

  Future<void> fetchPurchasedVendors() async {
    if (_authToken == null || _currentUser?.id == null) {
      _purchasedVendors = [];
      return;
    }

    try {
      final url = Uri.parse("$_laravelBaseUrl/api/customer/purchased-vendors");
      final client = HttpClient();
      client.connectionTimeout = const Duration(seconds: 4);
      final request = await client.getUrl(url);
      request.headers.add("Accept", "application/json");
      request.headers.add("Authorization", "Bearer $_authToken");
      final response = await request.close();

      if (response.statusCode != 200) return;

      final body = await response.transform(utf8.decoder).join();
      final decoded = json.decode(body);
      final raw = decoded is Map ? decoded['vendors'] : decoded;
      if (raw is List) {
        _purchasedVendors =
            raw.whereType<Map>().map(Map<String, dynamic>.from).toList();
      }
    } catch (e) {
      debugPrint('Purchased vendor sync skipped: $e');
    }
  }

  Future<String?> submitVendorReview({
    required int orderId,
    required int vendorRating,
    String? vendorComment,
    int? foodRating,
    String? foodComment,
  }) async {
    if (_authToken == null) {
      return 'Your login session has expired. Please sign in again.';
    }

    try {
      final url = Uri.parse("$_laravelBaseUrl/api/reviews");
      final client = HttpClient();
      client.connectionTimeout = const Duration(seconds: 6);
      final request = await client.postUrl(url);
      request.headers.add("Accept", "application/json");
      request.headers.add("Content-Type", "application/json");
      request.headers.add("Authorization", "Bearer $_authToken");
      request.add(utf8.encode(jsonEncode({
        'order_id': orderId,
        'vendor_rating': vendorRating,
        if (vendorComment != null && vendorComment.trim().isNotEmpty)
          'vendor_comment': vendorComment.trim(),
        if (foodRating != null) 'food_rating': foodRating,
        if (foodComment != null && foodComment.trim().isNotEmpty)
          'food_comment': foodComment.trim(),
      })));
      final response = await request.close();
      final body = await response.transform(utf8.decoder).join();

      Map<String, dynamic>? decoded;
      if (body.isNotEmpty) {
        final raw = json.decode(body);
        if (raw is Map) decoded = Map<String, dynamic>.from(raw);
      }

      if (response.statusCode == 201) {
        await fetchPurchasedVendors();
        notifyListeners();
        return null;
      }

      return decoded?['message']?.toString() ??
          'We could not submit your review. Please try again.';
    } catch (e) {
      debugPrint('Vendor review failed: $e');
      return 'Unable to submit the review. Please check your connection and try again.';
    }
  }

  Future<List<FoodItem>> _syncRemoteFoodItems() async {
    try {
      final response = await http.get(
        Uri.parse('$_laravelBaseUrl/api/catalog/food-items'),
        headers: const {
          'Accept': 'application/json',
        },
      ).timeout(const Duration(seconds: 10));

      if (response.statusCode != 200 || response.body.isEmpty) {
        return [];
      }

      final decoded = jsonDecode(response.body);
      if (decoded is! List) return [];

      final remoteItems = <FoodItem>[];
      for (final raw in decoded) {
        if (raw is! Map) continue;
        try {
          remoteItems.add(
            FoodItem.fromJson(Map<String, dynamic>.from(raw)),
          );
        } catch (e) {
          debugPrint('Skipping malformed remote food item: $e');
        }
      }

      // Keep SQLite as an offline cache. The live API list is returned
      // directly so the UI does not depend on a successful local upsert.
      for (final item in remoteItems) {
        try {
          await _upsertLocalFoodItem(item);
        } catch (e) {
          debugPrint('Local food cache update skipped: $e');
        }
      }

      return remoteItems;
    } catch (e) {
      debugPrint('Remote food catalogue sync skipped: $e');
      return [];
    }
  }

  Future<void> _upsertLocalFoodItem(FoodItem item) async {
    if (item.id == null) return;
    try {
      await _db.insertFoodItem(item);
    } catch (e) {
      debugPrint('Local food catalogue cache update skipped: $e');
    }
  }

  Future<void> _syncRemoteVendorFoodItems() async {
    if (_authToken == null ||
        _currentUser?.id == null ||
        _currentUser?.role != 'VENDOR') {
      return;
    }

    try {
      final response = await http.get(
        Uri.parse('$_laravelBaseUrl/api/vendor/my-menu'),
        headers: {
          'Accept': 'application/json',
          'Authorization': 'Bearer $_authToken',
        },
      ).timeout(const Duration(seconds: 10));

      if (response.statusCode != 200 || response.body.isEmpty) return;
      final decoded = jsonDecode(response.body);
      if (decoded is! List) return;

      for (final raw in decoded) {
        if (raw is! Map) continue;
        try {
          await _upsertLocalFoodItem(
              FoodItem.fromJson(Map<String, dynamic>.from(raw)));
        } catch (e) {
          debugPrint('Skipping malformed vendor menu item: $e');
        }
      }
    } catch (e) {
      debugPrint('Vendor menu sync skipped: $e');
    }
  }

  Future<void> refreshAllData() async {
    // The Laravel database is the source of truth for the live menu.
    // Keep SQLite as an offline cache, but always synchronize food items
    // when the API is reachable so vendor/kiosk views never depend on stale
    // local demo data.
    final remoteFoodItems = await _syncRemoteFoodItems();
    await _syncRemoteVendorFoodItems();

    if (_authToken != null) {
      await fetchAndCacheFeedback();
      await fetchAndCacheAuditLogs();
    }

    _allVendors = await _db.getAllVendors();
    // Prefer the live Laravel catalogue whenever it is available.
    // Fall back to SQLite only when the API cannot be reached.
    _allFoodItems = remoteFoodItems.isNotEmpty
        ? remoteFoodItems
        : await _db.getAllFoodItems();
    _allOrders = await _db.getAllOrders();
    _allFeedback = await _db.getAllFeedback();
    _auditLogs = await _db.getAllLogs();

    if (_currentUser != null) {
      if (_currentUser!.role == 'STUDENT') {
        final remoteOrders =
            await fetchAndCacheStudentOrders(_currentUser!.id!);
        // Prefer the live Laravel orders. SQLite remains an offline fallback
        // only when the server cannot be reached.
        _customerOrders = remoteOrders.isNotEmpty
            ? remoteOrders
            : await _db.getOrdersForCustomer(_currentUser!.id!);
        await fetchPurchasedVendors();
      } else if (_currentUser!.role == 'VENDOR') {
        _vendorOrders = await _db.getOrdersForVendor(_currentUser!.id!);
        final remoteVendorItems = _allFoodItems
            .where((item) => item.vendorId == _currentUser!.id!)
            .toList();
        _vendorFoodItems = remoteVendorItems.isNotEmpty
            ? remoteVendorItems
            : await _db.getFoodItemsByVendor(_currentUser!.id!);
        _vendorFeedback = await _db.getFeedbackForVendor(_currentUser!.id!);
      }
    }

    // Every page reads `customerWalletBalance` from the authenticated user.
    // Re-sync that user from the API so wallet changes made on the server
    // (checkouts, top-ups, admin adjustments) are visible on all pages.
    await _syncCurrentUserFromServer();
    notifyListeners();
  }

  /// Refresh the authenticated user's profile (notably the wallet balance)
  /// from `/api/me`. The server is the single source of truth for money held.
  Future<void> _syncCurrentUserFromServer() async {
    if (_authToken == null || _authToken!.isEmpty || _currentUser == null) {
      return;
    }
    try {
      final response = await http
          .get(
            Uri.parse('$_laravelBaseUrl/api/me'),
            headers: {
              'Accept': 'application/json',
              'Authorization': 'Bearer $_authToken',
            },
          )
          .timeout(const Duration(seconds: 8));

      if (response.statusCode != 200 || response.body.isEmpty) return;
      final decoded = jsonDecode(response.body);
      final raw = decoded is Map ? decoded['user'] : null;
      if (raw is Map) {
        final fresh = User.fromJson(Map<String, dynamic>.from(raw));
        if (_currentUser != null) {
          _currentUser = _currentUser!.copyWith(balance: fresh.balance);
        }
      }
    } catch (e) {
      debugPrint('Current user balance sync skipped: $e');
    }
  }

  Future<void> _seedDatabaseIfEmpty() async {
    // Demo identities/catalogue are development fixtures only. Production
    // builds must never manufacture accounts or menu records on the device.
    if (!kDebugMode) {
      return;
    }

    try {
      final userCount = await _db.getUserCount();
      if (userCount > 0) return;

      debugPrint(
          "Flutter local database is empty! Beginning Ghana ATU seed procedure...");

      // 1. Seed Student users
      final stud1Id = await _db.insertUser(User(
        username: "student",
        passwordHash: _localCacheCredentialHash("1234"),
        role: "STUDENT",
        fullName: "Daniel Mensah",
        info: "ATU-2024-D45",
      ));
      final stud2Id = await _db.insertUser(User(
        username: "student2",
        passwordHash: _localCacheCredentialHash("1234"),
        role: "STUDENT",
        fullName: "Abena Osei",
        info: "ATU-2025-S12",
      ));

      // 2. Seed Vendor profiles
      final v1Id = await _db.insertUser(User(
        id: 10,
        username: "maryjoint",
        passwordHash: _localCacheCredentialHash("1111"),
        role: "VENDOR",
        fullName: "Mary Joint",
        info: "Auntie Mary Special",
      ));
      final v2Id = await _db.insertUser(User(
        id: 11,
        username: "atkitch",
        passwordHash: _localCacheCredentialHash("2222"),
        role: "VENDOR",
        fullName: "Kofi Local Kitchen",
        info: "ATU Local Hub",
      ));
      final v3Id = await _db.insertUser(User(
        id: 12,
        username: "snackbag",
        passwordHash: _localCacheCredentialHash("3333"),
        role: "VENDOR",
        fullName: "Bakery & Treats",
        info: "ATU Snack Corner",
      ));

      // 3. Seed Admin profile
      await _db.insertUser(User(
        id: 99,
        username: "admin",
        passwordHash: _localCacheCredentialHash("admin123"),
        role: "ADMIN",
        fullName: "Dr. Emmanuel Kaku",
        info: "ATU Quality Assurance",
      ));

      // 4. Seed Delicious Accra Technical University specialties
      await _db.insertFoodItem(FoodItem(
        vendorId: v1Id,
        name: "ATU Chicken Jollof Rice",
        price: 25.0,
        category: "Lunch Specials",
        imageUrl: "",
        description:
            "Classic aromatic rice stewed with authentic Ghanaian tomato sauce, served with seasoned fried chicken salad & shito.",
      ));
      await _db.insertFoodItem(FoodItem(
        vendorId: v1Id,
        name: "Zesty Ginger Sobolo",
        price: 10.0,
        category: "Drinks",
        imageUrl: "",
        description:
            "Refreshing chilled local hibiscus flower drink brewed with fresh ginger, pineapple peels, and sweetener.",
      ));
      await _db.insertFoodItem(FoodItem(
        vendorId: v2Id,
        name: "Waakye Supreme",
        price: 30.0,
        category: "Traditional",
        imageUrl: "",
        description:
            "A student favorite! Local black-eyed peas boiled with rice and millet stalks. Accompanying boiled egg, spiced gari, talia, and hot wele shito.",
      ));
      await _db.insertFoodItem(FoodItem(
        vendorId: v2Id,
        name: "Fufu & Goat Light Soup",
        price: 35.0,
        category: "Traditional",
        imageUrl: "",
        description:
            "Rich Ghanaian fufu pounded from fresh cassava and green plantains, submerged in aromatic goat meat soup.",
      ));
      await _db.insertFoodItem(FoodItem(
        vendorId: v3Id,
        name: "Savoury Meat Pie",
        price: 15.0,
        category: "Snacks",
        imageUrl: "",
        description:
            "Crispy, flaky puff pastry loaded with moist, cooked mince beef seasoning.",
      ));

      // 5. Seed historical orders and ratings
      final o1Id = await _db.insertOrder(Order(
        customerId: stud1Id,
        vendorId: v1Id,
        foodItemId: 1,
        foodName: "ATU Chicken Jollof Rice",
        quantity: 1,
        unitPrice: 25.0,
        totalPrice: 25.0,
        orderTimestamp: DateTime.now().millisecondsSinceEpoch - 172800000,
        status: "COMPLETED",
        pickupPin: "4444",
      ));

      final o2Id = await _db.insertOrder(Order(
        customerId: stud2Id,
        vendorId: v1Id,
        foodItemId: 2,
        foodName: "Zesty Ginger Sobolo",
        quantity: 2,
        unitPrice: 10.0,
        totalPrice: 20.0,
        orderTimestamp: DateTime.now().millisecondsSinceEpoch - 86400000,
        status: "COMPLETED",
        pickupPin: "5555",
      ));

      // 6. Seed reviews
      await _db.insertFeedback(Feedback(
        orderId: o1Id,
        vendorId: v1Id,
        customerId: stud1Id,
        ratingFoodQuality: 5,
        ratingCleanliness: 4,
        ratingServiceSpeed: 4,
        ratingPriceValue: 4,
        comment:
            "The chicken was very delicious and juicy! A bit crowded but service was quite neat.",
        timestamp: DateTime.now().millisecondsSinceEpoch - 172800000,
      ));

      await _db.insertFeedback(Feedback(
        orderId: o2Id,
        vendorId: v1Id,
        customerId: stud2Id,
        ratingFoodQuality: 4,
        ratingCleanliness: 5,
        ratingServiceSpeed: 5,
        ratingPriceValue: 5,
        comment:
            "Chilled sobolo was exactly what I needed after lectures. Extremely clean booth!",
        timestamp: DateTime.now().millisecondsSinceEpoch - 86400000,
      ));

      // Seed audit log
      await _db.insertAuditLog(AuditLog(
        userId: 99,
        action: "SYSTEM_INIT",
        details:
            "Database seed initialised with traditional Accra Technical University menus.",
        timestamp: DateTime.now().millisecondsSinceEpoch,
      ));
    } catch (e) {
      debugPrint("Error seeding database: $e");
    }
  }

  // ==========================================
  // STUDENT ORDER READY NOTIFICATIONS
  // ==========================================
  void _startReadyOrderPolling() {
    _readyPollingTimer?.cancel();
    _readyPollingTimer = Timer.periodic(const Duration(seconds: 15), (_) {
      pollReadyOrders();
    });
    pollReadyOrders();
  }

  Future<void> pollReadyOrders() async {
    if (_authToken == null || _currentUser?.role != 'STUDENT') return;
    try {
      final url = Uri.parse('$_laravelBaseUrl/api/customer/orders/poll-ready');
      final response = await http.get(url, headers: {
        'Accept': 'application/json',
        'Authorization': 'Bearer $_authToken',
      }).timeout(const Duration(seconds: 8));

      if (response.statusCode != 200 || response.body.isEmpty) return;
      final decoded = jsonDecode(response.body);
      if (decoded is! Map) return;
      final alerts = decoded['ready_alerts'];
      if (alerts is! List) return;

      var changed = false;
      for (final raw in alerts) {
        if (raw is! Map) continue;
        final id = int.tryParse(raw['order_id']?.toString() ?? '');
        if (id == null || _announcedReadyOrders.contains(id)) continue;

        _announcedReadyOrders.add(id);
        final title = raw['title']?.toString() ?? 'Order Ready for Pickup! 🍽️';
        final body =
            raw['body']?.toString() ?? 'Your order #$id is ready for pickup.';
        _liveAlerts.insert(0, '$title $body');
        if (_liveAlerts.length > 5) _liveAlerts.removeLast();
        changed = true;
      }

      if (changed) notifyListeners();
    } catch (e) {
      debugPrint('Ready-order polling skipped: $e');
    }
  }

  // ==========================================
  // TRANSACTION / AUTH ACTIONS
  // ==========================================

  Timer? _vendorOrderPollingTimer;

  void startVendorOrderPolling() {
    _vendorOrderPollingTimer?.cancel();
    _vendorOrderPollingTimer = Timer.periodic(const Duration(seconds: 10), (_) {
      if (_currentUser?.role == 'VENDOR' || _currentUser?.role == 'ADMIN') {
        _refreshVendorOrdersSilently();
      }
    });
  }

  Future<void> _refreshVendorOrdersSilently() async {
    if (_authToken == null || _currentUser?.id == null) return;
    try {
      final url = Uri.parse('$_laravelBaseUrl/api/vendor/my-orders');
      final response = await http.get(url, headers: {
        'Accept': 'application/json',
        'Authorization': 'Bearer $_authToken',
      }).timeout(const Duration(seconds: 8));
      if (response.statusCode != 200) return;
      final value = jsonDecode(response.body);
      if (value is List) {
        _vendorOrders = value
            .whereType<Map>()
            .map((e) => Order.fromJson(Map<String, dynamic>.from(e)))
            .toList();
        notifyListeners();
      }
    } catch (e) {
      debugPrint('Vendor order refresh skipped: $e');
    }
  }

  Future<void> restoreSession() async {
    final token = await SecureSessionStore.token();
    if (token == null || token.isEmpty) return;
    _authToken = token;
    try {
      final url = Uri.parse('$_laravelBaseUrl/api/me');
      final response = await http.get(
        url,
        headers: {
          'Accept': 'application/json',
          'Authorization': 'Bearer $token',
        },
      ).timeout(const Duration(seconds: 10));

      if (response.statusCode != 200) {
        _authToken = null;
        await SecureSessionStore.clear();
        return;
      }

      final decoded = jsonDecode(response.body) as Map<String, dynamic>;
      _currentUser =
          User.fromJson(Map<String, dynamic>.from(decoded['user'] ?? {}));

      // A browser refresh restores the session token, but it does not restore
      // the provider's in-memory catalogue/order state. Load the same live
      // data used immediately after login so a returning student never sees
      // an empty menu simply because the page was refreshed.
      try {
        await refreshAllData();
      } catch (e) {
        debugPrint('Restored-session data refresh skipped: $e');
      }

      if (_currentUser?.role == 'STUDENT') _startReadyOrderPolling();
      if (_currentUser?.role == 'VENDOR' || _currentUser?.role == 'ADMIN') {
        startVendorOrderPolling();
      }
    } catch (e) {
      debugPrint('Session restore failed: $e');
      _authToken = null;
      await SecureSessionStore.clear();
    }
  }

  Future<bool> loginUser(String username, String pinCode) async {
    _isLoading = true;
    _loginError = null;
    _authToken = null;
    _requiresTwoFactor = false;
    notifyListeners();

    final normalizedUsername = username.trim();
    if (normalizedUsername.isEmpty || pinCode.trim().length < 4) {
      _loginError = 'Username and a valid PIN are required.';
      _isLoading = false;
      notifyListeners();
      return false;
    }

    // Use package:http so authentication works on Web, Linux, Android and iOS.
    try {
      final loginUrl = Uri.parse('$_laravelBaseUrl/api/login');
      final response = await http
          .post(
            loginUrl,
            headers: const {
              'Content-Type': 'application/json',
              'Accept': 'application/json',
            },
            body: jsonEncode({
              'username': normalizedUsername,
              'pin': pinCode,
            }),
          )
          .timeout(const Duration(seconds: 10));

      Map<String, dynamic> decoded = {};
      if (response.body.isNotEmpty) {
        final value = jsonDecode(response.body);
        if (value is Map) {
          decoded = Map<String, dynamic>.from(value);
        }
      }

      if (response.statusCode == 200 && decoded['requires_2fa'] == true) {
        _requiresTwoFactor = true;
        _loginError =
            decoded['message']?.toString() ?? 'Verification code required.';
        _isLoading = false;
        notifyListeners();
        return false;
      }

      if (response.statusCode == 200) {
        final payload = decoded['user'] is Map
            ? Map<String, dynamic>.from(decoded['user'] as Map)
            : decoded;
        final remoteUser = User.fromJson(payload).copyWith(
          username: normalizedUsername,
          passwordHash: _localCacheCredentialHash(pinCode),
        );
        _currentUser = remoteUser;
        _authToken =
            decoded['token']?.toString() ?? response.headers['x-auth-token'];

        if (_authToken != null && _authToken!.isNotEmpty) {
          await SecureSessionStore.save(
              token: _authToken!, username: normalizedUsername);
        }

        try {
          if (remoteUser.id != null) {
            await _db.insertUser(remoteUser);
          }
        } catch (_) {
          // Local cache failure must not invalidate a successful remote login.
        }

        try {
          await refreshAllData();
        } catch (e) {
          // Login is already authenticated. Keep the session even if optional
          // background synchronization is unavailable.
          debugPrint('Post-login data refresh skipped: $e');
        }

        _isLoading = false;
        if (_currentUser?.role == 'STUDENT') _startReadyOrderPolling();
        if (_currentUser?.role == 'VENDOR' || _currentUser?.role == 'ADMIN') {
          startVendorOrderPolling();
        }
        notifyListeners();
        return true;
      }

      if (response.statusCode == 400 ||
          response.statusCode == 401 ||
          response.statusCode == 403 ||
          response.statusCode == 422) {
        _loginError =
            decoded['message']?.toString() ?? 'Invalid username or PIN.';
        _isLoading = false;
        notifyListeners();
        return false;
      }

      _loginError = decoded['message']?.toString() ?? 'Unable to authenticate.';
    } catch (e) {
      debugPrint('Laravel login request failed: $e');
      _loginError =
          'Unable to authenticate. Please check the server connection.';
    }

    _isLoading = false;
    notifyListeners();
    return false;
  }

  Future<bool> verifyTwoFactor(String username, String code) async {
    _isLoading = true;
    _loginError = null;
    notifyListeners();
    try {
      final url = Uri.parse('$_laravelBaseUrl/api/login/2fa');
      final res = await http
          .post(
            url,
            headers: const {
              'Content-Type': 'application/json',
              'Accept': 'application/json',
            },
            body: jsonEncode({
              'username': username.trim(),
              'code': code.trim(),
            }),
          )
          .timeout(const Duration(seconds: 10));

      final decoded = res.body.isNotEmpty
          ? Map<String, dynamic>.from(jsonDecode(res.body) as Map)
          : <String, dynamic>{};

      if (res.statusCode == 200) {
        final payload = decoded['user'] is Map
            ? Map<String, dynamic>.from(decoded['user'])
            : decoded;
        _currentUser = User.fromJson(payload);
        _authToken =
            decoded['token']?.toString() ?? res.headers['x-auth-token'];
        _requiresTwoFactor = false;
        if (_authToken != null && _authToken!.isNotEmpty) {
          await SecureSessionStore.save(
              token: _authToken!, username: username.trim());
        }
        _isLoading = false;
        notifyListeners();
        try {
          await refreshAllData();
        } catch (e) {
          debugPrint('Post-2FA data refresh skipped: $e');
        }
        return true;
      }

      _loginError = decoded['message']?.toString() ?? 'Verification failed.';
    } catch (e) {
      debugPrint('2FA request failed: $e');
      _loginError = 'Unable to verify the code. Please try again.';
    }
    _isLoading = false;
    notifyListeners();
    return false;
  }

  Future<bool> requestPasswordReset(String username) async {
    try {
      await _authRequest(
          'POST', 'password/forgot', {'username': username.trim()});
      return true;
    } catch (e) {
      _loginError = e.toString();
      notifyListeners();
      return false;
    }
  }

  Future<bool> resetPassword(String username, String code, String pin) async {
    try {
      await _authRequest('POST', 'password/reset',
          {'username': username.trim(), 'code': code.trim(), 'pin': pin});
      return true;
    } catch (e) {
      _loginError = e.toString();
      notifyListeners();
      return false;
    }
  }

  Future<bool> logoutUser() async {
    try {
      if (_authToken != null) await _authRequest('POST', 'logout', {});
    } catch (_) {}
    _authToken = null;
    _currentUser = null;
    _requiresTwoFactor = false;
    await SecureSessionStore.clear();
    notifyListeners();
    return true;
  }

  Future<dynamic> _authRequest(
      String method, String path, Map<String, dynamic> body) async {
    final url = Uri.parse('$_laravelBaseUrl/api/$path');
    final headers = <String, String>{
      'Content-Type': 'application/json',
      'Accept': 'application/json',
    };
    if (_authToken != null && _authToken!.isNotEmpty) {
      headers['Authorization'] = 'Bearer $_authToken';
    }

    final encodedBody = jsonEncode(body);
    late http.Response response;
    switch (method.toUpperCase()) {
      case 'GET':
        response = await http.get(url, headers: headers).timeout(
              const Duration(seconds: 10),
            );
        break;
      case 'POST':
        response =
            await http.post(url, headers: headers, body: encodedBody).timeout(
                  const Duration(seconds: 10),
                );
        break;
      case 'PUT':
        response =
            await http.put(url, headers: headers, body: encodedBody).timeout(
                  const Duration(seconds: 10),
                );
        break;
      case 'PATCH':
        response =
            await http.patch(url, headers: headers, body: encodedBody).timeout(
                  const Duration(seconds: 10),
                );
        break;
      case 'DELETE':
        response =
            await http.delete(url, headers: headers, body: encodedBody).timeout(
                  const Duration(seconds: 10),
                );
        break;
      default:
        throw ArgumentError('Unsupported HTTP method: $method');
    }

    final decoded = response.body.isNotEmpty ? jsonDecode(response.body) : {};
    if (response.statusCode < 200 || response.statusCode >= 300) {
      final msg =
          decoded is Map ? decoded['message']?.toString() : 'Request failed.';
      throw Exception(msg ?? 'Request failed.');
    }
    return decoded;
  }

  Future<bool> registerUser({
    required String username,
    required String pinCode,
    required String role,
    required String fullName,
    required String info,
    String? email,
  }) async {
    _isLoading = true;
    _registrationSuccess = false;
    _loginError = null;
    notifyListeners();

    final normalizedUsername = username.trim();
    final normalizedRole = role.trim().toUpperCase();
    final normalizedName = fullName.trim();
    final normalizedInfo = info.trim().isEmpty
        ? (normalizedRole == 'STUDENT'
            ? 'ATU-2026-STUDENT'
            : 'ATU Local Vendor')
        : info.trim();

    if (normalizedUsername.isEmpty ||
        pinCode.trim().length < 4 ||
        normalizedName.isEmpty) {
      _loginError = 'All fields are required. PIN must be 4+ digits.';
      _isLoading = false;
      notifyListeners();
      return false;
    }

    if (normalizedRole != 'STUDENT') {
      _loginError = 'Only student registration is supported here.';
      _isLoading = false;
      notifyListeners();
      return false;
    }

    try {
      final registerUrl = Uri.parse('$_laravelBaseUrl/api/customer/register');
      final response = await http
          .post(
            registerUrl,
            headers: const {
              'Content-Type': 'application/json',
              'Accept': 'application/json',
            },
            body: jsonEncode({
              'username': normalizedUsername,
              'pin': pinCode,
              'pin_confirmation': pinCode,
              'fullName': normalizedName,
              'info': normalizedInfo,
              'email': email?.trim().toLowerCase() ?? '',
            }),
          )
          .timeout(const Duration(seconds: 10));

      final decoded = response.body.isNotEmpty
          ? Map<String, dynamic>.from(jsonDecode(response.body) as Map)
          : <String, dynamic>{};

      if (response.statusCode == 201 || response.statusCode == 200) {
        final payload = decoded['user'] is Map
            ? Map<String, dynamic>.from(decoded['user'] as Map)
            : decoded;
        final remoteUser = User.fromJson(payload).copyWith(
          passwordHash: _localCacheCredentialHash(pinCode),
        );
        if (remoteUser.id != null) {
          await _db.insertUser(remoteUser);
        }
        _registrationSuccess = true;
        _isLoading = false;
        notifyListeners();
        return true;
      }

      _loginError = decoded['message']?.toString() ??
          'Registration details are not valid.';
    } catch (e) {
      debugPrint('Student registration request failed: $e');
      _loginError =
          'Unable to create your account. Please check the server connection.';
    }

    _isLoading = false;
    notifyListeners();
    return false;
  }

  Future<bool> requestEmailVerification() async {
    try {
      await _authRequest('POST', 'email/verification/request', {});
      return true;
    } catch (e) {
      _loginError = e.toString();
      notifyListeners();
      return false;
    }
  }

  Future<bool> verifyEmail(String code) async {
    try {
      final result = await _authRequest(
          'POST', 'email/verification/verify', {'code': code.trim()});
      if (result is Map && result['user'] is Map) {
        _currentUser = User.fromJson(Map<String, dynamic>.from(result['user']));
      }
      notifyListeners();
      return true;
    } catch (e) {
      _loginError = e.toString();
      notifyListeners();
      return false;
    }
  }

  Future<List<dynamic>> securityActivity({int limit = 50}) async {
    final result = await _authRequest(
        'GET', 'admin/security/activity?limit=${limit.clamp(1, 100)}', {});
    if (result is Map && result['data'] is List) {
      return List<dynamic>.from(result['data']);
    }
    return const [];
  }

  Future<void> logOut() async {
    final oldToken = _authToken;
    final oldUser = _currentUser;
    try {
      if (oldToken != null && oldToken.isNotEmpty) {
        final url = Uri.parse('$_laravelBaseUrl/api/logout');
        final client = HttpClient()
          ..connectionTimeout = const Duration(seconds: 4);
        try {
          final request = await client.postUrl(url);
          request.headers
            ..set(HttpHeaders.acceptHeader, 'application/json')
            ..set(HttpHeaders.authorizationHeader, 'Bearer $oldToken');
          await request.close();
        } finally {
          client.close(force: true);
        }
      }
    } catch (_) {}
    if (oldUser?.id != null) {
      try {
        await _db.insertAuditLog(AuditLog(
            userId: oldUser!.id!,
            action: 'USER_LOGOUT',
            details: 'User logged out securely.',
            timestamp: DateTime.now().millisecondsSinceEpoch));
      } catch (_) {}
    }
    _readyPollingTimer?.cancel();
    _readyPollingTimer = null;
    _announcedReadyOrders.clear();
    _authToken = null;
    _currentUser = null;
    _loginError = null;
    _registrationSuccess = false;
    _customerOrders = [];
    _purchasedVendors = [];
    _vendorOrders = [];
    _vendorFoodItems = [];
    _vendorFeedback = [];
    _realAdminUser = null;
    _isAdminActing = false;
    await SecureSessionStore.clear();
    notifyListeners();
  }

  // ==========================================
  // ADMIN PORTAL OPERATIONS (CRUD & ACCESS)
  // ==========================================

  User? _realAdminUser;
  bool _isAdminActing = false;
  bool get isAdminActing => _isAdminActing;

  Future<List<User>> getAllUsers() async {
    final db = await _db.database;
    final maps = await db.query('users');
    return maps.map(User.fromMap).toList();
  }

  Future<bool> addVendor({
    required String username,
    required String pinCode,
    required String fullName,
    required String info,
  }) async {
    _isLoading = true;
    notifyListeners();

    try {
      final existing = await _db.getUserByUsername(username);
      if (existing != null) {
        _isLoading = false;
        notifyListeners();
        return false;
      }

      final newVendor = User(
        username: username,
        passwordHash: _localCacheCredentialHash(pinCode),
        role: "VENDOR",
        fullName: fullName,
        info: info,
      );

      await _db.insertUser(newVendor);
      if (_currentUser != null) {
        await _db.insertAuditLog(AuditLog(
          userId: _currentUser!.id!,
          action: "VENDOR_ADDED",
          details: "Vendor '$fullName' added by Admin.",
          timestamp: DateTime.now().millisecondsSinceEpoch,
        ));
      }
      await refreshAllData();
      _isLoading = false;
      notifyListeners();
      return true;
    } catch (e) {
      _isLoading = false;
      notifyListeners();
      return false;
    }
  }

  Future<bool> updateVendor(User vendor, String? newPinCode) async {
    _isLoading = true;
    notifyListeners();

    try {
      User updatedVendor = vendor;
      if (newPinCode != null && newPinCode.isNotEmpty) {
        updatedVendor = vendor.copyWith(
            passwordHash: _localCacheCredentialHash(newPinCode));
      }
      await _db.updateUser(updatedVendor);

      if (_currentUser != null) {
        await _db.insertAuditLog(AuditLog(
          userId: _currentUser!.id!,
          action: "VENDOR_UPDATED",
          details: "Vendor '${vendor.fullName}' updated by Admin.",
          timestamp: DateTime.now().millisecondsSinceEpoch,
        ));
      }
      await refreshAllData();
      _isLoading = false;
      notifyListeners();
      return true;
    } catch (e) {
      _isLoading = false;
      notifyListeners();
      return false;
    }
  }

  Future<bool> deleteVendor(int vendorId) async {
    _isLoading = true;
    notifyListeners();

    try {
      final vendor =
          _allVendors.firstWhere((element) => element.id == vendorId);
      await _db.deleteUser(vendorId);

      if (_currentUser != null) {
        await _db.insertAuditLog(AuditLog(
          userId: _currentUser!.id!,
          action: "VENDOR_DELETED",
          details: "Vendor '${vendor.fullName}' deleted by Admin.",
          timestamp: DateTime.now().millisecondsSinceEpoch,
        ));
      }
      await refreshAllData();
      _isLoading = false;
      notifyListeners();
      return true;
    } catch (e) {
      _isLoading = false;
      notifyListeners();
      return false;
    }
  }

  void startImpersonation(User targetUser) async {
    if (_currentUser?.role == 'ADMIN' && !_isAdminActing) {
      _realAdminUser = _currentUser;
    }
    _currentUser = targetUser;
    _isAdminActing = true;

    // Refresh user's lists specifically
    if (targetUser.role == 'STUDENT') {
      _customerOrders = await _db.getOrdersForCustomer(targetUser.id!);
    } else if (targetUser.role == 'VENDOR') {
      _vendorOrders = await _db.getOrdersForVendor(targetUser.id!);
      _vendorFoodItems = await _db.getFoodItemsByVendor(targetUser.id!);
      _vendorFeedback = await _db.getFeedbackForVendor(targetUser.id!);
    }
    notifyListeners();
  }

  void stopImpersonation() async {
    if (_realAdminUser != null) {
      _currentUser = _realAdminUser;
      _isAdminActing = false;
      _realAdminUser = null;
      await refreshAllData();
    }
  }

  // ==========================================
  // STUDENT WORKFLOW TRANSACTIONS
  // ==========================================

  void rechargeWallet(double amount) async {
    if (_currentUser != null) {
      // Re-sync the authoritative balance first, then apply the credit so a
      // legacy caller never shows an overwritten value.
      await refreshAllData();
      _currentUser = _currentUser!.copyWith(
        balance: _currentUser!.balance + amount,
      );
      await _db.insertAuditLog(AuditLog(
        userId: _currentUser!.id!,
        action: "WALLET_CREDIT",
        details:
            "Securely loaded GH₵ ${amount.toStringAsFixed(2)} via Mobile Money Gateway.",
        timestamp: DateTime.now().millisecondsSinceEpoch,
      ));
      notifyListeners();
    }
  }

  Future<bool> placeOrder(
      FoodItem foodItem, int quantity, bool useWallet) async {
    if (_currentUser == null) return false;

    final requiredSum = foodItem.price * quantity;
    if (useWallet) {
      if (_currentUser!.balance < requiredSum) {
        return false;
      }
      _currentUser = _currentUser!.copyWith(
        balance: _currentUser!.balance - requiredSum,
      );
    }

    // Generate a secure 4-digit numeric pickup PIN
    final pickupPin = (1000 + (9000 * (1.0 - 0.1))).toInt().toString();

    final order = Order(
      customerId: _currentUser!.id!,
      vendorId: foodItem.vendorId,
      foodItemId: foodItem.id!,
      foodName: foodItem.name,
      quantity: quantity,
      unitPrice: foodItem.price,
      totalPrice: requiredSum,
      orderTimestamp: DateTime.now().millisecondsSinceEpoch,
      status: "Order Received",
      pickupPin: pickupPin,
    );

    final orderId = await _db.insertOrder(order);
    await _db.insertAuditLog(AuditLog(
      userId: _currentUser!.id!,
      action: "ORDER_CREATED",
      details:
          "Created order #$orderId of ${foodItem.name} x$quantity. Wallet Pay: $useWallet.",
      timestamp: DateTime.now().millisecondsSinceEpoch,
    ));

    // Try to sync with Laravel backend
    int remoteOrderId = orderId;
    try {
      final postUrl = Uri.parse("$_laravelBaseUrl/api/orders");
      final client = HttpClient();
      client.connectionTimeout = const Duration(seconds: 3);
      final request = await client.postUrl(postUrl);
      request.headers.add("Content-Type", "application/json");
      request.headers.add("Accept", "application/json");
      if (_authToken != null && _authToken!.isNotEmpty) {
        request.headers.add("Authorization", "Bearer $_authToken");
      }

      final payload = json.encode({
        'customer_id': _currentUser!.id!,
        'student_id': _currentUser!.id!,
        'vendor_id': foodItem.vendorId,
        'food_item_id': foodItem.id!,
        'menu_item_id': foodItem.id!,
        'food_name': foodItem.name,
        'quantity': quantity,
        'unit_price': foodItem.price,
        'total_price': requiredSum,
      });
      request.add(utf8.encode(payload));

      final response = await request.close();
      if (response.statusCode == 201) {
        final body = await response.transform(utf8.decoder).join();
        final decoded = json.decode(body);
        if (decoded['id'] != null) {
          remoteOrderId = decoded['id'];
          debugPrint(
              "Order synced with Laravel backend successfully. Real order ID: $remoteOrderId");
        }
      }
    } catch (e) {
      debugPrint("Laravel sync unavailable ($e). Utilizing local fallback.");
    }

    _startRealTimeTrackingSimulation(remoteOrderId);

    await refreshAllData();
    return true;
  }

  // Standard Backend connection base URL. Resolves at call time so the
  // in-app "API server" setting (local USB / Wi-Fi / deployed URL) applies
  // immediately without rebuilding.
  String get _laravelBaseUrl => ServerConfig.baseUrl;
  String get laravelBaseUrl => _laravelBaseUrl;

  Future<void> updateLaravelBaseUrl(String? url) async {
    await ServerConfig.setOverride(url);
    notifyListeners();
  }

  // Remote Synchronization & Performance Metrics
  Future<void> fetchVendorPerformanceMetrics(int vendorId) async {
    // Both endpoints below are protected; without a bearer token Sanctum
    // rejects them with 401 "Unauthenticated.".
    final token = _authToken;
    if (token == null || token.isEmpty) return;

    _isFetchingRemoteMetrics = true;
    _remoteVendorMetrics = null;
    _remoteRechartsData = null;
    _remoteDailyRevenue = null;
    notifyListeners();

    try {
      // 1. Fetch performance metrics from Laravel API
      final metricsUrl =
          Uri.parse("$_laravelBaseUrl/api/vendor/performance-metrics");
      final client = HttpClient();
      client.connectionTimeout = const Duration(seconds: 4);
      final request = await client.getUrl(metricsUrl);
      request.headers.set(HttpHeaders.acceptHeader, 'application/json');
      request.headers.set(HttpHeaders.authorizationHeader, 'Bearer $token');
      final response = await request.close();

      if (response.statusCode == 200) {
        final body = await response.transform(utf8.decoder).join();
        final decoded = json.decode(body);
        if (decoded['success'] == true && decoded['performance'] != null) {
          final perfList = decoded['performance'] as List;
          final vendorMetrics = perfList.firstWhere(
            (item) => item['vendor_id'] == vendorId,
            orElse: () => null,
          );
          if (vendorMetrics != null) {
            _remoteVendorMetrics = Map<String, dynamic>.from(vendorMetrics);
          }
        }
      }

      // 2. Fetch Recharts timeline analytics from Laravel API
      final rechartsUrl = Uri.parse(
          "$_laravelBaseUrl/api/vendor/recharts-sales?vendor_id=$vendorId");
      final rRequest = await client.getUrl(rechartsUrl);
      rRequest.headers.set(HttpHeaders.acceptHeader, 'application/json');
      rRequest.headers.set(HttpHeaders.authorizationHeader, 'Bearer $token');
      final rResponse = await rRequest.close();
      if (rResponse.statusCode == 200) {
        final rBody = await rResponse.transform(utf8.decoder).join();
        final rDecoded = json.decode(rBody);
        if (rDecoded['success'] == true &&
            rDecoded['data'] != null &&
            rDecoded['data']['by_date'] != null) {
          _remoteRechartsData = rDecoded['data']['by_date'] as List;
        }
      }

      // 3. Fetch today's / daily revenue for the vendor
      final dailyUrl =
          Uri.parse("$_laravelBaseUrl/api/vendor/daily-revenue");
      final dRequest = await client.getUrl(dailyUrl);
      dRequest.headers.set(HttpHeaders.acceptHeader, 'application/json');
      dRequest.headers.set(HttpHeaders.authorizationHeader, 'Bearer $token');
      final dResponse = await dRequest.close();
      if (dResponse.statusCode == 200) {
        final dBody = await dResponse.transform(utf8.decoder).join();
        final dDecoded = json.decode(dBody);
        if (dDecoded['data'] is List) {
          _remoteDailyRevenue = List<dynamic>.from(dDecoded['data']);
        }
      }
    } catch (e) {
      debugPrint("Error fetching remote performance metrics from Laravel: $e");
    } finally {
      _isFetchingRemoteMetrics = false;
      notifyListeners();
    }
  }

  // Paystack Billing API Client Methods
  Future<Map<String, dynamic>?> initializePaystackPayment({
    required double amount,
    required String email,
    required String purpose,
  }) async {
    try {
      final initUrl = Uri.parse("$_laravelBaseUrl/api/paystack/initialize");
      final client = HttpClient();
      client.connectionTimeout = const Duration(seconds: 4);
      final request = await client.postUrl(initUrl);
      request.headers.add("Content-Type", "application/json");
      request.headers.add("Accept", "application/json");
      if (_authToken != null && _authToken!.isNotEmpty) {
        request.headers.add("Authorization", "Bearer $_authToken");
      }
      request.add(utf8.encode(json.encode({
        'amount': amount,
        'email': email,
        'purpose': purpose,
      })));
      final response = await request.close();
      final body = await response.transform(utf8.decoder).join();
      final decoded = json.decode(body);
      if (response.statusCode == 200 &&
          decoded is Map &&
          decoded['success'] == true) {
        return Map<String, dynamic>.from(decoded['data'] as Map);
      }
      debugPrint("Initialize Paystack error response: $body");
    } catch (e) {
      debugPrint("Exception initializing Paystack payment: $e");
    }
    return null;
  }

  Future<bool> verifyPaystackPayment({
    required String reference,
    required double amount,
    required String purpose,
  }) async {
    try {
      final verifyUrl = Uri.parse(
        "$_laravelBaseUrl/api/paystack/verify/$reference?amount=$amount&purpose=$purpose",
      );
      final client = HttpClient();
      client.connectionTimeout = const Duration(seconds: 4);
      final request = await client.getUrl(verifyUrl);
      request.headers.add("Accept", "application/json");
      if (_authToken != null && _authToken!.isNotEmpty) {
        request.headers.add("Authorization", "Bearer $_authToken");
      }
      final response = await request.close();
      final body = await response.transform(utf8.decoder).join();
      final decoded = json.decode(body);
      if (response.statusCode == 200 &&
          decoded is Map &&
          decoded['success'] == true) {
        if (purpose == 'WALLET_TOPUP') {
          // The server has already verified and credited the wallet (users.balance).
          // Mirror the credit locally so every page shows it even if the
          // subsequent /api/me re-sync is unavailable.
          if (_currentUser != null) {
            _currentUser = _currentUser!.copyWith(
              balance: _currentUser!.balance + amount,
            );
          }
          await refreshAllData();
        }
        return true;
      }
      debugPrint("Verify Paystack error response: $body");
    } catch (e) {
      debugPrint("Exception verifying Paystack payment: $e");
    }
    return false;
  }

  void _startRealTimeTrackingSimulation(int orderId) {
    final sseUrl =
        Uri.parse("$_laravelBaseUrl/api/orders/$orderId/tracking?stream=1");

    // Attempt real-time SSE stream connection to the Laravel backend
    HttpClient().getUrl(sseUrl).then((HttpClientRequest request) {
      request.headers.add("Accept", "text/event-stream");
      request.headers.add("X-Requested-With", "XMLHttpRequest");
      return request.close();
    }).then((HttpClientResponse response) {
      if (response.statusCode == 200) {
        debugPrint(
            "Successfully connected to ATU Laravel real-time SSE stream for Order #$orderId");

        response.transform(utf8.decoder).transform(const LineSplitter()).listen(
            (line) async {
          if (line.startsWith("data:")) {
            try {
              final jsonStr = line.substring(5).trim();
              final data = json.decode(jsonStr);
              final String newStatus = data['status'];
              debugPrint(
                  "Real-time stream update from Laravel for Order #$orderId: $newStatus");
              await updateOrderStatus(orderId, newStatus);
            } catch (e) {
              debugPrint("Error parsing real-time stream data: $e");
            }
          }
        }, onError: (err) {
          debugPrint(
              "Real-time stream error: $err. Falling back to local simulation.");
          _runOfflineFallbackSimulation(orderId);
        });
      } else {
        debugPrint(
            "Laravel response code is ${response.statusCode}. Falling back to simulation.");
        _runOfflineFallbackSimulation(orderId);
      }
    }).catchError((e) {
      debugPrint(
          "Could not connect to Laravel backend ($e). Running local real-time simulator.");
      _runOfflineFallbackSimulation(orderId);
    });
  }

  void _runOfflineFallbackSimulation(int orderId) {
    Stream.periodic(const Duration(seconds: 8)).take(3).listen((_) async {
      final orderList = await _db.getAllOrders();
      try {
        final order = orderList.firstWhere((o) => o.id == orderId);
        String nextStatus;
        if (order.status == 'Order Placed' ||
            order.status == 'Order Received' ||
            order.status == 'PENDING') {
          nextStatus = 'Preparing';
        } else if (order.status == 'Preparing' || order.status == 'PREPARING') {
          nextStatus = 'Ready for Pickup/Delivery';
        } else if (order.status == 'Out for Delivery' ||
            order.status == 'Ready for Pickup/Delivery' ||
            order.status == 'READY') {
          nextStatus = 'Delivered';
        } else {
          return; // Already completed or cancelled
        }
        await updateOrderStatus(orderId, nextStatus);
      } catch (e) {
        // Order deleted or not found
      }
    });
  }

  Future<void> submitOrderFeedback({
    required int orderId,
    required int vendorId,
    required int quality,
    required int cleanliness,
    required int speed,
    required int value,
    required String comment,
  }) async {
    if (_currentUser == null) return;

    final feedback = Feedback(
      orderId: orderId,
      vendorId: vendorId,
      customerId: _currentUser!.id!,
      ratingFoodQuality: quality,
      ratingCleanliness: cleanliness,
      ratingServiceSpeed: speed,
      ratingPriceValue: value,
      comment: comment,
      timestamp: DateTime.now().millisecondsSinceEpoch,
    );

    await _db.insertFeedback(feedback);

    // Sync feedback with Laravel API if authenticated
    if (_authToken != null) {
      try {
        final url = Uri.parse("$_laravelBaseUrl/api/feedback");
        final client = HttpClient();
        client.connectionTimeout = const Duration(seconds: 4);
        final request = await client.postUrl(url);
        request.headers.add("Content-Type", "application/json");
        request.headers.add("Authorization", "Bearer $_authToken");
        request.add(utf8.encode(json.encode({
          'order_id': orderId,
          'vendor_id': vendorId,
          'customer_id': _currentUser!.id,
          'food_quality': quality,
          'cleanliness': cleanliness,
          'speed': speed,
          'value': value,
          'comment': comment,
        })));
        final response = await request.close();
        if (response.statusCode == 201) {
          final body = await response.transform(utf8.decoder).join();
          final decoded = json.decode(body);
          if (decoded != null && decoded['id'] != null) {
            // Update local SQLite db with the server-generated feedback ID
            final remoteFb = Feedback(
              id: decoded['id'],
              orderId: orderId,
              vendorId: vendorId,
              customerId: _currentUser!.id!,
              ratingFoodQuality: quality,
              ratingCleanliness: cleanliness,
              ratingServiceSpeed: speed,
              ratingPriceValue: value,
              comment: comment,
              timestamp: feedback.timestamp,
            );
            await _db.insertFeedback(remoteFb);
          }
        }
      } catch (e) {
        debugPrint(
            "Campus network unstable: feedback saved to local SQLite cache only. Exception: $e");
      }
    }

    await insertAuditLog(AuditLog(
      userId: _currentUser!.id!,
      action: "FEEDBACK_POSTED",
      details: "Feedback rating logged for order #$orderId.",
      timestamp: DateTime.now().millisecondsSinceEpoch,
    ));

    await refreshAllData();
  }

  // ==========================================
  // VENDOR CONFIGURATION
  // ==========================================

  void updateVendorAnnouncement(String announcement) {
    _vendorAnnouncement = announcement.isEmpty
        ? "Serving appetizing, dynamic recipes. Check daily specials!"
        : announcement;
    notifyListeners();
  }

  Future<void> setStoreClosedState(bool isClosed) async {
    final previous = _isStoreClosed;
    _isStoreClosed = isClosed;
    notifyListeners();

    if (_authToken == null || _authToken!.isEmpty) {
      return;
    }

    try {
      final response = await http.patch(
        Uri.parse('$_laravelBaseUrl/api/vendor/status'),
        headers: {
          'Accept': 'application/json',
          'Content-Type': 'application/json',
          'Authorization': 'Bearer $_authToken',
        },
        body: jsonEncode({'is_open': !isClosed}),
      ).timeout(const Duration(seconds: 8));

      if (response.statusCode != 200) {
        _isStoreClosed = previous;
        notifyListeners();
        debugPrint('Vendor store status update failed: ${response.body}');
        return;
      }

      await refreshAllData();
    } catch (e) {
      _isStoreClosed = previous;
      notifyListeners();
      debugPrint('Vendor store status update failed: ${e}');
    }
  }

  Future<void> updateFoodAvailability(FoodItem item, bool isAvailable) async {
    final updated = item.copyWith(isAvailable: isAvailable);

    if (_authToken != null && item.id != null) {
      try {
        final response = await http
            .put(
              Uri.parse('$_laravelBaseUrl/api/food-items/${item.id}'),
              headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/json',
                'Authorization': 'Bearer $_authToken',
              },
              body: jsonEncode({'is_available': isAvailable}),
            )
            .timeout(const Duration(seconds: 10));
        if (response.statusCode != 200) {
          debugPrint('Remote availability update failed: ${response.body}');
        }
      } catch (e) {
        debugPrint('Remote availability update failed: $e');
      }
    }

    await _db.updateFoodItem(updated);
    _liveAlerts.insert(
        0,
        isAvailable
            ? "🟢 '${item.name}' is now available."
            : "🔴 '${item.name}' is temporarily unavailable.");
    if (_liveAlerts.length > 5) _liveAlerts.removeLast();
    await refreshAllData();
  }

  Future<String?> addVendorFoodItem(
    String name,
    double price,
    String category,
    String description,
  ) async {
    if (_currentUser == null) {
      return 'No vendor is currently logged in.';
    }

    final cleanName = name.trim();

    if (cleanName.isEmpty) {
      return 'Food name is required.';
    }

    if (price <= 0) {
      return 'Price must be greater than zero.';
    }

    if (_authToken == null || _authToken!.trim().isEmpty) {
      return 'Your session has expired. Please log in again.';
    }

    final vendorId = _currentUser!.id;

    if (vendorId == null) {
      return 'The logged-in vendor does not have a valid ID.';
    }

    final cleanDescription = description.trim();

    if (cleanDescription.length < 10) {
      return 'Description must be at least 10 characters.';
    }

    try {
      final url = Uri.parse('$_laravelBaseUrl/api/food-items');

      debugPrint('========================================');
      debugPrint('ADDING VENDOR FOOD ITEM');
      debugPrint('URL: $url');
      debugPrint('Vendor ID: $vendorId');
      debugPrint('Food: $cleanName');
      debugPrint('Price: $price');
      debugPrint('Category: $category');
      debugPrint('Description: $cleanDescription');
      debugPrint('========================================');

      final response = await http
          .post(
            url,
            headers: {
              'Accept': 'application/json',
              'Content-Type': 'application/json',
              'Authorization': 'Bearer $_authToken',
            },
            body: jsonEncode({
              'vendor_id': vendorId,
              'name': cleanName,
              'price': price,
              'category': category,
              'description': cleanDescription,
              'image_url': '',
              'initial_stock': 50,
              'low_stock_threshold': 10,
            }),
          )
          .timeout(const Duration(seconds: 10));

      debugPrint('ADD FOOD RESPONSE STATUS: ${response.statusCode}');
      debugPrint('ADD FOOD RESPONSE BODY: ${response.body}');

      if (response.statusCode == 201) {
        debugPrint('Food item created successfully.');

        await refreshAllData();

        return null;
      }

      try {
        if (response.body.isNotEmpty) {
          final decoded = jsonDecode(response.body);

          if (decoded is Map) {
            final errors = decoded['errors'];

            if (errors is Map) {
              final messages = <String>[];

              for (final value in errors.values) {
                if (value is List) {
                  messages.addAll(
                    value.map((item) => item.toString()),
                  );
                } else {
                  messages.add(value.toString());
                }
              }

              if (messages.isNotEmpty) {
                return messages.join('\n');
              }
            }

            final message = decoded['message'];

            if (message != null && message.toString().trim().isNotEmpty) {
              return message.toString();
            }
          }
        }
      } catch (e) {
        debugPrint('Could not decode Laravel error response: $e');
      }

      return 'Unable to add food item. Server returned ${response.statusCode}.';
    } on TimeoutException {
      debugPrint('ADD FOOD ERROR: Request timed out.');

      return 'The server took too long to respond. Please try again.';
    } catch (e) {
      debugPrint('ADD FOOD ERROR: $e');

      return 'Unable to connect to the server. Please check the backend connection.';
    }
  }

  Future<void> deleteVendorFoodItem(FoodItem item) async {
    if (item.id == null) return;
    if (_authToken != null) {
      try {
        final response = await http.delete(
          Uri.parse('$_laravelBaseUrl/api/food-items/${item.id}'),
          headers: {
            'Accept': 'application/json',
            'Authorization': 'Bearer $_authToken',
          },
        ).timeout(const Duration(seconds: 10));
        if (response.statusCode != 200) {
          debugPrint('Remote menu deletion failed: ${response.body}');
          return;
        }
      } catch (e) {
        debugPrint('Remote menu deletion failed: $e');
        return;
      }
    }
    await _db.deleteFoodItem(item.id!);
    await refreshAllData();
  }

  Future<void> updateOrderStatus(int orderId, String newStatus) async {
    await _db.updateOrderStatus(orderId, newStatus);

    // Attempt Laravel synchronization
    if (_authToken != null) {
      try {
        final statusUrl =
            Uri.parse("$_laravelBaseUrl/api/orders/$orderId/status");
        final client = HttpClient();
        client.connectionTimeout = const Duration(seconds: 3);
        final request = await client.putUrl(statusUrl);
        request.headers.add("Content-Type", "application/json");
        request.headers.add("Authorization", "Bearer $_authToken");
        request.add(utf8.encode(json.encode({
          'status': newStatus,
        })));
        final response = await request.close();
        if (response.statusCode == 200) {
          debugPrint("Order status synced with Laravel: $newStatus");
        } else {
          final body = await response.transform(utf8.decoder).join();
          debugPrint("Laravel status sync failed: $body");
        }
      } catch (e) {
        debugPrint("Exception syncing status with Laravel: $e");
      }
    }

    if (_currentUser != null) {
      await _db.insertAuditLog(AuditLog(
        userId: _currentUser!.id!,
        action: "ORDER_STATE_CHANGED",
        details: "Order #$orderId transitioned to state $newStatus.",
        timestamp: DateTime.now().millisecondsSinceEpoch,
      ));
    }
    await refreshAllData();
  }

  Future<bool> verifyAndCompletePickup(int orderId, String enteredPin) async {
    if (_currentUser == null) return false;

    final orders = await _db.getOrdersForVendor(_currentUser!.id!);
    final targetOrder = orders.firstWhere((o) => o.id == orderId);

    if (targetOrder.pickupPin == enteredPin) {
      await _db.updateOrderStatus(orderId, "Delivered");

      // Sync to Laravel if online
      if (_authToken != null) {
        try {
          final url =
              Uri.parse("$_laravelBaseUrl/api/orders/$orderId/verify-pickup");
          final client = HttpClient();
          client.connectionTimeout = const Duration(seconds: 3);
          final request = await client.postUrl(url);
          request.headers.add("Content-Type", "application/json");
          request.headers.add("Authorization", "Bearer $_authToken");
          request.add(utf8.encode(json.encode({
            'vendor_id': _currentUser!.id!,
            'pickup_pin': enteredPin,
          })));
          final response = await request.close();
          if (response.statusCode == 200) {
            debugPrint(
                "Order pickup validation synced with Laravel for #$orderId");
          }
        } catch (e) {
          debugPrint("Could not sync pickup validation with Laravel: $e");
        }
      }

      await _db.insertAuditLog(AuditLog(
        userId: _currentUser!.id!,
        action: "SECURE_PICKUP_VALIDATED",
        details:
            "Authenticity PIN verified for order #$orderId. Custody handoff certified.",
        timestamp: DateTime.now().millisecondsSinceEpoch,
      ));
      await refreshAllData();
      return true;
    }
    return false;
  }

  // Vendor quality metrics are calculated from recorded feedback.
  double getAverageRating(List<Feedback> feedbacks) {
    if (feedbacks.isEmpty) return 0.0;
    double sum = 0.0;
    for (final feedback in feedbacks) {
      sum += (feedback.ratingFoodQuality +
              feedback.ratingCleanliness +
              feedback.ratingServiceSpeed +
              feedback.ratingPriceValue) /
          4.0;
    }
    return sum / feedbacks.length;
  }

  Future<void> insertAuditLog(AuditLog log) async {
    await _db.insertAuditLog(log);
    if (_authToken != null) {
      try {
        final url = Uri.parse("$_laravelBaseUrl/api/audit-logs");
        final client = HttpClient();
        client.connectionTimeout = const Duration(seconds: 3);
        final request = await client.postUrl(url);
        request.headers.add("Content-Type", "application/json");
        request.headers.add("Authorization", "Bearer $_authToken");
        request.add(utf8.encode(json.encode({
          'user_id': log.userId,
          'action': log.action,
          'details': log.details,
        })));
        await request.close();
      } catch (e) {
        debugPrint("Exception syncing audit log: $e");
      }
    }
  }

  Future<void> fetchAndCacheFeedback() async {
    try {
      final url = Uri.parse("$_laravelBaseUrl/api/feedback");
      final client = HttpClient();
      client.connectionTimeout = const Duration(seconds: 4);
      final request = await client.getUrl(url);
      request.headers.add("Authorization", "Bearer $_authToken");
      final response = await request.close();
      if (response.statusCode == 200) {
        final body = await response.transform(utf8.decoder).join();
        final List decoded = json.decode(body);
        for (var item in decoded) {
          final fb = Feedback(
            id: item['id'],
            orderId: item['order_id'],
            vendorId: item['vendor_id'],
            customerId: item['customer_id'],
            ratingFoodQuality: item['rating_food_quality'],
            ratingCleanliness: item['rating_cleanliness'],
            ratingServiceSpeed: item['rating_service_speed'],
            ratingPriceValue: item['rating_price_value'],
            comment: item['comment'] ?? '',
            timestamp:
                item['timestamp'] ?? DateTime.now().millisecondsSinceEpoch,
          );
          await _db.insertFeedback(fb);
        }
      }
    } catch (e) {
      debugPrint(
          "Campus network unstable: cannot sync feedback from Laravel. $e");
    }
  }

  Future<void> fetchAndCacheAuditLogs() async {
    try {
      final url = Uri.parse("$_laravelBaseUrl/api/audit-logs");
      final client = HttpClient();
      client.connectionTimeout = const Duration(seconds: 4);
      final request = await client.getUrl(url);
      request.headers.add("Authorization", "Bearer $_authToken");
      final response = await request.close();
      if (response.statusCode == 200) {
        final body = await response.transform(utf8.decoder).join();
        final List decoded = json.decode(body);
        for (var item in decoded) {
          final log = AuditLog(
            id: item['id'],
            userId: item['user_id'],
            action: item['action'],
            details: item['details'],
            timestamp:
                item['timestamp'] ?? DateTime.now().millisecondsSinceEpoch,
          );
          await _db.insertAuditLog(log);
        }
      }
    } catch (e) {
      debugPrint(
          "Campus network unstable: cannot sync audit logs from Laravel. $e");
    }
  }

  Future<bool> deleteFeedback(int id) async {
    // 1. Delete locally
    await _db.deleteFeedback(id);

    // 2. Delete remotely
    bool remoteSuccess = false;
    if (_authToken != null) {
      try {
        final url = Uri.parse("$_laravelBaseUrl/api/feedback/$id");
        final client = HttpClient();
        client.connectionTimeout = const Duration(seconds: 3);
        final request = await client.deleteUrl(url);
        request.headers.add("Authorization", "Bearer $_authToken");
        final response = await request.close();
        if (response.statusCode == 200) {
          remoteSuccess = true;
          debugPrint("Feedback deleted remotely from Laravel.");
        } else {
          debugPrint(
              "Failed to delete feedback remotely. Code: ${response.statusCode}");
        }
      } catch (e) {
        debugPrint("Exception deleting feedback remotely: $e");
      }
    }

    await refreshAllData();
    return remoteSuccess || _authToken == null;
  }

  Future<bool> deleteAuditLog(int id) async {
    // 1. Delete locally
    await _db.deleteAuditLog(id);

    // 2. Delete remotely
    bool remoteSuccess = false;
    if (_authToken != null) {
      try {
        final url = Uri.parse("$_laravelBaseUrl/api/audit-logs/$id");
        final client = HttpClient();
        client.connectionTimeout = const Duration(seconds: 3);
        final request = await client.deleteUrl(url);
        request.headers.add("Authorization", "Bearer $_authToken");
        final response = await request.close();
        if (response.statusCode == 200) {
          remoteSuccess = true;
          debugPrint("Audit log deleted remotely from Laravel.");
        } else {
          debugPrint(
              "Failed to delete audit log remotely. Code: ${response.statusCode}");
        }
      } catch (e) {
        debugPrint("Exception deleting audit log remotely: $e");
      }
    }

    await refreshAllData();
    return remoteSuccess || _authToken == null;
  }
}
