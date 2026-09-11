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
        Schema::create('system_logs', function (Blueprint $table) {
            $table->id();
            $table->string('level', 15)->default('ERROR'); // ERROR, WARNING, etc.
            $table->integer('status_code')->nullable();
            $table->string('method', 10)->nullable();
            $table->string('path', 255)->nullable();
            $table->text('message');
            $table->text('stack_trace')->nullable();
            $table->string('ip_address', 45)->nullable();
            $table->json('user_agent')->nullable();
            $table->timestamp('created_at')->useCurrent();
        });
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
        Schema::dropIfExists('system_logs');
    }
};
