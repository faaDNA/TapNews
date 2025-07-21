package com.example.tapnews.ui.favorites

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.tapnews.model.FavoriteArticle
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class FavoriteViewModel : ViewModel() {
    private val _favorites = MutableLiveData<List<FavoriteArticle>>()
    val favorites: LiveData<List<FavoriteArticle>> = _favorites

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun loadFavorites() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _error.value = "Please login first"
            return
        }

        _isLoading.value = true
        firestore.collection("favorites")
            .whereEqualTo("uid", currentUser.uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    _error.value = "Error loading favorites: ${e.message}"
                    return@addSnapshotListener
                }

                val favoritesList = snapshot?.documents?.map { doc ->
                    FavoriteArticle(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        description = doc.getString("description"),
                        url = doc.getString("url") ?: "",
                        urlToImage = doc.getString("imageUrl"),
                        publishedAt = doc.getString("publishedAt") ?: ""
                    )
                } ?: emptyList()

                // Sort the list in memory instead
                val sortedList = favoritesList.sortedByDescending {
                    it.publishedAt
                }
                _favorites.value = sortedList
                _isLoading.value = false
            }
    }

    fun removeFromFavorites(favoriteId: String) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _error.value = "Please login first"
            return
        }

        firestore.collection("favorites")
            .document(favoriteId)
            .delete()
            .addOnSuccessListener {
                _error.value = "Article removed from favorites"
            }
            .addOnFailureListener { e ->
                _error.value = "Failed to remove: ${e.message}"
            }
    }
}
