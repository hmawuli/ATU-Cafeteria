<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\SoftDeletes;
use Illuminate\Support\Facades\Auth;

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
        'initial_stock',
        'current_stock',
        'low_stock_threshold', 'sku', 'preparation_minutes', 'dietary_tags', 'allergen_info', 'is_featured',
    ];

    protected static function boot()
    {
        parent::boot();

        static::saving(function ($model) {
            if (empty($model->name) && ! empty($model->food_name)) {
                $model->name = $model->food_name;
            } elseif (empty($model->food_name) && ! empty($model->name)) {
                $model->food_name = $model->name;
            }
        });

        static::updated(function (MenuItem $item) {
            if (! $item->wasChanged('current_stock')) return;

            $delta = (int) $item->current_stock - (int) $item->getOriginal('current_stock');
            $route = null;
            if (! app()->runningInConsole()) {
                $route = request()->path();
            }

            $type = 'ADJUSTMENT';
            if ($route && str_contains($route, 'cancel')) {
                $type = 'RESTOCK';
            } elseif ($route && (str_contains($route, 'customer/orders') || str_contains($route, 'cart-checkout') || str_contains($route, 'student/orders') || str_contains($route, 'vendor/kiosk/orders'))) {
                $type = 'SALE';
            }

            $orderId = null;
            if ($route && request()->has('inventory_order_id')) {
                $orderId = (int) request()->input('inventory_order_id');
            }

            InventoryMovement::create([
                'vendor_id' => $item->vendor_id,
                'menu_item_id' => $item->id,
                'order_id' => $orderId,
                'type' => $type,
                'quantity' => $delta,
                'balance_after' => $item->current_stock,
                'reference' => 'INV-'.strtoupper(bin2hex(random_bytes(6))),
                'reason' => 'Automatic stock audit from application inventory change.',
                'performed_by' => Auth::id(),
            ]);
        });
    }

    protected $casts = [
        'vendor_id' => 'integer',
        'price' => 'double',
        'is_available' => 'boolean',
        'initial_stock' => 'integer',
        'current_stock' => 'integer',
        'low_stock_threshold' => 'integer',
        'preparation_minutes' => 'integer', 'dietary_tags' => 'array', 'is_featured' => 'boolean',
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
