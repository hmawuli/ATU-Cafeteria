<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Http;
use Illuminate\Support\Facades\Log;
use App\Models\User;
use App\Models\Order;
use App\Models\MenuItem;

class IngredientDemandController extends Controller
{
    /**
     * Map menu items or food joint specialities to standardized raw culinary ingredients (recipes).
     */
    private function getRecipeBreakdown(string $foodName): array
    {
        $normalized = strtolower(trim($foodName));

        // Jollof Rice variants
        if (str_contains($normalized, 'jollof')) {
            return [
                ['name' => 'Raw Perfumed Rice', 'unit' => 'kg', 'qty_per_serving' => 0.15],
                ['name' => 'Tomato Paste & Puree', 'unit' => 'g', 'qty_per_serving' => 45.0],
                ['name' => 'Refined Vegetable Oil', 'unit' => 'liters', 'qty_per_serving' => 0.05],
                ['name' => 'Fresh Chicken / Protein Piece', 'unit' => 'pcs', 'qty_per_serving' => 1.0],
                ['name' => 'Onions & Ground Spices', 'unit' => 'g', 'qty_per_serving' => 25.0]
            ];
        }

        // Waakye variants
        if (str_contains($normalized, 'waakye')) {
            return [
                ['name' => 'Raw Rice', 'unit' => 'kg', 'qty_per_serving' => 0.12],
                ['name' => 'Red Cowpeas (Beans)', 'unit' => 'kg', 'qty_per_serving' => 0.06],
                ['name' => 'Waakye Millet Leaves', 'unit' => 'pcs', 'qty_per_serving' => 0.15],
                ['name' => 'Shito Pepper Sauce', 'unit' => 'g', 'qty_per_serving' => 40.0],
                ['name' => 'Wele (Cow Skin) / Egg', 'unit' => 'pcs', 'qty_per_serving' => 1.0],
                ['name' => 'Spaghetti / Gari Portions', 'unit' => 'g', 'qty_per_serving' => 30.0]
            ];
        }

        // Fried Rice / Stir Fry variants
        if (str_contains($normalized, 'fried rice') || str_contains($normalized, 'stir')) {
            return [
                ['name' => 'Raw Perfumed Rice', 'unit' => 'kg', 'qty_per_serving' => 0.15],
                ['name' => 'Refined Vegetable Oil', 'unit' => 'liters', 'qty_per_serving' => 0.04],
                ['name' => 'Mixed Fresh Vegetables', 'unit' => 'g', 'qty_per_serving' => 50.0],
                ['name' => 'Fresh Eggs', 'unit' => 'pcs', 'qty_per_serving' => 1.0],
                ['name' => 'Soy Sauce & Seasoning Mix', 'unit' => 'ml', 'qty_per_serving' => 15.0]
            ];
        }

        // Indomie / Instant Noodles variants
        if (str_contains($normalized, 'indomie') || str_contains($normalized, 'nood')) {
            return [
                ['name' => 'Instant Noodles Packs', 'unit' => 'packets', 'qty_per_serving' => 1.5],
                ['name' => 'Fresh Eggs', 'unit' => 'pcs', 'qty_per_serving' => 1.0],
                ['name' => 'Sautéed Onions & Carrots', 'unit' => 'g', 'qty_per_serving' => 40.0],
                ['name' => 'Vegetable Frying Oil', 'unit' => 'liters', 'qty_per_serving' => 0.02],
                ['name' => 'Chili & Spices Packets', 'unit' => 'pcs', 'qty_per_serving' => 1.5]
            ];
        }

        // Burgers, Hot Dogs or Sandwiches
        if (str_contains($normalized, 'burg') || str_contains($normalized, 'sandw') || str_contains($normalized, 'dog') || str_contains($normalized, 'bread')) {
            return [
                ['name' => 'Fresh Bread Buns / Loaves', 'unit' => 'pcs', 'qty_per_serving' => 1.0],
                ['name' => 'Beef / Chicken Patty', 'unit' => 'pcs', 'qty_per_serving' => 1.0],
                ['name' => 'Salad Leaves & Tomato Slices', 'unit' => 'g', 'qty_per_serving' => 35.0],
                ['name' => 'Mayonnaise & Mustard Spread', 'unit' => 'g', 'qty_per_serving' => 20.0],
                ['name' => 'Cheddar Cheese Slice', 'unit' => 'pcs', 'qty_per_serving' => 0.5]
            ];
        }

        // Pizza variants
        if (str_contains($normalized, 'pizza')) {
            return [
                ['name' => 'Baking Flour / Dough', 'unit' => 'g', 'qty_per_serving' => 180.0],
                ['name' => 'Mozzarella Cheese', 'unit' => 'g', 'qty_per_serving' => 90.0],
                ['name' => 'Tomato Sauce Base', 'unit' => 'g', 'qty_per_serving' => 50.0],
                ['name' => 'Sliced Meat Toppings', 'unit' => 'g', 'qty_per_serving' => 60.0]
            ];
        }

        // Fufu, light soups, or general heavy local soups
        if (str_contains($normalized, 'fufu') || str_contains($normalized, 'soup')) {
            return [
                ['name' => 'Cassava & Plantain Powder/Tuber', 'unit' => 'kg', 'qty_per_serving' => 0.45],
                ['name' => 'Fresh Goat Meat / Fish Portion', 'unit' => 'pcs', 'qty_per_serving' => 1.5],
                ['name' => 'Fresh Tomatoes & Garden Eggs', 'unit' => 'g', 'qty_per_serving' => 120.0],
                ['name' => 'Soup Palm Oil / Base Paste', 'unit' => 'g', 'qty_per_serving' => 30.0]
            ];
        }

        // Banku, Kenkey, Tilapia or Okro Stew variants
        if (str_contains($normalized, 'banku') || str_contains($normalized, 'kenkey') || str_contains($normalized, 'okro') || str_contains($normalized, 'tilapia')) {
            return [
                ['name' => 'Fermented Corn & Cassava Dough', 'unit' => 'kg', 'qty_per_serving' => 0.35],
                ['name' => 'Fresh Tilapia / Red Fish Piece', 'unit' => 'pcs', 'qty_per_serving' => 1.0],
                ['name' => 'Fresh Okro & Garden Vegetables', 'unit' => 'g', 'qty_per_serving' => 150.0],
                ['name' => 'Palm Frying Oil', 'unit' => 'liters', 'qty_per_serving' => 0.03],
                ['name' => 'Hot Black Shito & Raw Pepper', 'unit' => 'g', 'qty_per_serving' => 45.0]
            ];
        }

        // Fried Chicken / Chicken Wings wings
        if (str_contains($normalized, 'chicken') || str_contains($normalized, 'wing') || str_contains($normalized, 'fry')) {
            return [
                ['name' => 'Fresh Chicken Cutlets', 'unit' => 'kg', 'qty_per_serving' => 0.25],
                ['name' => 'Frying Vegetable Oil', 'unit' => 'liters', 'qty_per_serving' => 0.06],
                ['name' => 'Breading Flour & Spices', 'unit' => 'g', 'qty_per_serving' => 40.0]
            ];
        }

        // Beverages, mineral water, Sobolo
        if (str_contains($normalized, 'drink') || str_contains($normalized, 'coke') || str_contains($normalized, 'fanta') || str_contains($normalized, 'sprite') || str_contains($normalized, 'sobolo') || str_contains($normalized, 'water') || str_contains($normalized, 'bottle')) {
            return [
                ['name' => 'Bottled Drinks / Beverages', 'unit' => 'pcs', 'qty_per_serving' => 1.0]
            ];
        }

        // Smart, generic culinary recipe fallback for unspecified food items
        return [
            ['name' => 'Core Ingredients (' . ucwords($foodName) . ')', 'unit' => 'kg', 'qty_per_serving' => 0.2],
            ['name' => 'Universal Seasoning & Spice Packs', 'unit' => 'g', 'qty_per_serving' => 20.0],
            ['name' => 'Standard Biodegradable Pack Box', 'unit' => 'pcs', 'qty_per_serving' => 1.0]
        ];
    }

