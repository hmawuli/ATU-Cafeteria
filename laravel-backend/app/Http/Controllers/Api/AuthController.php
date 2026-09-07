<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\AuditLog;
use App\Models\User;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Hash;
use Illuminate\Support\Facades\Validator;

class AuthController extends Controller
{
    /**
     * Public registration. ADMIN can never be self-assigned here.
     */
    public function register(Request $request)
    {
        $validator = Validator::make($request->all(), [
            'username' => 'required|string|max:255|unique:users,username',
            'pin' => 'required|string|min:4|max:128',
            'role' => 'required|string|in:STUDENT,VENDOR',
            'fullName' => 'required|string|max:255',
            'info' => 'nullable|string|max:1000',
        ]);

        if ($validator->fails()) {
            return response()->json([
                'success' => false,
                'message' => 'Input validation failed.',
                'errors' => $validator->errors(),
            ], 422);
        }

        $user = DB::transaction(function () use ($request) {
            $created = User::create([
                'username' => $request->input('username'),
                'password' => Hash::make($request->input('pin')),
                'role' => strtoupper($request->input('role')),
                'fullName' => $request->input('fullName'),
                'info' => $request->input('info', ''),
            ]);

            AuditLog::create([
                'user_id' => $created->id,
                'timestamp' => time() * 1000,
                'action' => 'USER_REGISTRATION',
                'details' => "Registered {$created->fullName} as {$created->role}.",
            ]);

            return $created;
        });

        return $this->tokenResponse($user, 'Registration successful.', 201);
    }

    /**
     * Create an administrator. The route is protected by Sanctum + admin middleware.
     */
    public function registerAdmin(Request $request)
    {
        $creator = $request->user();
        if (!$creator || strtoupper((string) $creator->role) !== 'ADMIN') {
            return response()->json([
                'success' => false,
                'message' => 'Forbidden. Only an authenticated ADMIN can create administrators.',
            ], 403);
        }

        $validator = Validator::make($request->all(), [
            'username' => 'required|string|max:255|unique:users,username',
            'pin' => 'required|string|min:4|max:128',
            'fullName' => 'required|string|max:255',
            'student_staff_id' => 'nullable|string|max:255|unique:users,student_staff_id',
            'info' => 'nullable|string|max:1000',
        ]);

        if ($validator->fails()) {
            return response()->json([
                'success' => false,
                'message' => 'Administrator validation failed.',
                'errors' => $validator->errors(),
            ], 422);
        }

        $admin = DB::transaction(function () use ($request, $creator) {
            $created = User::create([
                'username' => $request->input('username'),
                'password' => Hash::make($request->input('pin')),
                'role' => 'ADMIN',
                'fullName' => $request->input('fullName'),
                'student_staff_id' => $request->input('student_staff_id'),
                'info' => $request->input('info', 'ATU Cafeteria Administration'),
            ]);

            AuditLog::create([
                'user_id' => $creator->id,
                'timestamp' => time() * 1000,
                'action' => 'ADMIN_ACCOUNT_CREATED',
                'details' => "Administrator account {$created->username} (ID {$created->id}) created by ADMIN {$creator->username}.",
            ]);

            return $created;
        });

        return response()->json([
            'success' => true,
            'message' => 'Administrator account created successfully.',
            'user' => $admin,
        ], 201);
    }

    /**
     * Authenticate with the raw PIN over HTTPS. Laravel hashes it server-side.
     * A legacy SHA-256 value is accepted once and upgraded to a modern password hash.
     */
    public function login(Request $request)
    {
        $validator = Validator::make($request->all(), [
            'username' => 'required|string|max:255',
            'pin' => 'required|string|max:128',
        ]);

        if ($validator->fails()) {
            return response()->json([
                'success' => false,
                'message' => 'Username and PIN are required.',
                'errors' => $validator->errors(),
            ], 422);
        }

        $user = User::where('username', $request->input('username'))->first();
        if (!$user || !$this->verifyAndUpgradePassword($user, $request->input('pin'))) {
            if ($user) {
                AuditLog::create([
                    'user_id' => $user->id,
                    'timestamp' => time() * 1000,
                    'action' => 'AUTH_FAILURE',
                    'details' => 'Failed authentication attempt.',
                ]);
            }

            return response()->json([
                'success' => false,
                'message' => 'Invalid username or PIN.',
            ], 401);
        }

        AuditLog::create([
            'user_id' => $user->id,
            'timestamp' => time() * 1000,
            'action' => 'USER_AUTHENTICATION',
            'details' => 'Successful authentication.',
        ]);

        return $this->tokenResponse($user, 'Login successful.', 200);
    }

