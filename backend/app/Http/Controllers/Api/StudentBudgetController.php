<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\Order;
use App\Models\User;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;

class StudentBudgetController extends Controller
{
    /**
     * Map food items dynamically to major cafeteria food categories.
     */
    private function getFoodCategory(string $foodName): string
    {
        $normalized = strtolower(trim($foodName));

        if (str_contains($normalized, 'jollof') || str_contains($normalized, 'waakye') || str_contains($normalized, 'fried rice') || str_contains($normalized, 'rice')) {
            return 'Rice Dishes';
        }
        if (str_contains($normalized, 'indomie') || str_contains($normalized, 'nood') || str_contains($normalized, 'spag') || str_contains($normalized, 'pasta')) {
            return 'Noodles & Pasta';
        }
        if (str_contains($normalized, 'fufu') || str_contains($normalized, 'banku') || str_contains($normalized, 'kenkey') || str_contains($normalized, 'soup') || str_contains($normalized, 'stew')) {
            return 'Traditional Meals';
        }
        if (str_contains($normalized, 'burg') || str_contains($normalized, 'sandw') || str_contains($normalized, 'dog') || str_contains($normalized, 'bread') || str_contains($normalized, 'pizza') || str_contains($normalized, 'pie')) {
            return 'Fast Food & Snacks';
        }
        if (str_contains($normalized, 'chicken') || str_contains($normalized, 'wing') || str_contains($normalized, 'fish') || str_contains($normalized, 'tilapia') || str_contains($normalized, 'egg') || str_contains($normalized, 'meat')) {
            return 'Proteins & Sides';
        }
        if (str_contains($normalized, 'drink') || str_contains($normalized, 'coke') || str_contains($normalized, 'fanta') || str_contains($normalized, 'sprite') || str_contains($normalized, 'sobolo') || str_contains($normalized, 'water') || str_contains($normalized, 'juice') || str_contains($normalized, 'beverage')) {
            return 'Beverages';
        }

        return 'Other Treats';
    }

