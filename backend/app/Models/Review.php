<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;

class Review extends Model
{
    use HasFactory;

    protected $table = 'reviews';

    protected $fillable = [
        'user_id',
        'vendor_id',
        'rating',
        'comment',
    ];

    protected $casts = [
        'user_id' => 'integer',
        'vendor_id' => 'integer',
        'rating' => 'integer',
        'comment' => 'string',
    ];

    /**
     * Get the student (user) associated with this Review.
     */
    public function user()
    {
        return $this->belongsTo(User::class, 'user_id');
    }

    /**
     * Get the vendor associated with this Review.
     */
    public function vendor()
    {
        return $this->belongsTo(Vendor::class, 'vendor_id');
    }
}
