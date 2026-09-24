<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;

class Payment extends Model
{
    use HasFactory;

    protected $fillable = [
        'order_id','checkout_session_id','customer_id','reference','gateway','gateway_transaction_id',
        'amount','currency','purpose','method','status','gateway_response',
        'initiated_at','paid_at','failed_at','refunded_at',
    ];

    protected $casts = [
        'order_id' => 'integer',
        'checkout_session_id' => 'integer',
        'customer_id' => 'integer',
        'amount' => 'decimal:2',
        'gateway_response' => 'array',
        'initiated_at' => 'datetime',
        'paid_at' => 'datetime',
        'failed_at' => 'datetime',
        'refunded_at' => 'datetime',
    ];

    public function checkoutSession()
    {
        return $this->belongsTo(CheckoutSession::class, 'checkout_session_id');
    }

    public function order() { return $this->belongsTo(Order::class); }
    public function customer() { return $this->belongsTo(User::class, 'customer_id'); }
    public function refunds() { return $this->hasMany(Refund::class); }
}
