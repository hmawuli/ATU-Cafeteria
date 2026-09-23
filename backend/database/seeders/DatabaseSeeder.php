<?php

namespace Database\Seeders;

use Illuminate\Database\Seeder;

class DatabaseSeeder extends Seeder
{
    /**
     * Production seeding is intentionally empty.
     *
     * Demo users, sample orders and sample balances must never be created
     * accidentally on a real deployment. Automated tests use factories and
     * local development data can be loaded through explicit, environment-
     * controlled seeders when required.
     */
    public function run(): void
    {
        // Intentionally empty.
    }
}
