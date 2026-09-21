<?php

namespace App\Observers;

use App\Models\WalletTransaction;
use Illuminate\Support\Facades\Auth;
use LogicException;

/**
 * Protects the wallet ledger from arbitrary successful deposits.
 *
 * A successful DEPOSIT may only originate from an explicitly enabled demo
 * transaction, a verified Paystack transaction, or an authenticated admin
 * adjustment. Ordinary clients cannot mint wallet balance by posting directly
 * to the legacy deposit endpoint.
 */
final class WalletTransactionObserver
{
    public function creating(WalletTransaction $transaction): void
    {
        $type = strtoupper(trim((string) $transaction->type));
        $status = strtoupper(trim((string) $transaction->status));

        if ($type !== 'DEPOSIT' || $status !== 'SUCCESS') {
            return;
        }

        // Allow explicit CLI seed/maintenance operations, but never bypass the
        // guard during PHPUnit execution.
        if (app()->runningInConsole() && ! app()->runningUnitTests()) {
            return;
        }

        $actor = Auth::user();
        $details = strtolower((string) $transaction->details);
        $demoMode = filter_var(env('PAYSTACK_DEMO_MODE', false), FILTER_VALIDATE_BOOL);
        $isPaystackSettlement = str_contains($details, 'paystack') && str_contains($details, 'verified');
        $isAdminAdjustment = $actor && strtoupper((string) $actor->role) === 'ADMIN';

        if ($demoMode || $isPaystackSettlement || $isAdminAdjustment) {
            return;
        }

        throw new LogicException('Successful wallet deposits must come from a verified payment or authorized admin adjustment.');
    }
}
