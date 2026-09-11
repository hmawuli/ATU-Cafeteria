<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\User;
use App\Models\AuditLog;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Validator;
use Illuminate\Support\Facades\Hash;
use Illuminate\Support\Facades\DB;

class StudentAuthController extends Controller
{
    /**
     * Register a new student in the system.
     */
    public function register(Request $request)
    {
        $validator = Validator::make($request->all(), [
            'username' => 'required|string|max:255',
            'pin' => 'required|string|min:4|max:128',
            'fullName' => 'required|string|max:255',
            'email' => 'nullable|email|max:255',
            'studentId' => 'required|string|max:255', // Maps to 'info' field
        ]);

        if ($validator->fails()) {
            return response()->json([
                'success' => false,
                'message' => 'Student registration input validation failed.',
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

        // Create the student user inside a transaction
        $user = DB::transaction(function () use ($request) {
            $createdUser = User::create([
                'username' => $request->input('username'),
                'password' => Hash::make($request->input('pin')),
                'role' => 'STUDENT',
                'fullName' => $request->input('fullName'),
                'info' => $request->input('studentId'),
                'profile_info' => array_filter(['email' => $request->input('email')], fn ($value) => filled($value)),
            ]);

            // Register Audit Log
            AuditLog::create([
                'user_id' => $createdUser->id,
                'timestamp' => time() * 1000,
                'action' => 'STUDENT_REGISTRATION',
                'details' => "Registered student {$createdUser->fullName} (ID: {$createdUser->info}) via Student API.",
            ]);

            return $createdUser;
        });

        // Generate Laravel Sanctum token
        $token = $user->createToken('student_token', ['student'])->plainTextToken;
        $response = $user->toArray();
        $response['token'] = $token;

        return response()->json([
            'success' => true,
            'message' => 'Student registered successfully.',
            'user' => $response
        ], 201)->header('X-Auth-Token', $token);
    }

    /**
     * Authenticate a student user and return a Sanctum access token.
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

        // Enforce student role and valid credentials
        if (!$user || $user->role !== 'STUDENT') {
            return response()->json([
                'success' => false,
                'message' => 'Invalid student credentials or unauthorized role.'
            ], 401);
        }

        // Compare direct PIN (Pre-hashed from clients)
        if (Hash::check($request->input('pin'), (string) $user->password) || Hash::check(hash('sha256', $request->input('pin')), (string) $user->password) || hash_equals((string) $user->password, hash('sha256', $request->input('pin')))) {
            if (!Hash::check($request->input('pin'), (string) $user->password)) { $user->password = Hash::make($request->input('pin')); $user->saveQuietly(); }
            // Register Audit Log
            AuditLog::create([
                'user_id' => $user->id,
                'timestamp' => time() * 1000,
                'action' => 'STUDENT_AUTHENTICATION',
                'details' => "Student {$user->fullName} logged in successfully via Sanctum.",
            ]);

            // Generate Laravel Sanctum token
            $token = $user->createToken('student_token', ['student'])->plainTextToken;
            $responseData = $user->toArray();
            $responseData['token'] = $token;

            return response()->json([
                'success' => true,
                'message' => 'Student logged in successfully.',
                'user' => $responseData
            ], 200)->header('X-Auth-Token', $token);
        }

        // Record failed login attempt
        AuditLog::create([
            'user_id' => $user->id,
            'timestamp' => time() * 1000,
            'action' => 'STUDENT_AUTH_FAILURE',
            'details' => "Failed student login attempt with wrong PIN.",
        ]);

        return response()->json([
            'success' => false,
            'message' => 'Invalid Pin-Code hash.'
        ], 401);
    }
}
