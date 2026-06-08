<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;

class Vendor extends Model
{
    use HasFactory;

    protected $table = 'vendors';

    protected $fillable = [
        'user_id',
        'name',
        'location',
        'contact_info',
        'operational_status',
    ];

    protected $casts = [
        'user_id' => 'integer',
        'name' => 'string',
        'location' => 'string',
        'contact_info' => 'string',
        'operational_status' => 'string',
    ];

    /**
     * Get the user account associated with the vendor metadata.
     */
    public function user()
    {
        return $this->belongsTo(User::class, 'user_id');
    }
}
