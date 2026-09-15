<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\AuditLog;
use App\Models\AuthVerificationCode;
use App\Models\User;
use App\Notifications\AuthenticationCodeNotification;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Hash;
use Illuminate\Support\Facades\Validator;

class AuthController extends Controller
{
    private function verifyCredential(User $user, string $credential): bool
    {
        $stored = (string) $user->password;
        if (Hash::check($credential, $stored)) {
            return true;
        }

        // Migrate credentials created by earlier Flutter clients that sent SHA-256.
        $legacySha256 = hash('sha256', $credential);
        if (Hash::check($legacySha256, $stored) || hash_equals($stored, $legacySha256)) {
            $user->password = Hash::make($credential);
            $user->saveQuietly();

            return true;
        }

        return false;
    }

    private function issueCode(User $user, string $purpose): string
    {
        AuthVerificationCode::where('username', $user->username)
            ->where('purpose', $purpose)
            ->whereNull('used_at')
            ->update(['used_at' => now()]);

        $code = (string) random_int(100000, 999999);
        AuthVerificationCode::create([
            'user_id' => $user->id,
            'username' => $user->username,
            'purpose' => $purpose,
            'code_hash' => Hash::make($code),
            'expires_at' => now()->addMinutes(10),
        ]);
        $user->notify(new AuthenticationCodeNotification($purpose, $code));

        return $code;
    }

    private function verifyCode(string $username, string $purpose, string $code): ?AuthVerificationCode
    {
        $record = AuthVerificationCode::where('username', $username)
            ->where('purpose', $purpose)
            ->whereNull('used_at')
            ->where('expires_at', '>', now())
            ->latest()->first();
        if (! $record || $record->attempts >= 5) {
            return null;
        }
        $record->increment('attempts');
        if (! Hash::check($code, $record->code_hash)) {
            return null;
        }
        $record->update(['used_at' => now()]);

        return $record;
    }

    /**
     * Register a new user in the cafeteria system.
     */
    public function register(Request $request)
    {
        $validator = Validator::make($request->all(), [
            'email' => 'required|email|max:255',
            'password' => 'required|string|min:8|max:128',
            'role' => 'required|string|in:STUDENT',
            'fullName' => 'required|string|max:255',
            'info' => 'nullable|string|max:1000',
        ]);

        if ($validator->fails()) {
            return response()->json([
                'success' => false,
                'message' => 'Please provide a valid name, email address, and password for a student account.',
                'errors' => $validator->errors(),
            ], 422);
        }

        $email = strtolower(trim($request->input('email')));
        if (User::where('username', $email)
            ->orWhereRaw("JSON_UNQUOTE(JSON_EXTRACT(profile_info, '$.email')) = ?", [$email])
            ->exists()) {
            return response()->json([
                'success' => false,
                'message' => 'An account with this email address already exists.',
            ], 409);
        }

        $user = DB::transaction(function () use ($request, $email) {
            $user = User::create([
                'username' => $email,
                'password' => Hash::make($request->input('password')),
                'role' => strtoupper($request->input('role')),
                'fullName' => trim($request->input('fullName')),
                'info' => trim((string) $request->input('info', '')),
                'profile_info' => ['email' => $email],
                'account_status' => 'ACTIVE',
            ]);

            AuditLog::create([
                'user_id' => $user->id,
                'timestamp' => time() * 1000,
                'action' => 'USER_REGISTRATION',
                'details' => "Registered {$user->fullName} as {$user->role} via Laravel API.",
            ]);

            return $user;
        });

        $token = $user->createToken(strtolower($user->role).'_token', [strtolower($user->role)])->plainTextToken;

        return response()->json([
            'success' => true,
            'message' => 'Account created successfully.',
            'user' => $user,
            'token' => $token,
        ], 201)->header('X-Auth-Token', $token);
    }

