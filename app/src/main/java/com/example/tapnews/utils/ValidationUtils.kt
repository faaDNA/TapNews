package com.example.tapnews.utils

object ValidationUtils {
    fun isValidEmail(email: String): Boolean {
        // Regex sederhana agar bisa di-test di unit test JVM
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$".toRegex()
        return email.isNotEmpty() && email.matches(emailRegex)
    }

    fun isValidPassword(password: String): Boolean {
        return password.length >= 6
    }
}
