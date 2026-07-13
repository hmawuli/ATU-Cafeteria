<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\SoftDeletes;

class Order extends Model
{
    use HasFactory, SoftDeletes;

    protected $table = 'orders';

    protected $fillable = [
        'customer_id',
        'student_id',
        'user_id',
        'vendor_id',
        'food_item_id',
        'menu_item_id',
        'food_name',
        'quantity',
        'unit_price',
        'total_price',
        'order_timestamp',
        'status',          // PENDING, PREPARING, READY, COMPLETED, DECLINED, CANCELLED
        'order_status',    // Alias/Explicit field
        'pickup_pin',       // 4 digit code e.g. "4932"
        'estimated_pickup_time',
        'points_redeemed',
        'discount_applied',
    ];

    protected $casts = [
        'customer_id' => 'integer',
        'student_id' => 'integer',
        'user_id' => 'integer',
        'vendor_id' => 'integer',
        'food_item_id' => 'integer',
        'menu_item_id' => 'integer',
        'quantity' => 'integer',
        'unit_price' => 'double',
        'total_price' => 'double',
        'order_timestamp' => 'integer',
        'order_status' => 'string',
        'points_redeemed' => 'integer',
        'discount_applied' => 'double',
    ];

    /**
     * Map order_status dynamically to status.
     */
    public function getOrderStatusAttribute()
    {
        return $this->attributes['order_status'] ?? ($this->attributes['status'] ?? null);
    }

    public function setOrderStatusAttribute($value)
    {
        $this->attributes['order_status'] = $value;
        $this->attributes['status'] = $value;
    }

    /**
     * Set user_id and customer_id dynamically to guarantee robust cross-compatibility.
     */
    public function setCustomerIdAttribute($value)
    {
        $this->attributes['customer_id'] = $value;
        $this->attributes['user_id'] = $value;
        $this->attributes['student_id'] = $value;
    }

    public function setUserIdAttribute($value)
    {
        $this->attributes['user_id'] = $value;
        $this->attributes['customer_id'] = $value;
        $this->attributes['student_id'] = $value;
    }

    public function setStudentIdAttribute($value)
    {
        $this->attributes['student_id'] = $value;
        $this->attributes['customer_id'] = $value;
        $this->attributes['user_id'] = $value;
    }

    /**
     * Get the student customer who placed the order.
     */
    public function customer()
    {
        return $this->belongsTo(User::class, 'customer_id');
    }

    /**
     * Get the student who placed the order.
     */
    public function student()
    {
        return $this->belongsTo(User::class, 'student_id');
    }

    /**
     * Secure direct aliased user relationship.
     */
    public function user()
    {
        return $this->belongsTo(User::class, 'user_id');
    }

    /**
     * Advanced International-Standard Relationship: Order Items.
     */
    public function items()
    {
        return $this->hasMany(OrderItem::class, 'order_id');
    }

    /**
     * Get the vendor who accepted the order.
     */
    public function vendor()
    {
        return $this->belongsTo(User::class, 'vendor_id');
    }

    /**
     * Get the food item record, if it exists.
     */
    public function foodItem()
    {
        return $this->belongsTo(FoodItem::class, 'food_item_id');
    }

    /**
     * Get the menu item record, if it exists.
     */
    public function menuItem()
    {
        return $this->belongsTo(MenuItem::class, 'menu_item_id');
    }

    /**
     * Feedback attached to this order.
     */
    public function feedback()
    {
        return $this->hasOne(Feedback::class, 'order_id');
    }
}