    /**
     * Aggregates personal spending history to build budget reports and Recharts datasets.
     */
    public function getBudgetAnalytics(Request $request)
    {
        $user = $request->user();
        if (! $user) {
            return response()->json([
                'success' => false,
                'message' => 'Unauthenticated.',
            ], 401);
        }

        // Only students or admins can access budget analytics
        $role = strtoupper($user->role);
        if ($role !== 'STUDENT' && $role !== 'ADMIN') {
            return response()->json([
                'success' => false,
                'message' => 'Unauthorized. Budget tracking and spending summaries are restricted to students.',
            ], 403);
        }

        // Fetch completed/delivered orders
        $orders = Order::where(function ($query) use ($user) {
            $query->where('customer_id', $user->id)
                ->orWhere('student_id', $user->id)
                ->orWhere('user_id', $user->id);
        })
            ->whereIn(DB::raw('upper(status)'), ['COMPLETED', 'DELIVERED'])
            ->orderBy('order_timestamp', 'asc')
            ->get();

        // Get monthly budget limit from user profile (default to GH₵ 400.00 if unconfigured)
        $profile = $user->profile_info ?: [];
        $monthlyBudgetLimit = isset($profile['monthly_budget_limit']) ? (float) $profile['monthly_budget_limit'] : 400.0;

        // Structure 1: Monthly Aggregations for Recharts (e.g. BarChart)
        $monthlyAggregations = [];
        // Structure 2: Category Aggregations for Recharts (e.g. PieChart)
        $categoryAggregations = [];
        // Structure 3: Daily Spending Aggregations for Recharts (e.g. AreaChart) in the last 30 days
        $dailyAggregations = [];

        // Pre-fill categories to ensure consistent color mapping in Recharts
        $categoriesList = ['Rice Dishes', 'Noodles & Pasta', 'Traditional Meals', 'Fast Food & Snacks', 'Proteins & Sides', 'Beverages', 'Other Treats'];
        foreach ($categoriesList as $cat) {
            $categoryAggregations[$cat] = [
                'category' => $cat,
                'total_spent' => 0.0,
                'item_count' => 0,
            ];
        }

        $totalSpentAllTime = 0.0;
        $totalOrdersCount = count($orders);

        // Get current year and month
        $currentYear = (int) date('Y');
        $currentMonth = (int) date('n');

        $currentMonthSpent = 0.0;

        foreach ($orders as $order) {
            $timestampSec = $order->order_timestamp / 1000;
            $year = (int) date('Y', $timestampSec);
            $monthNum = (int) date('n', $timestampSec);
            $monthName = date('M Y', $timestampSec);
            $dayString = date('Y-m-d', $timestampSec);

            $price = (float) $order->total_price;
            $totalSpentAllTime += $price;

            // 1. Accumulate Monthly
            $monthKey = "{$year}-".str_pad($monthNum, 2, '0', STR_PAD_LEFT);
            if (! isset($monthlyAggregations[$monthKey])) {
                $monthlyAggregations[$monthKey] = [
                    'key' => $monthKey,
                    'month' => $monthName,
                    'year' => $year,
                    'month_num' => $monthNum,
                    'total_spent' => 0.0,
                    'budget_limit' => $monthlyBudgetLimit,
                    'order_count' => 0,
                    'average_order_value' => 0.0,
                ];
            }
            $monthlyAggregations[$monthKey]['total_spent'] += $price;
            $monthlyAggregations[$monthKey]['order_count']++;

            if ($year === $currentYear && $monthNum === $currentMonth) {
                $currentMonthSpent += $price;
            }

            // 2. Accumulate Category
            $category = $this->getFoodCategory($order->food_name);
            $categoryAggregations[$category]['total_spent'] += $price;
            $categoryAggregations[$category]['item_count'] += ($order->quantity ?: 1);

            // 3. Accumulate Daily (filter last 30 days only)
            $thirtyDaysAgoMs = (time() - (30 * 24 * 60 * 60)) * 1000;
            if ($order->order_timestamp >= $thirtyDaysAgoMs) {
                if (! isset($dailyAggregations[$dayString])) {
                    $dailyAggregations[$dayString] = [
                        'date' => date('M d', $timestampSec),
                        'full_date' => $dayString,
                        'total_spent' => 0.0,
                        'order_count' => 0,
                    ];
                }
                $dailyAggregations[$dayString]['total_spent'] += $price;
                $dailyAggregations[$dayString]['order_count']++;
            }
        }

        // Refine calculations for monthly
        foreach ($monthlyAggregations as $key => $data) {
            $monthlyAggregations[$key]['total_spent'] = round($data['total_spent'], 2);
            $monthlyAggregations[$key]['average_order_value'] = round($data['total_spent'] / $data['order_count'], 2);
            $monthlyAggregations[$key]['compliance_percentage'] = round(($monthlyAggregations[$key]['total_spent'] / $data['budget_limit']) * 100, 1);
        }
        // Ensure chronological order for months
        ksort($monthlyAggregations);

        // Refine calculations for category
        $finalCategories = [];
        foreach ($categoryAggregations as $cat => $data) {
            if ($data['total_spent'] > 0) {
                $data['total_spent'] = round($data['total_spent'], 2);
                $finalCategories[] = $data;
            }
        }

        // Sort days chronologically
        ksort($dailyAggregations);
        $finalDaily = array_values($dailyAggregations);

        // Determine current budget status
        $budgetPercent = $monthlyBudgetLimit > 0 ? ($currentMonthSpent / $monthlyBudgetLimit) * 100 : 0;
        $statusLevel = 'UNDER';
        if ($budgetPercent >= 100.0) {
            $statusLevel = 'EXCEEDED';
        } elseif ($budgetPercent >= 80.0) {
            $statusLevel = 'WARNING';
        }

        // General overall statistics
        $overallMetrics = [
            'total_spent_all_time' => round($totalSpentAllTime, 2),
            'total_orders_count' => $totalOrdersCount,
            'current_month_spent' => round($currentMonthSpent, 2),
            'monthly_budget_limit' => $monthlyBudgetLimit,
            'budget_consumption_percentage' => round($budgetPercent, 1),
            'budget_status_level' => $statusLevel,
            'remaining_budget' => round(max(0.0, $monthlyBudgetLimit - $currentMonthSpent), 2),
        ];

        // Return deterministic budget analytics from verified transaction history.
        $budgetAdvice = $this->generateMockBudgetAdvice(
            $user->fullName ?: $user->username,
            $overallMetrics,
            $finalCategories
        );

        return response()->json([
            'success' => true,
            'student_id' => $user->id,
            'student_name' => $user->fullName ?: $user->username,
            'overall_metrics' => $overallMetrics,
            'monthly_chart_data' => array_values($monthlyAggregations),
            'category_chart_data' => $finalCategories,
            'daily_trend_chart_data' => $finalDaily,
            'budget_advice' => $budgetAdvice,
            'generated_at' => date('c'),
        ], 200);
    }

