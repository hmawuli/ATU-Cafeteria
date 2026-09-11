<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration {
    public function up(): void
    {
        Schema::create('vendor_workers', function (Blueprint $table) {
            $table->id();
            $table->foreignId('vendor_id')->constrained('users')->cascadeOnDelete();
            $table->string('full_name');
            $table->string('username')->unique();
            $table->string('password');
            $table->string('position')->default('Staff');
            $table->string('phone')->nullable();
            $table->boolean('is_active')->default(true);
            $table->timestamps();
            $table->index(['vendor_id', 'is_active']);
        });

        Schema::create('worker_shifts', function (Blueprint $table) {
            $table->id();
            $table->foreignId('vendor_id')->constrained('users')->cascadeOnDelete();
            $table->foreignId('worker_id')->constrained('vendor_workers')->cascadeOnDelete();
            $table->string('shift_name')->default('Regular');
            $table->date('shift_date')->nullable();
            $table->time('start_time');
            $table->time('end_time');
            $table->string('status')->default('SCHEDULED');
            $table->timestamps();
            $table->index(['vendor_id', 'shift_date']);
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('worker_shifts');
        Schema::dropIfExists('vendor_workers');
    }
};
