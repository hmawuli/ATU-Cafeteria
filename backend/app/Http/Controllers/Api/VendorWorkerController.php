<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\AuditLog;
use App\Models\WorkerPayment;
use App\Models\WorkerProfile;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Validator;

class VendorWorkerController extends Controller
{
    /** All workers belonging to the signed-in vendor. */
    public function index(Request $request)
    {
        $vendor = $this->requireVendor($request);

        $workers = WorkerProfile::with('payments')
            ->where('vendor_id', $vendor->id)
            ->orderBy('full_name')
            ->get()
            ->map(fn (WorkerProfile $w) => $this->serialize($w));

        return response()->json(['success' => true, 'workers' => $workers], 200);
    }

    /** Employ a new worker for the vendor (with shift schedule). */
    public function store(Request $request)
    {
        $vendor = $this->requireVendor($request);

        $validator = Validator::make($request->all(), [
            'full_name' => 'required|string|max:255',
            'staff_id' => 'nullable|string|max:80',
            'phone' => 'nullable|string|max:40',
            'daily_wage' => 'nullable|numeric|min:0|max:1000000',
            'days_of_week' => 'nullable|array|min:1|max:7',
            'days_of_week.*' => 'integer|between:0,6',
            'shift_start' => 'nullable|date_format:H:i',
            'shift_end' => 'nullable|date_format:H:i',
            'shift_label' => 'nullable|string|max:80',
            'weekly_hours' => 'nullable|numeric|min:0|max:168',
        ]);

        if ($validator->fails()) {
            return response()->json(['success' => false, 'message' => 'Validation error.', 'errors' => $validator->errors()], 422);
        }

        $worker = DB::transaction(function () use ($request, $vendor) {
            $profile = WorkerProfile::create([
                'vendor_id' => $vendor->id,
                'full_name' => trim((string) $request->input('full_name')),
                'staff_id' => $request->input('staff_id'),
                'phone' => $request->input('phone'),
                'daily_wage' => $request->input('daily_wage'),
                'days_of_week' => $request->input('days_of_week'),
                'shift_start' => $request->input('shift_start'),
                'shift_end' => $request->input('shift_end'),
                'shift_label' => $request->input('shift_label'),
                'weekly_hours' => $request->input('weekly_hours'),
            ]);

            AuditLog::create([
                'user_id' => $vendor->id,
                'timestamp' => time() * 1000,
                'action' => 'WORKER_ADDED',
                'details' => "Vendor employed worker '{$profile->full_name}'.",
            ]);

            return $profile;
        });

        return response()->json(['success' => true, 'message' => 'Worker added.', 'worker' => $this->serialize($worker)], 201);
    }

    /** Update a worker's profile or shift schedule. */
    public function update(Request $request, $id)
    {
        $vendor = $this->requireVendor($request);
        $worker = $this->owned($vendor, $id);

        $validator = Validator::make($request->all(), [
            'full_name' => 'nullable|string|max:255',
            'staff_id' => 'nullable|string|max:80',
            'phone' => 'nullable|string|max:40',
            'daily_wage' => 'nullable|numeric|min:0|max:1000000',
            'days_of_week' => 'nullable|array|min:1|max:7',
            'days_of_week.*' => 'integer|between:0,6',
            'shift_start' => 'nullable|date_format:H:i',
            'shift_end' => 'nullable|date_format:H:i',
            'shift_label' => 'nullable|string|max:80',
            'weekly_hours' => 'nullable|numeric|min:0|max:168',
            'is_active' => 'nullable|boolean',
        ]);

        if ($validator->fails()) {
            return response()->json(['success' => false, 'message' => 'Validation error.', 'errors' => $validator->errors()], 422);
        }

        $worker->update([
            'full_name' => $request->has('full_name') ? trim((string) $request->input('full_name')) : $worker->full_name,
            'staff_id' => $request->has('staff_id') ? $request->input('staff_id') : $worker->staff_id,
            'phone' => $request->has('phone') ? $request->input('phone') : $worker->phone,
            'daily_wage' => $request->has('daily_wage') ? $request->input('daily_wage') : $worker->daily_wage,
            'days_of_week' => $request->has('days_of_week') ? $request->input('days_of_week') : $worker->days_of_week,
            'shift_start' => $request->has('shift_start') ? $request->input('shift_start') : $worker->shift_start,
            'shift_end' => $request->has('shift_end') ? $request->input('shift_end') : $worker->shift_end,
            'shift_label' => $request->has('shift_label') ? $request->input('shift_label') : $worker->shift_label,
            'weekly_hours' => $request->has('weekly_hours') ? $request->input('weekly_hours') : $worker->weekly_hours,
            'is_active' => $request->has('is_active') ? $request->input('is_active') : $worker->is_active,
        ]);

        AuditLog::create([
            'user_id' => $vendor->id,
            'timestamp' => time() * 1000,
            'action' => 'WORKER_UPDATED',
            'details' => "Vendor updated worker '{$worker->full_name}'.",
        ]);

        return response()->json(['success' => true, 'message' => 'Worker updated.', 'worker' => $this->serialize($worker->fresh())], 200);
    }

