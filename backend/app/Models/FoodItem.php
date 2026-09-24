<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;

class FoodItem extends Model
{
    use HasFactory;

    protected $table = 'food_items';

    protected $fillable = [
        'vendor_id',
        'name',
        'price',
        'category',
        'image_url',
        'description',
        'is_available',
        'initial_stock',
        'current_stock',
        'low_stock_threshold', 'sku', 'preparation_minutes', 'dietary_tags', 'allergen_info', 'is_featured',
    ];

    protected $casts = [
        'price' => 'double',
        'is_available' => 'boolean',
        'initial_stock' => 'integer',
        'current_stock' => 'integer',
        'low_stock_threshold' => 'integer',
        'preparation_minutes' => 'integer', 'dietary_tags' => 'array', 'is_featured' => 'boolean',
    ];

    /**
     * Graceful fallback accessor for initial_stock
     */
    public function getInitialStockAttribute($value)
    {
        return $value !== null ? (int) $value : 50;
    }

    /**
     * Graceful fallback accessor for low_stock_threshold
     */
    public function getLowStockThresholdAttribute($value)
    {
        return $value !== null ? (int) $value : 10;
    }

    /**
     * Get the Vendor User that offers this food dish.
     */
    public function vendor()
    {
        return $this->belongsTo(User::class, 'vendor_id');
    }

    /**
     * Orders associated with this food dish.
     */
    public function orders()
    {
        return $this->hasMany(Order::class, 'food_item_id');
    }
}
