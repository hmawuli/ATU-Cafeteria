<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;
use Illuminate\Database\Eloquent\Relations\HasMany;

class WorkerProfile extends Model
{
    use HasFactory;

    protected $fillable = [
        'vendor_id',
        'full_name',
        'staff_id',
        'phone',
        'daily_wage',
        'days_of_week',
        'shift_start',
        'shift_end',
        'shift_label',
        'weekly_hours',
        'is_active',
    ];

    protected $casts = [
        'days_of_week' => 'array',
        'daily_wage' => 'float',
        'weekly_hours' => 'float',
        'is_active' => 'boolean',
    ];

    public function vendor(): BelongsTo
    {
        return $this->belongsTo(User::class, 'vendor_id');
    }

    public function payments(): HasMany
    {
        return $this->hasMany(WorkerPayment::class, 'worker_profile_id');
    }
}
