package com.example.tapnews

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.tapnews.databinding.ActivityEditProfileBinding
import com.example.tapnews.utils.ValidationUtils
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest

class EditProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditProfileBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Set current name to the field
        auth.currentUser?.let { user ->
            binding.nameEditText.setText(user.displayName)
        }

        // Back button - cancel and go back
        binding.backButton.setOnClickListener {
            finish()
        }

        // Save button - update profile and go back
        binding.saveButton.setOnClickListener {
            updateProfile()
        }
    }

    private fun updateProfile() {
        val currentUser = auth.currentUser ?: run {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val newName = binding.nameEditText.text.toString().trim()
        val currentPassword = binding.currentPasswordEditText.text.toString().trim()
        val newPassword = binding.newPasswordEditText.text.toString().trim()
        val confirmNewPassword = binding.confirmNewPasswordEditText.text.toString().trim()

        // Validate name
        if (newName.isEmpty()) {
            Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        // Check if password update is requested
        val isPasswordUpdateRequested = newPassword.isNotEmpty() || confirmNewPassword.isNotEmpty()

        // If password update is requested, validate current password and new password
        if (isPasswordUpdateRequested) {
            if (currentPassword.isEmpty()) {
                Toast.makeText(this, "Current password is required to change password", Toast.LENGTH_SHORT).show()
                return
            }

            if (newPassword.isEmpty()) {
                Toast.makeText(this, "New password cannot be empty", Toast.LENGTH_SHORT).show()
                return
            }

            if (!ValidationUtils.isValidPassword(newPassword)) {
                Toast.makeText(this, "New password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return
            }

            if (newPassword != confirmNewPassword) {
                Toast.makeText(this, "New passwords do not match", Toast.LENGTH_SHORT).show()
                return
            }
        }

        // Show loading
        binding.progressBar.visibility = View.VISIBLE
        binding.saveButton.isEnabled = false
        binding.backButton.isEnabled = false

        // Update profile and/or password
        updateProfileData(
            currentUser = currentUser,
            newName = newName,
            currentPassword = currentPassword,
            newPassword = newPassword,
            isPasswordUpdateRequested = isPasswordUpdateRequested
        )
    }

    private fun updateProfileData(
        currentUser: com.google.firebase.auth.FirebaseUser,
        newName: String,
        currentPassword: String,
        newPassword: String,
        isPasswordUpdateRequested: Boolean
    ) {
        // First update the user name
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(newName)
            .build()

        currentUser.updateProfile(profileUpdates)
            .addOnCompleteListener { nameUpdateTask ->
                if (nameUpdateTask.isSuccessful) {
                    Log.d(TAG, "User name updated successfully")

                    // If password update is requested
                    if (isPasswordUpdateRequested) {
                        // Reauthenticate user
                        val credential = EmailAuthProvider.getCredential(
                            currentUser.email ?: "", currentPassword
                        )

                        currentUser.reauthenticate(credential)
                            .addOnCompleteListener { reauthTask ->
                                if (reauthTask.isSuccessful) {
                                    // Update password
                                    currentUser.updatePassword(newPassword)
                                        .addOnCompleteListener { passwordUpdateTask ->
                                            if (passwordUpdateTask.isSuccessful) {
                                                Log.d(TAG, "Password updated successfully")
                                                Toast.makeText(
                                                    this,
                                                    "Profile and password updated successfully",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                // Reload user data and finish
                                                reloadUserAndFinish(currentUser)
                                            } else {
                                                Log.e(TAG, "Error updating password", passwordUpdateTask.exception)
                                                Toast.makeText(
                                                    this,
                                                    "Failed to update password: ${passwordUpdateTask.exception?.message}",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                hideLoading()
                                            }
                                        }
                                } else {
                                    Log.e(TAG, "Error re-authenticating", reauthTask.exception)
                                    Toast.makeText(
                                        this,
                                        "Current password is incorrect",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    hideLoading()
                                }
                            }
                    } else {
                        // Only name was updated
                        Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                        // Reload user data and finish
                        reloadUserAndFinish(currentUser)
                    }
                } else {
                    Log.e(TAG, "Error updating user name", nameUpdateTask.exception)
                    Toast.makeText(
                        this,
                        "Failed to update profile: ${nameUpdateTask.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    hideLoading()
                }
            }
    }

    private fun reloadUserAndFinish(user: com.google.firebase.auth.FirebaseUser) {
        user.reload().addOnCompleteListener { reloadTask ->
            if (reloadTask.isSuccessful) {
                Log.d(TAG, "User data reloaded successfully")
            } else {
                Log.e(TAG, "Failed to reload user data", reloadTask.exception)
            }
            // Finish activity and return to profile fragment
            finish()
        }
    }

    private fun hideLoading() {
        binding.progressBar.visibility = View.GONE
        binding.saveButton.isEnabled = true
        binding.backButton.isEnabled = true
    }

    companion object {
        private const val TAG = "EditProfileActivity"
    }
}
