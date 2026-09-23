<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\AuditLog;
use App\Models\CustomerDevice;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;

class CustomerAccountController extends Controller
{
    public function destroy(Request $request)
    {
        $user = $request->user();

        if (! $user || ! $user->isCustomer()) {
            return response()->json([
                'success' => false,
                'message' => 'Active customer account required.',
            ], 403);
        }

        // Preserve financial and order history. The account is closed and all
        // active sessions/devices are revoked rather than physically deleting
        // business records needed for reconciliation.
        DB::transaction(function () use ($user) {
            $user->account_status = 'DELETED';
            $user->tokens()->delete();
            CustomerDevice::where('customer_id', $user->id)->update([
                'revoked_at' => now(),
                'push_token' => null,
            ]);
            $user->saveQuietly();

            AuditLog::create([
                'user_id' => $user->id,
                'timestamp' => now()->getTimestampMs(),
                'action' => 'CUSTOMER_ACCOUNT_CLOSED',
                'details' => 'Customer account closed by account owner; financial and order history retained for audit.',
            ]);
        });

        return response()->json([
            'success' => true,
            'message' => 'Your account has been closed. Historical order and financial records were retained for business audit purposes.',
        ]);
    }
}
