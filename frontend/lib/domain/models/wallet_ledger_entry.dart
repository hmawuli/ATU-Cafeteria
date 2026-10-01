/// A single wallet ledger line, decoupled from the raw API map so the UI can
/// format it safely.
class WalletLedgerEntry {
  final int? id;

  final String type;

  final double amount;

  final String? reference;

  final String? details;

  final DateTime? at;

  const WalletLedgerEntry({
    this.id,
    required this.type,
    required this.amount,
    this.reference,
    this.details,
    this.at,
  });

  factory WalletLedgerEntry.fromJson(Map<String, dynamic> json) {
    final rawAmount = json['amount'];
    return WalletLedgerEntry(
      id: rawAmount is num ? json['id'] as int? : json['id'] as int?,
      type: (json['type']?.toString() ?? 'UNKNOWN').toUpperCase(),
      amount: rawAmount is num ? rawAmount.toDouble() : 0,
      reference: json['reference']?.toString(),
      details: json['details']?.toString(),
      at: DateTime.tryParse(json['created_at']?.toString() ?? ''),
    );
  }

  /// Credits increase the wallet; everything else decreases it.
  bool get isCredit => type == 'DEPOSIT' || type == 'REFUND';

  /// Human label for the ledger row.
  String get label => switch (type) {
        'DEPOSIT' => 'Top-up',
        'PAYMENT' => 'Order',
        'REFUND' => 'Refund',
        'PAYOUT' => 'Payout',
        _ => type,
      };

  /// Signed display, e.g. "+20.00" or "-15.00".
  String get signedAmount =>
      '${isCredit ? '+' : '-'}${amount.abs().toStringAsFixed(2)}';

  /// Short day label, e.g. "01-Oct".
  String get day {
    final d = at;
    if (d == null) return '—';
    const months = [
      'Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun',
      'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec',
    ];
    return '${d.day.toString().padLeft(2, '0')}-${months[d.month - 1]}';
  }
}