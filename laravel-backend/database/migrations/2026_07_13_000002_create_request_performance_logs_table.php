<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    /**
     * Run the migrations.
     */
    public function up(): void
    {
        Schema::create('request_performance_logs', function (Blueprint $table) {
            $table->id();
            $table->string('method', 10);
            $table->string('path', 255);
            $table->integer('status_code');
            $table->float('execution_time_ms');
            $table->float('memory_usage_mb')->nullable();
            $table->string('ip_address', 45)->nullable();
            $table->text('request_payload')->nullable(); // JSON payload or query params
            $table->timestamp('created_at')->useCurrent();
        });
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
        Schema::dropIfExists('request_performance_logs');
    }
};
