<?php

namespace Tests\Feature;

use App\Models\User;
use App\Notifications\AuthenticationCodeNotification;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\Hash;
use Illuminate\Support\Facades\Notification;
use Tests\TestCase;

class AuthenticationSecurityTest extends TestCase
{
    use RefreshDatabase;

    public function test_enabled_admin_requires_two_factor_before_token_is_issued(): void
    {
        Notification::fake();
        $admin = User::factory()->create([
            'username' => 'admin@example.com', 'password' => Hash::make('1234'), 'role' => 'ADMIN',
            'admin_level' => 'SUPER_ADMIN', 'two_factor_enabled' => true,
        ]);
        $response = $this->postJson('/api/login', ['username' => $admin->username, 'pin' => '1234']);
        $response->assertOk()->assertJsonPath('requires_2fa', true)->assertJsonMissingPath('token');
        Notification::assertSentTo($admin, AuthenticationCodeNotification::class);
        $this->assertDatabaseHas('auth_verification_codes', ['user_id' => $admin->id, 'purpose' => 'ADMIN_2FA']);
    }

    public function test_password_reset_revokes_existing_tokens(): void
    {
        Notification::fake();
        $user = User::factory()->create(['username' => 'reset@example.com', 'password' => Hash::make('1234'), 'role' => 'STUDENT']);
        $this->postJson('/api/password/forgot', ['username' => $user->username])->assertOk();
        $code = null;
        $notification = null;
        Notification::assertSentTo($user, AuthenticationCodeNotification::class, function ($notificationInstance) use (&$notification) {
            $notification = $notificationInstance;

            return true;
        });
        $ref = new \ReflectionClass($notification);
        $prop = $ref->getProperty('code');
        $prop->setAccessible(true);
        $code = $prop->getValue($notification);
        $this->postJson('/api/password/reset', ['username' => $user->username, 'code' => $code, 'pin' => '5678'])->assertOk();
        $this->assertDatabaseCount('personal_access_tokens', 0);
        $this->assertTrue(Hash::check('5678', $user->fresh()->password));
    }
}
