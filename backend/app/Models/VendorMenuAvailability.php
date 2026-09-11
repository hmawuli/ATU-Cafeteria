<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;

class VendorMenuAvailability extends Model
{
    use HasFactory;

    protected $table = 'vendor_menu_availabilities';

    protected $fillable = [
        'vendor_id',
        'menu_item_id',
        'day_of_week',
        'start_time',
        'end_time',
        'is_active',
    ];

    protected $casts = [
        'vendor_id' => 'integer',
        'menu_item_id' => 'integer',
        'is_active' => 'boolean',
    ];

    /**
     * Get the Vendor / User associated with this availability.
     */
    public function vendor()
    {
        return $this->belongsTo(User::class, 'vendor_id');
    }

    /**
     * Get the MenuItem associated with this availability.
     */
    public function menuItem()
    {
        return $this->belongsTo(MenuItem::class, 'menu_item_id');
    }
}
