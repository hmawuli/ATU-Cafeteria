import 'package:flutter/material.dart';
import 'package:atu_cafeteria/data/local/defense_local_api.dart';

class DefenseModeScreen extends StatefulWidget {
  const DefenseModeScreen({super.key});
  @override
  State<DefenseModeScreen> createState() => _DefenseModeScreenState();
}

class _DefenseModeScreenState extends State<DefenseModeScreen> {
  final api = DefenseLocalApi.instance;
  final username = TextEditingController(text: 'student');
  final pin = TextEditingController(text: '1234');
  Map<String, dynamic>? user;
  bool loading = false;
  String? error;

  @override
  void dispose() { username.dispose(); pin.dispose(); super.dispose(); }

  Future<void> login() async {
    setState(() { loading = true; error = null; });
    try {
      final result = await api.login(username.text, pin.text);
      if (!mounted) return;
      setState(() => user = result);
      if (result == null) setState(() => error = 'Invalid username or PIN.');
    } catch (e) {
      if (mounted) setState(() => error = 'Offline database error: $e');
    } finally { if (mounted) setState(() => loading = false); }
  }

  @override
  Widget build(BuildContext context) {
    if (user != null) return DefenseDashboard(user: user!, onLogout: () => setState(() => user = null));
    return Scaffold(
      body: Container(
        decoration: const BoxDecoration(gradient: LinearGradient(colors: [Color(0xFF063B82), Color(0xFF0B5BB8), Color(0xFF052B63)])),
        child: SafeArea(child: Center(child: SingleChildScrollView(padding: const EdgeInsets.all(24), child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 460),
          child: Card(shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(26)), child: Padding(
            padding: const EdgeInsets.all(28), child: Column(children: [
              const Icon(Icons.restaurant_menu_rounded, size: 64, color: Color(0xFF063B82)),
              const SizedBox(height: 12),
              const Text('ATU CAFETERIA', style: TextStyle(fontSize: 28, fontWeight: FontWeight.w900, color: Color(0xFF063B82))),
              const SizedBox(height: 5),
              const Text('PHONE-ONLY DEFENSE MODE', style: TextStyle(fontWeight: FontWeight.w800, letterSpacing: 1.2)),
              const SizedBox(height: 12),
              const Chip(avatar: Icon(Icons.wifi_off, size: 17), label: Text('100% OFFLINE • NO SERVER REQUIRED')),
              const SizedBox(height: 24),
              TextField(controller: username, textInputAction: TextInputAction.next, decoration: const InputDecoration(labelText: 'Username', prefixIcon: Icon(Icons.person_outline))),
              const SizedBox(height: 14),
              TextField(controller: pin, obscureText: true, onSubmitted: (_) => login(), decoration: const InputDecoration(labelText: 'PIN', prefixIcon: Icon(Icons.lock_outline))),
              if (error != null) ...[const SizedBox(height: 10), Text('', style: TextStyle(color: Colors.red)), Text(error!, textAlign: TextAlign.center, style: const TextStyle(color: Colors.red, fontWeight: FontWeight.w700))],
              const SizedBox(height: 18),
              SizedBox(width: double.infinity, height: 52, child: FilledButton(onPressed: loading ? null : login, child: loading ? const CircularProgressIndicator() : const Text('ENTER DEFENSE SYSTEM'))),
              const SizedBox(height: 18),
              const Text('Student: student / 1234\nVendor: maryjoint / 1111\nAdmin: admin / admin123', textAlign: TextAlign.center, style: TextStyle(height: 1.6, fontSize: 12)),
            ]),
          )),
        )))),
      ),
    );
  }
}

class DefenseDashboard extends StatefulWidget {
  final Map<String, dynamic> user;
  final VoidCallback onLogout;
  const DefenseDashboard({super.key, required this.user, required this.onLogout});
  @override State<DefenseDashboard> createState() => _DefenseDashboardState();
}

class _DefenseDashboardState extends State<DefenseDashboard> {
  final api = DefenseLocalApi.instance;
  List<Map<String, dynamic>> foods = [], orders = [], users = [], reviews = [], logs = [];
  double balance = 0;
  bool busy = true;
  int tab = 0;
  int get uid => (widget.user['id'] as num).toInt();
  String get role => widget.user['role'].toString();

