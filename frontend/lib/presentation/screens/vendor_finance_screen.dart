import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../core/network/api_client.dart';
import '../../core/theme/app_theme.dart';
import '../widgets/reference_design.dart';

class VendorFinanceScreen extends StatefulWidget {
  const VendorFinanceScreen({super.key});

  @override
  State<VendorFinanceScreen> createState() => _VendorFinanceScreenState();
}

class _VendorFinanceScreenState extends State<VendorFinanceScreen> {
  int _days = 30;
  bool _loading = false;
  String? _error;
  Map<String, dynamic>? _data;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final response = await context.read<ApiClient>().get('/vendor/finance?days=${_days}');
      if (response is! Map) {
        throw const ApiException(500, 'Invalid finance response.');
      }
      if (!mounted) return;
      setState(() {
        _data = Map<String, dynamic>.from(response);
        _loading = false;
      });
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() {
        _error = e.message;
        _loading = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _error = 'Unable to load finance data. Please try again.';
        _loading = false;
      });
    }
  }

  Map<String, dynamic> get _summary =>
      _data?['summary'] is Map ? Map<String, dynamic>.from(_data!['summary']) : const {};

  List<Map<String, dynamic>> get _transactions =>
      _asMapList(_data?['transactions']);

  List<Map<String, dynamic>> get _settlements =>
      _asMapList(_data?['settlements']);

  List<Map<String, dynamic>> get _trend =>
      _asMapList(_data?['trend']);

  List<Map<String, dynamic>> _asMapList(dynamic value) {
    if (value is! List) return const [];
    return value.whereType<Map>().map(Map<String, dynamic>.from).toList();
  }

  double _num(dynamic value) =>
      value is num ? value.toDouble() : double.tryParse('${value ?? 0}') ?? 0;

  String _ghs(dynamic value) => 'GH₵ ${_num(value).toStringAsFixed(2)}';

  @override
  Widget build(BuildContext context) {
    return RefreshIndicator(
      onRefresh: _load,
      child: ListView(
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.all(24),
        children: [
          _header(),
          const SizedBox(height: 16),
          if (_loading && _data != null) const LinearProgressIndicator(),
          if (_error != null) _errorCard(),
          if (_error == null && _data != null) ...[
            _hero(),
            const SizedBox(height: 14),
            _metrics(),
            const SizedBox(height: 14),
            _breakdown(),
            const SizedBox(height: 14),
            _trendCard(),
            const SizedBox(height: 14),
            _transactionsCard(),
            const SizedBox(height: 14),
            _settlementsCard(),
          ],
          if (_data == null && _error == null && !_loading)
            const Padding(
              padding: EdgeInsets.symmetric(vertical: 40),
              child: Center(child: Text('Finance data is not available yet.')),
            ),
          const SizedBox(height: 20),
          const Text(
            'Figures are based on completed/delivered orders and successful recorded refunds. Settlement figures are read from platform settlement records; no figures are fabricated when data is missing.',
            style: TextStyle(color: AppTheme.textMuted, fontSize: 11, height: 1.45),
          ),
        ],
      ),
    );
  }

  Widget _header() => Row(
        children: [
          const Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('Finance', style: TextStyle(fontSize: 24, fontWeight: FontWeight.w900, color: AppTheme.textDark)),
                SizedBox(height: 4),
                Text('Sales, refunds, transactions and settlements.', style: TextStyle(color: AppTheme.textMuted)),
              ],
            ),
          ),
          Wrap(
            spacing: 6,
            children: [for (final value in [7, 30, 90]) ChoiceChip(
              label: Text('${value}D'),
              selected: _days == value,
              onSelected: (_) {
                if (_days == value) return;
                setState(() => _days = value);
                _load();
              },
            )],
          ),
          const SizedBox(width: 8),
          IconButton(
            tooltip: 'Refresh',
            onPressed: _loading ? null : _load,
            icon: const Icon(Icons.refresh),
          ),
        ],
      );

  Widget _hero() {
    final vendor = _data?['vendor'] is Map
        ? Map<String, dynamic>.from(_data!['vendor'])
        : const <String, dynamic>{};

    return Container(
      padding: const EdgeInsets.all(22),
      decoration: BoxDecoration(
        gradient: const LinearGradient(colors: [AppTheme.primaryDark, AppTheme.primary]),
        borderRadius: BorderRadius.circular(20),
      ),
      child: Row(
        children: [
          Container(
            width: 54,
            height: 54,
            decoration: BoxDecoration(color: AppTheme.accent, borderRadius: BorderRadius.circular(15)),
            child: const Icon(Icons.account_balance_wallet_rounded, color: AppTheme.primaryDark, size: 29),
          ),
          const SizedBox(width: 15),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(vendor['name']?.toString() ?? 'Vendor', style: const TextStyle(color: Colors.white70, fontWeight: FontWeight.w700)),
                const SizedBox(height: 3),
                Text(_ghs(_summary['net_food_sales']), style: const TextStyle(color: Colors.white, fontSize: 27, fontWeight: FontWeight.w900)),
                const SizedBox(height: 2),
                Text('Net food sales • Last ${_days} days', style: const TextStyle(color: Colors.white70, fontSize: 12)),
              ],
            ),
          ),
          StatusPill(vendor['is_open'] == true ? 'Store Open' : 'Store Closed'),
        ],
      ),
    );
  }

  Widget _metrics() => Wrap(
        spacing: 12,
        runSpacing: 12,
        children: [
          MetricTile(label: 'Gross Sales', value: _ghs(_summary['gross_food_sales']), icon: Icons.trending_up),
          MetricTile(label: 'Discounts', value: _ghs(_summary['discounts']), icon: Icons.local_offer_outlined),
          MetricTile(label: 'Refunds', value: _ghs(_summary['refunds']), icon: Icons.reply_rounded),
          MetricTile(label: 'Completed Orders', value: '${_summary['completed_orders'] ?? 0}', icon: Icons.check_circle_outline),
          MetricTile(label: 'Average Order', value: _ghs(_summary['average_order_value']), icon: Icons.shopping_basket_outlined),
          MetricTile(label: 'Settled', value: _ghs(_summary['settled_amount']), icon: Icons.account_balance_outlined),
        ],
      );

  Widget _breakdown() => ReferenceCard(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text('Sales Breakdown', style: TextStyle(fontSize: 18, fontWeight: FontWeight.w900, color: AppTheme.textDark)),
            const SizedBox(height: 12),
            _line('Customer collected', _summary['customer_collected']),
            _line('Net food sales', _summary['net_food_sales'], strong: true),
            _line('Successful refunds', _summary['refunds']),
            _line('Service fees', _summary['service_fees']),
            _line('Delivery fees', _summary['delivery_fees']),
            _line('Taxes recorded', _summary['taxes']),
            const Divider(height: 22),
            _line('Pending / processing settlements', _summary['pending_settlement'], strong: true),
          ],
        ),
      );

  Widget _line(String label, dynamic value, {bool strong = false}) => Padding(
        padding: const EdgeInsets.symmetric(vertical: 6),
        child: Row(
          children: [
            Expanded(
              child: Text(label, style: TextStyle(
                color: strong ? AppTheme.textDark : AppTheme.textMuted,
                fontWeight: strong ? FontWeight.w900 : FontWeight.w500,
              )),
            ),
            Text(_ghs(value), style: TextStyle(
              color: strong ? AppTheme.primary : AppTheme.textDark,
              fontWeight: FontWeight.w900,
            )),
          ],
        ),
      );

  Widget _trendCard() {
    final points = _trend;
    final max = points.fold<double>(0, (value, point) {
      final sales = _num(point['sales']);
      return sales > value ? sales : value;
    });

    return ReferenceCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text('Sales Trend', style: TextStyle(fontSize: 18, fontWeight: FontWeight.w900, color: AppTheme.textDark)),
          const SizedBox(height: 4),
          Text('Net food sales across the selected ${_days}-day period.', style: const TextStyle(color: AppTheme.textMuted, fontSize: 11)),
          const SizedBox(height: 16),
          if (max <= 0)
            const Padding(
              padding: EdgeInsets.symmetric(vertical: 25),
              child: Row(children: [
                Icon(Icons.show_chart, color: AppTheme.primary),
                SizedBox(width: 10),
                Expanded(child: Text('No completed sales recorded for this period yet.')),
              ]),
            )
          else
            SizedBox(
              height: 165,
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.end,
                children: points.map((point) {
                  final ratio = (_num(point['sales']) / max).clamp(0.0, 1.0);
                  return Expanded(
                    child: Padding(
                      padding: const EdgeInsets.symmetric(horizontal: 1.5),
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.end,
                        children: [
                          Expanded(
                            child: Align(
                              alignment: Alignment.bottomCenter,
                              child: FractionallySizedBox(
                                heightFactor: ratio == 0 ? .02 : ratio,
                                child: DecoratedBox(
                                  decoration: BoxDecoration(
                                    color: AppTheme.primary.withValues(alpha: .85),
                                    borderRadius: const BorderRadius.vertical(top: Radius.circular(4)),
                                  ),
                                ),
                              ),
                            ),
                          ),
                          const SizedBox(height: 4),
                          Text(
                            (point['date']?.toString() ?? '').length >= 10
                                ? point['date'].toString().substring(5)
                                : '—',
                            style: const TextStyle(fontSize: 8, color: AppTheme.textMuted),
                          ),
                        ],
                      ),
                    ),
                  );
                }).toList(),
              ),
            ),
        ],
      ),
    );
  }

  Widget _transactionsCard() => ReferenceCard(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text('Recent Sales Transactions', style: TextStyle(fontSize: 18, fontWeight: FontWeight.w900, color: AppTheme.textDark)),
            const SizedBox(height: 4),
            const Text('Completed and delivered orders only.', style: TextStyle(color: AppTheme.textMuted, fontSize: 11)),
            const SizedBox(height: 12),
            if (_transactions.isEmpty)
              const Text('No completed sales transactions in this period.')
            else
              ..._transactions.map((tx) => Padding(
                    padding: const EdgeInsets.symmetric(vertical: 8),
                    child: Row(
                      children: [
                        const Icon(Icons.receipt_long, color: AppTheme.primary),
                        const SizedBox(width: 10),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(tx['order_number']?.toString() ?? 'ATU-—', style: const TextStyle(fontWeight: FontWeight.w900)),
                              Text('${tx['item'] ?? 'Food item'} × ${tx['quantity'] ?? 0} • ${tx['status'] ?? 'COMPLETED'}', maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 11, color: AppTheme.textMuted)),
                            ],
                          ),
                        ),
                        const SizedBox(width: 8),
                        Text(_ghs(tx['net_sales']), style: const TextStyle(fontWeight: FontWeight.w900)),
                      ],
                    ),
                  )),
          ],
        ),
      );

  Widget _settlementsCard() => ReferenceCard(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text('Settlement History', style: TextStyle(fontSize: 18, fontWeight: FontWeight.w900, color: AppTheme.textDark)),
            const SizedBox(height: 4),
            const Text('Records generated by the platform.', style: TextStyle(color: AppTheme.textMuted, fontSize: 11)),
            const SizedBox(height: 12),
            if (_settlements.isEmpty)
              const Text('No settlement records have been generated yet.')
            else
              ..._settlements.map((s) {
                final status = s['status']?.toString() ?? 'PENDING';
                final paid = const {'PAID', 'SETTLED', 'COMPLETED'}.contains(status);
                return Padding(
                  padding: const EdgeInsets.only(bottom: 8),
                  child: ReferenceCard(
                    padding: const EdgeInsets.all(13),
                    child: Row(
                      children: [
                        Icon(paid ? Icons.verified_rounded : Icons.schedule_rounded, color: paid ? Colors.green : AppTheme.accentDark),
                        const SizedBox(width: 10),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text('${s['period_start'] ?? '—'} to ${s['period_end'] ?? '—'}', style: const TextStyle(fontWeight: FontWeight.w800)),
                              Text(status, style: const TextStyle(fontSize: 10, color: AppTheme.textMuted)),
                            ],
                          ),
                        ),
                        Text(_ghs(s['net_amount']), style: const TextStyle(fontWeight: FontWeight.w900, color: AppTheme.primary)),
                      ],
                    ),
                  ),
                );
              }),
          ],
        ),
      );

  Widget _errorCard() => ReferenceCard(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Icon(Icons.error_outline_rounded, color: Colors.redAccent),
            const SizedBox(height: 8),
            Text(_error ?? 'Unable to load finance data.', style: const TextStyle(fontWeight: FontWeight.w700)),
            const SizedBox(height: 10),
            FilledButton.icon(onPressed: _load, icon: const Icon(Icons.refresh), label: const Text('Try Again')),
          ],
        ),
      );
}
