<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;

class FoodWasteRecord extends Model
{
    use HasFactory;

    protected $fillable = ['vendor_id', 'food_item_id', 'recorded_date', 'prepared_quantity', 'sold_quantity', 'wasted_quantity', 'reason'];

    protected $casts = ['recorded_date' => 'date', 'prepared_quantity' => 'integer', 'sold_quantity' => 'integer', 'wasted_quantity' => 'integer'];

    public function vendor()
    {
        return $this->belongsTo(User::class, 'vendor_id');
    }

    public function foodItem()
    {
        return $this->belongsTo(FoodItem::class);
    }
}
