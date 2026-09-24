import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../core/network/api_client.dart';
import '../../core/theme/app_theme.dart';
import '../widgets/reference_design.dart';

class VendorPromotionsScreen extends StatefulWidget {
  const VendorPromotionsScreen({super.key});

  @override
  State<VendorPromotionsScreen> createState() => _VendorPromotionsScreenState();
}

class _VendorPromotionsScreenState extends State<VendorPromotionsScreen> {
  bool _loading = true;
  List<Map<String, dynamic>> _promotions = [];
  String? _error;

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
      final response = await context.read<ApiClient>().get('/vendor/promotions');
      if (response is! Map) throw const ApiException(500, 'Invalid promotion response.');
      final raw = response['promotions'] is Map ? response['promotions']['data'] : response['promotions'];
      final list = raw is List ? raw.whereType<Map>().map(Map<String, dynamic>.from).toList() : <Map<String, dynamic>>[];
      if (!mounted) return;
      setState(() {
        _promotions = list;
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
        _error = 'Unable to load promotions.';
        _loading = false;
      });
    }
  }

  Future<void> _create() async {
    final formKey = GlobalKey<FormState>();
    final code = TextEditingController();
    final name = TextEditingController();
    final value = TextEditingController();
    final minimumOrder = TextEditingController();
    final maximumDiscount = TextEditingController();
    final usageLimit = TextEditingController();
    final customerLimit = TextEditingController();
    String type = 'PERCENTAGE';

    final ok = await showDialog<bool>(
      context: context,
      builder: (dialogContext) => StatefulBuilder(
        builder: (dialogContext, setDialogState) => AlertDialog(
          title: const Text('Create Vendor Promotion'),
          content: Form(
            key: formKey,
            child: SizedBox(
              width: 480,
              child: SingleChildScrollView(
                child: Column(
                  children: [
                    TextFormField(controller: code, decoration: const InputDecoration(labelText: 'Code e.g. LUNCH10'), validator: (v) => v == null || v.trim().length < 3 ? 'Enter a valid code' : null),
                    TextFormField(controller: name, decoration: const InputDecoration(labelText: 'Promotion name'), validator: (v) => v == null || v.trim().isEmpty ? 'Enter a name' : null),
                    DropdownButtonFormField<String>(
                      initialValue: type,
                      items: const [
                        DropdownMenuItem(value: 'PERCENTAGE', child: Text('Percentage')),
                        DropdownMenuItem(value: 'FIXED', child: Text('Fixed amount')),
                      ],
                      onChanged: (v) => setDialogState(() => type = v ?? type),
                      decoration: const InputDecoration(labelText: 'Type'),
                    ),
                    TextFormField(
                      controller: value,
                      keyboardType: const TextInputType.numberWithOptions(decimal: true),
                      decoration: InputDecoration(labelText: type == 'PERCENTAGE' ? 'Discount percentage' : 'Discount amount (GH₵)'),
                      validator: (v) {
                        final parsed = double.tryParse(v?.trim() ?? '');
                        if (parsed == null || parsed <= 0) return 'Enter a value greater than 0';
                        if (type == 'PERCENTAGE' && parsed > 100) return 'Percentage cannot exceed 100';
                        return null;
                      },
                    ),
                    TextFormField(
                      controller: minimumOrder,
                      keyboardType: const TextInputType.numberWithOptions(decimal: true),
                      decoration: const InputDecoration(labelText: 'Minimum order amount (optional)', prefixText: 'GH₵ '),
                    ),
                    if (type == 'PERCENTAGE')
                      TextFormField(
                        controller: maximumDiscount,
                        keyboardType: const TextInputType.numberWithOptions(decimal: true),
                        decoration: const InputDecoration(labelText: 'Maximum discount amount (optional)', prefixText: 'GH₵ '),
                      ),
                    TextFormField(
                      controller: usageLimit,
                      keyboardType: TextInputType.number,
                      decoration: const InputDecoration(labelText: 'Total usage limit (optional)'),
                    ),
                    TextFormField(
                      controller: customerLimit,
                      keyboardType: TextInputType.number,
                      decoration: const InputDecoration(labelText: 'Per-customer limit (optional)'),
                    ),
                  ],
                ),
              ),
            ),
          ),
          actions: [
            TextButton(onPressed: () => Navigator.pop(dialogContext, false), child: const Text('Cancel')),
            FilledButton(
              onPressed: () async {
                if (!formKey.currentState!.validate()) return;
                try {
                  await context.read<ApiClient>().post(
                    '/vendor/promotions',
                    idempotencyKey: ApiClient.newIdempotencyKey(),
                    body: {
                      'code': code.text.trim().toUpperCase(),
                      'name': name.text.trim(),
                      'type': type,
                      'value': double.parse(value.text.trim()),
                      'minimum_order_amount': _nullableDouble(minimumOrder.text),
                      'maximum_discount_amount': type == 'PERCENTAGE' ? _nullableDouble(maximumDiscount.text) : null,
                      'usage_limit': _nullableInt(usageLimit.text),
                      'per_customer_limit': _nullableInt(customerLimit.text),
                      'is_active': true,
                    },
                  );
                  if (dialogContext.mounted) Navigator.pop(dialogContext, true);
                } catch (e) {
                  if (dialogContext.mounted) {
                    ScaffoldMessenger.of(dialogContext).showSnackBar(SnackBar(content: Text(e.toString().replaceFirst('Exception: ', ''))));
                  }
                }
              },
              child: const Text('Create'),
            ),
          ],
        ),
      ),
    );
    code.dispose();
    name.dispose();
    value.dispose();
    minimumOrder.dispose();
    maximumDiscount.dispose();
    usageLimit.dispose();
    customerLimit.dispose();
    if (ok == true) _load();
  }

  Future<void> _disable(Map<String, dynamic> promotion) async {
    final id = promotion['id'];
    if (id == null) return;
    try {
      await context.read<ApiClient>().delete('/vendor/promotions/$id', idempotencyKey: ApiClient.newIdempotencyKey());
      await _load();
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.toString().replaceFirst('Exception: ', ''))));
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_loading && _promotions.isEmpty) return const Center(child: CircularProgressIndicator());

    return RefreshIndicator(
      onRefresh: _load,
      child: ListView(
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.all(24),
        children: [
          Row(
            children: [
              const Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('Promotions', style: TextStyle(fontSize: 24, fontWeight: FontWeight.w900, color: AppTheme.textDark)),
                    SizedBox(height: 4),
                    Text('Create and control offers for your own kiosk.', style: TextStyle(color: AppTheme.textMuted)),
                  ],
                ),
              ),
              FilledButton.icon(onPressed: _create, icon: const Icon(Icons.add), label: const Text('Create Promotion')),
            ],
          ),
          const SizedBox(height: 16),
          if (_error != null)
            ReferenceCard(child: Text(_error!))
          else if (_promotions.isEmpty)
            const ReferenceCard(child: Text('No vendor promotions yet.'))
          else
            ..._promotions.map((promotion) {
              final active = promotion['is_active'] == true;
              final type = promotion['type']?.toString() ?? 'FIXED';
              final value = promotion['value']?.toString() ?? '0';
              return Padding(
                padding: const EdgeInsets.only(bottom: 10),
                child: ReferenceCard(
                  child: Row(
                    children: [
                      Container(
                        width: 46,
                        height: 46,
                        decoration: BoxDecoration(color: AppTheme.primary.withValues(alpha: .08), borderRadius: BorderRadius.circular(12)),
                        child: const Icon(Icons.local_offer_outlined, color: AppTheme.primary),
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(promotion['code']?.toString() ?? '—', style: const TextStyle(fontWeight: FontWeight.w900)),
                            Text(promotion['name']?.toString() ?? 'Promotion', style: const TextStyle(color: AppTheme.textMuted)),
                            const SizedBox(height: 3),
                            Text(type == 'PERCENTAGE' ? '$value% off' : 'GH₵ $value off', style: const TextStyle(fontWeight: FontWeight.w800)),
                          ],
                        ),
                      ),
                      StatusPill(active ? 'Active' : 'Disabled'),
                      if (active) ...[
                        const SizedBox(width: 8),
                        IconButton(tooltip: 'Disable promotion', onPressed: () => _disable(promotion), icon: const Icon(Icons.pause_circle_outline)),
                      ],
                    ],
                  ),
                ),
              );
            }),
        ],
      ),
    );
  }
}
