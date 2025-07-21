package com.example.tapnews.ui.favorites

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tapnews.WebViewActivity
import com.example.tapnews.R
import com.example.tapnews.databinding.FragmentFavoritesBinding
import com.example.tapnews.databinding.ItemNewsBinding
import com.example.tapnews.model.FavoriteArticle

class FavoritesFragment : Fragment() {
    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FavoriteViewModel by viewModels()
    private lateinit var favoritesAdapter: FavoritesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupObservers()
        viewModel.loadFavorites() // Load favorites when fragment is created
    }

    private fun setupRecyclerView() {
        favoritesAdapter = FavoritesAdapter(
            onRemoveClick = { article ->
                viewModel.removeFromFavorites(article.id)
            },
            onArticleClick = { article ->
                openArticleInWebView(article)
            }
        )
        binding.favoritesRecyclerView.adapter = favoritesAdapter
    }

    private fun openArticleInWebView(article: FavoriteArticle) {
        val intent = Intent(requireContext(), WebViewActivity::class.java).apply {
            putExtra(WebViewActivity.EXTRA_URL, article.url)
            putExtra(WebViewActivity.EXTRA_TITLE, article.title)
        }
        startActivity(intent)
    }

    private fun setupObservers() {
        viewModel.favorites.observe(viewLifecycleOwner) { favorites ->
            favoritesAdapter.submitList(favorites)
            binding.emptyStateLayout.visibility =
                if (favorites.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (error.isNotEmpty()) {
                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class FavoritesAdapter(
    private val onRemoveClick: (FavoriteArticle) -> Unit,
    private val onArticleClick: (FavoriteArticle) -> Unit
) : RecyclerView.Adapter<FavoritesAdapter.FavoriteViewHolder>() {
    private var favorites = listOf<FavoriteArticle>()

    fun submitList(newList: List<FavoriteArticle>) {
        favorites = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteViewHolder {
        val binding = ItemNewsBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FavoriteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FavoriteViewHolder, position: Int) {
        holder.bind(favorites[position])
    }

    override fun getItemCount() = favorites.size

    inner class FavoriteViewHolder(
        private val binding: ItemNewsBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(article: FavoriteArticle) {
            binding.apply {
                newsTitle.text = article.title
                newsDescription.text = article.description ?: "No description available"
                newsDate.text = article.publishedAt

                // Load image
                Glide.with(root.context)
                    .load(article.urlToImage)
                    .placeholder(R.drawable.ic_news)
                    .error(R.drawable.ic_news)
                    .into(newsImage)

                // Setup click listeners
                root.setOnClickListener { onArticleClick(article) }
                favoriteButton.apply {
                    setImageResource(R.drawable.ic_bookmark_filled)
                    setOnClickListener { onRemoveClick(article) }
                }
            }
        }
    }
}
