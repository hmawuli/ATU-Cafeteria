<?php

namespace Database\Seeders;

use App\Models\InventoryMovement;
use App\Models\MenuItem;
use App\Models\Vendor;
use Illuminate\Database\Seeder;
use Illuminate\Support\Facades\DB;
use RuntimeException;

class DevelopmentRestaurantCatalogSeeder extends Seeder
{
    /**
     * Development restaurant catalog.
     *
     * This seeder is intentionally separate from DatabaseSeeder so that
     * demonstration products are never created accidentally in production.
     */
    public function run(): void
    {
        if (app()->environment('production')) {
            throw new RuntimeException('Development seeders must never run in production.');
        }
        DB::transaction(function (): void {
            $vendors = Vendor::query()
                ->whereIn('store_name', [
                    'Campus Delight',
                    'Quick Bites',
                ])
                ->get()
                ->keyBy('store_name');

            if ($vendors->count() !== 2) {
                throw new RuntimeException(
                    'Expected both development vendors: Campus Delight and Quick Bites.'
                );
            }

            $catalog = [
                [
                    'vendor' => 'Campus Delight',
                    'items' => [
                        [
                            'sku' => 'CD-BF-001',
                            'name' => 'Hausa Koko & Koose',
                            'category' => 'Breakfast',
                            'price' => 15.00,
                            'description' => 'Traditional Ghanaian millet porridge served with crispy koose.',
                            'stock' => 40,
                            'threshold' => 10,
                            'preparation_minutes' => 10,
                            'dietary_tags' => ['vegetarian'],
                            'allergen_info' => 'May contain legumes.',
                            'featured' => true,
                        ],
                        [
                            'sku' => 'CD-BF-002',
                            'name' => 'Tea & Bread',
                            'category' => 'Breakfast',
                            'price' => 12.00,
                            'description' => 'Hot tea served with fresh bread.',
                            'stock' => 50,
                            'threshold' => 10,
                            'preparation_minutes' => 5,
                            'dietary_tags' => ['vegetarian'],
                            'allergen_info' => 'Contains gluten and may contain milk.',
                            'featured' => false,
                        ],
                        [
                            'sku' => 'CD-LD-001',
                            'name' => 'Jollof Rice with Chicken',
                            'category' => 'Ghanaian Local Dishes',
                            'price' => 35.00,
                            'description' => 'Seasoned jollof rice served with grilled chicken and vegetables.',
                            'stock' => 35,
                            'threshold' => 8,
                            'preparation_minutes' => 20,
                            'dietary_tags' => [],
                            'allergen_info' => 'Prepared in a kitchen that handles common allergens.',
                            'featured' => true,
                        ],
                        [
                            'sku' => 'CD-LD-002',
                            'name' => 'Waakye with Chicken',
                            'category' => 'Ghanaian Local Dishes',
                            'price' => 38.00,
                            'description' => 'Ghanaian waakye served with chicken and traditional accompaniments.',
                            'stock' => 30,
                            'threshold' => 8,
                            'preparation_minutes' => 20,
                            'dietary_tags' => [],
                            'allergen_info' => 'May contain beans and other common allergens.',
                            'featured' => true,
                        ],
                        [
                            'sku' => 'CD-LD-003',
                            'name' => 'Fried Rice with Chicken',
                            'category' => 'Ghanaian Local Dishes',
                            'price' => 38.00,
                            'description' => 'Seasoned fried rice served with chicken and vegetables.',
                            'stock' => 30,
                            'threshold' => 8,
                            'preparation_minutes' => 20,
                            'dietary_tags' => [],
                            'allergen_info' => 'Prepared in a kitchen that handles common allergens.',
                            'featured' => false,
                        ],
                        [
                            'sku' => 'CD-BV-001',
                            'name' => 'Sobolo',
                            'category' => 'Beverages & Drinks',
                            'price' => 10.00,
                            'description' => 'Refreshing chilled hibiscus drink.',
                            'stock' => 60,
                            'threshold' => 15,
                            'preparation_minutes' => 2,
                            'dietary_tags' => ['vegan', 'vegetarian'],
                            'allergen_info' => null,
                            'featured' => false,
                        ],
                        [
                            'sku' => 'CD-BV-002',
                            'name' => 'Mango Juice',
                            'category' => 'Beverages & Drinks',
                            'price' => 15.00,
                            'description' => 'Refreshing mango fruit drink.',
                            'stock' => 45,
                            'threshold' => 10,
                            'preparation_minutes' => 3,
                            'dietary_tags' => ['vegan', 'vegetarian'],
                            'allergen_info' => null,
                            'featured' => false,
                        ],
                    ],
                ],

                [
                    'vendor' => 'Quick Bites',
                    'items' => [
                        [
                            'sku' => 'QB-PS-001',
                            'name' => 'Meat Pie',
                            'category' => 'Pastries & Snacks',
                            'price' => 12.00,
                            'description' => 'Golden baked pastry filled with seasoned minced meat.',
                            'stock' => 60,
                            'threshold' => 15,
                            'preparation_minutes' => 8,
                            'dietary_tags' => [],
                            'allergen_info' => 'Contains gluten and may contain egg and milk.',
                            'featured' => true,
                        ],
                        [
                            'sku' => 'QB-PS-002',
                            'name' => 'Sausage Roll',
                            'category' => 'Pastries & Snacks',
                            'price' => 10.00,
                            'description' => 'Crispy pastry wrapped around seasoned sausage.',
                            'stock' => 60,
                            'threshold' => 15,
                            'preparation_minutes' => 8,
                            'dietary_tags' => [],
                            'allergen_info' => 'Contains gluten and may contain egg and milk.',
                            'featured' => false,
                        ],
                        [
                            'sku' => 'QB-PS-003',
                            'name' => 'Chicken Pie',
                            'category' => 'Pastries & Snacks',
                            'price' => 15.00,
                            'description' => 'Baked pastry filled with seasoned chicken.',
                            'stock' => 50,
                            'threshold' => 12,
                            'preparation_minutes' => 10,
                            'dietary_tags' => [],
                            'allergen_info' => 'Contains gluten and may contain egg and milk.',
                            'featured' => true,
                        ],
                        [
                            'sku' => 'QB-BF-001',
                            'name' => 'Egg Sandwich',
                            'category' => 'Breakfast',
                            'price' => 18.00,
                            'description' => 'Fresh bread sandwich with seasoned egg filling.',
                            'stock' => 35,
                            'threshold' => 8,
                            'preparation_minutes' => 7,
                            'dietary_tags' => ['vegetarian'],
                            'allergen_info' => 'Contains egg and gluten.',
                            'featured' => false,
                        ],
                        [
                            'sku' => 'QB-BV-001',
                            'name' => 'Bottled Water',
                            'category' => 'Beverages & Drinks',
                            'price' => 5.00,
                            'description' => 'Chilled bottled drinking water.',
                            'stock' => 100,
                            'threshold' => 20,
                            'preparation_minutes' => 1,
                            'dietary_tags' => ['vegan', 'vegetarian'],
                            'allergen_info' => null,
                            'featured' => false,
                        ],
                        [
                            'sku' => 'QB-BV-002',
                            'name' => 'Malt Drink',
                            'category' => 'Beverages & Drinks',
                            'price' => 12.00,
                            'description' => 'Chilled malt beverage.',
                            'stock' => 80,
                            'threshold' => 20,
                            'preparation_minutes' => 1,
                            'dietary_tags' => ['vegetarian'],
                            'allergen_info' => 'Check product label for ingredients and allergens.',
                            'featured' => false,
                        ],
                    ],
                ],
            ];

            foreach ($catalog as $vendorCatalog) {
                /** @var Vendor $vendor */
                $vendor = $vendors->get($vendorCatalog['vendor']);

                foreach ($vendorCatalog['items'] as $data) {
                    $existing = MenuItem::withTrashed()
                        ->where('sku', $data['sku'])
                        ->first();

                    if ($existing) {
                        $existing->restore();

                        $existing->update([
                            'vendor_id' => $vendor->user_id,
                            'food_name' => $data['name'],
                            'name' => $data['name'],
                            'price' => $data['price'],
                            'description' => $data['description'],
                            'category' => $data['category'],
                            'is_available' => true,
                            'initial_stock' => $data['stock'],
                            'current_stock' => $data['stock'],
                            'low_stock_threshold' => $data['threshold'],
                            'preparation_minutes' => $data['preparation_minutes'],
                            'dietary_tags' => $data['dietary_tags'],
                            'allergen_info' => $data['allergen_info'],
                            'is_featured' => $data['featured'],
                        ]);

                        continue;
                    }

                    $menuItem = MenuItem::create([
                        'vendor_id' => $vendor->user_id,
                        'food_name' => $data['name'],
                        'name' => $data['name'],
                        'price' => $data['price'],
                        'description' => $data['description'],
                        'category' => $data['category'],
                        'is_available' => true,
                        'initial_stock' => $data['stock'],
                        'current_stock' => $data['stock'],
                        'low_stock_threshold' => $data['threshold'],
                        'sku' => $data['sku'],
                        'preparation_minutes' => $data['preparation_minutes'],
                        'dietary_tags' => $data['dietary_tags'],
                        'allergen_info' => $data['allergen_info'],
                        'is_featured' => $data['featured'],
                    ]);

                    InventoryMovement::create([
                        'vendor_id' => $vendor->user_id,
                        'menu_item_id' => $menuItem->id,
                        'food_item_id' => null,
                        'order_id' => null,
                        'type' => 'OPENING',
                        'quantity' => $data['stock'],
                        'balance_after' => $data['stock'],
                        'reference' => 'DEV-OPENING-'.$data['sku'],
                        'reason' => 'Development catalog opening stock.',
                        'performed_by' => $vendor->user_id,
                    ]);
                }
            }
        });

        $this->command?->info('Development restaurant catalog seeded successfully.');
    }
}
