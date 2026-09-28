import 'package:flutter/foundation.dart';

import 'package:atu_cafeteria/core/network/api_client.dart';

/// Focused state for the wallet ledger (balance mirror + transactions).
///
/// Extracted from the monolith `CafeteriaProvider`. The authoritative balance
/// always comes from the API (`GET /api/wallet` / `/api/me`); this store holds
/// the snapshot the UI mirrors, plus the latest transaction ledger.
class WalletState extends ChangeNotifier {
  double _balance = 0;

  double get balance => _balance;

  List<Map<String, dynamic>> _transactions = [];

  List<Map<String, dynamic>> get transactions =>
      List<Map<String, dynamic>>.unmodifiable(_transactions);

  void setBalance(double value) {
    _balance = value;
    notifyListeners();
  }

  /// Mirror a server-verified credit locally so a page shows it immediately.
  void applyLocalCredit(double amount) {
    _balance += amount;
    notifyListeners();
  }

  /// Fetch the wallet overview (balance + ledger) from `GET /api/wallet`.
  Future<void> refresh(ApiClient api, String? token) async {
    final decoded = await api
        .request('GET', 'wallet', token: token)
        .timeout(const Duration(seconds: 8));

    if (decoded is Map) {
      final rawBalance = decoded['balance'];
      if (rawBalance is num) {
        _balance = rawBalance.toDouble();
      }
      final raw = decoded['transactions'];
      if (raw is List) {
        _transactions = raw
            .whereType<Map>()
            .map(Map<String, dynamic>.from)
            .toList();
      }
      notifyListeners();
    }
  }
}