package com.nmanojlovski.briefing

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Url
import java.io.File
import java.time.LocalDateTime

const val BASE_URL = "https://nmanojlovski.github.io/daily-briefing/"
private const val INDEX_FILE = "index.json"

// One entry of index.json: {"date": "2026-11-25", "title": "...", "url": "briefings/2026-11-25.html"}
// @Serializable lets kotlinx.serialization turn that JSON into this class.
@Serializable
data class BriefingEntry(val date: String, val title: String, val url: String) {
    val fullUrl: String get() = BASE_URL + url
}

// Retrofit writes the networking code for this interface.
interface BriefingApi {
    // Downloads BASE_URL + "index.json" and decodes it into a list.
    @GET("index.json")
    suspend fun getIndex(): List<BriefingEntry>

    // Downloads any URL as raw text; used for the HTML of one briefing.
    @GET
    suspend fun getPage(@Url url: String): ResponseBody
}

private val json = Json { ignoreUnknownKeys = true } // don't crash if index.json gains new fields

val briefingApi: BriefingApi = Retrofit.Builder()
    .baseUrl(BASE_URL)
    .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
    .build()
    .create(BriefingApi::class.java)

// Everything a screen can be showing. `sealed` means these three are the only options,
// so `when (state)` in the UI must handle all of them.
sealed interface IndexState {
    data object Loading : IndexState

    // offline = true: there's no internet, so this is the list saved on the phone last time.
    data class Loaded(
        val entries: List<BriefingEntry>,
        val updatedAt: LocalDateTime,
        val offline: Boolean = false
    ) : IndexState

    data class Error(val message: String) : IndexState
}

// The ViewModel holds the list of briefings. It survives screen rotation,
// and all screens read the same `state`.
class BriefingViewModel(private val app: Application) : AndroidViewModel(app) {
    var state by mutableStateOf<IndexState>(IndexState.Loading)
        private set

    // True while a pull-to-refresh is running (shows the spinner above the list).
    var isRefreshing by mutableStateOf(false)
        private set

    fun refresh() {
        // launch starts a coroutine: getIndex() waits for the network without freezing the screen.
        viewModelScope.launch {
            if (state is IndexState.Loaded) isRefreshing = true else state = IndexState.Loading
            try {
                val entries = briefingApi.getIndex().sortedByDescending { it.date } // newest first
                val now = LocalDateTime.now()
                state = IndexState.Loaded(entries, updatedAt = now)
                saveFile(app, INDEX_FILE, json.encodeToString(SavedIndex(now.toString(), entries)))
            } catch (e: Exception) {
                // No internet, server error or broken JSON.
                // Keep a list we already show (marked offline), or fall back to the saved one.
                val current = state
                state = if (current is IndexState.Loaded) {
                    current.copy(offline = true)
                } else {
                    loadSavedIndex() ?: IndexState.Error("Couldn't load the briefings.\nCheck your internet connection.")
                }
            } finally {
                isRefreshing = false
            }
        }
    }

    private suspend fun loadSavedIndex(): IndexState.Loaded? {
        val text = readFile(app, INDEX_FILE) ?: return null
        return runCatching {
            val saved = json.decodeFromString<SavedIndex>(text)
            IndexState.Loaded(saved.entries, LocalDateTime.parse(saved.updatedAt), offline = true)
        }.getOrNull()
    }
}

// ----- Offline copies -----
// Saved as files in the app's private storage (filesDir). Only this app can read them,
// and they stay until the app is uninstalled.

// The list as it's saved on the phone, together with when it was downloaded.
@Serializable
private data class SavedIndex(val updatedAt: String, val entries: List<BriefingEntry>)

// Downloads one briefing page and keeps a copy. Without internet it returns the saved copy,
// or null if this briefing was never opened before.
suspend fun loadPage(context: Context, url: String): String? {
    val fileName = url.substringAfterLast('/') // e.g. "2026-11-25.html"
    return try {
        val html = briefingApi.getPage(url).string()
        saveFile(context, fileName, html)
        html
    } catch (e: Exception) {
        readFile(context, fileName)
    }
}

// Dispatchers.IO: file reading and writing happen on a background thread, not the UI thread.
private suspend fun saveFile(context: Context, name: String, text: String) = withContext(Dispatchers.IO) {
    runCatching { File(context.filesDir, name).writeText(text) }
}

private suspend fun readFile(context: Context, name: String): String? = withContext(Dispatchers.IO) {
    runCatching { File(context.filesDir, name).readText() }.getOrNull() // null if the file doesn't exist
}
