<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\AuditLog;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Validator;

class AuditLogController extends Controller
{
    /**
     * Retrieve system-wide administrator audit streams.
     */
    public function index()
    {
        $logs = AuditLog::orderBy('timestamp', 'desc')->get();
        return response()->json($logs, 200);
    }

    /**
     * Store a general trace, wallet recharges, or operational event log manually.
     */
    public function store(Request $request)
    {
        $validator = Validator::make($request->all(), [
            'user_id' => 'required|integer|exists:users,id',
            'action' => 'required|string|max:255',
            'details' => 'required|string',
        ]);

        if ($validator->fails()) {
            return response()->json([
                'success' => false,
                'message' => 'Audit structure validation failed.',
                'errors' => $validator->errors()
            ], 400);
        }

        $log = AuditLog::create([
            'user_id' => $request->input('user_id'),
            'timestamp' => time() * 1000,
            'action' => $request->input('action'),
            'details' => $request->input('details'),
        ]);

        return response()->json($log, 201);
    }
}
