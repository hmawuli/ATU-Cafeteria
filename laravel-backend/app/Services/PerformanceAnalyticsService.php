<?php

namespace App\Services;

use App\Models\User;
use App\Models\Order;
use App\Models\Feedback;
use App\Models\AuditLog;
use Illuminate\Support\Facades\DB;

class PerformanceAnalyticsService
{
    /**
     * Generate a robust performance analytics report for a single vendor.
     *
     * @param int $vendorId
     * @return array
     */
    public function getVendorReport(int $vendorId): array
    {
        $vendor = User::find($vendorId);
        if (!$vendor) {
            return [
                'success' => false,
                'message' => 'Vendor not found.'
            ];
        }

        // 1. Core Orders Analytics
        $ordersQuery = Order::where('vendor_id', $vendorId);
        $totalOrders = (clone $ordersQuery)->count();
        $statusCounts = (clone $ordersQuery)->select('status', DB::raw('count(*) as total'))
            ->groupBy('status')
            ->pluck('total', 'status')
            ->toArray();

        // Standardize all statuses
        $statuses = ['PENDING', 'PREPARING', 'READY', 'COMPLETED', 'DECLINED'];
        $orderBreakdown = [];
        foreach ($statuses as $stat) {
            $orderBreakdown[$stat] = $statusCounts[$stat] ?? 0;
        }

        $completedOrdersCount = $orderBreakdown['COMPLETED'];
        $declinedOrdersCount = $orderBreakdown['DECLINED'];
        
        // Calculate completion rate (completed / (total - declined))
        $denominator = $totalOrders - $declinedOrdersCount;
        $completionRate = $denominator > 0 ? round(($completedOrdersCount / $denominator) * 100, 1) : 0.0;

        // Cumulative sales/revenue (Sum of COMPLETED orders)
        $totalRevenue = (clone $ordersQuery)->where('status', 'COMPLETED')->sum('total_price');

        // 2. Order Completion Durations
        $completionTimes = [];
        $completedOrders = (clone $ordersQuery)->where('status', 'COMPLETED')->get();

        foreach ($completedOrders as $order) {
            $startMs = $order->order_timestamp;
            $endMs = null;

            // Attempt to fetch precise timestamp from PICKUP_VALIDATED audit log
            $log = AuditLog::where('user_id', $vendorId)
                ->where('action', 'PICKUP_VALIDATED')
                ->where('details', 'like', "%Order #{$order->id} %")
                ->first();

            if ($log) {
                $endMs = $log->timestamp;
            } else {
                // Fallback to ORDER_STATUS_CHANGED to COMPLETED
                $statusLog = AuditLog::where('user_id', $vendorId)
                    ->where('action', 'ORDER_STATUS_CHANGED')
                    ->where('details', 'like', "Order #{$order->id} status moved%to 'COMPLETED'%")
                    ->first();
                if ($statusLog) {
                    $endMs = $statusLog->timestamp;
                } else {
                    // Worst case fallback: database updated_at timestamp in milliseconds
                    $endMs = $order->updated_at ? ($order->updated_at->timestamp * 1000) : null;
                }
            }

            if ($endMs && $endMs >= $startMs) {
                // Duration in seconds
                $durationSeconds = ($endMs - $startMs) / 1000;
                $completionTimes[] = $durationSeconds;
            }
        }

        $averageCompletionSeconds = 0;
        $fastestCompletionSeconds = null;
        $slowestCompletionSeconds = null;

        if (count($completionTimes) > 0) {
            $averageCompletionSeconds = array_sum($completionTimes) / count($completionTimes);
            $fastestCompletionSeconds = min($completionTimes);
            $slowestCompletionSeconds = max($completionTimes);
        }

        // 3. User Ratings/Feedback Analytics
        $feedbackQuery = Feedback::where('vendor_id', $vendorId);
        $totalFeedbacks = (clone $feedbackQuery)->count();

        $avgFoodQuality = round((clone $feedbackQuery)->avg('rating_food_quality') ?? 0, 2);
        $avgCleanliness = round((clone $feedbackQuery)->avg('rating_cleanliness') ?? 0, 2);
        $avgServiceSpeed = round((clone $feedbackQuery)->avg('rating_service_speed') ?? 0, 2);
        $avgPriceValue = round((clone $feedbackQuery)->avg('rating_price_value') ?? 0, 2);

        // Overall cumulative rating
        $overallAverage = 0.0;
        if ($totalFeedbacks > 0) {
            $overallAverage = round(($avgFoodQuality + $avgCleanliness + $avgServiceSpeed + $avgPriceValue) / 4, 2);
        }

        // Retrieve last 10 comments
        $recentComments = (clone $feedbackQuery)
            ->with(['customer:id,fullName'])
            ->orderBy('id', 'desc')
            ->limit(10)
            ->get()
            ->map(function ($f) {
                return [
                    'id' => $f->id,
                    'customer_name' => $f->customer ? $f->customer->fullName : 'Anonymous Student',
                    'ratings' => [
                        'food_quality' => $f->rating_food_quality,
                        'cleanliness' => $f->rating_cleanliness,
                        'service_speed' => $f->rating_service_speed,
                        'price_value' => $f->rating_price_value,
                        'average' => round(($f->rating_food_quality + $f->rating_cleanliness + $f->rating_service_speed + $f->rating_price_value) / 4, 1),
                    ],
                    'comment' => $f->comment,
                    'submitted_at' => $f->timestamp ? date('Y-m-d H:i:s', floatval($f->timestamp) / 1000) : $f->created_at->toDateTimeString(),
                ];
            })
            ->toArray();

        return [
            'success' => true,
            'vendor' => [
                'id' => $vendor->id,
                'name' => $vendor->fullName,
                'username' => $vendor->username,
                'info' => $vendor->info,
            ],
            'order_metrics' => [
                'total_orders_placed' => $totalOrders,
                'status_breakdown' => $orderBreakdown,
                'completion_rate_percentage' => $completionRate,
                'total_completed_revenue' => round(floatval($totalRevenue), 2),
            ],
            'completion_time_metrics' => [
                'average_seconds' => round($averageCompletionSeconds, 1),
                'average_formatted' => $this->formatDuration($averageCompletionSeconds),
                'fastest_seconds' => $fastestCompletionSeconds !== null ? round($fastestCompletionSeconds, 1) : null,
                'fastest_formatted' => $fastestCompletionSeconds !== null ? $this->formatDuration($fastestCompletionSeconds) : 'N/A',
                'slowest_seconds' => $slowestCompletionSeconds !== null ? round($slowestCompletionSeconds, 1) : null,
                'slowest_formatted' => $slowestCompletionSeconds !== null ? $this->formatDuration($slowestCompletionSeconds) : 'N/A',
            ],
            'rating_metrics' => [
                'total_feedback_count' => $totalFeedbacks,
                'average_food_quality' => $avgFoodQuality,
                'average_cleanliness' => $avgCleanliness,
                'average_service_speed' => $avgServiceSpeed,
                'average_price_value' => $avgPriceValue,
                'overall_average_rating' => $overallAverage,
            ],
            'recent_customer_feedback' => $recentComments,
            'generated_at' => date('Y-m-d H:i:s'),
        ];
    }

