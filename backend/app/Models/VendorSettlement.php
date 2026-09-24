<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;

class VendorSettlement extends Model
{
    use HasFactory;

    protected $fillable = [
        'vendor_id','period_start','period_end','gross_sales','refunds','fees',
        'net_amount','status','payout_reference','transfer_code','gateway_status','failure_reason','payout_attempted_at','settled_at',
    ];

    protected $casts = [
        'vendor_id' => 'integer',
        'gross_sales' => 'decimal:2',
        'refunds' => 'decimal:2',
        'fees' => 'decimal:2',
        'net_amount' => 'decimal:2',
        'settled_at' => 'datetime',
        'payout_attempted_at' => 'datetime',
    ];

    public function vendor() { return $this->belongsTo(User::class, 'vendor_id'); }
}
