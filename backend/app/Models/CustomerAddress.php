<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;

class CustomerAddress extends Model
{
    use HasFactory;

    protected $fillable = [
        'customer_id','label','contact_name','phone','address_line1','address_line2',
        'city','landmark','is_default',
    ];

    protected $casts = [
        'customer_id' => 'integer',
        'is_default' => 'boolean',
    ];

    public function customer() { return $this->belongsTo(User::class, 'customer_id'); }
}
