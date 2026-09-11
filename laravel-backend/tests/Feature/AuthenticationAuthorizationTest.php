<?php

namespace Tests\Feature;

use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\Hash;
use Tests\TestCase;

class AuthenticationAuthorizationTest extends TestCase
{
    use RefreshDatabase;

    public function test_public_registration_cannot_create_an_admin(): void
    {
        $response = $this->postJson('/api/register', [
            'username' => 'attacker',
            'pin' => '1234',
            'role' => 'ADMIN',
            'fullName' => 'Unauthorized Admin',
        ]);

        $response->assertStatus(422);
        $this->assertDatabaseMissing('users', ['username' => 'attacker']);
    }

    public function test_admin_login_returns_a_sanctum_token(): void
    {
        User::create([
            'username' => 'admin-test',
            'password' => Hash::make('1234'),
            'role' => 'ADMIN',
            'fullName' => 'Test Administrator',
        ]);

        $response = $this->postJson('/api/login', [
            'username' => 'admin-test',
            'pin' => '1234',
        ]);

        $response->assertOk()
            ->assertJsonPath('success', true)
            ->assertJsonPath('user.role', 'ADMIN');

        $this->assertNotEmpty($response->json('token'));
        $this->assertDatabaseCount('personal_access_tokens', 1);
    }

    public function test_non_admin_token_cannot_access_admin_api(): void
    {
        $student = User::create([
            'username' => 'student-test',
            'password' => Hash::make('1234'),
            'role' => 'STUDENT',
            'fullName' => 'Test Student',
        ]);

        $token = $student->createToken('student-test', ['student'])->plainTextToken;

        $this->withToken($token)
            ->getJson('/api/admin/overview')
            ->assertForbidden();
    }

    public function test_admin_token_can_access_admin_api(): void
    {
        $admin = User::create([
            'username' => 'admin-test',
            'password' => Hash::make('1234'),
            'role' => 'ADMIN',
            'fullName' => 'Test Administrator',
        ]);

        $token = $admin->createToken('admin-test', ['admin'])->plainTextToken;

        $this->withToken($token)
            ->getJson('/api/admin/overview')
            ->assertOk()
            ->assertJsonPath('success', true);
    }
}
