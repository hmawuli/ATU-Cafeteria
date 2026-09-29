<?php

namespace Tests\Feature;

use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\Hash;
use Tests\TestCase;

class AdminBootstrapSecurityTest extends TestCase
{
    use RefreshDatabase;

    public function test_admin_create_rejects_a_known_weak_pin(): void
    {
        $this->artisan('admin:create', ['username' => 'weakadmin', '--pin' => 'admin123'])
            ->expectsOutputToContain('known weak value')
            ->assertExitCode(1);

        $this->assertDatabaseMissing('users', ['username' => 'weakadmin']);
    }

    public function test_admin_create_rejects_a_short_pin(): void
    {
        $this->artisan('admin:create', ['username' => 'shorty', '--pin' => '12'])
            ->assertExitCode(1);

        $this->assertDatabaseMissing('users', ['username' => 'shorty']);
    }

    public function test_admin_create_creates_an_audited_administrator(): void
    {
        $this->artisan('admin:create', ['username' => 'boss', '--pin' => 'x7K9!pW2qL', '--super' => true])
            ->assertExitCode(0);

        $user = User::where('username', 'boss')->first();
        $this->assertNotNull($user);
        $this->assertSame('ADMIN', $user->role);
        $this->assertSame('SUPER_ADMIN', $user->admin_level);
        $this->assertFalse((bool) $user->two_factor_enabled);
        $this->assertTrue(Hash::check('x7K9!pW2qL', $user->password));

        $this->assertDatabaseHas('audit_logs', ['user_id' => $user->id, 'action' => 'ADMIN_CREATED']);
    }

    public function test_admin_create_rejects_duplicate_username(): void
    {
        User::create([
            'username' => 'taken',
            'password' => Hash::make('aStrongP1n'),
            'role' => 'ADMIN',
            'admin_level' => 'CAFETERIA_ADMIN',
            'fullName' => 'Taken',
        ]);

        $this->artisan('admin:create', ['username' => 'taken', '--pin' => 'x7K9!pW2qL'])
            ->assertExitCode(1);
    }

    public function test_security_audit_detects_known_dev_pin_and_can_fail(): void
    {
        User::create([
            'username' => 'devleaky',
            'password' => Hash::make('admin123'),
            'role' => 'ADMIN',
            'admin_level' => 'CAFETERIA_ADMIN',
            'fullName' => 'Leaky',
        ]);

        $this->artisan('security:audit', ['--fail-on-critical' => true])
            ->expectsOutputToContain('devleaky')
            ->assertExitCode(1);
    }

    public function test_security_audit_passes_for_clean_admins(): void
    {
        User::create([
            'username' => 'cleanadmin',
            'password' => Hash::make('a9VeryStr0ng::Pin'),
            'role' => 'ADMIN',
            'admin_level' => 'SUPER_ADMIN',
            'fullName' => 'Clean',
            'two_factor_enabled' => true,
        ]);

        $this->artisan('security:audit', ['--fail-on-critical' => true])
            ->expectsOutputToContain('no known development PINs')
            ->assertExitCode(0);
    }
}
