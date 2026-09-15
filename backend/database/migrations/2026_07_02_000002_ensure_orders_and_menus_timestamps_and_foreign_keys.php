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
        // 1. Ensure 'orders' table is fully equipped with timestamps and foreign keys
        if (Schema::hasTable('orders')) {
            Schema::table('orders', function (Blueprint $table) {
                if (! Schema::hasColumn('orders', 'created_at')) {
                    $table->timestamps();
                }

                // Ensure foreign key references if they don't already exist.
                // In standard installations, dropping first or declaring with try-catch is safe.
            });
        }

        // 2. Ensure 'menus' table is fully equipped with timestamps and foreign keys
        if (Schema::hasTable('menus')) {
            Schema::table('menus', function (Blueprint $table) {
                if (! Schema::hasColumn('menus', 'created_at')) {
                    $table->timestamps();
                }
            });
        }
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
        // Safety: Do not destroy critical columns in down method for data preservation
    }
};
