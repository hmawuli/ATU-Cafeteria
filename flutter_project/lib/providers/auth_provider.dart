import 'package:flutter/foundation.dart';
import '../models/user.dart';
import '../services/api_service.dart';

class AuthProvider with ChangeNotifier {
  User? _currentUser;
  bool _isLoading = false;

  User? get currentUser => _currentUser;
  bool get isLoading => _isLoading;
  bool get isAuthenticated => _currentUser != null;

  Future<bool> login(String username, String password) async {
    _isLoading = true;
    notifyListeners();

    // 1. Try Remote API
    final result = await ApiService.login(username, password);
    if (result['success'] == true && result['user'] != null) {
      _currentUser = result['user'];
      _isLoading = false;
      notifyListeners();
      return true;
    }

    // 2. Local Fallback for offline demo
    final normalized = username.trim().toLowerCase();
    if (normalized == 'student' || normalized.contains('student')) {
      _currentUser = User(
        id: 101,
        username: username,
        fullName: 'Campus Student',
        role: 'STUDENT',
        balance: 150.00,
        studentStaffId: 'ATU-2026-889',
        telephone: '+233 24 123 4567',
      );
      _isLoading = false;
      notifyListeners();
      return true;
    } else if (normalized == 'vendor' || normalized.contains('vendor') || normalized == 'kitchen') {
      _currentUser = User(
        id: 201,
        username: username,
        fullName: 'Akwaaba Kitchen',
        role: 'VENDOR',
        isOpen: true,
      );
      _isLoading = false;
      notifyListeners();
      return true;
    } else if (normalized == 'admin') {
      _currentUser = User(
        id: 301,
        username: username,
        fullName: 'Cafeteria Manager',
        role: 'ADMIN',
      );
      _isLoading = false;
      notifyListeners();
      return true;
    }

    _isLoading = false;
    notifyListeners();
    return false;
  }

  void logout() {
    _currentUser = null;
    ApiService.authToken = null;
    notifyListeners();
  }
}
