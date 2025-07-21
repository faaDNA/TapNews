package com.example.tapnews.api

import com.example.tapnews.model.NewsResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface NewsApiService {
    @GET("v2/top-headlines")
    suspend fun getTopHeadlines(
        @Query("apiKey") apiKey: String = ApiConfig.API_KEY,
        @Query("country") country: String = "id",
        @Query("pageSize") pageSize: Int = 20
    ): NewsResponse

    @GET("v2/everything")
    suspend fun getEverything(
        @Query("apiKey") apiKey: String = ApiConfig.API_KEY,
        @Query("q") query: String = "indonesia",
        @Query("sortBy") sortBy: String = "publishedAt",
        @Query("pageSize") pageSize: Int = 20,
        @Query("language") language: String = "id"
    ): NewsResponse

    @GET("v2/everything")
    suspend fun searchNews(
        @Query("q") query: String,
        @Query("apiKey") apiKey: String = ApiConfig.API_KEY,
        @Query("sortBy") sortBy: String = "publishedAt",
        @Query("pageSize") pageSize: Int = 20
    ): NewsResponse
}
