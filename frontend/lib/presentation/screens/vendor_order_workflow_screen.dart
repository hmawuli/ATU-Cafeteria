import 'dart:async';
import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'package:provider/provider.dart';

import '../../core/theme/app_theme.dart';
import '../../domain/models/models.dart';
import '../providers/cafeteria_provider.dart';

class VendorOrderWorkflowScreen extends StatefulWidget {
  const VendorOrderWorkflowScreen({super.key});

  @override
  State<VendorOrderWorkflowScreen> createState() => _VendorOrderWorkflowScreenState();
}

class _VendorOrderWorkflowScreenState extends State<VendorOrderWorkflowScreen> {
  Timer? _timer;
  List<Order> _orders = const [];
  bool _loading = true;
  bool _busy = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    _loadOrders();
    _timer = Timer.periodic(const Duration(seconds: 5), (_) => _loadOrders(silent: true));
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  Future<void> _loadOrders({bool silent = false}) async {
    final provider = context.read<CafeteriaProvider>();
    final token = provider.authToken;
    final vendorId = provider.currentUser?.id;
    if (token == null || token.isEmpty || vendorId == null) return;

    try {
      final response = await http.get(
        Uri.parse('${provider.laravelBaseUrl}/api/vendor/my-orders'),
        headers: {
          'Accept': 'application/json',
          'Authorization': 'Bearer $token',
        },
      ).timeout(const Duration(seconds: 8));

      if (response.statusCode != 200) {
        throw Exception('Server returned ${response.statusCode}.');
      }
      final decoded = response.body.isEmpty ? null : jsonDecode(response.body);
      if (decoded is! List) throw Exception('Invalid order response.');

      final orders = <Order>[];
      for (final raw in decoded) {
        if (raw is! Map) continue;
        try {
          orders.add(Order.fromJson(Map<String, dynamic>.from(raw)));
        } catch (_) {}
      }
      orders.sort((a, b) => (b.id ?? 0).compareTo(a.id ?? 0));
      if (!mounted) return;
      setState(() {
        _orders = orders;
        _loading = false;
        _error = null;
      });
    } catch (e) {
      if (!mounted) return;
      if (!silent) {
        setState(() {
          _loading = false;
          _error = 'Unable to load live orders. Check that Laravel is running.';
        });
      }
    }
  }

