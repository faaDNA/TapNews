package com.example.tapnews.ui.news

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.tapnews.R
import com.example.tapnews.WebViewActivity
import com.example.tapnews.databinding.FragmentNewsBinding
import com.example.tapnews.databinding.ItemNewsBinding
import com.example.tapnews.model.Article
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.abs

class NewsFragment : Fragment() {
    private var _binding: FragmentNewsBinding? = null
    private val binding get() = _binding!!
    // Menggunakan activityViewModels() untuk berbagi ViewModel dengan activity parent
    private val viewModel: NewsViewModel by activityViewModels()
    private lateinit var newsAdapter: NewsAdapter
    private var isRefreshing = false
    private var isFirstLoad = true // Flag untuk menandai apakah fragment baru pertama kali dimuat

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNewsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupObservers()
        setupPullToRefresh()
        setupSearchView()
        setupPagination()

        // Periksa apakah viewModel sudah memiliki data, jika belum baru load data
        // dan hanya load data saat pertama kali fragment dibuat
        if (viewModel.news.value.isNullOrEmpty() && isFirstLoad) {
            viewModel.loadRandomNews(true) // Parameter true untuk memuat halaman pertama
            isFirstLoad = false
        }
    }

    private fun setupRecyclerView() {
        newsAdapter = NewsAdapter(
            onFavoriteClick = { article ->
                viewModel.saveToFavorites(article)
            },
            onArticleClick = { article ->
                openArticleInBrowser(article.url, article.title)
            }
        )
        binding.newsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.newsRecyclerView.adapter = newsAdapter
    }

    private fun setupPullToRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            // Ketika user menarik ke bawah untuk refresh, muat ulang berita dari awal (reset pagination)
            viewModel.loadRandomNews(true)
        }

        // Set colors for the refresh animation
        binding.swipeRefreshLayout.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        )
    }

    private fun openArticleInBrowser(url: String, title: String = "") {
        // Membuka artikel dalam WebViewActivity (in-app browser)
        val intent = Intent(requireContext(), WebViewActivity::class.java).apply {
            putExtra(WebViewActivity.EXTRA_URL, url)
            putExtra(WebViewActivity.EXTRA_TITLE, title)
        }
        startActivity(intent)
    }

    private fun setupObservers() {
        viewModel.news.observe(viewLifecycleOwner) { news ->
            newsAdapter.submitList(news)
            binding.progressBar.visibility = View.GONE
            binding.swipeRefreshLayout.isRefreshing = false
            binding.emptyStateLayout.visibility = if (news.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            if (!isLoading) {
                isRefreshing = false
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            if (errorMsg.isNotEmpty()) {
                Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                // Make sure to stop the refresh animation even when there's an error
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }

        viewModel.message.observe(viewLifecycleOwner) { message ->
            if (message.isNotEmpty()) {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupSearchView() {
        binding.searchView.apply {
            setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    query?.let {
                        if (it.isNotEmpty()) {
                            viewModel.searchNews(it)
                        }
                    }
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    // Hanya mengembalikan berita awal jika search dikosongkan
                    if (binding.searchView.hasFocus() && newText?.isEmpty() == true) {
                        viewModel.loadNews()
                    }
                    return true
                }
            })
        }
    }

    private fun setupPagination() {
        binding.newsRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as? LinearLayoutManager
                val totalItemCount = layoutManager?.itemCount ?: 0
                val lastVisibleItemPosition = layoutManager?.findLastVisibleItemPosition() ?: 0

                // Jika sudah mencapai 80% dari total item, dan tidak sedang loading, maka load lebih banyak data
                if (totalItemCount > 0 && lastVisibleItemPosition >= totalItemCount * 0.8 && !viewModel.isLoading.value!!) {
                    viewModel.loadRandomNews() // Memuat lebih banyak berita
                }
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class NewsAdapter(
    private val onFavoriteClick: (Article) -> Unit,
    private val onArticleClick: (Article) -> Unit
) : RecyclerView.Adapter<NewsAdapter.NewsViewHolder>() {
    private var news = listOf<Article>()

    fun submitList(newsList: List<Article>) {
        news = newsList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val binding = ItemNewsBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NewsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        holder.bind(news[position])
    }

    override fun getItemCount() = news.size

    inner class NewsViewHolder(
        private val binding: ItemNewsBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(article: Article) {
            binding.apply {
                newsTitle.text = article.title
                newsDescription.text = article.description ?: "No description available"

                // Format and display the date
                try {
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
                    val date = dateFormat.parse(article.publishedAt)
                    val displayFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                    newsDate.text = date?.let { displayFormat.format(it) }
                } catch (e: Exception) {
                    newsDate.text = article.publishedAt
                }

                // Load image with placeholder and error handling
                Glide.with(root.context)
                    .load(article.urlToImage)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .placeholder(R.drawable.ic_news)
                    .error(R.drawable.ic_news)
                    .into(newsImage)

                // Set click listeners
                root.setOnClickListener { onArticleClick(article) }
                favoriteButton.setOnClickListener { onFavoriteClick(article) }
            }
        }
    }
}
