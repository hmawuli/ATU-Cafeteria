<?php

namespace App\Http\Middleware;

use Closure;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\Response;

final class InactivityTimeout
{
    public function handle(Request $request, Closure $next): Response
    {
        $user = $request->user();
        if ($user && strtoupper((string) $user->role) === 'STUDENT') {
            $profile = is_array($user->profile_info) ? $user->profile_info : [];
            $last = isset($profile['last_activity_at']) ? (int) $profile['last_activity_at'] : null;
            $timeout = max(60, (int) env('STUDENT_SESSION_TIMEOUT_SECONDS', 900));
            if ($last && (time() - $last) > $timeout) {
                $user->tokens()->delete();

                return response()->json(['success' => false, 'message' => 'Session expired due to inactivity. Please log in again.'], 401);
            }
            $profile['last_activity_at'] = time();
            $user->profile_info = $profile;
            $user->saveQuietly();
        }

        return $next($request);
    }
}
