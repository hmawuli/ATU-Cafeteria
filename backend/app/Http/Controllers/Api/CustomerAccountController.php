<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\AuditLog;
use App\Models\CustomerAddress;
use App\Models\CustomerDevice;
use App\Models\Order;
use App\Models\WalletTransaction;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Hash;
use Illuminate\Support\Str;

class CustomerAccountController extends Controller
{
    /**
     * Portable export of everything the system holds about the student
     * (profile, orders, wallet ledger, devices, addresses) — data rights.
     */
    public function export(Request $request)
    {
        $user = $request->user();

        return response()->json([
            'success' => true,
            'data' => [
                'profile' => [
                    'id' => $user->id,
                    'username' => $user->username,
                    'email' => $user->email,
                    'role' => $user->role,
                    'fullName' => $user->fullName,
                    'balance' => (float) $user->balance,
                    'loyalty_points' => (int) ($user->loyalty_points ?? 0),
                    'account_status' => $user->account_status,
                    'created_at' => $user->created_at,
                ],
                'orders' => Order::where(function ($q) use ($user) {
                    $q->where('customer_id', $user->id)
                        ->orWhere('student_id', $user->id)
                        ->orWhere('user_id', $user->id);
                })->orderByDesc('created_at')->limit(500)->get(),
                'wallet_transactions' => WalletTransaction::where('user_id', $user->id)
                    ->orderByDesc('created_at')
                    ->limit(1000)
                    ->get(),
                'devices' => CustomerDevice::where('customer_id', $user->id)->get(),
                'addresses' => CustomerAddress::where('customer_id', $user->id)->get(),
                'exported_at' => now()->toIso8601String(),
            ],
        ], 200);
    }

    public function destroy(Request $request)
    {
        $user = $request->user();

        if (! $user || ! $user->isCustomer()) {
            return response()->json([
                'success' => false,
                'message' => 'Active customer account required.',
            ], 403);
        }

        if ((float) $user->balance > 0.01) {
            return response()->json([
                'success' => false,
                'message' => 'Please spend or withdraw your wallet balance before deleting your account.',
            ], 409);
        }

        $userId = $user->id;

        // Erase personal data (right to erasure) while keeping the account row
        // and financial/order history intact for audit and reconciliation.
        DB::transaction(function () use ($user, $userId) {
            $user->tokens()->delete();
            CustomerDevice::where('customer_id', $userId)->update([
                'revoked_at' => now(),
                'push_token' => null,
            ]);

            $user->forceFill([
                'username' => 'deleted_user_'.$userId.'_'.Str::lower(Str::random(6)),
                'fullName' => 'Deleted User',
                'password' => Hash::make(Str::random(40)),
                'profile_info' => null,
                'info' => null,
                'student_staff_id' => null,
                'account_status' => 'DELETED',
                'two_factor_enabled' => false,
            ])->saveQuietly();

            AuditLog::create([
                'user_id' => $userId,
                'timestamp' => now()->getTimestampMs(),
                'action' => 'CUSTOMER_ACCOUNT_CLOSED',
                'details' => 'Customer account deleted by owner; personal data anonymised, financial and order history retained for audit.',
            ]);
        });

        return response()->json([
            'success' => true,
            'message' => 'Your account has been deleted and your personal data anonymised. Historical order and financial records were retained for audit purposes.',
        ]);
    }
}
