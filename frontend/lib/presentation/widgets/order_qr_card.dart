import 'package:flutter/material.dart';
import 'package:qr_flutter/qr_flutter.dart';

class OrderQrCard extends StatelessWidget {
  final int orderId;
  final String pickupCode;
  const OrderQrCard({super.key, required this.orderId, required this.pickupCode});

  @override
  Widget build(BuildContext context) {
    final payload = 'ATU|ORDER:$orderId|PICKUP:$pickupCode';
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(children: [
          Text('QR COLLECTION PASS', style: Theme.of(context).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold)),
          const SizedBox(height: 12),
          QrImageView(data: payload, size: 190, version: QrVersions.auto),
          const SizedBox(height: 8),
          Text('Order #$orderId', style: Theme.of(context).textTheme.titleMedium),
          Text('Pickup code: $pickupCode'),
          const SizedBox(height: 8),
          const Text('The vendor should verify the order and pickup code before completing collection.', textAlign: TextAlign.center),
        ]),
      ),
    );
  }
}
