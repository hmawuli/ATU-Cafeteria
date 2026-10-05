<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

/**
 * Shared cache + cache locks.
 *
 * Rate limiting and payment/order idempotency use Cache::get / Cache::lock and
 * MUST be shared across processes and instances. On serverless hosting (Vercel)
 * the per-instance array store cannot provide that, so the database store is the
 * default there. On the single-host Docker stack the application and worker
 * share a volume, but the database store is safe for both.
 */
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('cache', function (Blueprint $table) {
            $table->string('key')->primary();
            $table->mediumText('value');
            $table->integer('expiration');
        });

        Schema::create('cache_locks', function (Blueprint $table) {
            $table->string('key')->primary();
            $table->string('owner');
            $table->integer('expiration');
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('cache_locks');
        Schema::dropIfExists('cache');
    }
};
