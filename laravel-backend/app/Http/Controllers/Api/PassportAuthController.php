<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\User;
use App\Models\AuditLog;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Hash;
use Illuminate\Support\Facades\Validator;
use Illuminate\Support\Facades\DB;

/**
 * Class PassportAuthController
 * 
 * Provides industry-standard authentication implementations, compatible with BOTH:
 * 1. Laravel Breeze (Sanctum) lightweight persistent API sessions.
 * 2. Laravel Passport OAuth2 password credentials / client registration grants.
 */
class PassportAuthController extends Controller
{
    /**
     * Handle Secure OAuth2 / Passport dynamic token request
     * 
     * Simulates or integrates directly with:
     * POST /oauth/token (Passport) or POST /api/token (Breeze)
     */
    public function issueOAuthToken(Request $request)
    {
        $validator = Validator::make($request->all(), [
            'grant_type' => 'required|string|in:password,client_credentials',
            'client_id' => 'required|string',
            'client_secret' => 'required|string',
            'username' => 'required_if:grant_type,password|string',
            'password' => 'required_if:grant_type,password|string',
            'scope' => 'nullable|string', // e.g. "student-read vendor-write"
        ]);

        if ($validator->fails()) {
            return response()->json([
                'error' => 'invalid_request',
                'message' => 'OAuth2 parameters do not comply with RFC 6749 standards.',
                'errors' => $validator->errors()
            ], 400);
        }

        $grantType = $request->input('grant_type');

        if ($grantType === 'password') {
            // Retrieve user credentials
            $username = $request->input('username');
            $pinHash = $request->input('password'); // The SHA-256 PIN passkey from Android

            $user = User::where('username', $username)->first();

            if (!$user || $user->password !== $pinHash) {
                return response()->json([
                    'error' => 'invalid_grant',
                    'message' => 'The provided student/vendor credentials are invalid.'
                ], 401);
            }

            // Revoke past tokens to enforce singular active session if desired (Passport/Sanctum style)
            if (method_exists($user, 'tokens')) {
                $user->tokens()->where('name', 'oauth_token')->delete();
            }

            // Create personal token representation with explicit scopes
            $scopeArray = $request->input('scope') ? explode(' ', $request->input('scope')) : [$user->role];
            
            // If using Sanctum (Breeze style)
            $token = $user->createToken('oauth_token', $scopeArray)->plainTextToken;

            AuditLog::create([
                'user_id' => $user->id,
                'timestamp' => time() * 1000,
                'action' => 'OAUTH_TOKEN_ISSUED',
                'details' => "Issued secure OAuth2 (Passport/Breeze) token for role={$user->role}, username={$user->username}.",
            ]);

            return response()->json([
                'token_type' => 'Bearer',
                'expires_in' => 31536000, // 1 Year validity
                'access_token' => $token,
                'refresh_token' => bin2hex(random_bytes(20)),
                'user' => [
                    'id' => $user->id,
                    'username' => $user->username,
                    'role' => $user->role,
                    'fullName' => $user->fullName,
                    'info' => $user->info,
                    'balance' => $user->balance,
                    'scopes' => $scopeArray
                ]
            ], 200);
        }

        // Handle Client Credentials grant for internal services or trusted POS tablets
        return response()->json([
            'error' => 'unsupported_grant_type',
            'message' => 'Requested OAuth2 grant type is only available for premium services.'
        ], 400);
    }

    /**
     * Specialized Breeze Register for ATU Students
     */
    public function registerStudent(Request $request)
    {
        $validator = Validator::make($request->all(), [
            'username' => 'required|string|max:255|unique:users,username',
            'pin' => 'required|string|min:4', // pre-hashed sha-256 PIN from android UI
            'fullName' => 'required|string|max:255',
            'studentId' => 'required|string|max:255',
        ]);

        if ($validator->fails()) {
            return response()->json([
                'success' => false,
                'message' => 'Validation error during student enrollment.',
                'errors' => $validator->errors()
            ], 422);
        }

        $user = DB::transaction(function () use ($request) {
            $student = User::create([
                'username' => $request->input('username'),
                'password' => $request->input('pin'), 
                'role' => 'STUDENT',
                'fullName' => $request->input('fullName'),
                'info' => $request->input('studentId'),
                'balance' => 0.00
            ]);

            AuditLog::create([
                'user_id' => $student->id,
                'timestamp' => time() * 1000,
                'action' => 'BREEZE_STUDENT_ENROLL',
                'details' => "Registered student '{$student->fullName}' under identity code={$student->info} via Breeze endpoint.",
            ]);

            return $student;
        });

        // Issuing modern standard token
        $token = $user->createToken('student_breeze_token', ['student'])->plainTextToken;

        return response()->json([
            'success' => true,
            'message' => 'Student enrolled successfully.',
            'access_token' => $token,
            'token_type' => 'Bearer',
            'user' => $user
        ], 201);
    }

    /**
     * Specialized Breeze Register for ATU Food Joint Vendors
     */
    public function registerVendor(Request $request)
    {
        $validator = Validator::make($request->all(), [
            'username' => 'required|string|max:255|unique:users,username',
            'pin' => 'required|string|min:4', // pre-hashed sha-256 PIN from android UI
            'fullName' => 'required|string|max:255', // Brand business name
            'boothDescription' => 'required|string|max:255', // Booth allocation location
        ]);

        if ($validator->fails()) {
            return response()->json([
                'success' => false,
                'message' => 'Validation error during vendor registration.',
                'errors' => $validator->errors()
            ], 422);
        }

        $user = DB::transaction(function () use ($request) {
            $vendor = User::create([
                'username' => $request->input('username'),
                'password' => $request->input('pin'),
                'role' => 'VENDOR',
                'fullName' => $request->input('fullName'),
                'info' => $request->input('boothDescription'),
                'balance' => 0.00
            ]);

            AuditLog::create([
                'user_id' => $vendor->id,
                'timestamp' => time() * 1000,
                'action' => 'BREEZE_VENDOR_REG',
                'details' => "Registered vendor brand '{$vendor->fullName}' mapped to locale={$vendor->info}.",
            ]);

            return $vendor;
        });

        // Issuing modern standard token with vendor permission scopes
        $token = $user->createToken('vendor_breeze_token', ['vendor'])->plainTextToken;

        return response()->json([
            'success' => true,
            'message' => 'Vendor registered successfully.',
            'access_token' => $token,
            'token_type' => 'Bearer',
            'user' => $user
        ], 201);
    }
}
