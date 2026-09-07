<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\AuditLog;
use App\Models\User;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Hash;
use Illuminate\Support\Facades\Validator;

class StudentAuthController extends Controller
{
    public function register(Request $request)
    {
        $validator = Validator::make($request->all(), [
            'username' => 'required|string|max:255|unique:users,username',
            'pin' => 'required|string|min:4|max:128',
            'fullName' => 'required|string|max:255',
            'studentId' => 'required|string|max:255|unique:users,student_staff_id',
        ]);

        if ($validator->fails()) {
            return response()->json(['success' => false, 'message' => 'Student registration validation failed.', 'errors' => $validator->errors()], 422);
        }

        $user = DB::transaction(function () use ($request) {
            $created = User::create([
                'username' => $request->input('username'),
                'password' => Hash::make($request->input('pin')),
                'role' => 'STUDENT',
                'fullName' => $request->input('fullName'),
                'student_staff_id' => $request->input('studentId'),
                'info' => $request->input('studentId'),
            ]);

            AuditLog::create([
                'user_id' => $created->id,
                'timestamp' => time() * 1000,
                'action' => 'STUDENT_REGISTRATION',
                'details' => "Registered student {$created->fullName}.",
            ]);

            return $created;
        });

        return $this->issueToken($user, 'Student registered successfully.', 201);
    }

    public function login(Request $request)
    {
        $validator = Validator::make($request->all(), [
            'username' => 'required|string|max:255',
            'pin' => 'required|string|max:128',
        ]);

        if ($validator->fails()) {
            return response()->json(['success' => false, 'message' => 'Username and PIN are required.', 'errors' => $validator->errors()], 422);
        }

        $user = User::where('username', $request->input('username'))->where('role', 'STUDENT')->first();
        if (!$user || !$this->verifyAndUpgradePassword($user, $request->input('pin'))) {
            return response()->json(['success' => false, 'message' => 'Invalid student credentials.'], 401);
        }

        AuditLog::create([
            'user_id' => $user->id,
            'timestamp' => time() * 1000,
            'action' => 'STUDENT_AUTHENTICATION',
            'details' => 'Successful student authentication.',
        ]);

        return $this->issueToken($user, 'Student logged in successfully.', 200);
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

        $legacyHash = hash('sha256', $pin);
        if (hash_equals((string) $user->password, $legacyHash)) {
            $user->password = Hash::make($pin);
            $user->save();
            return true;
        }

        return false;
    }

    private function issueToken(User $user, string $message, int $status)
    {
        $token = $user->createToken('atu_cafeteria_student_token', ['student'])->plainTextToken;
        return response()->json([
            'success' => true,
            'message' => $message,
            'user' => $user,
            'token' => $token,
        ], $status)->header('X-Auth-Token', $token);
    }
}
