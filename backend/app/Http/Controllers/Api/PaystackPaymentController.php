<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\AuditLog;
use App\Models\Payment;
use App\Models\User;
use App\Models\WalletTransaction;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Http;
use Illuminate\Support\Facades\Validator;

class PaystackPaymentController extends Controller
{
    protected function secretKey(): string
    {
        return trim((string) config('services.paystack.secret', ''));
    }

    protected function demoMode(): bool
    {
        return (bool) config('services.paystack.demo_mode', false);
    }

    protected function baseUrl(): string
    {
        return rtrim((string) config('services.paystack.base_url', 'https://api.paystack.co'), '/');
    }

    public function initialize(Request $request)
    {
        $user = $request->user();
        if (! $user) return response()->json(['success' => false, 'message' => 'Unauthenticated.'], 401);

        $validator = Validator::make($request->all(), [
            'email' => 'nullable|email',
            'amount' => 'required|numeric|min:0.50',
            'purpose' => 'required|string|in:WALLET_TOPUP,DIRECT_ORDER_PAY',
        ]);
        if ($validator->fails()) return response()->json(['success' => false, 'message' => 'Invalid payment inputs.', 'errors' => $validator->errors()], 422);

        $email = trim((string) ($user->email ?? ''));
        $requestedEmail = trim((string) ($request->input('email') ?? ''));
        if ($email === '') return response()->json(['success' => false, 'message' => 'Your account has no payment email address.'], 422);
        if ($requestedEmail !== '' && strcasecmp($requestedEmail, $email) !== 0) return response()->json(['success' => false, 'message' => 'The payment email must match your authenticated account.'], 422);

        $amount = round((float) $request->input('amount'), 2);
        $purpose = strtoupper((string) $request->input('purpose'));
        $reference = 'ATU-PAY-'.strtoupper(bin2hex(random_bytes(8))).'-'.time();
        $secret = $this->secretKey();

        if (! $this->demoMode() && $secret === '') {
            return response()->json(['success' => false, 'message' => 'Paystack is not configured on this server.'], 503);
        }

        $payment = Payment::create([
            'customer_id' => $user->id,
            'reference' => $reference,
            'gateway' => 'paystack',
            'amount' => $amount,
            'currency' => 'GHS',
            'purpose' => $purpose,
            'status' => 'INITIATED',
            'initiated_at' => now(),
        ]);

        WalletTransaction::create([
            'user_id' => $user->id,
            'payment_id' => $payment->id,
            'amount' => $amount,
            'type' => $purpose === 'WALLET_TOPUP' ? 'DEPOSIT' : 'PAYMENT',
            'status' => 'PENDING',
            'source' => 'PAYSTACK',
            'reference' => $reference,
            'details' => "Paystack initialization for {$purpose}.",
        ]);

        if ($this->demoMode()) {
            return response()->json(['success' => true, 'message' => 'Paystack demo transaction initialized.', 'data' => [
                'authorization_url' => 'https://checkout.paystack.com/demo?ref='.$reference,
                'access_code' => 'DEMO_'.strtoupper(bin2hex(random_bytes(6))),
                'reference' => $reference,
                'amount' => $amount,
                'is_simulated' => true,
            ]]);
        }

        try {
            $response = Http::timeout(20)->withToken($secret)->acceptJson()->post($this->baseUrl().'/transaction/initialize', [
                'email' => $email,
                'amount' => (int) round($amount * 100),
                'reference' => $reference,
                'metadata' => ['purpose' => $purpose, 'user_id' => $user->id],
            ]);

            if ($response->successful() && data_get($response->json(), 'status') === true) {
                $payment->update(['status' => 'PENDING', 'gateway_response' => $response->json('data')]);
                return response()->json(['success' => true, 'message' => 'Paystack transaction initialized.', 'data' => $response->json('data')]);
            }

            WalletTransaction::where('reference', $reference)->update(['status' => 'FAILED', 'details' => 'Paystack initialization failed.']);
            $payment->update(['status' => 'FAILED', 'failed_at' => now(), 'gateway_response' => $response->json()]);
            return response()->json(['success' => false, 'message' => 'Failed to initialize Paystack transaction.'], 502);
        } catch (\Throwable $e) {
            WalletTransaction::where('reference', $reference)->update(['status' => 'FAILED', 'details' => 'Paystack initialization connection failure.']);
            $payment->update(['status' => 'FAILED', 'failed_at' => now()]);
            report($e);
            return response()->json(['success' => false, 'message' => 'Unable to connect to Paystack. Please try again.'], 502);
        }
    }

