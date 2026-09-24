<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\AuditLog;
use App\Models\VendorPayoutAccount;
use App\Services\PaystackPayoutService;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Validator;

class VendorPayoutAccountController extends Controller
{
    public function show(Request $request)
    {
        $account = VendorPayoutAccount::where('vendor_id', $request->user()->id)->first();

        return response()->json([
            'success' => true,
            'account' => $account ? $this->payload($account) : null,
        ]);
    }

    public function banks(Request $request, PaystackPayoutService $paystack)
    {
        $type = strtoupper((string) $request->input('type', 'BANK'));
        if (! in_array($type, ['BANK', 'MOBILE_MONEY'], true)) {
            return response()->json(['success' => false, 'message' => 'Unsupported payout account type.'], 422);
        }

        try {
            $channels = $paystack->listGhanaBanks($type === 'MOBILE_MONEY' ? 'mobile_money' : 'ghipss');
        } catch (\Throwable $e) {
            report($e);
            return response()->json(['success' => false, 'message' => 'Unable to load payout channels.'], 503);
        }

        return response()->json(['success' => true, 'channels' => $channels]);
    }

    public function store(Request $request, PaystackPayoutService $paystack)
    {
        $validator = Validator::make($request->all(), [
            'type' => 'required|string|in:BANK,MOBILE_MONEY',
            'bank_code' => 'required|string|max:60',
            'bank_name' => 'nullable|string|max:160',
            'account_number' => 'required|string|min:7|max:30',
            'account_name' => 'nullable|string|max:160',
        ]);
        if ($validator->fails()) {
            return response()->json(['success' => false, 'message' => 'Invalid payout account details.', 'errors' => $validator->errors()], 422);
        }

        $vendor = $request->user();
        $type = strtoupper((string) $request->input('type'));
        $accountNumber = trim((string) $request->input('account_number'));
        $accountName = trim((string) $request->input('account_name', ''));
        $bankCode = trim((string) $request->input('bank_code'));

        if ($type === 'BANK') {
            try {
                $resolved = $paystack->resolveBankAccount($accountNumber, $bankCode);
                $accountName = trim((string) ($resolved['account_name'] ?? ''));
                if ($accountName === '') {
                    return response()->json(['success' => false, 'message' => 'Paystack could not resolve the account holder name.'], 422);
                }
            } catch (\Throwable $e) {
                report($e);
                return response()->json(['success' => false, 'message' => 'The bank account could not be verified.'], 422);
            }
        } elseif ($accountName === '') {
            return response()->json(['success' => false, 'message' => 'Account holder name is required for mobile money payouts.'], 422);
        }

        $account = VendorPayoutAccount::updateOrCreate(
            ['vendor_id' => $vendor->id],
            [
                'type' => $type,
                'bank_code' => $bankCode,
                'bank_name' => trim((string) $request->input('bank_name', '')) ?: null,
                'account_number' => $accountNumber,
                'account_number_last4' => substr($accountNumber, -4),
                'account_name' => $accountName,
                'currency' => 'GHS',
                'recipient_code' => null,
                'status' => 'PENDING',
                'verified_at' => null,
                'revoked_at' => null,
            ]
        );

        try {
            $recipient = $paystack->createRecipient($account);
            $account->update([
                'recipient_code' => $recipient['recipient_code'] ?? null,
                'status' => 'ACTIVE',
                'verified_at' => now(),
            ]);
        } catch (\Throwable $e) {
            $account->update(['status' => 'FAILED']);
            report($e);
            return response()->json(['success' => false, 'message' => 'The payout recipient could not be created.'], 502);
        }

        AuditLog::create([
            'user_id' => $vendor->id,
            'timestamp' => now()->getTimestampMs(),
            'action' => 'VENDOR_PAYOUT_ACCOUNT_UPDATED',
            'details' => 'Vendor payout account verified and activated.',
        ]);

        return response()->json([
            'success' => true,
            'message' => 'Payout account verified and saved.',
            'account' => $this->payload($account->fresh()),
        ]);
    }

    private function payload(VendorPayoutAccount $account): array
    {
        return [
            'id' => $account->id,
            'type' => $account->type,
            'bank_code' => $account->bank_code,
            'bank_name' => $account->bank_name,
            'account_name' => $account->account_name,
            'account_number' => $account->maskedAccountNumber(),
            'currency' => $account->currency,
            'status' => $account->status,
            'verified_at' => optional($account->verified_at)->toIso8601String(),
        ];
    }
}