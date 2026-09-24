<?php

namespace Tests\Feature;

use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Laravel\Sanctum\Sanctum;
use Tests\TestCase;

class VendorFinanceTest extends TestCase
{
    use RefreshDatabase;

    public function test_vendor_can_view_only_vendor_finance_resource(): void
    {
        $vendor = User::factory()->create([
            'role' => 'VENDOR',
            'account_status' => 'ACTIVE',
        ]);

        Sanctum::actingAs($vendor, ['vendor']);

        $this->getJson('/api/vendor/finance?days=30')
            ->assertOk()
            ->assertJsonPath('success', true)
            ->assertJsonPath('vendor.id', $vendor->id)
            ->assertJsonPath('period.days', 30)
            ->assertJsonStructure([
                'summary' => [
                    'gross_food_sales',
                    'discounts',
                    'refunds',
                    'net_food_sales',
                    'customer_collected',
                    'completed_orders',
                    'average_order_value',
                    'pending_settlement',
                    'settled_amount',
                ],
                'trend',
                'transactions',
                'settlements',
            ]);
    }

    public function test_customer_cannot_view_vendor_finance(): void
    {
        $customer = User::factory()->create([
            'role' => 'STUDENT',
            'account_status' => 'ACTIVE',
        ]);

        Sanctum::actingAs($customer, ['customer']);

        $this->getJson('/api/vendor/finance')
            ->assertForbidden();
    }
}
