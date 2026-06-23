<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\SoftDeletes;

class Vendor extends Model
{
    use HasFactory, SoftDeletes;

    protected $table = 'vendors';

    protected $fillable = [
        'user_id',
        'name',
        'location',
        'contact_info',
        'operational_status',
        'store_name',
        'location_within_campus',
        'contact_email',
        'operational_hours',
    ];

    protected $casts = [
        'user_id' => 'integer',
        'name' => 'string',
        'location' => 'string',
        'contact_info' => 'string',
        'operational_status' => 'string',
        'store_name' => 'string',
        'location_within_campus' => 'string',
        'contact_email' => 'string',
        'operational_hours' => 'string',
    ];

    /**
     * Get the user account associated with the vendor metadata.
     */
    public function user()
    {
        return $this->belongsTo(User::class, 'user_id');
    }

    /**
     * A Vendor has many MenuItems.
     */
    public function menuItems()
    {
        return $this->hasMany(MenuItem::class, 'vendor_id', 'user_id');
    }
}
