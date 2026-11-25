package com.nmanojlovski.briefing

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.format.DateTimeFormatter

private val UPDATED_FORMAT = DateTimeFormatter.ofPattern("d MMM, HH:mm") // e.g. "26 Nov, 09:03"

// Screen 1: the newest briefing, with Share and "all briefings" buttons.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LatestScreen(state: IndexState, onRetry: () -> Unit, onShowAll: () -> Unit) {
    val context = LocalContext.current
    val newest = (state as? IndexState.Loaded)?.entries?.firstOrNull()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { TitleText(newest?.title ?: "Daily briefing") },
                actions = {
                    if (newest != null) {
                        IconButton(onClick = { shareBriefing(context, newest.title, newest.fullUrl) }) {
                            Icon(Icons.Default.Share, contentDescription = "Share")
                        }
                    }
                    IconButton(onClick = onShowAll) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = "All briefings")
                    }
                }
            )
        }
    ) { innerPadding ->
        IndexContent(state, onRetry, Modifier.padding(innerPadding)) { loaded ->
            val latest = loaded.entries.first()
            // key(): when a newer briefing arrives, throw away the old WebView and build a fresh one.
            key(latest.url) {
                BriefingWebView(url = latest.fullUrl)
            }
        }
    }
}

// Screen 2: every briefing, newest first. Tapping a row opens it; pulling down reloads the list.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    state: IndexState,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onOpen: (BriefingEntry) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { TitleText("All briefings") },
                navigationIcon = { BackButton(onBack) }
            )
        }
    ) { innerPadding ->
        IndexContent(state, onRetry = onRefresh, Modifier.padding(innerPadding)) { loaded ->
            PullToRefreshBox(isRefreshing = isRefreshing, onRefresh = onRefresh) {
                // LazyColumn only builds the rows that are on screen, so long lists stay fast.
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        Text(
                            "Last updated ${loaded.updatedAt.format(UPDATED_FORMAT)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    items(loaded.entries) { entry ->
                        ListItem(
                            headlineContent = { Text(entry.title) },
                            supportingContent = { Text(entry.date) },
                            modifier = Modifier.clickable { onOpen(entry) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

// One briefing opened from the list.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(title: String, url: String, onBack: () -> Unit) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { TitleText(title) },
                navigationIcon = { BackButton(onBack) },
                actions = {
                    IconButton(onClick = { shareBriefing(context, title, url) }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                }
            )
        }
    ) { innerPadding ->
        BriefingWebView(url = url, modifier = Modifier.padding(innerPadding))
    }
}

// Shows a spinner while loading, or an error with "Try again", or "No briefings yet".
// Once there's a list it shows `content`, with an "Offline" banner above it when needed.
@Composable
fun IndexContent(
    state: IndexState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (IndexState.Loaded) -> Unit
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (state) {
            IndexState.Loading -> CircularProgressIndicator()
            is IndexState.Error -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(state.message, textAlign = TextAlign.Center, modifier = Modifier.padding(24.dp))
                Button(onClick = onRetry) { Text("Try again") }
            }
            is IndexState.Loaded -> Column(modifier = Modifier.fillMaxSize()) {
                if (state.offline) OfflineBanner()
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    if (state.entries.isEmpty()) Text("No briefings yet.") else content(state)
                }
            }
        }
    }
}

@Composable
private fun OfflineBanner() {
    Text(
        "Offline – showing briefings saved on the phone",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(8.dp)
    )
}

@Composable
private fun BackButton(onBack: () -> Unit) {
    IconButton(onClick = onBack) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
    }
}

@Composable
private fun TitleText(text: String) {
    Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

// Opens Android's share sheet (Viber, Gmail, ...) with the briefing's title and link.
private fun shareBriefing(context: Context, title: String, url: String) {
    val send = Intent(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_SUBJECT, title)
        .putExtra(Intent.EXTRA_TEXT, "$title\n$url")
    context.startActivity(Intent.createChooser(send, null))
}
