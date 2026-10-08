<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

/**
 * Allow image_url to hold a base64 data-URI (e.g. a photo picked on the
 * phone and stored inside the row), not just a 512-char remote URL.
 */
return new class extends Migration
{
    public function up(): void
    {
        Schema::table('menu_items', function (Blueprint $table) {
            $table->text('image_url')->nullable()->change();
        });
    }

    public function down(): void
    {
        Schema::table('menu_items', function (Blueprint $table) {
            $table->string('image_url', 512)->nullable()->change();
        });
    }
};
