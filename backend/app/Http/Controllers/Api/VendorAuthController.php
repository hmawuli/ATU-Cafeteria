<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\AuditLog;
use App\Models\User;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Hash;
use Illuminate\Support\Facades\Validator;

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
                'message' => 'Both username and pin are required.',
            ], 400);
        }

        $user = User::where('username', $request->input('username'))->first();

        // Enforce vendor role and valid credentials
        if (! $user || $user->role !== 'VENDOR') {
            return response()->json([
                'success' => false,
                'message' => 'Invalid vendor credentials or unauthorized role.',
            ], 401);
        }

        // Compare direct PIN (Pre-hashed from clients). Guard Hash::check with
        // a bcrypt prefix so malformed/legacy raw-hash rows fail safely (401)
        // instead of raising a RuntimeException (500).
        $storedPassword = (string) $user->password;
        $isBcrypt = str_starts_with($storedPassword, '$2');
        $pin = (string) $request->input('pin');
        $legacySha256 = hash('sha256', $pin);
        $directMatch = $isBcrypt && Hash::check($pin, $storedPassword);
        $legacySha256Match = $isBcrypt && Hash::check($legacySha256, $storedPassword);
        $rawSha256Match = hash_equals($storedPassword, $legacySha256);

        if ($directMatch || $legacySha256Match || $rawSha256Match) {
            if (! $directMatch) {
                $user->password = Hash::make($pin);
                $user->saveQuietly();
            }
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
                'user' => $responseData,
            ], 200)->header('X-Auth-Token', $token);
        }

        // Record failed login attempt
        AuditLog::create([
            'user_id' => $user->id,
            'timestamp' => time() * 1000,
            'action' => 'VENDOR_AUTH_FAILURE',
            'details' => 'Failed vendor login attempt with wrong PIN.',
        ]);

        return response()->json([
            'success' => false,
            'message' => 'Invalid Pin-Code hash.',
        ], 401);
    }
}
