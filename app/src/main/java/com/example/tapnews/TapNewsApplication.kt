package com.example.tapnews

import android.app.Application
import com.example.tapnews.utils.ThemeManager

class TapNewsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Inisialisasi tema saat aplikasi pertama kali dibuat
        ThemeManager.init(this)
    }
}
