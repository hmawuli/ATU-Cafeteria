import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../models/order.dart';
import '../providers/cafeteria_provider.dart';

class OrderTrackingScreen extends StatelessWidget {
  final Order order;

  const OrderTrackingScreen({Key? key, required this.order}) : super(key: key);

  int _getStepIndex(String status) {
    switch (status.toUpperCase()) {
      case 'RECEIVED':
      case 'PENDING':
      case 'ORDER_PLACED':
        return 0;
      case 'PREPARING':
        return 1;
      case 'OUT FOR DELIVERY':
      case 'OUT_FOR_DELIVERY':
      case 'READY':
      case 'READY_FOR_PICKUP':
        return 2;
      case 'DELIVERED':
      case 'COMPLETED':
        return 3;
      default:
        return 0;
    }
  }

  @override
  Widget build(BuildContext context) {
    final cafeteria = Provider.of<CafeteriaProvider>(context);
    // Find live order state
    final liveOrder = cafeteria.orders.firstWhere(
      (o) => o.id == order.id,
      orElse: () => order,
    );

    final activeStep = _getStepIndex(liveOrder.status);
    final steps = ['Received', 'Preparing', 'Out for Delivery', 'Delivered'];

    return Scaffold(
      appBar: AppBar(
        title: Text('Order #${liveOrder.id} Tracking'),
        backgroundColor: Colors.deepOrange,
        foregroundColor: Colors.white,
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(20.0),
        child: Column(
          children: [
            Container(
              padding: const EdgeInsets.all(24),
              decoration: BoxDecoration(
                color: Colors.deepOrange.shade50,
                shape: BoxShape.circle,
              ),
              child: const Icon(Icons.delivery_dining, size: 72, color: Colors.deepOrange),
            ),
            const SizedBox(height: 16),
            Text(
              liveOrder.foodName,
              style: const TextStyle(fontSize: 22, fontWeight: FontWeight.bold),
            ),
            Text(
              'GH₵ ${liveOrder.totalPrice.toStringAsFixed(2)} • PIN: ${liveOrder.pickupPin}',
              style: TextStyle(fontSize: 16, color: Colors.grey.shade700),
            ),
            const SizedBox(height: 24),
            // Progress Timeline Stepper
            Card(
              elevation: 2,
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
              child: Padding(
                padding: const EdgeInsets.all(20.0),
                child: Column(
                  children: List.generate(steps.length, (index) {
                    final isDone = index <= activeStep;
                    final isCurrent = index == activeStep;

                    return Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Column(
                          children: [
                            CircleAvatar(
                              radius: 14,
                              backgroundColor: isDone ? Colors.green : Colors.grey.shade300,
                              child: Icon(
                                isDone ? Icons.check : Icons.circle,
                                size: 14,
                                color: Colors.white,
                              ),
                            ),
                            if (index != steps.length - 1)
                              Container(
                                width: 2,
                                height: 40,
                                color: index < activeStep ? Colors.green : Colors.grey.shade300,
                              ),
                          ],
                        ),
                        const SizedBox(width: 16),
                        Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              steps[index],
                              style: TextStyle(
                                fontSize: 16,
                                fontWeight: isCurrent ? FontWeight.bold : FontWeight.normal,
                                color: isCurrent ? Colors.black : Colors.grey.shade600,
                              ),
                            ),
                            if (isCurrent)
                              Text(
                                index == 0
                                    ? 'Kitchen received your ticket'
                                    : index == 1
                                        ? 'Food is on the stove'
                                        : index == 2
                                            ? 'Ready for pickup at counter'
                                            : 'Meal completed. Enjoy!',
                                style: const TextStyle(fontSize: 12, color: Colors.deepOrange),
                              ),
                          ],
                        ),
                      ],
                    );
                  }),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
