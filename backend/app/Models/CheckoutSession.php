<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;

class CheckoutSession extends Model
{
    use HasFactory;

    protected $fillable = [
        'customer_id',
        'reference',
        'total_amount',
        'currency',
        'payment_method',
        'status',
    ];

    protected $casts = [
        'customer_id' => 'integer',
        'total_amount' => 'decimal:2',
    ];

    public function customer()
    {
        return $this->belongsTo(User::class, 'customer_id');
    }

    public function orders()
    {
        return $this->hasMany(Order::class, 'checkout_session_id');
    }

    public function payments()
    {
        return $this->hasMany(Payment::class, 'checkout_session_id');
    }
}
