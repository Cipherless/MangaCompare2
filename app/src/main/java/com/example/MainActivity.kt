package com.example

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.screens.ComparisonScreen
import com.example.ui.theme.MangaCompareTheme
import com.example.ui.viewmodel.ComparisonViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: ComparisonViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle image shared at launch
        handleIntent(intent)

        setContent {
            MangaCompareTheme {
                ComparisonScreen(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Handle image shared into already running activity (appends to active comparison)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val uris = extractUrisFromIntent(intent)
        if (uris.isNotEmpty()) {
            viewModel.importUris(uris, defaultNotePrefix = "Shared")
        }
    }

    private fun extractUrisFromIntent(intent: Intent?): List<Uri> {
        if (intent == null) return emptyList()
        val action = intent.action
        val type = intent.type ?: ""
        if (!type.startsWith("image/")) return emptyList()

        return when (action) {
            Intent.ACTION_SEND -> {
                val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri
                }
                listOfNotNull(uri)
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                val list = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
                }
                list?.filterNotNull() ?: emptyList()
            }
            else -> emptyList()
        }
    }
}
