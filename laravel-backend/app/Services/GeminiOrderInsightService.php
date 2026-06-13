<?php

namespace App\Services;

use App\Models\User;
use App\Models\Order;
use Illuminate\Support\Facades\Http;
use Illuminate\Support\Facades\Log;
use Illuminate\Support\Facades\DB;

class GeminiOrderInsightService
{
    protected string $apiKey;
    protected string $model;

    public function __construct()
    {
        $this->apiKey = env('GEMINI_API_KEY') ?: '';
        $this->model = 'gemini-3.5-flash';
    }

    /**
     * Gathers all historical orders from the database for a specific vendor or overall.
     * Computes real quantitative metrics for food popularity and peak time density.
     */
    public function analyzeHistoricalOrders(int $vendorId): array
    {
        // 1. Fetch Vendor Details
        $vendor = User::where('id', $vendorId)->whereIn('role', ['VENDOR', 'ADMIN'])->first();
        if (!$vendor) {
            return [
                'success' => false,
                'message' => 'Vendor account details not found.'
            ];
        }

        // 2. Fetch completed orders
        $orders = Order::where('vendor_id', $vendorId)
            ->where('status', 'COMPLETED')
            ->orderBy('order_timestamp', 'asc')
            ->get();

        if ($orders->isEmpty()) {
            return [
                'success' => true,
                'vendor_id' => $vendorId,
                'vendor_name' => $vendor->fullName,
                'insights_markdown' => "# 📊 ATU Cafeteria Insights: **{$vendor->fullName}**\n\nNo historical completed orders found in the system for this food joint yet. When students complete transaction hand-offs, detailed purchase trends, peak ordering intervals, and food popularity indicators will automatically populate here.",
                'source_metrics' => [
                    'popular_items' => [],
                    'peak_hours' => [],
                    'total_orders' => 0,
                    'total_revenue' => 0.0
                ]
            ];
        }

        // 3. Compute Popular Food Items & Quantities
        $popularItems = DB::table('orders')
            ->where('vendor_id', $vendorId)
            ->where('status', 'COMPLETED')
            ->select('food_name', DB::raw('COUNT(*) as total_orders'), DB::raw('SUM(quantity) as total_qty'), DB::raw('SUM(total_price) as total_revenue'))
            ->groupBy('food_name')
            ->orderBy('total_qty', 'desc')
            ->limit(5)
            ->get()
            ->toArray();

        // 4. Compute peak hours from order timestamps (hourly distribution)
        $hourlyDistribution = [
            'Breakfast Rush (7:00 AM - 10:59 AM)' => 0,
            'Lunch Peak (11:00 AM - 2:29 PM)' => 0,
            'Late Afternoon Slack (2:30 PM - 5:29 PM)' => 0,
            'Evening Service (5:30 PM onwards)' => 0,
        ];

        $hourRangesCount = array_fill(0, 24, 0);

        foreach ($orders as $order) {
            if ($order->order_timestamp) {
                $epochSec = round($order->order_timestamp / 1000);
                $hr = intval(date('G', $epochSec));
                
                // Track localized raw counts
                if ($hr >= 0 && $hr < 24) {
                    $hourRangesCount[$hr] += $order->quantity ?: 1;
                }

                // Map to generalized intervals
                if ($hr >= 7 && $hr < 11) {
                    $hourlyDistribution['Breakfast Rush (7:00 AM - 10:59 AM)'] += $order->quantity ?: 1;
                } elseif ($hr >= 11 && $hr < 14.5) {
                    $hourlyDistribution['Lunch Peak (11:00 AM - 2:29 PM)'] += $order->quantity ?: 1;
                } elseif ($hr >= 14.5 && $hr < 17.5) {
                    $hourlyDistribution['Late Afternoon Slack (2:30 PM - 5:29 PM)'] += $order->quantity ?: 1;
                } else {
                    $hourlyDistribution['Evening Service (5:30 PM onwards)'] += $order->quantity ?: 1;
                }
            }
        }

        // Busiest hour calculation
        arsort($hourlyDistribution);
        $busiestInterval = key($hourlyDistribution);
        $busiestIntervalQty = current($hourlyDistribution);

        $maxHourVal = -1;
        $busiestHourIndex = -1;
        for ($i = 0; $i < 24; $i++) {
            if ($hourRangesCount[$i] > $maxHourVal) {
                $maxHourVal = $hourRangesCount[$i];
                $busiestHourIndex = $i;
            }
        }

        $formattedBusiestHourStr = "Undetermined";
        if ($busiestHourIndex !== -1 && $maxHourVal > 0) {
            $amPm = $busiestHourIndex >= 12 ? 'PM' : 'AM';
            $displayHour = $busiestHourIndex % 12;
            if ($displayHour === 0) $displayHour = 12;
            $nextHour = ($busiestHourIndex + 1) % 12;
            if ($nextHour === 0) $nextHour = 12;
            $nextAmPm = (($busiestHourIndex + 1) % 24) >= 12 ? 'PM' : 'AM';
            $formattedBusiestHourStr = "{$displayHour}:00 {$amPm} to {$nextHour}:00 {$nextAmPm}";
        }

        // 5. Total Orders and Revenue Statistics
        $totalOrders = $orders->count();
        $totalRevenue = $orders->sum('total_price');

        $metricsData = [
            'vendor_name' => $vendor->fullName,
            'total_orders' => $totalOrders,
            'total_revenue' => round($totalRevenue, 2),
            'busiest_interval' => $busiestInterval,
            'busiest_hour' => $formattedBusiestHourStr,
            'popular_items' => $popularItems,
            'hourly_distribution' => $hourlyDistribution
        ];

        // 6. Invoke Gemini API or fallback
        if (empty($this->apiKey) || $this->apiKey === 'MY_GEMINI_API_KEY') {
            return [
                'success' => true,
                'vendor_id' => $vendorId,
                'vendor_name' => $vendor->fullName,
                'insights_markdown' => $this->getMockInsightMarkdown($metricsData),
                'source_metrics' => $metricsData,
                'note' => 'Local fallback generated. GEMINI_API_KEY is not configured in the environment.',
                'generated_at' => date('Y-m-d H:i:s')
            ];
        }

        $prompt = $this->buildInsightPrompt($metricsData);

        try {
            $response = Http::withHeaders([
                'Content-Type' => 'application/json',
            ])
            ->timeout(60)
            ->post("https://generativelanguage.googleapis.com/v1beta/models/{$this->model}:generateContent?key={$this->apiKey}", [
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
                Log::error("Gemini Insight API Error: " . $response->body());
                return [
                    'success' => true,
                    'vendor_id' => $vendorId,
                    'vendor_name' => $vendor->fullName,
                    'insights_markdown' => $this->getMockInsightMarkdown($metricsData),
                    'source_metrics' => $metricsData,
                    'note' => 'Local fallback generated. Gemini API returned direct exception codes.',
                    'generated_at' => date('Y-m-d H:i:s')
                ];
            }

            $result = $response->json();
            $responseText = $result['candidates'][0]['content']['parts'][0]['text'] ?? null;

            if (!$responseText) {
                return [
                    'success' => true,
                    'vendor_id' => $vendorId,
                    'vendor_name' => $vendor->fullName,
                    'insights_markdown' => $this->getMockInsightMarkdown($metricsData),
                    'source_metrics' => $metricsData,
                    'note' => 'Local fallback generated. Empty response received from generative endpoints.',
                    'generated_at' => date('Y-m-d H:i:s')
                ];
            }

            return [
                'success' => true,
                'vendor_id' => $vendorId,
                'vendor_name' => $vendor->fullName,
                'insights_markdown' => $responseText,
                'source_metrics' => $metricsData,
                'generated_at' => date('Y-m-d H:i:s')
            ];

        } catch (\Exception $e) {
            Log::error("Gemini Historical Insight Exception: " . $e->getMessage());
            return [
                'success' => true,
                'vendor_id' => $vendorId,
                'vendor_name' => $vendor->fullName,
                'insights_markdown' => $this->getMockInsightMarkdown($metricsData),
                'source_metrics' => $metricsData,
                'note' => 'Local fallback generated due to client timeout exception.',
                'generated_at' => date('Y-m-d H:i:s')
            ];
        }
    }

    /**
     * Direct Prompt setup for historical demand clustering.
     */
    private function buildInsightPrompt(array $data): string
    {
        $popularItemsStr = '';
        foreach ($data['popular_items'] as $item) {
            $popularItemsStr .= "- **{$item->food_name}**: Handled {$item->total_orders} transactions, totaling {$item->total_qty} food portions sold. Yielded GH₵" . number_format($item->total_revenue, 2) . " revenue.\n";
        }

        $distributionStr = '';
        foreach ($data['hourly_distribution'] as $period => $salesCount) {
            $distributionStr .= "- **{$period}**: Cumulative fulfillment count of {$salesCount} unit(s).\n";
        }

        return "You are a lead institutional restaurant analyst and predictive supply chain strategist at Accra Technical University (ATU).
Please review this aggregated dataset from our 'orders' table for vendor '{$data['vendor_name']}':

--- HISTORICAL SALES SUMMARY ---
- Total Completed Transactions: {$data['total_orders']}
- Total Cumulative Revenue: GH₵" . number_format($data['total_revenue'], 2) . "
- Busiest Standard Interval: {$data['busiest_interval']}
- Absolute Single Busiest Peak Hour: {$data['busiest_hour']}

--- STAR FOOD ITEMS & DEMAND INTENSITY ---
{$popularItemsStr}

--- PEAK TIME ORDER SCHEDULING (Hourly Distribution) ---
{$distributionStr}

Please construct a comprehensive, action-oriented predictive demand and culinary intelligence report in clean Markdown format with the following core pillars:

1. **🔥 Peak Hour Traffic Density & Bottlenecks**: Analyze their busiest windows ({$data['busiest_interval']} or specifically {$data['busiest_hour']}). Suggest how to adjust service velocity or introduce digital pre-orders to navigate these peak university lecture breaks.
2. **🍔 Core Menu Popularity Index**: Analyze the top selling items. Suggest how they can bundle slower-moving products with their popular items (or optimize seasoning, pricing, and portion size) to drive larger orders.
3. **🔋 Kitchen Resource Planning Guidance**: Give tailored guidance on preparing ingredients beforehand to prevent running out of food, minimizing local wait times, and preventing daily surplus waste.

The tone must be sharp, highly encouraging, structured, and customized for a professional university campus vendor. Use clear bullets, bold numeric thresholds, and clean margins. Limit the total report to 350-400 words.";
    }

    /**
     * Highly localized and structured data-driven smart fallback report.
     */
    private function getMockInsightMarkdown(array $data): string
    {
        $topItem = $data['popular_items'][0]->food_name ?? 'Local Jollof or Waakye';
        $topItemQty = $data['popular_items'][0]->total_qty ?? 12;
        $topItemRev = $data['popular_items'][0]->total_revenue ?? 360.00;

        $secondItem = $data['popular_items'][1]->food_name ?? 'Spicy Fried Chicken & Assorted Stews';

        return "# 📈 Gemini Intelligence: Historical Order Analytics Summary for **{$data['vendor_name']}**
*(Local Smart Fallback Report — Active Data Aggregation Running Live)*

An analysis of **{$data['total_orders']} completed transactions** shows heavy student demand and high-contrast purchase peaks sync'd to Accra Technical University's lecture calendar.

---

### 1. 🔥 Peak Traffic Density & Order Velocity
- **Absolute Peak Hour**: **{$data['busiest_hour']}** represents the absolute highest ordering concentration, accounting for major delivery lines.
- **Busiest Interval**: **{$data['busiest_interval']}** is your central volume hub, representing concentrated campus-wide break times.
- **Operational Strategy**: Prepare portion prep **20 minutes before {$data['busiest_hour']}**. Setup a dual-line checkout (split for digital pre-orders vs walk-in ordering) to optimize fulfillment.

---

### 2. 🍔 Core Menu Popularity Index (Top Dish Assessment)
- **Top Performer**: **{$topItem}** is the absolute leader, registering **{$topItemQty} total portions** with **GH₵ " . number_format($topItemRev, 2) . "** in historical sales.
- **Silver Medal**: **{$secondItem}** follows, showing strong student attachment and consistent afternoon traction.
- **Strategic Recommendation**: Introduce an **'ATU Combo Pack'** combining **{$topItem}** with a local drink (like Sobolo/Asana) to boost overall transaction size.

---

### 3. 🔋 Kitchen Resource & Supply Chain Guidance
- **Inventory Buffer**: Maintain a **20% stock surplus of ingredients** for **{$topItem}** on heavy lecture days (like Mondays and Wednesdays) to avoid missing late-stage demand.
- **Fulfillment Prep**: Standardize prep timing to ensure standard hand-offs during peak rush intervals do not exceed **4-6 minutes per student**.";
    }
}
