<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\CustomerDevice;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Validator;

class CustomerDeviceController extends Controller
{
    public function index(Request $request)
    {
        $devices = CustomerDevice::where('customer_id', $request->user()->id)
            ->active()->latest('last_seen_at')->get();

        return response()->json(['success' => true, 'devices' => $devices]);
    }

    public function store(Request $request)
    {
        $validator = Validator::make($request->all(), [
            'device_id' => 'required|string|min:3|max:191',
            'platform' => 'required|string|in:android,ios,web,linux,windows,macos',
            'push_token' => 'nullable|string|max:4096',
            'app_version' => 'nullable|string|max:40',
        ]);

        if ($validator->fails()) {
            return response()->json([
                'success' => false,
                'message' => 'Invalid device registration details.',
                'errors' => $validator->errors(),
            ], 422);
        }

        $device = CustomerDevice::updateOrCreate(
            [
                'customer_id' => $request->user()->id,
                'device_id' => trim((string) $request->input('device_id')),
            ],
            [
                'platform' => strtolower((string) $request->input('platform')),
                'push_token' => $request->input('push_token'),
                'app_version' => $request->input('app_version'),
                'last_seen_at' => now(),
                'revoked_at' => null,
            ]
        );

        return response()->json([
            'success' => true,
            'message' => 'Device registered successfully.',
            'device' => $device,
        ], 201);
    }

    public function heartbeat(Request $request, CustomerDevice $device)
    {
        abort_unless((int) $device->customer_id === (int) $request->user()->id, 403);

        $device->update([
            'last_seen_at' => now(),
            'app_version' => $request->input('app_version', $device->app_version),
        ]);

        return response()->json(['success' => true, 'device' => $device]);
    }

    public function destroy(Request $request, CustomerDevice $device)
    {
        abort_unless((int) $device->customer_id === (int) $request->user()->id, 403);

        $device->update(['revoked_at' => now(), 'push_token' => null]);

        return response()->json(['success' => true, 'message' => 'Device access revoked.']);
    }
}
