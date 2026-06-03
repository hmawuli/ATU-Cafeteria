<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\User;
use App\Models\AuditLog;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Validator;

use Illuminate\Support\Facades\DB;

class AuthController extends Controller
{
    /**
     * Register a new user in the cafeteria system.
     */
    public function register(Request $request)
    {
        $validator = Validator::make($request->all(), [
            'username' => 'required|string|max:255',
            'pin' => 'required|string|min:4', // Pre-hashed SHA-256 PIN from android
            'role' => 'required|string|in:STUDENT,VENDOR,ADMIN',
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

        // Check if username already exists
        $existing = User::where('username', $request->input('username'))->first();
        if ($existing) {
            return response()->json([
                'success' => false,
                'message' => 'Username already exists.'
            ], 400);
        }

        // Create safety rollback boundary for registration
        $user = DB::transaction(function () use ($request) {
            $createdUser = User::create([
                'username' => $request->input('username'),
                'password' => $request->input('pin'), // Stored pre-hashed
                'role' => $request->input('role'),
                'fullName' => $request->input('fullName'),
                'info' => $request->input('info') ?? '',
            ]);

            // Register Audit Log
            AuditLog::create([
                'user_id' => $createdUser->id,
                'timestamp' => time() * 1000,
                'action' => 'USER_REGISTRATION',
                'details' => "Registered {$createdUser->fullName} as {$createdUser->role} via Laravel API.",
            ]);

            return $createdUser;
        });

        // Generate Sanctum access token
        $token = $user->createToken('cafeteria-token')->plainTextToken;
        $response = $user->toArray();
        $response['token'] = $token;

        return response()->json($response, 201)->header('X-Auth-Token', $token);
    }

    /**
     * Authenticate and return the user profile.
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

        if (!$user) {
            return response()->json([
                'success' => false,
                'message' => 'Invalid Username or Pass-PIN.'
            ], 401);
        }

        // Compares directly. Since the android client hashes the raw pin with SHA-256,
        // we store and compare the pre-hashed SHA-256 strings directly for absolute simplicity.
        if ($user->password === $request->input('pin')) {
            // Register Audit Log
            AuditLog::create([
                'user_id' => $user->id,
                'timestamp' => time() * 1000,
                'action' => 'USER_AUTHENTICATION',
                'details' => "Successfully logged in via Laravel API.",
            ]);

            // Secure token issue
            $token = $user->createToken('cafeteria-token')->plainTextToken;
            $responseData = $user->toArray();
            $responseData['token'] = $token;

            return response()->json($responseData, 200)->header('X-Auth-Token', $token);
        }

        // Record failed login attempt
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
     * Retrieve all users.
     */
    public function getAllUsers()
    {
        $users = User::all();
        return response()->json($users, 200);
    }

    /**
     * Delete user by ID.
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
            $user->currentAccessToken()->delete();
            return response()->json([
                'success' => true,
                'message' => 'Secure Sanctum Token revoked successfully.'
            ], 200);
        }
        return response()->json([
            'success' => false,
            'message' => 'No active authenticated session.'
        ], 401);
    }
}
