<?php

namespace Tests\Feature;

use App\Models\MenuItem;
use App\Models\Order;
use App\Models\User;
use App\Models\Vendor;
use App\Models\WalletTransaction;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Laravel\Sanctum\Sanctum;
use Tests\TestCase;

class StandoutFeaturesTest extends TestCase
{
    use RefreshDatabase;

    protected User $vendor;

    protected User $student;

    protected MenuItem $item;

    protected function setUp(): void
    {
        parent::setUp();

        $this->vendor = User::create([
            'username' => 'standout-vendor',
            'password' => hash('sha256', '1111'),
            'role' => 'VENDOR',
            'fullName' => 'Standout Kitchen',
            'info' => 'Standout Stall',
            'is_open' => true,
        ]);

        Vendor::create([
            'user_id' => $this->vendor->id,
            'name' => 'Standout Kitchen',
            'store_name' => 'Standout Kitchen',
            'is_active' => true,
            'campus' => 'Accra Technical University',
        ]);

        $this->item = MenuItem::create([
            'vendor_id' => $this->vendor->id,
            'name' => 'Standout Jollof',
            'food_name' => 'Standout Jollof',
            'price' => 30.00,
            'category' => 'Ghanaian Local Dishes',
            'is_available' => true,
            'initial_stock' => 20,
            'current_stock' => 20,
            'dietary_tags' => ['vegetarian'],
            'allergen_info' => 'May contain legumes.',
            'is_featured' => true,
        ]);

        $this->student = User::create([
            'username' => 'standout-student',
            'password' => hash('sha256', '1234'),
            'role' => 'STUDENT',
            'fullName' => 'Standout Student',
            'info' => 'ATU-2026-S00',
            'balance' => 100.00,
        ]);
    }

    private function actingWithToken(User $user): void
    {
        $token = $user->createToken('standout-test')->plainTextToken;
        $this->withHeader('Authorization', 'Bearer '.$token);
    }

    public function test_public_stall_directory_lists_open_stalls(): void
    {
        $this->getJson('/api/public/stalls')
            ->assertOk()
            ->assertJsonPath('success', true)
            ->assertJsonStructure(['data' => [['id', 'store_name', 'is_open', 'campus', 'open_menu_items']]])
            ->assertJsonFragment(['store_name' => 'Standout Kitchen', 'is_open' => true]);
    }

    public function test_public_stall_directory_filters_by_campus(): void
    {
        $this->getJson('/api/public/stalls?campus=Other Campus')
            ->assertOk()
            ->assertJsonPath('data', []);
    }

    public function test_stall_menu_returns_health_badges(): void
    {
        $this->getJson('/api/stalls/'.$this->vendor->id)
            ->assertOk()
            ->assertJsonPath('success', true)
            ->assertJsonStructure(['data' => ['stall' => ['id', 'name', 'is_open', 'campus'], 'menu_items' => []]])
            ->assertJsonFragment([
                'name' => 'Standout Jollof',
                'badges' => ['VEGETARIAN', 'GLUTEN-FREE', 'FEATURED'],
            ]);
    }

    public function test_catalogue_includes_derived_badges(): void
    {
        $this->getJson('/api/catalog/menu-items')
            ->assertOk()
            ->assertJsonFragment(['id' => $this->item->id, 'badges' => ['VEGETARIAN', 'GLUTEN-FREE', 'FEATURED']]);
    }

    public function test_owner_can_schedule_a_pickup(): void
    {
        $order = Order::create([
            'customer_id' => $this->student->id,
            'vendor_id' => $this->vendor->id,
            'menu_item_id' => $this->item->id,
            'food_name' => 'Standout Jollof',
            'quantity' => 1,
            'unit_price' => 30.00,
            'total_price' => 30.00,
            'order_timestamp' => time() * 1000,
            'status' => 'ORDER_PLACED',
            'pickup_pin' => '1111',
        ]);

        $this->actingWithToken($this->student);

        $this->putJson("/api/orders/{$order->id}/schedule", [
            'pickup_at' => now()->addMinutes(15)->toDateTimeString(),
        ])
            ->assertOk()
            ->assertJsonPath('success', true);

        $this->assertNotNull($order->fresh()->scheduled_pickup_at);
        $this->assertDatabaseHas('audit_logs', ['action' => 'ORDER_SCHEDULED']);
    }

    public function test_receipt_endpoints_return_json_and_pdf(): void
    {
        $order = Order::create([
            'customer_id' => $this->student->id,
            'vendor_id' => $this->vendor->id,
            'menu_item_id' => $this->item->id,
            'food_name' => 'Standout Jollof',
            'quantity' => 1,
            'unit_price' => 30.00,
            'total_price' => 30.00,
            'order_timestamp' => time() * 1000,
            'status' => 'COMPLETED',
            'pickup_pin' => '1111',
        ]);

        WalletTransaction::create([
            'user_id' => $this->student->id,
            'order_id' => $order->id,
            'type' => 'PAYMENT',
            'amount' => -30.00,
            'status' => 'SUCCESS',
            'reference' => 'receipt-'.uniqid(),
        ]);

        $this->actingWithToken($this->student);

        $this->getJson("/api/orders/{$order->id}/receipt")
            ->assertOk()
            ->assertJsonPath('success', true)
            ->assertJsonStructure(['data' => ['order', 'vendor_name', 'transactions']]);

        $this->get("/api/orders/{$order->id}/pdf")
            ->assertOk()
            ->assertHeader('Content-Type', 'application/pdf');
    }

    public function test_other_student_cannot_read_receipt(): void
    {
        $order = Order::create([
            'customer_id' => $this->student->id,
            'vendor_id' => $this->vendor->id,
            'menu_item_id' => $this->item->id,
            'food_name' => 'Standout Jollof',
            'quantity' => 1,
            'unit_price' => 30.00,
            'total_price' => 30.00,
            'order_timestamp' => time() * 1000,
            'status' => 'ORDER_PLACED',
            'pickup_pin' => '1111',
        ]);

        $other = User::create([
            'username' => 'standout-other',
            'password' => hash('sha256', '1234'),
            'role' => 'STUDENT',
            'fullName' => 'Other Student',
            'info' => 'ATU-2026-S01',
        ]);

        $this->actingWithToken($other);

        $this->getJson("/api/orders/{$order->id}/receipt")->assertNotFound();
    }

    public function test_open_now_board_serves_html_page(): void
    {
        $this->get('/stalls')
            ->assertOk()
            ->assertHeader('Content-Type', 'text/html; charset=UTF-8')
            ->assertSee('Standout Kitchen');
    }

    public function test_loyalty_summary_reports_streak_meta(): void
    {
        Order::create([
            'customer_id' => $this->student->id,
            'vendor_id' => $this->vendor->id,
            'menu_item_id' => $this->item->id,
            'food_name' => 'Standout Jollof',
            'quantity' => 1,
            'unit_price' => 30.00,
            'total_price' => 30.00,
            'order_timestamp' => time() * 1000,
            'status' => 'COMPLETED',
            'pickup_pin' => '1111',
            'created_at' => now(),
        ]);

        Sanctum::actingAs($this->student, ['*']);

        $this->getJson('/api/customer/loyalty/summary')
            ->assertOk()
            ->assertJsonStructure(['streak_days', 'milestone_target_days', 'days_to_next_milestone']);
    }
}
