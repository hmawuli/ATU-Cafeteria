<?php

namespace Tests\Unit;

use App\Models\User;
use App\Models\WalletTransaction;
use App\Observers\WalletTransactionObserver;
use Illuminate\Support\Facades\Auth;
use LogicException;
use Tests\TestCase;

class WalletTransactionObserverTest extends TestCase
{
    public function test_unverified_successful_deposit_is_rejected_for_normal_user(): void
    {
        config(['app.env' => 'testing']);
        $student = new User;
        $student->id = 42;
        $student->role = 'STUDENT';
        Auth::login($student);

        $transaction = new WalletTransaction([
            'user_id' => 42,
            'type' => 'DEPOSIT',
            'amount' => 100,
            'status' => 'SUCCESS',
            'reference' => 'TEST-DEPOSIT-1',
            'details' => 'Loaded via Mobile Money Gateway',
        ]);

        $this->expectException(LogicException::class);
        (new WalletTransactionObserver)->creating($transaction);
    }

    public function test_verified_paystack_deposit_is_allowed(): void
    {
        $student = new User;
        $student->id = 42;
        $student->role = 'STUDENT';
        Auth::login($student);

        $transaction = new WalletTransaction([
            'user_id' => 42,
            'type' => 'DEPOSIT',
            'amount' => 100,
            'status' => 'SUCCESS',
            'reference' => 'ATU-PAY-TEST',
            'details' => 'Paystack payment verified for WALLET_TOPUP.',
        ]);

        (new WalletTransactionObserver)->creating($transaction);
        $this->assertSame('SUCCESS', $transaction->status);
    }

    public function test_admin_wallet_adjustment_is_allowed(): void
    {
        $admin = new User;
        $admin->id = 99;
        $admin->role = 'ADMIN';
        Auth::login($admin);

        $transaction = new WalletTransaction([
            'user_id' => 42,
            'type' => 'DEPOSIT',
            'amount' => 50,
            'status' => 'SUCCESS',
            'reference' => 'ADMIN-TEST-1',
            'details' => 'Approved administrative adjustment',
        ]);

        (new WalletTransactionObserver)->creating($transaction);
        $this->assertSame('SUCCESS', $transaction->status);
    }
}
