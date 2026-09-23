<?php

namespace Tests\Feature;

use App\Models\CustomerDevice;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Laravel\Sanctum\Sanctum;
use Tests\TestCase;

class CustomerProductionTest extends TestCase
{
    use RefreshDatabase;

    public function test_customer_registration_uses_restaurant_domain(): void
    {
        $response = $this->postJson('/api/customer/register', [
            'fullName' => 'Ama Mensah',
            'username' => 'ama_mensah',
            'email' => 'ama@example.com',
            'pin' => '1234',
            'pin_confirmation' => '1234',
            'info' => 'Regular customer',
        ]);

        $response->assertCreated()
            ->assertJsonPath('success', true)
            ->assertJsonPath('customer.account_type', 'CUSTOMER')
            ->assertJsonStructure(['customer' => ['customer_id', 'token']]);

        $this->assertDatabaseHas('users', [
            'username' => 'ama_mensah',
            'role' => 'STUDENT',
            'account_status' => 'ACTIVE',
        ]);
    }

    public function test_critical_customer_order_requires_idempotency_key(): void
    {
        $user = User::factory()->create([
            'role' => 'STUDENT',
            'account_status' => 'ACTIVE',
        ]);

        Sanctum::actingAs($user, ['customer']);

        $this->postJson('/api/customer/orders', [])
            ->assertStatus(400)
            ->assertJsonPath('error_code', 'IDEMPOTENCY_KEY_REQUIRED');
    }

    public function test_customer_can_register_and_revoke_a_device(): void
    {
        $user = User::factory()->create([
            'role' => 'STUDENT',
            'account_status' => 'ACTIVE',
        ]);

        Sanctum::actingAs($user, ['customer']);

        $create = $this->postJson('/api/customer/devices', [
            'device_id' => 'test-device-001',
            'platform' => 'android',
            'push_token' => 'token-value',
            'app_version' => '1.0.0',
        ]);

        $create->assertCreated()->assertJsonPath('success', true);
        $deviceId = $create->json('device.id');

        $this->deleteJson('/api/customer/devices/'.$deviceId)
            ->assertOk()
            ->assertJsonPath('success', true);

        $this->assertDatabaseHas('customer_devices', [
            'id' => $deviceId,
            'customer_id' => $user->id,
        ]);

        $this->assertNotNull(CustomerDevice::find($deviceId)?->revoked_at);
    }
}
