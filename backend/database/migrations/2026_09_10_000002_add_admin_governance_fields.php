<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::table('users', function (Blueprint $table) {
            $table->string('account_status')->default('ACTIVE')->index();
            $table->string('admin_level')->nullable()->index();
            $table->timestamp('last_login_at')->nullable()->index();
        });

        DB::table('users')->where('role', 'ADMIN')->whereNull('admin_level')->update([
            'admin_level' => 'CAFETERIA_ADMIN',
        ]);
    }

    public function down(): void
    {
        Schema::table('users', function (Blueprint $table) {
            $table->dropColumn(['account_status', 'admin_level', 'last_login_at']);
        });
    }
};
