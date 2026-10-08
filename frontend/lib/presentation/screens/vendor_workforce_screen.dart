import 'package:flutter/material.dart';
import 'package:atu_cafeteria/core/network/api_client.dart';
import 'package:atu_cafeteria/core/theme/app_theme.dart';

/// Vendor workforce: a supervisor employs workers, assigns weekly shifts and
/// keeps a worker-finance ledger.
class VendorWorkforceScreen extends StatefulWidget {
  const VendorWorkforceScreen({super.key});

  @override
  State<VendorWorkforceScreen> createState() => _VendorWorkforceScreenState();
}

class _VendorWorkforceScreenState extends State<VendorWorkforceScreen> {
  static const _days = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];

  final _api = ApiClient();
  List<Map<String, dynamic>> _workers = [];
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _api.close();
    super.dispose();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final data = await _api.get('vendor/workers');
      final raw = data is Map ? data['workers'] : null;
      setState(() {
        _workers = raw is List
            ? raw
                .whereType<Map>()
                .map(Map<String, dynamic>.from)
                .toList()
            : [];
        _loading = false;
      });
    } on ApiException catch (e) {
      setState(() {
        _error = e.message;
        _loading = false;
      });
    } catch (_) {
      setState(() {
        _error = 'Could not load the workforce.';
        _loading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Workforce'),
        actions: [
          TextButton.icon(
            onPressed: _addOrEdit,
            icon: const Icon(Icons.person_add_alt_1),
            label: const Text('Employ'),
          ),
          const SizedBox(width: 8),
        ],
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
              ? Center(
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      const Icon(Icons.cloud_off_outlined, size: 44),
                      const SizedBox(height: 12),
                      Text(_error!, textAlign: TextAlign.center),
                      const SizedBox(height: 16),
                      FilledButton.icon(
                        onPressed: _load,
                        icon: const Icon(Icons.refresh),
                        label: const Text('Retry'),
                      ),
                    ],
                  ),
                )
              : _workers.isEmpty
                  ? Center(
                      child: Column(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          const Icon(Icons.groups_2_outlined, size: 56),
                          const SizedBox(height: 12),
                          const Text('No workers yet.',
                              style: TextStyle(
                                  fontSize: 18,
                                  fontWeight: FontWeight.w800)),
                          const SizedBox(height: 6),
                          const Text(
                              'Tap “Employ” to add your first worker.'),
                          const SizedBox(height: 16),
                          FilledButton.icon(
                            onPressed: _addOrEdit,
                            icon: const Icon(Icons.person_add_alt_1),
                            label: const Text('Employ worker'),
                          ),
                        ],
                      ),
                    )
                  : RefreshIndicator(
                      onRefresh: _load,
                      child: ListView.builder(
                        padding: const EdgeInsets.all(14),
                        itemCount: _workers.length,
                        itemBuilder: (context, i) =>
                            _workerCard(_workers[i]),
                      ),
                    ),
    );
  }

  Widget _workerCard(Map<String, dynamic> worker) {
    final active = worker['is_active'] != false;
    final wage = worker['daily_wage'];
    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Container(
                  width: 40,
                  height: 40,
                  decoration: BoxDecoration(
                    color: AppTheme.primary.withValues(alpha: .09),
                    borderRadius: BorderRadius.circular(11),
                  ),
                  child: const Icon(Icons.badge_outlined,
                      color: AppTheme.primary, size: 22),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text('${worker['full_name'] ?? 'Worker'}',
                          style: const TextStyle(
                              fontSize: 16, fontWeight: FontWeight.w900)),
                      Text(
                        '${worker['staff_id'] ?? 'No staff ID'}'
                        '${worker['phone'] != null && (worker['phone'] as String).isNotEmpty ? '  •  ${worker['phone']}' : ''}',
                        style: const TextStyle(
                            fontSize: 12, color: AppTheme.textMuted),
                      ),
                    ],
                  ),
                ),
                if (!active)
                  Container(
                    padding: const EdgeInsets.symmetric(
                        horizontal: 8, vertical: 4),
                    decoration: BoxDecoration(
                      color: AppTheme.red50,
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: const Text('INACTIVE',
                        style: TextStyle(
                            fontSize: 10,
                            fontWeight: FontWeight.w800,
                            color: AppTheme.red500A)),
                  ),
              ],
            ),
            const SizedBox(height: 10),
            Text(_shiftSummary(worker),
                style: const TextStyle(fontSize: 12, color: AppTheme.textDark)),
            if (wage != null)
              Text('Daily wage: GH₵ ${wage.toStringAsFixed(2)}',
                  style: const TextStyle(
                      fontSize: 12,
                      color: AppTheme.textMuted,
                      fontWeight: FontWeight.w600)),
            const Divider(height: 18),
            Row(
              children: [
                const Expanded(
                  child: Text('Total paid: GH₵ 0.00',
                      style: TextStyle(
                          fontSize: 12, color: AppTheme.textMuted)),
                ),
                TextButton(
                    onPressed: () => _finance(worker),
                    child: const Text('Finances')),
                TextButton(
                    onPressed: () => _addOrEdit(worker),
                    child: const Text('Edit')),
                IconButton(
                  onPressed: () => _remove(worker),
                  tooltip: 'Remove worker',
                  icon: const Icon(Icons.delete_outline),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  String _shiftSummary(Map<String, dynamic> w) {
    final days = (w['days_of_week'] as List? ?? [])
        .whereType<int>()
        .toList();
    final daysStr =
        days.isEmpty ? 'Flexible days' : days.map((d) => _days[d]).join(',');
    final start = (w['shift_start'] ?? '').toString();
    final end = (w['shift_end'] ?? '').toString();
    final label = (w['shift_label'] ?? '').toString();
    final time = start.isNotEmpty && end.isNotEmpty
        ? '$start – $end'
        : (start.isNotEmpty
            ? start
            : (end.isNotEmpty ? end : ''));
    final parts = [
      if (label.isNotEmpty) label,
      if (time.isNotEmpty) time,
      daysStr,
    ];
    return 'Shift: ${parts.join('  •  ')}';
  }

  Future<void> _addOrEdit([Map<String, dynamic>? worker]) async {
    final saved = await _editor(worker);
    if (saved == true) await _load();
  }

  Future<bool?> _editor(Map<String, dynamic>? worker) async {
    final name = TextEditingController(text: '${worker?['full_name'] ?? ''}');
    final staff = TextEditingController(text: '${worker?['staff_id'] ?? ''}');
    final phone = TextEditingController(text: '${worker?['phone'] ?? ''}');
    final wage = TextEditingController(
        text: worker == null ? '' : '${worker['daily_wage'] ?? ''}');
    final start =
        TextEditingController(text: '${worker?['shift_start'] ?? ''}');
    final end = TextEditingController(text: '${worker?['shift_end'] ?? ''}');
    final label =
        TextEditingController(text: '${worker?['shift_label'] ?? ''}');
    final hours = TextEditingController(
        text: worker == null ? '' : '${worker['weekly_hours'] ?? ''}');
    final days = <int>[
      ...(worker?['days_of_week'] as List? ?? []).whereType<int>()
    ];
    bool active = worker == null ? true : worker['is_active'] != false;
    String? error;

    final saved = await showDialog<bool>(
      context: context,
      builder: (context) => StatefulBuilder(
        builder: (context, setDialogState) => AlertDialog(
          title: Text(worker == null ? 'Employ Worker' : 'Edit Worker'),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                TextField(controller: name,
                    decoration:
                        const InputDecoration(labelText: 'Full name')),
                const SizedBox(height: 10),
                TextField(controller: staff,
                    decoration: const InputDecoration(
                        labelText: 'Staff ID (optional)')),
                const SizedBox(height: 10),
                TextField(controller: phone,
                    keyboardType: TextInputType.phone,
                    decoration: const InputDecoration(
                        labelText: 'Phone (optional)')),
                const SizedBox(height: 10),
                TextField(
                    controller: wage,
                    keyboardType: const TextInputType.numberWithOptions(
                        decimal: true),
                    decoration: const InputDecoration(
                        labelText: 'Daily wage (GH₵, optional)')),
                const SizedBox(height: 12),
                const Align(
                  alignment: Alignment.centerLeft,
                  child: Text('Working days',
                      style: TextStyle(
                          fontSize: 12,
                          fontWeight: FontWeight.w700,
                          color: AppTheme.textMuted)),
                ),
                const SizedBox(height: 6),
                Wrap(
                  spacing: 6,
                  children: [
                    for (var d = 0; d < 7; d++)
                      ChoiceChip(
                        label: Text(_days[d]),
                        selected: days.contains(d),
                        onSelected: (sel) => setDialogState(() {
                          sel ? days.add(d) : days.remove(d);
                        }),
                      ),
                  ],
                ),
                const SizedBox(height: 12),
                Row(
                  children: [
                    Expanded(
                        child: TextField(controller: start,
                            decoration: const InputDecoration(
                                labelText: 'Shift start (HH:MM)',
                                hintText: '08:00'))),
                    const SizedBox(width: 10),
                    Expanded(
                        child: TextField(controller: end,
                            decoration: const InputDecoration(
                                labelText: 'Shift end (HH:MM)',
                                hintText: '16:00'))),
                  ],
                ),
                const SizedBox(height: 10),
                TextField(controller: label,
                    decoration: const InputDecoration(
                        labelText: 'Shift label (optional)',
                        hintText: 'Morning / Night')),
                const SizedBox(height: 10),
                TextField(
                    controller: hours,
                    keyboardType: const TextInputType.numberWithOptions(
                        decimal: true),
                    decoration: const InputDecoration(
                        labelText: 'Weekly hours (optional)')),
                SwitchListTile.adaptive(
                  contentPadding: EdgeInsets.zero,
                  title: const Text('Active worker'),
                  value: active,
                  onChanged: (v) => setDialogState(() => active = v),
                ),
                if (error != null)
                  Align(
                    alignment: Alignment.centerLeft,
                    child: Text(error!,
                        style: TextStyle(
                            color: Theme.of(context).colorScheme.error)),
                  ),
              ],
            ),
          ),
          actions: [
            TextButton(
                onPressed: () => Navigator.pop(context, false),
                child: const Text('Cancel')),
            FilledButton(
              onPressed: () async {
                if (name.text.trim().isEmpty) {
                  setDialogState(() => error = 'Enter the worker name.');
                  return;
                }
                final body = <String, dynamic>{
                  'full_name': name.text.trim(),
                  'staff_id':
                      staff.text.trim().isEmpty ? null : staff.text.trim(),
                  'phone':
                      phone.text.trim().isEmpty ? null : phone.text.trim(),
                  'daily_wage': wage.text.trim().isEmpty
                      ? null
                      : double.tryParse(wage.text.trim()),
                  'days_of_week': days.isEmpty ? null : days,
                  'shift_start':
                      start.text.trim().isEmpty ? null : start.text.trim(),
                  'shift_end':
                      end.text.trim().isEmpty ? null : end.text.trim(),
                  'shift_label':
                      label.text.trim().isEmpty ? null : label.text.trim(),
                  'weekly_hours': hours.text.trim().isEmpty
                      ? null
                      : double.tryParse(hours.text.trim()),
                  if (worker != null) 'is_active': active,
                };
                try {
                  if (worker == null) {
                    await _api.post('vendor/workers',
                        body: body,
                        idempotencyKey: ApiClient.newIdempotencyKey());
                  } else {
                    await _api.put('vendor/workers/${worker['id']}',
                        body: body);
                  }
                  if (context.mounted) Navigator.pop(context, true);
                } on ApiException catch (e) {
                  setDialogState(() => error = e.message);
                } catch (_) {
                  setDialogState(
                      () => error = 'Could not save the worker.');
                }
              },
              child: Text(worker == null ? 'Employ' : 'Save'),
            ),
          ],
        ),
      ),
    );

    name.dispose();
    staff.dispose();
    phone.dispose();
    wage.dispose();
    start.dispose();
    end.dispose();
    label.dispose();
    hours.dispose();
    return saved;
  }

  Future<void> _finance(Map<String, dynamic> worker) async {
    try {
      final data = await _api.get('vendor/workers/${worker['id']}/finance');
      final finance = data is Map && data['finance'] is Map
          ? Map<String, dynamic>.from(data['finance'] as Map)
          : <String, dynamic>{};
      final payments = finance['payments'] is List
          ? finance['payments']
              .whereType<Map>()
              .map(Map<String, dynamic>.from)
              .toList()
          : <Map<String, dynamic>>[];
      if (!mounted) return;
      await showDialog<void>(
        context: context,
        builder: (dialogContext) => AlertDialog(
          title: Text('${worker['full_name']} — Finances'),
          content: SizedBox(
            width: 380,
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'Total paid: GH₵ ${(finance['total_paid'] ?? 0).toStringAsFixed(2)}'
                  '  •  ${finance['payment_count'] ?? 0} payment(s)',
                  style: const TextStyle(fontWeight: FontWeight.w800),
                ),
                const Divider(),
                if (payments.isEmpty)
                  const Padding(
                    padding: EdgeInsets.symmetric(vertical: 8),
                    child: Text('No payments recorded yet.'),
                  )
                else
                  ...payments.map((p) => ListTile(
                        dense: true,
                        contentPadding: EdgeInsets.zero,
                        title: Text(
                            'GH₵ ${(p['amount'] ?? 0).toStringAsFixed(2)}'),
                        subtitle: Text(
                            '${p['payment_date'] ?? ''}'
                            '${p['note'] != null && (p['note'] as String).isNotEmpty ? ' — ${p['note']}' : ''}'),
                      )),
              ],
            ),
          ),
          actions: [
            TextButton(
                onPressed: () => Navigator.pop(dialogContext),
                child: const Text('Close')),
            FilledButton(
              onPressed: () {
                Navigator.pop(dialogContext);
                _recordPayment(worker);
              },
              child: const Text('Record payment'),
            ),
          ],
        ),
      );
    } on ApiException catch (e) {
      _show(e.message);
    } catch (_) {
      _show('Could not load worker finances.');
    }
  }

  Future<void> _recordPayment(Map<String, dynamic> worker) async {
    final amount = TextEditingController();
    final date =
        TextEditingController(text: DateTime.now().toString().split(' ')[0]);
    final note = TextEditingController();

    final ok = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text('Record payment — ${worker['full_name']}'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(
                controller: amount,
                keyboardType: const TextInputType.numberWithOptions(
                    decimal: true),
                decoration:
                    const InputDecoration(labelText: 'Amount (GH₵)')),
            const SizedBox(height: 10),
            TextField(controller: date,
                decoration: const InputDecoration(
                    labelText: 'Date (YYYY-MM-DD)')),
            const SizedBox(height: 10),
            TextField(controller: note,
                decoration:
                    const InputDecoration(labelText: 'Note (optional)')),
          ],
        ),
        actions: [
          TextButton(
              onPressed: () => Navigator.pop(context, false),
              child: const Text('Cancel')),
          FilledButton(
              onPressed: () => Navigator.pop(context, true),
              child: const Text('Save')),
        ],
      ),
    );
    if (ok != true) return;

    try {
      final amt = double.tryParse(amount.text.trim());
      if (amt == null || amt <= 0) {
        _show('Enter a valid amount.');
        return;
      }
      await _api.post(
        'vendor/workers/${worker['id']}/payments',
        body: {
          'amount': amt,
          'payment_date': date.text.trim(),
          'note':
              note.text.trim().isEmpty ? null : note.text.trim(),
        },
        idempotencyKey: ApiClient.newIdempotencyKey(),
      );
      _show('Payment recorded.');
    } on ApiException catch (e) {
      _show(e.message);
    } catch (_) {
      _show('Could not record the payment.');
    }
  }

  Future<void> _remove(Map<String, dynamic> worker) async {
    final sure = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text('Remove ${worker['full_name']}?'),
        content:
            const Text('The worker will be removed from your workforce.'),
        actions: [
          TextButton(
              onPressed: () => Navigator.pop(context, false),
              child: const Text('Cancel')),
          FilledButton(
              onPressed: () => Navigator.pop(context, true),
              child: const Text('Remove')),
        ],
      ),
    );
    if (sure != true) return;
    try {
      await _api.delete('vendor/workers/${worker['id']}');
      await _load();
    } on ApiException catch (e) {
      _show(e.message);
    } catch (_) {
      _show('Could not remove the worker.');
    }
  }

  void _show(String message) {
    if (mounted) {
      ScaffoldMessenger.of(context)
          .showSnackBar(SnackBar(content: Text(message)));
    }
  }
}