<?php

namespace Tests\Feature;

use App\Models\MenuItem;
use App\Models\Order;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Laravel\Sanctum\Sanctum;
use Tests\TestCase;

class GeminiRecommendationTest extends TestCase
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
            'username' => 'geministudent',
            'password' => hash('sha256', '1234'),
            'role' => 'STUDENT',
            'fullName' => 'Gemini Student Tester',
            'info' => 'ATU-2024-GEMINI',
            'balance' => 250.00,
            'loyalty_points' => 120,
            'dietaryPreferences' => 'Vegan, Gluten-Free',
        ]);

        // Create vendor user
        $this->vendor = User::create([
            'username' => 'geminivendor',
            'password' => hash('sha256', '1111'),
            'role' => 'VENDOR',
            'fullName' => 'ATU Main Cafeteria Stand',
            'info' => 'Quality Meal Hub',
            'balance' => 500.00,
            'is_open' => true,
        ]);

        // Create sample menu item
        $this->menuItem = MenuItem::create([
            'vendor_id' => $this->vendor->id,
            'name' => 'Vegetable Fried Rice & Plantain',
            'food_name' => 'Vegetable Fried Rice & Plantain',
            'price' => 25.00,
            'description' => 'Hearty vegetable fried rice served with fried plantain slices.',
            'category' => 'Rice Dishes',
            'is_available' => true,
            'initial_stock' => 50,
            'current_stock' => 45,
        ]);

        // Create a completed order history for analytics
        Order::create([
            'customer_id' => $this->student->id,
            'vendor_id' => $this->vendor->id,
            'menu_item_id' => $this->menuItem->id,
            'food_name' => $this->menuItem->name,
            'quantity' => 2,
            'total_price' => 50.00,
            'status' => 'COMPLETED',
            'pickup_pin' => '7890',
            'order_timestamp' => now()->subDays(2),
        ]);
    }

    /**
     * Test unauthenticated access to student Gemini budget analytics fails with 401.
     */
    public function test_unauthenticated_user_cannot_access_student_budget_analytics()
    {
        $response = $this->getJson('/api/v1/student/budget/analytics');
        $response->assertStatus(401);
    }

    /**
     * Test authenticated student receives Gemini budget analytics and recommendations.
     */
    public function test_student_receives_gemini_budget_analytics_and_recommendations()
    {
        Sanctum::actingAs($this->student, ['student']);

        $response = $this->getJson('/api/v1/student/budget/analytics');

        $response->assertStatus(200);
        $response->assertJsonPath('success', true);
        $response->assertJsonStructure([
            'success',
            'student_id',
            'student_name',
            'overall_metrics' => [
                'total_spent_all_time',
                'total_orders_count',
                'current_month_spent',
                'monthly_budget_limit',
                'budget_consumption_percentage',
                'remaining_budget'
            ],
            'monthly_chart_data',
            'category_chart_data',
            'daily_trend_chart_data',
            'budget_advice',
            'generated_at'
        ]);

        $this->assertNotEmpty($response->json('budget_advice'));
    }

    /**
     * Test vendor can generate Gemini performance report and actionable operational recommendations.
     */
    public function test_vendor_can_fetch_gemini_performance_report()
    {
        Sanctum::actingAs($this->vendor, ['vendor']);

        $response = $this->getJson('/api/v1/vendor/analytics/gemini-report');

        $response->assertStatus(200);
        $response->assertJsonPath('success', true);
        $response->assertJsonStructure([
            'success',
            'vendor_id',
            'vendor_name',
            'gemini_response',
            'metrics_summary',
            'generated_at'
        ]);

        $this->assertNotEmpty($response->json('gemini_response'));
    }

    /**
     * Test vendor can fetch Gemini order insights on peak traffic and popular items.
     */
    public function test_vendor_can_fetch_gemini_order_insights()
    {
        Sanctum::actingAs($this->vendor, ['vendor']);

        $response = $this->getJson('/api/v1/vendor/analytics/gemini-order-insights');

        $response->assertStatus(200);
        $response->assertJsonPath('success', true);
        $response->assertJsonStructure([
            'success',
            'vendor_id',
            'vendor_name',
            'gemini_insights',
            'analytics_summary',
            'generated_at'
        ]);

        $this->assertNotEmpty($response->json('gemini_insights'));
    }
}
