package com.example.tapnews

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.tapnews.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.example.tapnews.utils.ValidationUtils

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        binding.registerButton.setOnClickListener {
            val name = binding.nameEditText.text.toString().trim()
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()
            val confirmPassword = binding.confirmPasswordEditText.text.toString().trim()

            // Validate input fields
            when {
                name.isEmpty() -> {
                    Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                email.isEmpty() -> {
                    Toast.makeText(this, "Email cannot be empty", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                !ValidationUtils.isValidEmail(email) -> {
                    Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                password.isEmpty() -> {
                    Toast.makeText(this, "Password cannot be empty", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                !ValidationUtils.isValidPassword(password) -> {
                    Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                confirmPassword.isEmpty() -> {
                    Toast.makeText(this, "Confirm password cannot be empty", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                password != confirmPassword -> {
                    Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            // Show loading and disable button
            binding.registerButton.isEnabled = false
            binding.progressBar.visibility = View.VISIBLE

            // Create user with email and password
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    binding.progressBar.visibility = View.GONE

                    if (task.isSuccessful) {
                        // Update user profile with name
                        val user = auth.currentUser
                        user?.let {
                            // Membuat request untuk mengupdate profile user dengan nama yang dimasukkan
                            val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                                .setDisplayName(name)
                                .build()

                            // Mengupdate profile user dan menunggu proses selesai sebelum navigasi
                            it.updateProfile(profileUpdates)
                                .addOnCompleteListener { profileTask ->
                                    if (profileTask.isSuccessful) {
                                        // Reload user data untuk memastikan perubahan telah diterapkan
                                        user.reload().addOnCompleteListener { reloadTask ->
                                            if (reloadTask.isSuccessful) {
                                                // Log untuk debug
                                                android.util.Log.d("RegisterActivity", "User name saved: ${user.displayName}")
                                                Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                android.util.Log.e("RegisterActivity", "Failed to reload user data", reloadTask.exception)
                                                Toast.makeText(this, "Registration successful but profile may not be updated", Toast.LENGTH_SHORT).show()
                                            }
                                            // Go to MainActivity (news screen)
                                            startActivity(Intent(this, MainActivity::class.java))
                                            finishAffinity() // Close all activities in the stack
                                        }
                                    } else {
                                        android.util.Log.e("RegisterActivity", "Failed to save user name", profileTask.exception)
                                        Toast.makeText(this, "Failed to save user name: ${profileTask.exception?.message}", Toast.LENGTH_SHORT).show()
                                        // Tetap navigasi ke MainActivity meskipun gagal mengupdate nama
                                        startActivity(Intent(this, MainActivity::class.java))
                                        finishAffinity()
                                    }
                                }
                        }
                    } else {
                        // Handle specific registration errors
                        val errorMessage = when (task.exception) {
                            is FirebaseAuthUserCollisionException ->
                                "An account with this email already exists"
                            else -> "Registration failed: ${task.exception?.message}"
                        }
                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                        binding.registerButton.isEnabled = true
                    }
                }
        }

        binding.loginTextView.setOnClickListener {
            // Go back to login screen
            finish()
        }
    }
}
