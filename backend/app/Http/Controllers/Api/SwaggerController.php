<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Response;

class SwaggerController extends Controller
{
    /**
     * Display the Swagger UI documentation dashboard.
     */
    public function index(): Response
    {
        $html = <<<HTML
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>ATU Cafeteria API - Interactive OpenAPI Documentation</title>
  <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/swagger-ui-dist@5.9.0/swagger-ui.css" />
  <link rel="icon" type="image/png" href="https://img.icons8.com/color/48/hamburger.png" />
  <style>
    html {
      box-sizing: border-box;
      overflow-y: scroll;
    }
    *, *:before, *:after {
      box-sizing: inherit;
    }
    body {
      margin: 0;
      background: #f8fafc;
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
    }
    .swagger-ui .topbar {
      background-color: #1e1b4b;
      padding: 12px 0;
    }
    .swagger-ui .topbar .download-url-wrapper {
      display: none;
    }
    .custom-header {
      background-color: #1e1b4b;
      color: white;
      padding: 16px 24px;
      display: flex;
      align-items: center;
      border-bottom: 3px solid #4f46e5;
    }
    .custom-header img {
      height: 40px;
      margin-right: 16px;
    }
    .custom-header h1 {
      margin: 0;
      font-size: 20px;
      font-weight: 700;
      letter-spacing: -0.025em;
    }
    .custom-header span {
      background: #4f46e5;
      padding: 4px 8px;
      border-radius: 6px;
      font-size: 11px;
      font-weight: 600;
      margin-left: 12px;
      text-transform: uppercase;
    }
  </style>
</head>
<body>
  <div class="custom-header">
    <img src="https://img.icons8.com/color/192/hamburger.png" alt="ATU Logo" />
    <h1>ATU Cafeteria Platform</h1>
    <span>Core REST API v1.0.0</span>
  </div>

  <div id="swagger-ui"></div>

  <script src="https://cdn.jsdelivr.net/npm/swagger-ui-dist@5.9.0/swagger-ui-bundle.js" charset="UTF-8"></script>
  <script src="https://cdn.jsdelivr.net/npm/swagger-ui-dist@5.9.0/swagger-ui-standalone-preset.js" charset="UTF-8"></script>
  <script>
    window.onload = () => {
      window.ui = SwaggerUIBundle({
        url: '/api/docs/openapi.json',
        dom_id: '#swagger-ui',
        deepLinking: true,
        presets: [
          SwaggerUIBundle.presets.apis,
          SwaggerUIStandalonePreset
        ],
        plugins: [
          SwaggerUIBundle.plugins.DownloadUrl
        ],
        layout: 'BaseLayout',
        docExpansion: 'list',
        defaultModelsExpandDepth: 1,
        persistAuthorization: true
      });
    };
  </script>
</body>
</html>
HTML;

        return response($html, 200, [
            'Content-Type' => 'text/html'
        ]);
    }

