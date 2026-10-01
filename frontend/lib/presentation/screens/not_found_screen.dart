import 'package:flutter/material.dart';

import '../../core/theme/app_theme.dart';

/// Shown when navigation targets an unregistered page — guarantees a page can
/// never silently dead-end with a blank/error screen.
class NotFoundScreen extends StatelessWidget {
  const NotFoundScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Icon(Icons.route_outlined, size: 56, color: AppTheme.accent),
              const SizedBox(height: 16),
              Text(
                'Page not found',
                style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                      fontWeight: FontWeight.w800,
                      color: AppTheme.textDark,
                    ),
              ),
              const SizedBox(height: 8),
              Text(
                'This screen is not registered in the app. Return to the home page.',
                textAlign: TextAlign.center,
                style: Theme.of(context)
                    .textTheme
                    .bodyMedium
                    ?.copyWith(color: AppTheme.textMuted),
              ),
              const SizedBox(height: 24),
              FilledButton.icon(
                onPressed: () => Navigator.of(context)
                    .popUntil((route) => route.isFirst),
                icon: const Icon(Icons.home_outlined),
                label: const Text('Return home'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}