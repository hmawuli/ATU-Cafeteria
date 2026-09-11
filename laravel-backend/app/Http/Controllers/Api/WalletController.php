<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\User;
use App\Models\WalletTransaction;
use App\Models\AuditLog;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Validator;
use Illuminate\Support\Str;

class WalletController extends Controller
{
    /**
     * Get the authenticated user's balance and recent transactions.
     */
    public function getBalance(Request $request)
    {
        $user = $request->user();
        if (!$user) {
            return response()->json([
                'success' => false,
                'message' => 'Unauthorized access.'
            ], 401);
        }

        return response()->json([
            'success' => true,
            'balance' => (double)$user->balance,
            'username' => $user->username,
            'fullName' => $user->fullName
        ]);
    }

    /**
     * Retrieve a filtered history of wallet transactions for the authenticated user.
     */
    public function getTransactions(Request $request)
    {
        $user = $request->user();
        if (!$user) {
            return response()->json([
                'success' => false,
                'message' => 'Unauthorized.'
            ], 401);
        }

        $transactions = WalletTransaction::where('user_id', $user->id)
            ->orderBy('created_at', 'desc')
            ->get();

        return response()->json([
            'success' => true,
            'transactions' => $transactions
        ]);
    }

    /**
     * Deposit virtual funds securely (simulating real mobile money, e.g. MTN MoMo, Telecel Cash).
     */
    public function deposit(Request $request)
    {
        $user = $request->user();
        if (!$user) {
            return response()->json(['success' => false, 'message' => 'Unauthorized.'], 401);
        }

        $validator = Validator::make($request->all(), [
            'amount' => 'required|numeric|min:1',
            'details' => 'nullable|string'
        ]);

        if ($validator->fails()) {
            return response()->json([
                'success' => false,
                'message' => 'Validation error.',
                'errors' => $validator->errors()
            ], 422);
        }

        $amount = (double) $request->input('amount');
        $details = $request->input('details', 'Loaded via Mobile Money Gateway');

        try {
            DB::beginTransaction();

            // Reload user model inside transaction with shared lock
            $dbUser = User::lockForUpdate()->find($user->id);
            $dbUser->balance += $amount;
            $dbUser->save();

            // Create Transaction record
            $ref = 'TXN-' . strtoupper(Str::random(10));
            $transaction = WalletTransaction::create([
                'user_id' => $dbUser->id,
                'type' => 'DEPOSIT',
                'amount' => $amount,
                'status' => 'SUCCESS',
                'reference' => $ref,
                'details' => $details
            ]);

            // Add to system Audit Logs
            AuditLog::create([
                'user_id' => $dbUser->id,
                'timestamp' => time() * 1000,
                'action' => 'WALLET_DEPOSIT',
                'details' => "Securely deposited GH₵ " . number_format($amount, 2) . " via MoMo. Ref: $ref."
            ]);

            DB::commit();

            return response()->json([
                'success' => true,
                'message' => 'GH₵ ' . number_format($amount, 2) . ' loaded successfully!',
                'balance' => (double)$dbUser->balance,
                'transaction' => $transaction
            ]);
        } catch (\Exception $e) {
            DB::rollBack();
            return response()->json([
                'success' => false,
                'message' => 'Transaction failure: ' . $e->getMessage()
            ], 500);
        }
    }