    /** Remove (fire) a worker. */
    public function destroy(Request $request, $id)
    {
        $vendor = $this->requireVendor($request);
        $worker = $this->owned($vendor, $id);

        $name = $worker->full_name;
        $worker->delete();

        AuditLog::create([
            'user_id' => $vendor->id,
            'timestamp' => time() * 1000,
            'action' => 'WORKER_REMOVED',
            'details' => "Vendor removed worker '{$name}'.",
        ]);

        return response()->json(['success' => true, 'message' => 'Worker removed.'], 200);
    }

    /** Worker finances: wages summary + payment ledger. */
    public function finance(Request $request, $id)
    {
        $vendor = $this->requireVendor($request);
        $worker = WorkerProfile::with('payments')->where('vendor_id', $vendor->id)->findOrFail($id);

        $payments = $worker->payments->sortByDesc('payment_date')->values();
        $totalPaid = round((float) $worker->payments->sum('amount'), 2);

        return response()->json([
            'success' => true,
            'worker' => $this->serialize($worker),
            'finance' => [
                'total_paid' => $totalPaid,
                'payment_count' => $payments->count(),
                'daily_wage' => $worker->daily_wage,
                'payments' => $payments->map(fn (WorkerPayment $p) => [
                    'id' => $p->id,
                    'amount' => round((float) $p->amount, 2),
                    'payment_date' => $p->payment_date->toDateString(),
                    'note' => $p->note,
                    'recorded_at' => $p->created_at?->toIso8601String(),
                ])->values(),
            ],
        ], 200);
    }

    /** Record a payment to a worker (supervisor keeps the finance ledger). */
    public function storePayment(Request $request, $id)
    {
        $vendor = $this->requireVendor($request);
        $worker = $this->owned($vendor, $id);

        $validator = Validator::make($request->all(), [
            'amount' => 'required|numeric|min:0.01|max:1000000',
            'payment_date' => 'required|date',
            'note' => 'nullable|string|max:500',
        ]);

        if ($validator->fails()) {
            return response()->json(['success' => false, 'message' => 'Validation error.', 'errors' => $validator->errors()], 422);
        }

        $payment = WorkerPayment::create([
            'worker_profile_id' => $worker->id,
            'amount' => $request->input('amount'),
            'payment_date' => $request->input('payment_date'),
            'note' => $request->input('note'),
            'recorded_by' => $vendor->id,
        ]);

        AuditLog::create([
            'user_id' => $vendor->id,
            'timestamp' => time() * 1000,
            'action' => 'WORKER_PAYMENT_RECORDED',
            'details' => "Vendor recorded GH₵{$payment->amount} payment to worker '{$worker->full_name}'.",
        ]);

        return response()->json(['success' => true, 'message' => 'Payment recorded.', 'payment' => [
            'id' => $payment->id,
            'amount' => round((float) $payment->amount, 2),
            'payment_date' => $payment->payment_date->toDateString(),
            'note' => $payment->note,
        ]], 201);
    }

    private function requireVendor(Request $request)
    {
        $user = $request->user();
        if (! $user || strtoupper((string) $user->role) !== 'VENDOR') {
            abort(403, 'Vendor access required.');
        }

        return $user;
    }

    private function owned($vendor, $id): WorkerProfile
    {
        $worker = WorkerProfile::where('vendor_id', $vendor->id)->find($id);
        if (! $worker) {
            abort(404, 'Worker not found.');
        }

        return $worker;
    }

    private function serialize(WorkerProfile $worker): array
    {
        return [
            'id' => $worker->id,
            'full_name' => $worker->full_name,
            'staff_id' => $worker->staff_id,
            'phone' => $worker->phone,
            'daily_wage' => $worker->daily_wage,
            'days_of_week' => $worker->days_of_week ?? [],
            'shift_start' => $worker->shift_start,
            'shift_end' => $worker->shift_end,
            'shift_label' => $worker->shift_label,
            'weekly_hours' => $worker->weekly_hours,
            'is_active' => (bool) $worker->is_active,
            'created_at' => $worker->created_at?->toIso8601String(),
            'total_paid' => round((float) $worker->payments()->sum('amount'), 2),
            'payment_count' => $worker->payments()->count(),
        ];
    }
}
