<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Http\Requests\UpdateMenuAvailabilityRequest;
use App\Models\AuditLog;
use App\Models\Feedback;
use App\Models\FoodItem;
use App\Models\Menu;
use App\Models\MenuItem;
use App\Models\Order;
use App\Models\VendorMenuAvailability;
use App\Models\VendorOrderSummary;
use Carbon\Carbon;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;

class VendorSpecificController extends Controller
{
    /**
     * Update menu availability for a vendor's menu item or food item.
     * Supports both general toggle and day/time-specific scheduling.
     */
    public function updateMenuAvailability(UpdateMenuAvailabilityRequest $request)
    {
        $user = $request->user();

        $itemId = $request->input('item_id');
        $itemType = $request->input('item_type');
        $isAvailable = filter_var($request->input('is_available'), FILTER_VALIDATE_BOOLEAN);

        $itemName = '';

        DB::beginTransaction();
        try {
            // 1. Update the primary items availability
            if ($itemType === 'food_item') {
                $item = FoodItem::where('id', $itemId)
                    ->where('vendor_id', $user->id)
                    ->first();

                if (! $item) {
                    DB::rollBack();

                    return response()->json([
                        'success' => false,
                        'message' => 'Food Item not found or does not belong to vendor.',
                    ], 404);
                }

                $item->is_available = $isAvailable;
                $item->save();
                $itemName = $item->name;

            } else {
                $item = MenuItem::where('id', $itemId)
                    ->where('vendor_id', $user->id)
                    ->first();

                if (! $item) {
                    DB::rollBack();

                    return response()->json([
                        'success' => false,
                        'message' => 'Menu Item not found or does not belong to vendor.',
                    ], 404);
                }

                $item->is_available = $isAvailable;
                $item->save();
                $itemName = $item->name;
            }

            // 2. If day_of_week is provided, store detailed scheduling rules in vendor_menu_availabilities table
            $scheduledRule = null;
            if ($request->has('day_of_week') && $itemType === 'menu_item') {
                $scheduledRule = VendorMenuAvailability::updateOrCreate(
                    [
                        'vendor_id' => $user->id,
                        'menu_item_id' => $itemId,
                        'day_of_week' => strtolower($request->input('day_of_week')),
                    ],
                    [
                        'start_time' => $request->input('start_time'),
                        'end_time' => $request->input('end_time'),
                        'is_active' => $isAvailable,
                    ]
                );
            }

            // 3. Write dynamic Audit Log
            AuditLog::create([
                'user_id' => $user->id,
                'timestamp' => time() * 1000,
                'action' => 'MENU_AVAILABILITY_UPDATED',
                'details' => "Updated availability of item '{$itemName}' ({$itemType}) to ".($isAvailable ? 'AVAILABLE' : 'UNAVAILABLE').'.',
            ]);

            DB::commit();

            return response()->json([
                'success' => true,
                'message' => "Successfully updated availability of '{$itemName}' to ".($isAvailable ? 'Available' : 'Unavailable').'.',
                'item_id' => $itemId,
                'item_type' => $itemType,
                'is_available' => $isAvailable,
                'schedule_rule' => $scheduledRule,
            ], 200);

        } catch (\Exception $e) {
            DB::rollBack();

            return response()->json([
                'success' => false,
                'message' => 'Server error while updating menu availability.',
                'error' => $e->getMessage(),
            ], 500);
        }
    }

