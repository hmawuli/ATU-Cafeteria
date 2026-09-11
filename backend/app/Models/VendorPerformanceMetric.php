<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;

class VendorPerformanceMetric extends Model
{
    use HasFactory;

    protected $table = 'vendor_performance_metrics';

    protected $fillable = [
        'vendor_id',
        'total_orders',
        'total_completed_orders',
        'total_sales',
        'avg_completion_time_minutes',
        'order_fulfillment_rate',
        'rating_food_quality',
        'rating_cleanliness',
        'rating_service_speed',
        'rating_price_value',
        'rating_overall',
        'popular_menu_items',
        'calculated_at',
    ];

    protected $casts = [
        'vendor_id' => 'integer',
        'total_orders' => 'integer',
        'total_completed_orders' => 'integer',
        'total_sales' => 'double',
        'avg_completion_time_minutes' => 'double',
        'order_fulfillment_rate' => 'double',
        'rating_food_quality' => 'double',
        'rating_cleanliness' => 'double',
        'rating_service_speed' => 'double',
        'rating_price_value' => 'double',
        'rating_overall' => 'double',
        'popular_menu_items' => 'array',
        'calculated_at' => 'datetime',
    ];

    /**
     * Get the Vendor / User associated with this performance record.
     */
    public function vendor()
    {
        return $this->belongsTo(User::class, 'vendor_id');
    }
}
