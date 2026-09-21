<?php

namespace App\Observers;

use App\Models\WalletTransaction;
use Illuminate\Support\Facades\Auth;
use LogicException;

/**
 * Protects the wallet ledger from arbitrary successful deposits and payouts.
 */
final class WalletTransactionObserver
{
    public function creating(WalletTransaction $transaction): void
    {
        $type = strtoupper(trim((string) $transaction->type));
        $status = strtoupper(trim((string) $transaction->status));

        if (! in_array($type, ['DEPOSIT', 'PAYOUT'], true) || $status !== 'SUCCESS') {
            return;
        }

        // Explicit CLI seed/maintenance operations may create historical rows,
        // but PHPUnit must still exercise the guard.
        if (app()->runningInConsole() && ! app()->runningUnitTests()) {
            return;
        }

        $actor = Auth::user();
        $details = strtolower((string) $transaction->details);
        $demoMode = filter_var(env('PAYSTACK_DEMO_MODE', false), FILTER_VALIDATE_BOOL);
        $isPaystackSettlement = str_contains($details, 'paystack') && str_contains($details, 'verified');
        $isAdminAdjustment = $actor && strtoupper((string) $actor->role) === 'ADMIN' && $type === 'DEPOSIT';

        if ($demoMode || $isPaystackSettlement || $isAdminAdjustment) {
            return;
        }

        if ($type === 'PAYOUT') {
            throw new LogicException('Successful vendor payouts require a verified payout provider settlement.');
        }

        throw new LogicException('Successful wallet deposits must come from a verified payment or authorized admin adjustment.');
    }
}
