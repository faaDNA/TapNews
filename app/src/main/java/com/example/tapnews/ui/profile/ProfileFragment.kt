package com.example.tapnews.ui.profile

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.example.tapnews.EditProfileActivity
import com.example.tapnews.LoginActivity
import com.example.tapnews.R
import com.example.tapnews.auth.AuthManager
import com.example.tapnews.databinding.FragmentProfileBinding
import com.example.tapnews.utils.ThemeManager

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val authManager = AuthManager()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Update UI with user info
        updateUserInfo()

        // Setup dark mode switch
        setupDarkModeSwitch()

        // Handle edit profile button
        binding.editProfileButton.setOnClickListener {
            startActivity(Intent(requireContext(), EditProfileActivity::class.java))
        }

        // Handle delete account button
        binding.deleteAccountButton.setOnClickListener {
            showDeleteAccountConfirmationDialog()
        }

        // Handle logout
        binding.logoutButton.setOnClickListener {
            authManager.logout()
            // Start LoginActivity and clear all activities in stack
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    private fun setupDarkModeSwitch() {
        // Set initial switch state berdasarkan tema yang tersimpan
        binding.darkModeSwitch.isChecked = ThemeManager.isDarkMode(requireContext())

        // Handle switch changes
        binding.darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            ThemeManager.setDarkMode(requireContext(), isChecked)
        }
    }

    private fun showDeleteAccountConfirmationDialog() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_delete_account)

        // Mengatur window dialog
        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setDimAmount(0.6f) // Mengatur seberapa gelap background di belakang dialog

            // Memastikan dialog menggunakan tema yang sesuai
            val attributes = attributes
            attributes.windowAnimations = android.R.style.Animation_Dialog
            this.attributes = attributes
        }

        // Set up button listeners
        dialog.findViewById<Button>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }

        dialog.findViewById<Button>(R.id.btnDelete).setOnClickListener {
            dialog.dismiss()
            deleteAccount()
        }

        dialog.show()
    }

    private fun deleteAccount() {
        val user = authManager.currentUser
        user?.delete()?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Redirect to login screen
                val intent = Intent(requireContext(), LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            } else {
                // Show error message
                android.app.AlertDialog.Builder(requireContext())
                    .setTitle("Error")
                    .setMessage("Gagal menghapus akun. Silakan coba lagi nanti.")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Update user info when returning to this fragment (e.g. after editing profile)
        updateUserInfo()
    }

    private fun updateUserInfo() {
        // Display user name and email
        authManager.currentUser?.let { user ->
            // Refresh user data to get the latest displayName
            user.reload().addOnSuccessListener {
                // Tampilkan nama pengguna dalam format "Halo, [nama]!"
                val displayName = user.displayName ?: "User"
                binding.nameTextView.text = "Halo, $displayName!"
                binding.emailTextView.text = user.email
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
