<?php

return [

    /*
    |--------------------------------------------------------------------------
    | Cross-Origin Resource Sharing (CORS) Configuration
    |--------------------------------------------------------------------------
    |
    | Production origins must be supplied explicitly through
    | CORS_ALLOWED_ORIGINS. Localhost patterns remain available for local
    | Flutter web development.
    |
    */

    'paths' => ['api/*', 'sanctum/csrf-cookie'],

    'allowed_methods' => ['GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'OPTIONS'],

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

    'allowed_headers' => [
        'Accept',
        'Authorization',
        'Content-Type',
        'Origin',
        'X-Requested-With',
        'X-Request-ID',
    ],

    'exposed_headers' => ['X-Request-ID', 'X-Auth-Token'],

    'max_age' => 86400,

    'supports_credentials' => env('CORS_SUPPORTS_CREDENTIALS', false),

];
