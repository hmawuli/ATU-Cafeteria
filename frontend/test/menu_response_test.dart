import 'package:flutter_test/flutter_test.dart';
import 'package:atu_cafeteria/data/remote/menu_response.dart';

void main() {
  group('menuItemsFromResponse', () {
    test('accepts the production {menu_items: [...]} envelope', () {
      final decoded = <String, dynamic>{
        'success': true,
        'menu_items': [
          {'id': 1, 'name': 'Jollof'},
          {'id': 2, 'name': 'Waakye'},
        ],
      };

      expect(menuItemsFromResponse(decoded), hasLength(2));
    });

    test('accepts the {data: [...]} envelope', () {
      final decoded = <String, dynamic>{
        'success': true,
        'data': [
          {'id': 3},
        ],
      };

      expect(menuItemsFromResponse(decoded), hasLength(1));
    });

    test('accepts a bare JSON array', () {
      final decoded = [
        {'id': 4},
        {'id': 5},
      ];

      expect(menuItemsFromResponse(decoded), hasLength(2));
    });

    test('prefers menu_items when more than one list key exists', () {
      final decoded = <String, dynamic>{
        'menu_items': [
          {'id': 1},
        ],
        'data': [
          {'id': 2},
        ],
      };

      expect(menuItemsFromResponse(decoded), hasLength(1));
      expect(menuItemsFromResponse(decoded)!.first['id'], 1);
    });

    test('returns null for a non-list payload', () {
      expect(menuItemsFromResponse(<String, dynamic>{'success': true}), isNull);
      expect(menuItemsFromResponse('not a payload'), isNull);
      expect(menuItemsFromResponse(null), isNull);
    });

    test('returns the items untouched so callers can map them', () {
      final decoded = <String, dynamic>{
        'menu_items': <Object>['x'],
      };
      final items = menuItemsFromResponse(decoded);
      expect(items, isA<List<dynamic>>());
      expect(items!.first, 'x');
    });
  });
}