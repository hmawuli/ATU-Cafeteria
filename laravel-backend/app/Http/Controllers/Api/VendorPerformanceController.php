<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use App\Models\Order;
use App\Models\User;

class VendorPerformanceController extends Controller
{
    /**
     * Fetch daily revenue and total order count per vendor.
     * Supports filtering by vendor_id, start_date (Y-m-d), and end_date (Y-m-d).
     *
     * @param  \Illuminate\Http\Request  $request
     * @return \Illuminate\Http\JsonResponse
     */
    public function getPerformance(Request $request)
    {
        // 1. Authorization checks (Only active vendors or admin role can view)
        $user = $request->user();
        if ($user) {
            $role = strtoupper($user->role);
            if ($role !== 'VENDOR' && $role !== 'ADMIN') {
                return response()->json([
                    'success' => false,
                    'message' => 'Unauthorized. This resource requires VENDOR or ADMIN privileges.'
                ], 403);
            }

            // If a vendor is requesting, restrict them to their own statistics
            if ($role === 'VENDOR') {
                $request->merge(['vendor_id' => $user->id]);
            }
        }

        // 2. Validate possible input filters
        $vendorId = $request->input('vendor_id');
        $startDate = $request->input('start_date');
        $endDate = $request->input('end_date');

        // 3. Build Query for Daily Metrics across All or Selected Vendors
        $query = DB::table('orders')
            ->join('users', 'orders.vendor_id', '=', 'users.id')
            ->select(
                'orders.vendor_id',
                'users.fullName as vendor_name',
                DB::raw('CAST(orders.created_at AS DATE) as order_date'),
                DB::raw('COUNT(orders.id) as total_orders'),
                DB::raw('SUM(CASE WHEN orders.status = \'COMPLETED\' THEN orders.total_price ELSE 0 END) as daily_revenue'),
                DB::raw('SUM(CASE WHEN orders.status = \'COMPLETED\' THEN 1 ELSE 0 END) as completed_orders')
            );

        if ($vendorId) {
            $query->where('orders.vendor_id', $vendorId);
        }

        if ($startDate) {
            $query->whereDate('orders.created_at', '>=', $startDate);
        }

        if ($endDate) {
            $query->whereDate('orders.created_at', '<=', $endDate);
        }

        $dailyMetrics = $query
            ->groupBy('orders.vendor_id', 'users.fullName', DB::raw('CAST(orders.created_at AS DATE)'))
            ->orderBy('order_date', 'desc')
            ->orderBy('users.fullName', 'asc')
            ->get();

        // 4. Build Cumulative Summary Metrics across Vendors to enrich the dashboard resource
        $summaryQuery = DB::table('orders')
            ->join('users', 'orders.vendor_id', '=', 'users.id')
            ->select(
                'orders.vendor_id',
                'users.fullName as vendor_name',
                DB::raw('COUNT(orders.id) as total_orders'),
                DB::raw('SUM(CASE WHEN orders.status = \'COMPLETED\' THEN orders.total_price ELSE 0 END) as cumulative_revenue'),
                DB::raw('SUM(CASE WHEN orders.status = \'COMPLETED\' THEN 1 ELSE 0 END) as completed_orders'),
                DB::raw('SUM(CASE WHEN orders.status = \'DECLINED\' THEN 1 ELSE 0 END) as declined_orders')
            );

        if ($vendorId) {
            $summaryQuery->where('orders.vendor_id', $vendorId);
        }

        if ($startDate) {
            $summaryQuery->whereDate('orders.created_at', '>=', $startDate);
        }

        if ($endDate) {
            $summaryQuery->whereDate('orders.created_at', '<=', $endDate);
        }

        $vendorSummaries = $summaryQuery
            ->groupBy('orders.vendor_id', 'users.fullName')
            ->get()
            ->map(function ($row) {
                $denominator = $row->total_orders - $row->declined_orders;
                $row->completion_rate_percentage = $denominator > 0 
                    ? round(($row->completed_orders / $denominator) * 100, 1) 
                    : 0.0;
                $row->cumulative_revenue = round(floatval($row->cumulative_revenue), 2);
                return $row;
            });

        // 5. Structure final high-fidelity response resource
        return response()->json([
            'success' => true,
            'filters' => [
                'vendor_id' => $vendorId ? intval($vendorId) : null,
                'start_date' => $startDate,
                'end_date' => $endDate,
            ],
            'summary' => $vendorSummaries,
            'daily_performance' => $dailyMetrics->map(function($metric) {
                return [
                    'vendor_id' => intval($metric->vendor_id),
                    'vendor_name' => $metric->vendor_name,
                    'order_date' => $metric->order_date,
                    'total_orders' => intval($metric->total_orders),
                    'completed_orders' => intval($metric->completed_orders),
                    'daily_revenue' => round(floatval($metric->daily_revenue), 2),
                ];
            }),
            'generated_at' => date('Y-m-d H:i:s'),
        ], 200);
    }
}