    private function verifyAndUpgradePassword(User $user, string $pin): bool
    {
        if (Hash::check($pin, (string) $user->password)) {
            if (Hash::needsRehash((string) $user->password)) {
                $user->password = Hash::make($pin);
                $user->save();
            }
            return true;
        }

        // One-time migration path for accounts created by the old SHA-256 scheme.
        $legacyHash = hash('sha256', $pin);
        if (hash_equals((string) $user->password, $legacyHash)) {
            $user->password = Hash::make($pin);
            $user->save();
            return true;
        }

        return false;
    }

    private function tokenResponse(User $user, string $message, int $status)
    {
        $ability = match (strtoupper((string) $user->role)) {
            'ADMIN' => 'admin',
            'VENDOR' => 'vendor',
            default => 'student',
        };

        $token = $user->createToken('atu_cafeteria_'.$ability.'_token', [$ability])->plainTextToken;

        return response()->json([
            'success' => true,
            'message' => $message,
            'user' => $user,
            'token' => $token,
        ], $status)->header('X-Auth-Token', $token);
    }

    public function getAllUsers(Request $request)
    {
        return response()->json([
            'success' => true,
            'users' => User::orderBy('role')->orderBy('id')->get(),
        ]);
    }

    public function deleteUser(Request $request, $id)
    {
        $admin = $request->user();
        $user = User::find($id);

        if (!$user) {
            return response()->json(['success' => false, 'message' => 'User not found.'], 404);
        }

        if ($admin && $admin->id === $user->id) {
            return response()->json(['success' => false, 'message' => 'An administrator cannot delete their own active account.'], 422);
        }

        $fullName = $user->fullName;
        DB::transaction(function () use ($user, $admin, $fullName) {
            $user->tokens()->delete();
            $user->delete();
            AuditLog::create([
                'user_id' => $admin->id,
                'timestamp' => time() * 1000,
                'action' => 'ADMIN_USER_DELETED',
                'details' => "Administrator deleted user '{$fullName}'.",
            ]);
        });

        return response()->json(['success' => true, 'message' => "User '{$fullName}' deleted successfully."]);
    }

    public function logout(Request $request)
    {
        $user = $request->user();
        if (!$user) {
            return response()->json(['success' => false, 'message' => 'Unauthenticated.'], 401);
        }

        $token = method_exists($user, 'currentAccessToken') ? $user->currentAccessToken() : null;
        if ($token) {
            $token->delete();
        }

        AuditLog::create([
            'user_id' => $user->id,
            'timestamp' => time() * 1000,
            'action' => 'USER_LOGOUT',
            'details' => 'User logged out and current access token was revoked.',
        ]);

        return response()->json(['success' => true, 'message' => 'Logged out successfully.']);
    }

    public function updateProfile(Request $request)
    {
        $user = $request->user();
        if (!$user) {
            return response()->json(['success' => false, 'message' => 'Unauthenticated.'], 401);
        }

        $validator = Validator::make($request->all(), [
            'fullName' => 'nullable|string|max:255',
            'student_staff_id' => 'nullable|string|max:255',
            'phone_number' => 'nullable|string|max:255',
            'email' => 'nullable|string|max:255',
            'department' => 'nullable|string|max:255',
            'program_of_study' => 'nullable|string|max:255',
            'payment_methods' => 'nullable|array',
            'info' => 'nullable|string|max:1000',
        ]);

        if ($validator->fails()) {
            return response()->json(['success' => false, 'message' => 'Profile validation failed.', 'errors' => $validator->errors()], 422);
        }

        if ($request->has('fullName')) $user->fullName = $request->input('fullName');
        if ($request->has('student_staff_id')) $user->student_staff_id = $request->input('student_staff_id');
        if ($request->has('info')) $user->info = $request->input('info');

        $profile = is_array($user->profile_info) ? $user->profile_info : [];
        foreach (['phone_number', 'email', 'department', 'program_of_study', 'payment_methods'] as $key) {
            if ($request->has($key)) $profile[$key] = $request->input($key);
        }
        $user->profile_info = $profile;
        $user->save();

        AuditLog::create([
            'user_id' => $user->id,
            'timestamp' => time() * 1000,
            'action' => 'PROFILE_UPDATE',
            'details' => "Updated profile information for {$user->username}.",
        ]);

        return response()->json(['success' => true, 'message' => 'Profile updated successfully.', 'user' => $user]);
    }
}
