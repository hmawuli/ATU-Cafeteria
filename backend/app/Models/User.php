<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Foundation\Auth\User as Authenticatable;
use Illuminate\Notifications\Notifiable;
use Laravel\Sanctum\HasApiTokens;

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
        'password', // Laravel-managed password/PIN hash
        'role',     // STUDENT, VENDOR, ADMIN
        'fullName',
        'student_staff_id', // ATU unique student/staff registration ID
        'profile_info',     // Extended profile details (array)
        'info',     // Student Id or Brand description
        'balance',  // User's virtual wallet balance
        'is_open',  // Vendor open status
        'loyalty_points',
        'total_spent',
        'account_status',
        'admin_level',
        'last_login_at',
        'two_factor_enabled',
        'email_verified_at',
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
        'last_login_at' => 'datetime',
        'two_factor_enabled' => 'boolean',
        'email_verified_at' => 'datetime',
    ];

    /**
     * Disable Eloquent automatic password bcrypt hashing if we rely on
     * the custom pre-hashed SHA-256 PIN, keeping it fully compatible with
     * mobile clients.
     */
    protected $hidden = [
        'password',
    ];

    protected $appends = ['email'];

    public function getEmailAttribute($value): ?string
    {
        $profile = is_array($this->profile_info) ? $this->profile_info : [];

        return filter_var($profile['email'] ?? $value, FILTER_VALIDATE_EMAIL) ? ($profile['email'] ?? $value) : null;
    }

    public function emailAddress(): ?string
    {
        $profile = is_array($this->profile_info) ? $this->profile_info : [];
        $email = $profile['email'] ?? null;

        return filter_var($email, FILTER_VALIDATE_EMAIL) ? $email : null;
    }

    public function isActive(): bool
    {
        return strtoupper((string) ($this->account_status ?? 'ACTIVE')) === 'ACTIVE';
    }

    public function hasPermission(string $permission): bool
    {
        $role = strtoupper((string) $this->role);
        if ($role === 'ADMIN') {
            $level = strtoupper((string) ($this->admin_level ?? 'CAFETERIA_ADMIN'));
            $permissions = config("permissions.roles.$level", []);

            return $permissions === '*' || in_array($permission, $permissions, true);
        }

        $permissions = config('permissions.'.strtolower($role), []);

        return in_array($permission, $permissions, true);
    }

    public function isSuperAdmin(): bool
    {
        return strtoupper((string) $this->role) === 'ADMIN'
            && strtoupper((string) ($this->admin_level ?? 'CAFETERIA_ADMIN')) === 'SUPER_ADMIN';
    }

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

        return $this->username.'@atu.edu.gh';
    }
}
