<?php

namespace App\Console\Commands;

use App\Models\AuditLog;
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
        {--enable-2fa : Enable admin two-factor (dev default is off)}
        {--no-2fa : Explicitly disable 2FA where production defaults it on}
        {--force : Bypass the strong-PIN policy (test environments only)}';

    protected $description = 'Bootstrap an administrator account with a strong PIN and 2FA-by-default in production.';

    /**
     * Common dev/test PINs that must never secure a real admin.
     *
     * @var array<int, string>
     */
    private const WEAK_PINS = [
        'admin123', 'atuadmin123', 'password', '123456', '12345678',
        '123456789', '111111', 'test123', 'qwerty', 'superadmin', 'admin',
    ];

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

        $isProduction = app()->environment('production');
        $pin = $this->option('pin');
        if ($pin === null) {
            $pin = $this->secret('Admin PIN ('.($isProduction ? '10+ chars, letters + numbers' : 'min 6 chars').')');
        }
        $pin = (string) $pin;

        if (! $this->pinIsAcceptable($pin, $isProduction)) {
            return self::FAILURE;
        }

        $twoFa = $this->resolveTwoFactor($isProduction);

        $isSuper = (bool) $this->option('super');
        $user = User::create([
            'username' => $username,
            'password' => Hash::make($pin),
            'role' => 'ADMIN',
            'admin_level' => $isSuper ? 'SUPER_ADMIN' : 'CAFETERIA_ADMIN',
            'fullName' => $this->option('name') ?? ($isSuper ? 'Super Administrator' : 'Cafeteria Administrator'),
            'info' => 'Admin account',
            'account_status' => 'ACTIVE',
            'two_factor_enabled' => $twoFa,
            'balance' => 0,
        ]);

        AuditLog::create([
            'user_id' => $user->id,
            'timestamp' => time() * 1000,
            'action' => 'ADMIN_CREATED',
            'details' => "Bootstrap-created administrator '{$user->username}' (level {$user->admin_level}).",
        ]);

        $this->info('Admin created:');
        $this->line("    username : {$user->username}");
        $this->line("    level    : {$user->admin_level}");
        $this->line('    2FA      : '.($twoFa ? 'enabled' : 'disabled'));
        $this->line('    Login    : use the app (ADMIN role) or POST /api/login with username + PIN.');
        if (! $twoFa && $isProduction) {
            $this->warn('    2FA is DISABLED for a production admin — re-run with --no-2fa omitted to enable it.');
        }

        return self::SUCCESS;
    }

    private function pinIsAcceptable(string $pin, bool $isProduction): bool
    {
        $lower = strtolower($pin);

        if (! (bool) $this->option('force') && in_array($lower, self::WEAK_PINS, true)) {
            $this->error('That PIN is a known weak value and was rejected. Choose a strong, unique PIN.');

            return false;
        }

        if (strlen($pin) < 6) {
            $this->error('PIN must be at least 6 characters.');

            return false;
        }

        if ($isProduction && ! (bool) $this->option('force')) {
            if (strlen($pin) < 10 || ! preg_match('/[A-Za-z]/', $pin) || ! preg_match('/\d/', $pin)) {
                $this->error('Production admin PINs must be at least 10 characters with both letters and numbers (use --force to override).');

                return false;
            }
        }

        return true;
    }

    private function resolveTwoFactor(bool $isProduction): bool
    {
        if ($isProduction) {
            if ((bool) $this->option('no-2fa')) {
                // Explicit override — operator responsibility.
                return false;
            }

            return true;
        }

        return (bool) $this->option('enable-2fa');
    }
}
