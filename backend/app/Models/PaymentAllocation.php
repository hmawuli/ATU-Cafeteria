<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;

class PaymentAllocation extends Model
{
    use HasFactory;

    protected $fillable = [
        'payment_id',
        'order_id',
        'amount',
        'refunded_amount',
    ];

    protected $casts = [
        'payment_id' => 'integer',
        'order_id' => 'integer',
        'amount' => 'decimal:2',
        'refunded_amount' => 'decimal:2',
    ];

    public function payment()
    {
        return $this->belongsTo(Payment::class);
    }

    public function order()
    {
        return $this->belongsTo(Order::class);
    }

    public function refundableAmount(): float
    {
        return max(0, round((float) $this->amount - (float) $this->refunded_amount, 2));
    }
}
