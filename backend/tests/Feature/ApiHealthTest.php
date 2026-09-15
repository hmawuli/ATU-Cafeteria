<?php

namespace Tests\Feature;

use Tests\TestCase;

class ApiHealthTest extends TestCase
{
    public function test_api_health_endpoint_returns_json(): void
    {
        $response = $this->getJson('/api/health');
        $response->assertOk()->assertJsonPath('status', 'UP');
    }
}
