<?php

namespace App\Services;

use App\Models\User;
use App\Models\Order;
use App\Models\Feedback;
use App\Models\Review;
use Illuminate\Support\Facades\Http;
use Illuminate\Support\Facades\Log;
use Illuminate\Support\Facades\DB;

class GeminiPerformanceReportService
{
    protected string $apiKey;
    protected string $model;

    public function __construct()
    {
        $this->apiKey = env('GEMINI_API_KEY') ?: '';
        $this->model = 'gemini-3.5-flash';
    }

    /**
     * Gather historical statistics and feedback comments for a given vendor.
     */
    public function gatherVendorData(int $vendorId): array
    {
        $vendor = User::where('id', $vendorId)->whereIn('role', ['VENDOR', 'ADMIN'])->first();
        if (!$vendor) {
            return [];
        }

        // Gather Order Metrics
        $totalOrders = Order::where('vendor_id', $vendorId)->count();
        $completedOrdersCount = Order::where('vendor_id', $vendorId)->where('status', 'COMPLETED')->count();
        $declinedOrdersCount = Order::where('vendor_id', $vendorId)->where('status', 'DECLINED')->count();
        $totalRevenue = Order::where('vendor_id', $vendorId)->where('status', 'COMPLETED')->sum('total_price');

        // Top dishes structured
        $topDishes = Order::where('vendor_id', $vendorId)
            ->where('status', 'COMPLETED')
            ->select('food_name', DB::raw('COUNT(*) as order_count'), DB::raw('SUM(quantity) as total_qty'))
            ->groupBy('food_name')
            ->orderBy('order_count', 'desc')
            ->limit(5)
            ->get()
            ->toArray();

        // Weekly comparative statistics for orders (Last 7 days vs previous 7 days)
        $sevenDaysAgoMs = (time() - 7 * 24 * 60 * 60) * 1000;
        $fourteenDaysAgoMs = (time() - 14 * 24 * 60 * 60) * 1000;

        $ordersLast7Days = Order::where('vendor_id', $vendorId)
            ->where('status', 'COMPLETED')
            ->where('order_timestamp', '>=', $sevenDaysAgoMs)
            ->count();

        $ordersPrev7Days = Order::where('vendor_id', $vendorId)
            ->where('status', 'COMPLETED')
            ->where('order_timestamp', '>=', $fourteenDaysAgoMs)
            ->where('order_timestamp', '<', $sevenDaysAgoMs)
            ->count();

        // Hourly peak distribution metrics
        $hourlyDistribution = [
            'Breakfast (6am - 10:59am)' => 0,
            'Lunch Peak (11am - 2:59pm)' => 0,
            'Afternoon Off-Peak (3pm - 5:59pm)' => 0,
            'Evening/Night (6pm onwards)' => 0,
        ];

        $completedOrdersAllTime = Order::where('vendor_id', $vendorId)
            ->where('status', 'COMPLETED')
            ->get();

        foreach ($completedOrdersAllTime as $co) {
            if ($co->order_timestamp) {
                $epochSec = round($co->order_timestamp / 1000);
                $hr = intval(date('G', $epochSec));
                if ($hr >= 6 && $hr < 11) {
                    $hourlyDistribution['Breakfast (6am - 10:59am)']++;
                } elseif ($hr >= 11 && $hr < 15) {
                    $hourlyDistribution['Lunch Peak (11am - 2:59pm)']++;
                } elseif ($hr >= 15 && $hr < 18) {
                    $hourlyDistribution['Afternoon Off-Peak (3pm - 5:59pm)']++;
                } else {
                    $hourlyDistribution['Evening/Night (6pm onwards)']++;
                }
            }
        }

        // Calculate Completion Rate
        $denominator = $totalOrders - $declinedOrdersCount;
        $completionRate = $denominator > 0 ? round(($completedOrdersCount / $denominator) * 100, 1) : 0.0;

        // Gather Feedback ratings from Feedback table
        $feedbacks = Feedback::where('vendor_id', $vendorId)->get();
        $totalFeedbacks = $feedbacks->count();

        $avgFoodQuality = round($feedbacks->avg('rating_food_quality') ?? 0, 1);
        $avgCleanliness = round($feedbacks->avg('rating_cleanliness') ?? 0, 1);
        $avgServiceSpeed = round($feedbacks->avg('rating_service_speed') ?? 0, 1);
        $avgPriceValue = round($feedbacks->avg('rating_price_value') ?? 0, 1);

        $comments = $feedbacks->pluck('comment')->filter()->slice(0, 15)->toArray();

        // Gather General Reviews from the Reviews model/table
        $reviews = [];
        $totalReviews = 0;
        $avgReviewRating = 0.0;
        $reviewComments = [];

        try {
            if (class_exists(Review::class)) {
                $reviews = Review::where('vendor_id', $vendorId)->get();
                $totalReviews = $reviews->count();
                $avgReviewRating = round($reviews->avg('rating') ?? 0, 1);
                $reviewComments = $reviews->pluck('comment')->filter()->slice(0, 15)->toArray();
            }
        } catch (\Exception $e) {
            Log::warning("Review table lookup ignored. Review model/table might not be migrated yet: " . $e->getMessage());
        }

        // Combine all feedback comments
        $allComments = array_unique(array_merge($comments, $reviewComments));

        return [
            'vendor_name' => $vendor->fullName,
            'vendor_info' => $vendor->info,
            'metrics' => [
                'total_orders' => $totalOrders,
                'completed_orders' => $completedOrdersCount,
                'declined_orders' => $declinedOrdersCount,
                'completion_rate_percent' => $completionRate,
                'total_revenue_ghs' => round($totalRevenue, 2),
                'top_dishes' => $topDishes,
                'weekly_comparison' => [
                    'last_7_days' => $ordersLast7Days,
                    'prev_7_days' => $ordersPrev7Days,
                ],
                'hourly_distribution' => $hourlyDistribution,
            ],
            'ratings' => [
                'food_quality_avg' => $avgFoodQuality,
                'cleanliness_avg' => $avgCleanliness,
                'service_speed_avg' => $avgServiceSpeed,
                'price_value_avg' => $avgPriceValue,
                'feedback_count' => $totalFeedbacks,
                'general_reviews_count' => $totalReviews,
                'general_reviews_avg' => $avgReviewRating,
            ],
            'recent_comments' => array_values($allComments),
        ];
    }

