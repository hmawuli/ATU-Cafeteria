<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\SoftDeletes;

class MenuItem extends Model
{
    use HasFactory, SoftDeletes;

    protected $table = 'menu_items';

    protected $fillable = [
        'vendor_id',
        'food_name',
        'name',
        'price',
        'description',
        'category',
        'is_available',
    ];

    protected static function boot()
    {
        parent::boot();

        static::saving(function ($model) {
            if (empty($model->name) && !empty($model->food_name)) {
                $model->name = $model->food_name;
            } elseif (empty($model->food_name) && !empty($model->name)) {
                $model->food_name = $model->name;
            }
        });
    }

    protected $casts = [
        'vendor_id' => 'integer',
        'price' => 'double',
        'is_available' => 'boolean',
    ];

    /**
     * Associated Vendor (represented as a User with role 'VENDOR' or retrieved from vendors meta).
     */
    public function vendor()
    {
        return $this->belongsTo(User::class, 'vendor_id');
    }

    /**
     * A MenuItem has many Orders.
     */
    public function orders()
    {
        return $this->hasMany(Order::class, 'menu_item_id');
    }
}
