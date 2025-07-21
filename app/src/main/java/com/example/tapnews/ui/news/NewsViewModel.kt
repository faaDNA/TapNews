package com.example.tapnews.ui.news

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tapnews.api.ApiConfig
import com.example.tapnews.api.NewsApiService
import com.example.tapnews.model.Article
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.math.min

class NewsViewModel : ViewModel() {
    private val _news = MutableLiveData<List<Article>>()
    val news: LiveData<List<Article>> = _news

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _message = MutableLiveData<String>()
    val message: LiveData<String> = _message

    // Variabel untuk pagination
    private val _allNewsArticles = mutableListOf<Article>()
    private var currentPage = 1
    private val pageSize = 10
    private var isLastPage = false

    // LiveData untuk memberitahu fragment apakah ada data baru yang ditambahkan
    private val _newDataAdded = MutableLiveData<Boolean>()
    val newDataAdded: LiveData<Boolean> = _newDataAdded

    private val newsApiService: NewsApiService
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    init {
        val loggingInterceptor = HttpLoggingInterceptor { message ->
            Log.d("NewsAPI", message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        newsApiService = retrofit.create(NewsApiService::class.java)
    }

    fun loadNews() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = "" // Reset error message

            try {
                // First try to get top headlines
                Log.d("NewsAPI", "Trying to fetch top headlines...")
                var response = newsApiService.getTopHeadlines()

                // If no articles found in top headlines, try 'everything' endpoint
                if (response.articles.isEmpty()) {
                    Log.d("NewsAPI", "No top headlines found, trying everything endpoint...")
                    response = newsApiService.getEverything()
                }

                if (response.articles.isNotEmpty()) {
                    Log.d("NewsAPI", "Successfully fetched ${response.articles.size} articles")
                    _news.value = response.articles
                } else {
                    Log.e("NewsAPI", "No articles found from both endpoints")
                    _error.value = "No news available at the moment"
                    _news.value = emptyList()
                }
            } catch (e: HttpException) {
                Log.e("NewsAPI", "HTTP Error: ${e.code()}", e)
                val errorBody = e.response()?.errorBody()?.string()
                _error.value = when (e.code()) {
                    429 -> "Too many requests. Please try again later."
                    401 -> "API authorization failed. Please check the configuration."
                    404 -> "Could not find news at this time. Please try again later."
                    else -> "Network error: ${errorBody ?: e.message()}"
                }
                _news.value = emptyList()
            } catch (e: IOException) {
                Log.e("NewsAPI", "Network Error", e)
                _error.value = "Could not connect to the server. Please check your internet connection."
                _news.value = emptyList()
            } catch (e: Exception) {
                Log.e("NewsAPI", "Unexpected Error", e)
                _error.value = "An unexpected error occurred: ${e.message}"
                _news.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveToFavorites(article: Article) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _error.value = "Please login to save favorites"
            return
        }

        // Check if article already exists in favorites
        firestore.collection("favorites")
            .whereEqualTo("uid", currentUser.uid)
            .whereEqualTo("url", article.url)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    // Article doesn't exist in favorites, save it
                    val favorite = hashMapOf(
                        "uid" to currentUser.uid,
                        "title" to article.title,
                        "url" to article.url,
                        "imageUrl" to (article.urlToImage ?: ""),
                        "description" to (article.description ?: ""),
                        "publishedAt" to article.publishedAt,
                        "savedAt" to com.google.firebase.Timestamp.now()
                    )

                    firestore.collection("favorites")
                        .add(favorite)
                        .addOnSuccessListener {
                            _message.value = "Article saved to favorites"
                        }
                        .addOnFailureListener { e ->
                            _error.value = "Failed to save: ${e.localizedMessage}"
                        }
                } else {
                    _message.value = "Article is already in favorites"
                }
            }
            .addOnFailureListener { e ->
                _error.value = "Error checking favorites: ${e.localizedMessage}"
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
                _message.value = "Article removed from favorites"
            }
            .addOnFailureListener { e ->
                _error.value = "Failed to remove: ${e.localizedMessage}"
            }
    }

    fun searchNews(query: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = "" // Reset error message

            try {
                Log.d("NewsAPI", "Searching for news with query: $query")

                // First check if we already have news loaded
                val currentNews = _news.value
                if (!currentNews.isNullOrEmpty()) {
                    // Filter locally for better performance
                    val filteredNews = currentNews.filter { article ->
                        article.title.contains(query, ignoreCase = true) ||
                        (article.description?.contains(query, ignoreCase = true) ?: false)
                    }

                    if (filteredNews.isNotEmpty()) {
                        Log.d("NewsAPI", "Found ${filteredNews.size} articles locally matching query")
                        _news.value = filteredNews
                        _isLoading.value = false
                        return@launch
                    }
                }

                // If local filtering didn't produce results or we have no cached news,
                // try to search via API
                try {
                    val response = newsApiService.searchNews(query)
                    if (response.articles.isNotEmpty()) {
                        Log.d("NewsAPI", "Successfully fetched ${response.articles.size} articles from search API")
                        _news.value = response.articles
                    } else {
                        Log.e("NewsAPI", "No articles found matching query: $query")
                        _error.value = "No news found matching your search"
                        // Keep the current news list visible
                    }
                } catch (e: Exception) {
                    Log.e("NewsAPI", "Error searching via API, falling back to local filtering", e)
                    // If API search fails, fall back to searching the current list
                    if (!currentNews.isNullOrEmpty()) {
                        val filteredNews = currentNews.filter { article ->
                            article.title.contains(query, ignoreCase = true) ||
                            (article.description?.contains(query, ignoreCase = true) ?: false)
                        }

                        if (filteredNews.isNotEmpty()) {
                            _news.value = filteredNews
                        } else {
                            _error.value = "No news found matching your search"
                        }
                    } else {
                        _error.value = "No news available to search"
                    }
                }
            } catch (e: Exception) {
                Log.e("NewsAPI", "Error during search", e)
                _error.value = "Error searching news: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadRandomNews(loadFirstPage: Boolean = false) {
        viewModelScope.launch {
            // Jika ini halaman pertama atau mengambil ulang, reset pagination state
            if (loadFirstPage) {
                currentPage = 1
                _allNewsArticles.clear()
                isLastPage = false
            }

            // Jika sudah di halaman terakhir, tidak perlu memuat lagi
            if (isLastPage) {
                _isLoading.value = false
                return@launch
            }

            _isLoading.value = true
            _error.value = "" // Reset error message

            try {
                // Tentukan page size berdasarkan kebutuhan
                val apiPageSize = 20 // Lebih banyak daripada pageSize untuk caching

                // First try to get top headlines for Indonesia
                Log.d("NewsAPI", "Trying to fetch top headlines for Indonesia... Page: $currentPage")
                val headlinesResponse = newsApiService.getTopHeadlines(
                    apiKey = ApiConfig.API_KEY,
                    country = "id",
                    pageSize = apiPageSize
                )

                if (headlinesResponse.articles.isNotEmpty()) {
                    val shuffledArticles = headlinesResponse.articles.shuffled()
                    Log.d("NewsAPI", "Successfully fetched ${shuffledArticles.size} Indonesian headlines")

                    // Tambahkan artikel ke list utama
                    _allNewsArticles.addAll(shuffledArticles)

                    // Hitung dari mana hingga mana artikel yang akan ditampilkan
                    val startIndex = 0
                    val endIndex = min(_allNewsArticles.size, currentPage * pageSize)

                    // Update LiveData dengan artikel untuk halaman saat ini
                    val articlesForCurrentPage = _allNewsArticles.subList(startIndex, endIndex)
                    _news.value = articlesForCurrentPage

                    // Periksa apakah ini adalah halaman terakhir
                    isLastPage = _allNewsArticles.size < apiPageSize || endIndex >= _allNewsArticles.size

                    // Naikkan nomor halaman untuk request berikutnya
                    if (!isLastPage) {
                        currentPage++
                    }

                    // Beritahu fragment bahwa data baru telah ditambahkan
                    _newDataAdded.value = true

                    _isLoading.value = false
                    return@launch
                }

                // Jika top headlines gagal, gunakan pendekatan dengan endpoint everything
                val randomKeywords = listOf(
                    "indonesia", "jakarta", "bandung", "surabaya", "bali",
                    "politik", "ekonomi", "teknologi", "pendidikan", "kesehatan",
                    "olahraga", "budaya", "kuliner", "pariwisata",
                    "pemerintah"
                )

                val searchQuery = randomKeywords.random()
                Log.d("NewsAPI", "Fetching Indonesian news with query: $searchQuery")

                val response = newsApiService.getEverything(
                    query = searchQuery,
                    pageSize = apiPageSize,
                    language = "id",
                    sortBy = "publishedAt"
                )

                if (response.articles.isNotEmpty()) {
                    val shuffledArticles = response.articles.shuffled()
                    Log.d("NewsAPI", "Successfully fetched ${shuffledArticles.size} random Indonesian articles")

                    // Tambahkan artikel ke list utama
                    _allNewsArticles.addAll(shuffledArticles)

                    // Hitung dari mana hingga mana artikel yang akan ditampilkan
                    val startIndex = 0
                    val endIndex = min(_allNewsArticles.size, currentPage * pageSize)

                    // Update LiveData dengan artikel untuk halaman saat ini
                    val articlesForCurrentPage = _allNewsArticles.subList(startIndex, endIndex)
                    _news.value = articlesForCurrentPage

                    // Periksa apakah ini adalah halaman terakhir
                    isLastPage = _allNewsArticles.size < apiPageSize || endIndex >= _allNewsArticles.size

                    // Naikkan nomor halaman untuk request berikutnya
                    if (!isLastPage) {
                        currentPage++
                    }

                    // Beritahu fragment bahwa data baru telah ditambahkan
                    _newDataAdded.value = true
                } else {
                    if (_allNewsArticles.isEmpty()) {
                        // Hanya load news jika tidak ada artikel sama sekali
                        loadNews()
                    } else {
                        // Jika sudah ada artikel, anggap sudah di halaman terakhir
                        isLastPage = true
                    }
                }
            } catch (e: HttpException) {
                Log.e("NewsAPI", "HTTP Error: ${e.code()}", e)
                val errorBody = e.response()?.errorBody()?.string()

                if (e.code() == 429) {
                    _error.value = "Rate limit exceeded. Please try again later."

                    if (_allNewsArticles.isEmpty()) {
                        try {
                            val fallbackResponse = newsApiService.getTopHeadlines(
                                apiKey = ApiConfig.API_KEY,
                                country = "id",
                                pageSize = 10
                            )

                            if (fallbackResponse.articles.isNotEmpty()) {
                                _allNewsArticles.addAll(fallbackResponse.articles)

                                val articlesToShow = _allNewsArticles.subList(0, min(_allNewsArticles.size, pageSize))
                                _news.value = articlesToShow

                                // Beritahu fragment bahwa data baru telah ditambahkan
                                _newDataAdded.value = true
                            }
                        } catch (fallbackError: Exception) {
                            Log.e("NewsAPI", "Fallback loading also failed", fallbackError)
                        }
                    }
                } else {
                    _error.value = "Error refreshing news: ${errorBody ?: e.message()}"
                }
            } catch (e: Exception) {
                Log.e("NewsAPI", "Error fetching random news", e)
                _error.value = "Error refreshing news: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Fungsi untuk memuat halaman berikutnya
    fun loadNextPage() {
        if (!isLastPage && !_isLoading.value!!) {
            loadRandomNews(false)
        }
    }
}
