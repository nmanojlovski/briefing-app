package com.nmanojlovski.briefing

import android.graphics.Bitmap
import android.graphics.Color
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun BriefingWebView(url: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var html by remember { mutableStateOf<String?>(null) } // the page, once we have it
    var notSaved by remember { mutableStateOf(false) }     // offline and never opened before
    var isLoading by remember { mutableStateOf(true) }
    var canGoBack by remember { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WebView?>(null) }

    // Runs once for this url: download the page (and save a copy), or read the saved copy when offline.
    LaunchedEffect(url) {
        val page = loadPage(context, url)
        html = page
        if (page == null) {
            notSaved = true
            isLoading = false
        }
    }

    // While there is a previous page, Back goes there instead of leaving the screen.
    BackHandler(enabled = canGoBack) {
        webView?.goBack()
    }

    Box(modifier = modifier.fillMaxSize()) {
        val page = html
        if (page != null) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    WebView(it).apply {
                        setBackgroundColor(Color.TRANSPARENT) // no white flash in dark mode
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, pageUrl: String?, favicon: Bitmap?) {
                                isLoading = true
                            }

                            override fun onPageFinished(view: WebView?, pageUrl: String?) {
                                isLoading = false
                                canGoBack = view?.canGoBack() == true
                            }
                        }
                        // Show the HTML we already have. `url` tells the WebView where it came from,
                        // so links inside the page still work.
                        loadDataWithBaseURL(url, page, "text/html", "utf-8", url)
                        webView = this
                    }
                }
            )
        }
        if (notSaved) {
            Text(
                "You're offline, and this briefing isn't saved on the phone yet.\n" +
                    "Open it once with internet to read it offline later.",
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center).padding(24.dp)
            )
        }
        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
    }
}
