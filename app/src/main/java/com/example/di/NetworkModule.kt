package com.example.di

import com.example.data.GeminiApiService
import com.example.data.LaravelApiService
import com.example.data.LaravelClientManager
import com.example.data.RetrofitClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return LaravelClientManager.getOkHttpClient()
    }

    @Provides
    @Singleton
    fun provideLaravelApiService(): LaravelApiService {
        return LaravelClientManager.getService()
    }

    @Provides
    @Singleton
    fun provideGeminiApiService(): GeminiApiService {
        return RetrofitClient.geminiService
    }
}
