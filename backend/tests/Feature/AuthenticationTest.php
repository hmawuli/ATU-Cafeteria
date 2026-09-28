<?php

namespace Tests\Feature;

use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\Hash;
use Tests\TestCase;

class AuthenticationTest extends TestCase
{
    use RefreshDatabase;

    public function test_student_can_login_with_hashed_pin(): void
    {
        User::factory()->create([
            'username' => 'student-test', 'password' => Hash::make('1234'), 'role' => 'STUDENT',
        ]);
        $response = $this->postJson('/api/login', ['username' => 'student-test', 'pin' => '1234']);
        $response->assertOk()->assertJsonPath('success', true)->assertJsonStructure(['user', 'token']);
    }

    public function test_admin_role_cannot_be_created_from_public_registration(): void
    {
        $response = $this->postJson('/api/register', [
            'username' => 'admin-test', 'pin' => hash('sha256', '1234'), 'role' => 'ADMIN', 'fullName' => 'Admin Test',
        ]);
        $response->assertStatus(422);
    }

    public function test_unauthenticated_api_request_returns_json_401(): void
    {
        $response = $this->getJson('/api/me');

        $response->assertStatus(401)
            ->assertExactJson(['message' => 'Unauthenticated.']);
    }
}