    /**
     * Authenticate and return the user profile.
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
        $user = User::whereRaw('LOWER(username) = ?', [strtolower($username)])->first();

        if (! $user || ! $this->verifyCredential($user, (string) $request->input('pin'))) {
            if ($user) {
                AuditLog::create([
                    'user_id' => $user->id,
                    'timestamp' => time() * 1000,
                    'action' => 'AUTH_FAILURE',
                    'details' => 'Failed username/PIN login attempt.',
                ]);
            }

            return response()->json(['success' => false, 'message' => 'Invalid username or PIN.'], 401);
        }

        if (! $user->isActive()) {
            return response()->json(['success' => false, 'message' => 'Your account is not active.'], 403);
        }

        if (strtoupper((string) $user->role) === 'ADMIN' && $user->two_factor_enabled) {
            $this->issueCode($user, 'ADMIN_2FA');
            AuditLog::create([
                'user_id' => $user->id,
                'timestamp' => time() * 1000,
                'action' => 'AUTH_2FA_CHALLENGE',
                'details' => 'Admin login requires two-factor verification.',
            ]);

            return response()->json([
                'success' => true,
                'requires_2fa' => true,
                'message' => 'A verification code has been sent to the administrator email address.',
                'user' => $user,
            ]);
        }

        return $this->issueSession($user);
    }

    private function issueSession(User $user)
    {
        $user->last_login_at = now();
        $user->saveQuietly();
        $user->tokens()->delete();
        $abilities = [strtolower((string) $user->role)];
        $token = $user->createToken(strtolower((string) $user->role).'_token', $abilities)->plainTextToken;
        AuditLog::create([
            'user_id' => $user->id, 'timestamp' => time() * 1000,
            'action' => 'USER_AUTHENTICATION', 'details' => 'Successful login via Sanctum.',
        ]);

        return response()->json([
            'success' => true, 'message' => 'Login successful.',
            'user' => $user, 'token' => $token,
        ])->header('X-Auth-Token', $token);
    }

    public function verifyTwoFactor(Request $request)
    {
        $data = Validator::make($request->all(), [
            'username' => 'required|string|max:255',
            'code' => 'required|digits:6',
        ]);
        if ($data->fails()) {
            return response()->json(['success' => false, 'message' => 'A valid six-digit code is required.', 'errors' => $data->errors()], 422);
        }
        $user = User::where('username', trim($request->username))->where('role', 'ADMIN')->first();
        if (! $user || ! $user->isActive() || ! $user->two_factor_enabled) {
            return response()->json(['success' => false, 'message' => 'Invalid verification request.'], 403);
        }
        if (! $this->verifyCode($user->username, 'ADMIN_2FA', $request->code)) {
            return response()->json(['success' => false, 'message' => 'Invalid or expired verification code.'], 401);
        }
        AuditLog::create(['user_id' => $user->id, 'timestamp' => time() * 1000, 'action' => 'AUTH_2FA_SUCCESS', 'details' => 'Administrator completed two-factor verification.']);

        return $this->issueSession($user);
    }

    public function forgotPassword(Request $request)
    {
        $request->validate(['username' => 'required|string|min:3|max:100']);
        $username = trim((string) $request->input('username'));
        $user = User::whereRaw('LOWER(username) = ?', [strtolower($username)])->first();

        if ($user && $user->isActive()) {
            $this->issueCode($user, 'PASSWORD_RESET');
            AuditLog::create([
                'user_id' => $user->id,
                'timestamp' => time() * 1000,
                'action' => 'PASSWORD_RESET_REQUEST',
                'details' => 'PIN reset code requested.',
            ]);
        }

        return response()->json([
            'success' => true,
            'message' => 'If the account exists, a reset code has been sent.',
        ]);
    }

    public function resetPassword(Request $request)
    {
        $validator = Validator::make($request->all(), [
            'username' => 'required|string|min:3|max:100',
            'code' => 'required|digits:6',
            'pin' => 'required|digits_between:4,6',
        ]);
        if ($validator->fails()) {
            return response()->json([
                'success' => false,
                'message' => 'Invalid PIN reset details.',
                'errors' => $validator->errors(),
            ], 422);
        }

        $username = trim((string) $request->input('username'));
        $user = User::whereRaw('LOWER(username) = ?', [strtolower($username)])->first();

        if (! $user || ! $user->isActive() ||
            ! $this->verifyCode($user->username, 'PASSWORD_RESET', $request->input('code'))) {
            return response()->json(['success' => false, 'message' => 'Invalid or expired reset code.'], 401);
        }

        $user->password = Hash::make((string) $request->input('pin'));
        $user->saveQuietly();
        $user->tokens()->delete();

        AuditLog::create([
            'user_id' => $user->id,
            'timestamp' => time() * 1000,
            'action' => 'PIN_CHANGED',
            'details' => 'PIN reset completed; all existing sessions were revoked.',
        ]);

        return response()->json(['success' => true, 'message' => 'PIN changed successfully.']);
    }

    public function requestEmailVerification(Request $request)
    {
        $user = $request->user();
        if (! $user || ! $user->isActive()) {
            return response()->json(['success' => false, 'message' => 'Authenticated active account required.'], 401);
        }
        $email = $user->emailAddress();
        if (! $email) {
            return response()->json(['success' => false, 'message' => 'Add a valid email address to your profile first.'], 422);
        }
        if ($user->email_verified_at) {
            return response()->json(['success' => true, 'message' => 'Email address is already verified.']);
        }
        $this->issueCode($user, 'EMAIL_VERIFY');
        AuditLog::create(['user_id' => $user->id, 'timestamp' => time() * 1000, 'action' => 'EMAIL_VERIFICATION_REQUESTED', 'details' => 'Email verification code requested.']);

        return response()->json(['success' => true, 'message' => 'A verification code has been sent to your email address.']);
    }

    public function verifyEmail(Request $request)
    {
        $request->validate(['code' => 'required|digits:6']);
        $user = $request->user();
        if (! $user || ! $user->isActive()) {
            return response()->json(['success' => false, 'message' => 'Authenticated active account required.'], 401);
        }
        if (! $this->verifyCode($user->username, 'EMAIL_VERIFY', $request->code)) {
            return response()->json(['success' => false, 'message' => 'Invalid or expired verification code.'], 401);
        }
        $user->email_verified_at = now();
        $user->saveQuietly();
        AuditLog::create(['user_id' => $user->id, 'timestamp' => time() * 1000, 'action' => 'EMAIL_VERIFIED', 'details' => 'User email address verified.']);

        return response()->json(['success' => true, 'message' => 'Email address verified successfully.', 'user' => $user]);
    }

    public function requestTwoFactorEnable(Request $request)
    {
        $user = $request->user();
        if (! $user || strtoupper((string) $user->role) !== 'ADMIN') {
            return response()->json(['success' => false, 'message' => 'Administrator access required.'], 403);
        }
        $this->issueCode($user, 'ADMIN_2FA_ENABLE');

        return response()->json(['success' => true, 'message' => 'A verification code has been sent to your administrator email address.']);
    }

    public function enableTwoFactor(Request $request)
    {
        $request->validate(['code' => 'required|digits:6']);
        $user = $request->user();
        if (! $user || strtoupper((string) $user->role) !== 'ADMIN') {
            return response()->json(['success' => false, 'message' => 'Administrator access required.'], 403);
        }
        if (! $this->verifyCode($user->username, 'ADMIN_2FA_ENABLE', $request->code)) {
            return response()->json(['success' => false, 'message' => 'Invalid or expired verification code.'], 401);
        }
        $user->two_factor_enabled = true;
        $user->saveQuietly();
        AuditLog::create(['user_id' => $user->id, 'timestamp' => time() * 1000, 'action' => '2FA_ENABLED', 'details' => 'Administrator enabled two-factor authentication.']);

        return response()->json(['success' => true, 'message' => 'Two-factor authentication is now enabled.']);
    }

    public function disableTwoFactor(Request $request)
    {
        $request->validate(['pin' => 'required|string|min:4|max:128']);
        $user = $request->user();
        if (! $user || ! $user->isSuperAdmin()) {
            return response()->json(['success' => false, 'message' => 'Only the Super Admin can disable administrator two-factor authentication.'], 403);
        }
        $valid = $this->verifyCredential($user, (string) $request->pin);
        if (! $valid) {
            return response()->json(['success' => false, 'message' => 'Invalid credentials.'], 401);
        }
        $user->two_factor_enabled = false;
        $user->saveQuietly();
        AuditLog::create(['user_id' => $user->id, 'timestamp' => time() * 1000, 'action' => '2FA_DISABLED', 'details' => 'Super Admin disabled administrator two-factor authentication.']);

        return response()->json(['success' => true, 'message' => 'Two-factor authentication disabled.']);
    }

    private function permissionsFor(User $user): array|string
    {
        $role = strtoupper((string) $user->role);
        if ($role === 'ADMIN') {
            $level = strtoupper((string) ($user->admin_level ?? 'CAFETERIA_ADMIN'));

            return config("permissions.roles.$level", []);
        }

        return config('permissions.'.strtolower($role), []);
    }

    public function me(Request $request)
    {
        $user = $request->user();
        if (! $user || ! $user->isActive()) {
            return response()->json(['success' => false, 'message' => 'Unauthenticated.'], 401);
        }

        return response()->json(['success' => true, 'user' => $user, 'permissions' => $this->permissionsFor($user)]);
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
        if (! $user) {
            return response()->json([
                'success' => false,
                'message' => 'User not found.',
            ], 404);
        }

        $fullName = $user->fullName;
        $user->delete();

        return response()->json([
            'success' => true,
            'message' => "User '{$fullName}' has been erased successfully.",
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
                'message' => 'Secure Token invalidated successfully.',
            ], 200);
        }

        return response()->json([
            'success' => false,
            'message' => 'No active authenticated session.',
        ], 401);
    }

    /**
     * Update the authenticated user's profile info.
     */
    public function updateProfile(Request $request)
    {
        $user = auth()->user();
        if (! $user) {
            return response()->json([
                'success' => false,
                'message' => 'Unauthorized or no active session found.',
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
                'errors' => $validator->errors(),
            ], 400);
        }

        // Fetch user with write-safety
        $dbUser = User::find($user->id);
        if (! $dbUser) {
            return response()->json([
                'success' => false,
                'message' => 'User not found in system.',
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

        // Merge existing profile_info with the incoming values
        $currentProfileInfo = is_array($dbUser->profile_info) ? $dbUser->profile_info : [];

        $profileKeys = ['phone_number', 'email', 'department', 'program_of_study', 'payment_methods'];
        foreach ($profileKeys as $key) {
            if ($request->has($key)) {
                $currentProfileInfo[$key] = $request->input($key);
            }
        }

        $dbUser->profile_info = $currentProfileInfo;
        $dbUser->save();

        // Create audit log for profile update
        AuditLog::create([
            'user_id' => $dbUser->id,
            'timestamp' => time() * 1000,
            'action' => 'PROFILE_UPDATE',
            'details' => "Updated profile information for user: {$dbUser->username}",
        ]);

        return response()->json([
            'success' => true,
            'message' => 'Profile updated successfully.',
            'user' => $dbUser,
        ], 200);
    }
}
