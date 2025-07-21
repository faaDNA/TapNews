package com.example.tapnews.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate

object ThemeManager {
    private const val THEME_PREFERENCES = "theme_preferences"
    private const val KEY_IS_DARK_MODE = "is_dark_mode"

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(THEME_PREFERENCES, Context.MODE_PRIVATE)
    }

    fun setDarkMode(context: Context, isDarkMode: Boolean) {
        getPreferences(context).edit().apply {
            putBoolean(KEY_IS_DARK_MODE, isDarkMode)
            apply()
        }
        applyTheme(isDarkMode)
    }

    fun isDarkMode(context: Context): Boolean {
        return getPreferences(context).getBoolean(KEY_IS_DARK_MODE, false)
    }

    fun applyTheme(isDarkMode: Boolean) {
        val mode = if (isDarkMode) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    fun init(context: Context) {
        val isDarkMode = isDarkMode(context)
        applyTheme(isDarkMode)
    }
}
