import 'package:atu_cafeteria/core/network/api_client.dart';
import 'package:atu_cafeteria/domain/models/models.dart';

class OrderRepository {
  final ApiClient client;
  OrderRepository(this.client);
  Future<List<Order>> myOrders() async {
    final data = await client.request('GET', 'customer/orders');
    final list = data is Map ? (data['data'] ?? data['orders'] ?? []) : data;
    return (list as List)
        .map((e) => Order.fromJson(Map<String, dynamic>.from(e)))
        .toList();
  }
}
