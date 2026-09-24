<?php

namespace Tests\Feature;

use App\Models\CheckoutSession;
use App\Models\MenuItem;
use App\Models\Order;
use App\Models\Payment;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Laravel\Sanctum\Sanctum;
use Tests\TestCase;

class ProductionCheckoutTest extends TestCase
{
    use RefreshDatabase;

    public function test_cart_checkout_creates_one_vendor_order_with_multiple_line_items(): void
    {
        $customer = User::factory()->create([
            'role' => 'STUDENT',
            'account_status' => 'ACTIVE',
            'balance' => 100,
            'loyalty_points' => 0,
        ]);
        $vendor = User::factory()->create([
            'role' => 'VENDOR',
            'account_status' => 'ACTIVE',
        ]);

        $rice = MenuItem::create([
            'vendor_id' => $vendor->id,
            'food_name' => 'Jollof Rice',
            'name' => 'Jollof Rice',
            'price' => 15,
            'description' => 'Fresh jollof rice.',
            'category' => 'Meals',
            'is_available' => true,
            'initial_stock' => 20,
            'current_stock' => 20,
            'low_stock_threshold' => 3,
        ]);
        $drink = MenuItem::create([
            'vendor_id' => $vendor->id,
            'food_name' => 'Sobolo',
            'name' => 'Sobolo',
            'price' => 5,
            'description' => 'Chilled hibiscus drink.',
            'category' => 'Drinks',
            'is_available' => true,
            'initial_stock' => 20,
            'current_stock' => 20,
            'low_stock_threshold' => 3,
        ]);

        Sanctum::actingAs($customer, ['customer']);

        $this->postJson('/api/customer/cart-checkout/preview', [
            'items' => [
                ['menu_item_id' => $rice->id, 'quantity' => 1],
                ['menu_item_id' => $drink->id, 'quantity' => 2],
            ],
        ])->assertOk()
          ->assertJsonPath('final_total', 25)
          ->assertJsonPath('subtotal', 25);

        $response = $this->postJson('/api/customer/cart-checkout', [
            'items' => [
                ['menu_item_id' => $rice->id, 'quantity' => 1],
                ['menu_item_id' => $drink->id, 'quantity' => 2],
            ],
            'payment_method' => 'WALLET',
        ], [
            'Idempotency-Key' => 'checkout-test-001',
        ]);

        $response->assertOk()
            ->assertJsonPath('success', true);

        $sessionId = $response->json('checkout_session.id');
        $orderIds = collect($response->json('orders'))->pluck('id');

        $this->assertCount(1, $orderIds);
        $this->assertDatabaseHas('checkout_sessions', [
            'id' => $sessionId,
            'customer_id' => $customer->id,
            'total_amount' => 25,
        ]);
        $this->assertDatabaseHas('payments', [
            'checkout_session_id' => $sessionId,
            'customer_id' => $customer->id,
            'amount' => 25,
            'status' => 'SUCCESS',
        ]);
        $this->assertDatabaseHas('orders', [
            'id' => $orderIds->first(),
            'checkout_session_id' => $sessionId,
            'vendor_id' => $vendor->id,
            'subtotal' => 25,
            'grand_total' => 25,
        ]);
        $this->assertCount(2, \DB::table('order_items')->where('order_id', $orderIds->first())->get());

        $order = Order::withoutGlobalScopes()->with('items')->findOrFail($orderIds->first());
        $this->assertSame(2, $order->items->count());
        $this->assertSame(75.0, (float) User::find($customer->id)->balance);
        $this->assertSame(19, (int) MenuItem::find($rice->id)->current_stock);
        $this->assertSame(18, (int) MenuItem::find($drink->id)->current_stock);
        $this->assertNotNull(CheckoutSession::find($sessionId));
        $this->assertNotNull(Payment::where('checkout_session_id', $sessionId)->first());
    }
}
