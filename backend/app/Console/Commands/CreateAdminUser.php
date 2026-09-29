<?php

namespace App\Console\Commands;

use App\Models\User;
use Illuminate\Console\Command;
use Illuminate\Support\Facades\Hash;

class CreateAdminUser extends Command
{
    protected $signature = 'admin:create
        {username : Admin username (lowercase, unique)}
        {--pin= : Admin PIN (prompted if omitted)}
        {--name= : Display name (default per level)}
        {--super : Grant SUPER_ADMIN level}
        {--enable-2fa : Enable admin two-factor (default off)}';

    protected $description = 'Bootstrap an administrator account (use in production with a strong PIN).';

    public function handle(): int
    {
        $username = strtolower(trim($this->argument('username')));
        if ($username === '') {
            $this->error('A username is required.');

            return self::FAILURE;
        }

        if (User::where('username', $username)->exists()) {
            $this->error('A user with username "'.$username.'" already exists.');

            return self::FAILURE;
        }

        $pin = $this->option('pin');
        if ($pin === null) {
            $pin = $this->secret('Admin PIN (min 6 characters)');
        }
        if (strlen((string) $pin) < 6) {
            $this->error('PIN must be at least 6 characters.');

            return self::FAILURE;
        }

        $isSuper = (bool) $this->option('super');
        $user = User::create([
            'username' => $username,
            'password' => Hash::make($pin),
            'role' => 'ADMIN',
            'admin_level' => $isSuper ? 'SUPER_ADMIN' : 'CAFETERIA_ADMIN',
            'fullName' => $this->option('name') ?? ($isSuper ? 'Super Administrator' : 'Cafeteria Administrator'),
            'info' => 'Admin account',
            'account_status' => 'ACTIVE',
            'two_factor_enabled' => (bool) $this->option('enable-2fa'),
            'balance' => 0,
        ]);

        $this->info('Admin created:');
        $this->line("    username : {$user->username}");
        $this->line("    level    : {$user->admin_level}");
        $this->line('    2FA      : '.($user->two_factor_enabled ? 'enabled' : 'disabled'));
        $this->line('    Login    : use the app (ADMIN role) or POST /api/login with username + PIN.');

        return self::SUCCESS;
    }
}
