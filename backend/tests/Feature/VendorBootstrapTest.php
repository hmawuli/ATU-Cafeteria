<?php

namespace Tests\Feature;

use App\Models\User;
use App\Models\Vendor;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\Hash;
use Tests\TestCase;

class VendorBootstrapTest extends TestCase
{
    use RefreshDatabase;

    public function test_vendor_create_rejects_a_known_weak_pin(): void
    {
        $this->artisan('vendor:create', ['storeName' => 'Weak Grill', '--pin' => 'vendor123'])
            ->assertExitCode(1);

        $this->assertDatabaseMissing('users', ['username' => 'weak-grill']);
    }

    public function test_vendor_create_creates_user_vendor_and_audit(): void
    {
        $this->artisan('vendor:create', [
            'storeName' => 'Campus Grill',
            '--username' => 'campusgrill',
            '--pin' => 's7R!ngPin09',
            '--location' => 'Block F, Booth 2',
        ])->assertExitCode(0);

        $user = User::where('username', 'campusgrill')->first();
        $this->assertNotNull($user);
        $this->assertSame('VENDOR', $user->role);
        $this->assertTrue(Hash::check('s7R!ngPin09', $user->password));

        $this->assertDatabaseHas('vendors', [
            'user_id' => $user->id,
            'store_name' => 'Campus Grill',
            'campus' => 'Accra Technical University',
        ]);

        $this->assertDatabaseHas('audit_logs', ['user_id' => $user->id, 'action' => 'VENDOR_CREATED']);
    }

    public function test_vendor_create_supports_custom_campus(): void
    {
        $this->artisan('vendor:create', [
            'storeName' => 'Kumasi Annex',
            '--username' => 'kumasiannex',
            '--pin' => 'aNother$trong1',
            '--campus' => 'KumasiCampus',
        ])->assertExitCode(0);

        $vendor = Vendor::where('store_name', 'Kumasi Annex')->first();
        $this->assertNotNull($vendor);
        $this->assertSame('KumasiCampus', $vendor->campus);
    }

    public function test_vendor_create_rejects_duplicate_username(): void
    {
        User::create([
            'username' => 'takenvendor',
            'password' => Hash::make('aStr0ngOne!9'),
            'role' => 'VENDOR',
            'fullName' => 'Taken',
        ]);

        $this->artisan('vendor:create', [
            'storeName' => 'Taken Grill',
            '--username' => 'takenvendor',
            '--pin' => 'aStr0ngOne!9',
        ])->assertExitCode(1);
    }
}
