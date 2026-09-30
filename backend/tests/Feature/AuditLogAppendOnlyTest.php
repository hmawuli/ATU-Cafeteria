<?php

namespace Tests\Feature;

use App\Models\AuditLog;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use LogicException;
use Tests\TestCase;

class AuditLogAppendOnlyTest extends TestCase
{
    use RefreshDatabase;

    public function test_audit_log_cannot_be_deleted_by_default(): void
    {
        $user = User::factory()->create();
        $log = AuditLog::create([
            'user_id' => $user->id,
            'timestamp' => time() * 1000,
            'action' => 'TEST_EVENT',
            'details' => 'Append only.',
        ]);

        $this->expectException(LogicException::class);
        $log->delete();
    }

    public function test_forced_purge_flag_allows_maintenance_delete(): void
    {
        $user = User::factory()->create();
        $log = AuditLog::create([
            'user_id' => $user->id,
            'timestamp' => time() * 1000,
            'action' => 'TEST_EVENT',
            'details' => 'Purge.',
        ]);

        AuditLog::$allowForcedDeletion = true;
        try {
            $this->assertTrue($log->delete());
            $this->assertDatabaseMissing('audit_logs', ['id' => $log->id]);
        } finally {
            AuditLog::$allowForcedDeletion = false;
        }
    }
}
