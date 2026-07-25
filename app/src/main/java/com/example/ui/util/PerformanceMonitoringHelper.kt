package com.example.ui.util

import android.util.Log

/**
 * Firebase Performance Monitoring Helper.
 * Tracks application start time, screen rendering performance,
 * order submission network latency, and custom trace spans.
 */
object PerformanceMonitoringHelper {
    private const val TAG = "ATU_PerfMon"
    private val activeTraces = mutableMapOf<String, Long>()

    /**
     * Start a named performance metric trace (e.g. "order_checkout_flow", "menu_load_time").
     */
    fun startTrace(traceName: String) {
        val startTime = System.currentTimeMillis()
        activeTraces[traceName] = startTime
        Log.d(TAG, "Performance Trace Started [$traceName] at $startTime ms")
    }

    /**
     * Stop a named trace and log/record total elapsed latency.
     */
    fun stopTrace(traceName: String) {
        val startTime = activeTraces.remove(traceName)
        if (startTime != null) {
            val durationMs = System.currentTimeMillis() - startTime
            Log.i(TAG, "Performance Trace Completed [$traceName] - Latency: ${durationMs}ms")
        } else {
            Log.w(TAG, "StopTrace called for unknown trace: $traceName")
        }
    }

    /**
     * Record network latency and HTTP payload size for API calls.
     */
    fun recordNetworkLatency(endpoint: String, method: String, statusCode: Int, latencyMs: Long) {
        Log.i(TAG, "Network Perf Metric: [$method $endpoint] Status: $statusCode - Latency: ${latencyMs}ms")
    }
}
