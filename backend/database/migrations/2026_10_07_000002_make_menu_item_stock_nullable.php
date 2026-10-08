<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

/**
 * Allow "untracked stock": the app lets vendors leave opening stock blank, but
 * the stock columns are NOT NULL. Make initial_stock/current_stock nullable so
 * an item without stock can be created (works on PostgreSQL and SQLite).
 */
return new class extends Migration
{
    public function up(): void
    {
        Schema::table('menu_items', function (Blueprint $table) {
            $table->integer('initial_stock')->nullable()->change();
            $table->integer('current_stock')->nullable()->change();
        });
    }

    public function down(): void
    {
        Schema::table('menu_items', function (Blueprint $table) {
            $table->integer('initial_stock')->nullable(false)->change();
            $table->integer('current_stock')->nullable(false)->change();
        });
    }
};
