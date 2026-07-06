<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use Illuminate\Http\Request;
use App\Services\PerformanceAnalyticsService;

class VendorController extends Controller
{
    /**
     * Fetch the authenticated vendor's cafeteria menu items.
     */
    public function getMyFoodItems(Request $request)
    {
        $user = $request->user();

        if (strtoupper($user->role) !== 'VENDOR' && strtoupper($user->role) !== 'ADMIN') {
            return response()->json([
                'success' => false,
                'message' => 'Unauthorized. This resource requires VENDOR or ADMIN privileges.'
            ], 403);
        }

        $foodItems = $user->foodItems;

        return response()->json($foodItems, 200);
    }

    /**
     * Fetch the authenticated vendor's order history.
     */
    public function getMyOrders(Request $request)
    {
        $user = $request->user();

        if (strtoupper($user->role) !== 'VENDOR' && strtoupper($user->role) !== 'ADMIN') {
            return response()->json([
                'success' => false,
                'message' => 'Unauthorized. This resource requires VENDOR or ADMIN privileges.'
            ], 403);
        }

        $orders = $user->vendorOrders()->orderBy('order_timestamp', 'desc')->get();

        return response()->json($orders, 200);
    }

    /**
     * Fetch authenticated vendor's performance report (aggregate completion times, ratings, order volumes).
     */
    public function getMyAnalytics(Request $request)
    {
        $user = $request->user();

        if (strtoupper($user->role) !== 'VENDOR' && strtoupper($user->role) !== 'ADMIN') {
            return response()->json([
                'success' => false,
                'message' => 'Unauthorized. This resource requires VENDOR or ADMIN privileges.'
            ], 403);
        }

        $service = new PerformanceAnalyticsService();
        $report = $service->getVendorReport($user->id);

        return response()->json($report, 200);
    }

    /**
     * Fetch competitive vendor rankings and comparison report.
     */
    public function getComparativeAnalytics(Request $request)
    {
        $service = new PerformanceAnalyticsService();
        $report = $service->getComparativeVendorsReport();

        return response()->json($report, 200);
    }

    /**
     * Generate an AI-powered Gemini performance report for the logged-in vendor.
     */
    public function getMyGeminiReport(Request $request)
    {
        $user = $request->user();

        if (strtoupper($user->role) !== 'VENDOR' && strtoupper($user->role) !== 'ADMIN') {
            return response()->json([
                'success' => false,
                'message' => 'Unauthorized. This resource requires VENDOR or ADMIN privileges.'
            ], 403);
        }

        $service = new \App\Services\GeminiPerformanceReportService();
        $report = $service->generateReport($user->id);

        return response()->json($report, 200);
    }

    /**
     * Generate an AI-powered Gemini performance report for any specific vendor (e.g. for ADMIN overview).
     */
    public function getVendorGeminiReport(Request $request, $vendorId)
    {
        $service = new \App\Services\GeminiPerformanceReportService();
        $report = $service->generateReport(intval($vendorId));

        return response()->json($report, 200);
    }

    /**
     * Generate detailed Gemini AI Insights about popular food items and peak ordering times from historical logs.
     */
    public function getMyGeminiOrderInsights(Request $request)
    {
        $user = $request->user();

        if (strtoupper($user->role) !== 'VENDOR' && strtoupper($user->role) !== 'ADMIN') {
            return response()->json([
                'success' => false,
                'message' => 'Unauthorized. This resource requires VENDOR or ADMIN privileges.'
            ], 403);
        }

        $service = new \App\Services\GeminiOrderInsightService();
        $insights = $service->analyzeHistoricalOrders($user->id);

        return response()->json($insights, 200);
    }

    /**
     * Generate detailed Gemini AI Insights about popular food items and peak ordering times for any specific vendor (ADMIN overview).
     */
    public function getVendorGeminiOrderInsights(Request $request, $vendorId)
    {
        $service = new \App\Services\GeminiOrderInsightService();
        $insights = $service->analyzeHistoricalOrders(intval($vendorId));

        return response()->json($insights, 200);
    }

