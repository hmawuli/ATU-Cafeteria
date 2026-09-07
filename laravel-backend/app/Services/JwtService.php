<?php

namespace App\Services;

use App\Models\User;
use LogicException;

/**
 * Deprecated compatibility shim.
 *
 * Authentication is now exclusively handled by Laravel Sanctum. This class
 * remains only because older route code may reference its methods; it never
 * accepts or generates JWT credentials and contains no fallback secret.
 */
class JwtService
{
    public static function generateToken(User $user, int $expirySeconds = 0): string
    {
        throw new LogicException('JWT authentication has been disabled. Use Laravel Sanctum personal access tokens.');
    }

    public static function validateToken(string $token): ?array
    {
        return null;
    }

    public static function getUserFromToken(string $token): ?User
    {
        return null;
    }
}
