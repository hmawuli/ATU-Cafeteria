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
  String? _paymentReference;
  final TextEditingController _noteController = TextEditingController();
  bool _submitting = false;
  final ApiClient _api = ApiClient();
  @override
  void dispose() {
    _noteController.dispose();
    _api.close();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final cart = context.watch<CartProvider>();
    final total = cart.subtotal;
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
                                onChanged: (value) =>
                                    setState(() => _method = value!),
                                child: const RadioListTile<String>(
                                  value: 'Wallet',
                                  title: Text('ATU Cafeteria Wallet'),
                                  secondary: Icon(
                                      Icons.account_balance_wallet_outlined),
                                ),
                              ),
                              RadioListTile<String>(
                                value: 'Online',
                                title: const Text('Mobile Money / Card'),
                                subtitle: const Text(
                                    'Pay securely through Paystack.'),
                                secondary: const Icon(Icons.payments_outlined),
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
                          const Divider(height: 28),
                          Row(children: [
                            const Text('Total',
                                style: TextStyle(
                                    fontSize: 18, fontWeight: FontWeight.w900)),
                            const Spacer(),
                            Text('GH₵ ${total.toStringAsFixed(2)}',
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

  Future<void> _payOnline(BuildContext context, CafeteriaProvider auth, CartProvider cart) async {
    final user = auth.currentUser;
    final email = user?.email;
    if (email == null || email.trim().isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('A valid email address is required for online payment.')),
      );
      return;
    }

    setState(() => _submitting = true);
    try {
      final init = await auth.initializePaystackPayment(
        amount: cart.subtotal,
        email: email.trim(),
        purpose: 'DIRECT_ORDER_PAY',
      );
      if (init == null) throw Exception('Payment could not be initialized.');
      final reference = init['reference']?.toString();
      final authorizationUrl = init['authorization_url']?.toString();
      if (reference == null || authorizationUrl == null) {
        throw Exception('Payment gateway returned an incomplete response.');
      }
      _paymentReference = reference;

      final simulated = init['is_simulated'] == true;
      if (!simulated) {
        final launched = await launchUrl(
          Uri.parse(authorizationUrl),
          mode: LaunchMode.externalApplication,
        );
        if (!launched) throw Exception('Unable to open the payment page.');

        if (!mounted) return;
        final verified = await showDialog<bool>(
          context: context,
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
          context: context,
          builder: (ctx) => AlertDialog(
            title: const Text('Payment simulation'),
            content: Text(
              'The backend is using its local Paystack simulation. Continue as a simulated successful payment of GH₵ ${cart.subtotal.toStringAsFixed(2)}?',
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
        amount: cart.subtotal,
        purpose: 'DIRECT_ORDER_PAY',
      );
      if (!confirmed) throw Exception('Payment has not been confirmed by Paystack.');

      final result = await _api.post('/student/cart-checkout', body: {
        'items': cart.toCheckoutPayload(),
        'payment_method': 'momo',
        'payment_reference': reference,
        if (_noteController.text.trim().isNotEmpty) 'note': _noteController.text.trim(),
      });
      if (!mounted) return;
      cart.clear();
      final pickupPin = result is Map ? result['pickup_pin']?.toString() : null;
      await showDialog<void>(
        context: context,
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
                Navigator.pushReplacementNamed(context, '/student');
              },
              child: const Text('View order'),
            ),
          ],
        ),
      );
    } catch (e) {
      if (mounted) {
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
      if (goToLogin == true && mounted) {
        Navigator.pushNamed(context, '/login');
      }
      return;
    }

    final cart = context.read<CartProvider>();
    if (cart.isEmpty) return;
    if (_method == 'Online') {
      await _payOnline(context, auth, cart);
      return;
    }

    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Confirm order'),
        content: Text(
            'Place this order for GH₵ ${cart.subtotal.toStringAsFixed(2)} using your cafeteria wallet?'),
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
        if (_noteController.text.trim().isNotEmpty)
          'note': _noteController.text.trim(),
        'payment_method': _method.toLowerCase(),
      });
      if (!context.mounted) return;
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
                Navigator.pushReplacementNamed(ctx, '/student');
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
