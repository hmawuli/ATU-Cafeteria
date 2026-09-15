<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\Order;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;

class OrderItemMetricsController extends Controller
{
    /**
     * Get aggregated metrics from order_items for dashboard visualization.
     * Includes total daily revenue, order counts per menu/food item, and other key KPIs.
     */
    public function getDashboardMetrics(Request $request)
    {
        $user = $request->user();
        $vendorId = $request->input('vendor_id');

        // Enforcement: If VENDOR, restrict to their own ID.
        if ($user && strtoupper($user->role) === 'VENDOR') {
            $vendorId = $user->id;
        }

        $startDate = $request->input('start_date');
        $endDate = $request->input('end_date');
        $status = $request->input('status', 'COMPLETED'); // default to completed orders for revenue metrics

        // Base query for daily revenue from order_items
        $driver = DB::connection()->getDriverName();
        if ($driver === 'sqlite') {
            $dateExpr = "strftime('%Y-%m-%d', datetime(order_items.created_at, 'localtime'))";
        } else {
            $dateExpr = 'CAST(order_items.created_at AS DATE)';
        }

        // --- 1. Daily Revenue Aggregated via order_items ---
        $revenueQuery = DB::table('order_items')
            ->join('orders', 'order_items.order_id', '=', 'orders.id')
            ->select([
                DB::raw("$dateExpr as order_date"),
                DB::raw('SUM(order_items.total_price) as daily_revenue'),
                DB::raw('COUNT(DISTINCT order_items.order_id) as total_orders'),
                DB::raw('SUM(order_items.quantity) as total_items_sold'),
            ]);

        if ($vendorId) {
            $revenueQuery->where('orders.vendor_id', $vendorId);
        }
        if ($status && $status !== 'ALL') {
            $revenueQuery->where('orders.status', strtoupper($status));
        }
        if ($startDate) {
            $revenueQuery->whereDate('order_items.created_at', '>=', $startDate);
        }
        if ($endDate) {
            $revenueQuery->whereDate('order_items.created_at', '<=', $endDate);
        }

        $dailyRevenue = $revenueQuery
            ->groupBy(DB::raw($dateExpr))
            ->orderBy('order_date', 'asc')
            ->get()
            ->map(function ($row) {
                return [
                    'date' => $row->order_date,
                    'revenue' => round(floatval($row->daily_revenue), 2),
                    'orders_count' => intval($row->total_orders),
                    'items_sold_count' => intval($row->total_items_sold),
                ];
            });

        // --- 2. Metrics Per Menu/Food Item ---
        $itemMetricsQuery = DB::table('order_items')
            ->join('orders', 'order_items.order_id', '=', 'orders.id')
            ->select([
                'order_items.food_item_id',
                'order_items.name as item_name',
                DB::raw('COUNT(DISTINCT order_items.order_id) as orders_count'),
                DB::raw('SUM(order_items.quantity) as total_quantity_sold'),
                DB::raw('SUM(order_items.total_price) as total_revenue'),
                DB::raw('AVG(order_items.unit_price) as average_unit_price'),
            ]);

        if ($vendorId) {
            $itemMetricsQuery->where('orders.vendor_id', $vendorId);
        }
        if ($status && $status !== 'ALL') {
            $itemMetricsQuery->where('orders.status', strtoupper($status));
        }
        if ($startDate) {
            $itemMetricsQuery->whereDate('order_items.created_at', '>=', $startDate);
        }
        if ($endDate) {
            $itemMetricsQuery->whereDate('order_items.created_at', '<=', $endDate);
        }

        $itemMetrics = $itemMetricsQuery
            ->groupBy('order_items.food_item_id', 'order_items.name')
            ->orderBy('total_quantity_sold', 'desc')
            ->get()
            ->map(function ($row) {
                return [
                    'food_item_id' => intval($row->food_item_id),
                    'item_name' => $row->item_name,
                    'orders_count' => intval($row->orders_count),
                    'total_quantity_sold' => intval($row->total_quantity_sold),
                    'total_revenue' => round(floatval($row->total_revenue), 2),
                    'average_unit_price' => round(floatval($row->average_unit_price), 2),
                ];
            });

        // --- 3. Overall Dashboard KPI Summary ---
        $totalRevenueSum = $dailyRevenue->sum('revenue');
        $totalOrdersCount = $dailyRevenue->sum('orders_count');
        $totalItemsSoldSum = $dailyRevenue->sum('items_sold_count');
        $averageOrderValue = $totalOrdersCount > 0 ? round($totalRevenueSum / $totalOrdersCount, 2) : 0.0;

        return response()->json([
            'success' => true,
            'message' => 'Dashboard order items metrics aggregated successfully.',
            'filters' => [
                'vendor_id' => $vendorId ? intval($vendorId) : null,
                'start_date' => $startDate,
                'end_date' => $endDate,
                'status' => $status,
            ],
            'kpis' => [
                'total_revenue' => round($totalRevenueSum, 2),
                'total_orders' => intval($totalOrdersCount),
                'total_items_sold' => intval($totalItemsSoldSum),
                'average_order_value' => $averageOrderValue,
            ],
            'daily_revenue' => $dailyRevenue,
            'menu_item_metrics' => $itemMetrics,
            'generated_at' => date('Y-m-d H:i:s'),
        ], 200);
    }

