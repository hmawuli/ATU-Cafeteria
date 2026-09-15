<?php

namespace Tests\Feature;

use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class AdminGovernanceTest extends TestCase
{
    use RefreshDatabase;

    public function test_cafeteria_admin_can_view_dashboard(): void
    {
        $admin = User::factory()->create(['role' => 'ADMIN', 'admin_level' => 'CAFETERIA_ADMIN', 'account_status' => 'ACTIVE']);
        $response = $this->actingAs($admin, 'sanctum')->getJson('/api/admin/dashboard');
        $response->assertOk()->assertJsonPath('success', true);
    }

    public function test_finance_admin_cannot_change_admin_level(): void
    {
        $admin = User::factory()->create(['role' => 'ADMIN', 'admin_level' => 'FINANCE_ADMIN', 'account_status' => 'ACTIVE']);
        $target = User::factory()->create(['role' => 'ADMIN', 'admin_level' => 'CAFETERIA_ADMIN', 'account_status' => 'ACTIVE']);
        $response = $this->actingAs($admin, 'sanctum')->patchJson("/api/admin/users/{$target->id}/admin-level", ['admin_level' => 'SUPER_ADMIN']);
        $response->assertForbidden();
    }

    public function test_suspended_user_cannot_use_role_protected_endpoint(): void
    {
        $user = User::factory()->create(['role' => 'ADMIN', 'admin_level' => 'SUPER_ADMIN', 'account_status' => 'SUSPENDED']);
        $response = $this->actingAs($user, 'sanctum')->getJson('/api/admin/dashboard');
        $response->assertForbidden();
    }
}
