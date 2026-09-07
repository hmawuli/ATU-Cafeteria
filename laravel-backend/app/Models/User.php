<?php

namespace App\Models;

use Laravel\Sanctum\HasApiTokens;
use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Foundation\Auth\User as Authenticatable;
use Illuminate\Notifications\Notifiable;

class User extends Authenticatable
{
    use HasApiTokens, HasFactory, Notifiable;

    protected $table = 'users';

    protected $fillable = [
        'username',
        'password', // Laravel adaptive password hash; never store raw PINs.
        'role',
        'fullName',
        'student_staff_id',
        'profile_info',
        'info',
        'balance',
        'is_open',
        'loyalty_points',
        'total_spent',
    ];

    protected $casts = [
        'is_open' => 'boolean',
        'balance' => 'double',
        'profile_info' => 'array',
        'loyalty_points' => 'integer',
        'total_spent' => 'double',
    ];

    // Never expose password hashes through API responses.
    protected $hidden = [
        'password',
        'remember_token',
    ];

    public function foodItems()
    {
        return $this->hasMany(FoodItem::class, 'vendor_id');
    }

    public function customerOrders()
    {
        return $this->hasMany(Order::class, 'customer_id');
    }

    public function orders()
    {
        return $this->hasMany(Order::class, 'user_id');
    }

    public function vendorOrders()
    {
        return $this->hasMany(Order::class, 'vendor_id');
    }

    public function submittedFeedback()
    {
        return $this->hasMany(Feedback::class, 'customer_id');
    }

    public function receivedFeedback()
    {
        return $this->hasMany(Feedback::class, 'vendor_id');
    }

    public function walletTransactions()
    {
        return $this->hasMany(WalletTransaction::class, 'user_id');
    }

    public function routeNotificationForMail($notification)
    {
        if (filter_var($this->username, FILTER_VALIDATE_EMAIL)) {
            return $this->username;
        }
        return $this->username . '@atu.edu.gh';
    }
}