    /**
     * Retrieve the current order summaries for the authenticated vendor.
     * Dynamically aggregates database orders by status and caches in vendor_order_summaries.
     */
    public function getOrderSummary(Request $request)
    {
        $user = $request->user();

        if (! $user || (strtoupper($user->role) !== 'VENDOR' && strtoupper($user->role) !== 'ADMIN')) {
            return response()->json([
                'success' => false,
                'message' => 'Unauthorized. This endpoint requires VENDOR or ADMIN privileges.',
            ], 403);
        }

        $today = Carbon::today()->toDateString();

        try {
            // Aggregate totals for the vendor
            $allOrders = Order::where('vendor_id', $user->id)->get();
            $todayOrders = Order::where('vendor_id', $user->id)
                ->whereDate('created_at', $today)
                ->get();

            // Status distribution (Historical vs Today)
            $statusCountsToday = [
                'PENDING' => 0,
                'PREPARING' => 0,
                'READY' => 0,
                'COMPLETED' => 0,
                'CANCELLED' => 0,
                'DECLINED' => 0,
            ];

            foreach ($todayOrders as $order) {
                $status = strtoupper($order->status);
                if (array_key_exists($status, $statusCountsToday)) {
                    $statusCountsToday[$status]++;
                }
            }

            $totalRevenueHistoric = $allOrders->where('status', 'COMPLETED')->sum('total_price');
            $todayRevenue = $todayOrders->where('status', 'COMPLETED')->sum('total_price');

            // Calculate active vendor overall ratings
            $feedbackScores = Feedback::where('vendor_id', $user->id)->get();
            $avgRating = $feedbackScores->count() > 0
                ? round($feedbackScores->average('rating_food_quality'), 2)
                : 5.00;

            // 1. Cache the summary state in the vendor_order_summaries database table
            $cachedSummary = VendorOrderSummary::updateOrCreate(
                [
                    'vendor_id' => $user->id,
                    'summary_date' => $today,
                ],
                [
                    'total_orders' => $todayOrders->count(),
                    'completed_orders' => $statusCountsToday['COMPLETED'],
                    'pending_orders' => $statusCountsToday['PENDING'] + $statusCountsToday['PREPARING'],
                    'total_revenue' => $todayRevenue,
                    'average_rating' => $avgRating,
                ]
            );

            // 2. Identify items that are running low on stock based on actual order frequency
            $lowStockItems = [];
            $foodItems = FoodItem::where('vendor_id', $user->id)
                ->where('is_available', true)
                ->get();

            foreach ($foodItems as $item) {
                // Calculate order frequency in the last 24 hours
                $timestamp24hAgo = (time() - 24 * 60 * 60) * 1000;
                $orderFrequency24h = (int) Order::where('food_item_id', $item->id)
                    ->whereNotIn(DB::raw('UPPER(status)'), ['CANCELLED', 'DECLINED'])
                    ->where('order_timestamp', '>=', $timestamp24hAgo)
                    ->sum('quantity');

                // Simulate realistic standard stock limit (default 35 portions per item per day) subtract orders
                $totalTodaySum = (int) Order::where('food_item_id', $item->id)
                    ->whereNotIn(DB::raw('UPPER(status)'), ['CANCELLED', 'DECLINED'])
                    ->where('order_timestamp', '>=', (time() - 12 * 60 * 60) * 1000)
                    ->sum('quantity');

                $startingLimit = 35;
                $remainingStock = max(0, $startingLimit - $totalTodaySum);

                // Dynamic threshold
                $lowStockThreshold = max(4, (int) round($orderFrequency24h * 0.40));

                if ($remainingStock <= $lowStockThreshold) {
                    $lowStockItems[] = [
                        'id' => $item->id,
                        'name' => $item->name,
                        'price' => $item->price,
                        'description' => $item->description,
                        'remaining_stock' => $remainingStock,
                        'order_frequency_24h' => $orderFrequency24h,
                        'threshold' => $lowStockThreshold,
                        'warning' => $remainingStock === 0 ? 'SOLD_OUT' : 'LOW_STOCK',
                    ];
                }
            }

            return response()->json([
                'success' => true,
                'message' => 'Successfully captured current order summaries.',
                'summary_date' => $today,
                'cached_summary_id' => $cachedSummary->id,
                'metrics' => [
                    'today_orders_count' => $todayOrders->count(),
                    'today_revenue' => (float) $todayRevenue,
                    'historic_revenue' => (float) $totalRevenueHistoric,
                    'average_rating' => (float) $avgRating,
                    'today_status_breakdown' => $statusCountsToday,
                ],
                'notifications' => [
                    'unresolved_pending_count' => $statusCountsToday['PENDING'],
                    'active_preparing_count' => $statusCountsToday['PREPARING'],
                    'ready_pickup_count' => $statusCountsToday['READY'],
                ],
                'low_stock_warnings' => $lowStockItems,
            ], 200);

        } catch (\Exception $e) {
            return response()->json([
                'success' => false,
                'message' => 'Error capturing current order summaries.',
                'error' => $e->getMessage(),
            ], 500);
        }
    }

