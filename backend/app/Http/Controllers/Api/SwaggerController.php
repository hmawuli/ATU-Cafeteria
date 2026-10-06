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
        $html = <<<'HTML'
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
    <span>Core REST API v1.2.0</span>
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
            'Content-Type' => 'text/html',
        ]);
    }

    /**
     * Provide the detailed OpenAPI JSON specification.
     *
     * The contract below mirrors the production routes. See
     * docs/API_STANDARDS.md for the response envelope and error conventions.
     */
    public function openapiJson(): JsonResponse
    {
        $spec = [
            'openapi' => '3.0.0',
            'info' => [
                'title' => 'ATU Cafeteria REST API',
                'description' => 'REST API for the ATU Cafeteria platform: Laravel Sanctum authentication, restaurant catalogue, ordering, vendor operations, digital wallet and payments.',
                'version' => '1.2.0',
                'contact' => [
                    'name' => 'ATU Cafeteria Engineering',
                    'email' => 'support@atu.edu.gh',
                ],
            ],
            'servers' => [
                [
                    'url' => '/api',
                    'description' => 'Local/relative API gateway',
                ],
                [
                    'url' => 'https://api.example.com/api',
                    'description' => 'Production API',
                ],
            ],
            'tags' => [
                ['name' => 'Authentication', 'description' => 'Login, registration and session restoration'],
                ['name' => 'Catalog', 'description' => 'Public restaurant catalogue'],
                ['name' => 'Orders', 'description' => 'Order placement and lifecycle'],
                ['name' => 'Vendor', 'description' => 'Vendor operations: menu, status, orders, metrics, inventory'],
                ['name' => 'Wallet', 'description' => 'Digital wallet balance and top-up'],
                ['name' => 'System', 'description' => 'Health checks and diagnostics'],
            ],
            'paths' => [
                // -----------------------------------------------------------------
                // Authentication
                // -----------------------------------------------------------------
                '/login' => [
                    'post' => [
                        'tags' => ['Authentication'],
                        'summary' => 'Standard user login',
                        'description' => 'Authenticates STUDENT, VENDOR and ADMIN users and issues a Sanctum bearer token.',
                        'requestBody' => [
                            'required' => true,
                            'content' => [
                                'application/json' => [
                                    'schema' => [
                                        '$ref' => '#/components/schemas/LoginRequest',
                                    ],
                                ],
                            ],
                        ],
                        'responses' => [
                            '200' => [
                                'description' => 'Authenticated, token and user returned',
                                'content' => [
                                    'application/json' => [
                                        'schema' => [
                                            '$ref' => '#/components/schemas/LoginResponse',
                                        ],
                                    ],
                                ],
                            ],
                            '401' => [
                                'description' => 'Invalid credentials',
                                'content' => ['application/json' => ['schema' => ['$ref' => '#/components/schemas/Error']]],
                            ],
                            '422' => [
                                'description' => 'Validation failed',
                                'content' => ['application/json' => ['schema' => ['$ref' => '#/components/schemas/Error']]],
                            ],
                        ],
                    ],
                ],
                '/register' => [
                    'post' => [
                        'tags' => ['Authentication'],
                        'summary' => 'Create a student account',
                        'description' => 'Public self-registration. Only the STUDENT role may be created through this endpoint; ADMIN/VENDOR roles are provisioned by administrators.',
                        'requestBody' => [
                            'required' => true,
                            'content' => [
                                'application/json' => [
                                    'schema' => [
                                        '$ref' => '#/components/schemas/RegisterRequest',
                                    ],
                                ],
                            ],
                        ],
                        'responses' => [
                            '201' => [
                                'description' => 'Account created',
                                'content' => [
                                    'application/json' => [
                                        'schema' => ['$ref' => '#/components/schemas/User'],
                                    ],
                                ],
                            ],
                            '422' => [
                                'description' => 'Validation error or the role is not self-registrable',
                                'content' => ['application/json' => ['schema' => ['$ref' => '#/components/schemas/Error']]],
                            ],
                        ],
                    ],
                ],
                '/student/login' => [
                    'post' => [
                        'tags' => ['Authentication'],
                        'summary' => 'Student Sanctum login',
                        'description' => 'Student-specific authentication for the customer application.',
                        'requestBody' => [
                            'required' => true,
                            'content' => [
                                'application/json' => ['schema' => ['$ref' => '#/components/schemas/LoginRequest']],
                            ],
                        ],
                        'responses' => [
                            '200' => ['description' => 'Authenticated, token and user returned'],
                            '401' => ['description' => 'Invalid credentials'],
                        ],
                    ],
                ],
                '/vendor/login' => [
                    'post' => [
                        'tags' => ['Authentication'],
                        'summary' => 'Vendor Sanctum login',
                        'description' => 'Vendor-specific authentication for kitchen/vendor terminals.',
                        'requestBody' => [
                            'required' => true,
                            'content' => [
                                'application/json' => ['schema' => ['$ref' => '#/components/schemas/LoginRequest']],
                            ],
                        ],
                        'responses' => [
                            '200' => ['description' => 'Authenticated, token and vendor profile returned'],
                            '401' => ['description' => 'Invalid credentials'],
                        ],
                    ],
                ],
                '/me' => [
                    'get' => [
                        'tags' => ['Authentication'],
                        'summary' => 'Restore the authenticated session',
                        'description' => 'Returns the current user from the bearer token. Used to restore sessions after an app restart.',
                        'security' => [['bearerAuth' => []]],
                        'responses' => [
                            '200' => [
                                'description' => 'Current user profile',
                                'content' => ['application/json' => ['schema' => ['$ref' => '#/components/schemas/User']]],
                            ],
                            '401' => [
                                'description' => 'Missing or invalid bearer token',
                                'content' => ['application/json' => ['schema' => ['$ref' => '#/components/schemas/Error']]],
                            ],
                        ],
                    ],
                ],

                // -----------------------------------------------------------------
                // Catalog
                // -----------------------------------------------------------------
                '/catalog/menu-items' => [
                    'get' => [
                        'tags' => ['Catalog'],
                        'summary' => 'List restaurant menu items',
                        'description' => 'Public catalogue of menu items with vendor, availability, price and stock fields.',
                        'parameters' => [
                            [
                                'name' => 'vendor_id',
                                'in' => 'query',
                                'required' => false,
                                'schema' => ['type' => 'integer'],
                                'description' => 'Filter items by vendor',
                            ],
                            [
                                'name' => 'page',
                                'in' => 'query',
                                'required' => false,
                                'schema' => ['type' => 'integer'],
                                'description' => 'Page number (1-based). Omitting returns the full catalogue.',
                            ],
                            [
                                'name' => 'per_page',
                                'in' => 'query',
                                'required' => false,
                                'schema' => ['type' => 'integer', 'maximum' => 200],
                                'description' => 'Items per page when paginating (default 50, max 200).',
                            ],
                        ],
                        'responses' => [
                            '200' => [
                                'description' => 'Catalogue returned as {success, menu_items: [...]}',
                                'content' => [
                                    'application/json' => [
                                        'example' => [
                                            'success' => true,
                                            'menu_items' => [
                                                [
                                                    'id' => 1,
                                                    'vendor_id' => 20,
                                                    'name' => 'Jollof Rice with Chicken',
                                                    'price' => 35.0,
                                                    'category' => 'Ghanaian Local Dishes',
                                                    'is_available' => true,
                                                    'current_stock' => 35,
                                                ],
                                            ],
                                        ],
                                    ],
                                ],
                            ],
                        ],
                    ],
                ],
                '/catalog/food-items' => [
                    'get' => [
                        'tags' => ['Catalog'],
                        'summary' => 'List food items (deprecated)',
                        'description' => 'Legacy endpoint returning the standard {success, data: [...]} envelope. Prefer /catalog/menu-items, which returns richer menu items with vendor and stock fields.',
                        'deprecated' => true,
                        'responses' => [
                            '200' => ['description' => '{success, data: [...]} list of food items'],
                        ],
                    ],
                ],

                // -----------------------------------------------------------------
                // Orders
                // -----------------------------------------------------------------
                '/orders' => [
                    'post' => [
                        'tags' => ['Orders'],
                        'summary' => 'Place an order',
                        'description' => 'Places an order against a vendor menu item and debits the wallet. The authenticated student is the order customer.',
                        'security' => [['bearerAuth' => []]],
                        'requestBody' => [
                            'required' => true,
                            'content' => [
                                'application/json' => [
                                    'schema' => [
                                        'type' => 'object',
                                        'required' => ['vendor_id', 'menu_item_id', 'food_name', 'quantity', 'unit_price', 'total_price'],
                                        'properties' => [
                                            'vendor_id' => ['type' => 'integer', 'example' => 20],
                                            'menu_item_id' => ['type' => 'integer', 'example' => 3],
                                            'food_item_id' => ['type' => 'integer', 'nullable' => true],
                                            'food_name' => ['type' => 'string', 'example' => 'Jollof Rice with Chicken'],
                                            'quantity' => ['type' => 'integer', 'example' => 1],
                                            'unit_price' => ['type' => 'number', 'example' => 35.0],
                                            'total_price' => ['type' => 'number', 'example' => 35.0],
                                            'points_to_redeem' => ['type' => 'integer', 'example' => 0],
                                            'estimated_pickup_time' => ['type' => 'string', 'example' => 'In 15 Mins'],
                                        ],
                                    ],
                                ],
                            ],
                        ],
                        'responses' => [
                            '201' => ['description' => 'Order created'],
                            '400' => ['description' => 'Validation, stock or balance failure'],
                            '401' => ['description' => 'Unauthenticated'],
                        ],
                    ],
                    'get' => [
                        'tags' => ['Orders'],
                        'summary' => 'List all orders (admin)',
                        'description' => 'Administrative order list. Requires the orders.view permission.',
                        'security' => [['bearerAuth' => []]],
                        'responses' => [
                            '200' => ['description' => 'Collection of orders'],
                        ],
                    ],
                ],
                '/orders/{id}' => [
                    'get' => [
                        'tags' => ['Orders'],
                        'summary' => 'Fetch a single order',
                        'description' => 'Returns one order scoped to the authenticated user (students see only their own orders).',
                        'security' => [['bearerAuth' => []]],
                        'parameters' => [
                            ['name' => 'id', 'in' => 'path', 'required' => true, 'schema' => ['type' => 'integer'], 'example' => 1001],
                        ],
                        'responses' => [
                            '200' => ['description' => 'Order details'],
                            '404' => ['description' => 'Order not found or not owned by the caller'],
                        ],
                    ],
                ],

                // -----------------------------------------------------------------
                // Vendor operations
                // -----------------------------------------------------------------
                '/vendor/menu-items' => [
                    'get' => [
                        'tags' => ['Vendor'],
                        'summary' => 'List the vendor menu',
                        'security' => [['bearerAuth' => []]],
                        'responses' => ['200' => ['description' => '{success, menu_items: [...]}']],
                    ],
                    'post' => [
                        'tags' => ['Vendor'],
                        'summary' => 'Create a stock-tracked menu item',
                        'security' => [['bearerAuth' => []]],
                        'requestBody' => [
                            'required' => true,
                            'content' => [
                                'application/json' => [
                                    'schema' => [
                                        'type' => 'object',
                                        'required' => ['food_name', 'price', 'category'],
                                        'properties' => [
                                            'food_name' => ['type' => 'string', 'example' => 'Tea & Bread'],
                                            'price' => ['type' => 'number', 'example' => 12.0],
                                            'category' => ['type' => 'string', 'example' => 'Breakfast'],
                                            'description' => ['type' => 'string'],
                                            'is_available' => ['type' => 'boolean', 'default' => true],
                                            'initial_stock' => ['type' => 'integer', 'example' => 50],
                                            'low_stock_threshold' => ['type' => 'integer', 'example' => 10],
                                        ],
                                    ],
                                ],
                            ],
                        ],
                        'responses' => [
                            '201' => ['description' => 'Menu item created'],
                            '403' => ['description' => 'Only vendors and admins may create menu items'],
                        ],
                    ],
                ],
                '/vendor/menu-items/{id}' => [
                    'put' => [
                        'tags' => ['Vendor'],
                        'summary' => 'Update a menu item',
                        'security' => [['bearerAuth' => []]],
                        'parameters' => [
                            ['name' => 'id', 'in' => 'path', 'required' => true, 'schema' => ['type' => 'integer']],
                        ],
                        'responses' => ['200' => ['description' => 'Menu item updated'], '403' => ['description' => 'Not owned by this vendor']],
                    ],
                    'delete' => [
                        'tags' => ['Vendor'],
                        'summary' => 'Delete a menu item',
                        'security' => [['bearerAuth' => []]],
                        'parameters' => [
                            ['name' => 'id', 'in' => 'path', 'required' => true, 'schema' => ['type' => 'integer']],
                        ],
                        'responses' => ['200' => ['description' => 'Menu item deleted']],
                    ],
                ],
                '/vendor/status' => [
                    'patch' => [
                        'tags' => ['Vendor'],
                        'summary' => 'Toggle vendor open/closed status',
                        'description' => 'Idempotency-key protected (X-Idempotency-Key header).',
                        'security' => [['bearerAuth' => []]],
                        'requestBody' => [
                            'required' => true,
                            'content' => [
                                'application/json' => [
                                    'schema' => [
                                        'type' => 'object',
                                        'required' => ['is_open'],
                                        'properties' => ['is_open' => ['type' => 'boolean', 'example' => false]],
                                    ],
                                ],
                            ],
                        ],
                        'responses' => ['200' => ['description' => 'Status toggled, {success, is_open} returned']],
                    ],
                ],
                '/vendor/orders' => [
                    'get' => [
                        'tags' => ['Vendor'],
                        'summary' => 'List orders for this vendor',
                        'security' => [['bearerAuth' => []]],
                        'responses' => ['200' => ['description' => 'Collection of orders (pickup PIN never exposed)']],
                    ],
                ],
                '/vendor/orders/{id}/status' => [
                    'patch' => [
                        'tags' => ['Vendor'],
                        'summary' => 'Advance an order status',
                        'security' => [['bearerAuth' => []]],
                        'parameters' => [
                            ['name' => 'id', 'in' => 'path', 'required' => true, 'schema' => ['type' => 'integer']],
                        ],
                        'requestBody' => [
                            'required' => true,
                            'content' => [
                                'application/json' => [
                                    'schema' => [
                                        'type' => 'object',
                                        'required' => ['status'],
                                        'properties' => ['status' => ['type' => 'string', 'enum' => ['RECEIVED', 'PREPARING', 'READY', 'DELIVERED', 'CANCELLED']]],
                                    ],
                                ],
                            ],
                        ],
                        'responses' => ['200' => ['description' => 'Order status advanced']],
                    ],
                ],
                '/vendor/metrics' => [
                    'get' => [
                        'tags' => ['Vendor'],
                        'summary' => 'Vendor performance metrics',
                        'security' => [['bearerAuth' => []]],
                        'responses' => ['200' => ['description' => 'Metrics including popular menu items']],
                    ],
                ],

                // -----------------------------------------------------------------
                // Wallet
                // -----------------------------------------------------------------
                '/wallet' => [
                    'get' => [
                        'tags' => ['Wallet'],
                        'summary' => 'Wallet balance and ledger',
                        'security' => [['bearerAuth' => []]],
                        'parameters' => [
                            [
                                'name' => 'page',
                                'in' => 'query',
                                'required' => false,
                                'schema' => ['type' => 'integer'],
                                'description' => 'Page number (1-based); pagination meta returned when used.',
                            ],
                            [
                                'name' => 'per_page',
                                'in' => 'query',
                                'required' => false,
                                'schema' => ['type' => 'integer', 'maximum' => 100],
                                'description' => 'Transactions per page (default 50, max 100).',
                            ],
                        ],
                        'responses' => [
                            '200' => [
                                'description' => 'Balance and recent transactions',
                                'content' => ['application/json' => ['schema' => ['$ref' => '#/components/schemas/Wallet']]],
                            ],
                        ],
                    ],
                ],
                '/wallet/top-up' => [
                    'post' => [
                        'tags' => ['Wallet'],
                        'summary' => 'Initiate a wallet top-up',
                        'description' => 'Invokes the configured payment provider (Paystack) verification flow and credits the wallet transactionally.',
                        'security' => [['bearerAuth' => []]],
                        'requestBody' => [
                            'required' => true,
                            'content' => [
                                'application/json' => [
                                    'schema' => [
                                        'type' => 'object',
                                        'required' => ['reference'],
                                        'properties' => ['reference' => ['type' => 'string', 'example' => 'paystack-ref-12345']],
                                    ],
                                ],
                            ],
                        ],
                        'responses' => ['200' => ['description' => 'Top-up processed/verified'], '400' => ['description' => 'Invalid reference or duplicate success']],
                    ],
                ],

                // -----------------------------------------------------------------
                // System
                // -----------------------------------------------------------------
                '/health' => [
                    'get' => [
                        'tags' => ['System'],
                        'summary' => 'API health check',
                        'description' => 'Whether the API service is healthy. Used by deployment environments and container health checks.',
                        'responses' => [
                            '200' => [
                                'description' => 'Service healthy',
                                'content' => [
                                    'application/json' => [
                                        'example' => ['status' => 'healthy', 'framework' => 'Laravel 12'],
                                    ],
                                ],
                            ],
                        ],
                    ],
                ],
                '/docs' => [
                    'get' => [
                        'tags' => ['System'],
                        'summary' => 'Interactive API documentation (Swagger UI)',
                        'responses' => ['200' => ['description' => 'Swagger UI HTML page']],
                    ],
                ],
                '/docs/openapi.json' => [
                    'get' => [
                        'tags' => ['System'],
                        'summary' => 'OpenAPI 3.0 specification',
                        'responses' => ['200' => ['description' => 'This specification as JSON']],
                    ],
                ],
            ],
            'components' => [
                'securitySchemes' => [
                    'bearerAuth' => [
                        'type' => 'http',
                        'scheme' => 'bearer',
                        'description' => 'Sanctum bearer token issued by /login. Sent as: Authorization: Bearer <token>',
                    ],
                ],
                'schemas' => [
                    'LoginRequest' => [
                        'type' => 'object',
                        'required' => ['username', 'pin'],
                        'properties' => [
                            'username' => ['type' => 'string', 'example' => 'student'],
                            'pin' => ['type' => 'string', 'minLength' => 4, 'description' => 'PIN; either the plain PIN or a SHA-256 pre-hashed value', 'example' => '1234'],
                        ],
                    ],
                    'RegisterRequest' => [
                        'type' => 'object',
                        'required' => ['email', 'password', 'role', 'fullName'],
                        'properties' => [
                            'email' => ['type' => 'string', 'format' => 'email', 'example' => 'student@atu.edu.gh'],
                            'password' => ['type' => 'string', 'minLength' => 8, 'example' => 'a-secure-password'],
                            'role' => ['type' => 'string', 'enum' => ['STUDENT'], 'description' => 'Only STUDENT is self-registrable'],
                            'fullName' => ['type' => 'string', 'example' => 'Daniel Mensah'],
                        ],
                    ],
                    'LoginResponse' => [
                        'type' => 'object',
                        'properties' => [
                            'success' => ['type' => 'boolean'],
                            'token' => ['type' => 'string'],
                            'user' => ['$ref' => '#/components/schemas/User'],
                            'requires_2fa' => ['type' => 'boolean', 'description' => 'Present when an administrator must verify a 2FA code'],
                        ],
                    ],
                    'User' => [
                        'type' => 'object',
                        'properties' => [
                            'id' => ['type' => 'integer'],
                            'username' => ['type' => 'string'],
                            'role' => ['type' => 'string', 'enum' => ['STUDENT', 'VENDOR', 'ADMIN']],
                            'fullName' => ['type' => 'string'],
                            'balance' => ['type' => 'number'],
                            'loyalty_points' => ['type' => 'integer'],
                            'is_open' => ['type' => 'boolean', 'description' => 'Vendor open status'],
                        ],
                    ],
                    'Wallet' => [
                        'type' => 'object',
                        'properties' => [
                            'success' => ['type' => 'boolean'],
                            'balance' => ['type' => 'number'],
                            'transactions' => ['type' => 'array', 'items' => ['type' => 'object']],
                        ],
                    ],
                    'Error' => [
                        'type' => 'object',
                        'required' => ['message'],
                        'properties' => [
                            'success' => ['type' => 'boolean', 'example' => false],
                            'message' => ['type' => 'string'],
                            'errors' => ['type' => 'object', 'description' => 'Field-level validation errors when present'],
                        ],
                    ],
                ],
            ],
        ];

        return response()->json($spec, 200, [
            'Access-Control-Allow-Origin' => '*',
        ]);
    }
}
