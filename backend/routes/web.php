<?php

use App\Models\MenuItem;
use App\Models\Vendor;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Route;

// The application UI is Flutter. Laravel exposes the REST API.
Route::get('/', function () {
    return response()->json([
        'app' => 'ATU Cafeteria API',
        'status' => 'Healthy',
        'framework' => 'Laravel 12',
    ]);
});

Route::get('/health', function () {
    return response()->json([
        'app' => 'ATU Cafeteria API',
        'status' => 'Healthy',
        'framework' => 'Laravel 12',
    ]);
});

// Stand-out: public "what's open now" board (server-rendered, no auth).
Route::get('/stalls', function (Request $request) {
    $vendors = Vendor::query()
        ->with('user:id,username,fullName,is_open')
        ->when($request->filled('campus'), function ($q) use ($request) {
            $q->where('campus', $request->string('campus'));
        })
        ->get()
        ->map(function ($vendor) {
            $menuCount = MenuItem::where('vendor_id', $vendor->user_id)
                ->where('is_available', true)
                ->count();

            return [
                'name' => $vendor->store_name ?? $vendor->name,
                'open' => (bool) ($vendor->user?->is_open ?? false),
                'campus' => $vendor->campus,
                'menu_count' => $menuCount,
            ];
        });

    $rows = '';
    foreach ($vendors as $vendor) {
        $badge = $vendor['open']
            ? '<span style="color:#059669;font-weight:700">● OPEN NOW</span>'
            : '<span style="color:#b91c1c;font-weight:700">○ CLOSED</span>';
        $rows .= '<tr><td>'.htmlspecialchars((string) $vendor['name']).'</td>'
            .'<td>'.htmlspecialchars((string) $vendor['campus']).'</td>'
            ."<td>{$badge}</td>"
            .'<td>'.$vendor['menu_count'].' items</td></tr>';
    }

    return response(
        '<!DOCTYPE html><html lang="en"><head><meta charset="utf-8">'
            .'<meta name="viewport" content="width=device-width,initial-scale=1">'
            .'<title>What\'s Open Now — ATU Cafeteria</title></head>'
            .'<body style="font-family:system-ui,sans-serif;max-width:760px;margin:40px auto;padding:0 16px;color:#1e1b4b">'
            .'<h1>🍽️ ATU Cafeteria — What\'s Open Now</h1>'
            .'<p>Live stall availability from the cafeteria API.</p>'
            .'<table style="width:100%;border-collapse:collapse;text-align:left">'
            .'<tr style="border-bottom:2px solid #1e1b4b"><th>Stall</th><th>Campus</th><th>Status</th><th>Menu</th></tr>'
            .$rows
            .'</table><p style="margin-top:32px;color:#64748b;font-size:13px">'
            .'Point the Flutter app at <code>/api/stalls/{id}</code> to deep-link into a stall\'s menu (stall QR codes).'
            .'</p></body></html>',
        200,
        ['Content-Type' => 'text/html; charset=UTF-8']
    );
});
