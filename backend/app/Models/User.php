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

    protected $fillable = [
        'username', 'password', 'role', 'fullName', 'student_staff_id',
        'profile_info', 'info', 'balance', 'is_open', 'loyalty_points',
        'total_spent', 'account_status', 'admin_level', 'last_login_at',
        'two_factor_enabled', 'email_verified_at',
    ];

    protected $casts = [
        'is_open' => 'boolean', 'balance' => 'double', 'profile_info' => 'array',
        'loyalty_points' => 'integer', 'total_spent' => 'double',
        'last_login_at' => 'datetime', 'two_factor_enabled' => 'boolean',
        'email_verified_at' => 'datetime',
    ];

    protected $hidden = ['password'];

    protected $appends = ['email', 'account_type'];

    public function getEmailAttribute($value): ?string
    {
        $profile = is_array($this->profile_info) ? $this->profile_info : [];
        return filter_var($profile['email'] ?? $value, FILTER_VALIDATE_EMAIL)
            ? ($profile['email'] ?? $value) : null;
    }

    /** Restaurant domain label. Existing STUDENT records remain compatible. */
    public function getAccountTypeAttribute(): string
    {
        return strtoupper((string) $this->role) === 'STUDENT' ? 'CUSTOMER' : strtoupper((string) $this->role);
    }

    public function isCustomer(): bool
    {
        return strtoupper((string) $this->role) === 'STUDENT';
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

    public function foodItems() { return $this->hasMany(FoodItem::class, 'vendor_id'); }

    /** Restaurant-facing customer orders. */
    public function customerOrders() { return $this->hasMany(Order::class, 'customer_id'); }

    public function orders() { return $this->hasMany(Order::class, 'user_id'); }
    public function vendorOrders() { return $this->hasMany(Order::class, 'vendor_id'); }
    public function payments() { return $this->hasMany(Payment::class, 'customer_id'); }
    public function customerDevices() { return $this->hasMany(CustomerDevice::class, 'customer_id'); }
    public function addresses() { return $this->hasMany(CustomerAddress::class, 'customer_id'); }
    public function refunds() { return $this->hasMany(Refund::class, 'customer_id'); }
    public function supportTickets() { return $this->hasMany(SupportTicket::class, 'customer_id'); }
    public function vendorSettlements() { return $this->hasMany(VendorSettlement::class, 'vendor_id'); }
    public function payoutAccount() { return $this->hasOne(VendorPayoutAccount::class, 'vendor_id'); }
    public function submittedFeedback() { return $this->hasMany(Feedback::class, 'customer_id'); }
    public function receivedFeedback() { return $this->hasMany(Feedback::class, 'vendor_id'); }
    public function walletTransactions() { return $this->hasMany(WalletTransaction::class, 'user_id'); }

    public function routeNotificationForMail($notification)
    {
        $email = $this->emailAddress();
        if ($email) {
            return $email;
        }

        if (filter_var($this->username, FILTER_VALIDATE_EMAIL)) {
            return $this->username;
        }

        // Legacy institutional fallback remains only for non-customer accounts.
        return $this->isCustomer() ? null : $this->username.'@atu.edu.gh';
    }
}
