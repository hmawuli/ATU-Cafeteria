<?php

namespace Tests\Feature;

use App\Models\CheckoutSession;
use App\Models\MenuItem;
use App\Models\Order;
use App\Models\Payment;
use App\Models\PaymentAllocation;
use App\Models\Promotion;
use App\Models\PromotionRedemption;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\Cache;
use Laravel\Sanctum\Sanctum;
use Tests\TestCase;

class ProductionCheckoutTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();

        Cache::flush();
    }

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
        $this->assertDatabaseHas('payment_allocations', [
            'payment_id' => Payment::where('checkout_session_id', $sessionId)->value('id'),
            'order_id' => $orderIds->first(),
            'amount' => 25,
            'refunded_amount' => 0,
        ]);
    }

    public function test_multi_vendor_checkout_creates_allocations_for_each_vendor_order(): void
    {
        $customer = User::factory()->create([
            'role' => 'STUDENT',
            'account_status' => 'ACTIVE',
            'balance' => 100,
            'loyalty_points' => 0,
        ]);
        $vendorA = User::factory()->create(['role' => 'VENDOR', 'account_status' => 'ACTIVE']);
        $vendorB = User::factory()->create(['role' => 'VENDOR', 'account_status' => 'ACTIVE']);

        $mealA = MenuItem::create([
            'vendor_id' => $vendorA->id,
            'food_name' => 'Vendor A Meal',
            'name' => 'Vendor A Meal',
            'price' => 18,
            'description' => 'Meal A.',
            'category' => 'Meals',
            'is_available' => true,
            'initial_stock' => 10,
            'current_stock' => 10,
            'low_stock_threshold' => 2,
        ]);
        $mealB = MenuItem::create([
            'vendor_id' => $vendorB->id,
            'food_name' => 'Vendor B Meal',
            'name' => 'Vendor B Meal',
            'price' => 7,
            'description' => 'Meal B.',
            'category' => 'Meals',
            'is_available' => true,
            'initial_stock' => 10,
            'current_stock' => 10,
            'low_stock_threshold' => 2,
        ]);

        Sanctum::actingAs($customer, ['customer']);

        $response = $this->postJson('/api/customer/cart-checkout', [
            'items' => [
                ['menu_item_id' => $mealA->id, 'quantity' => 1],
                ['menu_item_id' => $mealB->id, 'quantity' => 1],
            ],
            'payment_method' => 'WALLET',
        ], [
            'Idempotency-Key' => 'checkout-split-test-001',
        ]);

        $response->assertOk()->assertJsonPath('success', true);

        $sessionId = $response->json('checkout_session.id');
        $paymentId = Payment::where('checkout_session_id', $sessionId)->value('id');
        $orders = Order::withoutGlobalScopes()->where('checkout_session_id', $sessionId)->get();

        $this->assertCount(2, $orders);
        $this->assertSame(25.0, (float) Payment::find($paymentId)->amount);
        $this->assertSame(2, PaymentAllocation::where('payment_id', $paymentId)->count());
        $this->assertSame(18.0, (float) PaymentAllocation::where('payment_id', $paymentId)->where('order_id', $orders->first(fn ($o) => $o->vendor_id === $vendorA->id)->id)->value('amount'));
        $this->assertSame(7.0, (float) PaymentAllocation::where('payment_id', $paymentId)->where('order_id', $orders->first(fn ($o) => $o->vendor_id === $vendorB->id)->id)->value('amount'));
    }

    public function test_split_checkout_cancel_refunds_only_one_vendor_allocation_until_all_orders_are_cancelled(): void
    {
        $customer = User::factory()->create([
            'role' => 'STUDENT',
            'account_status' => 'ACTIVE',
            'balance' => 100,
            'loyalty_points' => 0,
        ]);
        $vendorA = User::factory()->create(['role' => 'VENDOR', 'account_status' => 'ACTIVE']);
        $vendorB = User::factory()->create(['role' => 'VENDOR', 'account_status' => 'ACTIVE']);

        $mealA = MenuItem::create([
            'vendor_id' => $vendorA->id,
            'food_name' => 'Vendor A Meal',
            'name' => 'Vendor A Meal',
            'price' => 10,
            'description' => 'Meal A.',
            'category' => 'Meals',
            'is_available' => true,
            'initial_stock' => 10,
            'current_stock' => 10,
            'low_stock_threshold' => 2,
        ]);
        $mealB = MenuItem::create([
            'vendor_id' => $vendorB->id,
            'food_name' => 'Vendor B Meal',
            'name' => 'Vendor B Meal',
            'price' => 30,
            'description' => 'Meal B.',
            'category' => 'Meals',
            'is_available' => true,
            'initial_stock' => 10,
            'current_stock' => 10,
            'low_stock_threshold' => 2,
        ]);

        Sanctum::actingAs($customer, ['customer']);

        $checkout = $this->postJson('/api/customer/cart-checkout', [
            'items' => [
                ['menu_item_id' => $mealA->id, 'quantity' => 1],
                ['menu_item_id' => $mealB->id, 'quantity' => 1],
            ],
            'payment_method' => 'WALLET',
        ], [
            'Idempotency-Key' => 'checkout-cancel-split-001',
        ])->assertOk()->assertJsonPath('success', true);

        $sessionId = $checkout->json('checkout_session.id');
        $orders = Order::withoutGlobalScopes()->where('checkout_session_id', $sessionId)->orderBy('vendor_id')->get();
        $payment = Payment::where('checkout_session_id', $sessionId)->firstOrFail();

        $this->assertSame(60.0, (float) User::find($customer->id)->balance);

        $first = $orders->firstWhere('vendor_id', $vendorA->id);
        $second = $orders->firstWhere('vendor_id', $vendorB->id);

        $this->postJson('/api/customer/orders/'.$first->id.'/cancel', [], [
            'Idempotency-Key' => 'cancel-split-a-001',
        ])->assertOk()->assertJsonPath('success', true);

        $this->assertSame(70.0, (float) User::find($customer->id)->balance);
        $this->assertSame('SUCCESS', Payment::find($payment->id)->status);
        $this->assertSame(10.0, (float) PaymentAllocation::where('payment_id', $payment->id)->where('order_id', $first->id)->value('refunded_amount'));
        $this->assertSame(0.0, (float) PaymentAllocation::where('payment_id', $payment->id)->where('order_id', $second->id)->value('refunded_amount'));
        $this->assertSame(10, (int) MenuItem::find($mealA->id)->current_stock);
        $this->assertSame(0, PromotionRedemption::count());

        $this->postJson('/api/customer/orders/'.$second->id.'/cancel', [], [
            'Idempotency-Key' => 'cancel-split-b-001',
        ])->assertOk()->assertJsonPath('success', true);

        $this->assertSame(100.0, (float) User::find($customer->id)->balance);
        $this->assertSame('REFUNDED', Payment::find($payment->id)->status);
        $this->assertSame(30.0, (float) PaymentAllocation::where('payment_id', $payment->id)->where('order_id', $second->id)->value('refunded_amount'));
        $this->assertSame(10, (int) MenuItem::find($mealB->id)->current_stock);
    }

    public function test_loyalty_points_are_allocated_to_vendor_orders_and_restored_on_cancellation(): void
    {
        $customer = User::factory()->create([
            'role' => 'STUDENT',
            'account_status' => 'ACTIVE',
            'balance' => 100,
            'loyalty_points' => 10,
        ]);
        $vendorA = User::factory()->create(['role' => 'VENDOR', 'account_status' => 'ACTIVE']);
        $vendorB = User::factory()->create(['role' => 'VENDOR', 'account_status' => 'ACTIVE']);

        $mealA = MenuItem::create([
            'vendor_id' => $vendorA->id,
            'food_name' => 'A',
            'name' => 'A',
            'price' => 10,
            'description' => 'A.',
            'category' => 'Meals',
            'is_available' => true,
            'initial_stock' => 5,
            'current_stock' => 5,
            'low_stock_threshold' => 1,
        ]);
        $mealB = MenuItem::create([
            'vendor_id' => $vendorB->id,
            'food_name' => 'B',
            'name' => 'B',
            'price' => 30,
            'description' => 'B.',
            'category' => 'Meals',
            'is_available' => true,
            'initial_stock' => 5,
            'current_stock' => 5,
            'low_stock_threshold' => 1,
        ]);

        Sanctum::actingAs($customer, ['customer']);

        $checkout = $this->postJson('/api/customer/cart-checkout', [
            'items' => [
                ['menu_item_id' => $mealA->id, 'quantity' => 1],
                ['menu_item_id' => $mealB->id, 'quantity' => 1],
            ],
            'points_to_redeem' => 10,
            'payment_method' => 'WALLET',
        ], [
            'Idempotency-Key' => 'checkout-points-split-001',
        ])->assertOk()->assertJsonPath('success', true);

        $sessionId = $checkout->json('checkout_session.id');
        $orders = Order::withoutGlobalScopes()->where('checkout_session_id', $sessionId)->get();
        $first = $orders->firstWhere('vendor_id', $vendorA->id);
        $second = $orders->firstWhere('vendor_id', $vendorB->id);

        $this->assertSame(2, (int) $first->points_redeemed);
        $this->assertSame(8, (int) $second->points_redeemed);
        $this->assertSame(0, (int) User::find($customer->id)->loyalty_points);
        $this->assertSame(64.0, (float) User::find($customer->id)->balance);

        $this->postJson('/api/customer/orders/'.$first->id.'/cancel', [], [
            'Idempotency-Key' => 'cancel-points-a-001',
        ])->assertOk();

        $this->assertSame(2, (int) User::find($customer->id)->loyalty_points);

        $this->postJson('/api/customer/orders/'.$second->id.'/cancel', [], [
            'Idempotency-Key' => 'cancel-points-b-001',
        ])->assertOk();

        $this->assertSame(10, (int) User::find($customer->id)->loyalty_points);
        $this->assertSame(100.0, (float) User::find($customer->id)->balance);
    }

    public function test_global_promotion_is_redeemed_once_per_split_checkout_and_released_after_full_wallet_cancellation(): void
    {
        $customer = User::factory()->create([
            'role' => 'STUDENT',
            'account_status' => 'ACTIVE',
            'balance' => 100,
            'loyalty_points' => 0,
        ]);
        $vendorA = User::factory()->create(['role' => 'VENDOR', 'account_status' => 'ACTIVE']);
        $vendorB = User::factory()->create(['role' => 'VENDOR', 'account_status' => 'ACTIVE']);

        $mealA = MenuItem::create([
            'vendor_id' => $vendorA->id,
            'food_name' => 'Promo A',
            'name' => 'Promo A',
            'price' => 10,
            'description' => 'A.',
            'category' => 'Meals',
            'is_available' => true,
            'initial_stock' => 5,
            'current_stock' => 5,
            'low_stock_threshold' => 1,
        ]);
        $mealB = MenuItem::create([
            'vendor_id' => $vendorB->id,
            'food_name' => 'Promo B',
            'name' => 'Promo B',
            'price' => 30,
            'description' => 'B.',
            'category' => 'Meals',
            'is_available' => true,
            'initial_stock' => 5,
            'current_stock' => 5,
            'low_stock_threshold' => 1,
        ]);

        $promotion = Promotion::create([
            'code' => 'CAMPUS10',
            'name' => 'Campus Ten',
            'type' => 'PERCENTAGE',
            'value' => 10,
            'minimum_order_amount' => 0,
            'usage_limit' => 1,
            'per_customer_limit' => 1,
            'is_active' => true,
        ]);

        Sanctum::actingAs($customer, ['customer']);

        $checkout = $this->postJson('/api/customer/cart-checkout', [
            'items' => [
                ['menu_item_id' => $mealA->id, 'quantity' => 1],
                ['menu_item_id' => $mealB->id, 'quantity' => 1],
            ],
            'promotion_code' => $promotion->code,
            'payment_method' => 'WALLET',
        ], [
            'Idempotency-Key' => 'checkout-promo-split-001',
        ])->assertOk()->assertJsonPath('success', true);

        $sessionId = $checkout->json('checkout_session.id');
        $orders = Order::withoutGlobalScopes()->where('checkout_session_id', $sessionId)->get();
        $this->assertCount(2, $orders);
        $this->assertCount(1, PromotionRedemption::where('checkout_session_id', $sessionId)->get());
        $this->assertSame(4.0, (float) PromotionRedemption::where('checkout_session_id', $sessionId)->value('discount_amount'));
        $this->assertSame(4.0, (float) $orders->sum(fn ($order) => (float) $order->discount_amount));

        $first = $orders->firstWhere('vendor_id', $vendorA->id);
        $second = $orders->firstWhere('vendor_id', $vendorB->id);

        $this->postJson('/api/customer/orders/'.$first->id.'/cancel', [], [
            'Idempotency-Key' => 'cancel-promo-a-001',
        ])->assertOk();
        $this->assertSame(1, PromotionRedemption::where('checkout_session_id', $sessionId)->count());

        $this->postJson('/api/customer/orders/'.$second->id.'/cancel', [], [
            'Idempotency-Key' => 'cancel-promo-b-001',
        ])->assertOk();
        $this->assertSame(0, PromotionRedemption::where('checkout_session_id', $sessionId)->count());

        $this->assertSame(1, (int) $promotion->fresh()->usage_limit);
    }

}
