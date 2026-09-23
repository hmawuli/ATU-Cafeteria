class DefenseLocalApi {
  DefenseLocalApi._();
  static final DefenseLocalApi instance = DefenseLocalApi._();

  Never _unsupported() =>
      throw UnsupportedError('Phone-only defense mode requires an Android/iOS build.');

  Future<Map<String, dynamic>?> login(String username, String pin) async => _unsupported();
  Future<List<Map<String, dynamic>>> users({String? role}) async => _unsupported();
  Future<List<Map<String, dynamic>>> foods() async => _unsupported();
  Future<List<Map<String, dynamic>>> ordersFor(int userId, String role) async => _unsupported();
  Future<double> balance(int userId) async => _unsupported();
  Future<int> placeOrder({required int customerId, required int foodId, required int quantity}) async => _unsupported();
  Future<void> updateOrderStatus(int orderId, String status, int actorId) async => _unsupported();
  Future<void> addReview({required int orderId, required int customerId, required int rating, required String comment}) async => _unsupported();
  Future<List<Map<String, dynamic>>> reviews() async => _unsupported();
  Future<List<Map<String, dynamic>>> auditLogs() async => _unsupported();
  Future<void> resetDefenseData() async => _unsupported();
}
