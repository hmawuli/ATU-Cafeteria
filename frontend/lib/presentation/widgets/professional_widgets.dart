import 'package:flutter/material.dart';

class AppSectionHeader extends StatelessWidget {
  final String title;
  final String? subtitle;
  final VoidCallback? onAction;
  final String actionLabel;
  const AppSectionHeader({super.key, required this.title, this.subtitle, this.onAction, this.actionLabel='View all'});
  @override
  Widget build(BuildContext context) => Row(
    crossAxisAlignment: CrossAxisAlignment.end,
    children: [
      Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Text(title, style: Theme.of(context).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w800)),
        if (subtitle != null) ...[const SizedBox(height: 3), Text(subtitle!, style: Theme.of(context).textTheme.bodySmall)],
      ])),
      if (onAction != null) TextButton(onPressed: onAction, child: Text(actionLabel)),
    ],
  );
}

class AppMetricCard extends StatelessWidget {
  final String label, value;
  final IconData icon;
  final Color? color;
  const AppMetricCard({super.key, required this.label, required this.value, required this.icon, this.color});
  @override
  Widget build(BuildContext context) {
    final c = color ?? Theme.of(context).colorScheme.primary;
    return Card(child: Padding(padding: const EdgeInsets.all(16), child: Row(children: [
      Container(padding: const EdgeInsets.all(11), decoration: BoxDecoration(color: c.withOpacity(.10), borderRadius: BorderRadius.circular(12)), child: Icon(icon, color: c)),
      const SizedBox(width: 12),
      Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Text(label, style: Theme.of(context).textTheme.bodySmall),
        const SizedBox(height: 3),
        Text(value, style: Theme.of(context).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w900)),
      ])),
    ])));
  }
}

class AppEmptyState extends StatelessWidget {
  final IconData icon;
  final String title, message;
  final VoidCallback? onAction;
  final String actionLabel;
  const AppEmptyState({super.key, required this.icon, required this.title, required this.message, this.onAction, this.actionLabel='Try again'});
  @override
  Widget build(BuildContext context) => Center(child: Padding(padding: const EdgeInsets.all(32), child: Column(mainAxisSize: MainAxisSize.min, children: [
    Container(padding: const EdgeInsets.all(18), decoration: BoxDecoration(color: Theme.of(context).colorScheme.primary.withOpacity(.08), shape: BoxShape.circle), child: Icon(icon, size: 42, color: Theme.of(context).colorScheme.primary)),
    const SizedBox(height: 16), Text(title, style: Theme.of(context).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w800)),
    const SizedBox(height: 6), Text(message, textAlign: TextAlign.center, style: Theme.of(context).textTheme.bodyMedium),
    if (onAction != null) ...[const SizedBox(height: 16), FilledButton(onPressed: onAction, child: Text(actionLabel))],
  ])));
}

class AppSearchField extends StatelessWidget {
  final TextEditingController controller;
  final String hint;
  final ValueChanged<String>? onChanged;
  const AppSearchField({super.key, required this.controller, this.hint='Search...', this.onChanged});
  @override
  Widget build(BuildContext context) => TextField(
    controller: controller, onChanged: onChanged,
    textInputAction: TextInputAction.search,
    decoration: InputDecoration(hintText: hint, prefixIcon: const Icon(Icons.search_rounded), suffixIcon: controller.text.isEmpty ? null : IconButton(onPressed: () { controller.clear(); onChanged?.call(''); }, icon: const Icon(Icons.clear_rounded))),
  );
}

class AppStatusChip extends StatelessWidget {
  final String label;
  const AppStatusChip(this.label, {super.key});
  @override
  Widget build(BuildContext context) {
    final value=label.toUpperCase();
    final scheme=Theme.of(context).colorScheme;
    final Color c = value.contains('COMPLETE') || value.contains('READY') || value=='ACTIVE' ? Colors.green : value.contains('CANCEL') || value.contains('REJECT') || value.contains('SUSPEND') ? Colors.red : scheme.primary;
    return Chip(avatar: Icon(Icons.circle, size: 9, color: c), label: Text(label, style: const TextStyle(fontWeight: FontWeight.w700, fontSize: 11)));
  }
}
