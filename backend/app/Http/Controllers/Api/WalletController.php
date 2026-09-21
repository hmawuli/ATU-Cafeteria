<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\AuditLog;
use App\Models\User;
use App\Models\WalletTransaction;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Validator;
use Illuminate\Support\Str;

class WalletController extends Controller
{
    public function getBalance(Request $request)
    {
        $user = $request->user();
        if (! $user) {
            return response()->json(['success' => false, 'message' => 'Unauthorized access.'], 401);
        }

        return response()->json([
            'success' => true,
            'balance' => (float) $user->balance,
            'username' => $user->username,
            'fullName' => $user->fullName,
        ]);
    }

    public function getTransactions(Request $request)
    {
        $user = $request->user();
        if (! $user) {
            return response()->json(['success' => false, 'message' => 'Unauthorized.'], 401);
        }

        $transactions = WalletTransaction::where('user_id', $user->id)
            ->orderByDesc('created_at')
            ->get();

        return response()->json(['success' => true, 'transactions' => $transactions]);
    }

    /**
     * Legacy manual deposit endpoint.
     * Wallet credits must come from the verified Paystack flow so a client
     * cannot simply POST an amount and mint balance.
     */
    public function deposit(Request $request)
    {
        return response()->json([
            'success' => false,
            'message' => 'Direct wallet deposits are disabled. Use the Paystack wallet top-up flow.',
        ], 410);
    }

    public function transfer(Request $request)
    {
        $sender = $request->user();
        if (! $sender) {
            return response()->json(['success' => false, 'message' => 'Unauthorized.'], 401);
        }

        $validator = Validator::make($request->all(), [
            'receiver_username' => 'required|string|exists:users,username',
            'amount' => 'required|numeric|min:0.50',
        ]);

        if ($validator->fails()) {
            return response()->json([
                'success' => false,
                'message' => 'Invalid transaction parameters.',
                'errors' => $validator->errors(),
            ], 422);
        }

        $amount = round((float) $request->input('amount'), 2);
        $receiverUsername = trim((string) $request->input('receiver_username'));

        try {
            DB::beginTransaction();

            $dbSender = User::lockForUpdate()->find($sender->id);
            $dbReceiver = User::where('username', $receiverUsername)->lockForUpdate()->first();

            if (! $dbSender || ! $dbReceiver) {
                DB::rollBack();
                return response()->json(['success' => false, 'message' => 'Transfer account could not be found.'], 404);
            }

            if ((int) $dbSender->id === (int) $dbReceiver->id) {
                DB::rollBack();
                return response()->json(['success' => false, 'message' => 'Cannot transfer money to yourself.'], 400);
            }

            if ((float) $dbSender->balance < $amount) {
                DB::rollBack();
                return response()->json(['success' => false, 'message' => 'Insufficient wallet balance.'], 400);
            }

            $dbSender->balance = round((float) $dbSender->balance - $amount, 2);
            $dbSender->save();
            $dbReceiver->balance = round((float) $dbReceiver->balance + $amount, 2);
            $dbReceiver->save();

            $refText = 'TXF-'.strtoupper(Str::random(10));
            WalletTransaction::create([
                'user_id' => $dbSender->id,
                'type' => 'PAYMENT',
                'amount' => -$amount,
                'status' => 'SUCCESS',
                'reference' => $refText.'S',
                'details' => "Transfer to {$dbReceiver->fullName} (@{$dbReceiver->username})",
            ]);
            WalletTransaction::create([
                'user_id' => $dbReceiver->id,
                'type' => 'REFUND',
                'amount' => $amount,
                'status' => 'SUCCESS',
                'reference' => $refText.'R',
                'details' => "Received from {$dbSender->fullName} (@{$dbSender->username})",
            ]);

            AuditLog::create([
                'user_id' => $dbSender->id,
                'timestamp' => now()->getTimestampMs(),
                'action' => 'WALLET_TRANSFER',
                'details' => 'Sent GH₵ '.number_format($amount, 2)." to @{$dbReceiver->username}.",
            ]);

            DB::commit();

            return response()->json([
                'success' => true,
                'message' => 'Transfer completed successfully!',
                'balance' => (float) $dbSender->balance,
            ]);
        } catch (\Throwable $e) {
            DB::rollBack();
            report($e);
            return response()->json(['success' => false, 'message' => 'Transfer could not be completed.'], 500);
        }
    }

    /**
     * Create a payout request without pretending that external settlement has
     * already happened. The balance is reserved and the ledger remains PENDING
     * until an authorized settlement process marks it successful.
     */
    public function requestPayout(Request $request)
    {
        $vendor = $request->user();
        if (! $vendor || strtoupper((string) $vendor->role) !== 'VENDOR') {
            return response()->json([
                'success' => false,
                'message' => 'Unauthorized or user is not a verified vendor.',
            ], 403);
        }

        $validator = Validator::make($request->all(), [
            'amount' => 'required|numeric|min:5',
            'details' => 'required|string|max:255',
        ]);

        if ($validator->fails()) {
            return response()->json([
                'success' => false,
                'message' => 'Invalid payout amount or channel.',
                'errors' => $validator->errors(),
            ], 422);
        }

        $amount = round((float) $request->input('amount'), 2);

        try {
            DB::beginTransaction();

            $dbVendor = User::lockForUpdate()->find($vendor->id);
            if (! $dbVendor || (float) $dbVendor->balance < $amount) {
                DB::rollBack();
                return response()->json(['success' => false, 'message' => 'Insufficient earnings balance for payout.'], 400);
            }

            $dbVendor->balance = round((float) $dbVendor->balance - $amount, 2);
            $dbVendor->save();

            $ref = 'PAY-'.strtoupper(Str::random(10));
            $transaction = WalletTransaction::create([
                'user_id' => $dbVendor->id,
                'type' => 'PAYOUT',
                'amount' => -$amount,
                'status' => 'PENDING',
                'reference' => $ref,
                'details' => 'Payout requested to: '.$request->input('details'),
            ]);

            AuditLog::create([
                'user_id' => $dbVendor->id,
                'timestamp' => now()->getTimestampMs(),
                'action' => 'VENDOR_PAYOUT_REQUESTED',
                'details' => 'Requested payout of GH₵ '.number_format($amount, 2).". Ref: {$ref}.",
            ]);

            DB::commit();

            return response()->json([
                'success' => true,
                'message' => 'Payout request submitted for settlement.',
                'balance' => (float) $dbVendor->balance,
                'transaction' => $transaction,
            ], 202);
        } catch (\Throwable $e) {
            DB::rollBack();
            report($e);
            return response()->json(['success' => false, 'message' => 'Payout request could not be created.'], 500);
        }
    }
}
