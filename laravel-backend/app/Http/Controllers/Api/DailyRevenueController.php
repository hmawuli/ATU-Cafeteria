<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use App\Models\Order;
use App\Models\User;

class DailyRevenueController extends Controller
{
    /**
     * Get aggregated daily order totals and revenue for the authenticated vendor (or specified vendor if Admin).
     */
    public function getDailyRevenue(Request $request)
    {
        $user = $request->user();

        if (!$user) {
            return response()->json([
                'success' => false,
                'message' => 'Unauthorized access.'
            ], 401);
        }

        $role = strtoupper($user->role);
        if ($role !== 'VENDOR' && $role !== 'ADMIN') {
            return response()->json([
                'success' => false,
                'message' => 'Unauthorized role.'
            ], 403);
        }

        // Default vendor ID to current user's ID
        $vendorId = $user->id;

        // Admins can filter by a specific vendor_id query param
        if ($role === 'ADMIN' && $request->has('vendor_id')) {
            $vendorId = intval($request->input('vendor_id'));
        }

        // Group by Date and aggregate totals
        // Support SQLite/MySQL/PostgreSQL dates natively
        $driver = DB::connection()->getDriverName();
        if ($driver === 'sqlite') {
            $dateExpr = "strftime('%Y-%m-%d', datetime(created_at, 'localtime'))";
        } else {
            $dateExpr = "CAST(created_at AS DATE)";
        }

        $results = Order::where('vendor_id', $vendorId)
            ->where('status', 'COMPLETED')
            ->select([
                DB::raw("$dateExpr as revenue_date"),
                DB::raw("COUNT(id) as total_orders"),
                DB::raw("SUM(total_price) as total_revenue")
            ])
            ->groupBy(DB::raw($dateExpr))
            ->orderBy('revenue_date', 'asc')
            ->get();

        $chartData = $results->map(function ($row) {
            return [
                'date' => $row->revenue_date,
                'orders_count' => intval($row->total_orders),
                'revenue' => round(floatval($row->total_revenue), 2)
            ];
        });

        // Fetch vendor name as metadata
        $vendorName = User::where('id', $vendorId)->value('fullName') ?? 'Vendor';

        return response()->json([
            'success' => true,
            'vendor_id' => $vendorId,
            'vendor_name' => $vendorName,
            'data' => $chartData,
            'generated_at' => date('Y-m-d H:i:s')
        ], 200);
    }

    /**
     * Calculate and return daily sales revenue for a specific vendor.
     */
    public function getVendorDailyRevenue($vendorId, Request $request)
    {
        // Check if vendor exists
        $vendor = User::find($vendorId);
        if (!$vendor) {
            return response()->json([
                'success' => false,
                'message' => 'Vendor not found.'
            ], 404);
        }

        // Base query for orders belonging to this vendor
        $query = Order::where('vendor_id', $vendorId)
            ->where('status', 'COMPLETED');

        // Optional date filter (format: YYYY-MM-DD)
        if ($request->has('date') && $request->input('date') !== '') {
            $date = $request->input('date');
            $startTimestamp = strtotime($date . ' 00:00:00') * 1000;
            $endTimestamp = strtotime($date . ' 23:59:59') * 1000;
            if ($startTimestamp && $endTimestamp) {
                $query->whereBetween('order_timestamp', [$startTimestamp, $endTimestamp]);
            }
        }

        // Let's get the daily grouped totals
        $driver = DB::connection()->getDriverName();
        if ($driver === 'sqlite') {
            $dateExpr = "strftime('%Y-%m-%d', datetime(created_at, 'localtime'))";
        } else {
            $dateExpr = "CAST(created_at AS DATE)";
        }

        $results = $query->select([
                DB::raw("$dateExpr as revenue_date"),
                DB::raw("COUNT(id) as total_completed_orders"),
                DB::raw("SUM(total_price) as total_revenue")
            ])
            ->groupBy(DB::raw($dateExpr))
            ->orderBy('revenue_date', 'desc')
            ->get();

        $dailyData = $results->map(function ($row) {
            return [
                'date' => $row->revenue_date,
                'completed_orders_count' => intval($row->total_completed_orders),
                'revenue' => round(floatval($row->total_revenue), 2)
            ];
        });

        // Let's also compute the total aggregate completed revenue for this vendor
        $totalAggregateRevenue = Order::where('vendor_id', $vendorId)
            ->where('status', 'COMPLETED')
            ->sum('total_price');

        return response()->json([
            'success' => true,
            'vendor_id' => (int) $vendorId,
            'vendor_name' => $vendor->fullName,
            'total_aggregate_revenue' => round(floatval($totalAggregateRevenue), 2),
            'daily_sales_revenue' => $dailyData,
            'generated_at' => date('Y-m-d H:i:s')
        ], 200);
    }
}
