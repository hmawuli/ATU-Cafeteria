<?php

namespace Tests\Unit;

use App\Models\Order;
use App\Models\User;
use App\Observers\OrderStatusObserver;
use Illuminate\Support\Facades\Auth;
use LogicException;
use Tests\TestCase;

class OrderStatusObserverTest extends TestCase
{
    public function test_student_order_creation_is_bound_to_authenticated_student(): void
    {
        $student = new User;
        $student->id = 42;
        $student->role = 'STUDENT';
        Auth::login($student);

        $order = new Order(['customer_id' => 999, 'student_id' => 999, 'user_id' => 999, 'status' => 'PENDING']);

        (new OrderStatusObserver)->creating($order);

        $this->assertSame(42, $order->customer_id);
        $this->assertSame(42, $order->student_id);
        $this->assertSame(42, $order->user_id);
    }

    public function test_non_admin_cannot_skip_order_lifecycle(): void
    {
        $vendor = new User;
        $vendor->id = 7;
        $vendor->role = 'VENDOR';
        Auth::login($vendor);

        $order = new Order(['status' => 'PENDING']);
        $order->exists = true;
        $order->syncOriginalAttribute('status', 'PENDING');
        $order->status = 'READY';

        $this->expectException(LogicException::class);
        (new OrderStatusObserver)->saving($order);
    }

    public function test_admin_can_perform_an_explicit_status_reconciliation(): void
    {
        $admin = new User;
        $admin->id = 1;
        $admin->role = 'ADMIN';
        Auth::login($admin);

        $order = new Order(['status' => 'PENDING']);
        $order->exists = true;
        $order->syncOriginalAttribute('status', 'PENDING');
        $order->status = 'COMPLETED';

        (new OrderStatusObserver)->saving($order);
        $this->assertSame('COMPLETED', $order->status);
    }
}
