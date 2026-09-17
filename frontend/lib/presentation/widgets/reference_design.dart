import 'package:flutter/material.dart';
import '../../core/theme/app_theme.dart';

class AtuBrand {
  static Widget mark({double size = 42, Color? background}) => Container(
        width: size,
        height: size,
        decoration: BoxDecoration(
          color: background ?? AppTheme.accent,
          borderRadius: BorderRadius.circular(size * .24),
        ),
        child: Icon(Icons.restaurant_rounded,
            color: AppTheme.primaryDark, size: size * .58),
      );

  static Widget title({bool compact = false, Color color = Colors.white}) => Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          mark(size: compact ? 34 : 40),
          const SizedBox(width: 10),
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisSize: MainAxisSize.min,
            children: [
              Text('ATU CAFETERIA',
                  style: TextStyle(color: color, fontSize: compact ? 17 : 20,
                      fontWeight: FontWeight.w900, letterSpacing: .5)),
              if (!compact)
                Text('SMART CAMPUS FOOD PLATFORM',
                    style: TextStyle(color: color.withValues(alpha: .82),
                        fontSize: 8.5, fontWeight: FontWeight.w700, letterSpacing: .8)),
            ],
          ),
        ],
      );
}

class ReferencePage extends StatelessWidget {
  final String title;
  final String? subtitle;
  final Widget child;
  final List<Widget>? actions;
  final Widget? leading;
  const ReferencePage({super.key, required this.title, this.subtitle,
      required this.child, this.actions, this.leading});

  @override
  Widget build(BuildContext context) => Scaffold(
        appBar: AppBar(
          leading: leading,
          title: AtuBrand.title(compact: true),
          actions: actions,
        ),
        body: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Padding(
              padding: const EdgeInsets.fromLTRB(18, 18, 18, 8),
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Text(title, style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                    color: AppTheme.textDark, fontWeight: FontWeight.w900)),
                if (subtitle != null) ...[
                  const SizedBox(height: 3),
                  Text(subtitle!, style: const TextStyle(color: AppTheme.textMuted)),
                ],
              ]),
            ),
            Expanded(child: child),
          ],
        ),
      );
}

class ReferenceCard extends StatelessWidget {
  final Widget child;
  final EdgeInsets padding;
  const ReferenceCard({super.key, required this.child,
      this.padding = const EdgeInsets.all(16)});
  @override
  Widget build(BuildContext context) => Card(
        child: Padding(padding: padding, child: child),
      );
}

class StatusPill extends StatelessWidget {
  final String text;
  const StatusPill(this.text, {super.key});
  @override
  Widget build(BuildContext context) {
    final s = text.toUpperCase();
    final Color bg = s.contains('READY') ? const Color(0xFFE8F8EE)
        : s.contains('PREPAR') ? const Color(0xFFFFF6D6)
        : s.contains('COMPLE') || s.contains('DELIVER') ? const Color(0xFFE7F4FF)
        : s.contains('CANCEL') || s.contains('DECLIN') ? const Color(0xFFFFE9E9)
        : const Color(0xFFF0F4F8);
    final Color fg = s.contains('READY') ? AppTheme.success
        : s.contains('PREPAR') ? const Color(0xFF9A6A00)
        : s.contains('COMPLE') || s.contains('DELIVER') ? AppTheme.primary
        : s.contains('CANCEL') || s.contains('DECLIN') ? const Color(0xFFC62828)
        : AppTheme.textMuted;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
      decoration: BoxDecoration(color: bg, borderRadius: BorderRadius.circular(20)),
      child: Text(text, style: TextStyle(color: fg, fontSize: 11, fontWeight: FontWeight.w800)),
    );
  }
}

class FoodImage extends StatelessWidget {
  final String url;
  final double width;
  final double height;
  const FoodImage({super.key, required this.url, this.width = 100, this.height = 86});
  @override
  Widget build(BuildContext context) {
    if (url.trim().isEmpty) {
      return Container(width: width, height: height, color: AppTheme.primary.withValues(alpha: .08),
          child: const Icon(Icons.restaurant, color: AppTheme.primary, size: 34));
    }
    return ClipRRect(
      borderRadius: BorderRadius.circular(14),
      child: Image.network(url, width: width, height: height, fit: BoxFit.cover,
          errorBuilder: (_, __, ___) => Container(width: width, height: height,
              color: AppTheme.primary.withValues(alpha: .08),
              child: const Icon(Icons.restaurant, color: AppTheme.primary, size: 34))),
    );
  }
}

class MetricTile extends StatelessWidget {
  final String label;
  final String value;
  final IconData icon;
  const MetricTile({super.key, required this.label, required this.value, required this.icon});
  @override
  Widget build(BuildContext context) => ReferenceCard(
        child: Row(children: [
          Container(width: 46, height: 46,
              decoration: BoxDecoration(color: AppTheme.primary.withValues(alpha: .09), shape: BoxShape.circle),
              child: Icon(icon, color: AppTheme.primary)),
          const SizedBox(width: 12),
          Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Text(label, style: const TextStyle(fontSize: 12, color: AppTheme.textMuted)),
            const SizedBox(height: 3),
            Text(value, style: const TextStyle(fontSize: 22, color: AppTheme.textDark, fontWeight: FontWeight.w900)),
          ])),
        ]),
      );
}

