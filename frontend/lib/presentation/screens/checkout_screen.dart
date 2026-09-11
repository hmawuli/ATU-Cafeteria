import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/cart_provider.dart';
import '../../core/network/api_client.dart';

class CheckoutScreen extends StatefulWidget {
  const CheckoutScreen({super.key});
  @override State<CheckoutScreen> createState() => _CheckoutScreenState();
}
class _CheckoutScreenState extends State<CheckoutScreen> {
  String _method = 'Wallet';
  bool _submitting = false;
  final ApiClient _api = ApiClient();
  @override void dispose() { _api.close(); super.dispose(); }
  @override Widget build(BuildContext context) {
    final cart = context.watch<CartProvider>();
    final total = cart.subtotal;
    return Scaffold(
      appBar: AppBar(title: const Text('Checkout')),
      body: cart.isEmpty ? const Center(child: Text('Your cart is empty.')) : ListView(
        padding: const EdgeInsets.all(20),
        children: [
          Text('Review your order', style: Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w900)),
          const SizedBox(height: 6), const Text('Confirm your meals and payment method before placing the order.'),
          const SizedBox(height: 18),
          Card(child: Column(children: cart.lines.map((line) => ListTile(
            title: Text(line.item.name, style: const TextStyle(fontWeight: FontWeight.w700)),
            subtitle: Text('Quantity: ${line.quantity}'),
            trailing: Text('GH₵ ${line.total.toStringAsFixed(2)}', style: const TextStyle(fontWeight: FontWeight.w800)),
          )).toList())),
          const SizedBox(height: 18),
          Card(child: Padding(padding: const EdgeInsets.all(16), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            const Text('Payment method', style: TextStyle(fontWeight: FontWeight.w800)),
            RadioListTile<String>(value:'Wallet',groupValue:_method,onChanged:(v)=>setState(()=>_method=v!),title:const Text('ATU Cafeteria Wallet'),secondary:const Icon(Icons.account_balance_wallet_outlined)),
            RadioListTile<String>(value:'Paystack',groupValue:_method,onChanged:(v)=>setState(()=>_method=v!),title:const Text('Paystack'),secondary:const Icon(Icons.credit_card_rounded)),
          ]))),
          const SizedBox(height: 18),
          Card(child: Padding(padding: const EdgeInsets.all(18), child: Column(children: [
            Row(children:[const Text('Subtotal'),const Spacer(),Text('GH₵ ${total.toStringAsFixed(2)}')]),
            const Divider(height:28),
            Row(children:[const Text('Total',style:TextStyle(fontSize:18,fontWeight:FontWeight.w900)),const Spacer(),Text('GH₵ ${total.toStringAsFixed(2)}',style:TextStyle(fontSize:20,fontWeight:FontWeight.w900,color:Theme.of(context).colorScheme.primary))]),
            const SizedBox(height:16),
            SizedBox(width:double.infinity,height:52,child:FilledButton.icon(
              onPressed:_submitting?null:()=>_showConfirmation(context),
              icon:const Icon(Icons.check_circle_outline),label:Text(_submitting?'Processing...':'Place Order Securely'),
            )),
          ]))),
        ],
      ),
    );
  }
  Future<void> _showConfirmation(BuildContext context) async {
    setState(()=>_submitting=true);
    await Future<void>.delayed(const Duration(milliseconds:350));
    if(!mounted)return;
    setState(()=>_submitting=false);
    final ok=await showDialog<bool>(context:context,builder:(ctx)=>AlertDialog(
      title:const Text('Confirm order'),content:const Text('Your order details are ready. Continue to place this order?'),
      actions:[TextButton(onPressed:()=>Navigator.pop(ctx,false),child:const Text('Review')),FilledButton(onPressed:()=>Navigator.pop(ctx,true),child:const Text('Confirm'))],
    ));
    if(ok==true && mounted) {
      setState(()=>_submitting=true);
      try {
        final result = await _api.post('/student/cart-checkout', body: {'items': cart.toCheckoutPayload()});
        if (!mounted) return;
        context.read<CartProvider>().clear();
        await showDialog<void>(context: context, builder: (ctx) => AlertDialog(
          title: const Text('Order placed successfully'),
          content: Text(result is Map && result['message'] != null ? result['message'].toString() : 'Your order has been sent to the cafeteria.'),
          actions: [FilledButton(onPressed: () => Navigator.pop(ctx), child: const Text('Done'))],
        ));
        if (mounted) Navigator.pop(context);
      } on ApiException catch (e) {
        if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.message)));
      } catch (_) {
        if (mounted) ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Unable to place the order. Please try again.')));
      } finally {
        if (mounted) setState(()=>_submitting=false);
      }
    }
  }
}