<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;

class DemandForecast extends Model
{
    use HasFactory;

    protected $fillable = ['vendor_id', 'food_item_id', 'menu_item_id', 'forecast_date', 'predicted_quantity', 'method', 'confidence'];

    protected $casts = [
        'vendor_id' => 'integer',
        'food_item_id' => 'integer',
        'menu_item_id' => 'integer',
        'forecast_date' => 'date',
        'predicted_quantity' => 'integer',
        'confidence' => 'float',
    ];
}
