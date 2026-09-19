import 'package:atu_cafeteria/core/network/api_client.dart';
import 'package:atu_cafeteria/domain/models/models.dart';

/// Single source of truth for reading the cafeteria menu from the API.
class MenuRepository {
  final ApiClient client;
  MenuRepository(this.client);

  /// Fetches the full food catalogue from `GET /api/food-items`.
  ///
  /// The endpoint returns a bare JSON array, but we tolerate the common
  /// envelope shapes (`{"data": [...]}`, `{"food_items": [...]}`) for
  /// robustness against older API responses.
  ///
  /// Failures surface as:
  ///   * [ApiException] — HTTP error from the server;
  ///   * [TimeoutException] / [SocketException] / [http.ClientException] —
  ///     the server could not be reached;
  ///   * [FormatException] — the body parsed as JSON but had no usable list.
  Future<List<FoodItem>> items() async {
    final data = await client.request('GET', 'food-items');

    final raw = data is List
        ? data
        : data is Map
            ? (data['data'] ?? data['items'] ?? data['food_items'])
            : null;

    if (raw is! List) {
      throw const FormatException('Menu response was not a list.');
    }

    return raw
        .map((e) => FoodItem.fromJson(Map<String, dynamic>.from(e)))
        .toList();
  }
}
