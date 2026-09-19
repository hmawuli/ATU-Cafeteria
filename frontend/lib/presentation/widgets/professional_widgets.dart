import 'package:flutter/material.dart';
import 'package:atu_cafeteria/core/theme/app_theme.dart';

class AppSectionHeader extends StatelessWidget {
  final String title;
  final String? subtitle;
  final VoidCallback? onAction;
  final String actionLabel;
  const AppSectionHeader(
      {super.key,
      required this.title,
      this.subtitle,
      this.onAction,
      this.actionLabel = 'View all'});
  @override
  Widget build(BuildContext context) =>
      Row(crossAxisAlignment: CrossAxisAlignment.end, children: [
        Expanded(
            child:
                Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Text(title,
              style: const TextStyle(
                  color: AppTheme.textDark,
                  fontSize: 20,
                  fontWeight: FontWeight.w900)),
          if (subtitle != null) ...[
            const SizedBox(height: 3),
            Text(subtitle!,
                style: const TextStyle(color: AppTheme.textMuted, fontSize: 12))
          ],
        ])),
        if (onAction != null)
          TextButton(onPressed: onAction, child: Text(actionLabel)),
      ]);
}

class AppMetricCard extends StatelessWidget {
  final String label, value;
  final IconData icon;
  final Color? color;
  const AppMetricCard(
      {super.key,
      required this.label,
      required this.value,
      required this.icon,
      this.color});
  @override
  Widget build(BuildContext context) {
    final c = color ?? AppTheme.primary;
    return Card(
        child: Padding(
            padding: const EdgeInsets.all(15),
            child: Row(children: [
              Container(
                  width: 44,
                  height: 44,
                  decoration: BoxDecoration(
                      color: c.withValues(alpha: .10), shape: BoxShape.circle),
                  child: Icon(icon, color: c)),
              const SizedBox(width: 12),
              Expanded(
                  child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                    Text(label,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(
                            color: AppTheme.textMuted,
                            fontSize: 12,
                            fontWeight: FontWeight.w600)),
                    const SizedBox(height: 3),
                    Text(value,
                        style: const TextStyle(
                            color: AppTheme.textDark,
                            fontSize: 21,
                            fontWeight: FontWeight.w900)),
                  ])),
              const Icon(Icons.chevron_right_rounded,
                  color: AppTheme.textMuted),
            ])));
  }
}

class AppEmptyState extends StatelessWidget {
  final IconData icon;
  final String title, message;
  final VoidCallback? onAction;
  final String actionLabel;
  const AppEmptyState(
      {super.key,
      required this.icon,
      required this.title,
      required this.message,
      this.onAction,
      this.actionLabel = 'Try again'});
  @override
  Widget build(BuildContext context) => Center(
      child: Padding(
          padding: const EdgeInsets.all(32),
          child: Column(mainAxisSize: MainAxisSize.min, children: [
            Container(
                padding: const EdgeInsets.all(18),
                decoration: BoxDecoration(
                    color: AppTheme.primary.withValues(alpha: .08),
                    shape: BoxShape.circle),
                child: const Icon(Icons.restaurant_rounded,
                    size: 42, color: AppTheme.primary)),
            const SizedBox(height: 16),
            Text(title,
                style: const TextStyle(
                    color: AppTheme.textDark,
                    fontSize: 17,
                    fontWeight: FontWeight.w800)),
            const SizedBox(height: 6),
            Text(message,
                textAlign: TextAlign.center,
                style: const TextStyle(color: AppTheme.textMuted)),
            if (onAction != null) ...[
              const SizedBox(height: 16),
              FilledButton(onPressed: onAction, child: Text(actionLabel))
            ],
          ])));
}

class AppSearchField extends StatelessWidget {
  final TextEditingController controller;
  final String hint;
  final ValueChanged<String>? onChanged;
  const AppSearchField(
      {super.key,
      required this.controller,
      this.hint = 'Search...',
      this.onChanged});
  @override
  Widget build(BuildContext context) => TextField(
      controller: controller,
      onChanged: onChanged,
      textInputAction: TextInputAction.search,
      decoration: InputDecoration(
          hintText: hint,
          prefixIcon: const Icon(Icons.search_rounded),
          suffixIcon: controller.text.isEmpty
              ? null
              : IconButton(
                  onPressed: () {
                    controller.clear();
                    onChanged?.call('');
                  },
                  icon: const Icon(Icons.clear_rounded))));
}

class AppStatusChip extends StatelessWidget {
  final String label;
  const AppStatusChip(this.label, {super.key});
  @override
  Widget build(BuildContext context) {
    final value = label.toUpperCase();
    final c = value.contains('COMPLETE') ||
            value.contains('READY') ||
            value == 'ACTIVE'
        ? AppTheme.success
        : value.contains('CANCEL') ||
                value.contains('REJECT') ||
                value.contains('SUSPEND')
            ? Colors.red
            : AppTheme.primary;
    return Container(
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
        decoration: BoxDecoration(
            color: c.withValues(alpha: .10),
            borderRadius: BorderRadius.circular(20)),
        child: Text(label,
            style: TextStyle(
                color: c, fontSize: 11, fontWeight: FontWeight.w800)));
  }
}

