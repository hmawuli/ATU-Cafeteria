<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

class WorkerPayment extends Model
{
    use HasFactory;

    protected $table = 'worker_payment_ledger';

    protected $fillable = [
        'worker_profile_id',
        'amount',
        'payment_date',
        'note',
        'recorded_by',
    ];

    protected $casts = [
        'payment_date' => 'date',
        'amount' => 'float',
    ];

    public function profile(): BelongsTo
    {
        return $this->belongsTo(WorkerProfile::class, 'worker_profile_id');
    }

    public function recorder(): BelongsTo
    {
        return $this->belongsTo(User::class, 'recorded_by');
    }
}
