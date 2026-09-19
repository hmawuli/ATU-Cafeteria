import 'package:flutter/material.dart';
import 'package:atu_cafeteria/domain/models/models.dart';

class KfcOrderingSections extends StatelessWidget {
  final List<FoodItem> items;
  final void Function(FoodItem item) onAdd;
  const KfcOrderingSections(
      {super.key, required this.items, required this.onAdd});

  @override
  Widget build(BuildContext context) {
    final featured = items.take(6).toList();
    return Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      Container(
          width: double.infinity,
          padding: const EdgeInsets.all(20),
          decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(22),
              gradient: LinearGradient(colors: [
                Theme.of(context).colorScheme.primary,
                Theme.of(context).colorScheme.primaryContainer
              ])),
          child: const Row(children: [
            Expanded(
                child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                  Text('Hungry?',
                      style: TextStyle(color: Colors.white70, fontSize: 15)),
                  SizedBox(height: 4),
                  Text('Your meal is just a few taps away.',
                      style: TextStyle(
                          color: Colors.white,
                          fontSize: 23,
                          fontWeight: FontWeight.w900)),
                  SizedBox(height: 8),
                  Text('Fresh campus meals. Easy pickup. Fast ordering.',
                      style: TextStyle(color: Colors.white70)),
                ])),
            Icon(Icons.fastfood_rounded, size: 62, color: Colors.white)
          ])),
      const SizedBox(height: 18),
      Text('Popular on campus',
          style: Theme.of(context)
              .textTheme
              .titleLarge
              ?.copyWith(fontWeight: FontWeight.w900)),
      Text('Quick picks students love',
          style: Theme.of(context).textTheme.bodySmall),
      const SizedBox(height: 10),
      SizedBox(
          height: 225,
          child: ListView.separated(
              scrollDirection: Axis.horizontal,
              itemCount: featured.length,
              separatorBuilder: (_, __) => const SizedBox(width: 12),
              itemBuilder: (_, i) => _mealCard(context, featured[i]))),
      const SizedBox(height: 22),
      Text('Order your way',
          style: Theme.of(context)
              .textTheme
              .titleLarge
              ?.copyWith(fontWeight: FontWeight.w900)),
      Text('Choose what works for you',
          style: Theme.of(context).textTheme.bodySmall),
      const SizedBox(height: 10),
      Row(children: [
        Expanded(
            child: _choice(
                context, Icons.storefront, 'Pickup', 'Collect when ready')),
        const SizedBox(width: 10),
        Expanded(
            child: _choice(context, Icons.schedule, 'Schedule', 'Plan ahead')),
        const SizedBox(width: 10),
        Expanded(
            child: _choice(
                context, Icons.delivery_dining, 'Delivery', 'Campus delivery'))
      ]),
    ]);
  }

  Widget _mealCard(BuildContext context, FoodItem item) => SizedBox(
      width: 190,
      child: Card(
          clipBehavior: Clip.antiAlias,
          child: Padding(
              padding: const EdgeInsets.all(12),
              child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Expanded(
                        child: Container(
                            width: double.infinity,
                            decoration: BoxDecoration(
                                borderRadius: BorderRadius.circular(14),
                                color: Theme.of(context)
                                    .colorScheme
                                    .primary
                                    .withValues(alpha: .08)),
                            child: item.imageUrl.trim().isEmpty
                                ? Icon(Icons.restaurant_rounded,
                                    size: 48,
                                    color:
                                        Theme.of(context).colorScheme.primary)
                                : ClipRRect(
                                    borderRadius: BorderRadius.circular(14),
                                    child: Image.network(item.imageUrl,
                                        fit: BoxFit.cover,
                                        errorBuilder: (_, __, ___) =>
                                            const Icon(Icons.restaurant_rounded,
                                                size: 48))))),
                    const SizedBox(height: 8),
                    Text(item.name,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(fontWeight: FontWeight.w800)),
                    const SizedBox(height: 3),
                    Row(children: [
                      Text('GH₵ ${item.price.toStringAsFixed(2)}',
                          style: TextStyle(
                              fontWeight: FontWeight.w900,
                              color: Theme.of(context).colorScheme.primary)),
                      const Spacer(),
                      IconButton(
                          visualDensity: VisualDensity.compact,
                          onPressed:
                              item.isAvailable ? () => onAdd(item) : null,
                          icon: const Icon(Icons.add_circle_rounded),
                          tooltip: 'Add to cart')
                    ])
                  ]))));

  Widget _choice(
          BuildContext context, IconData icon, String title, String subtitle) =>
      Card(
          child: Padding(
              padding: const EdgeInsets.all(12),
              child: Column(children: [
                Icon(icon,
                    size: 28, color: Theme.of(context).colorScheme.primary),
                const SizedBox(height: 7),
                Text(title,
                    style: const TextStyle(fontWeight: FontWeight.w900)),
                const SizedBox(height: 2),
                Text(subtitle,
                    textAlign: TextAlign.center,
                    style: Theme.of(context).textTheme.bodySmall)
              ])));
}
