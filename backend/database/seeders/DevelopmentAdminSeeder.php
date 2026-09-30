<?php

namespace Database\Seeders;

use App\Models\User;
use Illuminate\Database\Seeder;
use Illuminate\Support\Facades\Hash;
use RuntimeException;

class DevelopmentAdminSeeder extends Seeder
{
    /**
     * Development-only administrator accounts.
     *
     * Kept out of DatabaseSeeder and guarded so demonstration admins are
     * never created in production. Use php artisan admin:create to bootstrap
     * a real administrator with a strong PIN.
     */
    public function run(): void
    {
        if (app()->environment(['production', 'staging'])) {
            throw new RuntimeException('Development seeders must never run in production or staging.');
        }

        $accounts = [
            [
                'username' => 'superadmin',
                'pin' => 'atuAdmin123',
                'level' => 'SUPER_ADMIN',
                'name' => 'Super Administrator',
            ],
            [
                'username' => 'admin',
                'pin' => 'admin123',
                'level' => 'CAFETERIA_ADMIN',
                'name' => 'Cafeteria Administrator',
            ],
        ];

        foreach ($accounts as $account) {
            User::updateOrCreate(
                ['username' => $account['username']],
                [
                    'password' => Hash::make($account['pin']),
                    'role' => 'ADMIN',
                    'admin_level' => $account['level'],
                    'fullName' => $account['name'],
                    'info' => 'Development administrator (change PIN before any public deployment).',
                    'account_status' => 'ACTIVE',
                    'two_factor_enabled' => false,
                ]
            );
        }
    }
}
