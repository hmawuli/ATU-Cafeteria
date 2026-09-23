<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;

class WalletTransaction extends Model
{
    use HasFactory;

    protected $table = 'wallet_transactions';

    protected $fillable = [
        'user_id',
        'type',      // DEPOSIT, PAYMENT, PAYOUT, REFUND
        'amount',
        'status',    // PENDING, SUCCESS, FAILED
        'reference',
        'details', 'order_id', 'payment_id', 'source', 'performed_by', 'balance_before', 'balance_after',
    ];

    protected $casts = [
        'user_id' => 'integer',
        'amount' => 'double', 'order_id' => 'integer', 'payment_id' => 'integer', 'performed_by' => 'integer', 'balance_before' => 'double', 'balance_after' => 'double',
    ];

    /**
     * Get the User associated with this wallet transaction.
     */
    public function user()
    {
        return $this->belongsTo(User::class, 'user_id');
    }

    public function order() { return $this->belongsTo(Order::class); }
    public function payment() { return $this->belongsTo(Payment::class); }
    public function performedBy() { return $this->belongsTo(User::class, 'performed_by'); }
}
