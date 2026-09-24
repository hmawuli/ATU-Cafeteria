<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        if (Schema::hasTable('demand_forecasts') && Schema::hasColumn('demand_forecasts', 'menu_item_id')) {
            $indexes = collect(Schema::getIndexes('demand_forecasts'))->pluck('name')->all();
            if (! in_array('demand_forecasts_vendor_menu_date_unique', $indexes, true)) {
                Schema::table('demand_forecasts', function (Blueprint $table) {
                    $table->unique(
                        ['vendor_id', 'menu_item_id', 'forecast_date'],
                        'demand_forecasts_vendor_menu_date_unique'
                    );
                });
            }
        }
    }

    public function down(): void
    {
        if (Schema::hasTable('demand_forecasts')) {
            $indexes = collect(Schema::getIndexes('demand_forecasts'))->pluck('name')->all();
            if (in_array('demand_forecasts_vendor_menu_date_unique', $indexes, true)) {
                Schema::table('demand_forecasts', function (Blueprint $table) {
                    $table->dropUnique('demand_forecasts_vendor_menu_date_unique');
                });
            }
        }
    }
};