    /**
     * Helper to formatted duration string from total seconds.
     *
     * @param float $seconds
     * @return string
     */
    private function formatDuration(float $seconds): string
    {
        if ($seconds <= 0) {
            return '0s';
        }
        $mins = floor($seconds / 60);
        $secs = round($seconds % 60);

        if ($mins > 0) {
            return "{$mins}m {$secs}s";
        }
        return "{$secs}s";
    }

    /**
     * Generate comparative summary report comparing all active vendors in the cafeteria ecosystem.
     * Perfect for ADMIN overviews or user comparative insights.
     */
    public function getComparativeVendorsReport(): array
    {
        $vendors = User::where('role', 'VENDOR')->get();
        $comparison = [];

        foreach ($vendors as $vendor) {
            $report = $this->getVendorReport($vendor->id);
            if ($report['success']) {
                $comparison[] = [
                    'vendor_id' => $vendor->id,
                    'vendor_name' => $vendor->fullName,
                    'info' => $vendor->info,
                    'total_orders' => $report['order_metrics']['total_orders_placed'],
                    'completion_rate_percentage' => $report['order_metrics']['completion_rate_percentage'],
                    'revenue' => $report['order_metrics']['total_completed_revenue'],
                    'overall_rating' => $report['rating_metrics']['overall_average_rating'],
                    'avg_completion_time' => $report['completion_time_metrics']['average_formatted'],
                    'avg_completion_seconds' => $report['completion_time_metrics']['average_seconds'],
                ];
            }
        }

        // Sort by overall rating desc by default
        usort($comparison, function ($a, $b) {
            return $b['overall_rating'] <=> $a['overall_rating'];
        });

        return [
            'success' => true,
            'total_vendors' => count($comparison),
            'rankings' => $comparison,
            'generated_at' => date('Y-m-d H:i:s'),
        ];
    }
}
