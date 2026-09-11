<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\DemandForecast;
use App\Models\FoodItem;
use App\Models\FoodWasteRecord;
use App\Models\Order;
use App\Models\SecurityAlert;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use Illuminate\Validation\Rule;

class SmartCafeteriaController extends Controller
{
    public function commandCenter(Request $request)
    {
        $today = now()->startOfDay();
        $activeOrders = Order::whereIn('status', ['PENDING','ORDER_PLACED','PREPARING','READY'])->count();
        $completedToday = Order::where('created_at','>=',$today)->where('status','COMPLETED')->count();
        $salesToday = (float) Order::where('created_at','>=',$today)->whereIn('status',['COMPLETED','READY'])->sum('total_price');
        
        $avgWaitQuery = Order::where('created_at','>=',now()->subDays(30))->whereNotNull('collected_at')->whereNotNull('created_at');
        $avgWait = DB::connection()->getDriverName() === 'sqlite'
            ? (int) round((float) $avgWaitQuery->selectRaw('AVG((julianday(collected_at) - julianday(created_at)) * 1440) as avg_wait')->value('avg_wait'))
            : (int) round((float) $avgWaitQuery->selectRaw('AVG(TIMESTAMPDIFF(MINUTE, created_at, collected_at)) as avg_wait')->value('avg_wait'));
        $openAlerts = SecurityAlert::whereNull('resolved_at')->count();
        $waste = FoodWasteRecord::where('recorded_date','>=',now()->subDays(30))->selectRaw('COALESCE(SUM(wasted_quantity),0) wasted, COALESCE(SUM(prepared_quantity),0) prepared')->first();
        $wasteRate = ((int)$waste->prepared > 0) ? round(((int)$waste->wasted / (int)$waste->prepared) * 100, 2) : 0;

        return response()->json(['success'=>true,'data'=>[
            'active_orders'=>$activeOrders,
            'completed_orders_today'=>$completedToday,
            'sales_today'=>round($salesToday,2),
            'average_wait_minutes'=>$avgWait,
            'open_security_alerts'=>$openAlerts,
            'waste_rate_percent'=>$wasteRate,
            'system_status'=>'HEALTHY',
        ]]);
    }

    public function queue(Request $request, int $orderId)
    {
        $order = Order::findOrFail($orderId);
        $user = $request->user();
        $role = strtoupper((string) $user->role);
        $ownsOrder = (int) $order->customer_id === (int) $user->id || (int) $order->user_id === (int) $user->id || (int) $order->vendor_id === (int) $user->id;
        if ($role !== 'ADMIN' && !$ownsOrder) {
            return response()->json(['success'=>false,'message'=>'You are not authorized to view this order queue position.'],403);
        }
        $position = Order::where('vendor_id',$order->vendor_id)
            ->whereIn('status',['PENDING','ORDER_PLACED','PREPARING'])
            ->where('id','<=',$order->id)->count();
        $avgQuery = Order::where('vendor_id',$order->vendor_id)->whereNotNull('collected_at')->whereNotNull('created_at')->where('created_at','>=',now()->subDays(30));
        $avg = DB::connection()->getDriverName() === 'sqlite'
            ? (float) $avgQuery->selectRaw('AVG((julianday(collected_at) - julianday(created_at)) * 1440) avg')->value('avg')
            : (float) $avgQuery->selectRaw('AVG(TIMESTAMPDIFF(MINUTE, created_at, collected_at)) avg')->value('avg');
        $wait = max(1, (int) round($avg ?: 15) * max(1, $position));
        $order->forceFill(['queue_position'=>$position,'estimated_wait_minutes'=>$wait])->save();
        return response()->json(['success'=>true,'data'=>['order_id'=>$order->id,'queue_position'=>$position,'estimated_wait_minutes'=>$wait,'status'=>$order->status]]);
    }