    /**
     * Peer-to-peer virtual transfer (e.g., student to student or manually to vendors).
     */
    public function transfer(Request $request)
    {
        $sender = $request->user();
        if (!$sender) {
            return response()->json(['success' => false, 'message' => 'Unauthorized.'], 401);
        }

        $validator = Validator::make($request->all(), [
            'receiver_username' => 'required|string|exists:users,username',
            'amount' => 'required|numeric|min:0.5'
        ]);

        if ($validator->fails()) {
            return response()->json([
                'success' => false,
                'message' => 'Invalid transaction parameters.',
                'errors' => $validator->errors()
            ], 422);
        }

        $amount = (double) $request->input('amount');
        if ($sender->balance < $amount) {
            return response()->json([
                'success' => false,
                'message' => 'Insufficient wallet balance.'
            ], 400);
        }

        $receiverUsername = $request->input('receiver_username');

        try {
            DB::beginTransaction();

            // Lock sender and receiver rows to prevent race-conditions
            $dbSender = User::lockForUpdate()->find($sender->id);
            if ($dbSender->balance < $amount) {
                DB::rollBack();
                return response()->json(['success' => false, 'message' => 'Insufficient wallet balance'], 400);
            }

            $dbReceiver = User::where('username', $receiverUsername)->lockForUpdate()->first();

            if ($dbSender->id === $dbReceiver->id) {
                DB::rollBack();
                return response()->json(['success' => false, 'message' => 'Cannot transfer money to yourself.'], 400);
            }

            // Perform balance exchange
            $dbSender->balance -= $amount;
            $dbSender->save();

            $dbReceiver->balance += $amount;
            $dbReceiver->save();

            // Create Transaction Log for Sender
            $refText = 'TXF-' . strtoupper(Str::random(10));
            WalletTransaction::create([
                'user_id' => $dbSender->id,
                'type' => 'PAYMENT',
                'amount' => -$amount,
                'status' => 'SUCCESS',
                'reference' => $refText . 'S',
                'details' => "Transfer to {$dbReceiver->fullName} (@{$dbReceiver->username})"
            ]);

            // Create Transaction Log for Receiver
            WalletTransaction::create([
                'user_id' => $dbReceiver->id,
                'type' => 'REFUND',
                'amount' => $amount,
                'status' => 'SUCCESS',
                'reference' => $refText . 'R',
                'details' => "Received from {$dbSender->fullName} (@{$dbSender->username})"
            ]);

            // Add Audit Logs for tracking
            AuditLog::create([
                'user_id' => $dbSender->id,
                'timestamp' => time() * 1000,
                'action' => 'WALLET_TRANSFER',
                'details' => "Sent GH₵ " . number_format($amount, 2) . " to @{$dbReceiver->username}."
            ]);

            DB::commit();

            return response()->json([
                'success' => true,
                'message' => 'Transfer completed successfully!',
                'balance' => (double)$dbSender->balance
            ]);
        } catch (\Exception $e) {
            DB::rollBack();
            return response()->json([
                'success' => false,
                'message' => 'Transfer error: ' . $e->getMessage()
            ], 500);
        }
    }

    /**
     * Request payout (for VENDOR role to request moving earnings/money to active mobile money wallets).
     */
    public function requestPayout(Request $request)
    {
        $vendor = $request->user();
        if (!$vendor || $vendor->role !== 'VENDOR') {
            return response()->json([
                'success' => false,
                'message' => 'Unauthorized or user is not a verified vendor.'
            ], 403);
        }

        $validator = Validator::make($request->all(), [
            'amount' => 'required|numeric|min:5',
            'details' => 'required|string'
        ]);

        if ($validator->fails()) {
            return response()->json([
                'success' => false,
                'message' => 'Invalid payout amount or channel.',
                'errors' => $validator->errors()
            ], 422);
        }

        $amount = (double) $request->input('amount');
        if ($vendor->balance < $amount) {
            return response()->json([
                'success' => false,
                'message' => 'Insufficient earnings balance for payout.'
            ], 400);
        }

        try {
            DB::beginTransaction();

            $dbVendor = User::lockForUpdate()->find($vendor->id);
            if ($dbVendor->balance < $amount) {
                DB::rollBack();
                return response()->json(['success' => false, 'message' => 'Insufficient earnings balance.'], 400);
            }

            $dbVendor->balance -= $amount;
            $dbVendor->save();

            $ref = 'PAY-' . strtoupper(Str::random(10));
            $transaction = WalletTransaction::create([
                'user_id' => $dbVendor->id,
                'type' => 'PAYOUT',
                'amount' => -$amount,
                'status' => 'SUCCESS',
                'reference' => $ref,
                'details' => 'Payout requested to: ' . $request->input('details')
            ]);

            AuditLog::create([
                'user_id' => $dbVendor->id,
                'timestamp' => time() * 1000,
                'action' => 'VENDOR_PAYOUT',
                'details' => "Requested settlement of GH₵ " . number_format($amount, 2) . " to mobile money account."
            ]);

            DB::commit();

            return response()->json([
                'success' => true,
                'message' => 'Payout processed successfully!',
                'balance' => (double)$dbVendor->balance,
                'transaction' => $transaction
            ]);
        } catch (\Exception $e) {
            DB::rollBack();
            return response()->json([
                'success' => false,
                'message' => 'Payout settlement error: ' . $e->getMessage()
            ], 500);
        }
    }
}
