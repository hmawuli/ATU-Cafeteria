<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        if (Schema::hasTable('promotions') && ! Schema::hasColumn('promotions', 'vendor_id')) {
            Schema::table('promotions', function (Blueprint $table) {
                $table->foreignId('vendor_id')->nullable()->after('id')
                    ->constrained('users')->nullOnDelete();
                $table->index(['vendor_id', 'is_active']);
            });
        }
    }

    public function down(): void
    {
        if (Schema::hasTable('promotions') && Schema::hasColumn('promotions', 'vendor_id')) {
            Schema::table('promotions', function (Blueprint $table) {
                $table->dropForeign(['vendor_id']);
                $table->dropIndex(['vendor_id', 'is_active']);
                $table->dropColumn('vendor_id');
            });
        }
    }
};
