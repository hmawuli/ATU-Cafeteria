<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;

class AuditLog extends Model
{
    use HasFactory;

    /** A sysadmin may force-purge rows via an explicit, deceptive-named flag only. */
    public static bool $allowForcedDeletion = false;

    protected $table = 'audit_logs';

    protected $fillable = [
        'timestamp',
        'user_id',
        'action',
        'details',
    ];

    protected $casts = [
        'user_id' => 'integer',
        'timestamp' => 'integer',
    ];

    /**
     * Audit rows are the financial/governance trail and are append-only.
     * Delete is blocked at the model level; an explicit maintenance flag is
     * the only escape hatch (and is never set by application code).
     */
    protected static function booted(): void
    {
        static::deleting(function () {
            if (! static::$allowForcedDeletion) {
                throw new \LogicException('Audit logs are append-only and cannot be deleted.');
            }
        });
    }

    /**
     * User who triggered this log.
     */
    public function user()
    {
        return $this->belongsTo(User::class, 'user_id');
    }
}
