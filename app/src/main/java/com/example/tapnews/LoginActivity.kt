package com.example.tapnews

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.tapnews.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.example.tapnews.utils.ValidationUtils

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Check if user is already signed in
        if (auth.currentUser != null) {
            navigateToNews()
        }

        binding.loginButton.setOnClickListener {
            // Reset error messages
            binding.emailInputLayout.error = null
            binding.passwordInputLayout.error = null

            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()

            // Validate input fields
            when {
                email.isEmpty() -> {
                    binding.emailInputLayout.error = "Email tidak boleh kosong"
                    return@setOnClickListener
                }
                !ValidationUtils.isValidEmail(email) -> {
                    binding.emailInputLayout.error = "Format email tidak valid"
                    return@setOnClickListener
                }
                password.isEmpty() -> {
                    binding.passwordInputLayout.error = "Password tidak boleh kosong"
                    return@setOnClickListener
                }
                !ValidationUtils.isValidPassword(password) -> {
                    binding.passwordInputLayout.error = "Password minimal 6 karakter"
                    return@setOnClickListener
                }
            }

            // Show loading and disable button
            binding.loginButton.isEnabled = false
            binding.progressBar.visibility = View.VISIBLE

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    binding.progressBar.visibility = View.GONE

                    if (task.isSuccessful) {
                        // Sign in success
                        navigateToNews()
                    } else {
                        // Handle specific authentication errors
                        when (task.exception) {
                            is FirebaseAuthInvalidUserException -> {
                                binding.emailInputLayout.error = "Akun tidak ditemukan"
                            }
                            is FirebaseAuthInvalidCredentialsException -> {
                                binding.passwordInputLayout.error = "Password salah"
                            }
                            else -> {
                                Toast.makeText(this, "Login gagal. Periksa koneksi internet Anda", Toast.LENGTH_LONG).show()
                            }
                        }
                        binding.loginButton.isEnabled = true
                    }
                }
        }

        binding.registerTextView.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun navigateToNews() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}

