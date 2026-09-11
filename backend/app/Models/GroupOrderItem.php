<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;

class GroupOrderItem extends Model
{
    use HasFactory;

    protected $table = 'group_order_items';

    protected $fillable = [
        'group_order_id',
        'user_id',
        'menu_item_id',
        'quantity',
        'custom_notes',
    ];

    protected $casts = [
        'group_order_id' => 'integer',
        'user_id' => 'integer',
        'menu_item_id' => 'integer',
        'quantity' => 'integer',
    ];

    /**
     * Parent group order session.
     */
    public function groupOrder()
    {
        return $this->belongsTo(GroupOrder::class, 'group_order_id');
    }

    /**
     * Contributor who added this item.
     */
    public function user()
    {
        return $this->belongsTo(User::class, 'user_id');
    }

    /**
     * Menu item contributed.
     */
    public function menuItem()
    {
        return $this->belongsTo(MenuItem::class, 'menu_item_id');
    }
}
