<?php

namespace Tests\Feature;

use App\Models\MenuItem;
use App\Models\Order;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Laravel\Sanctum\Sanctum;
use Tests\TestCase;

class VendorOrderConfidentialityTest extends TestCase
{
    use RefreshDatabase;

    protected $student;

    protected $vendor;

    protected $order;

    protected function setUp(): void
    {
        parent::setUp();

        $this->student = User::create([
            'username' => 'confidstudent',
            'password' => hash('sha256', '1234'),
            'role' => 'STUDENT',
            'fullName' => 'Confidential Student',
            'info' => 'ATU-2026-C01',
            'balance' => 100.00,
        ]);

        $this->vendor = User::create([
            'username' => 'confidvendor',
            'password' => hash('sha256', '1111'),
            'role' => 'VENDOR',
            'fullName' => 'Confidential Vendor',
            'info' => 'Confidential Kitchen',
            'balance' => 0.00,
            'is_open' => true,
        ]);

        $menuItem = MenuItem::create([
            'vendor_id' => $this->vendor->id,
            'name' => 'Confidential Waakye',
            'food_name' => 'Confidential Waakye',
            'price' => 20.00,
            'category' => 'Traditional',
            'is_available' => true,
            'initial_stock' => 10,
            'current_stock' => 10,
        ]);

        $this->order = Order::create([
            'customer_id' => $this->student->id,
            'vendor_id' => $this->vendor->id,
            'menu_item_id' => $menuItem->id,
            'food_name' => 'Confidential Waakye',
            'quantity' => 1,
            'unit_price' => 20.00,
            'total_price' => 20.00,
            'order_timestamp' => time() * 1000,
            'status' => 'ORDER_PLACED',
            'pickup_pin' => '4321',
        ]);
    }

    /**
     * Authenticate with a real Sanctum bearer token because the legacy
     * api/orders routes are guarded by RequireAuthenticatedApiRoutes, which
     * resolves the user from a PersonalAccessToken beaner token.
     */
    private function actingWithToken(User $user): void
    {
        $token = $user->createToken('confidentiality-test')->plainTextToken;
        $this->withHeader('Authorization', 'Bearer '.$token);
    }

    public function test_vendor_order_list_never_exposes_pickup_pin(): void
    {
        Sanctum::actingAs($this->vendor, ['*']);

        $this->getJson('/api/vendor/my-orders')
            ->assertOk()
            ->assertJsonMissingPath('0.pickup_pin')
            ->assertJsonMissingPath('0.pickupPin');
    }

    public function test_vendor_status_update_never_exposes_pickup_pin(): void
    {
        $this->actingWithToken($this->vendor);

        $this->putJson("/api/orders/{$this->order->id}/status", ['status' => 'PREPARING'])
            ->assertOk()
            ->assertJsonMissingPath('pickup_pin');
    }

    public function test_vendor_pickup_completion_never_exposes_pickup_pin(): void
    {
        $this->order->status = 'READY';
        $this->order->save();

        $this->actingWithToken($this->vendor);

        $this->postJson("/api/orders/{$this->order->id}/verify-pickup", [
            'pickup_pin' => '4321',
        ])
            ->assertOk()
            ->assertJsonMissingPath('order.pickup_pin');
    }

    public function test_student_still_sees_own_pickup_pin(): void
    {
        $this->actingWithToken($this->student);

        // The student-facing order history endpoint is the one the Flutter app
        // polls. It must keep the PIN so the student can present it at the
        // counter — only vendor-facing responses must hide it.
        $this->getJson("/api/orders/customer/{$this->student->id}")
            ->assertOk()
            ->assertJsonPath('0.pickup_pin', '4321');
    }
}
