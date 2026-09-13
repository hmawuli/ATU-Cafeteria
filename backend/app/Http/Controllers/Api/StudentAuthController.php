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
            return response()->json(['success' => false, 'message' => 'That username is already in use.'], 409);
        }

        $emailExists = DB::connection()->getDriverName() === 'sqlite'
            ? User::whereRaw("json_extract(profile_info, '$.email') = ?", [$email])->exists()
            : User::whereRaw("JSON_UNQUOTE(JSON_EXTRACT(profile_info, '$.email')) = ?", [$email])->exists();

        if ($emailExists) {
            return response()->json(['success' => false, 'message' => 'An account with that email already exists.'], 409);
        }

        $user = DB::transaction(function () use ($request, $username, $email) {
            $createdUser = User::create([
                'username' => $username,
                'password' => Hash::make((string) $request->input('pin')),
                'role' => 'STUDENT',
                'fullName' => trim((string) $request->input('fullName')),
                'info' => trim((string) $request->input('info', '')),
                'profile_info' => ['email' => $email],
            ]);

            AuditLog::create([
                'user_id' => $createdUser->id,
                'timestamp' => time() * 1000,
                'action' => 'STUDENT_REGISTRATION',
                'details' => "Registered student {$createdUser->fullName}.",
            ]);

            return $createdUser;
        });

        $token = $user->createToken('student_token', ['student'])->plainTextToken;
        $response = $user->toArray();
        $response['token'] = $token;

        return response()->json([
            'success' => true,
            'message' => 'Student registered successfully.',
            'user' => $response,
        ], 201)->header('X-Auth-Token', $token);
    }

    /**
     * Authenticate a student user and return a Sanctum access token.
     */
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

        if (!$user || !Hash::check((string) $request->input('pin'), (string) $user->password)) {
            if ($user) {
                AuditLog::create([
                    'user_id' => $user->id,
                    'timestamp' => time() * 1000,
                    'action' => 'STUDENT_AUTH_FAILURE',
                    'details' => 'Failed student username/PIN login attempt.',
                ]);
            }
            return response()->json(['success' => false, 'message' => 'Invalid student username or PIN.'], 401);
        }

        if (!$user->isActive()) {
            return response()->json(['success' => false, 'message' => 'Your account is not active.'], 403);
        }

        $user->last_login_at = now();
        $user->saveQuietly();
        $user->tokens()->delete();
        $token = $user->createToken('student_token', ['student'])->plainTextToken;

        AuditLog::create([
            'user_id' => $user->id,
            'timestamp' => time() * 1000,
            'action' => 'STUDENT_AUTHENTICATION',
            'details' => 'Student logged in successfully via Sanctum.',
        ]);

        $responseData = $user->toArray();
        $responseData['token'] = $token;

        return response()->json([
            'success' => true,
            'message' => 'Student logged in successfully.',
            'user' => $responseData,
        ], 200)->header('X-Auth-Token', $token);
    }
}
