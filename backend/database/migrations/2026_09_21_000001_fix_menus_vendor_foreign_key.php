<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

/**
 * The rest of the schema (food_items, menu_items, orders, feedback, vendor_menus,
 * vendor_menu_availabilities, ...) treats `vendor_id` as a reference to the
 * `users` table (the VENDOR role account). Only the legacy `menus` pivot table
 * pointed at `vendors.id`, while MenuController::store resolves the vendor from
 * the authenticated user's id — so menu creation failed under SQLite FK
 * enforcement. Repoint the foreign key to `users` for consistency.
 */
return new class extends Migration
{
    public function up(): void
    {
        Schema::table('menus', function (Blueprint $table) {
            $table->dropForeign(['vendor_id']);
            $table->foreign('vendor_id')->references('id')->on('users')->onDelete('cascade');
        });
    }

    public function down(): void
    {
        Schema::table('menus', function (Blueprint $table) {
            $table->dropForeign(['vendor_id']);
            $table->foreign('vendor_id')->references('id')->on('vendors')->onDelete('cascade');
        });
    }
};
