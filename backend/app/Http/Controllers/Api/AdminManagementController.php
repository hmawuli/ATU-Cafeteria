<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\AuditLog;
use App\Models\Order;
use App\Models\SystemSetting;
use App\Models\User;
use App\Models\Vendor;
use App\Models\WalletTransaction;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use Illuminate\Validation\Rule;

class AdminManagementController extends Controller
{
    public function dashboard()
    {
        $today = now()->startOfDay();
        return response()->json([
            'success' => true,
            'data' => [
                'students' => User::where('role', 'STUDENT')->count(),
                'vendors' => User::where('role', 'VENDOR')->count(),
                'active_users' => User::where('account_status', 'ACTIVE')->count(),
                'suspended_users' => User::where('account_status', 'SUSPENDED')->count(),
                'orders_today' => Order::where('created_at', '>=', $today)->count(),
                'pending_orders' => Order::whereIn('status', ['PENDING', 'ORDER_PLACED'])->count(),
                'completed_orders' => Order::where('status', 'COMPLETED')->count(),
                'sales_today' => (float) Order::where('created_at', '>=', $today)->whereIn('status', ['COMPLETED', 'READY'])->sum('total_price'),
                'wallet_balance' => (float) User::sum('balance'),
            ],
        ]);
    }

    public function users(Request $request)
    {
        $query = User::query()->select(['id','username','role','fullName','student_staff_id','balance','is_open','account_status','admin_level','last_login_at','created_at']);
        if ($request->filled('role')) $query->where('role', strtoupper($request->string('role')));
        if ($request->filled('status')) $query->where('account_status', strtoupper($request->string('status')));
        if ($request->filled('search')) {
            $term = '%'.trim($request->string('search')).'%';
            $query->where(fn($q) => $q->where('username','like',$term)->orWhere('fullName','like',$term)->orWhere('student_staff_id','like',$term));
        }
        return response()->json(['success' => true, 'data' => $query->latest()->paginate(min((int) $request->input('per_page', 50), 100))]);
    }

    public function updateUserStatus(Request $request, User $user)
    {
        $data = $request->validate(['account_status' => ['required', Rule::in(['ACTIVE','SUSPENDED','DISABLED'])]]);
        if ($user->id === $request->user()->id && $data['account_status'] !== 'ACTIVE') {
            return response()->json(['success' => false, 'message' => 'You cannot deactivate your own active administrator account.'], 422);
        }
        $old = $user->account_status;
        $user->account_status = $data['account_status'];
        $user->save();
        $this->audit($request, 'USER_STATUS_CHANGED', "User {$user->id}: {$old} -> {$user->account_status}");
        return response()->json(['success' => true, 'message' => 'Account status updated.', 'data' => $user->fresh()]);
    }

    public function updateAdminLevel(Request $request, User $user)
    {
        abort_unless($request->user()->isSuperAdmin(), 403, 'Only a Super Admin can change administrative levels.');
        if ($user->role !== 'ADMIN') return response()->json(['success'=>false,'message'=>'Target user is not an administrator.'],422);
        $data = $request->validate(['admin_level' => ['required', Rule::in(['SUPER_ADMIN','CAFETERIA_ADMIN','FINANCE_ADMIN'])]]);
        if ($user->id === $request->user()->id && $data['admin_level'] !== 'SUPER_ADMIN') return response()->json(['success'=>false,'message'=>'You cannot lower your own Super Admin privileges.'],422);
        $user->admin_level = $data['admin_level'];
        $user->save();
        $this->audit($request, 'ADMIN_LEVEL_CHANGED', "Admin {$user->id} assigned {$user->admin_level}.");
        return response()->json(['success'=>true,'message'=>'Administrative level updated.','data'=>$user->fresh()]);
    }

    public function vendors(Request $request)
    {
        $vendors = Vendor::with('user:id,username,fullName,account_status,admin_level')->latest()->paginate(min((int)$request->input('per_page',50),100));
        return response()->json(['success'=>true,'data'=>$vendors]);
    }

    public function updateVendorStatus(Request $request, Vendor $vendor)
    {
        $data = $request->validate(['operational_status' => ['required', Rule::in(['ACTIVE','INACTIVE','SUSPENDED','PENDING'])]]);
        $old = $vendor->operational_status;
        $vendor->operational_status = $data['operational_status'];
        $vendor->save();
        $this->audit($request, 'VENDOR_STATUS_CHANGED', "Vendor {$vendor->id}: {$old} -> {$vendor->operational_status}");
        return response()->json(['success'=>true,'message'=>'Vendor status updated.','data'=>$vendor->fresh('user')]);
    }

