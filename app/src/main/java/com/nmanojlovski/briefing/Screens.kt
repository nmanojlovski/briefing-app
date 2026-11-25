package com.nmanojlovski.briefing

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

// Screen 1: the newest briefing, with an "All" button that opens the list.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LatestScreen(state: IndexState, onRetry: () -> Unit, onShowAll: () -> Unit) {
    val newest = (state as? IndexState.Loaded)?.entries?.firstOrNull()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { TitleText(newest?.title ?: "Daily briefing") },
                actions = { TextButton(onClick = onShowAll) { Text("All") } }
            )
        }
    ) { innerPadding ->
        IndexContent(state, onRetry, Modifier.padding(innerPadding)) { entries ->
            val latest = entries.first()
            // key(): when a newer briefing arrives, throw away the old WebView and build a fresh one.
            key(latest.url) {
                BriefingWebView(url = latest.fullUrl)
            }
        }
    }
}

// Screen 2: every briefing, newest first. Tapping a row opens it.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(state: IndexState, onRetry: () -> Unit, onOpen: (BriefingEntry) -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { TitleText("All briefings") }) }
    ) { innerPadding ->
        IndexContent(state, onRetry, Modifier.padding(innerPadding)) { entries ->
            // LazyColumn only builds the rows that are on screen, so long lists stay fast.
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(entries) { entry ->
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

// One briefing opened from the list.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(title: String, url: String) {
    Scaffold(
        topBar = { TopAppBar(title = { TitleText(title) }) }
    ) { innerPadding ->
        BriefingWebView(url = url, modifier = Modifier.padding(innerPadding))
    }
}

// Shows a spinner while loading, an error with "Try again", or "No briefings yet".
// Only when the list has entries does it show `content`.
@Composable
fun IndexContent(
    state: IndexState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (List<BriefingEntry>) -> Unit
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (state) {
            IndexState.Loading -> CircularProgressIndicator()
            is IndexState.Error -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(state.message, textAlign = TextAlign.Center, modifier = Modifier.padding(24.dp))
                Button(onClick = onRetry) { Text("Try again") }
            }
            is IndexState.Loaded ->
                if (state.entries.isEmpty()) Text("No briefings yet.") else content(state.entries)
        }
    }
}

@Composable
private fun TitleText(text: String) {
    Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis)
}
