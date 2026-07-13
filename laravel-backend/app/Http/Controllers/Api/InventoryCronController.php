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
                            $notif = new LowStockAlertNotification(
                                $item->name,
                                $item->id,
                                $remainingStock,
                                $orderFrequency24h,
                                'food_items'
                            );
                            $vendor->notify($notif);

                            // Notify cafeteria administrators via mail & push channels
                            $admins = User::where('role', 'ADMIN')->orWhere('role', 'admin')->get();
                            foreach ($admins as $admin) {
                                $admin->notify($notif);
                            }

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
                                $notif = new LowStockAlertNotification(
                                    $item->name,
                                    $item->id,
                                    $remainingStock,
                                    $orderFrequency24h,
                                    'vendor_menu_items'
                                );
                                $vendor->notify($notif);

                                // Notify cafeteria administrators via mail & push channels
                                $admins = User::where('role', 'ADMIN')->orWhere('role', 'admin')->get();
                                foreach ($admins as $admin) {
                                    $admin->notify($notif);
                                }

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

                // 3. Audit standard menu_items (VendorMenuItem CRUD list)
                if (Schema::hasTable('menu_items')) {
                    $menuItems = \App\Models\MenuItem::where('vendor_id', $vendor->id)
                        ->where('is_available', true)
                        ->get();

                    foreach ($menuItems as $item) {
                        $checkedCount++;

                        // Calculate recent 24-hour order frequencies
                        $timestamp24hAgo = (time() - 24 * 60 * 60) * 1000;
                        $orderFrequency24h = (int) Order::where('menu_item_id', $item->id)
                            ->whereNotIn(DB::raw('UPPER(status)'), ['CANCELLED', 'DECLINED'])
                            ->where('order_timestamp', '>=', $timestamp24hAgo)
                            ->sum('quantity');

                        // Use the database tracked current_stock
                        $remainingStock = $item->current_stock !== null ? $item->current_stock : 50;
                        $lowStockThreshold = $item->low_stock_threshold !== null ? $item->low_stock_threshold : 10;

                        if ($remainingStock <= $lowStockThreshold) {
                            $alreadyAlerted = DB::table('notifications')
                                ->where('notifiable_id', $vendor->id)
                                ->where('type', 'App\Notifications\LowStockAlertNotification')
                                ->where('created_at', '>=', now()->subHours(12))
                                ->get()
                                ->contains(function ($notif) use ($item) {
                                    $data = json_decode($notif->data, true);
                                    return isset($data['item_id']) && $data['item_id'] == $item->id && isset($data['type']) && $data['type'] == 'menu_items';
                                });

                            if (!$alreadyAlerted) {
                                $notif = new LowStockAlertNotification(
                                    $item->food_name ?: $item->name,
                                    $item->id,
                                    $remainingStock,
                                    $orderFrequency24h,
                                    'menu_items'
                                );
                                $vendor->notify($notif);

                                // Notify cafeteria administrators via mail & push channels
                                $admins = User::where('role', 'ADMIN')->orWhere('role', 'admin')->get();
                                foreach ($admins as $admin) {
                                    $admin->notify($notif);
                                }

                                $alertsTriggered[] = [
                                    'item_id' => $item->id,
                                    'item_name' => $item->food_name ?: $item->name,
                                    'vendor_id' => $vendor->id,
                                    'vendor_name' => $vendor->fullName,
                                    'remaining_stock' => $remainingStock,
                                    'frequency_24h' => $orderFrequency24h,
                                    'threshold_limit' => $lowStockThreshold,
                                    'type' => 'menu_items'
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

    /**
     * Create an API trigger that notifies a vendor when a specific menu item's inventory drops below a defined threshold.
     */
    public function checkItemStock(Request $request)
    {
        try {
            $itemId = $request->input('item_id');
            $itemType = $request->input('item_type', 'food_items'); // food_items or vendor_menu_items
            $definedThreshold = $request->input('threshold'); // optional custom threshold

            if (!$itemId) {
                return response()->json([
                    'success' => false,
                    'message' => 'Please provide a valid item_id parameter.'
                ], 400);
            }

            $itemName = '';
            $remainingStock = 0;
            $lowStockThreshold = 10; // Default fallback
            $vendorId = null;
            $orderFrequency24h = 0;

            if ($itemType === 'food_items') {
                $item = FoodItem::find($itemId);
                if (!$item) {
                    return response()->json([
                        'success' => false,
                        'message' => 'Specific food item not found.'
                    ], 404);
                }

                $itemName = $item->name;
                $vendorId = $item->vendor_id;

                // Calculate recent 24-hour order frequencies
                $timestamp24hAgo = (time() - 24 * 60 * 60) * 1000;
                $orderFrequency24h = (int) Order::where('food_item_id', $item->id)
                    ->whereNotIn(DB::raw('UPPER(status)'), ['CANCELLED', 'DECLINED'])
                    ->where('order_timestamp', '>=', $timestamp24hAgo)
                    ->sum('quantity');

                // Today's cycle total sum
                $totalTodaySum = (int) Order::where('food_item_id', $item->id)
                    ->whereNotIn(DB::raw('UPPER(status)'), ['CANCELLED', 'DECLINED'])
                    ->where('order_timestamp', '>=', (time() - 12 * 60 * 60) * 1000)
                    ->sum('quantity');

                $startingLimit = $item->initial_stock ?? 35;
                $remainingStock = max(0, $startingLimit - $totalTodaySum);
                
                // If custom threshold is defined, use it. Otherwise use model threshold or dynamic threshold
                if (is_numeric($definedThreshold)) {
                    $lowStockThreshold = (int) $definedThreshold;
                } else {
                    $lowStockThreshold = $item->low_stock_threshold ?? max(4, (int) round($orderFrequency24h * 0.40));
                }

            } elseif ($itemType === 'vendor_menu_items' && Schema::hasTable('vendor_menu_items')) {
                $item = DB::table('vendor_menu_items')
                    ->join('vendor_menus', 'vendor_menu_items.menu_id', '=', 'vendor_menus.id')
                    ->where('vendor_menu_items.id', $itemId)
                    ->select('vendor_menu_items.*', 'vendor_menus.vendor_id')
                    ->first();

                if (!$item) {
                    return response()->json([
                        'success' => false,
                        'message' => 'Specific vendor menu item not found.'
                    ], 404);
                }

                $itemName = $item->name;
                $vendorId = $item->vendor_id;
                $remainingStock = (int) $item->current_stock_count;

                // Calculate recent 24-hour order volume
                $timestamp24hAgo = (time() - 24 * 60 * 60) * 1000;
                $orderFrequency24h = (int) Order::where('menu_item_id', $item->id)
                    ->whereNotIn(DB::raw('UPPER(status)'), ['CANCELLED', 'DECLINED'])
                    ->where('order_timestamp', '>=', $timestamp24hAgo)
                    ->sum('quantity');

                if (is_numeric($definedThreshold)) {
                    $lowStockThreshold = (int) $definedThreshold;
                } else {
                    $lowStockThreshold = max(5, (int) round($orderFrequency24h * 0.45));
                }
            } elseif ($itemType === 'menu_items') {
                $item = \App\Models\MenuItem::find($itemId);
                if (!$item) {
                    return response()->json([
                        'success' => false,
                        'message' => 'Specific menu item not found.'
                    ], 404);
                }

                $itemName = $item->food_name ?: $item->name;
                $vendorId = $item->vendor_id;
                $remainingStock = $item->current_stock !== null ? $item->current_stock : 50;

                // Calculate recent 24-hour order volume
                $timestamp24hAgo = (time() - 24 * 60 * 60) * 1000;
                $orderFrequency24h = (int) Order::where('menu_item_id', $item->id)
                    ->whereNotIn(DB::raw('UPPER(status)'), ['CANCELLED', 'DECLINED'])
                    ->where('order_timestamp', '>=', $timestamp24hAgo)
                    ->sum('quantity');

                if (is_numeric($definedThreshold)) {
                    $lowStockThreshold = (int) $definedThreshold;
                } else {
                    $lowStockThreshold = $item->low_stock_threshold !== null ? $item->low_stock_threshold : 10;
                }
            } else {
                return response()->json([
                    'success' => false,
                    'message' => 'Unsupported item_type or table not found. Supported: food_items, vendor_menu_items, menu_items.'
                ], 400);
            }

            $vendor = User::find($vendorId);
            if (!$vendor) {
                return response()->json([
                    'success' => false,
                    'message' => 'Vendor associated with this item does not exist.'
                ], 404);
            }

            $alertFired = false;
            $message = "Current stock ($remainingStock) is above the low-stock threshold ($lowStockThreshold). No notification sent.";

            if ($remainingStock <= $lowStockThreshold) {
                // Fire notification
                $notif = new LowStockAlertNotification(
                    $itemName,
                    $itemId,
                    $remainingStock,
                    $orderFrequency24h,
                    $itemType
                );
                $vendor->notify($notif);

                // Notify admins too
                $admins = User::where('role', 'ADMIN')->orWhere('role', 'admin')->get();
                foreach ($admins as $admin) {
                    $admin->notify($notif);
                }

                $alertFired = true;
                $message = "INVENTORY ALERT: '{$itemName}' dropped below threshold. Vendor '{$vendor->fullName}' has been notified successfully.";
            }

            return response()->json([
                'success' => true,
                'alert_fired' => $alertFired,
                'message' => $message,
                'details' => [
                    'item_id' => (int)$itemId,
                    'item_name' => $itemName,
                    'item_type' => $itemType,
                    'remaining_stock' => $remainingStock,
                    'low_stock_threshold' => $lowStockThreshold,
                    'order_frequency_24h' => $orderFrequency24h,
                    'vendor_id' => $vendor->id,
                    'vendor_name' => $vendor->fullName
                ]
            ], 200);

        } catch (\Exception $e) {
            return response()->json([
                'success' => false,
                'message' => 'Failed to perform inventory check.',
                'error' => $e->getMessage()
            ], 500);
        }
    }
}
