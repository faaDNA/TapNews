package com.example.tapnews

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebSettings
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.example.tapnews.databinding.ActivityWebViewBinding
import androidx.appcompat.app.AppCompatDelegate
import android.content.res.Configuration

class WebViewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWebViewBinding

    companion object {
        const val EXTRA_URL = "extra_url"
        const val EXTRA_TITLE = "extra_title"
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWebViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Ambil URL dan title dari intent
        val url = intent.getStringExtra(EXTRA_URL) ?: ""
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "TapNews Article"

        // Setup toolbar
        binding.toolbar.title = title
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        // Setup WebView dengan dark mode support
        setupWebView(url)
    }

    private fun setupWebView(url: String) {
        binding.webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadsImagesAutomatically = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
        }

        // Terapkan dark mode jika tersedia dan aktif
        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
                Configuration.UI_MODE_NIGHT_YES -> {
                    WebSettingsCompat.setForceDark(binding.webView.settings, WebSettingsCompat.FORCE_DARK_ON)
                }
                Configuration.UI_MODE_NIGHT_NO -> {
                    WebSettingsCompat.setForceDark(binding.webView.settings, WebSettingsCompat.FORCE_DARK_OFF)
                }
            }
        }

        // Handle loading dan error
        binding.webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                if (newProgress < 100) {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.progressBar.progress = newProgress
                } else {
                    binding.progressBar.visibility = View.GONE
                }
            }
        }

        binding.webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                binding.progressBar.visibility = View.GONE
            }
        }

        // Load URL
        if (url.isNotEmpty()) {
            binding.webView.loadUrl(url)
        }
    }

    override fun onBackPressed() {
        // Handle back navigation in WebView
        if (binding.webView.canGoBack()) {
            binding.webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
