import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:atu_cafeteria/presentation/providers/cafeteria_provider.dart';

void main() {

  group('CafeteriaProvider catalogue sync (injected client)', () {
    test('parses the production {menu_items: [...]} envelope', () async {
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
              {
                'id': 2,
                'vendor_id': 21,
                'name': 'Meat Pie',
                'price': 12.00,
                'category': 'Pastries & Snacks',
              },
            ],
          }),
          200,
          headers: {'content-type': 'application/json'},
        );
      });

      final provider = CafeteriaProvider()..httpClientOverride = client;
      addTearDown(provider.dispose);

      final items = await provider.fetchRemoteFoodItems();

      expect(items, hasLength(2));
      expect(items.first.name, 'Jollof Rice with Chicken');
      expect(items.first.vendorId, 20);
      expect(items.first.isAvailable, isTrue);
    });

    test('tolerates a bare JSON array and skips malformed rows', () async {
      final client = MockClient((request) async {
        return http.Response(
          jsonEncode([
            {'id': 3, 'vendor_id': 20, 'name': 'Sobolo', 'price': 10.00},
            'not-a-map',
            null,
            {'name': 'Waakye with Chicken', 'price': 38.00},
          ]),
          200,
          headers: {'content-type': 'application/json'},
        );
      });

      final provider = CafeteriaProvider()..httpClientOverride = client;
      addTearDown(provider.dispose);

      final items = await provider.fetchRemoteFoodItems();

      expect(items, hasLength(2));
    });

    test('returns an empty list when the server responds non-200', () async {
      final client = MockClient((request) async {
        return http.Response('Service Unavailable', 503);
      });

      final provider = CafeteriaProvider()..httpClientOverride = client;
      addTearDown(provider.dispose);

      expect(await provider.fetchRemoteFoodItems(), isEmpty);
    });

    test('returns an empty list on a garbage payload', () async {
      final client = MockClient((request) async {
        return http.Response(jsonEncode({'success': true}), 200,
            headers: {'content-type': 'application/json'});
      });

      final provider = CafeteriaProvider()..httpClientOverride = client;
      addTearDown(provider.dispose);

      expect(await provider.fetchRemoteFoodItems(), isEmpty);
    });
  });
}