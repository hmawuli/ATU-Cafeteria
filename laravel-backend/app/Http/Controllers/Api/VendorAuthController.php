<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\User;
use App\Models\AuditLog;
use App\Services\JwtService;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Validator;
use Illuminate\Support\Facades\DB;

class VendorAuthController extends Controller
{
    /**
     * Register a new vendor in the system.
     */
    public function register(Request $request)
    {
        $validator = Validator::make($request->all(), [
            'username' => 'required|string|max:255',
            'pin' => 'required|string|min:4', // Pre-hashed SHA-256 PIN from android
            'fullName' => 'required|string|max:255', // Represents Brand name
            'boothDescription' => 'required|string|max:255', // Maps to 'info' field (e.g., "Booth 3 / Indomie Center")
        ]);

        if ($validator->fails()) {
            return response()->json([
                'success' => false,
                'message' => 'Vendor registration input validation failed.',
                'errors' => $validator->errors()
            ], 400);
        }

        // Check if username already exists
        $existing = User::where('username', $request->input('username'))->first();
        if ($existing) {
            return response()->json([
                'success' => false,
                'message' => 'Username already exists.'
            ], 400);
        }

        // Create the vendor user inside a transaction
        $user = DB::transaction(function () use ($request) {
            $createdUser = User::create([
                'username' => $request->input('username'),
                'password' => $request->input('pin'), // Stored pre-hashed
                'role' => 'VENDOR',
                'fullName' => $request->input('fullName'),
                'info' => $request->input('boothDescription'),
            ]);

            // Register Audit Log
            AuditLog::create([
                'user_id' => $createdUser->id,
                'timestamp' => time() * 1000,
                'action' => 'VENDOR_REGISTRATION',
                'details' => "Registered vendor brand {$createdUser->fullName} at {$createdUser->info} via Vendor API.",
            ]);

            return $createdUser;
        });

        // Generate JWT access token
        $token = JwtService::generateToken($user);
        $response = $user->toArray();
        $response['token'] = $token;

        return response()->json([
            'success' => true,
            'message' => 'Vendor brand registered successfully.',
            'user' => $response
        ], 201)->header('X-Auth-Token', $token);
    }

    /**
     * Authenticate a vendor and return a Sanctum access token.
     */
    public function login(Request $request)
    {
        $validator = Validator::make($request->all(), [
            'username' => 'required|string',
            'pin' => 'required|string', // Pre-hashed SHA-256 PIN
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
        if ($user->password === $request->input('pin')) {
            // Register Audit Log
            AuditLog::create([
                'user_id' => $user->id,
                'timestamp' => time() * 1000,
                'action' => 'VENDOR_AUTHENTICATION',
                'details' => "Vendor {$user->fullName} logged in successfully via Sanctum.",
            ]);

            // Secure JWT token issue
            $token = JwtService::generateToken($user);
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
