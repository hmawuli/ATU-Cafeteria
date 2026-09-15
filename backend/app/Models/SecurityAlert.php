<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;

class SecurityAlert extends Model
{
    use HasFactory;

    protected $fillable = ['user_id', 'type', 'severity', 'message', 'occurred_at', 'resolved_at', 'resolved_by'];

    protected $casts = ['occurred_at' => 'datetime', 'resolved_at' => 'datetime'];

    public function user()
    {
        return $this->belongsTo(User::class);
    }

    public function resolver()
    {
        return $this->belongsTo(User::class, 'resolved_by');
    }
}
