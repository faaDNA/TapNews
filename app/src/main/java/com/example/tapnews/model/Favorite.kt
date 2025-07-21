package com.example.tapnews.model

import com.google.firebase.Timestamp

data class Favorite(
    val uid: String = "",
    val title: String = "",
    val url: String = "",
    val imageUrl: String = "",
    val savedAt: Timestamp = Timestamp.now()
)
