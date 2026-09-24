<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;

class PromotionRedemption extends Model
{
    use HasFactory;

    protected $fillable = ['promotion_id','customer_id','order_id','checkout_session_id','discount_amount'];

    protected $casts = [
        'promotion_id' => 'integer',
        'customer_id' => 'integer',
        'order_id' => 'integer',
        'checkout_session_id' => 'integer',
        'discount_amount' => 'decimal:2',
    ];

    public function promotion() { return $this->belongsTo(Promotion::class); }
    public function customer() { return $this->belongsTo(User::class, 'customer_id'); }
    public function order() { return $this->belongsTo(Order::class); }
    public function checkoutSession() { return $this->belongsTo(CheckoutSession::class, 'checkout_session_id'); }
}
