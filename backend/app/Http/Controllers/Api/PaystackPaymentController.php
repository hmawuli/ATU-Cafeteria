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

        WalletTransaction::create([
            'user_id' => $user->id,
            'amount' => $amount,
            'type' => $purpose === 'WALLET_TOPUP' ? 'DEPOSIT' : 'PAYMENT',
            'status' => 'PENDING',
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
                return response()->json(['success' => true, 'message' => 'Paystack transaction initialized.', 'data' => $response->json('data')]);
            }

            WalletTransaction::where('reference', $reference)->update(['status' => 'FAILED', 'details' => 'Paystack initialization failed.']);
            return response()->json(['success' => false, 'message' => 'Failed to initialize Paystack transaction.'], 502);
        } catch (\Throwable $e) {
            WalletTransaction::where('reference', $reference)->update(['status' => 'FAILED', 'details' => 'Paystack initialization connection failure.']);
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

        DB::transaction(function () use ($transaction, $user, $amountPaid, $purpose, $reference) {
            $locked = WalletTransaction::whereKey($transaction->id)->lockForUpdate()->firstOrFail();
            if ($locked->status === 'SUCCESS') return;

            if ($purpose === 'WALLET_TOPUP') {
                $dbUser = User::lockForUpdate()->findOrFail($user->id);
                $dbUser->balance = round((float) $dbUser->balance + $amountPaid, 2);
                $dbUser->save();
            }

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
        });

        return response()->json(['success' => true, 'message' => 'Paystack payment verified successfully.', 'reference' => $reference, 'amount' => $amountPaid, 'purpose' => $purpose]);
    }
}