  Future<void> _advance(Order order) async {
    final status = order.status.toUpperCase();
    if (order.id == null) return;
    if (status == 'READY' || status == 'READY_FOR_PICKUP') {
      await _verifyPickup(order);
      return;
    }

    final next = status == 'PENDING' || status == 'ORDER_PLACED'
        ? 'PREPARING'
        : status == 'PREPARING'
            ? 'READY'
            : null;
    if (next == null) return;

    setState(() => _busy = true);
    try {
      final provider = context.read<CafeteriaProvider>();
      await provider.updateOrderStatus(order.id!, next);
      await _loadOrders(silent: true);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Order #${order.id} moved to $next.')),
        );
      }
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Future<void> _verifyPickup(Order order) async {
    final controller = TextEditingController();
    final pin = await showDialog<String>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text('Verify pickup • #${order.id}'),
        content: TextField(
          controller: controller,
          autofocus: true,
          keyboardType: TextInputType.number,
          maxLength: 4,
          obscureText: true,
          decoration: const InputDecoration(
            labelText: 'Student pickup PIN',
            hintText: '4 digits',
            prefixIcon: Icon(Icons.lock_outline),
          ),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Cancel')),
          FilledButton(
            onPressed: () {
              final value = controller.text.trim();
              if (value.length == 4) Navigator.pop(ctx, value);
            },
            child: const Text('Verify & Complete'),
          ),
        ],
      ),
    );
    controller.dispose();
    if (pin == null || !mounted || order.id == null) return;

    setState(() => _busy = true);
    try {
      final provider = context.read<CafeteriaProvider>();
      final token = provider.authToken;
      if (token == null || token.isEmpty) throw Exception('Session expired.');
      final response = await http.post(
        Uri.parse('${provider.laravelBaseUrl}/api/orders/${order.id}/verify-pickup'),
        headers: {
          'Accept': 'application/json',
          'Content-Type': 'application/json',
          'Authorization': 'Bearer $token',
        },
        body: jsonEncode({'pickup_pin': pin}),
      ).timeout(const Duration(seconds: 8));

      final body = response.body.isEmpty ? null : jsonDecode(response.body);
      final message = body is Map ? body['message']?.toString() : null;
      if (response.statusCode != 200) {
        throw Exception(message ?? 'Pickup verification failed.');
      }
      await _loadOrders(silent: true);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(message ?? 'Pickup verified. Order completed.')),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(e.toString().replaceFirst('Exception: ', ''))),
        );
      }
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  String _actionLabel(Order order) {
    switch (order.status.toUpperCase()) {
      case 'PENDING':
      case 'ORDER_PLACED':
        return 'ACCEPT • PREPARE';
      case 'PREPARING':
        return 'MARK READY';
      case 'READY':
      case 'READY_FOR_PICKUP':
        return 'VERIFY PICKUP';
      default:
        return '';
    }
  }

  Color _statusColor(String status) {
    switch (status.toUpperCase()) {
      case 'COMPLETED':
        return AppTheme.success;
      case 'READY':
      case 'READY_FOR_PICKUP':
        return AppTheme.accent;
      case 'PREPARING':
        return AppTheme.primary;
      case 'CANCELLED':
      case 'DECLINED':
        return Colors.red;
      default:
        return AppTheme.textMuted;
    }
  }

  @override
  Widget build(BuildContext context) {
    final active = _orders.where((o) => !['COMPLETED', 'DELIVERED', 'CANCELLED', 'DECLINED'].contains(o.status.toUpperCase())).length;
    return Scaffold(
      appBar: AppBar(
        title: const Text('Vendor Order Centre'),
        actions: [
          IconButton(onPressed: _loadOrders, icon: const Icon(Icons.refresh)),
          IconButton(
            tooltip: 'Full vendor dashboard',
            onPressed: () => Navigator.pushNamed(context, '/vendor-dashboard'),
            icon: const Icon(Icons.dashboard_outlined),
          ),
        ],
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : RefreshIndicator(
              onRefresh: _loadOrders,
              child: ListView(
                padding: const EdgeInsets.all(16),
                children: [
                  Container(
                    padding: const EdgeInsets.all(20),
                    decoration: BoxDecoration(
                      gradient: const LinearGradient(colors: [AppTheme.primaryDark, AppTheme.primary]),
                      borderRadius: BorderRadius.circular(20),
                    ),
                    child: Row(
                      children: [
                        const Icon(Icons.receipt_long, color: Colors.white, size: 34),
                        const SizedBox(width: 14),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              const Text('LIVE ORDERS', style: TextStyle(color: Colors.white, fontWeight: FontWeight.w900, fontSize: 20)),
                              const SizedBox(height: 4),
                              Text('$active active • updates every 5 seconds', style: const TextStyle(color: Colors.white70)),
                            ],
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 16),
                  if (_error != null)
                    Card(child: Padding(padding: const EdgeInsets.all(16), child: Text(_error!))),
                  if (_orders.isEmpty)
                    const Card(child: Padding(padding: EdgeInsets.all(28), child: Center(child: Text('No customer orders yet.')))),
                  ..._orders.map(_orderCard),
                ],
              ),
            ),
    );
  }

  Widget _orderCard(Order order) {
    final status = order.status.toUpperCase();
    final action = _actionLabel(order);
    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      child: Padding(
        padding: const EdgeInsets.all(18),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(
                  child: Text('Order #${order.id ?? '—'}', style: const TextStyle(fontWeight: FontWeight.w900, fontSize: 17)),
                ),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                  decoration: BoxDecoration(
                    color: _statusColor(status).withValues(alpha: .12),
                    borderRadius: BorderRadius.circular(20),
                  ),
                  child: Text(status, style: TextStyle(fontWeight: FontWeight.w800, color: _statusColor(status))),
                ),
              ],
            ),
            const SizedBox(height: 12),
            Text('Customer: ${order.customerName ?? 'Student'}'),
            const SizedBox(height: 4),
            Text('Total: GH₵ ${order.totalAmount.toStringAsFixed(2)}', style: const TextStyle(fontWeight: FontWeight.w800)),
            if (order.items.isNotEmpty) ...[
              const SizedBox(height: 10),
              ...order.items.map(
                (item) => Padding(
                  padding: const EdgeInsets.only(bottom: 4),
                  child: Text('• ${item.name} × ${item.quantity}'),
                ),
              ),
            ],
            if (action.isNotEmpty) ...[
              const SizedBox(height: 14),
              SizedBox(
                width: double.infinity,
                child: FilledButton.icon(
                  onPressed: _busy ? null : () => _advance(order),
                  icon: Icon(status == 'READY' || status == 'READY_FOR_PICKUP' ? Icons.verified_user : Icons.arrow_forward),
                  label: Text(action),
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }
}
