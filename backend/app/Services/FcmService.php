<?php

namespace App\Services;

use Illuminate\Support\Facades\Http;
use Illuminate\Support\Facades\Log;

class FcmService
{
    /**
     * Send a Firebase Cloud Messaging HTTP v1 notification.
     *
     * FCM_ACCESS_TOKEN should be supplied by the production secret manager and
     * rotated according to the Firebase credential lifecycle. The service
     * deliberately returns false when no real delivery credential exists.
     */
    public static function sendPush($recipientToken, string $title, string $body, array $data = []): bool
    {
        if (empty($recipientToken)) {
            return false;
        }

        $projectId = trim((string) env('FCM_PROJECT_ID', ''));
        $accessToken = trim((string) env('FCM_ACCESS_TOKEN', ''));

        if ($projectId === '' || $accessToken === '') {
            Log::warning('FCM is not configured; push delivery skipped.', [
                'project_id_present' => $projectId !== '',
            ]);

            return false;
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

            if (! $response->successful()) {
                Log::error('FCM delivery failed.', [
                    'status' => $response->status(),
                    'response' => $response->json(),
                ]);

                return false;
            }

            return true;
        } catch (\Throwable $e) {
            Log::error('FCM delivery exception.', ['message' => $e->getMessage()]);
            return false;
        }
    }
}