class FoodCard extends StatelessWidget {
  final String name, description, category, imageUrl;
  final double price;
  final bool available;
  final VoidCallback? onAdd, onFavorite;
  const FoodCard(
      {super.key,
      required this.name,
      required this.description,
      required this.category,
      required this.imageUrl,
      required this.price,
      this.available = true,
      this.onAdd,
      this.onFavorite});
  IconData _icon() {
    final v = category.toLowerCase();
    if (v.contains('drink')) return Icons.local_drink_rounded;
    if (v.contains('snack')) return Icons.fastfood_rounded;
    if (v.contains('breakfast')) return Icons.breakfast_dining_rounded;
    return Icons.restaurant_rounded;
  }

  @override
  Widget build(BuildContext context) => Card(
      clipBehavior: Clip.antiAlias,
      child: Padding(
          padding: const EdgeInsets.all(10),
          child: Row(children: [
            SizedBox(
                width: 92,
                height: 82,
                child: ClipRRect(
                    borderRadius: BorderRadius.circular(14),
                    child: imageUrl.trim().isNotEmpty
                        ? Image.network(imageUrl,
                            fit: BoxFit.cover,
                            errorBuilder: (_, __, ___) => Container(
                                color: AppTheme.primary.withValues(alpha: .08),
                                child: Icon(_icon(),
                                    size: 36, color: AppTheme.primary)),
                            loadingBuilder: (context, child, progress) =>
                                progress == null
                                    ? child
                                    : const Center(
                                        child: CircularProgressIndicator(
                                            strokeWidth: 2)))
                        : Container(
                            color: AppTheme.primary.withValues(alpha: .08),
                            child: Icon(_icon(),
                                size: 36, color: AppTheme.primary)))),
            const SizedBox(width: 13),
            Expanded(
                child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                  Text(name,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                          color: AppTheme.textDark,
                          fontSize: 16,
                          fontWeight: FontWeight.w900)),
                  const SizedBox(height: 3),
                  Text(description,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                          color: AppTheme.textMuted,
                          fontSize: 12,
                          height: 1.25)),
                  const SizedBox(height: 7),
                  Row(children: [
                    Text('GH₵ ${price.toStringAsFixed(2)}',
                        style: const TextStyle(
                            color: AppTheme.primary,
                            fontSize: 16,
                            fontWeight: FontWeight.w900)),
                    const Spacer(),
                    if (onFavorite != null)
                      IconButton(
                          visualDensity: VisualDensity.compact,
                          onPressed: onFavorite,
                          icon: const Icon(Icons.favorite_border_rounded)),
                    if (onAdd != null)
                      FilledButton(
                          onPressed: available ? onAdd : null,
                          style: FilledButton.styleFrom(
                              backgroundColor: AppTheme.accent,
                              foregroundColor: AppTheme.primaryDark,
                              minimumSize: const Size(0, 38),
                              padding:
                                  const EdgeInsets.symmetric(horizontal: 13)),
                          child: Text(available ? '+ Add' : 'Unavailable'))
                  ])
                ]))
          ])));
}

class OrderStatusTimeline extends StatelessWidget {
  final String status;
  const OrderStatusTimeline({super.key, required this.status});
  @override
  Widget build(BuildContext context) {
    final n = status.toUpperCase().replaceAll('_', ' ');
    const stages = ['RECEIVED', 'PREPARING', 'READY', 'PICKED UP'];
    final a = n.contains('COMPLETE') || n.contains('DELIVER')
        ? 3
        : n.contains('READY')
            ? 2
            : n.contains('PREPAR')
                ? 1
                : 0;
    return Row(
        children: List.generate(
            stages.length,
            (i) => Expanded(
                    child: Column(children: [
                  Row(children: [
                    if (i > 0)
                      Expanded(
                          child: Divider(
                              thickness: 2,
                              color: a >= i
                                  ? AppTheme.primary
                                  : const Color(0xFFD5E0ED))),
                    Container(
                        width: 28,
                        height: 28,
                        decoration: BoxDecoration(
                            shape: BoxShape.circle,
                            color: a >= i
                                ? (i == a ? AppTheme.accent : AppTheme.primary)
                                : Colors.white,
                            border: Border.all(
                                color: a >= i
                                    ? AppTheme.primary
                                    : const Color(0xFFB8CDE3),
                                width: 2)),
                        child: Icon(
                            a >= i
                                ? Icons.check_rounded
                                : Icons.circle_outlined,
                            size: a >= i ? 16 : 11,
                            color: a >= i
                                ? (i == a ? AppTheme.primaryDark : Colors.white)
                                : const Color(0xFF8CA0B7))),
                    if (i < stages.length - 1)
                      Expanded(
                          child: Divider(
                              thickness: 2,
                              color: a > i
                                  ? AppTheme.primary
                                  : const Color(0xFFD5E0ED)))
                  ]),
                  const SizedBox(height: 5),
                  Text(stages[i],
                      textAlign: TextAlign.center,
                      style: TextStyle(
                          color: AppTheme.textDark,
                          fontSize: 9,
                          fontWeight:
                              i == a ? FontWeight.w900 : FontWeight.w600))
                ]))));
  }
}
