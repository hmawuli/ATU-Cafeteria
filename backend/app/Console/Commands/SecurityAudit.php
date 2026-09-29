<?php

namespace App\Console\Commands;

use App\Models\User;
use Illuminate\Console\Command;
use Illuminate\Support\Facades\Hash;

class SecurityAudit extends Command
{
    protected $signature = 'security:audit {--fail-on-critical : Exit non-zero when critical findings exist}';

    protected $description = 'Audit administrator accounts (known development PINs, missing 2FA).';

    /**
     * Known development/demo PINs that must never protect a real admin.
     *
     * @var array<int, string>
     */
    private const DEV_PINS = [
        'admin123', 'atuadmin123', 'atuAdmin123', 'superadmin', 'vendor123',
        '1234', '1111', '123456', '12345678', 'password',
    ];

    public function handle(): int
    {
        $admins = User::where('role', 'ADMIN')->orderBy('username')->get();

        if ($admins->isEmpty()) {
            $this->info('No administrator accounts found.');

            return self::SUCCESS;
        }

        $this->table(
            ['Username', 'Level', '2FA', 'Known dev PIN'],
            $admins->map(fn (User $user) => [
                $user->username,
                $user->admin_level ?? 'CAFETERIA_ADMIN',
                $user->two_factor_enabled ? 'yes' : 'NO',
                $this->matchesKnownPin($user) ? 'YES' : 'no',
            ])
        );

        $critical = $admins->filter(fn (User $user) => $this->matchesKnownPin($user));
        $noTwoFa = $admins->filter(fn (User $user) => ! $user->two_factor_enabled && ! $this->matchesKnownPin($user));

        foreach ($critical as $user) {
            $this->error("CRITICAL: '{$user->username}' still uses a known development PIN. Change it immediately.");
        }
        foreach ($noTwoFa as $user) {
            $this->warn("'{$user->username}' has 2FA disabled.");
        }

        if ($critical->isNotEmpty()) {
            $this->line('');
            $this->warn('Fix: use php artisan admin:create <username> --pin <strong-pin> --super --enable-2fa');
            $this->line('     then delete or reset the compromised account.');

            if ((bool) $this->option('fail-on-critical')) {
                $this->error('Critical administrator findings — aborting.');

                return self::FAILURE;
            }
        } else {
            $this->info('Admin audit: no known development PINs found.');
        }

        return self::SUCCESS;
    }

    private function matchesKnownPin(User $user): bool
    {
        $stored = (string) $user->password;

        foreach (self::DEV_PINS as $pin) {
            if (Hash::check($pin, $stored) || hash_equals($stored, hash('sha256', $pin))) {
                return true;
            }
        }

        return false;
    }
}
