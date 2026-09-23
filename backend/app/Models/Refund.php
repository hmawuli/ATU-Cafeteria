<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;

class Refund extends Model
{
    use HasFactory;

    protected $fillable = [
        'order_id','payment_id','customer_id','requested_by','amount','reason',
        'status','gateway_reference','processed_at',
    ];

    protected $casts = [
        'order_id' => 'integer',
        'payment_id' => 'integer',
        'customer_id' => 'integer',
        'requested_by' => 'integer',
        'amount' => 'decimal:2',
        'processed_at' => 'datetime',
    ];

    public function order() { return $this->belongsTo(Order::class); }
    public function payment() { return $this->belongsTo(Payment::class); }
    public function customer() { return $this->belongsTo(User::class, 'customer_id'); }
    public function requestedBy() { return $this->belongsTo(User::class, 'requested_by'); }
}