    public function recommendations(Request $request)
    {
        $user = $request->user();
        $ordered = Order::where('user_id',$user->id)->select('food_item_id')->whereNotNull('food_item_id')->groupBy('food_item_id')->orderByRaw('COUNT(*) DESC')->pluck('food_item_id');
        $items = FoodItem::where('is_available',true)->when($ordered->isNotEmpty(),fn($q)=>$q->orderByRaw('CASE WHEN id IN ('.implode(',', $ordered->map('intval')->all()).') THEN 0 ELSE 1 END'))
            ->orderByDesc('id')->limit(6)->get();
        return response()->json(['success'=>true,'data'=>$items,'meta'=>['strategy'=>'personal_history_then_availability']]);
    }

    public function demandForecast(Request $request)
    {
        $vendorId = $request->user()->id;
        $days = max(7, min(30, (int)$request->input('days',14)));
        $items = FoodItem::where('vendor_id',$vendorId)->get();
        $result=[];
        foreach($items as $item){
            $total=(int)Order::where('vendor_id',$vendorId)->where('food_item_id',$item->id)->where('created_at','>=',now()->subDays($days))->sum('quantity');
            $avg=$total / $days;
            $predicted=max(0,(int)ceil($avg * ($request->boolean('weekend_adjustment') ? 1.05 : 1)));
            $confidence=$total >= 30 ? 90 : ($total >= 10 ? 70 : 45);
            $forecast=DemandForecast::updateOrCreate(['vendor_id'=>$vendorId,'food_item_id'=>$item->id,'forecast_date'=>now()->addDay()->toDateString()],['predicted_quantity'=>$predicted,'method'=>'moving_average','confidence'=>$confidence]);
            $result[]=['food_item_id'=>$item->id,'food_name'=>$item->name,'predicted_quantity'=>$predicted,'confidence'=>$confidence,'forecast_date'=>$forecast->forecast_date->toDateString()];
        }
        return response()->json(['success'=>true,'data'=>$result]);
    }

    public function waste(Request $request)
    {
        $data=$request->validate(['food_item_id'=>'nullable|integer|exists:food_items,id','recorded_date'=>'nullable|date','prepared_quantity'=>'required|integer|min:0','sold_quantity'=>'required|integer|min:0','wasted_quantity'=>'required|integer|min:0','reason'=>'nullable|string|max:500']);
        $data['vendor_id']=$request->user()->id;
        $data['recorded_date']=$data['recorded_date'] ?? now()->toDateString();
        if ($data['sold_quantity'] + $data['wasted_quantity'] > $data['prepared_quantity']) return response()->json(['success'=>false,'message'=>'Sold plus wasted quantity cannot exceed prepared quantity.'],422);
        $record=FoodWasteRecord::create($data);
        return response()->json(['success'=>true,'message'=>'Waste record saved.','data'=>$record],201);
    }

    public function wasteSummary(Request $request)
    {
        $vendorId=$request->user()->id;
        $rows=FoodWasteRecord::where('vendor_id',$vendorId)->where('recorded_date','>=',now()->subDays(30))->selectRaw('COALESCE(SUM(prepared_quantity),0) prepared, COALESCE(SUM(sold_quantity),0) sold, COALESCE(SUM(wasted_quantity),0) wasted')->first();
        $rate=((int)$rows->prepared)>0?round(((int)$rows->wasted/(int)$rows->prepared)*100,2):0;
        return response()->json(['success'=>true,'data'=>['prepared'=>(int)$rows->prepared,'sold'=>(int)$rows->sold,'wasted'=>(int)$rows->wasted,'waste_rate_percent'=>$rate]]);
    }

    public function securityAlerts(Request $request)
    {
        return response()->json(['success'=>true,'data'=>SecurityAlert::with('user:id,username,fullName')->whereNull('resolved_at')->latest()->paginate(50)]);
    }

    public function resolveSecurityAlert(Request $request, SecurityAlert $alert)
    {
        $alert->update(['resolved_at'=>now(),'resolved_by'=>$request->user()->id]);
        return response()->json(['success'=>true,'message'=>'Security alert resolved.','data'=>$alert->fresh()]);
    }

    public static function recordFailedLogin(?int $userId, string $message='Multiple failed login attempts detected.'): void
    {
        SecurityAlert::create(['user_id'=>$userId,'type'=>'FAILED_LOGIN_PATTERN','severity'=>'HIGH','message'=>$message,'occurred_at'=>now()]);
    }
}
