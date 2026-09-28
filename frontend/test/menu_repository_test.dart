import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:atu_cafeteria/core/network/api_client.dart';
import 'package:atu_cafeteria/data/repositories/menu_repository.dart';

void main() {
  group('MenuRepository (public catalogue)', () {
    test('fetches the public catalogue and parses the envelope', () async {
      final client = MockClient((request) async {
        expect(request.url.path, '/api/catalog/menu-items');
        return http.Response(
          jsonEncode({
            'success': true,
            'menu_items': [
              {
                'id': 1,
                'vendor_id': 20,
                'name': 'Jollof Rice with Chicken',
                'price': 35.00,
                'category': 'Ghanaian Local Dishes',
                'is_available': true,
              },
            ],
          }),
          200,
          headers: {'content-type': 'application/json'},
        );
      });

      final repo = MenuRepository(ApiClient(client: client));
      final items = await repo.items();

      expect(items, hasLength(1));
      expect(items.first.name, 'Jollof Rice with Chicken');
      expect(items.first.isAvailable, isTrue);
    });

    test('throws FormatException when the payload has no meal list', () async {
      final client = MockClient((request) async {
        return http.Response(jsonEncode({'success': true, 'message': 'none'}),
            200,
            headers: {'content-type': 'application/json'});
      });

      final repo = MenuRepository(ApiClient(client: client));

      expect(repo.items(), throwsA(isA<FormatException>()));
    });

    test('surfaces a 401 as ApiException', () async {
      final client = MockClient((request) async {
        return http.Response(
            jsonEncode({'message': 'Unauthenticated.'}), 401);
      });

      final repo = MenuRepository(ApiClient(client: client));

      expect(repo.items(), throwsA(isA<ApiException>()));
    });
  });
}