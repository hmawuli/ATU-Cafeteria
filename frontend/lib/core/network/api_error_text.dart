import 'dart:async';
import 'dart:io';

import 'package:http/http.dart' as http;
import 'package:atu_cafeteria/core/network/api_client.dart';

/// Translates low-level network/API failures into copy a user can act on.
///
/// Keep this the single place where screens map exceptions to user-facing
/// text, so every screen shows accurate guidance (instead of one generic
/// "something went wrong" message).
///
/// The cases match what [ApiClient] can produce:
///   * [ApiException]   — the server answered with a non-2xx status;
///   * [TimeoutException] — the request exceeded its timeout;
///   * [SocketException] / [http.ClientException] — the server could not be
///     reached at all (down, wrong address, web fetch failure);
///   * [FormatException] — the response body was not valid JSON / shape.
String friendlyNetworkError(
  Object error, {
  String fallback = 'Something went wrong. Please try again.',
}) {
  if (error is ApiException) {
    if (error.statusCode >= 500) {
      return 'The cafeteria server hit an error (HTTP ${error.statusCode}). '
          'Please try again in a moment.';
    }
    return error.message.trim().isNotEmpty ? error.message : fallback;
  }
  if (error is TimeoutException) {
    return 'The cafeteria server is taking too long to respond. '
        'Please check your connection and try again.';
  }
  if (error is SocketException || error is http.ClientException) {
    return 'Cannot reach the cafeteria server. Make sure it is running and '
        'the API address is correct — from a phone it must be your '
        'computer\'s network IP (or USB with adb reverse), not 127.0.0.1.';
  }
  if (error is FormatException) {
    return 'The server returned an unexpected response. Please try again.';
  }
  return fallback;
}
