<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\AuditLog;
use App\Models\Feedback;
use App\Models\Order;
use App\Models\VendorWorker;
use App\Models\WorkerShift;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Hash;
use Illuminate\Support\Facades\Validator;

class VendorStaffController extends Controller
{
    private function vendor(Request $request)
    {
        return $request->user();
    }

    public function overview(Request $request)
    {
        $vendor = $this->vendor($request);
        $completed = ['COMPLETED', 'DELIVERED'];
        $orders = Order::where('vendor_id', $vendor->id);
        $reviews = Feedback::where('vendor_id', $vendor->id);

        return response()->json([
            'success' => true,
            'vendor' => $vendor,
            'statistics' => [
                'orders' => (clone $orders)->count(),
                'completed_orders' => (clone $orders)->whereIn('status', $completed)->count(),
                'revenue' => (float) (clone $orders)->whereIn('status', $completed)->sum('total_price'),
                'revenue_today' => (float) (clone $orders)->whereIn('status', $completed)->whereDate('created_at', today())->sum('total_price'),
                'workers' => VendorWorker::where('vendor_id', $vendor->id)->count(),
                'active_workers' => VendorWorker::where('vendor_id', $vendor->id)->where('is_active', true)->count(),
                'reviews' => (clone $reviews)->count(),
                'average_rating' => round((float) (($reviews->avg(DB::raw('(rating_food_quality + rating_cleanliness + rating_service_speed + rating_price_value) / 4')))), 2),
            ],
            'workers' => VendorWorker::with('shifts')->where('vendor_id', $vendor->id)->latest()->get(),
            'recent_orders' => (clone $orders)->latest('id')->limit(20)->get(),
            'reviews' => $reviews->latest('timestamp')->limit(30)->get(),
        ]);
    }

    public function workers(Request $request)
    {
        return response()->json(['success' => true, 'workers' => VendorWorker::with('shifts')->where('vendor_id', $this->vendor($request)->id)->latest()->get()]);
    }

    public function createWorker(Request $request)
    {
        $vendor = $this->vendor($request);
        $validator = Validator::make($request->all(), [
            'full_name' => 'required|string|max:255',
            'username' => 'required|string|max:255|unique:vendor_workers,username',
            'pin' => 'required|string|min:4|max:128',
            'position' => 'nullable|string|max:100',
            'phone' => 'nullable|string|max:40',
        ]);
        if ($validator->fails()) return response()->json(['success' => false, 'errors' => $validator->errors()], 422);

        $worker = VendorWorker::create([
            'vendor_id' => $vendor->id,
            'full_name' => $request->full_name,
            'username' => $request->username,
            'password' => Hash::make($request->pin),
            'position' => $request->position ?: 'Staff',
            'phone' => $request->phone,
        ]);
        AuditLog::create(['user_id' => $vendor->id, 'timestamp' => time() * 1000, 'action' => 'VENDOR_WORKER_CREATED', 'details' => "Vendor created worker '{$worker->full_name}'."]);
        return response()->json(['success' => true, 'worker' => $worker], 201);
    }

    public function updateWorker(Request $request, int $workerId)
    {
        $vendor = $this->vendor($request);
        $worker = VendorWorker::where('vendor_id', $vendor->id)->find($workerId);
        if (!$worker) return response()->json(['success' => false, 'message' => 'Worker not found.'], 404);

        $validator = Validator::make($request->all(), [
            'full_name' => 'sometimes|required|string|max:255',
            'username' => 'sometimes|required|string|max:255|unique:vendor_workers,username,' . $worker->id,
            'pin' => 'nullable|string|min:4|max:128',
            'position' => 'nullable|string|max:100',
            'phone' => 'nullable|string|max:40',
            'is_active' => 'sometimes|boolean',
        ]);
        if ($validator->fails()) return response()->json(['success' => false, 'errors' => $validator->errors()], 422);
        $worker->fill($request->only(['full_name', 'username', 'position', 'phone', 'is_active']));
        if ($request->filled('pin')) $worker->password = Hash::make($request->pin);
        $worker->save();
        AuditLog::create(['user_id' => $vendor->id, 'timestamp' => time() * 1000, 'action' => 'VENDOR_WORKER_UPDATED', 'details' => "Vendor updated worker '{$worker->full_name}'."]);
        return response()->json(['success' => true, 'worker' => $worker]);
    }

