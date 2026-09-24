<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\SoftDeletes;
use Illuminate\Support\Facades\Auth;

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
        'order_number', 'order_type', 'payment_id', 'checkout_session_id', 'payment_method', 'payment_status',
        'subtotal', 'discount_amount', 'tax_amount', 'service_fee', 'delivery_fee',
        'grand_total', 'currency', 'customer_note', 'cancellation_reason',
        'placed_at', 'confirmed_at', 'cancelled_at',
    ];

    protected $casts = [
        'customer_id' => 'integer',
        'payment_id' => 'integer',
        'checkout_session_id' => 'integer',
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
        'subtotal' => 'decimal:2', 'discount_amount' => 'decimal:2',
        'tax_amount' => 'decimal:2', 'service_fee' => 'decimal:2',
        'delivery_fee' => 'decimal:2', 'grand_total' => 'decimal:2',
        'placed_at' => 'datetime', 'confirmed_at' => 'datetime', 'cancelled_at' => 'datetime',
    ];

    protected static function booted(): void
    {
        static::addGlobalScope('authenticatedOrderAccess', function ($builder) {
            $user = Auth::user();

            if (! $user) {
                return;
            }

            $role = strtoupper((string) ($user->role ?? ''));

            if ($role === 'STUDENT') {
                $builder->where(function ($query) use ($user) {
                    $query->where('customer_id', $user->id)
                        ->orWhere('student_id', $user->id)
                        ->orWhere('user_id', $user->id);
                });
            } elseif ($role === 'VENDOR') {
                $builder->where('vendor_id', $user->id);
            }
        });

        static::created(function (Order $order) {
            OrderStatusHistory::create([
                'order_id' => $order->id,
                'from_status' => null,
                'to_status' => strtoupper((string) $order->status),
                'changed_by' => Auth::id(),
                'reason' => 'Order created.',
                'changed_at' => now(),
            ]);
        });

        static::updated(function (Order $order) {
            if (! $order->wasChanged('status')) return;

            OrderStatusHistory::create([
                'order_id' => $order->id,
                'from_status' => strtoupper((string) $order->getOriginal('status')),
                'to_status' => strtoupper((string) $order->status),
                'changed_by' => Auth::id(),
                'reason' => 'Order status changed.',
                'changed_at' => now(),
            ]);
        });
    }

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

    public function customer()
    {
        return $this->belongsTo(User::class, 'customer_id');
    }

    public function student()
    {
        return $this->belongsTo(User::class, 'student_id');
    }

    public function user()
    {
        return $this->belongsTo(User::class, 'user_id');
    }

    public function items()
    {
        return $this->hasMany(OrderItem::class, 'order_id');
    }

    public function vendor()
    {
        return $this->belongsTo(User::class, 'vendor_id');
    }

    public function foodItem()
    {
        return $this->belongsTo(FoodItem::class, 'food_item_id');
    }

    public function menuItem()
    {
        return $this->belongsTo(MenuItem::class, 'menu_item_id');
    }

    public function feedback()
    {
        return $this->hasOne(Feedback::class, 'order_id');
    }

    public function statusHistory()
    {
        return $this->hasMany(OrderStatusHistory::class, 'order_id')->orderBy('changed_at');
    }

    public function checkoutSession()
    {
        return $this->belongsTo(CheckoutSession::class, 'checkout_session_id');
    }

    public function payments()
    {
        return $this->hasMany(Payment::class, 'order_id');
    }

    public function payment()
    {
        return $this->belongsTo(Payment::class, 'payment_id');
    }

    public function inventoryMovements()
    {
        return $this->hasMany(InventoryMovement::class, 'order_id');
    }

    public function refunds()
    {
        return $this->hasMany(Refund::class, 'order_id');
    }
}