    /**
     * Toggle or explicitly set the authenticated vendor's open status (is_open).
     */
    public function toggleStatus(Request $request)
    {
        $user = $request->user();

        if (strtoupper($user->role) !== 'VENDOR' && strtoupper($user->role) !== 'ADMIN') {
            return response()->json([
                'success' => false,
                'message' => 'Unauthorized. This resource requires VENDOR or ADMIN privileges.'
            ], 403);
        }

        // Check if explicit status was passed, otherwise toggle the current status
        if ($request->has('is_open')) {
            $user->is_open = filter_var($request->input('is_open'), FILTER_VALIDATE_BOOLEAN);
        } else {
            $user->is_open = !$user->is_open;
        }

        $user->save();

        return response()->json([
            'success' => true,
            'is_open' => $user->is_open,
            'message' => "Cafeteria open status updated to " . ($user->is_open ? 'OPEN' : 'CLOSED') . "."
        ], 200);
    }

    /**
     * Display a beautiful Blade-based dashboard for vendors.
     */
    public function dashboardView(Request $request)
    {
        // Try authenticated user first, then fallback to vendor_id parameter (default: 10)
        $user = $request->user();
        if (!$user) {
            $vendorId = $request->query('vendor_id', 10);
            $user = \App\Models\User::find($vendorId);
        }

        if (!$user || (strtoupper($user->role) !== 'VENDOR' && strtoupper($user->role) !== 'ADMIN')) {
            $user = \App\Models\User::find(10); // fallback to Mary Joint (Auntie Mary Special)
        }

        // Search logic
        $search = $request->query('search');
        if (!empty($search)) {
            $foodItems = $user->foodItems()->where('name', 'LIKE', '%' . $search . '%')->get();
        } else {
            $foodItems = $user->foodItems;
        }

        $service = new PerformanceAnalyticsService();
        $metrics = $service->getVendorReport($user->id);

        // Weekly sales aggregation for Chart.js
        $weeklySalesData = [];
        for ($i = 6; $i >= 0; $i--) {
            $dateStr = now()->subDays($i)->format('Y-m-d');
            $dayName = now()->subDays($i)->format('D'); // e.g. Mon, Tue
            $weeklySalesData[$dateStr] = [
                'day' => $dayName,
                'sales' => 0.0
            ];
        }

        $sevenDaysAgo = now()->subDays(6)->startOfDay();
        $ordersLastSevenDays = \App\Models\Order::where('vendor_id', $user->id)
            ->where('status', 'COMPLETED')
            ->where('created_at', '>=', $sevenDaysAgo)
            ->select(\Illuminate\Support\Facades\DB::raw('DATE(created_at) as date_val'), \Illuminate\Support\Facades\DB::raw('SUM(total_price) as total_sales'))
            ->groupBy('date_val')
            ->get();

        foreach ($ordersLastSevenDays as $orderSales) {
            $dateKey = $orderSales->date_val;
            if (isset($weeklySalesData[$dateKey])) {
                $weeklySalesData[$dateKey]['sales'] = (float)$orderSales->total_sales;
            }
        }

        // Fetch vendor's orders history
        $orders = \App\Models\Order::where('vendor_id', $user->id)
            ->with(['customer', 'student', 'user'])
            ->orderBy('id', 'desc')
            ->get();

        // Calculate top performing food items based on completed order quantity/volume
        $topPerformingItems = \App\Models\Order::where('vendor_id', $user->id)
            ->whereIn('status', ['COMPLETED', 'DELIVERED'])
            ->select('food_name', \Illuminate\Support\Facades\DB::raw('SUM(quantity) as total_quantity'), \Illuminate\Support\Facades\DB::raw('COUNT(*) as total_orders'), \Illuminate\Support\Facades\DB::raw('SUM(total_price) as total_revenue'))
            ->groupBy('food_name')
            ->orderBy('total_quantity', 'desc')
            ->get();

        // If there are no completed orders yet, fallback to all orders to show mock / seeded data if applicable
        if ($topPerformingItems->isEmpty()) {
            $topPerformingItems = \App\Models\Order::where('vendor_id', $user->id)
                ->select('food_name', \Illuminate\Support\Facades\DB::raw('SUM(quantity) as total_quantity'), \Illuminate\Support\Facades\DB::raw('COUNT(*) as total_orders'), \Illuminate\Support\Facades\DB::raw('SUM(total_price) as total_revenue'))
                ->groupBy('food_name')
                ->orderBy('total_quantity', 'desc')
                ->get();
        }

        return view('vendor.dashboard', [
            'vendor' => $user,
            'foodItems' => $foodItems,
            'metrics' => $metrics,
            'search' => $search,
            'weeklySales' => array_values($weeklySalesData),
            'orders' => $orders,
            'topPerformingItems' => $topPerformingItems,
        ]);
    }
}
