import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../core/network/api_client.dart';
import '../../core/theme/app_theme.dart';
import '../widgets/reference_design.dart';

class VendorPayoutAccountScreen extends StatefulWidget {
  const VendorPayoutAccountScreen({super.key});

  @override
  State<VendorPayoutAccountScreen> createState() => _VendorPayoutAccountScreenState();
}

class _VendorPayoutAccountScreenState extends State<VendorPayoutAccountScreen> {
  bool _loading = true;
  bool _saving = false;
  String _type = 'BANK';
  List<Map<String, dynamic>> _channels = <Map<String, dynamic>>[];
  Map<String, dynamic>? _account;
  String? _error;
  final _formKey = GlobalKey<FormState>();
  final _bankCode = TextEditingController();
  final _accountNumber = TextEditingController();
  final _accountName = TextEditingController();
  final _bankName = TextEditingController();

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _bankCode.dispose();
    _accountNumber.dispose();
    _accountName.dispose();
    _bankName.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });

    final api = context.read<ApiClient>();
    Map<String, dynamic>? account;
    List<Map<String, dynamic>> channels = <Map<String, dynamic>>[];
    String? accountError;
    String? channelError;

    try {
      final response = await api.get('/vendor/payout-account');
      if (response is Map && response['account'] is Map) {
        account = Map<String, dynamic>.from(response['account']);
      }
    } catch (e) {
      accountError = e.toString().replaceFirst('Exception: ', '');
    }

    try {
      final response = await api.get('/vendor/payout-banks?type=' + _type);
      if (response is Map && response['channels'] is List) {
        channels = response['channels']
            .whereType<Map>()
            .map(Map<String, dynamic>.from)
            .toList();
      }
    } catch (_) {
      channelError = 'Payout channels are unavailable until Paystack transfers are configured.';
    }

    if (!mounted) return;
    setState(() {
      _channels = channels;
      _account = account;
      _error = accountError ?? channelError;
      _loading = false;
    });
  }

  Future<void> _changeType(String value) async {
    setState(() {
      _type = value;
      _bankCode.clear();
      _bankName.clear();
    });
    try {
      final response = await context.read<ApiClient>().get('/vendor/payout-banks?type=' + value);
      final channels = response is Map && response['channels'] is List
          ? response['channels'].whereType<Map>().map(Map<String, dynamic>.from).toList()
          : <Map<String, dynamic>>[];
      if (mounted) setState(() => _channels = channels);
    } catch (_) {}
  }

  Future<void> _save() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() => _saving = true);
    try {
      final response = await context.read<ApiClient>().post(
        '/vendor/payout-account',
        idempotencyKey: ApiClient.newIdempotencyKey(),
        body: {
          'type': _type,
          'bank_code': _bankCode.text.trim(),
          'bank_name': _bankName.text.trim().isEmpty ? null : _bankName.text.trim(),
          'account_number': _accountNumber.text.trim(),
          if (_type == 'MOBILE_MONEY') 'account_name': _accountName.text.trim(),
        },
      );
      if (!mounted) return;
      setState(() {
        _account = response is Map && response['account'] is Map
            ? Map<String, dynamic>.from(response['account'])
            : _account;
        _saving = false;
      });
      _accountNumber.clear();
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Payout account verified and saved.')),
      );
    } catch (e) {
      if (!mounted) return;
      setState(() => _saving = false);
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(e.toString().replaceFirst('Exception: ', ''))),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) return const Center(child: CircularProgressIndicator());

    return RefreshIndicator(
      onRefresh: _load,
      child: ListView(
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.all(24),
        children: [
          const Text('Payout Account', style: TextStyle(fontSize: 24, fontWeight: FontWeight.w900, color: AppTheme.textDark)),
          const SizedBox(height: 4),
          const Text('Receive vendor settlements securely through your verified bank account or mobile money channel.'),
          const SizedBox(height: 18),
          if (_account != null)
            ReferenceCard(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(children: [
                    const Icon(Icons.verified_outlined, color: AppTheme.primary),
                    const SizedBox(width: 10),
                    Expanded(child: Text(_account!['account_name']?.toString() ?? 'Verified account', style: const TextStyle(fontSize: 17, fontWeight: FontWeight.w900))),
                    StatusPill(_account!['status']?.toString() ?? 'PENDING'),
                  ]),
                  const SizedBox(height: 10),
                  Text((_account!['type']?.toString() ?? 'BANK') + ' • ' + (_account!['bank_name']?.toString() ?? _account!['bank_code']?.toString() ?? 'Channel')),
                  const SizedBox(height: 4),
                  Text(_account!['account_number']?.toString() ?? '••••', style: const TextStyle(fontWeight: FontWeight.w700)),
                ],
              ),
            ),
          const SizedBox(height: 14),
          ReferenceCard(
            child: Form(
              key: _formKey,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  const Text('Add or replace payout details', style: TextStyle(fontSize: 17, fontWeight: FontWeight.w900)),
                  const SizedBox(height: 12),
                  DropdownButtonFormField<String>(
                    initialValue: _type,
                    items: const [
                      DropdownMenuItem(value: 'BANK', child: Text('Ghana bank account')),
                      DropdownMenuItem(value: 'MOBILE_MONEY', child: Text('Mobile money')),
                    ],
                    onChanged: (value) { if (value != null) _changeType(value); },
                    decoration: const InputDecoration(labelText: 'Payout method'),
                  ),
                  const SizedBox(height: 8),
                  if (_channels.isNotEmpty)
                    DropdownButtonFormField<String>(
                      initialValue: _channels.any((c) => c['code']?.toString() == _bankCode.text) ? _bankCode.text : null,
                      items: _channels.map((channel) => DropdownMenuItem<String>(
                        value: channel['code']?.toString(),
                        child: Text(channel['name']?.toString() ?? channel['code']?.toString() ?? 'Channel'),
                      )).toList(),
                      onChanged: (value) {
                        Map<String, dynamic>? selected;
                        for (final channel in _channels) {
                          if (channel['code']?.toString() == value) {
                            selected = channel;
                            break;
                          }
                        }
                        _bankCode.text = value ?? '';
                        _bankName.text = selected?['name']?.toString() ?? '';
                        setState(() {});
                      },
                      decoration: InputDecoration(labelText: _type == 'BANK' ? 'Bank' : 'Mobile network'),
                      validator: (_) => _bankCode.text.trim().isEmpty ? 'Select a payout channel' : null,
                    )
                  else
                    TextFormField(
                      controller: _bankCode,
                      decoration: const InputDecoration(labelText: 'Bank / network code'),
                      validator: (v) => v == null || v.trim().isEmpty ? 'Enter the channel code' : null,
                    ),
                  const SizedBox(height: 8),
                  TextFormField(
                    controller: _accountNumber,
                    keyboardType: TextInputType.number,
                    decoration: InputDecoration(labelText: _type == 'BANK' ? 'Account number' : 'Mobile number'),
                    validator: (v) => v == null || v.trim().length < 7 ? 'Enter valid payout details' : null,
                  ),
                  if (_type == 'MOBILE_MONEY') ...[
                    const SizedBox(height: 8),
                    TextFormField(
                      controller: _accountName,
                      decoration: const InputDecoration(labelText: 'Account holder name'),
                      validator: (v) => v == null || v.trim().isEmpty ? 'Enter the account holder name' : null,
                    ),
                  ],
                  const SizedBox(height: 16),
                  FilledButton.icon(
                    onPressed: _saving ? null : _save,
                    icon: _saving ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2)) : const Icon(Icons.verified_user_outlined),
                    label: Text(_saving ? 'Verifying…' : 'Verify & Save'),
                  ),
                  const SizedBox(height: 8),
                  const Text(
                    'Bank accounts are resolved with Paystack before a recipient is created. Account numbers are encrypted at rest by Laravel.',
                    style: TextStyle(fontSize: 11, color: AppTheme.textMuted),
                  ),
                ],
              ),
            ),
          ),
          if (_error != null) ...[
            const SizedBox(height: 12),
            ReferenceCard(child: Text(_error!)),
          ],
        ],
      ),
    );
  }
}