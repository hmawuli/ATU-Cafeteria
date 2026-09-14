import 'dart:async';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/cafeteria_provider.dart';

class VendorOrderDisplayScreen extends StatefulWidget {
  const VendorOrderDisplayScreen({super.key});
  @override
  State<VendorOrderDisplayScreen> createState() => _VendorOrderDisplayScreenState();
}

class _VendorOrderDisplayScreenState extends State<VendorOrderDisplayScreen> {
  Timer? _timer;

  @override
  void initState() {
    super.initState();
    _timer = Timer.periodic(const Duration(seconds: 10), (_) {
      if (mounted) context.read<CafeteriaProvider>().refreshAllData();
    });
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final provider = context.watch<CafeteriaProvider>();
    final orders = [...provider.vendorOrders]
      ..sort((a, b) => (b.id ?? 0).compareTo(a.id ?? 0));

    return Scaffold(
      appBar: AppBar(
        title: const Text('ATU CAFETERIA • ORDER DISPLAY'),
        actions: [
          IconButton(
            tooltip: 'Refresh',
            onPressed: () => provider.refreshAllData(),
            icon: const Icon(Icons.refresh),
          ),
          const SizedBox(width: 12),
        ],
      ),
      body: orders.isEmpty
          ? const Center(
              child: Text('NO ACTIVE ORDERS',
                  style: TextStyle(fontSize: 28, fontWeight: FontWeight.w900)))
          : GridView.builder(
              padding: const EdgeInsets.all(24),
              gridDelegate: const SliverGridDelegateWithMaxCrossAxisExtent(
                maxCrossAxisExtent: 480,
                mainAxisExtent: 300,
                crossAxisSpacing: 20,
                mainAxisSpacing: 20,
              ),
              itemCount: orders.length,
              itemBuilder: (_, index) {
                final order = orders[index];
                final status = order.status.toUpperCase();
                final next = status == 'PENDING' || status == 'ORDER_PLACED'
                    ? 'PREPARING'
                    : status == 'PREPARING'
                        ? 'READY'
                        : status == 'READY'
                            ? 'COMPLETED'
                            : null;

                return Card(
                  child: Padding(
                    padding: const EdgeInsets.all(22),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        Row(
                          children: [
                            Text('#${order.id ?? '-'}',
                                style: const TextStyle(
                                    fontSize: 30, fontWeight: FontWeight.w900)),
                            const Spacer(),
                            Chip(label: Text(status)),
                          ],
                        ),
                        const SizedBox(height: 12),
                        Text(order.foodName,
                            maxLines: 2,
                            overflow: TextOverflow.ellipsis,
                            style: const TextStyle(
                                fontSize: 23, fontWeight: FontWeight.w800)),
                        const SizedBox(height: 8),
                        Text('Quantity: ${order.quantity}'),
                        Text('Total: GH₵ ${order.totalPrice.toStringAsFixed(2)}'),
                        const Spacer(),
                        if (next != null)
                          FilledButton(
                            onPressed: () => provider.updateOrderStatus(
                                order.id!, next),
                            child: Text(
                              next == 'PREPARING'
                                  ? 'ACCEPT • START PREPARING'
                                  : next == 'READY'
                                      ? 'MARK READY'
                                      : 'MARK COMPLETED',
                            ),
                          ),
                      ],
                    ),
                  ),
                );
              },
            ),
    );
  }
}
