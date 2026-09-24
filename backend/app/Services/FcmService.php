<?php

namespace AppServices;

use IlluminateSupportFacadesHttp;
use IlluminateSupportFacadesLog;

class FcmService
{
    /**
     * Send a Firebase Cloud Messaging HTTP v1 notification.
     *
     * Returns false when delivery is unavailable. The richer result method
     * distinguishes configuration/network failures from permanently invalid
     * registration tokens so callers can safely retire dead devices.
     */
    public static function sendPush($recipientToken, string $title, string $body, array $data = []): bool
    {
        return self::sendPushResult($recipientToken, $title, $body, $data)['sent'];
    }

    public static function sendPushResult($recipientToken, string $title, string $body, array $data = []): array
    {
        if (empty($recipientToken)) {
            return ['sent' => false, 'invalid_token' => true];
        }

        $projectId = trim((string) config('services.fcm.project_id', ''));
        $accessToken = trim((string) config('services.fcm.access_token', ''));

        if ($projectId === '' || $accessToken === '') {
            Log::warning('FCM is not configured; push delivery skipped.', [
                'project_id_present' => $projectId !== '',
            ]);

            return ['sent' => false, 'invalid_token' => false];
        }

        try {
            $response = Http::timeout(10)
                ->withToken($accessToken)
                ->acceptJson()
                ->post(
                    "https://fcm.googleapis.com/v1/projects/{$projectId}/messages:send",
                    [
                        'message' => [
                            'token' => $recipientToken,
                            'notification' => [
                                'title' => $title,
                                'body' => $body,
                            ],
                            'data' => array_map('strval', $data),
                        ],
                    ]
                );

            if ($response->successful()) {
                return ['sent' => true, 'invalid_token' => false];
            }

            $errorCode = (string) data_get($response->json(), 'error.details.0.errorCode', '');
            $invalidToken = $errorCode === 'UNREGISTERED'
                || ($response->status() === 404 && str_contains(strtoupper((string) $response->json('error.message', '')), 'REGISTRATION TOKEN'));

            Log::error('FCM delivery failed.', [
                'status' => $response->status(),
                'error_code' => $errorCode ?: null,
                'response' => $response->json(),
            ]);

            return ['sent' => false, 'invalid_token' => $invalidToken];
        } catch (Throwable $e) {
            Log::error('FCM delivery exception.', ['message' => $e->getMessage()]);

            return ['sent' => false, 'invalid_token' => false];
        }
    }
}
