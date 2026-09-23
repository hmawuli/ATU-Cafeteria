<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;

class OrderStatusHistory extends Model
{
    use HasFactory;

    protected $fillable = [
        'order_id','from_status','to_status','changed_by','reason','changed_at',
    ];

    protected $casts = [
        'order_id' => 'integer',
        'changed_by' => 'integer',
        'changed_at' => 'datetime',
    ];

    public function order() { return $this->belongsTo(Order::class); }
    public function changedBy() { return $this->belongsTo(User::class, 'changed_by'); }
}
