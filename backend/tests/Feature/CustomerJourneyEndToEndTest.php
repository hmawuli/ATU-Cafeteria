<?php

namespace Tests\Feature;

use App\Models\MenuItem;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Str;
use Tests\TestCase;

class CustomerJourneyEndToEndTest extends TestCase
{
    use RefreshDatabase;

    private function resetAuth(): void
    {
        // Each HTTP request in a test shares one auth manager; the Sanctum
        // guard caches the resolved user, so force a fresh resolution whenever
        // the test switches between the student and vendor bearer sessions.
        $this->app['auth']->forgetGuards();
    }

    /**
     * The whole restaurant flow over the real HTTP API, in one journey:
     * register -> login -> catalogue -> order (wallet debit) -> vendor READY
     * -> student ready-poll -> vendor checks out pickup with the PIN.
     */
    public function test_full_customer_journey_registers_orders_and_picks_up(): void
    {
        // 1. Public self-registration.
        $registered = $this->postJson('/api/customer/register', [
            'fullName' => 'Journey Student',
            'username' => 'journey_student',
            'email' => 'journey@example.com',
            'pin' => '1234',
            'pin_confirmation' => '1234',
            'info' => 'E2E journey',
        ])->assertCreated()->assertJsonPath('success', true);

        $studentId = $registered->json('customer.customer_id');
        $student = User::findOrFail($studentId);

        // 2. Real login issues the bearer token used for the rest of the journey.
        $token = $this->postJson('/api/customer/login', [
            'username' => 'journey_student',
            'pin' => '1234',
        ])->assertOk()->assertJsonPath('success', true)->json('token');
        $studentHeaders = ['Authorization' => 'Bearer '.$token];

        // 3. Vendor and stock-tracked menu item.
        $vendor = User::create([
            'username' => 'journey_vendor',
            'password' => hash('sha256', '1111'),
            'role' => 'VENDOR',
            'fullName' => 'Journey Vendor',
            'info' => 'Journey Kitchen',
            'balance' => 0.00,
            'is_open' => true,
        ]);
        $menuItem = MenuItem::create([
            'vendor_id' => $vendor->id,
            'name' => 'Journey Jollof',
            'food_name' => 'Journey Jollof',
            'price' => 25.00,
            'category' => 'Ghanaian Local Dishes',
            'is_available' => true,
            'initial_stock' => 20,
            'current_stock' => 20,
        ]);

        // 4. Fund the student so the wallet debit can be verified.
        $student->update(['balance' => 100.00]);

        // 5. Catalogue is public and lists the item.
        $this->getJson('/api/catalog/menu-items')
            ->assertOk()
            ->assertJsonPath('success', true)
            ->assertJsonFragment(['id' => $menuItem->id]);

        // 6. Place the order over the API (idempotency-protected).
        $this->resetAuth();
        $created = $this->withHeaders($studentHeaders)->postJson(
            '/api/customer/orders',
            [
                'vendor_id' => $vendor->id,
                'menu_item_id' => $menuItem->id,
                'food_name' => $menuItem->name,
                'quantity' => 1,
                'unit_price' => 25.00,
                'total_price' => 25.00,
                'estimated_pickup_time' => '15 mins',
            ],
            ['Idempotency-Key' => 'e2e-'.Str::uuid()->toString()]
        );
        $this->assertTrue($created->status() === 201, 'order POST returned '.$created->status().' body='.$created->getContent());

        $orderId = $created->json('order.id');
        $pickupPin = $created->json('order.pickup_pin');
        $this->assertSame(75.0, (float) $student->refresh()->balance, 'expected balance 75.0, got '.var_export($student->balance, true));

        // 7. The vendor advances the order through the lifecycle to READY.
        $vendorToken = $vendor->createToken('e2e-journey')->plainTextToken;
        $vendorHeaders = ['Authorization' => 'Bearer '.$vendorToken];
        $this->resetAuth();
        $first = $this->withHeaders($vendorHeaders)->patchJson(
            "/api/vendor/orders/{$orderId}/status",
            ['status' => 'PREPARING']
        );
        $this->assertTrue($first->status() === 200, 'vendor PREPARING returned '.$first->status().' body='.$first->getContent());
        $this->withHeaders($vendorHeaders)->patchJson(
            "/api/vendor/orders/{$orderId}/status",
            ['status' => 'READY']
        )->assertOk();

        // 8. The student's ready-order poll now surfaces the order.
        $this->resetAuth();
        $this->withHeaders($studentHeaders)->getJson('/api/customer/orders/poll-ready')
            ->assertOk()
            ->assertJsonFragment(['order_id' => $orderId]);

        // 9. The vendor checks out the pickup with the student's PIN.
        $this->resetAuth();
        $this->withHeaders($vendorHeaders)->postJson(
            "/api/orders/{$orderId}/verify-pickup",
            ['pickup_pin' => $pickupPin]
        )->assertOk()
            ->assertJsonPath('order.status', 'COMPLETED');

        $this->assertDatabaseHas('orders', ['id' => $orderId, 'status' => 'COMPLETED']);
    }
}
