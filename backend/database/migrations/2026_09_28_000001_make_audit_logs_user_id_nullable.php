<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    /**
     * Order-audit middleware records placement attempts before the auth layer
     * resolves the bearer token, so user_id is legitimately null for
     * unauthenticated attempts. SQLite tolerated NULL here; PostgreSQL does
     * not, so the column must be nullable.
     */
    public function up(): void
    {
        Schema::table('audit_logs', function (Blueprint $table) {
            $table->unsignedBigInteger('user_id')->nullable()->change();
        });
    }

    public function down(): void
    {
        // Nullable audits cannot be re-constrained; drop them first.
        DB::table('audit_logs')->whereNull('user_id')->delete();

        Schema::table('audit_logs', function (Blueprint $table) {
            $table->unsignedBigInteger('user_id')->nullable(false)->change();
        });
    }
};
