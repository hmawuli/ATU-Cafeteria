import 'dart:async';

import 'package:flutter/material.dart';
import 'package:atu_cafeteria/core/network/api_client.dart';

class OrderTrackingScreen extends StatefulWidget {
  final int orderId;
  const OrderTrackingScreen({super.key, required this.orderId});

  @override
  State<OrderTrackingScreen> createState() => _OrderTrackingScreenState();
}

class _OrderTrackingScreenState extends State<OrderTrackingScreen> {
  final ApiClient _api = ApiClient();
  Timer? _timer;
  Map<String, dynamic>? _order;
  List<dynamic> _stages = const [];
  String? _error;
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _load();
    _timer =
        Timer.periodic(const Duration(seconds: 4), (_) => _load(silent: true));
  }

  @override
  void dispose() {
    _timer?.cancel();
    _api.close();
    super.dispose();
  }

  Future<void> _load({bool silent = false}) async {
    if (!silent && mounted) setState(() => _loading = true);
    try {
      final value = await _api.get('/orders/${widget.orderId}/tracking');
      if (!mounted) return;
      final data =
          value is Map ? Map<String, dynamic>.from(value) : <String, dynamic>{};
      setState(() {
        _order = data;
        _stages = data['stages'] is List
            ? List<dynamic>.from(data['stages'])
            : const [];
        _error = null;
        _loading = false;
      });
      final status = data['status']?.toString().toUpperCase();
      if (status == 'COMPLETED' ||
          status == 'DELIVERED' ||
          status == 'CANCELLED' ||
          status == 'DECLINED') {
        _timer?.cancel();
      }
    } catch (e) {
      if (!mounted) return;
      if (!silent) {
        setState(() {
          _error = e.toString();
          _loading = false;
        });
      }
    }
  }

  Future<void> _cancelOrder() async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Cancel this order?'),
        content: const Text(
          'You can cancel while the order is still being processed. This action cannot be undone.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx, false),
            child: const Text('Keep order'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(ctx, true),
            child: const Text('Cancel order'),
          ),
        ],
      ),
    );
    if (confirmed != true || !mounted) return;

    try {
      await _api.post('/orders/${widget.orderId}/cancel');
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Order cancelled successfully.')),
      );
      await _load();
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(e.toString().replaceFirst('Exception: ', ''))),
      );
    }
  }

  String _label(String status) {
    switch (status.toUpperCase()) {
      case 'PENDING':
      case 'ORDER_PLACED':
        return 'Order received';
      case 'PREPARING':
        return 'Preparing your meal';
      case 'READY':
        return 'Ready for pickup';
      case 'OUT_FOR_DELIVERY':
        return 'Out for delivery';
      case 'DELIVERED':
      case 'COMPLETED':
        return 'Order completed';
      case 'CANCELLED':
        return 'Order cancelled';
      case 'DECLINED':
        return 'Order declined';
      default:
        return status.replaceAll('_', ' ');
    }
  }

  @override
  Widget build(BuildContext context) {
    final status = _order?['status']?.toString() ?? 'PENDING';
    final eta = _order?['estimated_pickup_time']?.toString();
    final terminal = const {'COMPLETED', 'DELIVERED', 'CANCELLED', 'DECLINED'}
        .contains(status.toUpperCase());

    return Scaffold(
      appBar: AppBar(
        title: Text('Order #${widget.orderId}'),
        actions: [
          IconButton(
            tooltip: 'Refresh',
            onPressed: _loading ? null : _load,
            icon: const Icon(Icons.refresh_rounded),
          ),
        ],
      ),
      body: _loading && _order == null
          ? const Center(child: CircularProgressIndicator())
          : _error != null && _order == null
              ? _errorView()
              : RefreshIndicator(
                  onRefresh: _load,
                  child: ListView(
                    padding: const EdgeInsets.all(20),
                    children: [
                      Card(
                        clipBehavior: Clip.antiAlias,
                        child: Padding(
                          padding: const EdgeInsets.all(20),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                terminal ? _label(status) : 'We’re on it!',
                                style: Theme.of(context)
                                    .textTheme
                                    .headlineSmall
                                    ?.copyWith(
                                      fontWeight: FontWeight.w900,
                                    ),
                              ),
                              const SizedBox(height: 6),
                              Text(
                                terminal
                                    ? 'Your order status is up to date.'
                                    : 'Your order is being prepared. This page updates automatically.',
                              ),
                              const SizedBox(height: 18),
                              Row(
                                children: [
                                  Icon(
                                    status.toUpperCase() == 'CANCELLED'
                                        ? Icons.cancel_outlined
                                        : Icons.schedule_rounded,
                                    size: 28,
                                  ),
                                  const SizedBox(width: 10),
                                  Expanded(
                                    child: Text(
                                      eta == null ||
                                              eta.isEmpty ||
                                              eta == 'Calculating...'
                                          ? 'Pickup time: Calculating...'
                                          : 'Pickup time: $eta',
                                      style: const TextStyle(
                                          fontWeight: FontWeight.w800),
                                    ),
                                  ),
                                ],
                              ),
                            ],
                          ),
                        ),
                      ),
                      const SizedBox(height: 18),
                      Card(
                        child: Padding(
                          padding: const EdgeInsets.fromLTRB(18, 20, 18, 10),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              const Text(
                                'Order progress',
                                style: TextStyle(
                                    fontSize: 18, fontWeight: FontWeight.w900),
                              ),
                              const SizedBox(height: 12),
                              ..._stages.asMap().entries.map((entry) {
                                final stage = entry.value is Map
                                    ? Map<String, dynamic>.from(
                                        entry.value as Map)
                                    : <String, dynamic>{};
                                final completed = stage['completed'] == true;
                                final active = stage['active'] == true;
                                final name =
                                    stage['name']?.toString() ?? 'Step';
                                return ListTile(
                                  contentPadding: EdgeInsets.zero,
                                  leading: CircleAvatar(
                                    child: Icon(
                                      completed
                                          ? Icons.check_rounded
                                          : Icons.circle_outlined,
                                      size: 20,
                                    ),
                                  ),
                                  title: Text(
                                    name,
                                    style: TextStyle(
                                      fontWeight: active || completed
                                          ? FontWeight.w800
                                          : FontWeight.w500,
                                    ),
                                  ),
                                  subtitle: active
                                      ? const Text('Current status')
                                      : null,
                                );
                              }),
                            ],
                          ),
                        ),
                      ),
                      const SizedBox(height: 18),
                      if (status.toUpperCase() == 'READY')
                        const Card(
                          child: ListTile(
                            leading: Icon(Icons.pin_outlined),
                            title: Text('Ready for pickup'),
                            subtitle: Text(
                                'Use the pickup PIN shown after checkout at the vendor counter.'),
                          ),
                        ),
                      if (const {'PENDING', 'ORDER_PLACED', 'PREPARING'}
                          .contains(status.toUpperCase()))
                        Card(
                          child: Padding(
                            padding: const EdgeInsets.all(12),
                            child: SizedBox(
                              width: double.infinity,
                              child: OutlinedButton.icon(
                                onPressed: _cancelOrder,
                                icon: const Icon(Icons.cancel_outlined),
                                label: const Text('Cancel order'),
                              ),
                            ),
                          ),
                        ),
                      if (status.toUpperCase() == 'CANCELLED' ||
                          status.toUpperCase() == 'DECLINED')
                        Card(
                          child: ListTile(
                            leading: const Icon(Icons.info_outline),
                            title: Text(_label(status)),
                            subtitle: const Text(
                                'Please contact the cafeteria if you need assistance with this order.'),
                          ),
                        ),
                    ],
                  ),
                ),
    );
  }

  Widget _errorView() => Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Icon(Icons.cloud_off_rounded, size: 52),
              const SizedBox(height: 12),
              Text(_error ?? 'Unable to load order tracking.',
                  textAlign: TextAlign.center),
              const SizedBox(height: 14),
              FilledButton.icon(
                onPressed: _load,
                icon: const Icon(Icons.refresh_rounded),
                label: const Text('Try again'),
              ),
            ],
          ),
        ),
      );
}
