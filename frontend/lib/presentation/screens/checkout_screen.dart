import 'package:flutter/material.dart';
import 'package:url_launcher/url_launcher.dart';
import 'package:provider/provider.dart';
import '../providers/cart_provider.dart';
import '../providers/cafeteria_provider.dart';
import '../../core/network/api_client.dart';

class CheckoutScreen extends StatefulWidget {
  const CheckoutScreen({super.key});
  @override
  State<CheckoutScreen> createState() => _CheckoutScreenState();
}

class _CheckoutScreenState extends State<CheckoutScreen> {
  String _method = 'Wallet';
  String _fulfilment = 'Pickup now';
  DateTime? _scheduledPickup;
  final TextEditingController _pointsController = TextEditingController();
  final TextEditingController _noteController = TextEditingController();
  bool _submitting = false;
  final ApiClient _api = ApiClient();
  @override
  void dispose() {
    _noteController.dispose();
    _pointsController.dispose();
    _api.close();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final cart = context.watch<CartProvider>();
    final total = cart.subtotal;
    final points = int.tryParse(_pointsController.text.trim()) ?? 0;
    final discount = points * 0.10;
    final finalTotal = (total - discount).clamp(0.0, double.infinity).toDouble();
    return Scaffold(
      appBar: AppBar(title: const Text('Checkout')),
      body: cart.isEmpty
          ? const Center(child: Text('Your cart is empty.'))
          : ListView(
              padding: const EdgeInsets.all(20),
              children: [
                Text('Review your order',
                    style: Theme.of(context)
                        .textTheme
                        .headlineSmall
                        ?.copyWith(fontWeight: FontWeight.w900)),
                const SizedBox(height: 6),
                const Text(
                    'Confirm your meals and payment method before placing the order.'),
                const SizedBox(height: 18),
                Card(
                  child: Column(
                    children: cart.lines
                        .map((line) => ListTile(
                              title: Text(line.item.name,
                                  style: const TextStyle(
                                      fontWeight: FontWeight.w700)),
                              subtitle: Text('Quantity: ${line.quantity}'),
                              trailing: Text(
                                  'GH₵ ${line.total.toStringAsFixed(2)}',
                                  style: const TextStyle(
                                      fontWeight: FontWeight.w800)),
                            ))
                        .toList(),
                  ),
                ),
                const SizedBox(height: 18),
                _fulfilmentCard(context),
                const SizedBox(height: 18),
                Card(
                    child: Padding(
                        padding: const EdgeInsets.all(16),
                        child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              const Text('Payment method',
                                  style:
                                      TextStyle(fontWeight: FontWeight.w800)),
                              RadioGroup<String>(
                                groupValue: _method,
                                onChanged: (value) => setState(() => _method = value!),
                                child: const Column(children: [
                                  RadioListTile<String>(
                                    value: 'Wallet',
                                    title: Text('ATU Cafeteria Wallet'),
                                    secondary: Icon(Icons.account_balance_wallet_outlined),
                                  ),
                                  RadioListTile<String>(
                                    value: 'Online',
                                    title: Text('Mobile Money / Card'),
                                    subtitle: Text('Pay securely through Paystack.'),
                                    secondary: Icon(Icons.payments_outlined),
                                  ),
                                ]),
                              ),
                            ]))),
                const SizedBox(height: 18),
                Card(
                    child: Padding(
                        padding: const EdgeInsets.all(16),
                        child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              const Text('Use loyalty points',
                                  style: TextStyle(fontWeight: FontWeight.w800)),
                              const SizedBox(height: 8),
                              const Text('10 points = GH₵ 1.00 discount.'),
                              const SizedBox(height: 8),
                              TextField(
                                controller: _pointsController,
                                keyboardType: TextInputType.number,
                                onChanged: (_) => setState(() {}),
                                decoration: const InputDecoration(
                                  labelText: 'Points to redeem',
                                  prefixIcon: Icon(Icons.stars_outlined),
                                ),
                              ),
                            ]))),
                const SizedBox(height: 18),
                Card(
                    child: Padding(
                        padding: const EdgeInsets.all(16),
                        child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              const Text('Order note (optional)',
                                  style:
                                      TextStyle(fontWeight: FontWeight.w800)),
                              const SizedBox(height: 8),
                              TextField(
                                controller: _noteController,
                                maxLength: 160,
                                decoration: const InputDecoration(
                                  hintText: 'Add a note for the cafeteria',
                                  prefixIcon: Icon(Icons.notes_outlined),
                                ),
                              ),
                            ]))),
                const SizedBox(height: 18),
                Card(
                    child: Padding(
                        padding: const EdgeInsets.all(18),
                        child: Column(children: [
                          Row(children: [
                            const Text('Subtotal'),
                            const Spacer(),
                            Text('GH₵ ${total.toStringAsFixed(2)}')
                          ]),
                          if (discount > 0) ...[
                            const SizedBox(height: 8),
                            Row(children: [const Text('Loyalty discount'), const Spacer(), Text('- GH₵ ${discount.toStringAsFixed(2)}')]),
                          ],
                          const Divider(height: 28),
                          Row(children: [
                            const Text('Total',
                                style: TextStyle(
                                    fontSize: 18, fontWeight: FontWeight.w900)),
                            const Spacer(),
                            Text('GH₵ ${finalTotal.toStringAsFixed(2)}',
                                style: TextStyle(
                                    fontSize: 20,
                                    fontWeight: FontWeight.w900,
                                    color:
                                        Theme.of(context).colorScheme.primary))
                          ]),
                          const SizedBox(height: 16),
                          SizedBox(
                              width: double.infinity,
                              height: 52,
                              child: FilledButton.icon(
                                onPressed: _submitting
                                    ? null
                                    : () => _showConfirmation(context),
                                icon: const Icon(Icons.check_circle_outline),
                                label: Text(_submitting
                                    ? 'Processing...'
                                    : 'Place Order Securely'),
                              )),
                        ]))),
              ],
            ),
    );
  }

  Widget _fulfilmentCard(BuildContext context) => Card(
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            const Text('Pickup options', style: TextStyle(fontWeight: FontWeight.w800)),
            RadioGroup<String>(
              groupValue: _fulfilment,
              onChanged: (value) => setState(() => _fulfilment = value!),
              child: const Column(children: [
                RadioListTile<String>(value: 'Pickup now', title: Text('Pickup when ready'), subtitle: Text('Collect from the vendor when your order is ready.'), secondary: Icon(Icons.storefront_outlined)),
                RadioListTile<String>(value: 'Schedule pickup', title: Text('Schedule pickup'), subtitle: Text('Choose a future pickup time.'), secondary: Icon(Icons.schedule_outlined)),
              ]),
            ),
            if (_fulfilment == 'Schedule pickup')
              OutlinedButton.icon(
                onPressed: _pickSchedule,
                icon: const Icon(Icons.event_outlined),
                label: Text(_scheduledPickup == null ? 'Choose pickup time' : _formatDateTime(_scheduledPickup!)),
              ),
          ]),
        ),
      );

  Future<void> _pickSchedule() async {
    final now = DateTime.now();
    final date = await showDatePicker(context: context, firstDate: now, lastDate: now.add(const Duration(days: 14)), initialDate: _scheduledPickup ?? now);
    if (date == null || !mounted) return;
    final time = await showTimePicker(context: context, initialTime: _scheduledPickup == null ? TimeOfDay.fromDateTime(now.add(const Duration(hours: 1))) : TimeOfDay.fromDateTime(_scheduledPickup!));
    if (time == null || !mounted) return;
    final chosen = DateTime(date.year, date.month, date.day, time.hour, time.minute);
    if (chosen.isBefore(now.add(const Duration(minutes: 10)))) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Please choose a pickup time at least 10 minutes from now.')));
      return;
    }
    setState(() => _scheduledPickup = chosen);
  }

  String _formatDateTime(DateTime value) {
    final hour = value.hour == 0 ? 12 : (value.hour > 12 ? value.hour - 12 : value.hour);
    final minute = value.minute.toString().padLeft(2, '0');
    final period = value.hour >= 12 ? 'PM' : 'AM';
    return '${value.day}/${value.month}/${value.year} at $hour:$minute $period';
  }
  Future<void> _payOnline(CafeteriaProvider auth, CartProvider cart, int points, double finalTotal) async {
    final user = auth.currentUser;
    final email = user?.email;
    if (email == null || email.trim().isEmpty) {
      if (!mounted) return;
      ScaffoldMessenger.of(this.context).showSnackBar(
        const SnackBar(content: Text('A valid email address is required for online payment.')),
      );
      return;
    }

    setState(() => _submitting = true);
    try {
      final init = await auth.initializePaystackPayment(
        amount: finalTotal,
        email: email.trim(),
        purpose: 'DIRECT_ORDER_PAY',
      );
      if (init == null) throw Exception('Payment could not be initialized.');
      final reference = init['reference']?.toString();
      final authorizationUrl = init['authorization_url']?.toString();
      if (reference == null || authorizationUrl == null) {
        throw Exception('Payment gateway returned an incomplete response.');
      }

      final simulated = init['is_simulated'] == true;
      if (!simulated) {
        final launched = await launchUrl(
          Uri.parse(authorizationUrl),
          mode: LaunchMode.externalApplication,
        );
        if (!launched) throw Exception('Unable to open the payment page.');

        if (!mounted) return;
        final verified = await showDialog<bool>(
          context: this.context,
          barrierDismissible: false,
          builder: (ctx) => AlertDialog(
            title: const Text('Complete your payment'),
            content: const Text(
              'Finish the payment in the browser, then return here and tap “I have paid”.',
            ),
            actions: [
              TextButton(
                onPressed: () => Navigator.pop(ctx, false),
                child: const Text('Cancel'),
              ),
              FilledButton(
                onPressed: () => Navigator.pop(ctx, true),
                child: const Text('I have paid'),
              ),
            ],
          ),
        );
        if (verified != true || !mounted) return;
      } else {
        if (!mounted) return;
        final proceed = await showDialog<bool>(
          context: this.context,
          builder: (ctx) => AlertDialog(
            title: const Text('Payment simulation'),
            content: Text(
              'The backend is using its local Paystack simulation. Continue as a simulated successful payment of GH₵ ${finalTotal.toStringAsFixed(2)}?',
            ),
            actions: [
              TextButton(
                onPressed: () => Navigator.pop(ctx, false),
                child: const Text('Cancel'),
              ),
              FilledButton(
                onPressed: () => Navigator.pop(ctx, true),
                child: const Text('Simulate payment'),
              ),
            ],
          ),
        );
        if (proceed != true || !mounted) return;
      }

      final confirmed = await auth.verifyPaystackPayment(
        reference: reference,
        amount: finalTotal,
        purpose: 'DIRECT_ORDER_PAY',
      );
      if (!confirmed) throw Exception('Payment has not been confirmed by Paystack.');

      final result = await _api.post('/student/cart-checkout', body: {
        'items': cart.toCheckoutPayload(),
        'payment_method': 'momo',
        'payment_reference': reference,
        if (points > 0) 'points_to_redeem': points,
        'estimated_pickup_time': _fulfilment == 'Schedule pickup' && _scheduledPickup != null ? _scheduledPickup!.toIso8601String() : 'Calculating...',
        if (_noteController.text.trim().isNotEmpty) 'note': _noteController.text.trim(),
      });
      if (!mounted) return;
      final orderId = result is Map
          ? (result['order'] is Map ? (result['order']['id'] ?? result['id']) : result['id'])
          : null;
      cart.clear();
      final pickupPin = result is Map ? result['pickup_pin']?.toString() : null;
      await showDialog<void>(
        context: this.context,
        builder: (ctx) => AlertDialog(
          icon: const Icon(Icons.verified_outlined, size: 48),
          title: const Text('Payment confirmed'),
          content: Text(
            pickupPin == null
                ? 'Your order has been placed successfully.'
                : 'Order placed successfully. Pickup PIN: $pickupPin',
          ),
          actions: [
            FilledButton(
              onPressed: () {
                Navigator.pop(ctx);
                if (orderId != null) {
                  Navigator.pushReplacementNamed(
                    this.context,
                    '/order-tracking',
                    arguments: orderId is int ? orderId : int.tryParse(orderId.toString()),
                  );
                } else {
                  Navigator.pushReplacementNamed(context, '/student');
                }
              },
              child: const Text('View order'),
            ),
          ],
        ),
      );
    } catch (e) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(e.toString().replaceFirst('Exception: ', ''))),
        );
      }
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  Future<void> _showConfirmation(BuildContext context) async {
    final auth = context.read<CafeteriaProvider>();
    if (auth.currentUser == null) {
      final goToLogin = await showDialog<bool>(
        context: context,
        builder: (ctx) => AlertDialog(
          icon: const Icon(Icons.lock_outline, size: 42),
          title: const Text('Sign in required'),
          content: const Text(
            'Please sign in to place your order. Your cart will be kept while you sign in.',
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(ctx, false),
              child: const Text('Not now'),
            ),
            FilledButton(
              onPressed: () => Navigator.pop(ctx, true),
              child: const Text('Sign in'),
            ),
          ],
        ),
      );
      if (goToLogin == true && context.mounted) {
        Navigator.pushNamed(context, '/login');
      }
      return;
    }

    final cart = context.read<CartProvider>();
    if (cart.isEmpty) return;
    if (_fulfilment == 'Schedule pickup' && _scheduledPickup == null) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Choose a pickup time first.')));
      return;
    }
    final points = int.tryParse(_pointsController.text.trim()) ?? 0;
    if (points < 0) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Loyalty points cannot be negative.')));
      return;
    }
    final total = cart.subtotal;
    final finalTotal = (total - (points * 0.10)).clamp(0.0, double.infinity).toDouble();
    if (points > 0) {
      try {
        await _api.post('/student/loyalty/preview-discount', body: {'points_to_redeem': points});
      } on ApiException catch (e) {
        if (context.mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.message)));
        return;
      }
    }
    if (_method == 'Online') {
      if (!mounted) return;
      await _payOnline(auth, cart, points, finalTotal);
      return;
    }

    if (!mounted) return;
    final ok = await showDialog<bool>(
      context: this.context,
      builder: (ctx) => AlertDialog(
        title: const Text('Confirm order'),
        content: Text(
            'Place this order for GH₵ ${finalTotal.toStringAsFixed(2)} using your cafeteria wallet?'),
        actions: [
          TextButton(
              onPressed: () => Navigator.pop(ctx, false),
              child: const Text('Review')),
          FilledButton(
              onPressed: () => Navigator.pop(ctx, true),
              child: const Text('Confirm')),
        ],
      ),
    );
    if (ok != true || !mounted) return;
    setState(() => _submitting = true);
    try {
      final result = await _api.post('/student/cart-checkout', body: {
        'items': cart.toCheckoutPayload(),
        if (points > 0) 'points_to_redeem': points,
        if (_noteController.text.trim().isNotEmpty)
          'note': _noteController.text.trim(),
        'payment_method': _method.toLowerCase(),
        'estimated_pickup_time': _fulfilment == 'Schedule pickup' && _scheduledPickup != null ? _scheduledPickup!.toIso8601String() : 'Calculating...',
      });
      if (!context.mounted) return;
      final orderId = result is Map
          ? (result['order'] is Map ? (result['order']['id'] ?? result['id']) : result['id'])
          : null;
      cart.clear();
      final message = result is Map && result['message'] != null
          ? result['message'].toString()
          : 'Your order has been placed successfully.';
      await showDialog<void>(
        context: context,
        builder: (ctx) => AlertDialog(
          icon: const Icon(Icons.check_circle_outline, size: 48),
          title: const Text('Order placed'),
          content: Text(message),
          actions: [
            FilledButton(
              onPressed: () {
                Navigator.pop(ctx);
                if (orderId != null) {
                  Navigator.pushReplacementNamed(
                    context,
                    '/order-tracking',
                    arguments: orderId is int ? orderId : int.tryParse(orderId.toString()),
                  );
                } else {
                  Navigator.pushReplacementNamed(ctx, '/student');
                }
              },
              child: const Text('View dashboard'),
            ),
          ],
        ),
      );
    } on ApiException catch (e) {
      if (context.mounted) {
        ScaffoldMessenger.of(context)
            .showSnackBar(SnackBar(content: Text(e.message)));
      }
    } catch (_) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
              content: Text(
                  'Unable to place the order. Please check your connection and try again.')),
        );
      }
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }
}