class Sidebar extends StatelessWidget {
  final String selected;
  final ValueChanged<String> onSelected;
  final VoidCallback onLogout;
  final bool vendor;
  const Sidebar({super.key, required this.selected, required this.onSelected,
      required this.onLogout, this.vendor = false});
  @override
  Widget build(BuildContext context) => Container(
        width: 220,
        color: AppTheme.primaryDark,
        child: SafeArea(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Padding(padding: const EdgeInsets.all(18), child: AtuBrand.title()),
          const Divider(color: Colors.white24),
          ...(vendor
              ? ['Dashboard','Menu Catalog','Orders','Kiosk','Performance','Reviews','Transactions','Store Settings','Notifications']
              : ['Dashboard','Users','Vendors','Orders','Finance','Food & Menu','Ratings & Reviews','Reports','System Settings'])
              .map((item) => InkWell(
                    onTap: () => onSelected(item),
                    child: Container(width: double.infinity,
                      padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 13),
                      color: selected == item ? Colors.white.withValues(alpha: .13) : Colors.transparent,
                      child: Row(children: [
                        Icon(_iconFor(item), color: selected == item ? AppTheme.accent : Colors.white70, size: 19),
                        const SizedBox(width: 12),
                        Expanded(child: Text(item, style: TextStyle(color: Colors.white,
                            fontWeight: selected == item ? FontWeight.w900 : FontWeight.w500, fontSize: 12))),
                      ]),
                    ),
                  )),
          const Spacer(),
          InkWell(onTap: onLogout, child: const Padding(padding: EdgeInsets.all(18), child: Row(children: [
            Icon(Icons.logout, color: Color(0xFFFF7777), size: 19), SizedBox(width: 12),
            Text('Logout', style: TextStyle(color: Color(0xFFFF7777), fontWeight: FontWeight.w800)),
          ]))),
          Padding(padding: const EdgeInsets.fromLTRB(18, 0, 18, 18),
              child: Text('Good Food • Healthy Minds • Brighter Future', style: TextStyle(color: Colors.white54, fontSize: 8))),
        ])),
      );

  IconData _iconFor(String item) {
    switch (item) {
      case 'Dashboard': return Icons.dashboard_outlined;
      case 'Menu Catalog': return Icons.restaurant_menu;
      case 'Orders': return Icons.receipt_long;
      case 'Kiosk': return Icons.point_of_sale_outlined;
      case 'Performance': case 'Reports': return Icons.insights;
      case 'Reviews': case 'Ratings & Reviews': return Icons.star_outline;
      case 'Transactions': case 'Finance': return Icons.payments_outlined;
      case 'Store Settings': case 'System Settings': return Icons.settings_outlined;
      case 'Notifications': return Icons.notifications_none;
      case 'Users': return Icons.people_outline;
      case 'Vendors': return Icons.storefront_outlined;
      case 'Food & Menu': return Icons.fastfood_outlined;
      default: return Icons.circle_outlined;
    }
  }
}

class StepTrack extends StatelessWidget {
  final String status;
  const StepTrack({super.key, required this.status});
  @override
  Widget build(BuildContext context) {
    final s = status.toUpperCase();
    int active = s == 'PREPARING' ? 1 : (s.contains('READY') ? 2 : (s.contains('COMPLE') || s.contains('DELIVER') ? 3 : 0));
    const labels = ['Received','Preparing','Ready','Picked Up'];
    return Row(children: List.generate(labels.length, (i) => Expanded(child: Column(children: [
      Row(children: [
        if (i > 0) Expanded(child: Container(height: 3, color: i <= active ? AppTheme.primary : AppTheme.border)),
        Container(width: 25, height: 25,
          decoration: BoxDecoration(shape: BoxShape.circle,
              color: i <= active ? (i == active ? AppTheme.accent : AppTheme.primary) : Colors.white,
              border: Border.all(color: i <= active ? AppTheme.primary : const Color(0xFFB8CDE3), width: 2)),
          child: i < active ? const Icon(Icons.check, size: 15, color: Colors.white) :
              Text('${i+1}', style: TextStyle(fontSize: 10, fontWeight: FontWeight.w800,
                  color: i == active ? AppTheme.primaryDark : AppTheme.textMuted))),
        if (i < labels.length - 1) Expanded(child: Container(height: 3, color: i < active ? AppTheme.primary : AppTheme.border)),
      ]),
      const SizedBox(height: 6),
      Text(labels[i], textAlign: TextAlign.center, style: TextStyle(fontSize: 10,
          color: i <= active ? AppTheme.textDark : AppTheme.textMuted,
          fontWeight: i == active ? FontWeight.w900 : FontWeight.w500)),
    ]))));
  }
}
