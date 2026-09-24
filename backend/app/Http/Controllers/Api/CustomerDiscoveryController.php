<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\DeliveredOrderReview;
use App\Models\FoodItem;
use App\Models\Promotion;
use App\Models\Review;
use App\Models\User;
use Illuminate\Http\Request;

class CustomerDiscoveryController extends Controller
{
    public function index(Request $request)
    {
        $vendors = User::where('role', 'VENDOR')
            ->where('account_status', 'ACTIVE')
            ->orderByDesc('is_open')
            ->orderBy('fullName')
            ->limit(20)
            ->get();

        $vendorData = $vendors->map(function (User $vendor) {
            $delivered = DeliveredOrderReview::where('vendor_id', $vendor->id)
                ->whereNotNull('vendor_rating')->pluck('vendor_rating')->all();

            $legacy = Review::where('vendor_id', $vendor->id)
                ->whereNotNull('rating')->pluck('rating')->all();

            $ratings = array_merge($delivered, $legacy);
            $rating = count($ratings) ? round(array_sum($ratings) / count($ratings), 1) : null;

            $menu = FoodItem::where('vendor_id', $vendor->id)
                ->where('is_available', true)
                ->orderByDesc('is_featured')
                ->orderByDesc('id')
                ->limit(1)
                ->first();

            return [
                'id' => $vendor->id,
                'name' => (string) $vendor->fullName,
                'category' => trim((string) $vendor->info),
                'is_open' => (bool) $vendor->is_open,
                'rating' => $rating,
                'reviews_count' => count($ratings),
                'featured_image' => $menu?->image_url,
                'available_items' => FoodItem::where('vendor_id', $vendor->id)
                    ->where('is_available', true)->count(),
            ];
        })->values();

        $promotions = Promotion::where('is_active', true)
            ->where(function ($q) {
                $q->whereNull('starts_at')->orWhere('starts_at', '<=', now());
            })
            ->where(function ($q) {
                $q->whereNull('ends_at')->orWhere('ends_at', '>=', now());
            })
            ->orderByDesc('id')
            ->limit(6)
            ->get([
                'id','code','name','type','value','minimum_order_amount',
                'maximum_discount_amount','starts_at','ends_at',
            ]);

        $featuredItems = FoodItem::where('is_available', true)
            ->orderByDesc('is_featured')
            ->orderByDesc('id')
            ->limit(12)
            ->get();

        return response()->json([
            'success' => true,
            'data' => [
                'vendors' => $vendorData,
                'featured_items' => $featuredItems,
                'promotions' => $promotions,
                'generated_at' => now()->toIso8601String(),
            ],
        ]);
    }
}
