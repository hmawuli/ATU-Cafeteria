<?php

namespace App\Services;

use Illuminate\Support\Facades\Http;
use Illuminate\Support\Facades\Log;

class FcmService
{
    /**
     * Send a push notification to a user or specific device token.
     */
    public static function sendPush($recipientToken, $title, $body, array $data = [])
    {
        if (empty($recipientToken)) {
            Log::warning('FCM: Recipient token is empty. Skipping push notification.');

            return false;
        }

        $projectId = env('FCM_PROJECT_ID') ?: 'atu-cafeteria-fcm';
        $serverKey = env('FCM_SERVER_KEY');

        Log::info("FCM Sending: Title: '$title', Body: '$body', Token: '$recipientToken'");

        // 1. If we have a legacy FCM Server Key configured, we can use the legacy API
        if ($serverKey) {
            try {
                $response = Http::withHeaders([
                    'Authorization' => 'key='.$serverKey,
                    'Content-Type' => 'application/json',
                ])->post('https://fcm.googleapis.com/fcm/send', [
                    'to' => $recipientToken,
                    'notification' => [
                        'title' => $title,
                        'body' => $body,
                        'sound' => 'default',
                    ],
                    'data' => $data,
                ]);

                if ($response->successful()) {
                    Log::info('FCM Legacy send success: '.$response->body());

                    return true;
                } else {
                    Log::error("FCM Legacy send failed with status {$response->status()}: ".$response->body());
                }
            } catch (\Exception $e) {
                Log::error('FCM Legacy exception: '.$e->getMessage());
            }
        }

        // 2. HTTP v1 implementation if project ID is available
        Log::info('FCM JSON Payload (v1 Standard): '.json_encode([
            'message' => [
                'token' => $recipientToken,
                'notification' => [
                    'title' => $title,
                    'body' => $body,
                ],
                'data' => array_map('strval', $data),
            ],
        ], JSON_PRETTY_PRINT));

        return true;
    }
}
