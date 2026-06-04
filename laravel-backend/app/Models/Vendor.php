<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;

class Vendor extends Model
{
    use HasFactory;

    protected $table = 'vendors';

    protected $fillable = [
        'name',
        'location',
        'contact_info',
        'operational_status',
    ];

    protected $casts = [
        'name' => 'string',
        'location' => 'string',
        'contact_info' => 'string',
        'operational_status' => 'string',
    ];
}
