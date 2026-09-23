<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\AuditLog;
use App\Models\User;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Hash;
use Illuminate\Support\Facades\Validator;

/**
 * Customer-facing authentication for the restaurant application.
 *
 * The database keeps the legacy STUDENT role for existing ATU accounts during
 * the migration. The public API uses CUSTOMER terminology so the application
 * is no longer coupled to an educational institution domain.
 */
class CustomerAuthController extends Controller
{
    public function register(Request $request)
    {
        $validator = Validator::make($request->all(), [
            'fullName' => 'required|string|min:2|max:255',
            'username' => 'required|string|min:3|max:100|alpha_dash',
            'email' => 'required|email|max:255',
            'pin' => 'required|digits_between:4,6',
            'pin_confirmation' => 'required|same:pin',
            'info' => 'nullable|string|max:500',
        ]);

        if ($validator->fails()) {
            return response()->json([
                'success' => false,
                'message' => 'Please correct the highlighted registration details.',
                'errors' => $validator->errors(),
            ], 422);
        }

        $username = trim((string) $request->input('username'));
        $email = strtolower(trim((string) $request->input('email')));

        if (User::whereRaw('LOWER(username) = ?', [strtolower($username)])->exists()) {
            return response()->json([
                'success' => false,
                'message' => 'That username is already in use.',
            ], 409);
        }

        $emailExists = DB::connection()->getDriverName() === 'sqlite'
            ? User::whereRaw("json_extract(profile_info, '$.email') = ?", [$email])->exists()
            : User::whereRaw("JSON_UNQUOTE(JSON_EXTRACT(profile_info, '$.email')) = ?", [$email])->exists();

        if ($emailExists) {
            return response()->json([
                'success' => false,
                'message' => 'An account with that email already exists.',
            ], 409);
        }

        $user = DB::transaction(function () use ($request, $username, $email) {
            $createdUser = User::create([
                'username' => $username,
                'password' => Hash::make((string) $request->input('pin')),
                'role' => 'STUDENT',
                'fullName' => trim((string) $request->input('fullName')),
                'info' => trim((string) $request->input('info', '')),
                'profile_info' => [
                    'email' => $email,
                    'account_type' => 'CUSTOMER',
                ],
                'account_status' => 'ACTIVE',
            ]);

            AuditLog::create([
                'user_id' => $createdUser->id,
                'timestamp' => time() * 1000,
                'action' => 'CUSTOMER_REGISTRATION',
                'details' => "Registered customer {$createdUser->fullName}.",
            ]);

            return $createdUser;
        });

        return $this->sessionResponse($user, 201, 'Customer account created successfully.');
    }

    public function login(Request $request)
    {
        $validator = Validator::make($request->all(), [
            'username' => 'required|string|max:255',
            'pin' => 'required|string|min:4|max:128',
        ]);

        if ($validator->fails()) {
            return response()->json([
                'success' => false,
                'message' => 'Username and PIN are required.',
                'errors' => $validator->errors(),
            ], 422);
        }

        $username = trim((string) $request->input('username'));
        $user = User::whereRaw('LOWER(username) = ?', [strtolower($username)])
            ->where('role', 'STUDENT')
            ->first();

        $credential = (string) $request->input('pin');
        $stored = (string) ($user->password ?? '');
        $valid = $user && (
            Hash::check($credential, $stored)
            || hash_equals($stored, hash('sha256', $credential))
        );

        if (! $valid) {
            if ($user) {
                AuditLog::create([
                    'user_id' => $user->id,
                    'timestamp' => time() * 1000,
                    'action' => 'CUSTOMER_AUTH_FAILURE',
                    'details' => 'Failed customer username/PIN login attempt.',
                ]);
            }

            return response()->json([
                'success' => false,
                'message' => 'Invalid username or PIN.',
            ], 401);
        }

        if (! $user->isActive()) {
            return response()->json([
                'success' => false,
                'message' => 'Your account is not active.',
            ], 403);
        }

        return $this->sessionResponse($user, 200, 'Customer login successful.');
    }

    private function sessionResponse(User $user, int $status, string $message)
    {
        $user->last_login_at = now();
        $user->saveQuietly();
        $user->tokens()->delete();
        $token = $user->createToken('customer_token', ['customer'])->plainTextToken;

        AuditLog::create([
            'user_id' => $user->id,
            'timestamp' => time() * 1000,
            'action' => 'CUSTOMER_AUTHENTICATION',
            'details' => 'Customer authenticated through restaurant API.',
        ]);

        $customer = $user->toArray();
        $customer['account_type'] = 'CUSTOMER';
        $customer['customer_id'] = $user->id;
        $customer['token'] = $token;

        return response()->json([
            'success' => true,
            'message' => $message,
            'customer' => $customer,
            'user' => $customer,
            'token' => $token,
        ], $status)->header('X-Auth-Token', $token);
    }
}
