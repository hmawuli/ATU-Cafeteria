package com.example.di

import android.content.Context
import com.example.data.AppDatabase
import com.example.data.CafeteriaRepository
import com.example.data.GeminiAnalyticsRepository
import com.example.data.repository.UserSessionRepository
import com.example.ui.util.CrashlyticsHelper

/**
 * Dependency Injection & Service Locator Container.
 * Centralizes management of ViewModel, Repository, Room Database,
 * Gemini Analytics, and Network service dependencies for production testability
 * and clean architecture.
 */
object ServiceLocator {

    @Volatile
    private var database: AppDatabase? = null

    @Volatile
    private var userSessionRepository: UserSessionRepository? = null

    @Volatile
    private var cafeteriaRepository: CafeteriaRepository? = null

    @Volatile
    private var geminiAnalyticsRepository: GeminiAnalyticsRepository? = null

    /**
     * Get or initialize AppDatabase singleton.
     */
    fun provideDatabase(context: Context): AppDatabase {
        return database ?: synchronized(this) {
            database ?: AppDatabase.getDatabase(context.applicationContext).also {
                database = it
            }
        }
    }

    /**
     * Get or initialize UserSessionRepository singleton.
     */
    fun provideUserSessionRepository(context: Context): UserSessionRepository {
        return userSessionRepository ?: synchronized(this) {
            userSessionRepository ?: UserSessionRepository(context.applicationContext).also {
                userSessionRepository = it
            }
        }
    }

    /**
     * Get or initialize CafeteriaRepository singleton.
     */
    fun provideCafeteriaRepository(context: Context): CafeteriaRepository {
        return cafeteriaRepository ?: synchronized(this) {
            val db = provideDatabase(context)
            cafeteriaRepository ?: CafeteriaRepository(db).also {
                cafeteriaRepository = it
            }
        }
    }

    /**
     * Get or initialize GeminiAnalyticsRepository singleton.
     */
    fun provideGeminiAnalyticsRepository(): GeminiAnalyticsRepository {
        return geminiAnalyticsRepository ?: synchronized(this) {
            geminiAnalyticsRepository ?: GeminiAnalyticsRepository().also {
                geminiAnalyticsRepository = it
            }
        }
    }

    /**
     * Initialize global application services and monitoring telemetry.
     */
    fun initAppServices(context: Context) {
        CrashlyticsHelper.init(context.applicationContext)
        provideDatabase(context)
        provideUserSessionRepository(context)
        provideCafeteriaRepository(context)
        provideGeminiAnalyticsRepository()
    }
}