    /**
     * Provide the detailed OpenAPI JSON specification.
     */
    public function openapiJson(): JsonResponse
    {
        $spec = [
            'openapi' => '3.0.0',
            'info' => [
                'title' => 'Accra Technical University (ATU) Cafeteria REST API',
                'description' => 'Comprehensive, enterprise-grade API backend supporting students, food vendors, digital wallets, escrow transactions, real-time tracking, chat conversations, and automated inventory checking.',
                'version' => '1.0.0',
                'contact' => [
                    'name' => 'ATU QA Directorate & Software Engineering Team',
                    'email' => 'emmanuel.kaku@atu.edu.gh'
                ]
            ],
            'servers' => [
                [
                    'url' => '/api',
                    'description' => 'Local/Relative API Gateway'
                ]
            ],
            'paths' => [
                '/login' => [
                    'post' => [
                        'tags' => ['Authentication'],
                        'summary' => 'Standard User Login',
                        'description' => 'Authenticates general users (STUDENT, VENDOR, ADMIN) using username and cross-client compatible pre-hashed SHA-256 PIN codes.',
                        'requestBody' => [
                            'required' => true,
                            'content' => [
                                'application/json' => [
                                    'schema' => [
                                        'type' => 'object',
                                        'required' => ['username', 'password'],
                                        'properties' => [
                                            'username' => ['type' => 'string', 'example' => 'student'],
                                            'password' => ['type' => 'string', 'description' => 'SHA-256 pre-hashed PIN', 'example' => '1234 (Pre-hashed as SHA-256)']
                                        ]
                                    ]
                                ]
                            ]
                        ],
                        'responses' => [
                            '200' => [
                                'description' => 'Successful Login authentication',
                                'content' => [
                                    'application/json' => [
                                        'example' => [
                                            'success' => true,
                                            'token' => 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...',
                                            'user' => [
                                                'id' => 1,
                                                'username' => 'student',
                                                'role' => 'STUDENT',
                                                'fullName' => 'Daniel Mensah',
                                                'balance' => 250.00,
                                                'loyalty_points' => 120
                                            ]
                                        ]
                                    ]
                                ]
                            ],
                            '401' => [
                                'description' => 'Invalid credentials or failed validation'
                            ]
                        ]
                    ]
                ],
                '/student/login' => [
                    'post' => [
                        'tags' => ['Authentication'],
                        'summary' => 'Dedicated Student Sanctum Login',
                        'description' => 'Student-specific Sanctum session authentication.',
                        'requestBody' => [
                            'required' => true,
                            'content' => [
                                'application/json' => [
                                    'schema' => [
                                        'type' => 'object',
                                        'required' => ['username', 'password'],
                                        'properties' => [
                                            'username' => ['type' => 'string', 'example' => 'student'],
                                            'password' => ['type' => 'string', 'example' => '1234 (SHA-256 pre-hashed)']
                                        ]
                                    ]
                                ]
                            ]
                        ],
                        'responses' => [
                            '200' => ['description' => 'Successful Sanctum session init']
                        ]
                    ]
                ],
                '/vendor/login' => [
                    'post' => [
                        'tags' => ['Authentication'],
                        'summary' => 'Dedicated Vendor Sanctum Login',
                        'description' => 'Vendor-specific Sanctum session authentication.',
                        'requestBody' => [
                            'required' => true,
                            'content' => [
                                'application/json' => [
                                    'schema' => [
                                        'type' => 'object',
                                        'required' => ['username', 'password'],
                                        'properties' => [
                                            'username' => ['type' => 'string', 'example' => 'maryjoint'],
                                            'password' => ['type' => 'string', 'example' => '1111 (SHA-256 pre-hashed)']
                                        ]
                                    ]
                                ]
                            ]
                        ],
                        'responses' => [
                            '200' => ['description' => 'Successful Sanctum session init']
                        ]
                    ]
                ],
                '/menus' => [
                    'get' => [
                        'tags' => ['Menus & Items'],
                        'summary' => 'Get All Menus',
                        'description' => 'Retrieves list of active categories and current daily offerings.',
                        'responses' => [
                            '200' => [
                                'description' => 'Array of menus returned'
                            ]
                        ]
                    ]
                ],
                '/menu-items' => [
                    'get' => [
                        'tags' => ['Menus & Items'],
                        'summary' => 'List Standard Menu Items',
                        'description' => 'Returns list of individual food choices and side elements.',
                        'responses' => [
                            '200' => [
                                'description' => 'List of menu items'
                            ]
                        ]
                    ]
                ],
                '/menu-items/search' => [
                    'get' => [
                        'tags' => ['Menus & Items'],
                        'summary' => 'Search & Filter Menu Items',
                        'description' => 'Dynamic searching by term, vendor, or category.',
                        'parameters' => [
                            [
                                'name' => 'term',
                                'in' => 'query',
                                'required' => false,
                                'schema' => ['type' => 'string'],
                                'example' => 'Jollof'
                            ],
                            [
                                'name' => 'category',
                                'in' => 'query',
                                'required' => false,
                                'schema' => ['type' => 'string'],
                                'example' => 'Drinks'
                            ]
                        ],
                        'responses' => [
                            '200' => ['description' => 'Filtered results returned']
                        ]
                    ]
                ],
                '/orders' => [
                    'get' => [
                        'tags' => ['Orders'],
                        'summary' => 'Get All Orders',
                        'description' => 'Public tracking feed of current system pre-orders (Administrator or general tracking dashboard use).',
                        'responses' => [
                            '200' => ['description' => 'Collection of orders']
                        ]
                    ],
                    'post' => [
                        'tags' => ['Orders'],
                        'summary' => 'Place Standard Order',
                        'description' => 'Triggers order placement and virtual wallet balance checking.',
                        'requestBody' => [
                            'required' => true,
                            'content' => [
                                'application/json' => [
                                    'schema' => [
                                        'type' => 'object',
                                        'required' => ['customer_id', 'vendor_id', 'food_item_id', 'quantity'],
                                        'properties' => [
                                            'customer_id' => ['type' => 'integer', 'example' => 1],
                                            'vendor_id' => ['type' => 'integer', 'example' => 10],
                                            'food_item_id' => ['type' => 'integer', 'example' => 101],
                                            'quantity' => ['type' => 'integer', 'example' => 1],
                                            'pickup_time' => ['type' => 'string', 'example' => 'In 15 Mins']
                                        ]
                                    ]
                                ]
                            ]
                        ],
                        'responses' => [
                            '201' => ['description' => 'Order created and payment deducted from wallet.'],
                            '400' => ['description' => 'Insufficient balance or stock limitation.']
                        ]
                    ]
                ],
                '/orders/{id}' => [
                    'get' => [
                        'tags' => ['Orders'],
                        'summary' => 'Show Order Status',
                        'description' => 'Retrieve details of a single order.',
                        'parameters' => [
                            [
                                'name' => 'id',
                                'in' => 'path',
                                'required' => true,
                                'schema' => ['type' => 'integer'],
                                'example' => 1001
                            ]
                        ],
                        'responses' => [
                            '200' => ['description' => 'Order details returned']
                        ]
                    ]
                ],
                '/wallet/balance' => [
                    'get' => [
                        'tags' => ['Wallet & Ledger'],
                        'summary' => 'Get Wallet Balance',
                        'description' => 'Retrieve the virtual credit balance of the authenticated user (Requires Bearer Token).',
                        'security' => [['bearerAuth' => []]],
                        'responses' => [
                            '200' => [
                                'description' => 'Current wallet balance',
                                'content' => [
                                    'application/json' => [
                                        'example' => [
                                            'success' => true,
                                            'balance' => 250.00
                                        ]
                                    ]
                                ]
                            ]
                        ]
                    ]
                ],
                '/wallet/transactions' => [
                    'get' => [
                        'tags' => ['Wallet & Ledger'],
                        'summary' => 'Get Wallet Transactions',
                        'description' => 'Retrieve chronological list of ledger items (deposits, purchases, refunds) for the user (Requires Bearer Token).',
                        'security' => [['bearerAuth' => []]],
                        'responses' => [
                            '200' => ['description' => 'Wallet history ledger collection']
                        ]
                    ]
                ],
                '/chats/conversation/{otherUserId}' => [
                    'get' => [
                        'tags' => ['Chat & Interaction'],
                        'summary' => 'Get Private Conversations',
                        'description' => 'Get chat bubbles and messages exchanged with another campus member (Requires Bearer Token).',
                        'security' => [['bearerAuth' => []]],
                        'parameters' => [
                            [
                                'name' => 'otherUserId',
                                'in' => 'path',
                                'required' => true,
                                'schema' => ['type' => 'integer'],
                                'example' => 10
                            ]
                        ],
                        'responses' => [
                            '200' => ['description' => 'Conversation timeline details']
                        ]
                    ]
                ],
                '/system/status' => [
                    'get' => [
                        'tags' => ['System Status'],
                        'summary' => 'Get System Diagnostics',
                        'description' => 'Checks connection health to SQLite DB, local cache buffers, and underlying container metrics.',
                        'responses' => [
                            '200' => [
                                'description' => 'All dependencies healthy',
                                'content' => [
                                    'application/json' => [
                                        'example' => [
                                            'status' => 'OK',
                                            'database' => 'connected',
                                            'cached_items_count' => 12,
                                            'timestamp' => '2026-07-13T09:55:18'
                                        ]
                                    ]
                                ]
                            ]
                        ]
                    ]
                ],
                '/health' => [
                    'get' => [
                        'tags' => ['System Status'],
                        'summary' => 'Lightweight App Health Check',
                        'description' => 'Used by deployment environments (such as Railway) to check container status.',
                        'responses' => [
                            '200' => [
                                'description' => 'Container operational',
                                'content' => [
                                    'application/json' => [
                                        'example' => [
                                            'status' => 'healthy',
                                            'framework' => 'Laravel 11.x'
                                        ]
                                    ]
                                ]
                            ]
                        ]
                    ]
                ]
            ],
            'components' => [
                'securitySchemes' => [
                    'bearerAuth' => [
                        'type' => 'http',
                        'scheme' => 'bearer',
                        'bearerFormat' => 'JWT',
                        'description' => 'Input your JWT Token (obtained from /login response) or Sanctum Access Token to authorize requests.'
                    ]
                ]
            ]
        ];

        return response()->json($spec, 200, [
            'Access-Control-Allow-Origin' => '*'
        ]);
    }
}
