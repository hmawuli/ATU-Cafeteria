<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;

class VendorWorker extends Model
{
    use HasFactory;

    protected $table = 'vendor_workers';

    protected $fillable = [
        'vendor_id', 'full_name', 'username', 'password', 'position', 'phone', 'is_active',
    ];

    protected $hidden = ['password'];

    protected $casts = ['is_active' => 'boolean'];

    public function vendor()
    {
        return $this->belongsTo(User::class, 'vendor_id');
    }

    public function shifts()
    {
        return $this->hasMany(WorkerShift::class, 'worker_id');
    }
}
