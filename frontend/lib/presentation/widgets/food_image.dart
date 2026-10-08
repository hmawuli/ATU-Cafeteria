import 'dart:convert';

import 'package:flutter/material.dart';

import '../../core/theme/app_theme.dart';

/// Renders a food image that is either a remote https URL or an embedded
/// `data:image/…;base64,…` photo uploaded from a phone gallery.
Widget buildFoodImage(
  String url, {
  double? width,
  double? height,
  BoxFit fit = BoxFit.cover,
}) {
  Widget placeholder({IconData icon = Icons.restaurant_rounded}) => Container(
        decoration: BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: [
              AppTheme.primary.withValues(alpha: .16),
              AppTheme.accent.withValues(alpha: .32),
            ],
          ),
        ),
        child: Center(
          child: Icon(icon, color: AppTheme.primary, size: 30),
        ),
      );

  if (url.startsWith('data:')) {
    final comma = url.indexOf(',');
    final body = comma >= 0 ? url.substring(comma + 1) : url;
    try {
      final bytes = base64Decode(body);
      return Image.memory(
        bytes,
        width: width,
        height: height,
        fit: fit,
        errorBuilder: (_, __, ___) => placeholder(),
      );
    } catch (_) {
      return placeholder();
    }
  }

  return Image.network(
    url,
    width: width,
    height: height,
    fit: fit,
    errorBuilder: (_, __, ___) => placeholder(),
  );
}