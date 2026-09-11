<?php

return [

    /*
    |--------------------------------------------------------------------------
    | Cross-Origin Resource Sharing (CORS) Configuration
    |--------------------------------------------------------------------------
    |
    | Local Flutter web development uses a dynamically assigned localhost
    | port, so development origins are matched by pattern. Production
    | deployments should set CORS_ALLOWED_ORIGINS explicitly.
    |
    */

    'paths' => ['api/*', 'sanctum/csrf-cookie'],

    'allowed_methods' => ['*'],

    'allowed_origins' => array_values(array_filter(array_map(
        'trim',
        explode(',', env(
            'CORS_ALLOWED_ORIGINS',
            'http://localhost:3000,http://localhost:5173'
        ))
    ))),

    'allowed_origins_patterns' => [
        '#^http://localhost:\\d+$#',
        '#^http://127\.0\.0\.1:\\d+$#',
    ],

    'allowed_headers' => ['*'],

    'exposed_headers' => ['X-Auth-Token'],

    'max_age' => 0,

    'supports_credentials' => env('CORS_SUPPORTS_CREDENTIALS', false),

];
