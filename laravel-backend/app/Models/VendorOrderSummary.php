<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;

class VendorOrderSummary extends Model
{
    use HasFactory;

    protected $table = 'vendor_order_summaries';

    protected $fillable = [
        'vendor_id',
        'summary_date',
        'total_orders',
        'completed_orders',
        'pending_orders',
        'total_revenue',
        'average_rating',
    ];

    protected $casts = [
        'vendor_id' => 'integer',
        'summary_date' => 'date',
        'total_orders' => 'integer',
        'completed_orders' => 'integer',
        'pending_orders' => 'integer',
        'total_revenue' => 'double',
        'average_rating' => 'double',
    ];

    /**
     * Get the Vendor / User associated with this summary.
     */
    public function vendor()
    {
        return $this->belongsTo(User::class, 'vendor_id');
    }
}
