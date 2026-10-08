<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        // Workers belong to a vendor and are managed by the vendor supervisor.
        Schema::create('worker_profiles', function (Blueprint $table) {
            $table->id();
            $table->unsignedBigInteger('vendor_id');
            $table->string('full_name');
            $table->string('staff_id')->nullable();
            $table->string('phone')->nullable();
            $table->decimal('daily_wage', 10, 2)->nullable();
            $table->json('days_of_week')->nullable();       // [0..6]
            $table->time('shift_start')->nullable();
            $table->time('shift_end')->nullable();
            $table->string('shift_label')->nullable();       // e.g. "Morning", "Night"
            $table->decimal('weekly_hours', 5, 2)->nullable();
            $table->boolean('is_active')->default(true);
            $table->timestamps();

            $table->foreign('vendor_id')->references('id')->on('users')->cascadeOnDelete();
            $table->index('vendor_id');
        });

        // Simple ledger a supervisor keeps for each worker ("check their finances").
        Schema::create('worker_payment_ledger', function (Blueprint $table) {
            $table->id();
            $table->unsignedBigInteger('worker_profile_id');
            $table->decimal('amount', 10, 2);
            $table->date('payment_date');
            $table->string('note')->nullable();
            $table->unsignedBigInteger('recorded_by')->nullable();
            $table->timestamps();

            $table->foreign('worker_profile_id')->references('id')->on('worker_profiles')->cascadeOnDelete();
            $table->foreign('recorded_by')->references('id')->on('users')->nullOnDelete();
            $table->index('worker_profile_id');
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('worker_payment_ledger');
        Schema::dropIfExists('worker_profiles');
    }
};