  @override void initState() { super.initState(); load(); }

  Future<void> load() async {
    setState(() => busy = true);
    try {
      final f = await api.foods(); final o = await api.ordersFor(uid, role); final b = await api.balance(uid);
      final u = role == 'ADMIN' ? await api.users() : <Map<String, dynamic>>[];
      final r = await api.reviews(); final l = await api.auditLogs();
      if (!mounted) return;
      setState(() { foods = f; orders = o; balance = b; users = u; reviews = r; logs = l; busy = false; });
    } catch (e) { if (mounted) { setState(() => busy = false); ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('$e'))); } }
  }

  Future<void> order(Map<String, dynamic> food) async {
    final controller = TextEditingController(text: '1');
    final qty = await showDialog<int>(context: context, builder: (_) => AlertDialog(title: Text(food['name'].toString()), content: TextField(controller: controller, keyboardType: TextInputType.number, decoration: const InputDecoration(labelText: 'Quantity')), actions: [TextButton(onPressed: () => Navigator.pop(context), child: const Text('Cancel')), FilledButton(onPressed: () => Navigator.pop(context, int.tryParse(controller.text) ?? 1), child: const Text('Checkout'))]));
    controller.dispose();
    if (qty == null) return;
    try { await api.placeOrder(customerId: uid, foodId: (food['id'] as num).toInt(), quantity: qty); await load(); if (mounted) ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Order placed. Wallet debited locally.'))); }
    catch (e) { if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('$e'))); }
  }

  Future<void> advance(Map<String, dynamic> o) async {
    final s = o['status'].toString(); final next = s == 'ORDER_PLACED' ? 'PREPARING' : s == 'PREPARING' ? 'READY_FOR_PICKUP' : s == 'READY_FOR_PICKUP' ? 'COMPLETED' : null;
    if (next == null) return;
    try { await api.updateOrderStatus((o['id'] as num).toInt(), next, uid); await load(); } catch (e) { if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('$e'))); }
  }

  Future<void> review(Map<String, dynamic> o) async {
    final c = TextEditingController(text: 'Excellent cafeteria service.'); int rating = 5;
    await showDialog(context: context, builder: (_) => StatefulBuilder(builder: (context, setLocal) => AlertDialog(title: const Text('Review completed order'), content: Column(mainAxisSize: MainAxisSize.min, children: [DropdownButton<int>(value: rating, isExpanded: true, items: List.generate(5, (i) => DropdownMenuItem(value: i + 1, child: Text('${i + 1} / 5 stars'))), onChanged: (v) { if (v != null) setLocal(() => rating = v); }), TextField(controller: c, maxLines: 3, decoration: const InputDecoration(labelText: 'Comment'))]), actions: [TextButton(onPressed: () => Navigator.pop(context), child: const Text('Cancel')), FilledButton(onPressed: () async { try { await api.addReview(orderId: (o['id'] as num).toInt(), customerId: uid, rating: rating, comment: c.text); if (context.mounted) Navigator.pop(context); await load(); } catch (e) { if (context.mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('$e'))); } }, child: const Text('Submit'))])));
    c.dispose();
  }

  Widget stat(String label, String value, IconData icon) => Card(child: ListTile(leading: Icon(icon), title: Text(label), trailing: Text(value, style: const TextStyle(fontWeight: FontWeight.w900, fontSize: 18))));
  Widget foodCard(Map<String, dynamic> f) { final p = (f['price'] as num).toDouble(); return Card(child: ListTile(leading: const CircleAvatar(child: Icon(Icons.restaurant)), title: Text(f['name'].toString(), style: const TextStyle(fontWeight: FontWeight.w800)), subtitle: Text(f['category'].toString()), trailing: role == 'STUDENT' ? TextButton(onPressed: () => order(f), child: Text('GH₵ ${p.toStringAsFixed(2)}\nORDER')) : Text('GH₵ ${p.toStringAsFixed(2)}'))); }
  Widget orderCard(Map<String, dynamic> o) { final total = (o['total_price'] as num).toDouble(); final status = o['status'].toString(); final actionable = role == 'VENDOR' && status != 'COMPLETED' && status != 'CANCELLED'; final reviewable = role == 'STUDENT' && status == 'COMPLETED'; return Card(child: ListTile(title: Text('#${o['id']} • ${o['food_name']}'), subtitle: Text('Qty ${o['quantity']} • ${status.replaceAll('_', ' ')}'), trailing: Column(mainAxisAlignment: MainAxisAlignment.center, children: [Text('GH₵ ${total.toStringAsFixed(2)}', style: const TextStyle(fontWeight: FontWeight.w900)), if (actionable) TextButton(onPressed: () => advance(o), child: const Text('ADVANCE')), if (reviewable) TextButton(onPressed: () => review(o), child: const Text('REVIEW'))]))); }

  Widget body() {
    if (busy) return const Center(child: CircularProgressIndicator());
    if (tab == 0) return RefreshIndicator(onRefresh: load, child: ListView(padding: const EdgeInsets.all(16), children: [Card(color: const Color(0xFF063B82), child: Padding(padding: const EdgeInsets.all(20), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text('Welcome, ${widget.user['full_name']}', style: const TextStyle(color: Colors.white, fontSize: 21, fontWeight: FontWeight.w900)), Text('$role • LOCAL DATABASE', style: const TextStyle(color: Colors.white70)), if (role == 'STUDENT') Text('GH₵ ${balance.toStringAsFixed(2)}', style: const TextStyle(color: Colors.white, fontSize: 30, fontWeight: FontWeight.w900))])), const SizedBox(height: 12), if (role == 'STUDENT') ...[const Text('Available Food', style: TextStyle(fontSize: 19, fontWeight: FontWeight.w900)), ...foods.map(foodCard)] else ...[Text(role == 'VENDOR' ? 'Incoming Orders' : 'System Overview', style: const TextStyle(fontSize: 19, fontWeight: FontWeight.w900)), ...orders.map(orderCard)], if (role == 'ADMIN') ...[stat('Users', '${users.length}', Icons.people), stat('Orders', '${orders.length}', Icons.receipt_long), stat('Reviews', '${reviews.length}', Icons.star), stat('Audit events', '${logs.length}', Icons.security)] ]));
    if (tab == 1) return ListView(padding: const EdgeInsets.all(16), children: [...orders.map(orderCard)]);
    if (tab == 2) return ListView(padding: const EdgeInsets.all(16), children: [const Text('Reviews', style: TextStyle(fontSize: 20, fontWeight: FontWeight.w900)), const SizedBox(height: 10), ...reviews.map((r) => Card(child: ListTile(title: Text('${r['rating']}/5 • ${r['customer_name'] ?? 'Student'}'), subtitle: Text(r['comment'].toString()))))]);
    return ListView(padding: const EdgeInsets.all(16), children: [const Text('Audit Trail', style: TextStyle(fontSize: 20, fontWeight: FontWeight.w900)), const SizedBox(height: 10), ...logs.map((l) => Card(child: ListTile(title: Text(l['action'].toString()), subtitle: Text(l['details'].toString()))))]);
  }

  @override Widget build(BuildContext context) => Scaffold(appBar: AppBar(title: const Text('ATU Cafeteria Defense'), actions: [IconButton(onPressed: widget.onLogout, icon: const Icon(Icons.logout))]), body: body(), bottomNavigationBar: NavigationBar(selectedIndex: tab, onDestinationSelected: (i) => setState(() => tab = i), destinations: const [NavigationDestination(icon: Icon(Icons.dashboard), label: 'Dashboard'), NavigationDestination(icon: Icon(Icons.receipt_long), label: 'Orders'), NavigationDestination(icon: Icon(Icons.star), label: 'Reviews'), NavigationDestination(icon: Icon(Icons.security), label: 'Audit')]), floatingActionButton: role == 'ADMIN' ? FloatingActionButton.extended(onPressed: () async { await api.resetDefenseData(); await load(); }, icon: const Icon(Icons.restart_alt), label: const Text('Reset Demo')) : null);
}
