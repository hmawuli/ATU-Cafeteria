<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;

class FavoriteMenuItem extends Model
{
    use HasFactory;

    protected $table = 'favorite_menu_items';

    protected $fillable = [
        'user_id',
        'menu_item_id',
    ];

    protected $casts = [
        'user_id' => 'integer',
        'menu_item_id' => 'integer',
    ];

    /**
     * User who marked the item as favorite.
     */
    public function user()
    {
        return $this->belongsTo(User::class, 'user_id');
    }

    /**
     * The favorited MenuItem.
     */
    public function menuItem()
    {
        return $this->belongsTo(MenuItem::class, 'menu_item_id');
    }
}
