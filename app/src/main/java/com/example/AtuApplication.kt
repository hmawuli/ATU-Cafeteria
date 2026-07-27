package com.example

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.util.DebugLogger
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.example.di.ServiceLocator
import com.example.ui.util.CrashlyticsHelper
import timber.log.Timber

/**
 * Custom Application class for ATU Cafeteria Mobile App.
 * Initializes Coil disk caching, Timber debug/production tree logging,
 * Firebase Crashlytics monitoring, and Hilt dependency injection framework.
 */
class AtuApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()

        // 0. Initialize FirebaseApp if not already initialized
        setupFirebaseApp()

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

        // 2. Initialize Firebase Crashlytics & Analytics & Custom Exception Handler
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
     * Custom Exception Handler to capture and report non-fatal and uncaught
     * UI thread exceptions to Firebase Crashlytics console.
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

    private fun setupFirebaseApp() {
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:108930000000:android:com.aistudio.atucafeteria")
                    .setProjectId("atu-cafeteria")
                    .setApiKey("AIzaSyATUCafeteriaDefaultKeyForBuild")
                    .setGcmSenderId("108930000000")
                    .build()
                FirebaseApp.initializeApp(this, options)
                Timber.i("FirebaseApp initialized with default fallback options.")
            }
        } catch (e: Exception) {
            Timber.e(e, "Could not initialize default FirebaseApp.")
        }
    }
}
