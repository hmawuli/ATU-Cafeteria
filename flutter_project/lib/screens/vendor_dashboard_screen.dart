import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/auth_provider.dart';
import '../providers/cafeteria_provider.dart';
import 'login_screen.dart';
import 'vendor_staff_screen.dart';

class VendorDashboardScreen extends StatelessWidget {
  const VendorDashboardScreen({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final auth = Provider.of<AuthProvider>(context);
    final cafeteria = Provider.of<CafeteriaProvider>(context);
    return Scaffold(
      appBar: AppBar(
        title: const Text('Vendor Kitchen Hub'), backgroundColor: Colors.indigo, foregroundColor: Colors.white,
        actions: [
          IconButton(tooltip:'Staff & shifts', icon:const Icon(Icons.groups), onPressed:()=>Navigator.push(context,MaterialPageRoute(builder:(_)=>const VendorStaffScreen()))),
          IconButton(icon: const Icon(Icons.logout), onPressed: () { auth.logout(); Navigator.pushReplacement(context, MaterialPageRoute(builder: (_) => const LoginScreen())); }),
        ],
      ),
      body: cafeteria.orders.isEmpty ? Center(child: Column(mainAxisAlignment: MainAxisAlignment.center, children: [Icon(Icons.soup_kitchen, size: 64, color: Colors.grey.shade400), const SizedBox(height: 12), const Text('No active campus orders right now', style: TextStyle(fontSize: 16, color: Colors.grey)), const SizedBox(height:16), OutlinedButton.icon(onPressed:()=>Navigator.push(context,MaterialPageRoute(builder:(_)=>const VendorStaffScreen())),icon:const Icon(Icons.groups),label:const Text('Manage Staff & Shifts'))])) : ListView.builder(
        padding: const EdgeInsets.all(12), itemCount: cafeteria.orders.length,
        itemBuilder: (context, index) { final order = cafeteria.orders[index]; return Card(margin: const EdgeInsets.only(bottom: 12), shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)), elevation: 3, child: Padding(padding: const EdgeInsets.all(16), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Row(mainAxisAlignment: MainAxisAlignment.spaceBetween, children: [Text('Order #${order.id}', style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16)), Container(padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4), decoration: BoxDecoration(color: _getStatusColor(order.status), borderRadius: BorderRadius.circular(12)), child: Text(order.displayStatus, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 12)))]),
          const SizedBox(height: 8), Text('${order.quantity}x ${order.foodName}', style: const TextStyle(fontSize: 15)), Text('Total: GH₵ ${order.totalPrice.toStringAsFixed(2)} • PIN: ${order.pickupPin}', style: TextStyle(color: Colors.grey.shade700, fontSize: 13)), const SizedBox(height: 12),
          Wrap(spacing:8, children: [if(order.status.toUpperCase()=='PENDING'||order.status.toUpperCase()=='RECEIVED') ElevatedButton(onPressed:()=>cafeteria.updateOrderStatus(order.id,'PREPARING'),child:const Text('Start Preparing')), if(order.status.toUpperCase()=='PREPARING') ElevatedButton(onPressed:()=>cafeteria.updateOrderStatus(order.id,'OUT_FOR_DELIVERY'),child:const Text('Ready / Deliver')), if(order.status.toUpperCase()=='OUT_FOR_DELIVERY'||order.status.toUpperCase()=='READY') ElevatedButton(onPressed:()=>cafeteria.updateOrderStatus(order.id,'DELIVERED'),child:const Text('Complete Pickup'))])
        ]))); },
      ),
    );
  }
  Color _getStatusColor(String status) { switch(status.toUpperCase()){case 'PENDING':case 'RECEIVED':return Colors.orange;case 'PREPARING':return Colors.blue;case 'READY':case 'OUT_FOR_DELIVERY':return Colors.green;case 'COMPLETED':case 'DELIVERED':return Colors.purple;default:return Colors.grey;} }
}
