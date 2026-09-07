<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\AuditLog;
use App\Models\FoodItem;
use App\Models\Order;
use App\Models\User;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Hash;
use Illuminate\Support\Facades\Validator;

class AdminController extends Controller
{
    /**
     * The route is protected by auth:sanctum + admin middleware.
     * This second role check keeps the controller safe if reused elsewhere.
     */
    private function admin(Request $request): ?User
    {
        $user = $request->user();
        return $user && strtoupper((string) $user->role) === 'ADMIN' ? $user : null;
    }

    private function deny(): \Illuminate\Http\JsonResponse
    {
        return response()->json([
            'success' => false,
            'message' => 'Forbidden. Administrator privileges are required.',
        ], 403);
    }

    public function overview(Request $request)
    {
        $admin = $this->admin($request);
        if (!$admin) return $this->deny();

        $today = now()->startOfDay();

        return response()->json([
            'success' => true,
            'statistics' => [
                'users' => User::count(),
                'students' => User::where('role', 'STUDENT')->count(),
                'vendors' => User::where('role', 'VENDOR')->count(),
                'admins' => User::where('role', 'ADMIN')->count(),
                'food_items' => FoodItem::count(),
                'orders' => Order::count(),
                'orders_today' => Order::where('created_at', '>=', $today)->count(),
                'completed_orders' => Order::whereIn('status', ['COMPLETED', 'DELIVERED'])->count(),
                'revenue' => (float) Order::whereIn('status', ['COMPLETED', 'DELIVERED'])->sum('total_price'),
                'revenue_today' => (float) Order::whereIn('status', ['COMPLETED', 'DELIVERED'])->where('created_at', '>=', $today)->sum('total_price'),
            ],
            'vendors' => User::where('role', 'VENDOR')->orderBy('id')->get(),
            'recent_orders' => Order::with(['user', 'vendor'])->latest('id')->limit(25)->get(),
            'recent_activity' => AuditLog::latest('timestamp')->limit(50)->get(),
        ]);
    }

    public function createVendor(Request $request)
    {
        $admin = $this->admin($request);
        if (!$admin) return $this->deny();

        $validator = Validator::make($request->all(), [
            'username' => 'required|string|max:255|unique:users,username',
            'pin' => 'required|string|min:4|max:128',
            'fullName' => 'required|string|max:255',
            'info' => 'nullable|string|max:1000',
        ]);
        if ($validator->fails()) {
            return response()->json(['success' => false, 'errors' => $validator->errors()], 422);
        }

        $vendor = DB::transaction(function () use ($request, $admin) {
            $vendor = User::create([
                'username' => $request->input('username'),
                'password' => Hash::make($request->input('pin')),
                'role' => 'VENDOR',
                'fullName' => $request->input('fullName'),
                'info' => $request->input('info', ''),
            ]);

            AuditLog::create([
                'user_id' => $admin->id,
                'timestamp' => time() * 1000,
                'action' => 'ADMIN_VENDOR_CREATED',
                'details' => "Administrator created vendor '{$vendor->fullName}' (ID {$vendor->id}).",
            ]);

            return $vendor;
        });

        return response()->json(['success' => true, 'message' => 'Vendor registered successfully.', 'vendor' => $vendor], 201);
    }

    public function updateVendor(Request $request, int $vendorId)
    {
        $admin = $this->admin($request);
        if (!$admin) return $this->deny();

        $vendor = User::where('role', 'VENDOR')->find($vendorId);
        if (!$vendor) return response()->json(['success' => false, 'message' => 'Vendor not found.'], 404);

        $validator = Validator::make($request->all(), [
            'username' => 'sometimes|required|string|max:255|unique:users,username,' . $vendor->id,
            'fullName' => 'sometimes|required|string|max:255',
            'info' => 'nullable|string|max:1000',
            'pin' => 'nullable|string|min:4|max:128',
            'is_open' => 'sometimes|boolean',
        ]);
        if ($validator->fails()) return response()->json(['success' => false, 'errors' => $validator->errors()], 422);

        $vendor->fill($request->only(['username', 'fullName', 'info', 'is_open']));
        if ($request->filled('pin')) $vendor->password = Hash::make($request->input('pin'));
        $vendor->save();

        AuditLog::create([
            'user_id' => $admin->id,
            'timestamp' => time() * 1000,
            'action' => 'ADMIN_VENDOR_UPDATED',
            'details' => "Administrator updated vendor '{$vendor->fullName}' (ID {$vendor->id}).",
        ]);

        return response()->json(['success' => true, 'message' => 'Vendor updated successfully.', 'vendor' => $vendor]);
    }

    public function deleteVendor(Request $request, int $vendorId)
    {
        $admin = $this->admin($request);
        if (!$admin) return $this->deny();

        $vendor = User::where('role', 'VENDOR')->find($vendorId);
        if (!$vendor) return response()->json(['success' => false, 'message' => 'Vendor not found.'], 404);

        $name = $vendor->fullName;
        DB::transaction(function () use ($vendor, $admin, $name) {
            $vendor->tokens()->delete();
            $vendor->delete();
            AuditLog::create([
                'user_id' => $admin->id,
                'timestamp' => time() * 1000,
                'action' => 'ADMIN_VENDOR_DELETED',
                'details' => "Administrator deleted vendor '{$name}' (ID {$vendor->id}).",
            ]);
        });

        return response()->json(['success' => true, 'message' => 'Vendor deleted successfully.']);
    }

    public function users(Request $request)
    {
        $admin = $this->admin($request);
        if (!$admin) return $this->deny();

        return response()->json([
            'success' => true,
            'users' => User::orderBy('role')->orderBy('id')->get(),
        ]);
    }
}
