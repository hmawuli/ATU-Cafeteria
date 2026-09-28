<?php

namespace Tests\Feature;

use App\Models\MenuItem;
use App\Models\User;
use App\Models\WalletTransaction;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Str;
use Laravel\Sanctum\Sanctum;
use Tests\TestCase;

class PaginationContractTest extends TestCase
{
    use RefreshDatabase;

    public function test_catalogue_accepts_optional_pagination_meta(): void
    {
        $vendor = User::create([
            'username' => 'page-vendor',
            'password' => hash('sha256', '1111'),
            'role' => 'VENDOR',
            'fullName' => 'Page Vendor',
            'is_open' => true,
        ]);

        foreach (['Jollof', 'Waakye', 'Fried Rice'] as $i => $name) {
            MenuItem::create([
                'vendor_id' => $vendor->id,
                'name' => $name,
                'food_name' => $name,
                'price' => 20.00 + $i,
                'category' => 'Dishes',
                'is_available' => true,
                'initial_stock' => 10,
                'current_stock' => 10,
            ]);
        }

        $response = $this->getJson('/api/catalog/menu-items?page=1&per_page=1')
            ->assertOk()
            ->assertJsonPath('success', true);

        $this->assertCount(1, $response->json('menu_items'));
        $this->assertSame(3, $response->json('pagination.total'));
        $this->assertSame(1, $response->json('pagination.per_page'));
        $this->assertSame(3, $response->json('pagination.last_page'));
    }

    public function test_catalogue_without_page_returns_full_list_no_meta(): void
    {
        $this->getJson('/api/catalog/menu-items')
            ->assertOk()
            ->assertJsonPath('success', true)
            ->assertJsonStructure(['menu_items' => []])
            ->assertJsonMissingPath('pagination');
    }

    public function test_wallet_index_returns_balance_ledger_and_pagination(): void
    {
        $user = User::factory()->create(['role' => 'STUDENT', 'balance' => 42.50]);

        for ($i = 0; $i < 3; $i++) {
            WalletTransaction::create([
                'user_id' => $user->id,
                'type' => 'PAYMENT',
                'amount' => -5.00,
                'status' => 'SUCCESS',
                'reference' => 'pg-test-'.$i.'-'.Str::uuid()->toString(),
                'details' => "paginated transaction {$i}",
            ]);
        }

        Sanctum::actingAs($user, ['*']);

        $this->getJson('/api/wallet?page=1&per_page=2')
            ->assertOk()
            ->assertJsonPath('success', true)
            ->assertJsonPath('balance', 42.5)
            ->assertJsonStructure([
                'transactions' => [],
                'pagination' => ['current_page', 'per_page', 'last_page', 'total'],
            ]);
    }
}
