<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;

class Menu extends Model
{
    use HasFactory;

    protected $table = 'menus';

    protected $fillable = [
        'vendor_id',
        'food_item_id',
        'price',
        'description',
        'is_available',
    ];

    protected $casts = [
        'vendor_id' => 'integer',
        'food_item_id' => 'integer',
        'price' => 'double',
        'is_available' => 'boolean',
    ];

    /**
     * Get the Vendor profile offering this menu entry.
     */
    public function vendor()
    {
        return $this->belongsTo(Vendor::class, 'vendor_id');
    }

    /**
     * Get the FoodItem associated with this menu entry.
     */
    public function foodItem()
    {
        return $this->belongsTo(FoodItem::class, 'food_item_id');
    }
}
