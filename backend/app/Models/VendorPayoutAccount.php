<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;

class VendorPayoutAccount extends Model
{
    use HasFactory;

    protected $fillable = [
        'vendor_id', 'type', 'bank_code', 'bank_name', 'account_number',
        'account_number_last4', 'account_name', 'currency', 'recipient_code',
        'status', 'verified_at', 'revoked_at',
    ];

    protected $casts = [
        'vendor_id' => 'integer',
        'account_number' => 'encrypted',
        'verified_at' => 'datetime',
        'revoked_at' => 'datetime',
    ];

    protected $hidden = [
        'account_number',
    ];

    public function vendor()
    {
        return $this->belongsTo(User::class, 'vendor_id');
    }

    public function maskedAccountNumber(): string
    {
        return '••••' . $this->account_number_last4;
    }
}