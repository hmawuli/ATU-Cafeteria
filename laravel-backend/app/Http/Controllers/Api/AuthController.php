<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\User;
use App\Models\AuditLog;
use App\Services\JwtService;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Validator;
use Illuminate\Support\Facades\DB;

class AuthController extends Controller
{
    /**
     * Register a normal cafeteria user.
     *
     * SECURITY: ADMIN is intentionally excluded from public registration.
     * Administrator accounts must be created by an existing ADMIN.
     */
    public function register(Request $request)
    {
        $validator = Validator::make($request->all(), [
            'username' => 'required|string|max:255',
            'pin' => 'required|string|min:4', // Pre-hashed SHA-256 PIN from clients
            'role' => 'required|string|in:STUDENT,VENDOR',
            'fullName' => 'required|string|max:255',
            'info' => 'nullable|string',
        ]);

        if ($validator->fails()) {
            return response()->json([
                'success' => false,
                'message' => 'Input validation failed.',
                'errors' => $validator->errors()
            ], 400);
        }

        $existing = User::where('username', $request->input('username'))->first();
        if ($existing) {
            return response()->json([
                'success' => false,
                'message' => 'Username already exists.'
            ], 400);
        }

        $user = DB::transaction(function () use ($request) {
            $createdUser = User::create([
                'username' => $request->input('username'),
                'password' => $request->input('pin'),
                'role' => strtoupper($request->input('role')),
                'fullName' => $request->input('fullName'),
                'info' => $request->input('info') ?? '',
            ]);

            AuditLog::create([
                'user_id' => $createdUser->id,
                'timestamp' => time() * 1000,
                'action' => 'USER_REGISTRATION',
                'details' => "Registered {$createdUser->fullName} as {$createdUser->role} via Laravel API.",
            ]);

            return $createdUser;
        });

        $token = JwtService::generateToken($user);
        $response = $user->toArray();
        $response['token'] = $token;

        return response()->json($response, 201)->header('X-Auth-Token', $token);
    }

    /**
     * Create an administrator account.
     *
     * This endpoint must only be called by an already authenticated ADMIN.
     * Public registration can never self-assign the ADMIN role.
     */
    public function registerAdmin(Request $request)
    {
        $validator = Validator::make($request->all(), [
            'username' => 'required|string|max:255|unique:users,username',
            'pin' => 'required|string|min:4',
            'fullName' => 'required|string|max:255',
            'student_staff_id' => 'nullable|string|max:255|unique:users,student_staff_id',
            'info' => 'nullable|string',
        ]);

        if ($validator->fails()) {
            return response()->json([
                'success' => false,
                'message' => 'Administrator validation failed.',
                'errors' => $validator->errors(),
            ], 422);
        }

        $creator = $request->user();
        if (!$creator || strtoupper((string) $creator->role) !== 'ADMIN') {
            return response()->json([
                'success' => false,
                'message' => 'Unauthorized. Only an existing ADMIN can create another administrator.',
            ], 403);
        }

        $admin = DB::transaction(function () use ($request, $creator) {
            $created = User::create([
                'username' => $request->input('username'),
                'password' => $request->input('pin'),
                'role' => 'ADMIN',
                'fullName' => $request->input('fullName'),
                'student_staff_id' => $request->input('student_staff_id'),
                'info' => $request->input('info') ?? 'ATU Cafeteria Administration',
            ]);

            AuditLog::create([
                'user_id' => $creator->id,
                'timestamp' => time() * 1000,
                'action' => 'ADMIN_ACCOUNT_CREATED',
                'details' => "Administrator {$created->fullName} (ID {$created->id}) created by ADMIN {$creator->fullName}.",
            ]);

            return $created;
        });

        return response()->json([
            'success' => true,
            'message' => 'Administrator account created successfully.',
            'user' => $admin->toArray(),
        ], 201);
    }

