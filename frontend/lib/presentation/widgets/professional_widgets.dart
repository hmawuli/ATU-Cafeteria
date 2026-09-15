import 'package:flutter/material.dart';

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
  Widget build(BuildContext context) => Row(
        crossAxisAlignment: CrossAxisAlignment.end,
        children: [
          Expanded(
              child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                Text(title,
                    style: Theme.of(context)
                        .textTheme
                        .titleLarge
                        ?.copyWith(fontWeight: FontWeight.w800)),
                if (subtitle != null) ...[
                  const SizedBox(height: 3),
                  Text(subtitle!, style: Theme.of(context).textTheme.bodySmall)
                ],
              ])),
          if (onAction != null)
            TextButton(onPressed: onAction, child: Text(actionLabel)),
        ],
      );
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
    final c = color ?? Theme.of(context).colorScheme.primary;
    return Card(
        child: Padding(
            padding: const EdgeInsets.all(16),
            child: Row(children: [
              Container(
                  padding: const EdgeInsets.all(11),
                  decoration: BoxDecoration(
                      color: c.withValues(alpha: .10),
                      borderRadius: BorderRadius.circular(12)),
                  child: Icon(icon, color: c)),
              const SizedBox(width: 12),
              Expanded(
                  child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                    Text(label, style: Theme.of(context).textTheme.bodySmall),
                    const SizedBox(height: 3),
                    Text(value,
                        style: Theme.of(context)
                            .textTheme
                            .titleLarge
                            ?.copyWith(fontWeight: FontWeight.w900)),
                  ])),
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
                    color: Theme.of(context)
                        .colorScheme
                        .primary
                        .withValues(alpha: .08),
                    shape: BoxShape.circle),
                child: Icon(icon,
                    size: 42, color: Theme.of(context).colorScheme.primary)),
            const SizedBox(height: 16),
            Text(title,
                style: Theme.of(context)
                    .textTheme
                    .titleMedium
                    ?.copyWith(fontWeight: FontWeight.w800)),
            const SizedBox(height: 6),
            Text(message,
                textAlign: TextAlign.center,
                style: Theme.of(context).textTheme.bodyMedium),
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
                    icon: const Icon(Icons.clear_rounded))),
      );
}

class AppStatusChip extends StatelessWidget {
  final String label;
  const AppStatusChip(this.label, {super.key});
  @override
  Widget build(BuildContext context) {
    final value = label.toUpperCase();
    final scheme = Theme.of(context).colorScheme;
    final Color c = value.contains('COMPLETE') ||
            value.contains('READY') ||
            value == 'ACTIVE'
        ? Colors.green
        : value.contains('CANCEL') ||
                value.contains('REJECT') ||
                value.contains('SUSPEND')
            ? Colors.red
            : scheme.primary;
    return Chip(
        avatar: Icon(Icons.circle, size: 9, color: c),
        label: Text(label,
            style: const TextStyle(fontWeight: FontWeight.w700, fontSize: 11)));
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
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Card(
        clipBehavior: Clip.antiAlias,
        child: Padding(
            padding: const EdgeInsets.all(12),
            child: Row(children: [
              Container(
                  width: 82,
                  height: 82,
                  decoration: BoxDecoration(
                      color: scheme.primary.withValues(alpha: .09),
                      borderRadius: BorderRadius.circular(14)),
                  child: imageUrl.trim().isNotEmpty
                      ? ClipRRect(
                          borderRadius: BorderRadius.circular(14),
                          child: Image.network(
                            imageUrl,
                            fit: BoxFit.cover,
                            errorBuilder: (_, __, ___) => Icon(
                              _icon(),
                              size: 38,
                              color: scheme.primary,
                            ),
                            loadingBuilder: (context, child, progress) =>
                                progress == null
                                    ? child
                                    : Center(
                                        child: SizedBox(
                                          width: 22,
                                          height: 22,
                                          child: CircularProgressIndicator(
                                            strokeWidth: 2,
                                            value: progress.expectedTotalBytes !=
                                                    null
                                                ? progress.cumulativeBytesLoaded /
                                                    progress.expectedTotalBytes!
                                                : null,
                                          ),
                                        ),
                                      ),
                          ),
                        )
                      : Icon(_icon(), size: 38, color: scheme.primary)),
              const SizedBox(width: 14),
              Expanded(
                  child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                    Text(name,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(
                            fontWeight: FontWeight.w800, fontSize: 16)),
                    const SizedBox(height: 4),
                    Text(description,
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis,
                        style: Theme.of(context).textTheme.bodySmall),
                    const SizedBox(height: 8),
                    Row(children: [
                      Text('GH₵ ${price.toStringAsFixed(2)}',
                          style: TextStyle(
                              fontWeight: FontWeight.w900,
                              color: scheme.primary)),
                      const Spacer(),
                      if (onFavorite != null)
                        IconButton(
                            visualDensity: VisualDensity.compact,
                            onPressed: onFavorite,
                            icon: const Icon(Icons.favorite_border_rounded),
                            tooltip: 'Add to favourites'),
                      if (onAdd != null)
                        FilledButton.icon(
                            onPressed: available ? onAdd : null,
                            icon: const Icon(Icons.add_shopping_cart_rounded,
                                size: 17),
                            label: Text(available ? 'Add' : 'Unavailable'))
                    ])
                  ]))
            ])));
  }
}

class OrderStatusTimeline extends StatelessWidget {
  final String status;
  const OrderStatusTimeline({super.key, required this.status});
  @override
  Widget build(BuildContext context) {
    final n = status.toUpperCase().replaceAll('_', ' ');
    const stages = ['ORDER PLACED', 'PREPARING', 'READY', 'COMPLETED'];
    var a = n.contains('COMPLETE') || n.contains('DELIVER')
        ? 3
        : n.contains('READY')
            ? 2
            : n.contains('PREPAR')
                ? 1
                : 0;
    if (n.contains('CANCEL') || n.contains('DECLIN')) a = -1;
    return Row(
        children: List.generate(stages.length, (i) {
      final done = a >= i && a >= 0;
      final current = a == i;
      return Expanded(
          child: Column(children: [
        Row(children: [
          if (i > 0)
            Expanded(
                child: Divider(
                    thickness: 2,
                    color: a >= i
                        ? Theme.of(context).colorScheme.primary
                        : Colors.grey.shade300)),
          Container(
              width: 28,
              height: 28,
              decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  color: done
                      ? Theme.of(context).colorScheme.primary
                      : Colors.grey.shade200),
              child: Icon(done ? Icons.check_rounded : Icons.circle,
                  size: done ? 17 : 8,
                  color: done ? Colors.white : Colors.grey.shade500)),
          if (i < stages.length - 1)
            Expanded(
                child: Divider(
                    thickness: 2,
                    color: a > i
                        ? Theme.of(context).colorScheme.primary
                        : Colors.grey.shade300))
        ]),
        const SizedBox(height: 6),
        Text(stages[i],
            textAlign: TextAlign.center,
            style: TextStyle(
                fontSize: 9,
                fontWeight: current ? FontWeight.w900 : FontWeight.w600))
      ]));
    }));
  }
}
