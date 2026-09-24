<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;

class Promotion extends Model
{
    use HasFactory;

    protected $fillable = [
        'vendor_id','code','name','type','value','minimum_order_amount','maximum_discount_amount',
        'usage_limit','per_customer_limit','starts_at','ends_at','is_active',
    ];

    protected $casts = [
        'vendor_id' => 'integer',
        'value' => 'decimal:2',
        'minimum_order_amount' => 'decimal:2',
        'maximum_discount_amount' => 'decimal:2',
        'usage_limit' => 'integer',
        'per_customer_limit' => 'integer',
        'starts_at' => 'datetime',
        'ends_at' => 'datetime',
        'is_active' => 'boolean',
    ];

    public function vendor()
    {
        return $this->belongsTo(User::class, 'vendor_id');
    }

    public function redemptions() { return $this->hasMany(PromotionRedemption::class); }

    public function isCurrentlyActive(): bool
    {
        $now = now();
        return $this->is_active
            && (! $this->starts_at || $this->starts_at->lte($now))
            && (! $this->ends_at || $this->ends_at->gte($now));
    }
}
