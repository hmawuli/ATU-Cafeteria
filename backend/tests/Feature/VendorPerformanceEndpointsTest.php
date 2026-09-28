<?php

namespace Tests\Feature;

use App\Models\MenuItem;
use App\Models\Order;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Laravel\Sanctum\Sanctum;
use Tests\TestCase;

class VendorPerformanceEndpointsTest extends TestCase
{
    use RefreshDatabase;

    protected User $vendor;

    protected User $student;

    protected function setUp(): void
    {
        parent::setUp();

        $this->vendor = User::create([
            'username' => 'perfvendor',
            'password' => hash('sha256', '1111'),
            'role' => 'VENDOR',
            'fullName' => 'Performance Vendor',
            'info' => 'Perf Kitchen',
            'balance' => 0.00,
            'is_open' => true,
        ]);

        $this->student = User::create([
            'username' => 'perfstudent',
            'password' => hash('sha256', '1234'),
            'role' => 'STUDENT',
            'fullName' => 'Performance Student',
            'info' => 'ATU-2026-P01',
            'balance' => 100.00,
        ]);

        $menuItem = MenuItem::create([
            'vendor_id' => $this->vendor->id,
            'name' => 'Perf Jollof',
            'food_name' => 'Perf Jollof',
            'price' => 20.00,
            'category' => 'Ghanaian Local Dishes',
            'is_available' => true,
            'initial_stock' => 10,
            'current_stock' => 10,
        ]);

        Order::create([
            'customer_id' => $this->student->id,
            'vendor_id' => $this->vendor->id,
            'menu_item_id' => $menuItem->id,
            'food_name' => 'Perf Jollof',
            'quantity' => 1,
            'unit_price' => 20.00,
            'total_price' => 20.00,
            'order_timestamp' => time() * 1000,
            'status' => 'COMPLETED',
            'pickup_pin' => '1122',
        ]);
    }

    public function test_vendor_receives_daily_revenue_for_own_orders(): void
    {
        Sanctum::actingAs($this->vendor, ['*']);

        $this->getJson('/api/vendor/daily-revenue')
            ->assertOk()
            ->assertJsonPath('success', true)
            ->assertJsonStructure(['data' => [['date', 'revenue', 'orders_count']]])
            ->assertJsonCount(30, 'data')
            ->assertJsonFragment([
                'date' => now()->toDateString(),
                'revenue' => 20.0,
                'orders_count' => 1,
            ]);
    }

    public function test_vendor_daily_revenue_is_scoped_to_own_vendor(): void
    {
        $otherVendor = User::create([
            'username' => 'othervendor',
            'password' => hash('sha256', '2222'),
            'role' => 'VENDOR',
            'fullName' => 'Other Vendor',
            'info' => 'Other Kitchen',
            'is_open' => true,
        ]);

        $otherItem = MenuItem::create([
            'vendor_id' => $otherVendor->id,
            'name' => 'Other Dish',
            'food_name' => 'Other Dish',
            'price' => 500.00,
            'category' => 'Traditional',
            'is_available' => true,
            'initial_stock' => 5,
            'current_stock' => 5,
        ]);

        Order::create([
            'customer_id' => $this->student->id,
            'vendor_id' => $otherVendor->id,
            'menu_item_id' => $otherItem->id,
            'food_name' => 'Other Dish',
            'quantity' => 1,
            'unit_price' => 500.00,
            'total_price' => 500.00,
            'order_timestamp' => time() * 1000,
            'status' => 'COMPLETED',
            'pickup_pin' => '3344',
        ]);

        Sanctum::actingAs($this->vendor, ['*']);

        // The vendor's own day must still show only their own revenue.
        $this->getJson('/api/vendor/daily-revenue')
            ->assertOk()
            ->assertJsonFragment([
                'date' => now()->toDateString(),
                'revenue' => 20.0,
                'orders_count' => 1,
            ]);
    }

    public function test_customer_cannot_access_vendor_performance_endpoints(): void
    {
        Sanctum::actingAs($this->student, ['*']);

        $this->getJson('/api/vendor/daily-revenue')->assertForbidden();
        $this->getJson('/api/vendor/recharts-sales')->assertForbidden();
    }

    public function test_vendor_receives_recharts_sales_by_date(): void
    {
        Sanctum::actingAs($this->vendor, ['*']);

        $this->getJson('/api/vendor/recharts-sales?vendor_id='.$this->vendor->id)
            ->assertOk()
            ->assertJsonPath('success', true)
            ->assertJsonStructure(['data' => ['by_date']]);
    }
}
