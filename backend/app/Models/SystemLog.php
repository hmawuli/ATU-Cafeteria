<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class SystemLog extends Model
{
    protected $table = 'system_logs';

    public $timestamps = false; // Only using created_at via database default

    protected $fillable = [
        'level',
        'status_code',
        'method',
        'path',
        'message',
        'stack_trace',
        'ip_address',
        'user_agent',
        'created_at',
    ];

    protected $casts = [
        'status_code' => 'integer',
        'user_agent' => 'array',
        'created_at' => 'datetime',
    ];
}
