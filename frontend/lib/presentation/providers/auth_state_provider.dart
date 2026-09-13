import 'package:flutter/foundation.dart';

/// Focused state holder for the auth domain.
/// The legacy CafeteriaProvider remains as a compatibility facade while screens are migrated.
class AuthStateProvider extends ChangeNotifier {
  bool _loading = false;
  String? _error;
  bool get loading => _loading;
  String? get error => _error;
  void setLoading(bool value) {
    _loading = value;
    notifyListeners();
  }

  void setError(String? value) {
    _error = value;
    notifyListeners();
  }
}
