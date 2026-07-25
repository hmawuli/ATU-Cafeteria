package com.example.ui.util

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import timber.log.Timber
import java.util.Locale

/**
 * Locale Manager utility object that allows users to dynamically switch
 * between English ("en") and French ("fr") in Settings, updating UI text using strings.xml.
 */
object LocaleManager {

    private const val PREF_NAME = "atu_locale_settings"
    private const val KEY_LANGUAGE = "key_app_language"
    const val LANGUAGE_ENGLISH = "en"
    const val LANGUAGE_FRENCH = "fr"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Get the currently saved language code ("en" default or "fr").
     */
    fun getSavedLanguage(context: Context): String {
        return getPrefs(context).getString(KEY_LANGUAGE, LANGUAGE_ENGLISH) ?: LANGUAGE_ENGLISH
    }

    /**
     * Set the application language code ("en" or "fr") and update locale configuration.
     */
    fun setLanguage(context: Context, languageCode: String) {
        val validLanguage = if (languageCode == LANGUAGE_FRENCH) LANGUAGE_FRENCH else LANGUAGE_ENGLISH
        getPrefs(context).edit().putString(KEY_LANGUAGE, validLanguage).apply()

        // Log analytics event
        FirebaseAnalyticsHelper.logLanguageChanged(validLanguage)

        // Update resources configuration
        updateResourcesLocale(context, validLanguage)

        Timber.i("App locale updated to: $validLanguage")
    }

    /**
     * Apply saved locale configuration to context for Activity wrap.
     */
    fun applyLocale(context: Context): Context {
        val language = getSavedLanguage(context)
        val locale = Locale(language)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(LocaleList(locale))
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
        }
        return context.createConfigurationContext(config)
    }

    private fun updateResourcesLocale(context: Context, languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val resources = context.resources
        val config = Configuration(resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(LocaleList(locale))
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
        }

        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    /**
     * Helper to recreate activity if necessary for visual refresh.
     */
    fun refreshActivity(activity: Activity?) {
        activity?.recreate()
    }
}
