<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        if (! Schema::hasTable('vendor_payout_accounts')) {
            Schema::create('vendor_payout_accounts', function (Blueprint $table) {
                $table->id();
                $table->foreignId('vendor_id')->unique()->constrained('users')->cascadeOnDelete();
                $table->string('type', 30);
                $table->string('bank_code', 60);
                $table->string('bank_name', 160)->nullable();
                $table->text('account_number');
                $table->string('account_number_last4', 8);
                $table->string('account_name', 160);
                $table->string('currency', 3)->default('GHS');
                $table->string('recipient_code', 120)->nullable()->index();
                $table->string('status', 30)->default('PENDING')->index();
                $table->timestamp('verified_at')->nullable();
                $table->timestamp('revoked_at')->nullable();
                $table->timestamps();
            });
        }

        if (Schema::hasTable('vendor_settlements') && ! Schema::hasColumn('vendor_settlements', 'transfer_code')) {
            Schema::table('vendor_settlements', function (Blueprint $table) {
                $table->string('transfer_code', 120)->nullable()->index()->after('payout_reference');
                $table->string('gateway_status', 40)->nullable()->after('transfer_code');
                $table->text('failure_reason')->nullable()->after('gateway_status');
                $table->timestamp('payout_attempted_at')->nullable()->after('failure_reason');
            });
        }
    }

    public function down(): void
    {
        if (Schema::hasTable('vendor_settlements') && Schema::hasColumn('vendor_settlements', 'transfer_code')) {
            Schema::table('vendor_settlements', function (Blueprint $table) {
                $table->dropColumn(['transfer_code', 'gateway_status', 'failure_reason', 'payout_attempted_at']);
            });
        }

        Schema::dropIfExists('vendor_payout_accounts');
    }
};