    /**
     * Get daily revenue metrics specifically.
     */
    public function getDailyRevenueMetrics(Request $request)
    {
        $user = $request->user();
        $vendorId = $request->input('vendor_id');

        if ($user && strtoupper($user->role) === 'VENDOR') {
            $vendorId = $user->id;
        }

        $driver = DB::connection()->getDriverName();
        if ($driver === 'sqlite') {
            $dateExpr = "strftime('%Y-%m-%d', datetime(order_items.created_at, 'localtime'))";
        } else {
            $dateExpr = 'CAST(order_items.created_at AS DATE)';
        }

        $query = DB::table('order_items')
            ->join('orders', 'order_items.order_id', '=', 'orders.id')
            ->select([
                DB::raw("$dateExpr as order_date"),
                DB::raw('SUM(order_items.total_price) as daily_revenue'),
            ])
            ->where('orders.status', 'COMPLETED');

        if ($vendorId) {
            $query->where('orders.vendor_id', $vendorId);
        }

        $results = $query->groupBy(DB::raw($dateExpr))
            ->orderBy('order_date', 'asc')
            ->get()
            ->map(function ($row) {
                return [
                    'date' => $row->order_date,
                    'revenue' => round(floatval($row->daily_revenue), 2),
                ];
            });

        return response()->json([
            'success' => true,
            'data' => $results,
            'generated_at' => date('Y-m-d H:i:s'),
        ], 200);
    }

    /**
     * Get order counts and metrics per menu/food item specifically.
     */
    public function getMenuItemMetrics(Request $request)
    {
        $user = $request->user();
        $vendorId = $request->input('vendor_id');

        if ($user && strtoupper($user->role) === 'VENDOR') {
            $vendorId = $user->id;
        }

        $query = DB::table('order_items')
            ->join('orders', 'order_items.order_id', '=', 'orders.id')
            ->select([
                'order_items.food_item_id',
                'order_items.name as item_name',
                DB::raw('COUNT(DISTINCT order_items.order_id) as orders_count'),
                DB::raw('SUM(order_items.quantity) as total_quantity_sold'),
            ])
            ->where('orders.status', 'COMPLETED');

        if ($vendorId) {
            $query->where('orders.vendor_id', $vendorId);
        }

        $results = $query->groupBy('order_items.food_item_id', 'order_items.name')
            ->orderBy('total_quantity_sold', 'desc')
            ->get()
            ->map(function ($row) {
                return [
                    'food_item_id' => intval($row->food_item_id),
                    'item_name' => $row->item_name,
                    'orders_count' => intval($row->orders_count),
                    'total_quantity_sold' => intval($row->total_quantity_sold),
                ];
            });

        return response()->json([
            'success' => true,
            'data' => $results,
            'generated_at' => date('Y-m-d H:i:s'),
        ], 200);
    }
}
