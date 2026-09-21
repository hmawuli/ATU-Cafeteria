<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\AuditLog;
use App\Models\User;
use App\Models\WalletTransaction;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Http;
use Illuminate\Support\Facades\Validator;
use Illuminate\Validation\ValidationException;

class PaystackPaymentController extends Controller
{
    /**
     * Resolve the configured Paystack secret without ever falling back to a
     * fake credential in a deployed environment.
     */
    protected function getSecretKey(): string
    {
        return trim((string) env('PAYSTACK_SECRET_KEY', ''));
    }

    protected function demoMode(): bool
    {
        return filter_var(env('PAYSTACK_DEMO_MODE', false), FILTER_VALIDATE_BOOL);
    }

    /**
     * Initialize a Paystack checkout transaction.
     *
     * The authenticated user's identity is authoritative. The email supplied
     * by a client is accepted only when it matches the account email.
     */
    public function initialize(Request $request)
    {
        $user = $request->user();
        if (! $user) {
            return response()->json(['success' => false, 'message' => 'Unauthenticated.'], 401);
        }

        $validator = Validator::make($request->all(), [
            'email' => 'nullable|email',
            'amount' => 'required|numeric|min:0.50',
            'purpose' => 'required|string|in:WALLET_TOPUP,DIRECT_ORDER_PAY',
        ]);

        if ($validator->fails()) {
            return response()->json([
                'success' => false,
                'message' => 'Invalid payment inputs.',
                'errors' => $validator->errors(),
            ], 422);
        }

        $accountEmail = trim((string) ($user->email ?? ''));
        $requestedEmail = trim((string) ($request->input('email') ?? ''));
        if ($accountEmail === '') {
            return response()->json([
                'success' => false,
                'message' => 'Your account does not have a payment email address. Update your profile first.',
            ], 422);
        }
        if ($requestedEmail !== '' && strcasecmp($requestedEmail, $accountEmail) !== 0) {
            return response()->json([
                'success' => false,
                'message' => 'The payment email must match your authenticated account.',
            ], 422);
        }

        $amount = round((float) $request->input('amount'), 2);
        $amountInPesewas = (int) round($amount * 100);
        $reference = 'ATU-PAY-'.strtoupper(bin2hex(random_bytes(8))).'-'.time();
        $purpose = strtoupper((string) $request->input('purpose'));
        $secretKey = $this->getSecretKey();
        $demoMode = $this->demoMode();

        if (! $demoMode && $secretKey === '') {
            return response()->json([
                'success' => false,
                'message' => 'Paystack is not configured on this server. Set PAYSTACK_SECRET_KEY before accepting payments.',
            ], 503);
        }

        // Persist a pending ledger row before contacting the gateway. This
        // binds the reference to this user and gives verification an auditable
        // transaction to update, preventing cross-user reference reuse.
        WalletTransaction::create([
            'user_id' => $user->id,
            'amount' => $amount,
            'type' => $purpose === 'WALLET_TOPUP' ? 'DEPOSIT' : 'PAYMENT',
            'status' => 'PENDING',
            'reference' => $reference,
            'details' => "Paystack initialization for {$purpose}.",
        ]);

        if ($demoMode) {
            return response()->json([
                'success' => true,
                'message' => 'Paystack demo transaction initialized.',
                'data' => [
                    'authorization_url' => 'https://checkout.paystack.com/demo?ref='.$reference,
                    'access_code' => 'DEMO_'.strtoupper(bin2hex(random_bytes(6))),
                    'reference' => $reference,
                    'amount' => $amount,
                    'is_simulated' => true,
                ],
            ], 200);
        }

        try {
            $response = Http::timeout(20)
                ->withToken($secretKey)
                ->acceptJson()
                ->post('https://api.paystack.co/transaction/initialize', [
                    'email' => $accountEmail,
                    'amount' => $amountInPesewas,
                    'reference' => $reference,
                    'metadata' => [
                        'purpose' => $purpose,
                        'user_id' => $user->id,
                    ],
                ]);

            if ($response->successful() && data_get($response->json(), 'status') === true) {
                return response()->json([
                    'success' => true,
                    'message' => 'Paystack transaction initialized.',
                    'data' => $response->json('data'),
                ], 200);
            }

            WalletTransaction::where('reference', $reference)->update([
                'status' => 'FAILED',
                'details' => 'Paystack initialization failed.',
            ]);

            return response()->json([
                'success' => false,
                'message' => 'Failed to initialize Paystack gateway transaction.',
            ], 502);
        } catch (\Throwable $e) {
            WalletTransaction::where('reference', $reference)->update([
                'status' => 'FAILED',
                'details' => 'Paystack initialization connection failure.',
            ]);

            report($e);
            return response()->json([
                'success' => false,
                'message' => 'Unable to connect to Paystack. Please try again.',
            ], 502);
        }
    }

