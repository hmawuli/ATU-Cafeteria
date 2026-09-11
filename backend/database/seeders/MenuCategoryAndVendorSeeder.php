<?php

namespace Database\Seeders;

use Illuminate\Database\Seeder;
use Illuminate\Support\Str;
use App\Models\User;
use App\Models\Vendor;
use Illuminate\Support\Facades\DB;

class MenuCategoryAndVendorSeeder extends Seeder
{
    /**
     * Run the database seeds.
     */
    public function run(): void
    {
        // 1. Seed Menu Categories
        $categories = [
            [
                'name' => 'Breakfast',
                'slug' => 'breakfast',
                'description' => 'Start your day with hot tea, porridge, bread, and eggs.',
                'is_active' => true,
                'sort_order' => 1,
            ],
            [
                'name' => 'Ghanaian Local Dishes',
                'slug' => 'ghanaian-local-dishes',
                'description' => 'Authentic Ghanaian food including Waakye, Jollof, Fufu, and Banku.',
                'is_active' => true,
                'sort_order' => 2,
            ],
            [
                'name' => 'Pastries & Snacks',
                'slug' => 'pastries-snacks',
                'description' => 'Flaky meat pies, cakes, sausage rolls, and chips.',
                'is_active' => true,
                'sort_order' => 3,
            ],
            [
                'name' => 'Beverages & Drinks',
                'slug' => 'beverages-drinks',
                'description' => 'Chilled drinks, fresh juices, Sobolo, and water.',
                'is_active' => true,
                'sort_order' => 4,
            ],
        ];

        foreach ($categories as $cat) {
            DB::table('menu_categories')->updateOrInsert(
                ['slug' => $cat['slug']],
                [
                    'name' => $cat['name'],
                    'description' => $cat['description'],
                    'is_active' => $cat['is_active'],
                    'sort_order' => $cat['sort_order'],
                    'created_at' => now(),
                    'updated_at' => now(),
                ]
            );
        }

        // 2. Seed Test Vendor Users and Vendor Profiles
        $vendorHash = hash('sha256', 'vendor123');
        
        $vendorUsers = [
            [
                'id' => 20,
                'username' => 'testvendor1',
                'password' => $vendorHash,
                'role' => 'VENDOR',
                'fullName' => 'Campus Delight Kitchen',
                'student_staff_id' => 'ATU-VND-020',
                'profile_info' => [
                    'outlet_name' => 'Campus Delight',
                    'location' => 'Block F Annex Booth 2',
                    'telephone' => '+233 24 555 7777',
                    'primary_category' => 'Local Dishes'
                ],
                'info' => 'Campus Delight',
            ],
            [
                'id' => 21,
                'username' => 'testvendor2',
                'password' => $vendorHash,
                'role' => 'VENDOR',
                'fullName' => 'Quick Bites Bakery',
                'student_staff_id' => 'ATU-VND-021',
                'profile_info' => [
                    'outlet_name' => 'Quick Bites',
                    'location' => 'Main Gate Kiosk B',
                    'telephone' => '+233 20 888 9999',
                    'primary_category' => 'Snacks & Drinks'
                ],
                'info' => 'Quick Bites',
            ],
        ];

        foreach ($vendorUsers as $vUser) {
            DB::table('users')->updateOrInsert(
                ['id' => $vUser['id']],
                [
                    'username' => $vUser['username'],
                    'password' => $vUser['password'],
                    'role' => $vUser['role'],
                    'fullName' => $vUser['fullName'],
                    'student_staff_id' => $vUser['student_staff_id'],
                    'profile_info' => json_encode($vUser['profile_info']),
                    'info' => $vUser['info'],
                    'created_at' => now(),
                    'updated_at' => now(),
                ]
            );

            DB::table('vendors')->updateOrInsert(
                ['user_id' => $vUser['id']],
                [
                    'name' => $vUser['fullName'],
                    'location' => $vUser['profile_info']['location'],
                    'contact_info' => $vUser['profile_info']['telephone'],
                    'operational_status' => 'active',
                    'store_name' => $vUser['profile_info']['outlet_name'],
                    'location_within_campus' => $vUser['profile_info']['location'],
                    'contact_email' => $vUser['username'] . '@atu.edu.gh',
                    'operational_hours' => '08:00 AM - 06:00 PM',
                    'created_at' => now(),
                    'updated_at' => now(),
                ]
            );
        }
    }
}