    public function orders(Request $request)
    {
        $query = Order::with(['user:id,username,fullName','vendor:id,username,fullName'])->latest();
        if ($request->filled('status')) $query->where('status', strtoupper($request->string('status')));
        if ($request->filled('vendor_id')) $query->where('vendor_id', $request->integer('vendor_id'));
        if ($request->filled('student_id')) $query->where('user_id', $request->integer('student_id'));
        return response()->json(['success'=>true,'data'=>$query->paginate(min((int)$request->input('per_page',50),100))]);
    }

    public function financeSummary()
    {
        return response()->json(['success'=>true,'data'=>[
            'total_wallet_balance'=>(float)User::sum('balance'),
            'successful_deposits'=>(float)WalletTransaction::where('type','DEPOSIT')->where('status','SUCCESS')->sum('amount'),
            'successful_payments'=>(float)WalletTransaction::where('type','PAYMENT')->where('status','SUCCESS')->sum('amount'),
            'successful_refunds'=>(float)WalletTransaction::where('type','REFUND')->where('status','SUCCESS')->sum('amount'),
            'pending_payouts'=>(float)WalletTransaction::where('type','PAYOUT')->where('status','PENDING')->sum('amount'),
        ]]);
    }

    public function walletAdjustment(Request $request, User $user)
    {
        $data = $request->validate(['amount'=>['required','numeric','min:0.01','max:1000000'],'type'=>['required',Rule::in(['REFUND','DEPOSIT'])],'reason'=>['required','string','min:5','max:500']]);
        $amount=(float)$data['amount'];
        DB::transaction(function() use ($request,$user,$data,$amount) {
            if ($data['type']==='REFUND') $user->balance += $amount; else $user->balance += $amount;
            $user->save();
            WalletTransaction::create(['user_id'=>$user->id,'type'=>$data['type'],'amount'=>$amount,'status'=>'SUCCESS','reference'=>'ADMIN-'.str()->upper(str()->random(12)),'details'=>$data['reason']]);
            $this->audit($request,'WALLET_ADJUSTMENT',"User {$user->id}: {$data['type']} GHS {$amount}. Reason: {$data['reason']}");
        });
        return response()->json(['success'=>true,'message'=>'Wallet adjustment recorded.','data'=>['user_id'=>$user->id,'balance'=>(float)$user->fresh()->balance]]);
    }

    public function settings()
    {
        return response()->json(['success'=>true,'data'=>SystemSetting::orderBy('key')->get()]);
    }

    public function updateSetting(Request $request, string $key)
    {
        $data=$request->validate(['value'=>'nullable|string|max:5000','type'=>['nullable',Rule::in(['string','integer','decimal','boolean','json'])],'description'=>'nullable|string|max:1000']);
        $setting=SystemSetting::updateOrCreate(['key'=>$key],['value'=>$data['value'] ?? null,'type'=>$data['type'] ?? 'string','description'=>$data['description'] ?? null]);
        $this->audit($request,'SYSTEM_SETTING_CHANGED',"System setting {$key} updated.");
        return response()->json(['success'=>true,'message'=>'System setting updated.','data'=>$setting]);
    }

    public function auditLogs(Request $request)
    {
        $query=AuditLog::with('user:id,username,fullName')->latest();
        if($request->filled('action')) $query->where('action',$request->string('action'));
        if($request->filled('user_id')) $query->where('user_id',$request->integer('user_id'));
        return response()->json(['success'=>true,'data'=>$query->paginate(min((int)$request->input('per_page',50),100))]);
    }

    private function audit(Request $request,string $action,string $details): void
    {
        AuditLog::create(['user_id'=>$request->user()->id,'timestamp'=>now()->getTimestampMs(),'action'=>$action,'details'=>$details]);
    }
    public function securityActivity(Request $request)
    {
        $limit = min(100, max(1, (int) $request->query('limit', 50)));
        $query = AuditLog::query()
            ->whereIn('action', [
                'AUTH_FAILURE','AUTH_2FA_CHALLENGE','AUTH_2FA_SUCCESS','USER_AUTHENTICATION',
                'PASSWORD_RESET_REQUEST','PASSWORD_CHANGED','2FA_ENABLED','2FA_DISABLED',
                'EMAIL_VERIFICATION_REQUESTED','EMAIL_VERIFIED','USER_LOGOUT',
            ])
            ->latest('id')
            ->limit($limit);

        return response()->json([
            'success' => true,
            'data' => $query->get(['id','user_id','action','details','timestamp','created_at']),
        ]);
    }

}
