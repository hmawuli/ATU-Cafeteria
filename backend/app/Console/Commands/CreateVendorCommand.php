<?php

namespace App\Console\Commands;

use App\Models\AuditLog;
use App\Models\User;
use App\Models\Vendor;
use Illuminate\Console\Command;
use Illuminate\Support\Facades\Hash;
use Illuminate\Support\Str;

class CreateVendorCommand extends Command
{
    protected $signature = 'vendor:create
        {storeName : Display name of the stall/kitchen, e.g. "Campus Grill"}
        {--username= : Login username (defaults to a slug of the store name)}
        {--pin= : Vendor PIN (prompted if omitted)}
        {--campus= : Campus (default: Accra Technical University)}
        {--location= : Location within campus, e.g. "Block F, Booth 2"}';

    protected $description = 'Onboard a real vendor account (strong PIN) for production use.';

    private const WEAK_PINS = ['vendor123', 'admin123', 'atuadmin123', 'password', '123456', '1111', 'vendor'];

    public function handle(): int
    {
        $storeName = trim($this->argument('storeName'));
        if ($storeName === '') {
            $this->error('A store name is required.');

            return self::FAILURE;
        }

        $username = strtolower(trim($this->option('username') ?: Str::slug($storeName).mt_rand(10, 99)));
        if (User::where('username', $username)->exists()) {
            $this->error('Username "'.$username.'" already exists — pass a unique --username.');

            return self::FAILURE;
        }

        $isProduction = app()->environment('production');
        $pin = $this->option('pin');
        if ($pin === null) {
            $pin = $this->secret('Vendor PIN ('.($isProduction ? '10+ chars, letters + numbers' : 'min 6 chars').')');
        }
        $pin = (string) $pin;

        if (! $this->pinIsAcceptable($pin, $isProduction)) {
            return self::FAILURE;
        }

        $user = User::create([
            'username' => $username,
            'password' => Hash::make($pin),
            'role' => 'VENDOR',
            'fullName' => $storeName,
            'info' => $storeName,
            'account_status' => 'ACTIVE',
            'is_open' => false,
            'balance' => 0,
        ]);

        Vendor::create([
            'user_id' => $user->id,
            'name' => $storeName,
            'store_name' => $storeName,
            'location' => $this->option('location'),
            'location_within_campus' => $this->option('location'),
            'campus' => $this->option('campus') ?? 'Accra Technical University',
            'operational_status' => 'active',
        ]);

        AuditLog::create([
            'user_id' => $user->id,
            'timestamp' => time() * 1000,
            'action' => 'VENDOR_CREATED',
            'details' => "Bootstrap-created vendor '{$storeName}' (username {$username}).",
        ]);

        $this->info('Vendor created:');
        $this->line("    store    : {$storeName}");
        $this->line("    username : {$username}");
        $this->line('    login    : use the app (VENDOR role) or POST /api/vendor/login');

        return self::SUCCESS;
    }

    private function pinIsAcceptable(string $pin, bool $isProduction): bool
    {
        if (in_array(strtolower($pin), self::WEAK_PINS, true)) {
            $this->error('That PIN is a known weak value and was rejected.');

            return false;
        }

        if (strlen($pin) < 6) {
            $this->error('PIN must be at least 6 characters.');

            return false;
        }

        if ($isProduction && (strlen($pin) < 10 || ! preg_match('/[A-Za-z]/', $pin) || ! preg_match('/\d/', $pin))) {
            $this->error('Production vendor PINs must be at least 10 characters with letters and numbers.');

            return false;
        }

        return true;
    }
}
