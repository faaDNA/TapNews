package com.daffa.tapnews.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate

object ThemeManager {
    private const val PREF_NAME = "theme_pref"
    private const val KEY_THEME_MODE = "theme_mode"

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun setThemeMode(context: Context, isNightMode: Boolean) {
        getPreferences(context).edit().putBoolean(KEY_THEME_MODE, isNightMode).apply()
        applyTheme(isNightMode)
    }

    fun loadSavedTheme(context: Context) {
        val isNightMode = getPreferences(context).getBoolean(KEY_THEME_MODE, false)
        applyTheme(isNightMode)
    }

    private fun applyTheme(isNightMode: Boolean) {
        val mode = if (isNightMode) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }
}
