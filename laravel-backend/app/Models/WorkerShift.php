<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;

class WorkerShift extends Model
{
    use HasFactory;

    protected $table = 'worker_shifts';

    protected $fillable = [
        'vendor_id', 'worker_id', 'shift_name', 'shift_date', 'start_time', 'end_time', 'status',
    ];

    protected $casts = ['shift_date' => 'date'];

    public function vendor() { return $this->belongsTo(User::class, 'vendor_id'); }
    public function worker() { return $this->belongsTo(VendorWorker::class, 'worker_id'); }
}