    /**
     * Authenticate and return the user profile.
     */
    public function login(Request $request)
    {
        $validator = Validator::make($request->all(), [
            'username' => 'required|string',
            'pin' => 'required|string',
        ]);

        if ($validator->fails()) {
            return response()->json([
                'success' => false,
                'message' => 'Both username and pin are required.'
            ], 400);
        }

        $user = User::where('username', $request->input('username'))->first();

        if (!$user) {
            return response()->json([
                'success' => false,
                'message' => 'Invalid Username or Pass-PIN.'
            ], 401);
        }

        if ($user->password === $request->input('pin')) {
            AuditLog::create([
                'user_id' => $user->id,
                'timestamp' => time() * 1000,
                'action' => 'USER_AUTHENTICATION',
                'details' => "Successfully logged in via Laravel API.",
            ]);

            $token = JwtService::generateToken($user);
            $responseData = $user->toArray();
            $responseData['token'] = $token;

            return response()->json($responseData, 200)->header('X-Auth-Token', $token);
        }

        AuditLog::create([
            'user_id' => $user->id,
            'timestamp' => time() * 1000,
            'action' => 'AUTH_FAILURE',
            'details' => "Failed login attempt with wrong PIN on Laravel API.",
        ]);

        return response()->json([
            'success' => false,
            'message' => 'Invalid Username or Pass-PIN.'
        ], 401);
    }

    /**
     * Retrieve all users. Route is ADMIN-only.
     */
    public function getAllUsers()
    {
        return response()->json(User::all(), 200);
    }

    /**
     * Delete user by ID. Route is ADMIN-only.
     */
    public function deleteUser($id)
    {
        $user = User::find($id);
        if (!$user) {
            return response()->json([
                'success' => false,
                'message' => 'User not found.'
            ], 404);
        }

        $fullName = $user->fullName;
        $user->delete();

        return response()->json([
            'success' => true,
            'message' => "User '{$fullName}' has been erased successfully."
        ], 200);
    }

    /**
     * Terminate session and revoke Sanctum access tokens.
     */
    public function logout(Request $request)
    {
        $user = $request->user();
        if ($user) {
            try {
                if (method_exists($user, 'currentAccessToken') && $user->currentAccessToken()) {
                    $user->currentAccessToken()->delete();
                }
            } catch (\Exception $e) {
                // Ignore exception if using stateless JWT
            }
            return response()->json([
                'success' => true,
                'message' => 'Secure Token invalidated successfully.'
            ], 200);
        }
        return response()->json([
            'success' => false,
            'message' => 'No active authenticated session.'
        ], 401);
    }

    /**
     * Update the authenticated user's profile info.
     */
    public function updateProfile(Request $request)
    {
        $user = auth()->user();
        if (!$user) {
            return response()->json([
                'success' => false,
                'message' => 'Unauthorized or no active session found.'
            ], 401);
        }

        $validator = Validator::make($request->all(), [
            'fullName' => 'nullable|string|max:255',
            'student_staff_id' => 'nullable|string|max:255',
            'phone_number' => 'nullable|string|max:255',
            'email' => 'nullable|string|max:255',
            'department' => 'nullable|string|max:255',
            'program_of_study' => 'nullable|string|max:255',
            'payment_methods' => 'nullable|array',
            'info' => 'nullable|string',
        ]);

        if ($validator->fails()) {
            return response()->json([
                'success' => false,
                'message' => 'Profile validation failed.',
                'errors' => $validator->errors()
            ], 400);
        }

        $dbUser = User::find($user->id);
        if (!$dbUser) {
            return response()->json([
                'success' => false,
                'message' => 'User not found in system.'
            ], 404);
        }

        if ($request->has('fullName')) {
            $dbUser->fullName = $request->input('fullName');
        }
        if ($request->has('student_staff_id')) {
            $dbUser->student_staff_id = $request->input('student_staff_id');
        }
        if ($request->has('info')) {
            $dbUser->info = $request->input('info');
        }

        $profile = is_array($dbUser->profile_info) ? $dbUser->profile_info : [];
        foreach (['phone_number', 'email', 'department', 'program_of_study', 'payment_methods'] as $field) {
            if ($request->has($field)) {
                $profile[$field] = $request->input($field);
            }
        }
        $dbUser->profile_info = $profile;
        $dbUser->save();

        AuditLog::create([
            'user_id' => $dbUser->id,
            'timestamp' => time() * 1000,
            'action' => 'PROFILE_UPDATED',
            'details' => "Updated profile for {$dbUser->fullName}.",
        ]);

        return response()->json([
            'success' => true,
            'message' => 'Profile updated successfully.',
            'user' => $dbUser->fresh(),
        ], 200);
    }
}
