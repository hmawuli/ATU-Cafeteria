<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;

class OrderItem extends Model
{
    use HasFactory;

    protected $table = 'order_items';

    protected $fillable = [
        'order_id',
        'food_item_id',
        'name',
        'quantity',
        'unit_price',
        'total_price', 'name_snapshot', 'sku_snapshot', 'discount_amount', 'tax_amount', 'line_total',
        'menu_item_id',
    ];

    protected $casts = [
        'order_id' => 'integer',
        'food_item_id' => 'integer',
        'menu_item_id' => 'integer',
        'quantity' => 'integer',
        'unit_price' => 'decimal:2',
        'total_price' => 'decimal:2', 'discount_amount' => 'decimal:2', 'tax_amount' => 'decimal:2', 'line_total' => 'decimal:2',
    ];

    /**
     * Get the order that owns this item.
     */
    public function order()
    {
        return $this->belongsTo(Order::class, 'order_id');
    }

    /**
     * Get the associated food item.
     */
    public function foodItem()
    {
        return $this->belongsTo(FoodItem::class, 'food_item_id');
    }
}
