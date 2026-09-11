<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration {
    public function up(): void
    {
        if (!Schema::hasTable('food_waste_records')) {
            Schema::create('food_waste_records', function (Blueprint $table) {
                $table->id();
                $table->foreignId('vendor_id')->constrained('users')->cascadeOnDelete();
                $table->foreignId('food_item_id')->nullable()->constrained('food_items')->nullOnDelete();
                $table->date('recorded_date');
                $table->unsignedInteger('prepared_quantity')->default(0);
                $table->unsignedInteger('sold_quantity')->default(0);
                $table->unsignedInteger('wasted_quantity')->default(0);
                $table->string('reason')->nullable();
                $table->timestamps();
                $table->index(['vendor_id', 'recorded_date']);
            });
        }

        if (!Schema::hasTable('demand_forecasts')) {
            Schema::create('demand_forecasts', function (Blueprint $table) {
                $table->id();
                $table->foreignId('vendor_id')->constrained('users')->cascadeOnDelete();
                $table->foreignId('food_item_id')->nullable()->constrained('food_items')->nullOnDelete();
                $table->date('forecast_date');
                $table->unsignedInteger('predicted_quantity')->default(0);
                $table->string('method')->default('moving_average');
                $table->decimal('confidence', 5, 2)->default(0);
                $table->timestamps();
                $table->unique(['vendor_id', 'food_item_id', 'forecast_date']);
            });
        }

        if (!Schema::hasTable('security_alerts')) {
            Schema::create('security_alerts', function (Blueprint $table) {
                $table->id();
                $table->foreignId('user_id')->nullable()->constrained('users')->nullOnDelete();
                $table->string('type');
                $table->string('severity')->default('MEDIUM');
                $table->text('message');
                $table->timestamp('occurred_at');
                $table->timestamp('resolved_at')->nullable();
                $table->foreignId('resolved_by')->nullable()->constrained('users')->nullOnDelete();
                $table->timestamps();
                $table->index(['severity', 'resolved_at']);
            });
        }

        if (Schema::hasTable('orders')) {
            $missing = [];
            foreach (['accepted_at','preparing_at','ready_at','collected_at','queue_position','estimated_wait_minutes'] as $column) {
                if (!Schema::hasColumn('orders', $column)) $missing[] = $column;
            }
            if ($missing) {
                Schema::table('orders', function (Blueprint $table) use ($missing) {
                    foreach ($missing as $column) {
                        if (in_array($column, ['queue_position','estimated_wait_minutes'], true)) $table->unsignedInteger($column)->nullable();
                        else $table->timestamp($column)->nullable();
                    }
                });
            }
        }
    }

    public function down(): void
    {
        Schema::dropIfExists('security_alerts');
        Schema::dropIfExists('demand_forecasts');
        Schema::dropIfExists('food_waste_records');
        if (Schema::hasTable('orders')) {
            Schema::table('orders', function (Blueprint $table) {
                foreach (['accepted_at','preparing_at','ready_at','collected_at','queue_position','estimated_wait_minutes'] as $column) {
                    if (Schema::hasColumn('orders', $column)) $table->dropColumn($column);
                }
            });
        }
    }
};
