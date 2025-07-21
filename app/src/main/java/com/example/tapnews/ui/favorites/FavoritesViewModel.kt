package com.example.tapnews.ui.favorites

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.tapnews.model.Favorite
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class FavoritesViewModel : ViewModel() {
    private val _favorites = MutableLiveData<List<Favorite>>()
    val favorites: LiveData<List<Favorite>> = _favorites

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    init {
        loadFavorites()
    }

    fun loadFavorites() {
        val currentUser = auth.currentUser ?: return

        firestore.collection("favorites")
            .whereEqualTo("uid", currentUser.uid)
            .orderBy("savedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    return@addSnapshotListener
                }

                val favoritesList = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Favorite::class.java)
                } ?: emptyList()

                _favorites.value = favoritesList
            }
    }

    fun removeFavorite(favorite: Favorite) {
        val currentUser = auth.currentUser ?: return

        firestore.collection("favorites")
            .whereEqualTo("uid", currentUser.uid)
            .whereEqualTo("url", favorite.url)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    document.reference.delete()
                }
            }
    }
}
