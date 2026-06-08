<?php

namespace App\Services;

use App\Models\User;
use Exception;

class JwtService
{
    /**
     * Get the secret key used for singing tokens.
     */
    protected static function getSecret(): string
    {
        return env('JWT_SECRET') ?: (env('APP_KEY') ?: 'atu-cafeteria-management-system-jwt-secret-key-signature-256');
    }

    /**
     * Generate a signed JWT token for a given user.
     */
    public static function generateToken(User $user, int $expirySeconds = 86400 * 30): string
    {
        $header = json_encode([
            'alg' => 'HS256',
            'typ' => 'JWT'
        ]);

        $now = time();
        $payload = json_encode([
            'iss' => 'atu-cafeteria', // Issuer Name
            'sub' => $user->id,        // Subject (user ID)
            'username' => $user->username,
            'role' => $user->role,
            'iat' => $now,             // Issued at timestamp
            'exp' => $now + $expirySeconds, // Expiration timestamp
        ]);

        $base64UrlHeader = self::base64UrlEncode($header);
        $base64UrlPayload = self::base64UrlEncode($payload);

        $signature = hash_hmac('sha256', $base64UrlHeader . "." . $base64UrlPayload, self::getSecret(), true);
        $base64UrlSignature = self::base64UrlEncode($signature);

        return $base64UrlHeader . "." . $base64UrlPayload . "." . $base64UrlSignature;
    }

    /**
     * Validate and decode a JWT token string.
     * Returns the payload array if valid, null otherwise.
     */
    public static function validateToken(string $token): ?array
    {
        $parts = explode('.', $token);
        if (count($parts) !== 3) {
            return null;
        }

        list($header, $payload, $signature) = $parts;

        // Verify signature matches
        $expectedSignature = hash_hmac('sha256', $header . "." . $payload, self::getSecret(), true);
        if (!hash_equals(self::base64UrlDecode($signature), $expectedSignature)) {
            return null;
        }

        $decodedPayload = json_decode(self::base64UrlDecode($payload), true);
        if (!$decodedPayload) {
            return null;
        }

        // Check token expiration
        if (isset($decodedPayload['exp']) && $decodedPayload['exp'] < time()) {
            return null;
        }

        return $decodedPayload;
    }

    /**
     * Extract a User model from a JWT token.
     */
    public static function getUserFromToken(string $token): ?User
    {
        $payload = self::validateToken($token);
        if (!$payload || !isset($payload['sub'])) {
            return null;
        }

        return User::find($payload['sub']);
    }

    /**
     * Helper to encode to Base64Url standard.
     */
    private static function base64UrlEncode(string $data): string
    {
        return rtrim(strtr(base64_encode($data), '+/', '-_'), '=');
    }

    /**
     * Helper to decode from Base64Url standard.
     */
    private static function base64UrlDecode(string $data): string
    {
        return base64_decode(strtr($data, '-_', '+/'));
    }
}
