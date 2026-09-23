<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        foreach (['food_items', 'menu_items'] as $tableName) {
            if (! Schema::hasTable($tableName)) {
                continue;
            }

            Schema::table($tableName, function (Blueprint $table) use ($tableName) {
                if (! Schema::hasColumn($tableName, 'sku')) {
                    $table->string('sku', 100)->nullable();
                }
                if (! Schema::hasColumn($tableName, 'preparation_minutes')) {
                    $table->unsignedSmallInteger('preparation_minutes')->nullable();
                }
                if (! Schema::hasColumn($tableName, 'dietary_tags')) {
                    $table->json('dietary_tags')->nullable();
                }
                if (! Schema::hasColumn($tableName, 'allergen_info')) {
                    $table->string('allergen_info', 500)->nullable();
                }
                if (! Schema::hasColumn($tableName, 'is_featured')) {
                    $table->boolean('is_featured')->default(false)->index();
                }
            });

            try {
                Schema::table($tableName, function (Blueprint $table) use ($tableName) {
                    $table->unique('sku', $tableName.'_sku_unique');
                });
            } catch (\Throwable) {
                // Existing deployments may already have a SKU index.
            }
        }
    }

    public function down(): void
    {
        foreach (['food_items', 'menu_items'] as $tableName) {
            if (! Schema::hasTable($tableName)) continue;
            Schema::table($tableName, function (Blueprint $table) use ($tableName) {
                foreach (['sku','preparation_minutes','dietary_tags','allergen_info','is_featured'] as $column) {
                    if (Schema::hasColumn($tableName, $column)) {
                        $table->dropColumn($column);
                    }
                }
            });
        }
    }
};