    /**
     * Verify a Paystack transaction and apply its financial effect exactly once.
     */
    public function verify(Request $request, $reference)
    {
        $user = $request->user();
        if (! $user) {
            return response()->json(['success' => false, 'message' => 'Unauthenticated.'], 401);
        }

        $validator = Validator::make($request->all(), [
            'amount' => 'nullable|numeric|min:0.50',
            'purpose' => 'nullable|string|in:WALLET_TOPUP,DIRECT_ORDER_PAY',
        ]);
        if ($validator->fails()) {
            return response()->json([
                'success' => false,
                'message' => 'Invalid verification inputs.',
                'errors' => $validator->errors(),
            ], 422);
        }

        $transaction = WalletTransaction::where('reference', $reference)->first();
        if (! $transaction) {
            return response()->json([
                'success' => false,
                'message' => 'Payment reference was not initialized by this system.',
            ], 404);
        }

        if ((int) $transaction->user_id !== (int) $user->id) {
            return response()->json([
                'success' => false,
                'message' => 'This payment reference does not belong to your account.',
            ], 403);
        }

        if ($transaction->status === 'SUCCESS') {
            return response()->json([
                'success' => true,
                'message' => 'Transaction was already processed.',
                'reference' => $reference,
                'amount' => (float) $transaction->amount,
                'purpose' => $this->purposeFromTransaction($transaction),
                'already_processed' => true,
            ], 200);
        }

        if ($transaction->status === 'FAILED') {
            return response()->json([
                'success' => false,
                'message' => 'This payment initialization failed and cannot be verified.',
            ], 409);
        }

        $expectedAmount = (float) $transaction->amount;
        $requestedAmount = $request->filled('amount') ? (float) $request->input('amount') : $expectedAmount;
        if (abs($requestedAmount - $expectedAmount) > 0.01) {
            return response()->json([
                'success' => false,
                'message' => 'The requested verification amount does not match the initialized transaction.',
            ], 409);
        }

        $purpose = $this->purposeFromTransaction($transaction);
        $secretKey = $this->getSecretKey();
        $demoMode = $this->demoMode();
        $amountPaid = 0.0;
        $gatewayMetadata = [];

        if ($demoMode) {
            $amountPaid = $expectedAmount;
            $gatewayMetadata = ['purpose' => $purpose, 'user_id' => $user->id];
        } elseif ($secretKey !== '') {
            try {
                $response = Http::timeout(20)
                    ->withToken($secretKey)
                    ->acceptJson()
                    ->get("https://api.paystack.co/transaction/verify/{$reference}");

                $data = $response->json('data');
                if (! $response->successful() || data_get($response->json(), 'status') !== true || data_get($data, 'status') !== 'success') {
                    return response()->json([
                        'success' => false,
                        'message' => 'Paystack has not confirmed this transaction as successful.',
                    ], 402);
                }

                $amountPaid = ((int) data_get($data, 'amount', 0)) / 100;
                $gatewayMetadata = (array) data_get($data, 'metadata', []);

                if (abs($amountPaid - $expectedAmount) > 0.01) {
                    return response()->json([
                        'success' => false,
                        'message' => 'The amount confirmed by Paystack does not match the initialized amount.',
                    ], 409);
                }

                $gatewayUserId = data_get($gatewayMetadata, 'user_id');
                if ($gatewayUserId !== null && (int) $gatewayUserId !== (int) $user->id) {
                    return response()->json([
                        'success' => false,
                        'message' => 'Paystack metadata does not match the authenticated account.',
                    ], 403);
                }
            } catch (\Throwable $e) {
                report($e);
                return response()->json([
                    'success' => false,
                    'message' => 'Unable to connect to Paystack for verification. Please try again.',
                ], 502);
            }
        } else {
            return response()->json([
                'success' => false,
                'message' => 'Paystack is not configured on this server.',
            ], 503);
        }

        DB::beginTransaction();
        try {
            $locked = WalletTransaction::whereKey($transaction->id)->lockForUpdate()->firstOrFail();

            // A concurrent verification may have completed while the gateway
            // request was in flight. Never credit the wallet twice.
            if ($locked->status === 'SUCCESS') {
                DB::commit();
                return response()->json([
                    'success' => true,
                    'message' => 'Transaction was already processed.',
                    'reference' => $reference,
                    'amount' => (float) $locked->amount,
                    'purpose' => $purpose,
                    'already_processed' => true,
                ], 200);
            }

            $dbUser = User::lockForUpdate()->findOrFail($user->id);
            $dbUser->balance = round((float) $dbUser->balance + $amountPaid, 2);
            $dbUser->save();

            $locked->status = 'SUCCESS';
            $locked->amount = $amountPaid;
            $locked->details = "Paystack payment verified for {$purpose}.";
            $locked->save();

            AuditLog::create([
                'user_id' => $user->id,
                'timestamp' => now()->getTimestampMs(),
                'action' => $purpose === 'WALLET_TOPUP' ? 'PAYSTACK_WALLET_TOPUP' : 'PAYSTACK_DIRECT_PAY',
                'details' => 'Verified GH₵ '.number_format($amountPaid, 2)." via Paystack. Ref: {$reference}.",
            ]);

            DB::commit();

            return response()->json([
                'success' => true,
                'message' => 'Paystack payment verified and ledger updated successfully.',
                'reference' => $reference,
                'amount' => $amountPaid,
                'purpose' => $purpose,
            ], 200);
        } catch (ValidationException $e) {
            DB::rollBack();
            throw $e;
        } catch (\Throwable $e) {
            DB::rollBack();
            report($e);
            return response()->json([
                'success' => false,
                'message' => 'Financial transaction could not be completed safely.',
            ], 500);
        }
    }

    protected function purposeFromTransaction(WalletTransaction $transaction): string
    {
        if (str_contains((string) $transaction->details, 'DIRECT_ORDER_PAY')) {
            return 'DIRECT_ORDER_PAY';
        }
        return 'WALLET_TOPUP';
    }
}
