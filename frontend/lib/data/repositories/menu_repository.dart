import 'package:atu_cafeteria/core/network/api_client.dart';
import 'package:atu_cafeteria/domain/models/models.dart';

class MenuRepository {
  final ApiClient client;
  MenuRepository(this.client);
  Future<List<FoodItem>> items() async {
    final data = await client.request('GET', 'food-items');
    final list = data is Map ? (data['data'] ?? data['items'] ?? data['food_items'] ?? []) : data;
    return (list as List).map((e) => FoodItem.fromJson(Map<String, dynamic>.from(e))).toList();
  }
}