    /**
     * Set/update the authenticated student's monthly budget limit.
     */
    public function setBudgetLimit(Request $request)
    {
        $user = $request->user();
        if (! $user) {
            return response()->json([
                'success' => false,
                'message' => 'Unauthenticated.',
            ], 401);
        }

        $role = strtoupper($user->role);
        if ($role !== 'STUDENT' && $role !== 'ADMIN') {
            return response()->json([
                'success' => false,
                'message' => 'Unauthorized. Only students can set personal budget limits.',
            ], 403);
        }

        $request->validate([
            'monthly_budget_limit' => 'required|numeric|min:1',
        ]);

        $limit = (float) $request->input('monthly_budget_limit');

        $profile = $user->profile_info ?: [];
        $profile['monthly_budget_limit'] = $limit;

        $user->profile_info = $profile;
        $user->save();

        return response()->json([
            'success' => true,
            'message' => 'Monthly spending budget threshold updated successfully.',
            'monthly_budget_limit' => $limit,
            'updated_at' => date('c'),
        ], 200);
    }

    /**
     * Dynamic smart fallback advice generator.
     */
    private function generateMockBudgetAdvice(string $name, array $metrics, array $categories): string
    {
        $categoryDetails = '';
        if (empty($categories)) {
            $categoryDetails = "We haven't recorded any meal purchases for you this week. Once you start placing orders at the cafeteria, we will analyze your distribution trends!";
        } else {
            // Sort categories to get top one
            usort($categories, function ($a, $b) {
                return $b['total_spent'] <=> $a['total_spent'];
            });
            $topCat = $categories[0]['category'];
            $topSpent = $categories[0]['total_spent'];
            $categoryDetails = "Our analyzer indicates you have spent the most on **{$topCat}** (totaling **GH₵ {$topSpent}**). Balancing high-velocity treats with staple grains keeps your pockets secure.";
        }

        $statusSection = '';
        if ($metrics['budget_status_level'] === 'EXCEEDED') {
            $statusSection = "⚠️ **Budget Warning**: You have fully consumed your budget of **GH₵ {$metrics['monthly_budget_limit']}** (currently at **".$metrics['budget_consumption_percentage'].'%**). Consider cooking simple meals or opting for budget staple pairings for the rest of the week.';
        } elseif ($metrics['budget_status_level'] === 'WARNING') {
            $statusSection = '🔔 **Caution**: You have utilized **'.$metrics['budget_consumption_percentage']."%** of your budget. You only have **GH₵ {$metrics['remaining_budget']}** left. Slow down on premium fast foods to stay on track!";
        } else {
            $statusSection = '🎉 **Excellent Discipline!**: You are currently safely within your budget limits. You have utilized only **'.$metrics['budget_consumption_percentage']."%** of your **GH₵ {$metrics['monthly_budget_limit']}** target. Great job managing your pocket money!";
        }

        return "# 📊 Personal Student Savings & Budgeting Guide
Hello **{$name}**! Welcome to your personalized ATU Cafeteria spending advisor. Here is a review of your campus dining financials.

---

### 1. 🔍 Personal Spending Diagnostics
{$categoryDetails}

---

### 2. 🛡️ Budget Health Assessment
{$statusSection}

---

### 3. 💡 3 Actionable ATU Cafeteria Savings Hacks
1.  **🍚 Lean on Traditional Volume Staples**: Choose local meals like *Waakye* or *Banku* instead of pizza or loaded burgers. Traditional meals at ATU Cafeteria offer significantly better nutritional density and larger portions per Cedi.
2.  **💧 The Sobolo Swap**: Opt for local *Sobolo* or mineral water instead of brand-name carbonated cans. This single substitution can save you up to GH₵ 50.00 every month!
3.  **🍗 Strategic Custom Protein Combining**: Instead of ordering two separate main meals, buy one solid rice portion and add a side of boiled egg or wele. This satisfies your appetite at half the cost.";
    }
}