    /**
     * Bulk update multiple menu item prices or availability via JSON.
     * Supports both JSON file uploads and raw array input.
     */
    public function bulkUpdateMenu(Request $request)
    {
        $user = $request->user();

        if (! $user || (strtoupper($user->role) !== 'VENDOR' && strtoupper($user->role) !== 'ADMIN')) {
            return response()->json([
                'success' => false,
                'message' => 'Unauthorized. This endpoint requires VENDOR or ADMIN privileges.',
            ], 403);
        }

        $itemsData = null;

        // Check if there is an uploaded JSON file
        if ($request->hasFile('file')) {
            $file = $request->file('file');
            $contents = file_get_contents($file->getRealPath());
            $itemsData = json_decode($contents, true);
            if (json_last_error() !== JSON_ERROR_NONE) {
                return response()->json([
                    'success' => false,
                    'message' => 'Invalid JSON file structure.',
                ], 400);
            }
        } else {
            // Check direct JSON content input
            $itemsData = $request->isJson() ? $request->json()->all() : $request->all();
            if (isset($itemsData['items'])) {
                $itemsData = $itemsData['items'];
            }
        }

        if (empty($itemsData) || ! is_array($itemsData)) {
            return response()->json([
                'success' => false,
                'message' => 'No item data found. Please upload a valid JSON file or JSON array.',
            ], 400);
        }

        // Normalize if it's a single item instead of array
        if (isset($itemsData['id']) || isset($itemsData['item_id'])) {
            $itemsData = [$itemsData];
        }

        $updatedCount = 0;
        $errors = [];
        $updatesLog = [];

        DB::beginTransaction();
        try {
            foreach ($itemsData as $index => $itemData) {
                $itemId = $itemData['id'] ?? $itemData['item_id'] ?? null;
                if (! $itemId) {
                    $errors[] = "Item at index {$index} missing 'id' or 'item_id'.";

                    continue;
                }

                $food = FoodItem::where('id', $itemId)->first();
                if (! $food) {
                    $errors[] = "Food item with ID {$itemId} not found.";

                    continue;
                }

                if ($food->vendor_id !== $user->id && strtoupper($user->role) !== 'ADMIN') {
                    $errors[] = "Food item width ID {$itemId} does not belong to your vendor profile.";

                    continue;
                }

                $hasChanges = false;
                $changedFields = [];

                if (isset($itemData['price'])) {
                    $oldPrice = $food->price;
                    $newPrice = floatval($itemData['price']);
                    if ($newPrice !== $oldPrice) {
                        $food->price = $newPrice;
                        $hasChanges = true;
                        $changedFields[] = "price from GH₵{$oldPrice} to GH₵{$newPrice}";
                    }
                }

                if (isset($itemData['is_available'])) {
                    $oldAvail = $food->is_available;
                    $newAvail = filter_var($itemData['is_available'], FILTER_VALIDATE_BOOLEAN);
                    if ($newAvail !== $oldAvail) {
                        $food->is_available = $newAvail;
                        $hasChanges = true;
                        $changedFields[] = 'availability from '.($oldAvail ? 'Active' : 'Inactive').' to '.($newAvail ? 'Active' : 'Inactive');
                    }
                }

                if ($hasChanges) {
                    $food->save();
                    $updatedCount++;
                    $updatesLog[] = "{$food->name} (ID {$itemId}) updated: ".implode(', ', $changedFields);
                }
            }

            if ($updatedCount > 0) {
                // Dynamic Audit Log for this bulk update operation
                AuditLog::create([
                    'user_id' => $user->id,
                    'timestamp' => time() * 1000,
                    'action' => 'BULK_MENU_UPLOAD',
                    'details' => "Bulk updated {$updatedCount} menu item(s) properties via JSON upload: ".implode('; ', $updatesLog),
                ]);
            }

            DB::commit();

            return response()->json([
                'success' => true,
                'message' => "Successfully processed JSON upload. Updated {$updatedCount} menu item(s).",
                'updated_count' => $updatedCount,
                'errors' => $errors,
                'updates_log' => $updatesLog,
            ], 200);

        } catch (\Exception $e) {
            DB::rollBack();

            return response()->json([
                'success' => false,
                'message' => 'Server error while performing bulk update from JSON.',
                'error' => $e->getMessage(),
            ], 500);
        }
    }

