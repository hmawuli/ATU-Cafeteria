<?php

namespace Tests\Feature;

use App\Models\MenuItem;
use App\Models\Order;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Laravel\Sanctum\Sanctum;
use Tests\TestCase;

class OrderPlacementTest extends TestCase
{
    use RefreshDatabase;

    protected $student;

    protected $vendor;

    protected $menuItem;

    protected function setUp(): void
    {
        parent::setUp();

        // Create student user
        $this->student = User::create([
            'username' => 'teststudent',
            'password' => hash('sha256', '1234'), // SHA-256 pre-hashed
            'role' => 'STUDENT',
            'fullName' => 'Test Student',
            'info' => 'ATU-2024-TEST',
            'balance' => 100.00,
            'loyalty_points' => 50,
            'total_spent' => 0.00,
        ]);

        // Create vendor user
        $this->vendor = User::create([
            'username' => 'testvendor',
            'password' => hash('sha256', '1111'),
            'role' => 'VENDOR',
            'fullName' => 'Test Vendor Brand',
            'info' => 'Indomie & Rice Stand',
            'balance' => 0.00,
            'is_open' => true,
        ]);

        // Create menu item
        $this->menuItem = MenuItem::create([
            'vendor_id' => $this->vendor->id,
            'name' => 'Jollof Delight',
            'food_name' => 'Jollof Delight',
            'price' => 20.00,
            'description' => 'Delicious jollof rice with egg and plantain.',
            'category' => 'Rice Dishes',
            'is_available' => true,
            'initial_stock' => 10,
            'current_stock' => 10,
        ]);
    }

    /**
     * Test successful authenticated order placement.
     */
    public function test_student_can_place_order_successfully()
    {
        Sanctum::actingAs($this->student, ['student']);

        $response = $this->postJson('/api/v1/student/orders', [
            'vendor_id' => $this->vendor->id,
            'menu_item_id' => $this->menuItem->id,
            'food_name' => $this->menuItem->name,
            'quantity' => 2,
            'unit_price' => 20.00,
            'total_price' => 40.00,
            'estimated_pickup_time' => '15 mins',
        ]);

        $response->assertStatus(201);
        $response->assertJsonPath('success', true);
        $response->assertJsonStructure([
            'success',
            'message',
            'order' => [
                'id',
                'customer_id',
                'vendor_id',
                'menu_item_id',
                'food_name',
                'quantity',
                'total_price',
                'status',
                'pickup_pin',
            ],
        ]);

        // Verify balance was deducted
        $this->student->refresh();
        $this->assertEquals(60.00, $this->student->balance);

        // Verify stock was decremented
        $this->menuItem->refresh();
        $this->assertEquals(8, $this->menuItem->current_stock);

        // Verify order saved in database
        $this->assertDatabaseHas('orders', [
            'customer_id' => $this->student->id,
            'vendor_id' => $this->vendor->id,
            'menu_item_id' => $this->menuItem->id,
            'total_price' => 40.00,
            'status' => 'PENDING',
        ]);

        // Verify wallet transaction log created
        $this->assertDatabaseHas('wallet_transactions', [
            'user_id' => $this->student->id,
            'type' => 'PAYMENT',
            'amount' => -40.00,
            'status' => 'SUCCESS',
        ]);

        // Verify audit log created
        $this->assertDatabaseHas('audit_logs', [
            'user_id' => $this->student->id,
            'action' => 'ORDER_CREATED',
        ]);
    }

    /**
     * Test order placement validation when required fields are missing.
     */
    public function test_order_placement_fails_if_required_fields_missing()
    {
        Sanctum::actingAs($this->student, ['student']);

        $response = $this->postJson('/api/v1/student/orders', []);

        $response->assertStatus(400);
        $response->assertJsonPath('success', false);
        $response->assertJsonValidationErrors([
            'vendor_id',
            'menu_item_id',
            'food_name',
            'quantity',
            'unit_price',
            'total_price',
        ]);
    }

    /**
     * Test order placement fails if unit_price or total_price is wrong/mismatched.
     */
    public function test_order_placement_fails_if_price_mismatched()
    {
        Sanctum::actingAs($this->student, ['student']);

        // Wrong unit price
        $response = $this->postJson('/api/v1/student/orders', [
            'vendor_id' => $this->vendor->id,
            'menu_item_id' => $this->menuItem->id,
            'food_name' => $this->menuItem->name,
            'quantity' => 1,
            'unit_price' => 15.00, // actual is 20
            'total_price' => 20.00,
        ]);

        $response->assertStatus(400);
        $response->assertJsonPath('success', false);
        $response->assertJsonFragment([
            'message' => 'Validation error: unit_price does not match the actual menu item price.',
        ]);

        // Wrong total price
        $response2 = $this->postJson('/api/v1/student/orders', [
            'vendor_id' => $this->vendor->id,
            'menu_item_id' => $this->menuItem->id,
            'food_name' => $this->menuItem->name,
            'quantity' => 2,
            'unit_price' => 20.00,
            'total_price' => 30.00, // actual should be 40
        ]);

        $response2->assertStatus(400);
        $response2->assertJsonPath('success', false);
        $response2->assertJsonFragment([
            'message' => 'Validation error: total_price is incorrect based on menu item price and quantity.',
        ]);
    }

    /**
     * Test order placement fails if the menu item is out of stock.
     */
    public function test_order_placement_fails_if_insufficient_stock()
    {
        Sanctum::actingAs($this->student, ['student']);

        // Set stock to 1
        $this->menuItem->update(['current_stock' => 1]);

        $response = $this->postJson('/api/v1/student/orders', [
            'vendor_id' => $this->vendor->id,
            'menu_item_id' => $this->menuItem->id,
            'food_name' => $this->menuItem->name,
            'quantity' => 2, // requesting 2
            'unit_price' => 20.00,
            'total_price' => 40.00,
        ]);

        $response->assertStatus(500); // Because Db transaction throws exception and it triggers 500
        $response->assertJsonPath('success', false);
        $response->assertJsonFragment([
            'message' => 'Failed to place order securely on server.',
        ]);
    }

    /**
     * Test order placement fails if customer has insufficient balance.
     */
    public function test_order_placement_fails_if_insufficient_balance()
    {
        Sanctum::actingAs($this->student, ['student']);

        // Set student balance to 10
        $this->student->update(['balance' => 10.00]);

        $response = $this->postJson('/api/v1/student/orders', [
            'vendor_id' => $this->vendor->id,
            'menu_item_id' => $this->menuItem->id,
            'food_name' => $this->menuItem->name,
            'quantity' => 1,
            'unit_price' => 20.00,
            'total_price' => 20.00,
        ]);

        $response->assertStatus(400);
        $response->assertJsonPath('success', false);
        $response->assertJsonFragment([
            'message' => 'Insufficient wallet balance. Please top up your wallet first.',
        ]);
    }

    /**
     * Test order placement fails if menu item is unavailable.
     */
    public function test_order_placement_fails_if_menu_item_unavailable()
    {
        Sanctum::actingAs($this->student, ['student']);

        $this->menuItem->update(['is_available' => false]);

        $response = $this->postJson('/api/v1/student/orders', [
            'vendor_id' => $this->vendor->id,
            'menu_item_id' => $this->menuItem->id,
            'food_name' => $this->menuItem->name,
            'quantity' => 1,
            'unit_price' => 20.00,
            'total_price' => 20.00,
        ]);

        $response->assertStatus(400);
        $response->assertJsonPath('success', false);
        $response->assertJsonFragment([
            'message' => 'The selected menu item is currently unavailable.',
        ]);
    }
}
