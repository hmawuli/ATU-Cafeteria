<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;

class GroupOrder extends Model
{
    use HasFactory;

    protected $table = 'group_orders';

    protected $fillable = [
        'code',
        'creator_id',
        'vendor_id',
        'status',
        'payment_mode',
        'expires_at',
    ];

    protected $casts = [
        'creator_id' => 'integer',
        'vendor_id' => 'integer',
        'expires_at' => 'datetime',
    ];

    /**
     * User who created the group order session.
     */
    public function creator()
    {
        return $this->belongsTo(User::class, 'creator_id');
    }

    /**
     * Vendor associated with this group order.
     */
    public function vendor()
    {
        return $this->belongsTo(User::class, 'vendor_id');
    }

    /**
     * Items added to this group order session.
     */
    public function items()
    {
        return $this->hasMany(GroupOrderItem::class, 'group_order_id');
    }
}