    public function verify(Request $request, string $reference)
    {
        $user = $request->user();
        if (! $user) return response()->json(['success' => false, 'message' => 'Unauthenticated.'], 401);

        $validator = Validator::make($request->all(), [
            'amount' => 'nullable|numeric|min:0.50',
            'purpose' => 'nullable|string|in:WALLET_TOPUP,DIRECT_ORDER_PAY',
        ]);
        if ($validator->fails()) return response()->json(['success' => false, 'message' => 'Invalid verification inputs.', 'errors' => $validator->errors()], 422);

        $transaction = WalletTransaction::where('reference', $reference)->first();
        if (! $transaction) return response()->json(['success' => false, 'message' => 'Payment reference was not initialized by this system.'], 404);
        if ((int) $transaction->user_id !== (int) $user->id) return response()->json(['success' => false, 'message' => 'This payment reference does not belong to your account.'], 403);
        if ($transaction->status === 'SUCCESS') return response()->json(['success' => true, 'message' => 'Transaction was already processed.', 'reference' => $reference, 'amount' => (float) $transaction->amount, 'already_processed' => true]);
        if ($transaction->status !== 'PENDING') return response()->json(['success' => false, 'message' => 'This payment cannot be verified.'], 409);

        $expected = (float) $transaction->amount;
        if ($request->filled('amount') && abs((float) $request->input('amount') - $expected) > 0.01) return response()->json(['success' => false, 'message' => 'Verification amount does not match the initialized transaction.'], 409);

        $purpose = str_contains((string) $transaction->details, 'DIRECT_ORDER_PAY') ? 'DIRECT_ORDER_PAY' : 'WALLET_TOPUP';
        $amountPaid = $expected;
        $secret = $this->secretKey();

        if (! $this->demoMode()) {
            if ($secret === '') return response()->json(['success' => false, 'message' => 'Paystack is not configured on this server.'], 503);
            try {
                $response = Http::timeout(20)->withToken($secret)->acceptJson()->get($this->baseUrl().'/transaction/verify/'.rawurlencode($reference));
                $data = $response->json('data');
                if (! $response->successful() || data_get($response->json(), 'status') !== true || data_get($data, 'status') !== 'success') return response()->json(['success' => false, 'message' => 'Paystack has not confirmed this transaction as successful.'], 402);
                $amountPaid = ((int) data_get($data, 'amount', 0)) / 100;
                if (abs($amountPaid - $expected) > 0.01) return response()->json(['success' => false, 'message' => 'Paystack amount does not match the initialized transaction.'], 409);
                $gatewayUser = data_get($data, 'metadata.user_id');
                if ($gatewayUser !== null && (int) $gatewayUser !== (int) $user->id) return response()->json(['success' => false, 'message' => 'Paystack account metadata does not match the authenticated user.'], 403);
            } catch (\Throwable $e) {
                report($e);
                return response()->json(['success' => false, 'message' => 'Unable to connect to Paystack for verification. Please try again.'], 502);
            }
        }

        $this->completeWalletTransaction($transaction->id, $user->id, $amountPaid, $purpose, $reference);

        Payment::where('reference', $reference)->update([
            'gateway_transaction_id' => (string) data_get($data ?? [], 'id', ''),
            'gateway_response' => $data ?? null,
        ]);

        return response()->json(['success' => true, 'message' => 'Paystack payment verified successfully.', 'reference' => $reference, 'amount' => $amountPaid, 'purpose' => $purpose]);
    }

