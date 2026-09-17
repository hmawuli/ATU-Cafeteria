import 'package:flutter/material.dart';
import 'package:atu_cafeteria/core/theme/app_theme.dart';

class AtuUi {
  static const Color navy = AppTheme.primary;
  static const Color gold = AppTheme.accent;
  static const Color green = AppTheme.success;
  static const Color surface = AppTheme.background;

  static BoxDecoration card({Color? color, double radius = 16}) => BoxDecoration(
        color: color ?? Colors.white,
        borderRadius: BorderRadius.circular(radius),
        border: Border.all(color: AppTheme.border),
        boxShadow: const [
          BoxShadow(color: Color(0x12000000), blurRadius: 8, offset: Offset(0, 2)),
        ],
      );

  static Widget brandHeader({
    String title = 'ATU CAFETERIA',
    String subtitle = 'SMART CAMPUS FOOD PLATFORM',
    bool back = false,
    VoidCallback? onBack,
    List<Widget> actions = const [],
  }) => Container(
        color: navy,
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
        child: SafeArea(
          bottom: false,
          child: Row(
            children: [
              if (back)
                IconButton(
                  onPressed: onBack,
                  color: Colors.white,
                  icon: const Icon(Icons.arrow_back_rounded),
                ),
              Container(
                width: 42,
                height: 42,
                decoration: BoxDecoration(
                  color: gold,
                  borderRadius: BorderRadius.circular(10),
                ),
                child: const Icon(Icons.restaurant, color: navy, size: 25),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      title,
                      style: const TextStyle(
                        color: Colors.white,
                        fontSize: 18,
                        fontWeight: FontWeight.w900,
                        letterSpacing: .5,
                      ),
                    ),
                    Text(
                      subtitle,
                      style: const TextStyle(
                        color: Colors.white70,
                        fontSize: 9,
                        fontWeight: FontWeight.w700,
                        letterSpacing: .8,
                      ),
                    ),
                  ],
                ),
              ),
              ...actions,
            ],
          ),
        ),
      );

  static Widget sectionTitle(
    String title, {
    String? action,
    VoidCallback? onAction,
  }) => Padding(
        padding: const EdgeInsets.fromLTRB(16, 18, 16, 10),
        child: Row(
          children: [
            Expanded(
              child: Text(
                title,
                style: const TextStyle(
                  color: navy,
                  fontSize: 20,
                  fontWeight: FontWeight.w900,
                ),
              ),
            ),
            if (action != null)
              TextButton(
                onPressed: onAction,
                child: Text(
                  action,
                  style: const TextStyle(fontWeight: FontWeight.w800),
                ),
              ),
          ],
        ),
      );

  static Widget primaryButton(
    String text,
    VoidCallback? onPressed, {
    IconData? icon,
  }) => SizedBox(
        width: double.infinity,
        height: 52,
        child: ElevatedButton.icon(
          onPressed: onPressed,
          icon: Icon(icon ?? Icons.check_circle_outline),
          label: Text(text),
        ),
      );

  static Widget status(String text, {Color? color}) => Container(
        padding: const EdgeInsets.symmetric(horizontal: 11, vertical: 6),
        decoration: BoxDecoration(
          color: (color ?? green).withValues(alpha: .12),
          borderRadius: BorderRadius.circular(20),
        ),
        child: Text(
          text,
          style: TextStyle(
            color: color ?? green,
            fontSize: 11,
            fontWeight: FontWeight.w900,
          ),
        ),
      );
}

class AtuSearchBar extends StatelessWidget {
  final String hint;
  final TextEditingController? controller;
  final ValueChanged<String>? onChanged;

  const AtuSearchBar({
    super.key,
    required this.hint,
    this.controller,
    this.onChanged,
  });

  @override
  Widget build(BuildContext context) => Padding(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
        child: TextField(
          controller: controller,
          onChanged: onChanged,
          decoration: InputDecoration(
            hintText: hint,
            prefixIcon: const Icon(Icons.search, color: AtuUi.navy),
            suffixIcon: controller != null
                ? const Icon(Icons.tune, color: AtuUi.navy)
                : null,
          ),
        ),
      );
}