    /**
     * Get dynamic sales and revenue trends for Chart.js based on date range.
     */
    public function getAnalyticsTrends(Request $request)
    {
        $user = $request->user();
        if (! $user) {
            return response()->json([
                'success' => false,
                'message' => 'Unauthenticated.',
            ], 401);
        }

        $vendorId = $user->id;
        // If Admin, they can view trends of any vendor by passing vendor_id
        if (strtoupper($user->role) === 'ADMIN' && $request->has('vendor_id')) {
            $vendorId = $request->input('vendor_id');
        }

        $startDateStr = $request->input('start_date');
        $endDateStr = $request->input('end_date');

        if (! $startDateStr || ! $endDateStr) {
            // Default to last 30 days
            $startDate = now()->subDays(29)->startOfDay();
            $endDate = now()->endOfDay();
        } else {
            try {
                $startDate = Carbon::parse($startDateStr)->startOfDay();
                $endDate = Carbon::parse($endDateStr)->endOfDay();
            } catch (\Exception $e) {
                return response()->json([
                    'success' => false,
                    'message' => 'Invalid date format. Use Y-m-d.',
                ], 400);
            }
        }

        // Limit range to 365 days
        if ($startDate->diffInDays($endDate) > 365) {
            return response()->json([
                'success' => false,
                'message' => 'Date range cannot exceed 365 days.',
            ], 400);
        }

        // Initialize array for every date in range
        $trends = [];
        $currentDate = clone $startDate;
        while ($currentDate->lte($endDate)) {
            $dateStr = $currentDate->format('Y-m-d');
            $dayLabel = $currentDate->format('M d'); // e.g. Jul 15
            $trends[$dateStr] = [
                'date' => $dateStr,
                'label' => $dayLabel,
                'sales' => 0.0,
                'order_count' => 0,
            ];
            $currentDate->addDay();
        }

        // Query sales from DB
        $orders = Order::where('vendor_id', $vendorId)
            ->whereIn('status', ['COMPLETED', 'DELIVERED', 'READY'])
            ->whereBetween('created_at', [$startDate, $endDate])
            ->select(
                DB::raw('DATE(created_at) as date_val'),
                DB::raw('SUM(total_price) as total_sales'),
                DB::raw('COUNT(*) as total_orders')
            )
            ->groupBy('date_val')
            ->get();

        foreach ($orders as $orderSales) {
            $dateKey = $orderSales->date_val;
            if (isset($trends[$dateKey])) {
                $trends[$dateKey]['sales'] = (float) $orderSales->total_sales;
                $trends[$dateKey]['order_count'] = (int) $orderSales->total_orders;
            }
        }

        $trendsList = array_values($trends);
        $labels = array_map(function ($item) {
            return $item['label'];
        }, $trendsList);
        $sales = array_map(function ($item) {
            return $item['sales'];
        }, $trendsList);
        $orderCounts = array_map(function ($item) {
            return $item['order_count'];
        }, $trendsList);

        return response()->json([
            'success' => true,
            'start_date' => $startDate->format('Y-m-d'),
            'end_date' => $endDate->format('Y-m-d'),
            'labels' => $labels,
            'sales' => $sales,
            'order_counts' => $orderCounts,
            'raw_trends' => $trendsList,
        ], 200);
    }
}
