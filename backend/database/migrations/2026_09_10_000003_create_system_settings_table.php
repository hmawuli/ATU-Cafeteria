<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('system_settings', function (Blueprint $table) {
            $table->id();
            $table->string('key')->unique();
            $table->text('value')->nullable();
            $table->string('type')->default('string');
            $table->text('description')->nullable();
            $table->timestamps();
        });

        DB::table('system_settings')->insert([
            ['key' => 'cafeteria_open_time', 'value' => '07:00', 'type' => 'string', 'description' => 'Daily cafeteria opening time.', 'created_at' => now(), 'updated_at' => now()],
            ['key' => 'cafeteria_close_time', 'value' => '20:00', 'type' => 'string', 'description' => 'Daily cafeteria closing time.', 'created_at' => now(), 'updated_at' => now()],
            ['key' => 'order_cutoff_minutes', 'value' => '15', 'type' => 'integer', 'description' => 'Minutes before closing when new orders stop.', 'created_at' => now(), 'updated_at' => now()],
            ['key' => 'queue_average_prep_minutes', 'value' => '15', 'type' => 'integer', 'description' => 'Fallback preparation time used by smart queue estimation.', 'created_at' => now(), 'updated_at' => now()],
            ['key' => 'maintenance_mode', 'value' => 'false', 'type' => 'boolean', 'description' => 'Temporarily block non-administrative operations.', 'created_at' => now(), 'updated_at' => now()],
        ]);
    }

    public function down(): void
    {
        Schema::dropIfExists('system_settings');
    }
};
