<?php

namespace Tests\Feature;

use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\Hash;
use Laravel\Sanctum\Sanctum;
use Tests\TestCase;

class CustomerDataRightsTest extends TestCase
{
    use RefreshDatabase;

    private function student(float $balance = 0): User
    {
        return User::create([
            'username' => 'rights-student',
            'password' => Hash::make('aStr0ngPin9!'),
            'role' => 'STUDENT',
            'fullName' => 'Rights Student',
            'info' => 'ATU-2026-R01',
            'balance' => $balance,
            'profile_info' => ['email' => 'rights@atu.edu.gh'],
        ]);
    }

    public function test_export_returns_the_students_data(): void
    {
        $user = $this->student();
        Sanctum::actingAs($user, ['*']);

        $this->getJson('/api/customer/account/data-export')
            ->assertOk()
            ->assertJsonPath('success', true)
            ->assertJsonPath('data.profile.username', 'rights-student')
            ->assertJsonStructure([
                'data' => [
                    'profile' => ['id', 'username', 'email', 'balance'],
                    'orders',
                    'wallet_transactions',
                    'devices',
                    'addresses',
                    'exported_at',
                ],
            ]);
    }

    public function test_export_requires_authentication(): void
    {
        $this->getJson('/api/customer/account/data-export')->assertUnauthorized();
    }

    public function test_delete_is_blocked_while_a_wallet_balance_remains(): void
    {
        $user = $this->student(25.00);
        Sanctum::actingAs($user, ['*']);

        $this->deleteJson('/api/customer/account', [], ['Idempotency-Key' => 'del-block-1'])
            ->assertStatus(409);

        $this->assertSame('ACTIVE', (string) ($user->fresh()->account_status ?? 'ACTIVE'));
    }

    public function test_delete_anonymises_personal_data_and_revokes_sessions(): void
    {
        $user = $this->student(0);
        $user->createToken('device-session');
        Sanctum::actingAs($user, ['*']);

        $this->deleteJson('/api/customer/account', [], ['Idempotency-Key' => 'del-ok-1'])
            ->assertOk()
            ->assertJsonPath('success', true);

        $fresh = $user->fresh();
        $this->assertSame('DELETED', $fresh->account_status);
        $this->assertNotSame('rights-student', $fresh->username);
        $this->assertSame('Deleted User', $fresh->fullName);
        $this->assertNull($fresh->profile_info);
        $this->assertFalse(Hash::check('aStr0ngPin9!', $fresh->password));

        $this->assertDatabaseCount('personal_access_tokens', 0);
        $this->assertDatabaseHas('audit_logs', [
            'user_id' => $user->id,
            'action' => 'CUSTOMER_ACCOUNT_CLOSED',
        ]);
    }
}
