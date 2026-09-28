<?php

namespace Tests\Feature;

use App\Models\MenuItem;
use App\Models\Order;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Laravel\Sanctum\Sanctum;
use Tests\TestCase;

class CustomerReadyPollingTest extends TestCase
{
    use RefreshDatabase;

    protected User $student;

    protected User $vendor;

    protected User $otherStudent;

    protected function setUp(): void
    {
        parent::setUp();

        $this->student = User::create([
            'username' => 'ready-student',
            'password' => hash('sha256', '1234'),
            'role' => 'STUDENT',
            'fullName' => 'Ready Student',
            'info' => 'ATU-2026-R01',
            'balance' => 100.00,
        ]);

        $this->otherStudent = User::create([
            'username' => 'ready-other',
            'password' => hash('sha256', '1234'),
            'role' => 'STUDENT',
            'fullName' => 'Other Student',
            'info' => 'ATU-2026-R02',
            'balance' => 100.00,
        ]);

        $this->vendor = User::create([
            'username' => 'ready-vendor',
            'password' => hash('sha256', '1111'),
            'role' => 'VENDOR',
            'fullName' => 'Ready Vendor',
            'info' => 'Ready Kitchen',
            'is_open' => true,
        ]);
    }

    private function makeOrder(User $student, string $status, ?User $vendor = null): Order
    {
        $vendor ??= $this->vendor;
        $item = MenuItem::create([
            'vendor_id' => $vendor->id,
            'name' => 'Ready Dish',
            'food_name' => 'Ready Dish',
            'price' => 15.00,
            'category' => 'Breakfast',
            'is_available' => true,
            'initial_stock' => 10,
            'current_stock' => 10,
        ]);

        return Order::create([
            'customer_id' => $student->id,
            'vendor_id' => $vendor->id,
            'menu_item_id' => $item->id,
            'food_name' => 'Ready Dish',
            'quantity' => 1,
            'unit_price' => 15.00,
            'total_price' => 15.00,
            'order_timestamp' => time() * 1000,
            'status' => $status,
            'pickup_pin' => '1122',
        ]);
    }

    public function test_student_receives_own_ready_order_alerts(): void
    {
        $order = $this->makeOrder($this->student, 'READY');

        Sanctum::actingAs($this->student, ['*']);

        $this->getJson('/api/customer/orders/poll-ready')
            ->assertOk()
            ->assertJsonPath('success', true)
            ->assertJsonStructure(['ready_alerts' => [['order_id', 'title', 'body']]])
            ->assertJsonFragment(['order_id' => $order->id]);
    }

    public function test_poll_ready_excludes_orders_from_other_students(): void
    {
        $this->makeOrder($this->student, 'READY');
        $this->makeOrder($this->otherStudent, 'READY');

        Sanctum::actingAs($this->student, ['*']);

        $response = $this->getJson('/api/customer/orders/poll-ready')->assertOk();

        $alerts = $response->json('ready_alerts');
        $this->assertCount(1, $alerts);
    }

    public function test_poll_ready_ignores_orders_not_yet_ready(): void
    {
        $this->makeOrder($this->student, 'PREPARING');

        Sanctum::actingAs($this->student, ['*']);

        $this->getJson('/api/customer/orders/poll-ready')
            ->assertOk()
            ->assertJsonPath('ready_alerts', []);
    }

    public function test_vendor_cannot_call_student_ready_polling(): void
    {
        Sanctum::actingAs($this->vendor, ['*']);

        $this->getJson('/api/customer/orders/poll-ready')->assertForbidden();
    }
}
