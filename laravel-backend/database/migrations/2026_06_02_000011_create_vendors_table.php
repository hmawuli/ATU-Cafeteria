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
        Schema::create('vendors', function (Blueprint $table) {
            $table->id();
            $table->unsignedBigInteger('user_id')->nullable()->unique();
            $table->string('name');
            $table->string('location')->nullable();
            $table->string('contact_info')->nullable();
            $table->string('operational_status')->default('active'); // active, inactive, suspended
            $table->string('store_name')->nullable();
            $table->string('location_within_campus')->nullable();
            $table->string('contact_email')->nullable();
            $table->string('operational_hours')->nullable();
            $table->timestamps();
            $table->softDeletes();

            $table->foreign('user_id')->references('id')->on('users')->onDelete('cascade');
        });
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
        Schema::dropIfExists('vendors');
    }
};