    /**
     * Predictive analytical engine. Parses history to calculate weekly ingredient demand and suggest stock levels.
     */
    public function getIngredientDemandPrediction(Request $request, $vendorId = null)
    {
        $user = $request->user();
        if (!$user) {
            return response()->json([
                'success' => false,
                'message' => 'Unauthenticated.'
            ], 401);
        }

        $role = strtoupper($user->role);
        if ($role !== 'VENDOR' && $role !== 'ADMIN') {
            return response()->json([
                'success' => false,
                'message' => 'Unauthorized. Only registered vendors and administrators can retrieve predictive supply chain analytics.'
            ], 403);
        }

        // Set target vendor
        if ($role === 'ADMIN') {
            if ($vendorId) {
                $targetVendorId = (int)$vendorId;
            } elseif ($request->has('vendor_id')) {
                $targetVendorId = (int)$request->input('vendor_id');
            } else {
                return response()->json([
                    'success' => false,
                    'message' => 'Vendor ID is required for administrative requests.'
                ], 400);
            }
        } else {
            $targetVendorId = $user->id;
        }

        $vendor = User::find($targetVendorId);
        if (!$vendor) {
            return response()->json([
                'success' => false,
                'message' => 'Vendor account not found.'
            ], 404);
        }

        $vendorName = $vendor->fullName ?: $vendor->username;

        // Fetch completed historical orders for this vendor in the last 30 days
        $thirtyDaysAgoMs = (time() - (30 * 24 * 60 * 60)) * 1000;
        $orders = Order::where('vendor_id', $targetVendorId)
            ->whereIn(DB::raw('upper(status)'), ['COMPLETED', 'DELIVERED'])
            ->where('order_timestamp', '>=', $thirtyDaysAgoMs)
            ->get();

        // Calculate timeframe weeks (minimum 1 week, max 4.3 weeks)
        $timeframeDays = 30;
        if (!$orders->isEmpty()) {
            $minTs = $orders->min('order_timestamp') / 1000;
            $maxTs = $orders->max('order_timestamp') / 1000;
            $daysDiff = max(1, round(($maxTs - $minTs) / (24 * 60 * 60)));
            $weeks = max(1.0, round($daysDiff / 7, 2));
        } else {
            $weeks = 4.0; // Assume 4 weeks default timeframe
        }

        if ($orders->isEmpty()) {
            return response()->json([
                'success' => true,
                'vendor_id' => $targetVendorId,
                'vendor_name' => $vendorName,
                'timeframe_weeks' => $weeks,
                'predictive_report' => "# 📈 Predictive Supply Intelligence: **{$vendorName}**\n\nNo historical completed transactions were found in the last 30 days to build a predictive supply model. Once students complete transaction checkouts, our analytical engine will calculate your food sales velocities, map them to exact raw culinary ingredients, and suggest optimal stock replenishment schedules here.",
                'predictions' => [],
                'menu_sales_velocity' => [],
                'generated_at' => date('c')
            ], 200);
        }

        // Map menu item sales velocities
        $menuSales = [];
        foreach ($orders as $order) {
            $name = trim($order->food_name);
            if (empty($name)) continue;

            if (!isset($menuSales[$name])) {
                $menuSales[$name] = [
                    'food_name' => $name,
                    'menu_item_id' => $order->menu_item_id,
                    'total_qty_sold' => 0,
                    'order_count' => 0,
                ];
            }
            $menuSales[$name]['total_qty_sold'] += ($order->quantity ?: 1);
            $menuSales[$name]['order_count']++;
        }

        // Compute weekly velocity per dish
        $menuVelocity = [];
        foreach ($menuSales as $name => $data) {
            $weeklyQty = round($data['total_qty_sold'] / $weeks, 2);
            $menuVelocity[$name] = [
                'food_name' => $name,
                'menu_item_id' => $data['menu_item_id'],
                'total_qty_sold' => $data['total_qty_sold'],
                'weekly_sales_velocity' => $weeklyQty,
                'average_orders_per_week' => round($data['order_count'] / $weeks, 2),
            ];
        }

        // Map dishes to raw ingredients & aggregate ingredient demand
        $ingredientAggregates = [];
        foreach ($menuVelocity as $dishName => $velocity) {
            $recipe = $this->getRecipeBreakdown($dishName);
            $weeklyDishDemand = $velocity['weekly_sales_velocity'];

            foreach ($recipe as $ing) {
                $ingName = $ing['name'];
                $unit = $ing['unit'];
                $requiredPerServing = $ing['qty_per_serving'];
                $weeklyRequiredQty = round($requiredPerServing * $weeklyDishDemand, 2);

                if (!isset($ingredientAggregates[$ingName])) {
                    $ingredientAggregates[$ingName] = [
                        'name' => $ingName,
                        'unit' => $unit,
                        'predicted_weekly_demand' => 0.0,
                        'source_dishes' => [],
                    ];
                }

                $ingredientAggregates[$ingName]['predicted_weekly_demand'] += $weeklyRequiredQty;
                $ingredientAggregates[$ingName]['source_dishes'][] = [
                    'dish_name' => $dishName,
                    'contribution_qty' => $weeklyRequiredQty
                ];
            }
        }

        // Apply a safety stock buffer (e.g., 25% safety buffer for institutional break traffic)
        $safetyBufferMultiplier = 1.25; 
        $predictionsList = [];

        // Attempt to cross-reference menu items to estimate standard "current stock" if applicable
        $vendorMenuItems = MenuItem::where('vendor_id', $targetVendorId)->get();

        foreach ($ingredientAggregates as $name => $data) {
            $weeklyDemand = round($data['predicted_weekly_demand'], 2);
            $suggestedStock = round($weeklyDemand * $safetyBufferMultiplier, 2);
            
            // Try to match a menu item's current stock as a proxy if it shares a similar name
            $currentStockProxy = 0;
            $matchedMenuItem = $vendorMenuItems->first(function ($mi) use ($name) {
                return str_contains(strtolower($mi->name), strtolower(explode(' ', $name)[0]));
            });

            if ($matchedMenuItem) {
                $currentStockProxy = $matchedMenuItem->current_stock;
            } else {
                // Generically seed proxy based on demand
                $currentStockProxy = round($weeklyDemand * 0.4); // assume they have 40% on hand
            }

            $restockAmount = max(0.0, round($suggestedStock - $currentStockProxy, 2));

            $predictionsList[] = [
                'ingredient_name' => $name,
                'unit' => $data['unit'],
                'predicted_weekly_demand' => $weeklyDemand,
                'suggested_stock_level' => $suggestedStock,
                'safety_buffer_percent' => 25,
                'current_estimated_stock' => (double)$currentStockProxy,
                'suggested_restock_amount' => $restockAmount,
                'source_dishes' => $data['source_dishes']
            ];
        }

        // Format source metrics
        $metricsData = [
            'vendor_id' => $targetVendorId,
            'vendor_name' => $vendorName,
            'timeframe_weeks' => $weeks,
            'total_orders_analyzed' => count($orders),
            'menu_sales_velocity' => array_values($menuVelocity),
            'predictions' => $predictionsList
        ];

        // Retrieve Gemini API Key & check configuration
        $apiKey = env('GEMINI_API_KEY') ?: '';
        $model = 'gemini-3.5-flash';

        if (empty($apiKey) || $apiKey === 'MY_GEMINI_API_KEY') {
            return response()->json([
                'success' => true,
                'vendor_id' => $targetVendorId,
                'vendor_name' => $vendorName,
                'timeframe_weeks' => $weeks,
                'predictive_report' => $this->generateMockDemandReport($vendorName, $metricsData),
                'predictions' => $predictionsList,
                'menu_sales_velocity' => array_values($menuVelocity),
                'note' => 'Local fallback report generated. GEMINI_API_KEY is not configured.',
                'generated_at' => date('c')
            ], 200);
        }

        // Build data string for prompt to avoid excessive tokens
        $itemsSummary = '';
        foreach ($menuVelocity as $dish => $vel) {
            $itemsSummary .= "- **{$dish}**: sold {$vel['total_qty_sold']} portions total (Weekly velocity: {$vel['weekly_sales_velocity']} servings/week)\n";
        }

        $ingredientsSummary = '';
        foreach ($predictionsList as $pred) {
            $ingredientsSummary .= "- **{$pred['ingredient_name']}**: Weekly Demand of {$pred['predicted_weekly_demand']}{$pred['unit']} (Suggested Stock: {$pred['suggested_stock_level']}{$pred['unit']} | Suggested Restock: {$pred['suggested_restock_amount']}{$pred['unit']})\n";
        }

        $prompt = "You are an AI Supply Chain Planner and Culinary Operations Expert at Accra Technical University.
Please review the 30-day historical cafeteria ordering statistics for vendor \"{$vendorName}\":

--- MENU WEEKLY SALES VELOCITIES ---
{$itemsSummary}

--- AGGREGATED WEEKLY INGREDIENT DEMAND PREDICTIONS (Derived from Recipes) ---
{$ingredientsSummary}

Please construct a highly detailed, executive-level Predictive Supply Chain and Ingredient Demand Report in Markdown:
1. **📉 Core Ingredient Demand Outlook**: Highlight the top 3 ingredients with the most critical upcoming weekly demand and explain their relevance to high-velocity menu items.
2. **🛡️ Dynamic Stock Optimization**: Justify our suggested 25% safety stock buffer. Discuss how to handle perishable ingredients (e.g. fresh chicken, eggs, vegetables) vs. stable dry ingredients (perfumed rice, noodles).
3. **💡 Strategic Kitchen Planning**: Provide 3 concrete recommendations for the kitchen crew (e.g. bulk pre-purchases, pre-chop preparation times ahead of high-traffic lecture breaks, cost containment strategies during off-peak campus periods).

Keep the tone sharp, extremely professional, data-centric, and encouraging for a cafeteria vendor. Use bold numbers and clear bullet structures. Limit to 350-400 words.";

        try {
            $response = Http::withHeaders([
                'Content-Type' => 'application/json',
            ])
            ->timeout(60)
            ->post("https://generativelanguage.googleapis.com/v1beta/models/{$model}:generateContent?key={$apiKey}", [
                'contents' => [
                    [
                        'parts' => [
                            ['text' => $prompt]
                        ]
                    ]
                ],
                'generationConfig' => [
                    'temperature' => 0.3
                ]
            ]);

            if ($response->failed()) {
                Log::error("Gemini Demand Prediction API Error: " . $response->body());
                return response()->json([
                    'success' => true,
                    'vendor_id' => $targetVendorId,
                    'vendor_name' => $vendorName,
                    'timeframe_weeks' => $weeks,
                    'predictive_report' => $this->generateMockDemandReport($vendorName, $metricsData),
                    'predictions' => $predictionsList,
                    'menu_sales_velocity' => array_values($menuVelocity),
                    'note' => 'Local fallback generated due to external endpoint error.',
                    'generated_at' => date('c')
                ], 200);
            }

            $result = $response->json();
            $responseText = $result['candidates'][0]['content']['parts'][0]['text'] ?? null;

            if (!$responseText) {
                return response()->json([
                    'success' => true,
                    'vendor_id' => $targetVendorId,
                    'vendor_name' => $vendorName,
                    'timeframe_weeks' => $weeks,
                    'predictive_report' => $this->generateMockDemandReport($vendorName, $metricsData),
                    'predictions' => $predictionsList,
                    'menu_sales_velocity' => array_values($menuVelocity),
                    'note' => 'Local fallback generated due to empty API output.',
                    'generated_at' => date('c')
                ], 200);
            }

            return response()->json([
                'success' => true,
                'vendor_id' => $targetVendorId,
                'vendor_name' => $vendorName,
                'timeframe_weeks' => $weeks,
                'predictive_report' => $responseText,
                'predictions' => $predictionsList,
                'menu_sales_velocity' => array_values($menuVelocity),
                'generated_at' => date('c')
            ], 200);

        } catch (\Exception $e) {
            Log::error("Gemini Demand Prediction Exception: " . $e->getMessage());
            return response()->json([
                'success' => true,
                'vendor_id' => $targetVendorId,
                'vendor_name' => $vendorName,
                'timeframe_weeks' => $weeks,
                'predictive_report' => $this->generateMockDemandReport($vendorName, $metricsData),
                'predictions' => $predictionsList,
                'menu_sales_velocity' => array_values($menuVelocity),
                'note' => 'Local fallback generated due to connection timeout.',
                'generated_at' => date('c')
            ], 200);
        }
    }

