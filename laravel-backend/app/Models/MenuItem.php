<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;

class MenuItem extends Model
{
    use HasFactory;

    protected $table = 'menu_items';

    protected $fillable = [
        'vendor_id',
        'name',
        'price',
        'description',
        'is_available',
    ];

    protected $casts = [
        'vendor_id' => 'integer',
        'price' => 'double',
        'is_available' => 'boolean',
    ];

    /**
     * Associated Vendor (represented as a User with role 'VENDOR' or retrieved from vendors meta).
     */
    public function vendor()
    {
        return $this->belongsTo(User::class, 'vendor_id');
    }
}
