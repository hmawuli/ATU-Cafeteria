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
            'email' => 'required|email|max:255',
            'password' => 'required|string|min:8|max:128',
            'fullName' => 'required|string|max:255',
        ]);

        if ($validator->fails()) {
            return response()->json([
                'success' => false,
                'message' => 'Please provide a valid email address, password, and full name.',
                'errors' => $validator->errors()
            ], 400);
        }

        // Email is the account identity; Student ID is not required.
        $email = strtolower(trim($request->input('email')));
        $existing = User::where('username', $email)
            ->orWhereRaw("JSON_UNQUOTE(JSON_EXTRACT(profile_info, '$.email')) = ?", [$email])
            ->first();
        if ($existing) {
            return response()->json([
                'success' => false,
                'message' => 'An account with this email address already exists.'
            ], 400);
        }

        // Create the student user inside a transaction
        $user = DB::transaction(function () use ($request) {
            $createdUser = User::create([
                'username' => strtolower(trim($request->input('email'))),
                'password' => Hash::make($request->input('password')),
                'role' => 'STUDENT',
                'fullName' => $request->input('fullName'),
                'info' => '',
                'profile_info' => array_filter(['email' => $request->input('email')], fn ($value) => filled($value)),
            ]);

            // Register Audit Log
            AuditLog::create([
                'user_id' => $createdUser->id,
                'timestamp' => time() * 1000,
                'action' => 'STUDENT_REGISTRATION',
                'details' => "Registered student {$createdUser->fullName} via Student API.",
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
            'email' => 'required|email|max:255',
            'password' => 'required|string|min:8|max:128',
        ]);

        if ($validator->fails()) {
            return response()->json([
                'success' => false,
                'message' => 'Email address and password are required.'
            ], 400);
        }

        $email = strtolower(trim($request->input('email')));
        $user = User::where('username', $email)
            ->orWhereRaw("JSON_UNQUOTE(JSON_EXTRACT(profile_info, '$.email')) = ?", [$email])
            ->first();

        // Enforce student role and valid credentials
        if (!$user || $user->role !== 'STUDENT') {
            return response()->json([
                'success' => false,
                'message' => 'Invalid student credentials or unauthorized role.'
            ], 401);
        }

        // Compare direct PIN (Pre-hashed from clients)
        if (Hash::check($request->input('password'), (string) $user->password)) {
            
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
            'details' => 'Failed student login attempt with invalid password.',
        ]);

        return response()->json([
            'success' => false,
            'message' => 'Invalid credentials.'
        ], 401);
    }
}
