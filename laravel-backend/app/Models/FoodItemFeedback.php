<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;

class FoodItemFeedback extends Model
{
    use HasFactory;

    protected $table = 'food_item_feedbacks';

    protected $fillable = [
        'order_id',
        'food_item_id',
        'customer_id',
        'rating',
        'comment',
        'timestamp',
    ];

    protected $casts = [
        'order_id' => 'integer',
        'food_item_id' => 'integer',
        'customer_id' => 'integer',
        'rating' => 'integer',
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
     * Associated Food Item.
     */
    public function foodItem()
    {
        return $this->belongsTo(FoodItem::class, 'food_item_id');
    }

    /**
     * Associated Customer.
     */
    public function customer()
    {
        return $this->belongsTo(User::class, 'customer_id');
    }
}
