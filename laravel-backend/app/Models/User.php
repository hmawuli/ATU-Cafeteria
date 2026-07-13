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

    /**
     * The attributes that are mass assignable.
     *
     * @var array<int, string>
     */
    protected $fillable = [
        'username',
        'password', // Stores SHA-256 pin-code hashes for cross-client compliance
        'role',     // STUDENT, VENDOR, ADMIN
        'fullName',
        'student_staff_id', // ATU unique student/staff registration ID
        'profile_info',     // Extended profile details (array)
        'info',     // Student Id or Brand description
        'balance',  // User's virtual wallet balance
        'is_open',  // Vendor open status
        'loyalty_points',
        'total_spent',
    ];

    /**
     * The attributes that should be cast.
     *
     * @var array<string, string>
     */
    protected $casts = [
        'is_open' => 'boolean',
        'balance' => 'double',
        'profile_info' => 'array',
        'loyalty_points' => 'integer',
        'total_spent' => 'double',
    ];

    /**
     * Disable Eloquent automatic password bcrypt hashing if we rely on
     * the custom pre-hashed SHA-256 PIN, keeping it fully compatible with
     * mobile clients.
     */
    protected $hidden = [
        'password',
    ];

    /**
     * FoodItems added by this user (only applicable for VENDOR role)
     */
    public function foodItems()
    {
        return $this->hasMany(FoodItem::class, 'vendor_id');
    }

    /**
     * Orders placed by this user (applicable for STUDENT role)
     */
    public function customerOrders()
    {
        return $this->hasMany(Order::class, 'customer_id');
    }

    /**
     * Direct alias for orders belonging to this user account (STUDENT or general USER)
     */
    public function orders()
    {
        return $this->hasMany(Order::class, 'user_id');
    }

    /**
     * Orders received by this vendor (applicable for VENDOR role)
     */
    public function vendorOrders()
    {
        return $this->hasMany(Order::class, 'vendor_id');
    }

    /**
     * Feedback submitted by this student user
     */
    public function submittedFeedback()
    {
        return $this->hasMany(Feedback::class, 'customer_id');
    }

    /**
     * Feedback received by this vendor user
     */
     public function receivedFeedback()
     {
         return $this->hasMany(Feedback::class, 'vendor_id');
     }

    /**
     * Wallet transactions registered under this user account
     */
    public function walletTransactions()
    {
        return $this->hasMany(WalletTransaction::class, 'user_id');
    }

    /**
     * Route notifications for mail channel.
     */
    public function routeNotificationForMail($notification)
    {
        if (filter_var($this->username, FILTER_VALIDATE_EMAIL)) {
            return $this->username;
        }
        return $this->username . '@atu.edu.gh';
    }
}
