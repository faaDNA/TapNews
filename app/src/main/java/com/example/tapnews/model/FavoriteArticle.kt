package com.example.tapnews.model

data class FavoriteArticle(
    val id: String,
    val title: String,
    val description: String?,
    val url: String,
    val urlToImage: String?,
    val publishedAt: String
)
