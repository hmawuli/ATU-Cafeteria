<?php

namespace Tests\Feature;

use App\Models\InventoryMovement;
use App\Models\MenuItem;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Laravel\Sanctum\Sanctum;
use Tests\TestCase;

class VendorInventoryOperationsTest extends TestCase
{
    use RefreshDatabase;

    public function test_vendor_can_create_stock_tracked_menu_item_and_adjust_inventory(): void
    {
        $vendor = User::factory()->create([
            'role' => 'VENDOR',
            'account_status' => 'ACTIVE',
        ]);

        Sanctum::actingAs($vendor, ['vendor']);

        $create = $this->postJson('/api/vendor/menu-items', [
            'food_name' => 'Jollof Rice',
            'price' => 25,
            'category' => 'Meals',
            'description' => 'Fresh jollof rice.',
            'is_available' => true,
            'initial_stock' => 20,
            'low_stock_threshold' => 5,
        ], [
            'Idempotency-Key' => 'vendor-menu-stock-create-001',
        ]);

        $create->assertCreated()->assertJsonPath('success', true);

        $itemId = $create->json('menu_item.id');
        $this->assertSame(20, (int) MenuItem::findOrFail($itemId)->current_stock);
        $this->assertDatabaseHas('inventory_movements', [
            'menu_item_id' => $itemId,
            'vendor_id' => $vendor->id,
            'quantity' => 20,
            'balance_after' => 20,
            'type' => 'RESTOCK',
        ]);

        $this->postJson('/api/vendor/inventory/adjust', [
            'menu_item_id' => $itemId,
            'quantity' => -3,
            'reason' => 'Damaged stock removed.',
        ], [
            'Idempotency-Key' => 'vendor-inventory-adjust-001',
        ])->assertOk()->assertJsonPath('success', true);

        $this->assertSame(17, (int) MenuItem::findOrFail($itemId)->current_stock);
        $this->assertDatabaseHas('inventory_movements', [
            'menu_item_id' => $itemId,
            'vendor_id' => $vendor->id,
            'quantity' => -3,
            'balance_after' => 17,
            'type' => 'ADJUSTMENT',
        ]);

        $this->assertSame(2, InventoryMovement::where('menu_item_id', $itemId)->count());
    }

    public function test_vendor_cannot_adjust_another_vendors_inventory(): void
    {
        $owner = User::factory()->create([
            'role' => 'VENDOR',
            'account_status' => 'ACTIVE',
        ]);
        $attacker = User::factory()->create([
            'role' => 'VENDOR',
            'account_status' => 'ACTIVE',
        ]);

        $item = MenuItem::create([
            'vendor_id' => $owner->id,
            'food_name' => 'Rice',
            'name' => 'Rice',
            'price' => 10,
            'category' => 'Meals',
            'is_available' => true,
            'initial_stock' => 10,
            'current_stock' => 10,
            'low_stock_threshold' => 2,
        ]);

        Sanctum::actingAs($attacker, ['vendor']);

        $this->postJson('/api/vendor/inventory/adjust', [
            'menu_item_id' => $item->id,
            'quantity' => 5,
        ], [
            'Idempotency-Key' => 'vendor-inventory-forbidden-001',
        ])->assertForbidden();

        $this->assertSame(10, (int) MenuItem::findOrFail($item->id)->current_stock);
    }
}
