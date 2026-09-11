<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class RequestPerformanceLog extends Model
{
    protected $table = 'request_performance_logs';

    // Disable standard timestamps as we only need 'created_at' and manage it automatically
    public $timestamps = false;

    protected $fillable = [
        'method',
        'path',
        'status_code',
        'execution_time_ms',
        'memory_usage_mb',
        'ip_address',
        'request_payload',
        'created_at',
    ];

    protected $casts = [
        'execution_time_ms' => 'float',
        'memory_usage_mb' => 'float',
        'status_code' => 'integer',
        'created_at' => 'datetime',
    ];
}
