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
}
