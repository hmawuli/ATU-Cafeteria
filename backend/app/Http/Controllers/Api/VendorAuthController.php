<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\User;
use App\Models\AuditLog;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Validator;
use Illuminate\Support\Facades\Hash;
use Illuminate\Support\Facades\DB;

class VendorAuthController extends Controller
{
    /**
     * Register a new vendor in the system.
     */
    public function register(Request $request)
    {
        return response()->json([
            'success' => false,
            'message' => 'Vendor accounts must be created or approved by an administrator.',
        ], 403);
    }

    /**
     * Authenticate a vendor and return a Sanctum access token.
     */
    public function login(Request $request)
    {
        $validator = Validator::make($request->all(), [
            'username' => 'required|string',
            'pin' => 'required|string|min:4|max:128',
        ]);

        if ($validator->fails()) {
            return response()->json([
                'success' => false,
                'message' => 'Both username and pin are required.'
            ], 400);
        }

        $user = User::where('username', $request->input('username'))->first();

        // Enforce vendor role and valid credentials
        if (!$user || $user->role !== 'VENDOR') {
            return response()->json([
                'success' => false,
                'message' => 'Invalid vendor credentials or unauthorized role.'
            ], 401);
        }

        // Compare direct PIN (Pre-hashed from clients)
        if (Hash::check($request->input('pin'), (string) $user->password) || Hash::check(hash('sha256', $request->input('pin')), (string) $user->password) || hash_equals((string) $user->password, hash('sha256', $request->input('pin')))) {
            if (!Hash::check($request->input('pin'), (string) $user->password)) { $user->password = Hash::make($request->input('pin')); $user->saveQuietly(); }
            // Register Audit Log
            AuditLog::create([
                'user_id' => $user->id,
                'timestamp' => time() * 1000,
                'action' => 'VENDOR_AUTHENTICATION',
                'details' => "Vendor {$user->fullName} logged in successfully via Sanctum.",
            ]);

            // Generate Laravel Sanctum token
            $token = $user->createToken('vendor_token', ['vendor'])->plainTextToken;
            $responseData = $user->toArray();
            $responseData['token'] = $token;

            return response()->json([
                'success' => true,
                'message' => 'Vendor logged in successfully.',
                'user' => $responseData
            ], 200)->header('X-Auth-Token', $token);
        }

        // Record failed login attempt
        AuditLog::create([
            'user_id' => $user->id,
            'timestamp' => time() * 1000,
            'action' => 'VENDOR_AUTH_FAILURE',
            'details' => "Failed vendor login attempt with wrong PIN.",
        ]);

        return response()->json([
            'success' => false,
            'message' => 'Invalid Pin-Code hash.'
        ], 401);
    }
}