    /**
     * Paystack server-to-server webhook. This endpoint is deliberately outside
     * Sanctum authentication: Paystack authenticates it with the HMAC signature.
     * It only credits transactions that were initialized by this application.
     */
    public function webhook(Request $request)
    {
        $secret = $this->secretKey();
        if ($secret === '' || $this->demoMode()) {
            return response()->json(['success' => false, 'message' => 'Webhook processing is not configured.'], 503);
        }

        $signature = trim((string) $request->header('x-paystack-signature', ''));
        $payload = $request->getContent();
        $expectedSignature = hash_hmac('sha512', $payload, $secret);

        if ($signature === '' || ! hash_equals($expectedSignature, $signature)) {
            return response()->json(['success' => false, 'message' => 'Invalid webhook signature.'], 401);
        }

        $event = (string) $request->input('event', '');
        if (str_starts_with($event, 'transfer.')) {
            $reference = trim((string) $request->input('data.reference', ''));
            if ($reference === '') {
                return response()->json(['success' => false, 'message' => 'Transfer reference is missing.'], 422);
            }

            $settlement = \App\Models\VendorSettlement::where('payout_reference', $reference)
                ->lockForUpdate()
                ->first();

            if (! $settlement) {
                return response()->json(['success' => true, 'message' => 'Transfer reference is not registered.']);
            }

            $transferStatus = strtolower((string) $request->input('data.status', ''));
            $settlement->gateway_status = strtoupper($transferStatus ?: 'UNKNOWN');
            $settlement->transfer_code = $request->input('data.transfer_code', $settlement->transfer_code);

            $settlement->status = match ($event) {
                'transfer.success' => 'PAID',
                'transfer.failed', 'transfer.reversed' => 'FAILED',
                default => 'PROCESSING',
            };

            if ($settlement->status === 'PAID') {
                $settlement->settled_at = now();
                $settlement->failure_reason = null;
            }

            if ($settlement->status === 'FAILED') {
                $settlement->failure_reason = (string) ($request->input('data.reason') ?: $request->input('data.failures') ?: 'Paystack transfer did not complete.');
            }

            $settlement->save();

            return response()->json(['success' => true, 'message' => 'Transfer webhook reconciled.']);
        }

        if (str_starts_with($event, 'refund.')) {
            return DB::transaction(function () use ($request, $event) {
            $transactionReference = trim((string) $request->input('data.transaction_reference', ''));
            $refundStatus = strtolower((string) $request->input('data.status', ''));
            if ($transactionReference === '') {
                return response()->json(['success' => false, 'message' => 'Refund transaction reference is missing.'], 422);
            }

            $payment = Payment::where('reference', $transactionReference)->lockForUpdate()->first();
            if (! $payment) {
                return response()->json(['success' => true, 'message' => 'Refund payment reference is not registered.']);
            }

            $gatewayReference = trim((string) (
                $request->input('data.refund_reference')
                ?: $request->input('data.id')
                ?: ''
            ));

            $refundQuery = \App\Models\Refund::where('payment_id', $payment->id)
                ->whereIn('status', ['PENDING', 'PROCESSING']);

            if ($gatewayReference !== '') {
                $refund = (clone $refundQuery)
                    ->where(function ($query) use ($gatewayReference) {
                        $query->where('gateway_reference', $gatewayReference);
                    })
                    ->lockForUpdate()
                    ->first();

                if (! $refund) {
                    // Some Paystack payloads expose a numeric refund id while
                    // others expose refund_reference. Both are stored locally
                    // as gateway_reference during initiation.
                    $refund = (clone $refundQuery)
                        ->whereRaw('CAST(gateway_reference AS TEXT) = ?', [$gatewayReference])
                        ->lockForUpdate()
                        ->first();
                }
            } else {
                $pendingCount = (clone $refundQuery)->count();
                if ($pendingCount !== 1) {
                    return response()->json([
                        'success' => false,
                        'message' => 'Refund webhook could not uniquely identify a pending refund.',
                    ], 409);
                }
                $refund = $refundQuery->lockForUpdate()->first();
            }

            if (! $refund) {
                return response()->json(['success' => true, 'message' => 'Refund already reconciled or not registered.']);
            }

            $refund->gateway_reference = $gatewayReference !== ''
                ? $gatewayReference
                : $refund->gateway_reference;
            $refund->status = match ($refundStatus) {
                'processing' => 'PROCESSING',
                'processed' => 'SUCCESS',
                'failed' => 'FAILED',
                default => 'PENDING',
            };
            if ($refund->status === 'SUCCESS') {
                $refund->processed_at = now();

                if ($refund->payment_id && $refund->order_id) {
                    $allocation = \App\Models\PaymentAllocation::where('payment_id', $refund->payment_id)
                        ->where('order_id', $refund->order_id)
                        ->lockForUpdate()
                        ->first();

                    if ($allocation) {
                        $allocation->refunded_amount = round(
                            min(
                                (float) $allocation->amount,
                                (float) $allocation->refunded_amount + (float) $refund->amount
                            ),
                            2
                        );
                        $allocation->save();
                    }
                }

                $payment->load('allocations');
                $allocated = (float) $payment->allocations->sum(fn ($row) => (float) $row->amount);
                $refunded = (float) $payment->allocations->sum(fn ($row) => (float) $row->refunded_amount);
                $pendingRefunds = \App\Models\Refund::where('payment_id', $payment->id)
                    ->whereIn('status', ['PENDING', 'PROCESSING'])
                    ->exists();

                if ($allocated > 0 && $refunded + 0.01 >= $allocated) {
                    $payment->status = 'REFUNDED';
                } elseif ($pendingRefunds) {
                    $payment->status = 'REFUND_PENDING';
                } else {
                    $payment->status = 'SUCCESS';
                }

                $payment->refunded_at = $payment->status === 'REFUNDED'
                    ? now()
                    : $payment->refunded_at;
            } elseif ($refund->status === 'FAILED') {
                $payment->status = 'SUCCESS';
            } else {
                $payment->status = 'REFUND_PENDING';
            }

            $refund->save();
            $payment->save();

            AuditLog::create([
                'user_id' => $refund->customer_id,
                'timestamp' => now()->getTimestampMs(),
                'action' => 'PAYSTACK_REFUND_RECONCILED',
                'details' => "Paystack refund {$refund->status} reconciled for payment {$payment->reference}.",
            ]);

            return response()->json(['success' => true, 'message' => 'Refund webhook processed successfully.']);
            });
        }

        if ($event !== 'charge.success') {
            return response()->json(['success' => true, 'message' => 'Event acknowledged.']);
        }

        $reference = trim((string) $request->input('data.reference', ''));
        if ($reference === '') {
            return response()->json(['success' => false, 'message' => 'Webhook reference is missing.'], 422);
        }

        $transaction = WalletTransaction::where('reference', $reference)->first();
        if (! $transaction) {
            // A valid gateway event must never create an uninitialized wallet credit.
            return response()->json(['success' => true, 'message' => 'Payment reference is not registered; no credit issued.']);
        }

        if ($transaction->status === 'SUCCESS') {
            return response()->json(['success' => true, 'message' => 'Payment already processed.']);
        }

        if ($transaction->status !== 'PENDING') {
            return response()->json(['success' => true, 'message' => 'Payment is not pending; no credit issued.']);
        }

        $gatewayStatus = (string) $request->input('data.status', '');
        $gatewayAmount = ((int) $request->input('data.amount', 0)) / 100;
        $expectedAmount = (float) $transaction->amount;
        $gatewayUser = $request->input('data.metadata.user_id');

        if ($gatewayStatus !== 'success' || $gatewayAmount <= 0 || abs($gatewayAmount - $expectedAmount) > 0.01) {
            return response()->json(['success' => true, 'message' => 'Payment details did not pass validation; no credit issued.']);
        }

        if ($gatewayUser !== null && (int) $gatewayUser !== (int) $transaction->user_id) {
            return response()->json(['success' => true, 'message' => 'Payment metadata did not match the registered user; no credit issued.']);
        }

        $purpose = str_contains((string) $transaction->details, 'DIRECT_ORDER_PAY') ? 'DIRECT_ORDER_PAY' : 'WALLET_TOPUP';
        $this->completeWalletTransaction($transaction->id, (int) $transaction->user_id, $gatewayAmount, $purpose, $reference);

        return response()->json(['success' => true, 'message' => 'Webhook processed successfully.']);
    }

