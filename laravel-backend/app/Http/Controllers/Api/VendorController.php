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
}
