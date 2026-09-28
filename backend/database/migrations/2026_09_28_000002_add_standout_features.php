<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    /**
     * Stand-out features:
     *  - orders.scheduled_pickup_at   → scheduled pre-ordering
     *  - vendors.campus               → multi-campus / franchise-ready
     */
    public function up(): void
    {
        Schema::table('orders', function (Blueprint $table) {
            $table->timestamp('scheduled_pickup_at')->nullable()->after('estimated_pickup_time');
        });

        Schema::table('vendors', function (Blueprint $table) {
            $table->string('campus')->default('Accra Technical University')->after('location_within_campus');
            $table->index('campus');
        });
    }

    public function down(): void
    {
        Schema::table('orders', function (Blueprint $table) {
            $table->dropColumn('scheduled_pickup_at');
        });

        Schema::table('vendors', function (Blueprint $table) {
            $table->dropIndex(['campus']);
            $table->dropColumn('campus');
        });
    }
};
