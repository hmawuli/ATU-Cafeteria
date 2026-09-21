<?php

namespace Tests\Unit;

use App\Models\Order;
use App\Models\User;
use Illuminate\Support\Facades\Auth;
use Tests\TestCase;

class OrderAccessScopeTest extends TestCase
{
    public function test_student_only_sees_own_orders(): void
    {
        $student = new User;
        $student->id = 10;
        $student->role = 'STUDENT';

        Auth::login($student);

        $query = Order::query()->toSql();

        $this->assertStringContainsString('customer_id', $query);
        $this->assertStringContainsString('student_id', $query);
        $this->assertStringContainsString('user_id', $query);
    }

    public function test_vendor_queries_are_scoped_to_vendor_id(): void
    {
        $vendor = new User;
        $vendor->id = 20;
        $vendor->role = 'VENDOR';

        Auth::login($vendor);

        $query = Order::query()->toSql();

        $this->assertStringContainsString('vendor_id', $query);
    }

    public function test_admin_queries_are_not_restricted_by_order_scope(): void
    {
        $admin = new User;
        $admin->id = 1;
        $admin->role = 'ADMIN';

        Auth::login($admin);

        $query = Order::query()->toSql();

        $this->assertStringNotContainsString('customer_id', $query);
        $this->assertStringNotContainsString('vendor_id', $query);
    }
}
