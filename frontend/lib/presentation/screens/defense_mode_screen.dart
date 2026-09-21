import 'package:flutter/material.dart';
import 'package:atu_cafeteria/data/local/defense_local_api.dart';

class DefenseModeScreen extends StatefulWidget {
  const DefenseModeScreen({super.key});

  @override
  State<DefenseModeScreen> createState() => _DefenseModeScreenState();
}

class _DefenseModeScreenState extends State<DefenseModeScreen> {
  final _api = DefenseLocalApi.instance;
  final _username = TextEditingController(text: 'student');
  final _pin = TextEditingController(text: '1234');
  bool _loading = false;
  String? _error;
  Map<String, dynamic>? _user;

  @override
  void dispose() {
    _username.dispose();
    _pin.dispose();
    super.dispose();
  }

  Future<void> _login() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final user = await _api.login(_username.text, _pin.text);
      if (user == null) {
        setState(() => _error = 'Invalid defense account or PIN.');
      } else {
        setState(() => _user = user);
      }
    } catch (e) {
      setState(() => _error = 'Local database error: $e');
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  void _logout() => setState(() => _user = null);

  @override
  Widget build(BuildContext context) {
    if (_user != null) return _DefenseDashboard(user: _user!, onLogout: _logout);
    return Scaffold(
      body: Container(
        decoration: const BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: [Color(0xFF063B82), Color(0xFF0B5BB8), Color(0xFF052B63)],
          ),
        ),
        child: SafeArea(
          child: Center(
            child: SingleChildScrollView(
              padding: const EdgeInsets.all(22),
              child: ConstrainedBox(
                constraints: const BoxConstraints(maxWidth: 480),
                child: Card(
                  elevation: 12,
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(28)),
                  child: Padding(
                    padding: const EdgeInsets.fromLTRB(28, 32, 28, 28),
                    child: Column(
                      children: [
                        Container(
                          width: 78,
                          height: 78,
                          decoration: BoxDecoration(color: const Color(0xFFFFC400), borderRadius: BorderRadius.circular(22)),
                          child: const Icon(Icons.restaurant_menu_rounded, size: 43, color: Color(0xFF063B82)),
                        ),
                        const SizedBox(height: 18),
                        const Text('ATU CAFETERIA', style: TextStyle(fontSize: 28, fontWeight: FontWeight.w900, color: Color(0xFF063B82))),
                        const SizedBox(height: 5),
                        const Text('PHONE-ONLY DEFENSE MODE', style: TextStyle(fontSize: 12, letterSpacing: 1.4, fontWeight: FontWeight.w800)),
                        const SizedBox(height: 12),
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 13, vertical: 8),
                          decoration: BoxDecoration(color: const Color(0xFFE8F7EE), borderRadius: BorderRadius.circular(30)),
                          child: const Row(mainAxisSize: MainAxisSize.min, children: [
                            Icon(Icons.wifi_off_rounded, size: 16, color: Color(0xFF138A43)),
                            SizedBox(width: 7),
                            Text('100% OFFLINE • NO SERVER REQUIRED', style: TextStyle(fontSize: 11, fontWeight: FontWeight.w900, color: Color(0xFF138A43))),
                          ]),
                        ),
                        const SizedBox(height: 28),
                        TextField(controller: _username, decoration: const InputDecoration(labelText: 'Username', prefixIcon: Icon(Icons.person_outline))),
                        const SizedBox(height: 14),
                        TextField(controller: _pin, obscureText: true, decoration: const InputDecoration(labelText: 'PIN', prefixIcon: Icon(Icons.lock_outline))),
                        if (_error != null) ...[
                          const SizedBox(height: 12),
                          Text(_error!, style: const TextStyle(color: Colors.red, fontWeight: FontWeight.w700), textAlign: TextAlign.center),
                        ],
                        const SizedBox(height: 20),
                        SizedBox(width: double.infinity, height: 52, child: FilledButton(
                          onPressed: _loading ? null : _login,
                          child: _loading ? const SizedBox(width: 22, height: 22, child: CircularProgressIndicator(strokeWidth: 2)) : const Text('ENTER DEFENSE SYSTEM', style: TextStyle(fontWeight: FontWeight.w900)),
                        )),
                        const SizedBox(height: 20),
                        const Text('Demo accounts', style: TextStyle(fontWeight: FontWeight.w900)),
                        const SizedBox(height: 9),
                        const Text('Student: student / 1234\nVendor: maryjoint / 1111\nAdmin: admin / admin123', textAlign: TextAlign.center, style: TextStyle(fontSize: 12, height: 1.55)),
                      ],
                    ),
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _DefenseDashboard extends StatefulWidget {
  final Map<String, dynamic> user;
  final VoidCallback onLogout;
  const _DefenseDashboard({required this.user, required this.onLogout});

  @override
  State<_DefenseDashboard> createState() => _DefenseDashboardState();
}

class _DefenseDashboardState extends State<_DefenseDashboard> {
  final _api = DefenseLocalApi.instance;
  List<Map<String, dynamic>> _foods = [];
  List<Map<String, dynamic>> _orders = [];
  List<Map<String, dynamic>> _users = [];
  List<Map<String, dynamic>> _reviews = [];
  List<Map<String, dynamic>> _logs = [];
  double _balance = 0;
  bool _busy = false;
  int _tab = 0;

  int get _userId => (widget.user['id'] as num).toInt();
  String get _role => widget.user['role'].toString();

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() => _busy = true);
    try {
      _foods = await _api.foods();
      _orders = await _api.ordersFor(_userId, _role);
      _balance = await _api.balance(_userId);
      if (_role == 'ADMIN') _users = await _api.users();
      _reviews = await _api.reviews();
      _logs = await _api.auditLogs();
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Future<void> _placeOrder(Map<String, dynamic> food) async {
    final controller = TextEditingController(text: '1');
    final quantity = await showDialog<int>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(food['name'].toString()),
        content: TextField(controller: controller, keyboardType: TextInputType.number, decoration: const InputDecoration(labelText: 'Quantity')),
        actions: [TextButton(onPressed: () => Navigator.pop(context), child: const Text('Cancel')), FilledButton(onPressed: () => Navigator.pop(context, int.tryParse(controller.text) ?? 1), child: const Text('Checkout'))],
      ),
    );
    controller.dispose();
    if (quantity == null || quantity < 1) return;
    try {
      await _api.placeOrder(customerId: _userId, foodId: (food['id'] as num).toInt(), quantity: quantity);
      if (mounted) {
        await _load();
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Order placed successfully. Wallet updated locally.')));
      }
    } catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.toString().replaceFirst('Bad state: ', ''))));
    }
  }

  Future<void> _advanceOrder(Map<String, dynamic> order) async {
    final current = order['status'].toString();
    final next = current == 'ORDER_PLACED' ? 'PREPARING' : current == 'PREPARING' ? 'READY_FOR_PICKUP' : current == 'READY_FOR_PICKUP' ? 'COMPLETED' : null;
    if (next == null) return;
    try {
      await _api.updateOrderStatus((order['id'] as num).toInt(), next, _userId);
      await _load();
    } catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.toString().replaceFirst('Bad state: ', ''))));
    }
  }

  Future<void> _review(Map<String, dynamic> order) async {
    final rating = ValueNotifier(5);
    final comment = TextEditingController(text: 'Excellent cafeteria service.');
    await showDialog<void>(context: context, builder: (context) => AlertDialog(
      title: const Text('Review completed order'),
      content: Column(mainAxisSize: MainAxisSize.min, children: [
        ValueListenableBuilder<int>(valueListenable: rating, builder: (_, value, __) => DropdownButton<int>(value: value, isExpanded: true, items: List.generate(5, (i) => DropdownMenuItem(value: i + 1, child: Text('${i + 1} / 5 stars'))), onChanged: (v) { if (v != null) rating.value = v; })),
        TextField(controller: comment, maxLines: 3, decoration: const InputDecoration(labelText: 'Comment')),
      ]),
      actions: [TextButton(onPressed: () => Navigator.pop(context), child: const Text('Cancel')), FilledButton(onPressed: () async { try { await _api.addReview(orderId: (order['id'] as num).toInt(), customerId: _userId, rating: rating.value, comment: comment.text); if (context.mounted) Navigator.pop(context); await _load(); } catch (e) { if (context.mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.toString()))); } }, child: const Text('Submit'))],
    ));
    rating.dispose();
    comment.dispose();
  }

  Widget _home() {
    final student = _role == 'STUDENT';
    final vendor = _role == 'VENDOR';
    return RefreshIndicator(onRefresh: _load, child: ListView(padding: const EdgeInsets.all(16), children: [
      Container(padding: const EdgeInsets.all(18), decoration: BoxDecoration(borderRadius: BorderRadius.circular(22), gradient: const LinearGradient(colors: [Color(0xFF063B82), Color(0xFF0B5BB8)])), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Text('Welcome, ${widget.user['full_name']}', style: const TextStyle(color: Colors.white, fontSize: 21, fontWeight: FontWeight.w900)),
        const SizedBox(height: 5), Text('${_role} • LOCAL PHONE DATABASE', style: const TextStyle(color: Colors.white70, fontSize: 11, fontWeight: FontWeight.w800)),
        if (student) ...[const SizedBox(height: 15), Text('GH₵ ${_balance.toStringAsFixed(2)}', style: const TextStyle(color: Colors.white, fontSize: 30, fontWeight: FontWeight.w900)), const Text('Wallet balance', style: TextStyle(color: Colors.white70))],
      ])),
      const SizedBox(height: 18),
      if (student) ...[_sectionTitle('Available food'), ..._foods.map((food) => _foodCard(food))],
      if (!student) ...[_sectionTitle(vendor ? 'Incoming orders' : 'System overview'), ..._orders.map((order) => _orderCard(order))],
      if (_role == 'ADMIN') ...[
        _sectionTitle('System statistics'),
        _statCard(Icons.people_outline, 'Users', '${_users.length}'),
        _statCard(Icons.receipt_long_outlined, 'Orders', '${_orders.length}'),
        _statCard(Icons.rate_review_outlined, 'Reviews', '${_reviews.length}'),
      ],
    ]));
  }

  Widget _foodCard(Map<String, dynamic> food) => Card(margin: const EdgeInsets.only(bottom: 10), child: ListTile(leading: CircleAvatar(child: const Icon(Icons.restaurant)), title: Text(food['name'].toString(), style: const TextStyle(fontWeight: FontWeight.w800)), subtitle: Text('${food['category']} • ${food['description']}'), trailing: Column(mainAxisAlignment: MainAxisAlignment.center, children: [Text('GH₵ ${(food['price'] as num).toDouble().toStringAsFixed(2)}', style: const TextStyle(fontWeight: FontWeight.w900)), if (_role == 'STUDENT') TextButton(onPressed: () => _placeOrder(food), child: const Text('ORDER'))])));

  Widget _orderCard(Map<String, dynamic> order) => Card(margin: const EdgeInsets.only(bottom: 10), child: ListTile(title: Text('#${order['id']} • ${order['food_name']}', style: const TextStyle(fontWeight: FontWeight.w800)), subtitle: Text('Qty ${order['quantity']} • GH₵ ${(order['total_price'] as num).toDouble().toStringAsFixed(2)}\nStatus: ${order['status']}\nPickup PIN: ${order['pickup_pin']}'), trailing: _role == 'VENDOR' && order['status'] != 'COMPLETED' ? IconButton(onPressed: () => _advanceOrder(order), icon: const Icon(Icons.arrow_forward_rounded)) : _role == 'STUDENT' && order['status'] == 'COMPLETED' ? IconButton(onPressed: () => _review(order), icon: const Icon(Icons.star_outline)) : null));

  Widget _sectionTitle(String text) => Padding(padding: const EdgeInsets.only(bottom: 10, top: 4), child: Text(text, style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w900)));
  Widget _statCard(IconData icon, String label, String value) => Card(child: ListTile(leading: Icon(icon), title: Text(label), trailing: Text(value, style: const TextStyle(fontSize: 20, fontWeight: FontWeight.w900))));

  Widget _secondary() {
    final data = _role == 'ADMIN' ? _logs : _reviews;
    return RefreshIndicator(onRefresh: _load, child: ListView(padding: const EdgeInsets.all(16), children: [
      _sectionTitle(_role == 'ADMIN' ? 'Audit activity' : 'Reviews'),
      if (data.isEmpty) const Padding(padding: EdgeInsets.all(30), child: Center(child: Text('No records yet.'))),
      ...data.map((row) => Card(child: ListTile(leading: Icon(_role == 'ADMIN' ? Icons.security : Icons.star), title: Text((_role == 'ADMIN' ? row['action'] : '${row['rating']} / 5')?.toString() ?? ''), subtitle: Text((_role == 'ADMIN' ? row['details'] : '${row['comment']}')?.toString() ?? '')))),
      if (_role == 'ADMIN') ...[_sectionTitle('Users'), ..._users.map((u) => Card(child: ListTile(title: Text(u['full_name'].toString()), subtitle: Text('${u['username']} • ${u['role']}'))))],
    ]));
  }

  @override
  Widget build(BuildContext context) {
    final title = _role == 'STUDENT' ? 'Student Portal' : _role == 'VENDOR' ? 'Vendor Portal' : 'Admin Portal';
    return Scaffold(
      appBar: AppBar(title: Text(title), actions: [if (_busy) const Padding(padding: EdgeInsets.all(14), child: SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2))), IconButton(onPressed: widget.onLogout, tooltip: 'Logout', icon: const Icon(Icons.logout))]),
      body: _tab == 0 ? _home() : _secondary(),
      bottomNavigationBar: NavigationBar(selectedIndex: _tab, onDestinationSelected: (i) => setState(() => _tab = i), destinations: [const NavigationDestination(icon: Icon(Icons.dashboard_outlined), selectedIcon: Icon(Icons.dashboard), label: 'Dashboard'), NavigationDestination(icon: Icon(_role == 'ADMIN' ? Icons.security_outlined : Icons.history), selectedIcon: Icon(_role == 'ADMIN' ? Icons.security : Icons.history), label: _role == 'ADMIN' ? 'Audit' : 'Activity')]),
    );
  }
}
