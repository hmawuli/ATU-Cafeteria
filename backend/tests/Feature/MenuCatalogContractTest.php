<?php

namespace Tests\Feature;

use App\Models\MenuItem;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class MenuCatalogContractTest extends TestCase
{
    use RefreshDatabase;

    public function test_catalog_menu_items_uses_production_envelope(): void
    {
        $vendor = User::create([
            'username' => 'contract-vendor',
            'password' => hash('sha256', '1111'),
            'role' => 'VENDOR',
            'fullName' => 'Contract Vendor',
            'info' => 'Contract Kitchen',
            'is_open' => true,
        ]);

        MenuItem::create([
            'vendor_id' => $vendor->id,
            'name' => 'Contract Jollof',
            'food_name' => 'Contract Jollof',
            'price' => 25.00,
            'category' => 'Ghanaian Local Dishes',
            'is_available' => true,
            'initial_stock' => 10,
            'current_stock' => 10,
        ]);

        $this->getJson('/api/catalog/menu-items')
            ->assertOk()
            ->assertJsonPath('success', true)
            ->assertJsonStructure(['menu_items' => [['id', 'name', 'price']]]);
    }

    public function test_catalog_food_items_uses_standard_data_envelope(): void
    {
        $this->getJson('/api/catalog/food-items')
            ->assertOk()
            ->assertJsonPath('success', true)
            ->assertJsonStructure(['data']);

        $body = $this->getJson('/api/catalog/food-items')->json();
        $this->assertIsArray($body['data']);
    }
}
