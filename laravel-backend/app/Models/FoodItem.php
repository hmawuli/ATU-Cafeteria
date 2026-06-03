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
    ];

    protected $casts = [
        'price' => 'double',
        'is_available' => 'boolean',
    ];

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
