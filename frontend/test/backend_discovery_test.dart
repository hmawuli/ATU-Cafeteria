import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:atu_cafeteria/core/network/backend_discovery.dart';

void main() {
  group('BackendDiscovery (debug auto-connect)', () {
    test('picks the first reachable candidate in order', () async {
      var calls = <String>[];
      final client = MockClient((request) async {
        calls.add(request.url.toString());
        if (request.url.host == '10.0.2.2') {
          return http.Response('{"status":"UP"}', 200);
        }
        throw http.ClientException('unreachable');
      });

      final resolved = await BackendDiscovery().resolve(
        candidates: [
          'http://127.0.0.1:8000',
          'http://10.0.2.2:8000',
          'http://192.168.0.103:8000',
        ],
        client: client,
      );

      expect(calls.first, 'http://127.0.0.1:8000/api/health');
      expect(resolved, 'http://10.0.2.2:8000');
    });

    test('returns null when nothing answers', () async {
      final client = MockClient((request) async {
        throw http.ClientException('down');
      });

      final resolved = await BackendDiscovery().resolve(
        candidates: ['http://127.0.0.1:1', 'http://10.0.2.2:1'],
        client: client,
      );

      expect(resolved, isNull);
    });
  });
}