    /**
     * Highly detailed and polished local fallback report generator.
     */
    private function generateMockDemandReport(string $vendorName, array $metrics): string
    {
        $topDishes = array_slice($metrics['menu_sales_velocity'], 0, 3);
        $dishLines = '';
        foreach ($topDishes as $td) {
            $dishLines .= "*   **{$td['food_name']}**: weekly velocity of **{$td['weekly_sales_velocity']} servings**.\n";
        }

        $topIngredients = array_slice($metrics['predictions'], 0, 3);
        $ingLines = '';
        foreach ($topIngredients as $ti) {
            $ingLines .= "*   **{$ti['ingredient_name']}**: predicted weekly baseline of **{$ti['predicted_weekly_demand']} {$ti['unit']}** (Safety Target: **{$ti['suggested_stock_level']} {$ti['unit']}**)\n";
        }

        return "# 📈 Predictive Supply Intelligence Report for **{$vendorName}**
*(Local Smart Predictive Modeling — Active Supply Chain Analytics)*

This analytics report parses **{$metrics['total_orders_analyzed']} completed orders** over a **{$metrics['timeframe_weeks']}-week timeframe** to model your ingredient requirements and suggest optimized kitchen restocking targets.

---

### 1. 📉 Core Ingredient Demand Outlook
By decomposing your active sales records using our culinary recipe mapper, we have mapped your weekly menu velocity to baseline raw kitchen inventory needs. Your highest-density upcoming ingredients are:
{$ingLines}

The heavy sales velocity is fueled primarily by your top-selling student dishes:
{$dishLines}

---

### 2. 🛡️ Dynamic Stock Optimization & Storage Rules
We have applied a custom **25% safety stock buffer** to your baseline demand. This buffer is designed to absorb unexpected campus-wide congestion waves, such as late-morning lecture releases.
*   **Perishable Supplies**: For items like *Fresh Eggs, Chicken, and Mixed Vegetables*, maintain a tight 24-hour delivery loop to guarantee sanitation and flavor freshness.
*   **Dry Goods**: For bulk goods like *Raw Rice, Flour, and Noodles*, leverage bulk storage spaces to lock in discounted wholesale prices.

---

### 3. 💡 Actionable Kitchen Resource Recommendations
To improve serving velocity and minimize food waste during ATU lecture sessions, apply these operational steps:
1.  **⚡ High-Velocity Batch Prep**: Chop, marinate, and pre-portion ingredients for high-velocity dishes 30 minutes before the 11:00 AM lunch breaks.
2.  **📦 Supplier Lead-Time Consolidation**: Secure fixed deliveries from vegetable and protein merchants every Monday at 6:30 AM to match the weekly academic surge.
3.  **🍂 Off-Peak Cost Containment**: Reduce preparation batches on Friday afternoons when student traffic drops as lectures conclude.";
    }
}
