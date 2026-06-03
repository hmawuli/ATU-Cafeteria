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
        'details',
    ];

    protected $casts = [
        'user_id' => 'integer',
        'amount' => 'double',
    ];

    /**
     * Get the User associated with this wallet transaction.
     */
    public function user()
    {
        return $this->belongsTo(User::class, 'user_id');
    }
}
