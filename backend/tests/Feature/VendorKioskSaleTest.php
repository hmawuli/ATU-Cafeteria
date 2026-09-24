<?php

namespace Tests\Feature;

use App\Models\MenuItem;
use App\Models\InventoryMovement;
use App\Models\Order;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Laravel\Sanctum\Sanctum;
use Tests\TestCase;

class VendorKioskSaleTest extends TestCase
{
    use RefreshDatabase;

    public function test_vendor_can_record_walk_in_kiosk_sale_without_customer_account(): void
    {
        $vendor = User::factory()->create([
            'role' => 'VENDOR',
            'account_status' => 'ACTIVE',
        ]);

        $item = MenuItem::create([
            'vendor_id' => $vendor->id,
            'food_name' => 'Kiosk Meal',
            'name' => 'Kiosk Meal',
            'price' => 12.50,
            'description' => 'Walk-in kiosk meal.',
            'category' => 'Meals',
            'is_available' => true,
            'initial_stock' => 10,
            'current_stock' => 10,
            'low_stock_threshold' => 2,
        ]);

        Sanctum::actingAs($vendor, ['vendor']);

        $response = $this->postJson('/api/vendor/kiosk/orders', [
            'items' => [
                ['menu_item_id' => $item->id, 'quantity' => 2],
            ],
            'customer_name' => 'Walk-in Customer',
            'payment_method' => 'CASH',
        ], [
            'Idempotency-Key' => 'kiosk-test-001',
        ]);

        $response->assertCreated()
            ->assertJsonPath('success', true)
            ->assertJsonPath('order.sales_channel', 'KIOSK')
            ->assertJsonPath('order.customer_id', null);

        $orderId = $response->json('order.id');

        $this->assertDatabaseHas('orders', [
            'id' => $orderId,
            'vendor_id' => $vendor->id,
            'customer_id' => null,
            'sales_channel' => 'KIOSK',
            'status' => 'COMPLETED',
            'grand_total' => 25,
        ]);
        $this->assertDatabaseHas('payments', [
            'order_id' => $orderId,
            'customer_id' => null,
            'amount' => 25,
            'purpose' => 'WALK_IN_SALE',
            'status' => 'SUCCESS',
        ]);
        $this->assertSame(8, (int) MenuItem::find($item->id)->current_stock);
        $this->assertDatabaseHas('inventory_movements', [
            'order_id' => $orderId,
            'menu_item_id' => $item->id,
            'quantity' => -2,
            'type' => 'SALE',
        ]);
        $this->assertSame(0, Order::withoutGlobalScopes()->where('id', $orderId)->whereNotNull('customer_id')->count());
    }
}
