<?php

namespace Tests\Feature;

use App\Models\MenuItem;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class CatalogueCacheTest extends TestCase
{
    use RefreshDatabase;

    public function test_catalogue_cache_invalidates_when_a_menu_item_is_added(): void
    {
        $vendor = User::create([
            'username' => 'cache-vendor',
            'password' => hash('sha256', '1111'),
            'role' => 'VENDOR',
            'fullName' => 'Cache Vendor',
            'info' => 'Cache Kitchen',
            'is_open' => true,
        ]);

        // Seed the cache with an empty catalogue.
        $this->getJson('/api/catalog/menu-items')->assertOk();

        // A new menu item must invalidate the cache.
        MenuItem::create([
            'vendor_id' => $vendor->id,
            'name' => 'Freshly Cached Jollof',
            'food_name' => 'Freshly Cached Jollof',
            'price' => 25.00,
            'category' => 'Ghanaian Local Dishes',
            'is_available' => true,
            'initial_stock' => 10,
            'current_stock' => 10,
        ]);

        $this->getJson('/api/catalog/menu-items')
            ->assertOk()
            ->assertJsonFragment([
                'name' => 'Freshly Cached Jollof',
                'badges' => ['GLUTEN-FREE'],
            ]);
    }

    public function test_catalogue_updates_after_availability_change(): void
    {
        $vendor = User::create([
            'username' => 'cache-vendor-2',
            'password' => hash('sha256', '1111'),
            'role' => 'VENDOR',
            'fullName' => 'Cache Vendor 2',
            'info' => 'Cache Kitchen 2',
            'is_open' => true,
        ]);

        $item = MenuItem::create([
            'vendor_id' => $vendor->id,
            'name' => 'Cache Waakye',
            'food_name' => 'Cache Waakye',
            'price' => 22.00,
            'category' => 'Ghanaian Local Dishes',
            'is_available' => true,
            'initial_stock' => 5,
            'current_stock' => 5,
        ]);

        $this->getJson('/api/catalog/menu-items')
            ->assertOk()
            ->assertJsonFragment(['id' => $item->id, 'is_available' => true]);

        // Toggle unavailable — the cached response must refresh.
        $item->update(['is_available' => false]);

        $this->getJson('/api/catalog/menu-items')
            ->assertOk()
            ->assertJsonFragment(['id' => $item->id, 'is_available' => false]);
    }
}