    protected function completeWalletTransaction(int $transactionId, int $userId, float $amountPaid, string $purpose, string $reference): void
    {
        DB::transaction(function () use ($transactionId, $userId, $amountPaid, $purpose, $reference) {
            $locked = WalletTransaction::whereKey($transactionId)->lockForUpdate()->firstOrFail();
            if ($locked->status === 'SUCCESS') return;
            if ($locked->status !== 'PENDING') return;
            if ((int) $locked->user_id !== $userId) throw new \RuntimeException('Payment ownership validation failed.');

            if ($purpose === 'WALLET_TOPUP') {
                $dbUser = User::lockForUpdate()->findOrFail($userId);
                $dbUser->balance = round((float) $dbUser->balance + $amountPaid, 2);
                $dbUser->save();
            }

            $locked->status = 'SUCCESS';
            $locked->amount = $amountPaid;
            $locked->details = "Paystack payment verified for {$purpose}.";
            $locked->save();

            $payment = Payment::where('reference', $reference)->lockForUpdate()->first();
            if ($payment) {
                $payment->status = 'SUCCESS';
                $payment->amount = $amountPaid;
                $payment->paid_at = now();
                $payment->save();
            }

            AuditLog::create([
                'user_id' => $userId,
                'timestamp' => now()->getTimestampMs(),
                'action' => $purpose === 'WALLET_TOPUP' ? 'PAYSTACK_WALLET_TOPUP' : 'PAYSTACK_DIRECT_PAY',
                'details' => 'Verified GH₵ '.number_format($amountPaid, 2)." via Paystack. Ref: {$reference}.",
            ]);
        });
    }
}
