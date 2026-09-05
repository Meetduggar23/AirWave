package com.example.airwave.util

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LanguageHelper {
    fun setLocale(context: Context, languageCode: String): Context {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        // Keep the layout direction in sync with the locale so future RTL
        // languages (Arabic, Hebrew, ...) render correctly.
        config.setLayoutDirection(locale)
        return context.createConfigurationContext(config)
    }

    /**
     * Display names are always shown in their native script so the language
     * selector is readable even while the app is running in another language.
     */
    fun getLanguageName(code: String): String {
        return when (code) {
            "hi" -> "हिंदी"
            "te" -> "తెలుగు"
            "mr" -> "मराठी"
            "bn" -> "বাংলা"
            else -> "English"
        }
    }

    fun getAvailableLanguages(): List<Pair<String, String>> {
        return listOf(
            "en" to "English",
            "hi" to "हिंदी",
            "te" to "తెలుగు",
            "mr" to "मराठी",
            "bn" to "বাংলা"
        )
    }
}
