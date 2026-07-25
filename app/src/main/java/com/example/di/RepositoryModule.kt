package com.example.di

import android.content.Context
import com.example.data.AppDatabase
import com.example.data.CafeteriaRepository
import com.example.data.GeminiAnalyticsRepository
import com.example.data.repository.UserSessionRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideCafeteriaRepository(database: AppDatabase): CafeteriaRepository {
        return CafeteriaRepository(database)
    }

    @Provides
    @Singleton
    fun provideUserSessionRepository(@ApplicationContext context: Context): UserSessionRepository {
        return UserSessionRepository(context)
    }

    @Provides
    @Singleton
    fun provideGeminiAnalyticsRepository(): GeminiAnalyticsRepository {
        return GeminiAnalyticsRepository()
    }
}
