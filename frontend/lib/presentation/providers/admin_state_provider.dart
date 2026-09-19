import 'package:flutter/foundation.dart';
import 'package:atu_cafeteria/core/network/api_client.dart';
import 'package:atu_cafeteria/data/repositories/admin_repository.dart';
import 'package:atu_cafeteria/data/repositories/smart_cafeteria_repository.dart';

class AdminStateProvider extends ChangeNotifier {
  final AdminRepository repository;
  final SmartCafeteriaRepository smart;
  AdminStateProvider(ApiClient api)
      : repository = AdminRepository(api),
        smart = SmartCafeteriaRepository(api);
  void setToken(String? token) {
    repository.api.token = token;
  }

  bool _loading = false;
  String? _error;
  Map<String, dynamic> dashboardData = {};
  List<dynamic> users = [];
  List<dynamic> vendors = [];
  List<dynamic> orders = [];
  Map<String, dynamic> financeData = {};
  List<dynamic> auditLogs = [];
  List<dynamic> settings = [];
  Map<String, dynamic> commandCenterData = {};
  List<dynamic> securityAlerts = [];

  /// Set when the Finance endpoint rejects the current admin (e.g. a
  /// CAFETERIA_ADMIN lacking `payments.view`). Other sections must keep
  /// loading normally, so this is tracked separately from [_error].
  String? financeError;

  bool get loading => _loading;
  String? get error => _error;

  Future<void> loadAll(
      {bool includeFinance = true, bool includeSettings = false}) async {
    _loading = true;
    _error = null;
    if (includeFinance) financeError = null;
    notifyListeners();

    // Each section loads independently: a single forbidden/failed endpoint
    // (e.g. Finance for a CAFETERIA_ADMIN) must never prevent Users, Vendors
    // or the dashboard from refreshing, otherwise newly created records stay
    // invisible after a page refresh.
    final futures = <Future<void>>[
      _assign(
        repository.dashboard,
        (v) => dashboardData = Map<String, dynamic>.from(v['data'] ?? {}),
      ),
      _assign(
        repository.users,
        (v) => users = _items(v['data']),
      ),
      _assign(
        repository.vendors,
        (v) => vendors = _items(v['data']),
      ),
      _assign(
        repository.orders,
        (v) => orders = _items(v['data']),
      ),
      _assign(
        repository.auditLogs,
        (v) => auditLogs = _items(v['data']),
      ),
      _assign(
        smart.commandCenter,
        (v) => commandCenterData = Map<String, dynamic>.from(v['data'] ?? {}),
      ),
      _assign(
        smart.securityAlerts,
        (v) => securityAlerts = _items(v['data']),
      ),
    ];
    if (includeFinance) {
      futures.add(_assign(
        repository.finance,
        (v) {
          financeData = Map<String, dynamic>.from(v['data'] ?? {});
          financeError = null;
        },
        onError: (_) => financeError =
            'You do not have permission to view finance. Contact a SUPER_ADMIN or FINANCE_ADMIN.',
      ));
    }
    if (includeSettings) {
      futures.add(_assign(
        repository.settings,
        (v) => settings = _items(v['data']),
      ));
    }
    await Future.wait(futures);
    _loading = false;
    notifyListeners();
  }

  /// Runs one admin API call and commits its result only on success.
  Future<void> _assign(
    Future<Map<String, dynamic>> Function() request,
    void Function(Map<String, dynamic>) commit, {
    void Function(Object error)? onError,
  }) async {
    try {
      final result = await request();
      commit(result);
    } catch (e) {
      _error = e.toString();
      onError?.call(e);
    }
  }

  Future<bool> changeUserStatus(int id, String status) async => _run(() async {
        await repository.updateUserStatus(id, status);
        await loadAll();
      });
  Future<bool> changeVendorStatus(int id, String status) async =>
      _run(() async {
        await repository.updateVendorStatus(id, status);
        await loadAll();
      });
  Future<bool> createVendor(
          {required String email,
          required String password,
          required String fullName,
          required String storeName,
          String? location,
          String? contactEmail,
          String? contactInfo}) async =>
      _run(() async {
        await repository.createVendor(
            email: email,
            password: password,
            fullName: fullName,
            storeName: storeName,
            location: location,
            contactEmail: contactEmail,
            contactInfo: contactInfo);
        await loadAll();
      });
  Future<bool> changeAdminLevel(int id, String level) async => _run(() async {
        await repository.updateAdminLevel(id, level);
        await loadAll();
      });
  Future<bool> adjustWallet(
          int id, double amount, String type, String reason) async =>
      _run(() async {
        await repository.walletAdjustment(id, amount, type, reason);
        await loadAll();
      });
  Future<bool> _run(Future<void> Function() action) async {
    try {
      _error = null;
      await action();
      return true;
    } catch (e) {
      _error = e.toString();
      notifyListeners();
      return false;
    }
  }

  List<dynamic> _items(dynamic data) => data is Map && data['data'] is List
      ? List<dynamic>.from(data['data'])
      : data is List
          ? List<dynamic>.from(data)
          : [];
}
