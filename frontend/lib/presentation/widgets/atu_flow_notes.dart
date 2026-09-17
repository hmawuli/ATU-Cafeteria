import 'package:flutter/foundation.dart';

/// Central UI-flow contract for the ATU Cafeteria presentation layer.
/// All primary screens should preserve these transitions:
/// student: home -> vendors -> menu -> cart -> checkout -> orders -> tracking;
/// vendor: dashboard -> menu -> orders -> preparing -> ready -> pickup;
/// admin: dashboard -> users/vendors/orders -> reports/settings.
class AtuFlow {
  static void log(String from, String to) => debugPrint('ATU FLOW: $from -> $to');
}
