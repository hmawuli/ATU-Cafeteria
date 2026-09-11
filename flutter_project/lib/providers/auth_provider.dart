import 'package:flutter/foundation.dart';
import '../models/user.dart';
import '../services/api_service.dart';

class AuthProvider with ChangeNotifier {
  User? _currentUser;
  bool _isLoading = false;
  String? _errorMessage;

  User? get currentUser => _currentUser;
  bool get isLoading => _isLoading;
  bool get isAuthenticated => _currentUser != null && ApiService.authToken != null;
  String? get errorMessage => _errorMessage;

  /// All authentication decisions come from the backend.
  /// There is deliberately no local/demo login fallback because a local role
  /// flag must never be able to grant STUDENT, VENDOR, or ADMIN privileges.
  Future<bool> login(String username, String pin) async {
    _isLoading = true;
    _errorMessage = null;
    notifyListeners();

    final result = await ApiService.login(username, pin);
    if (result['success'] == true && result['user'] is User && ApiService.authToken != null) {
      _currentUser = result['user'] as User;
      _isLoading = false;
      notifyListeners();
      return true;
    }

    _currentUser = null;
    _errorMessage = result['message'] as String? ?? 'Authentication failed.';
    _isLoading = false;
    notifyListeners();
    return false;
  }

  Future<void> logout() async {
    await ApiService.logout();
    _currentUser = null;
    _errorMessage = null;
    notifyListeners();
  }
}