    /**
     * Call Gemini to analyze the gathered data & generate a performance report.
     */
    public function generateReport(int $vendorId): array
    {
        $data = $this->gatherVendorData($vendorId);
        if (empty($data)) {
            return [
                'success' => false,
                'message' => 'Vendor not found or has no active stats profile to analyze.'
            ];
        }

        if (empty($this->apiKey) || $this->apiKey === 'MY_GEMINI_API_KEY') {
            return [
                'success' => true,
                'vendor_id' => $vendorId,
                'vendor_name' => $data['vendor_name'],
                'report_markdown' => $this->getMockReportMarkdown($data),
                'source_metrics' => $data,
                'note' => 'Local smart fallback generation. GEMINI_API_KEY is unset or default template.',
                'generated_at' => date('Y-m-d H:i:s')
            ];
        }

        $prompt = $this->buildAnalysisPrompt($data);

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
                    'temperature' => 0.2
                ]
            ]);

            if ($response->failed()) {
                Log::error("Gemini API error Response: " . $response->body());
                return [
                    'success' => true,
                    'vendor_id' => $vendorId,
                    'vendor_name' => $data['vendor_name'],
                    'report_markdown' => $this->getMockReportMarkdown($data),
                    'source_metrics' => $data,
                    'note' => 'Fallback activation: Gemini API endpoint was unreachable or responded with code: ' . $response->status(),
                    'generated_at' => date('Y-m-d H:i:s')
                ];
            }

            $result = $response->json();
            $responseText = $result['candidates'][0]['content']['parts'][0]['text'] ?? null;

            if (!$responseText) {
                return [
                    'success' => true,
                    'vendor_id' => $vendorId,
                    'vendor_name' => $data['vendor_name'],
                    'report_markdown' => $this->getMockReportMarkdown($data),
                    'source_metrics' => $data,
                    'note' => 'Fallback activation: Blank candidate content parts received from LLM.',
                    'generated_at' => date('Y-m-d H:i:s')
                ];
            }

            return [
                'success' => true,
                'vendor_id' => $vendorId,
                'vendor_name' => $data['vendor_name'],
                'report_markdown' => $responseText,
                'source_metrics' => $data,
                'generated_at' => date('Y-m-d H:i:s')
            ];

        } catch (\Exception $e) {
            Log::error("Gemini Performance Report Exception: " . $e->getMessage());
            return [
                'success' => true,
                'vendor_id' => $vendorId,
                'vendor_name' => $data['vendor_name'],
                'report_markdown' => $this->getMockReportMarkdown($data),
                'source_metrics' => $data,
                'note' => 'Local fallback generated due to system connection issue: ' . $e->getMessage(),
                'generated_at' => date('Y-m-d H:i:s')
            ];
        }
    }

    /**
     * Construct the analytical text prompt containing vendor statistics and qualitative student comments.
     */
    private function buildAnalysisPrompt(array $data): string
    {
        $topDishesStr = '';
        foreach ($data['metrics']['top_dishes'] as $dish) {
            $topDishesStr .= "- {$dish['food_name']} (Orders count: {$dish['order_count']}, Total Quantity items sold: {$dish['total_qty']})\n";
        }

        $hourlyDistributionStr = '';
        foreach ($data['metrics']['hourly_distribution'] ?? [] as $period => $count) {
            $hourlyDistributionStr .= "- {$period}: {$count} completed order(s)\n";
        }

        $weeklyComparisonStr = "- Past 7 Days Completed Orders: " . ($data['metrics']['weekly_comparison']['last_7_days'] ?? 0) . "\n" .
                              "- Previous 7 Days Completed Orders: " . ($data['metrics']['weekly_comparison']['prev_7_days'] ?? 0) . "\n";

        $commentsStr = '';
        foreach ($data['recent_comments'] as $comment) {
            $commentsStr .= "- \"{$comment}\"\n";
        }

        return "You are an expert hospitality and business intelligence performance consultant specializing in campus cafeteria systems.
Please analyze the following historical metrics and customer feedback for cafeteria vendor '{$data['vendor_name']}' at Accra Technical University (ATU):

--- VENDOR STATS & METRICS ---
- Description/Tagline: \"{$data['vendor_info']}\"
- Total Orders Placed: {$data['metrics']['total_orders']}
- Successfully Completed Orders: {$data['metrics']['completed_orders']}
- Canceled/Declined Orders: {$data['metrics']['declined_orders']}
- Order Completion Success Rate: {$data['metrics']['completion_rate_percent']}%
- Cumulative Sales Volume: GH₵" . number_format($data['metrics']['total_revenue_ghs'], 2) . "

--- POPULAR ITEM TRENDS (Top Dishes) ---
{$topDishesStr}

--- RECENT WEEK-OVER-WEEK ACTIVITY ---
{$weeklyComparisonStr}

--- PEAK SERVICE INTERVALS (Hourly Distribution) ---
{$hourlyDistributionStr}

--- AVERAGE RATINGS (Scale: 1-5 Stars) ---
- Food Taste & Culinary Quality: {$data['ratings']['food_quality_avg']} / 5.0
- Sanitation & Desk Cleanliness: {$data['ratings']['cleanliness_avg']} / 5.0
- Speed of Prep & Service: {$data['ratings']['service_speed_avg']} / 5.0
- Price Fairness & Portion Value: {$data['ratings']['price_value_avg']} / 5.0
- General Review Survey Rating Count: {$data['ratings']['general_reviews_count']}
- Overall Star Rating Average: {$data['ratings']['general_reviews_avg']} / 5.0

--- ANONYMIZED STUDENT & STAFF VERBATIM COMMENTS ---
{$commentsStr}

Based on this historical dataset, generate an incredibly detailed, highly professional, Actionable Vendor Performance Analysis Report. Your tone must be constructive, objective, encouraging yet precise about failure points. Recommended sections:

1. **Executive Performance Rating**: Summarize their status in 2-3 sentences. Grade them (A+, B, etc.) based strictly on the metrics.
2. **Key Strengths (Backed by Data)**: Point out their biggest advantages. Highlight specific scores, popular item trends, or comments.
3. **Core Performance Bottlenecks & Operational Constraints**: Identify why orders are being declined, delayed, or what negative reviews are saying (e.g., cleanliness, price concern, slow prep times during rush hour).
4. **Strategic Action Roadmap (Next 30 Days)**: Give exactly 3 highly specific, creative, and realistic business recommendations for the vendor to implement to maximize revenue, improve operations based on the hourly peak distribution, and handle Accra Technical University student traffic more efficiently.

Make sure to format using clean Markdown with distinct sections, neat bold text, lists, and spacing. Include a professional closing line.";
    }

    /**
     * A fallback standard markdown generator to keep the app functional if API Key is not loaded or rate-limited.
     */
    private function getMockReportMarkdown(array $data): string
    {
        $topDish = $data['metrics']['top_dishes'][0]['food_name'] ?? 'Local Specialty Meals';
        $secondDish = $data['metrics']['top_dishes'][1]['food_name'] ?? 'Assorted Stews';
        
        $last7Days = $data['metrics']['weekly_comparison']['last_7_days'] ?? 0;
        $prev7Days = $data['metrics']['weekly_comparison']['prev_7_days'] ?? 0;
        $diff = $last7Days - $prev7Days;
        $trendText = $diff >= 0 ? "up by +{$diff} orders" : "down by " . abs($diff) . " orders";

        $lunchCount = $data['metrics']['hourly_distribution']['Lunch Peak (11am - 2:59pm)'] ?? 0;

        return "# 🖨️ ATU Cafeteria Performance Report: **{$data['vendor_name']}**
*(Local Fallback Report — Gemini AI Connection Dormant or Offline)*

### 1. Executive Performance Rating: **Grade B**
**{$data['vendor_name']}** shows consistent campus popularity, managing a total database intake of **{$data['metrics']['total_orders']}** orders with a completion success rate of **{$data['metrics']['completion_rate_percent']}%**. Their cumulative sales volume of **GH₵" . number_format($data['metrics']['total_revenue_ghs'], 2) . "** demonstrates heavy transaction velocity. Weekly activity shows they are **{$trendText}** compared to the previous week.

---

### 2. Key Strengths & Popular Item Trends
- **High Popularity**: Solid footprint on campus, particularly driven by high volume dishes like **{$topDish}** and **{$secondDish}**.
- **Culinary Quality Satisfaction**: Average food taste rating of **{$data['ratings']['food_quality_avg']} / 5.0** suggests students highly appreciate the flavor profile of the meals.
- **Economic Value**: Portions are graded at **{$data['ratings']['price_value_avg']} / 5.0**, marking this stall as pricing-friendly for the student demographic.

---

### 3. Core Performance Bottlenecks & Operational Constraints
- **Unfulfilled Orders**: There are **{$data['metrics']['declined_orders']} declined orders**, leading to leaked sales opportunity and customer disappointment.
- **Peak Hour Congestion**: With **{$lunchCount}** orders concentrated during the Lunch Peak (11am - 3pm), current speed index stands at **{$data['ratings']['service_speed_avg']} / 5.0**. This indicates bottlenecking during peak university lecture breaks, common for manual kitchen prep.
- **Sanitation Feedback**: Maintenance and prep station sanitization score of **{$data['ratings']['cleanliness_avg']} / 5.0** reveals minor student concerns regarding eating surface hygiene.

---

### 4. Strategic Action Roadmap (Next 30 Days)
1. **Prepare Ingredients in Batches**: Pre-chop, containerize, and setup cold storage assembly lines for high-traction items like **{$topDish}** prior to the intense 12:00 PM lecture break.
2. **Adopt Smart Digital Notification Queues**: Transition the student handoff stage entirely from manual calling to instant digital pickup alerts to shave minutes off the average queue.
3. **Weekly Sanitation Audits**: Create a structured cleaning schedule where food assembly tables, utensils, and service counters are fully sanitized every 2 hours during continuous kitchen service.";
    }
}
