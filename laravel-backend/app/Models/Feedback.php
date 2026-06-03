<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;

class Feedback extends Model
{
    use HasFactory;

    protected $table = 'feedback';

    protected $fillable = [
        'order_id',
        'vendor_id',
        'customer_id',
        'rating_food_quality', // 1-5
        'rating_cleanliness',  // 1-5
        'rating_service_speed', // 1-5
        'rating_price_value',  // 1-5
        'comment',
        'timestamp',
    ];

    protected $casts = [
        'order_id' => 'integer',
        'vendor_id' => 'integer',
        'customer_id' => 'integer',
        'rating_food_quality' => 'integer',
        'rating_cleanliness' => 'integer',
        'rating_service_speed' => 'integer',
        'rating_price_value' => 'integer',
        'timestamp' => 'integer',
    ];

    /**
     * Associated Order.
     */
    public function order()
    {
        return $this->belongsTo(Order::class, 'order_id');
    }

    /**
     * Associated Customer.
     */
    public function customer()
    {
        return $this->belongsTo(User::class, 'customer_id');
    }

    /**
     * Associated Vendor.
     */
    public function vendor()
    {
        return $this->belongsTo(User::class, 'vendor_id');
    }
}
