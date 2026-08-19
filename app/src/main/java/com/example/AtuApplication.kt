package com.example

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.util.DebugLogger
import com.example.di.ServiceLocator
import com.example.ui.util.CrashlyticsHelper
import timber.log.Timber

/**
 * Custom Application class for ATU Cafeteria Mobile App.
 * Initializes Coil disk caching, Timber debug/production tree logging,
 * telemetry monitoring, and Hilt dependency injection framework.
 */
class AtuApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()

        // 1. Initialize Timber Logging Tree
        if (BuildConfig.DEBUG) {
            Timber.plant(object : Timber.DebugTree() {
                override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                    // Detailed network & database debug logging
                    val customTag = "ATU_LOG_${tag ?: "General"}"
                    super.log(priority, customTag, message, t)
                }
            })
            Timber.d("Timber debug logging tree initialized for development.")
        } else {
            Timber.plant(object : Timber.Tree() {
                override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                    // Silenced in production for security; report errors to Crashlytics
                    if (priority >= android.util.Log.ERROR && t != null) {
                        CrashlyticsHelper.recordException(t, "Production Error: $message")
                    }
                }
            })
        }

        // 2. Initialize Telemetry & Custom Exception Handler
        CrashlyticsHelper.init(this)
        com.example.ui.util.FirebaseAnalyticsHelper.init(this)
        setupCustomExceptionHandler()

        // 3. Initialize Core App Services (Database, Session, Network)
        ServiceLocator.initAppServices(this)
    }

    /**
     * Configure Coil's disk cache and memory cache in application initialization
     * to optimize loading times for vendor/food images and reduce mobile data consumption.
     */
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25) // 25% of app memory for image cache
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("atu_image_disk_cache"))
                    .maxSizeBytes(100 * 1024 * 1024) // 100 MB persistent disk cache
                    .build()
            }
            .apply {
                if (BuildConfig.DEBUG) {
                    logger(DebugLogger())
                }
            }
            .crossfade(true)
            .build()
    }

    /**
     * Handle system memory trim requests to comply with modern Android Q+ memory management.
     */
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        try {
            if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE) {
                coil.Coil.imageLoader(this).memoryCache?.clear()
            }
        } catch (_: Exception) {}
    }

    override fun onLowMemory() {
        super.onLowMemory()
        try {
            coil.Coil.imageLoader(this).memoryCache?.clear()
        } catch (_: Exception) {}
    }

    /**
     * Custom Exception Handler to capture and report non-fatal and uncaught
     * UI thread exceptions to console.
     */
    private fun setupCustomExceptionHandler() {
        val originalHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Timber.e(throwable, "Uncaught Exception captured on thread: ${thread.name}")
            CrashlyticsHelper.recordException(
                throwable,
                "Uncaught Exception on thread '${thread.name}': ${throwable.localizedMessage}"
            )
            originalHandler?.uncaughtException(thread, throwable)
        }
    }
}
