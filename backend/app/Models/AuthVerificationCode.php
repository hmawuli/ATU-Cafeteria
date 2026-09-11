<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class AuthVerificationCode extends Model
{
    protected $fillable = ['user_id', 'username', 'purpose', 'code_hash', 'expires_at', 'used_at', 'attempts'];

    protected $casts = [
        'expires_at' => 'datetime',
        'used_at' => 'datetime',
    ];
}
