<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\AuditLog;
use App\Models\MenuItem;
use App\Models\Order;
use App\Models\Vendor;
use App\Models\WalletTransaction;
use App\Services\ReceiptPdfWriter;
use App\Support\MenuBadges;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Http\Response;

/**
 * Stand-out features: public stall directory + "what's open now" board,
 * stall QR/deep-link menus, scheduled pre-ordering and digital receipts.
 */
class StandoutController extends Controller
{
    /**
     * Public stall directory (used by the "what's open now" board and
     * marketing pages). Optional ?campus= filter.
     */
    public function publicStalls(Request $request): JsonResponse
    {
        $query = Vendor::query()->with('user:id,username,fullName,is_open');

        if ($request->filled('campus')) {
            $query->where('campus', $request->string('campus'));
        }

        $data = $query->get()->map(function (Vendor $vendor) {
            $menuCount = MenuItem::where('vendor_id', $vendor->user_id)
                ->where('is_available', true)
                ->count();

            return [
                'id' => $vendor->user_id,
                'store_name' => $vendor->store_name ?? $vendor->name,
                'is_open' => (bool) ($vendor->user?->is_open ?? false),
                'campus' => $vendor->campus,
                'open_menu_items' => $menuCount,
            ];
        })->values();

        return response()->json(['success' => true, 'data' => $data], 200);
    }

    /**
     * A single stall with its live menu — the target of stall QR codes /
     * deep links so scanning (or typing a stall code) lands straight on the
     * vendor's offerings.
     */
    public function stall(Request $request, $id): JsonResponse
    {
        $vendor = Vendor::with('user:id,username,fullName,is_open')
            ->where('user_id', (int) $id)
            ->first();

        if (! $vendor) {
            return response()->json(['success' => false, 'message' => 'Stall not found.'], 404);
        }

        $menu = MenuItem::where('vendor_id', (int) $id)
            ->where('is_available', true)
            ->orderBy('name')
            ->get()
            ->map(fn (MenuItem $item) => $this->menuItemPayload($item))
            ->values();

        return response()->json([
            'success' => true,
            'data' => [
                'stall' => [
                    'id' => $vendor->user_id,
                    'name' => $vendor->store_name ?? $vendor->name,
                    'is_open' => (bool) ($vendor->user?->is_open ?? false),
                    'campus' => $vendor->campus,
                ],
                'menu_items' => $menu,
            ],
        ], 200);
    }

    /**
     * Schedule (or reschedule) a pre-order's pickup time.
     */
    public function schedulePickup(Request $request, $id): JsonResponse
    {
        $user = $request->user();
        $order = Order::find($id);

        if (! $order) {
            return response()->json(['success' => false, 'message' => 'Order not found.'], 404);
        }

        $ownerId = (int) ($order->customer_id ?? $order->student_id ?? $order->vendor_id);
        $role = strtoupper((string) $user->role);
        if ($user->id !== $ownerId && $role !== 'ADMIN') {
            return response()->json(['success' => false, 'message' => 'You cannot schedule this order.'], 403);
        }

        $request->validate(['pickup_at' => 'required|date|after_or_equal:now']);

        $order->scheduled_pickup_at = now()->parse($request->input('pickup_at'));
        $order->save();

        AuditLog::create([
            'user_id' => $user->id,
            'timestamp' => time() * 1000,
            'action' => 'ORDER_SCHEDULED',
            'details' => "Pickup for order #{$order->id} scheduled for {$order->scheduled_pickup_at}.",
        ]);

        return response()->json(['success' => true, 'order' => $order->fresh()], 200);
    }

    /**
     * Digital receipt (JSON) for an order: line data, vendor and wallet ledger.
     */
    public function receipt(Request $request, $id): JsonResponse
    {
        $order = $this->authorizedOrder($request, $id);
        if ($order instanceof Response) {
            return response()->json(['success' => false, 'message' => $order->getContent()], $order->getStatusCode());
        }

        $transactions = WalletTransaction::where('order_id', $order->id)->get();

        return response()->json([
            'success' => true,
            'data' => [
                'order' => $order,
                'vendor_name' => $order->vendor?->fullName,
                'transactions' => $transactions,
                'issued_at' => now()->toIso8601String(),
            ],
        ], 200);
    }

    /**
     * Printable receipt as PDF (A4, offline-safe, no external services).
     */
    public function pdf(Request $request, $id): Response
    {
        $order = $this->authorizedOrder($request, $id);
        if ($order instanceof Response) {
            return $order;
        }

        $writer = new ReceiptPdfWriter;
        $pdf = $writer->generate($order);

        return response($pdf, 200, [
            'Content-Type' => 'application/pdf',
            'Content-Disposition' => "inline; filename=\"atu-receipt-{$order->id}.pdf\"",
        ]);
    }

    private function menuItemPayload(MenuItem $item): array
    {
        return [
            'id' => $item->id,
            'vendor_id' => $item->vendor_id,
            'name' => $item->name ?? $item->food_name,
            'price' => (float) $item->price,
            'category' => $item->category,
            'is_available' => (bool) $item->is_available,
            'dietary_tags' => $item->dietary_tags ?? [],
            'allergen_info' => $item->allergen_info,
            'badges' => MenuBadges::for($item),
        ];
    }

    /**
     * Load an order authorized for the caller (owner student, the vendor it
     * belongs to, or an admin). Returns the Order, or a JSON Response error.
     */
    private function authorizedOrder(Request $request, $id): Order|Response
    {
        $user = $request->user();
        $order = Order::with('vendor')->find($id);

        if (! $order) {
            return response('Order not found.', 404);
        }

        $role = strtoupper((string) $user->role);
        $isOwner = in_array($user->id, [
            (int) ($order->customer_id ?? 0),
            (int) ($order->student_id ?? 0),
            (int) ($order->user_id ?? 0),
        ], true);
        $isTheirOrder = ($role === 'VENDOR' && (int) $order->vendor_id === (int) $user->id);

        if (! $isOwner && ! $isTheirOrder && $role !== 'ADMIN') {
            return response('You are not authorized to view this order.', 403);
        }

        return $order;
    }
}
