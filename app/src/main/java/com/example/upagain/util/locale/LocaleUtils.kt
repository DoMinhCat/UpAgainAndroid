package com.example.upagain.util.locale

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

object LocaleManager {
    private val languageMap = mapOf(
        "English" to "en",
        "Français" to "fr"
    )

    /**
     * Updates the application-wide locale via AppCompatDelegate.
     * AndroidX automatically handles storage, context updating, and activity redraws.
     */
    fun setLocaleByDisplayName(displayName: String) {
        val languageTag = languageMap[displayName] ?: "en"
        val appLocale = LocaleListCompat.forLanguageTags(languageTag)
        AppCompatDelegate.setApplicationLocales(appLocale)
    }

    /**
     * Finds the human-readable language name for the currently active locale.
     * Useful for pre-filling dropdown menus on layout rendering.
     */
    fun getCurrentLanguageDisplayName(): String {
        val currentLocaleTag = AppCompatDelegate.getApplicationLocales()[0]?.language ?: "en"
        return languageMap.entries.find { it.value == currentLocaleTag }?.key ?: "English"
    }
}