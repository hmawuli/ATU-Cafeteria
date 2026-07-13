<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;

class DeliveredOrderReview extends Model
{
    use HasFactory;

    protected $table = 'delivered_order_reviews';

    protected $fillable = [
        'order_id',
        'student_id',
        'vendor_id',
        'food_item_id',
        'vendor_rating',
        'vendor_comment',
        'food_rating',
        'food_comment',
    ];

    protected $casts = [
        'order_id' => 'integer',
        'student_id' => 'integer',
        'vendor_id' => 'integer',
        'food_item_id' => 'integer',
        'vendor_rating' => 'integer',
        'food_rating' => 'integer',
    ];

    public function order()
    {
        return $this->belongsTo(Order::class, 'order_id');
    }

    public function student()
    {
        return $this->belongsTo(User::class, 'student_id');
    }

    public function vendor()
    {
        return $this->belongsTo(User::class, 'vendor_id');
    }

    public function foodItem()
    {
        return $this->belongsTo(FoodItem::class, 'food_item_id');
    }
}
