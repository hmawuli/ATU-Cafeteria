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
 * Restaurant-facing customer authentication.
 *
 * The database continues to use the legacy STUDENT role for existing ATU
 * accounts so current orders, permissions and mobile clients remain compatible.
 * The public API deliberately exposes the account as a CUSTOMER domain concept.
 */
class CustomerAuthController extends Controller
{
    public function register(Request $request)
    {
        $validator = Validator::make($request->all(), [
            'name' => 'required|string|min:2|max:255',
            'email' => 'required|email|max:255',
            'password' => 'required|string|min:8|max:128',
            'phone' => 'nullable|string|max:30',
        ]);

        if ($validator->fails()) {
            return response()->json([
                'success' => false,
                'message' => 'Please provide a valid customer name, email and password.',
                'errors' => $validator->errors(),
            ], 422);
        }

        $email = strtolower(trim((string) $request->input('email')));
        if (User::whereRaw('LOWER(username) = ?', [$email])->exists()) {
            return response()->json([
                'success' => false,
                'message' => 'An account with this email address already exists.',
            ], 409);
        }

        $user = DB::transaction(function () use ($request, $email) {
            $profile = [
                'email' => $email,
                'customer_type' => 'CUSTOMER',
            ];
            $phone = trim((string) $request->input('phone', ''));
            if ($phone !== '') {
                $profile['phone'] = $phone;
            }

            $user = User::create([
                'username' => $email,
                'password' => Hash::make((string) $request->input('password')),
                // Keep the existing role for backwards-compatible order access.
                'role' => 'STUDENT',
                'fullName' => trim((string) $request->input('name')),
                'info' => '',
                'profile_info' => $profile,
                'account_status' => 'ACTIVE',
            ]);

            AuditLog::create([
                'user_id' => $user->id,
                'timestamp' => time() * 1000,
                'action' => 'CUSTOMER_REGISTRATION',
                'details' => 'Registered a restaurant customer account.',
            ]);

            return $user;
        });

        return $this->sessionResponse($user, 201, 'Customer account created successfully.');
    }

    public function login(Request $request)
    {
        $validator = Validator::make($request->all(), [
            'email' => 'required|email|max:255',
            'password' => 'required|string|min:8|max:128',
        ]);

        if ($validator->fails()) {
            return response()->json([
                'success' => false,
                'message' => 'Email and password are required.',
                'errors' => $validator->errors(),
            ], 422);
        }

        $email = strtolower(trim((string) $request->input('email')));
        $user = User::whereRaw('LOWER(username) = ?', [$email])
            ->where('role', 'STUDENT')
            ->first();

        if (! $user || ! Hash::check((string) $request->input('password'), (string) $user->password)) {
            return response()->json([
                'success' => false,
                'message' => 'Invalid email or password.',
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
            // Keep user for compatibility with existing clients.
            'user' => $customer,
            'token' => $token,
        ], $status)->header('X-Auth-Token', $token);
    }
}
