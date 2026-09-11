import 'package:flutter/foundation.dart';
import 'package:atu_cafeteria/core/network/api_client.dart';
import 'package:atu_cafeteria/data/repositories/admin_repository.dart';
import 'package:atu_cafeteria/data/repositories/smart_cafeteria_repository.dart';

class AdminStateProvider extends ChangeNotifier {
  final AdminRepository repository;
  final SmartCafeteriaRepository smart;
  AdminStateProvider(ApiClient api) : repository = AdminRepository(api), smart = SmartCafeteriaRepository(api);
  void setToken(String? token) { repository.api.token = token; }

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

  bool get loading => _loading;
  String? get error => _error;

  Future<void> loadAll({bool includeFinance = true, bool includeSettings = false}) async {
    _loading = true; _error = null; notifyListeners();
    try {
      final futures = <Future<Map<String, dynamic>>>[
        repository.dashboard(), repository.users(), repository.vendors(), repository.orders(), repository.auditLogs(), smart.commandCenter(), smart.securityAlerts(),
      ];
      if (includeFinance) futures.add(repository.finance());
      if (includeSettings) futures.add(repository.settings());
      final results = await Future.wait(futures);
      dashboardData = Map<String, dynamic>.from(results[0]['data'] ?? {});
      users = _items(results[1]['data']);
      vendors = _items(results[2]['data']);
      orders = _items(results[3]['data']);
      auditLogs = _items(results[4]['data']);
      commandCenterData = Map<String, dynamic>.from(results[5]['data'] ?? {});
      securityAlerts = _items(results[6]['data']);
      var i = 7;
      if (includeFinance) financeData = Map<String, dynamic>.from(results[i++]['data'] ?? {});
      if (includeSettings) settings = _items(results[i]['data']);
    } catch (e) { _error = e.toString(); }
    _loading = false; notifyListeners();
  }

  Future<bool> changeUserStatus(int id, String status) async => _run(() async { await repository.updateUserStatus(id, status); await loadAll(); });
  Future<bool> changeVendorStatus(int id, String status) async => _run(() async { await repository.updateVendorStatus(id, status); await loadAll(); });
  Future<bool> createVendor({required String email, required String password, required String fullName, required String storeName, String? location, String? contactEmail, String? contactInfo}) async => _run(() async { await repository.createVendor(email: email, password: password, fullName: fullName, storeName: storeName, location: location, contactEmail: contactEmail, contactInfo: contactInfo); await loadAll(); });
  Future<bool> changeAdminLevel(int id, String level) async => _run(() async { await repository.updateAdminLevel(id, level); await loadAll(); });
  Future<bool> adjustWallet(int id, double amount, String type, String reason) async => _run(() async { await repository.walletAdjustment(id, amount, type, reason); await loadAll(); });
  Future<bool> _run(Future<void> Function() action) async { try { _error=null; await action(); return true; } catch(e) { _error=e.toString(); notifyListeners(); return false; } }
  List<dynamic> _items(dynamic data) => data is Map && data['data'] is List ? List<dynamic>.from(data['data']) : data is List ? List<dynamic>.from(data) : [];
}
