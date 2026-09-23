<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;

class CustomerDevice extends Model
{
    use HasFactory;

    protected $fillable = [
        'customer_id','device_id','platform','push_token','app_version',
        'last_seen_at','revoked_at',
    ];

    protected $casts = [
        'customer_id' => 'integer',
        'last_seen_at' => 'datetime',
        'revoked_at' => 'datetime',
    ];

    protected $hidden = ['push_token'];

    public function customer() { return $this->belongsTo(User::class, 'customer_id'); }

    public function scopeActive($query)
    {
        return $query->whereNull('revoked_at');
    }
}
