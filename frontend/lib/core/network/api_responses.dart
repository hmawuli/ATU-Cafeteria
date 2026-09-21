import 'dart:async';
import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'api_client.dart';

/// Converts any error raised while talking to the backend into a clear,
/// professional message that can be shown directly to the user.
///
/// Backend-provided messages (carried in the API `message` field) are
/// preferred when available; transport-level and unexpected errors fall back
/// to friendly, plain-language explanations.
String friendlyApiError(
  Object error, {
  String fallback = 'Something went wrong. Please try again.',
}) {
  if (error is ApiException) {
    final message = error.message.trim();
    if (message.isNotEmpty && message != 'Request failed. Please try again.') {
      return message;
    }
    switch (error.statusCode) {
      case 400:
        return 'The request was invalid. Please review the information provided.';
      case 401:
        return 'Your session has expired. Please sign in again.';
      case 403:
        return 'You do not have permission to perform this action.';
      case 404:
        return 'The requested resource was not found on the server.';
      case 409:
        return 'This action conflicts with existing records.';
      case 422:
        return message.isNotEmpty
            ? message
            : 'Please fix the highlighted fields and try again.';
      case 429:
        return 'Too many requests. Please wait a moment and try again.';
      default:
        return error.statusCode >= 500
            ? 'The server encountered a problem. Please try again later.'
            : fallback;
    }
  }
  if (error is TimeoutException) {
    return 'The server is taking too long to respond. Please try again.';
  }
  final text = error.toString();
  if (error is http.ClientException ||
      text.contains('SocketException') ||
      text.contains('Connection refused') ||
      text.contains('Failed host lookup') ||
      text.contains('Unable to establish connection')) {
    return 'Cannot reach the cafeteria server. Check your connection and try again.';
  }
  if (error is FormatException) {
    return 'The server returned an unexpected response. Please try again.';
  }
  return fallback;
}

/// Shows a standardized feedback message (success or error) using a floating
/// snack bar with an icon, so every screen reports the same professional way.
void showAppMessage(
  BuildContext context, {
  required String message,
  bool isError = false,
}) {
  final theme = Theme.of(context);
  final background =
      isError ? theme.colorScheme.error : theme.colorScheme.inverseSurface;
  final foreground = isError
      ? theme.colorScheme.onError
      : theme.colorScheme.onInverseSurface;
  ScaffoldMessenger.of(context)
    ..hideCurrentSnackBar()
    ..showSnackBar(
      SnackBar(
        behavior: SnackBarBehavior.floating,
        backgroundColor: background,
        content: Row(
          children: [
            Icon(
              isError ? Icons.error_outline : Icons.check_circle_outline,
              color: foreground,
              size: 20,
            ),
            const SizedBox(width: 10),
            Expanded(
              child: Text(message, style: TextStyle(color: foreground)),
            ),
          ],
        ),
      ),
    );
}