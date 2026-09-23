<?php

namespace App\Http\Middleware;

use Closure;
use Illuminate\Cache\LockTimeoutException;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Cache;
use Symfony\Component\HttpFoundation\Response;

class IdempotencyMiddleware
{
    private const TTL_SECONDS = 86400;
    private const KEY_MAX_LENGTH = 128;

    public function handle(Request $request, Closure $next): Response
    {
        $idempotencyKey = trim((string) $request->header('Idempotency-Key'));

        // The header is optional for backwards compatibility. Production
        // clients should send it for financial/order mutations.
        if ($idempotencyKey === '') {
            return $next($request);
        }

        if (strlen($idempotencyKey) > self::KEY_MAX_LENGTH) {
            return response()->json([
                'success' => false,
                'message' => 'Idempotency-Key is too long.',
                'error_code' => 'INVALID_IDEMPOTENCY_KEY',
            ], 422);
        }

        $userId = $request->user()?->getAuthIdentifier() ?? 'anonymous';
        $fingerprint = hash('sha256', implode('|', [
            strtoupper($request->method()),
            $request->path(),
            (string) $userId,
            $request->getContent(),
        ]));
        $cacheKey = 'idempotency:response:'.hash('sha256', $userId.'|'.$idempotencyKey);
        $lockKey = $cacheKey.':lock';

        $cached = Cache::get($cacheKey);
        if (is_array($cached)) {
            if (($cached['fingerprint'] ?? null) !== $fingerprint) {
                return response()->json([
                    'success' => false,
                    'message' => 'This Idempotency-Key was already used with a different request.',
                    'error_code' => 'IDEMPOTENCY_KEY_REUSED',
                ], 409);
            }

            return response(
                $cached['body'] ?? '',
                (int) ($cached['status'] ?? 200),
                (array) ($cached['headers'] ?? [])
            );
        }

        try {
            return Cache::lock($lockKey, 15)->block(5, function () use (
                $cacheKey,
                $fingerprint,
                $next,
                $request
            ) {
                $cached = Cache::get($cacheKey);
                if (is_array($cached)) {
                    if (($cached['fingerprint'] ?? null) !== $fingerprint) {
                        return response()->json([
                            'success' => false,
                            'message' => 'This Idempotency-Key was already used with a different request.',
                            'error_code' => 'IDEMPOTENCY_KEY_REUSED',
                        ], 409);
                    }

                    return response(
                        $cached['body'] ?? '',
                        (int) ($cached['status'] ?? 200),
                        (array) ($cached['headers'] ?? [])
                    );
                }

                $response = $next($request);

                // Cache only successful responses. Failed validation/server
                // responses remain retryable and are never permanently pinned.
                if ($response->getStatusCode() >= 200 && $response->getStatusCode() < 300) {
                    Cache::put($cacheKey, [
                        'fingerprint' => $fingerprint,
                        'status' => $response->getStatusCode(),
                        'headers' => [
                            'Content-Type' => $response->headers->get('Content-Type', 'application/json'),
                        ],
                        'body' => $response->getContent(),
                    ], self::TTL_SECONDS);
                }

                return $response;
            });
        } catch (LockTimeoutException) {
            return response()->json([
                'success' => false,
                'message' => 'The same request is already being processed. Please retry shortly.',
                'error_code' => 'IDEMPOTENCY_IN_PROGRESS',
            ], 409);
        }
    }
}
