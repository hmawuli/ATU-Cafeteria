<?php

namespace Tests\Feature;

use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Laravel\Sanctum\Sanctum;
use Tests\TestCase;

class VendorWorkerTest extends TestCase
{
    use RefreshDatabase;

    private function vendor(): User
    {
        return User::factory()->create(['role' => 'VENDOR']);
    }

    public function test_vendor_can_employ_workers_schedule_shifts_and_keep_finances(): void
    {
        $vendor = $this->vendor();
        Sanctum::actingAs($vendor, ['vendor']);

        $created = $this->postJson('/api/vendor/workers', [
            'full_name' => 'Ama Mensah',
            'staff_id' => 'W-001',
            'phone' => '0244000000',
            'daily_wage' => 60,
            'days_of_week' => [0, 1, 2, 3, 4],
            'shift_start' => '08:00',
            'shift_end' => '16:00',
            'shift_label' => 'Morning',
            'weekly_hours' => 40,
        ])->assertStatus(201)->assertJsonPath('success', true);

        $id = $created->json('worker.id');
        $this->assertDatabaseHas('worker_profiles', [
            'id' => $id,
            'vendor_id' => $vendor->id,
            'full_name' => 'Ama Mensah',
        ]);

        $this->postJson("/api/vendor/workers/{$id}/payments", [
            'amount' => 240,
            'payment_date' => '2026-10-08',
            'note' => 'Weekly wages',
        ])->assertStatus(201);

        $finance = $this->getJson("/api/vendor/workers/{$id}/finance")->assertOk();
        $this->assertSame(240.0, (float) $finance->json('finance.total_paid'));
        $this->assertSame(1, $finance->json('finance.payment_count'));

        $this->getJson('/api/vendor/workers')->assertOk()->assertJsonCount(1, 'workers');

        $this->putJson("/api/vendor/workers/{$id}", [
            'shift_label' => 'Night',
            'shift_start' => '16:00',
        ])->assertOk()->assertJsonPath('worker.shift_label', 'Night');

        // Another vendor must not see or manage this worker.
        $other = $this->vendor();
        Sanctum::actingAs($other, ['vendor']);
        $this->getJson("/api/vendor/workers/{$id}/finance")->assertNotFound();
        $this->deleteJson("/api/vendor/workers/{$id}")->assertNotFound();
    }

    public function test_workers_require_vendor_role(): void
    {
        Sanctum::actingAs(User::factory()->create(['role' => 'STUDENT']));
        $this->getJson('/api/vendor/workers')->assertStatus(403);
    }
}
