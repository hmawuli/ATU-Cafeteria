<?php

return [
    'fcm' => [
        'project_id' => env('FCM_PROJECT_ID', ''),
        'access_token' => env('FCM_ACCESS_TOKEN', ''),
    ],
    'paystack' => [
        'secret' => env('PAYSTACK_SECRET_KEY', ''),
        'public' => env('PAYSTACK_PUBLIC_KEY', ''),
        'demo_mode' => (bool) env('PAYSTACK_DEMO_MODE', false),
        'base_url' => env('PAYSTACK_BASE_URL', 'https://api.paystack.co'),
    ],
];