    public function deleteWorker(Request $request, int $workerId)
    {
        $vendor = $this->vendor($request);
        $worker = VendorWorker::where('vendor_id', $vendor->id)->find($workerId);
        if (!$worker) return response()->json(['success' => false, 'message' => 'Worker not found.'], 404);
        $name = $worker->full_name;
        $worker->delete();
        AuditLog::create(['user_id' => $vendor->id, 'timestamp' => time() * 1000, 'action' => 'VENDOR_WORKER_DELETED', 'details' => "Vendor deleted worker '{$name}'."]);
        return response()->json(['success' => true, 'message' => 'Worker removed.']);
    }

    public function createShift(Request $request)
    {
        $vendor = $this->vendor($request);
        $validator = Validator::make($request->all(), [
            'worker_id' => 'required|integer', 'shift_name' => 'nullable|string|max:100',
            'shift_date' => 'nullable|date', 'start_time' => 'required|date_format:H:i', 'end_time' => 'required|date_format:H:i',
        ]);
        if ($validator->fails()) return response()->json(['success' => false, 'errors' => $validator->errors()], 422);
        $worker = VendorWorker::where('vendor_id', $vendor->id)->find($request->worker_id);
        if (!$worker) return response()->json(['success' => false, 'message' => 'Worker does not belong to this vendor.'], 403);
        $shift = WorkerShift::create(['vendor_id' => $vendor->id, 'worker_id' => $worker->id, 'shift_name' => $request->shift_name ?: 'Regular', 'shift_date' => $request->shift_date, 'start_time' => $request->start_time, 'end_time' => $request->end_time]);
        AuditLog::create(['user_id' => $vendor->id, 'timestamp' => time() * 1000, 'action' => 'VENDOR_SHIFT_CREATED', 'details' => "Scheduled {$shift->shift_name} for '{$worker->full_name}'."]);
        return response()->json(['success' => true, 'shift' => $shift], 201);
    }

    public function updateShift(Request $request, int $shiftId)
    {
        $vendor = $this->vendor($request);
        $shift = WorkerShift::where('vendor_id', $vendor->id)->find($shiftId);
        if (!$shift) return response()->json(['success' => false, 'message' => 'Shift not found.'], 404);
        $validator = Validator::make($request->all(), ['shift_name' => 'nullable|string|max:100', 'shift_date' => 'nullable|date', 'start_time' => 'sometimes|required|date_format:H:i', 'end_time' => 'sometimes|required|date_format:H:i', 'status' => 'sometimes|string|max:30']);
        if ($validator->fails()) return response()->json(['success' => false, 'errors' => $validator->errors()], 422);
        $shift->fill($request->only(['shift_name', 'shift_date', 'start_time', 'end_time', 'status']))->save();
        return response()->json(['success' => true, 'shift' => $shift]);
    }

    public function deleteShift(Request $request, int $shiftId)
    {
        $shift = WorkerShift::where('vendor_id', $this->vendor($request)->id)->find($shiftId);
        if (!$shift) return response()->json(['success' => false, 'message' => 'Shift not found.'], 404);
        $shift->delete();
        return response()->json(['success' => true, 'message' => 'Shift removed.']);
    }

    public function reviews(Request $request)
    {
        $vendor = $this->vendor($request);
        $reviews = Feedback::where('vendor_id', $vendor->id)->latest('timestamp')->get();
        return response()->json(['success' => true, 'reviews' => $reviews]);
    }
}
