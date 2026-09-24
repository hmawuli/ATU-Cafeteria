<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        if (! Schema::hasTable('orders')) {
            return;
        }

        Schema::table('orders', function (Blueprint $table) {
            if (! Schema::hasColumn('orders', 'accepted_at')) $table->timestamp('accepted_at')->nullable()->after('confirmed_at');
            if (! Schema::hasColumn('orders', 'preparing_at')) $table->timestamp('preparing_at')->nullable()->after('accepted_at');
            if (! Schema::hasColumn('orders', 'ready_at')) $table->timestamp('ready_at')->nullable()->after('preparing_at');
            if (! Schema::hasColumn('orders', 'collected_at')) $table->timestamp('collected_at')->nullable()->after('ready_at');
        });
    }

    public function down(): void
    {
        if (! Schema::hasTable('orders')) return;

        Schema::table('orders', function (Blueprint $table) {
            foreach (['collected_at', 'ready_at', 'preparing_at', 'accepted_at'] as $column) {
                if (Schema::hasColumn('orders', $column)) $table->dropColumn($column);
            }
        });
    }
};