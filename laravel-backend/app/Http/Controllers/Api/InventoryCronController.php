<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\User;
use App\Models\FoodItem;
use App\Models\Order;
use App\Notifications\LowStockAlertNotification;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;

class InventoryCronController extends Controller
{
    /**
     * Run the automated inventory audit and notify vendors of low stock items.
     * Based on actual high-velocity order frequencies.
     */
    public function checkAndNotify(Request $request)
    {
        try {
            $checkedCount = 0;
            $alertsTriggered = [];
            $vendorsAnalyzed = 0;

            // Fetch all active vendors
            $vendors = User::where('role', 'VENDOR')->get();

            foreach ($vendors as $vendor) {
                $vendorsAnalyzed++;
                
                // 1. Audit core FoodItem table (standard menu items)
                $foodItems = FoodItem::where('vendor_id', $vendor->id)
                    ->where('is_available', true)
                    ->get();

                foreach ($foodItems as $item) {
                    $checkedCount++;

                    // Calculate recent 24-hour order frequencies (using precision milli-timestamp)
                    $timestamp24hAgo = (time() - 24 * 60 * 60) * 1000;
                    $orderFrequency24h = (int) Order::where('food_item_id', $item->id)
                        ->whereNotIn(DB::raw('UPPER(status)'), ['CANCELLED', 'DECLINED'])
                        ->where('order_timestamp', '>=', $timestamp24hAgo)
                        ->sum('quantity');

                    // Simulate realistic dynamic standard stock limits (starting default 35 portions per item per day)
                    $totalTodaySum = (int) Order::where('food_item_id', $item->id)
                        ->whereNotIn(DB::raw('UPPER(status)'), ['CANCELLED', 'DECLINED'])
                        ->where('order_timestamp', '>=', (time() - 12 * 60 * 60) * 1000) // Today's cycle
                        ->sum('quantity');
                    
                    $startingLimit = 35; // Standard cafeteria batch size
                    $remainingStock = max(0, $startingLimit - $totalTodaySum);

                    // Dynamic threshold algorithm: High velocity items warrant a larger safety stock margin
                    // If an item sells 15 portions a day, threshold warning should trigger earlier!
                    $lowStockThreshold = max(4, (int) round($orderFrequency24h * 0.40));

                    if ($remainingStock <= $lowStockThreshold) {
                        // Prevent identical spam notifications (check last 12 hours)
                        $alreadyAlerted = DB::table('notifications')
                            ->where('notifiable_id', $vendor->id)
                            ->where('type', 'App\Notifications\LowStockAlertNotification')
                            ->where('created_at', '>=', now()->subHours(12))
                            ->get()
                            ->contains(function ($notif) use ($item) {
                                $data = json_decode($notif->data, true);
                                return isset($data['item_id']) && $data['item_id'] == $item->id;
                            });

                        if (!$alreadyAlerted) {
                            $vendor->notify(new LowStockAlertNotification(
                                $item->name,
                                $item->id,
                                $remainingStock,
                                $orderFrequency24h,
                                'food_items'
                            ));

                            $alertsTriggered[] = [
                                'item_id' => $item->id,
                                'item_name' => $item->name,
                                'vendor_id' => $vendor->id,
                                'vendor_name' => $vendor->fullName,
                                'remaining_stock' => $remainingStock,
                                'frequency_24h' => $orderFrequency24h,
                                'threshold_limit' => $lowStockThreshold,
                                'type' => 'food_items'
                            ];
                        }
                    }
                }

                // 2. Audit detailed extended vendor_menu_items from modern comprehensive table if they exist
                if (Schema::hasTable('vendor_menu_items')) {
                    $comprehensiveItems = DB::table('vendor_menu_items')
                        ->join('vendor_menus', 'vendor_menu_items.menu_id', '=', 'vendor_menus.id')
                        ->where('vendor_menus.vendor_id', $vendor->id)
                        ->where('vendor_menu_items.is_available', true)
                        ->select('vendor_menu_items.*', 'vendor_menus.vendor_id')
                        ->get();

                    foreach ($comprehensiveItems as $item) {
                        $checkedCount++;

                        // Calculate recent 24-hour order volume
                        $timestamp24hAgo = (time() - 24 * 60 * 60) * 1000;
                        $orderFrequency24h = (int) Order::where('menu_item_id', $item->id)
                            ->whereNotIn(DB::raw('UPPER(status)'), ['CANCELLED', 'DECLINED'])
                            ->where('order_timestamp', '>=', $timestamp24hAgo)
                            ->sum('quantity');

                        // Use actual database tracked current stock
                        $remainingStock = $item->current_stock_count;

                        // Dynamic threshold logic
                        $lowStockThreshold = max(5, (int) round($orderFrequency24h * 0.45));

                        if ($remainingStock <= $lowStockThreshold) {
                            $alreadyAlerted = DB::table('notifications')
                                ->where('notifiable_id', $vendor->id)
                                ->where('type', 'App\Notifications\LowStockAlertNotification')
                                ->where('created_at', '>=', now()->subHours(12))
                                ->get()
                                ->contains(function ($notif) use ($item) {
                                    $data = json_decode($notif->data, true);
                                    return isset($data['item_id']) && $data['item_id'] == $item->id;
                                });

                            if (!$alreadyAlerted) {
                                $vendor->notify(new LowStockAlertNotification(
                                    $item->name,
                                    $item->id,
                                    $remainingStock,
                                    $orderFrequency24h,
                                    'vendor_menu_items'
                                ));

                                $alertsTriggered[] = [
                                    'item_id' => $item->id,
                                    'item_name' => $item->name,
                                    'vendor_id' => $vendor->id,
                                    'vendor_name' => $vendor->fullName,
                                    'remaining_stock' => $remainingStock,
                                    'frequency_24h' => $orderFrequency24h,
                                    'threshold_limit' => $lowStockThreshold,
                                    'type' => 'vendor_menu_items'
                                ];
                            }
                        }
                    }
                }
            }

            return response()->json([
                'success' => true,
                'message' => 'Automated vendor inventory and availability checks performed successfully.',
                'timestamp' => date('c'),
                'metrics' => [
                    'vendors_analyzed' => $vendorsAnalyzed,
                    'items_checked' => $checkedCount,
                    'new_low_stock_alerts_fired' => count($alertsTriggered)
                ],
                'fired_alerts' => $alertsTriggered
            ], 200);

        } catch (\Exception $e) {
            return response()->json([
                'success' => false,
                'message' => 'Cron execution failed with errors.',
                'error' => $e->getMessage(),
                'trace' => $e->getTraceAsString()
            ], 500);
        }
    }
}
