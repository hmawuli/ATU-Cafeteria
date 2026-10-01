import 'package:flutter/foundation.dart';
import 'package:http/http.dart' as http;

import '../config/app_config.dart';

/// Development-only backend auto-discovery.
///
/// In debug builds the app probes a short candidate list and uses the first
/// backend that answers `/api/health`, so:
///   * USB + `scripts/connect_phone.sh`           → 127.0.0.1:8000 works
///   * Android emulator                           → 10.0.2.2:8000 works
///   * custom `--dart-define=API_BASE_URL=...`    → always wins
///   * same Wi-Fi / LAN (`staticApiHost`)         → always wins
///
/// Release builds never probe: the explicitly configured URL is used as-is,
/// and HTTPS is still enforced.
class BackendDiscovery {
  Future<String?> resolve({
    List<String>? candidates,
    http.Client? client,
  }) async {
    if (!kDebugMode) {
      return null;
    }

    final probe = client ?? http.Client();
    final effective = (candidates ?? [
          AppConfig.normalizedApiBaseUrl,
          'http://10.0.2.2:8000', // Android emulator → host loopback
          'http://127.0.0.1:8000', // USB adb reverse / web / desktop
        ]).toSet();

    for (final base in effective) {
      if (base.isEmpty) continue;
      try {
        final res = await probe
            .get(Uri.parse('$base/api/health'))
            .timeout(const Duration(milliseconds: 800));
        if (res.statusCode == 200) {
          return base;
        }
      } catch (_) {
        // Candidate unreachable — try the next one.
      }
    }

    return null;
  }
}