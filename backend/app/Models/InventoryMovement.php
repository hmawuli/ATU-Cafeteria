<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;

class InventoryMovement extends Model
{
    use HasFactory;

    protected $fillable = [
        'vendor_id','food_item_id','menu_item_id','order_id','type','quantity',
        'balance_after','reference','reason','performed_by',
    ];

    protected $casts = [
        'vendor_id' => 'integer',
        'food_item_id' => 'integer',
        'menu_item_id' => 'integer',
        'order_id' => 'integer',
        'quantity' => 'integer',
        'balance_after' => 'integer',
        'performed_by' => 'integer',
    ];

    public function vendor() { return $this->belongsTo(User::class, 'vendor_id'); }
    public function foodItem() { return $this->belongsTo(FoodItem::class); }
    public function menuItem() { return $this->belongsTo(MenuItem::class); }
    public function order() { return $this->belongsTo(Order::class); }
    public function performedBy() { return $this->belongsTo(User::class, 'performed_by'); }
}
