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

// One entry of index.json: {"date": "2026-11-25", "title": "...", "url": "briefings/2026-11-25.html", "headlines": [...]}
// @Serializable lets kotlinx.serialization turn that JSON into this class.
// `headlines` (the day's top 3 stories) was added later, so older entries don't have it: then it's empty.
@Serializable
data class BriefingEntry(
    val date: String,
    val title: String,
    val url: String,
    val headlines: List<String> = emptyList()
) {
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

    // File names of the archived briefings, e.g. "2026-11-26.html" (see Archive.kt).
    var archived by mutableStateOf(loadArchived(app))
        private set

    // Light, dark or the same as the phone (see ThemeSetting.kt).
    var themeMode by mutableStateOf(loadThemeMode(app))
        private set

    init {
        // Once per app start: delete saved briefings older than KEEP_DAYS days, unless archived.
        viewModelScope.launch { cleanUpOldBriefings(app) }
    }

    // The ★ button: archive a briefing (keep it on the phone for good), or take it out again.
    fun toggleArchive(url: String) {
        val file = briefingFileName(url)
        archived = if (file in archived) archived - file else archived + file
        saveArchived(app, archived)
        // Make sure an archived briefing really is saved on the phone.
        if (file in archived) viewModelScope.launch { loadPage(app, url) }
    }

    // The sun/moon button: switch to the opposite of what the app shows right now.
    fun toggleTheme(isDarkNow: Boolean) {
        themeMode = if (isDarkNow) ThemeMode.LIGHT else ThemeMode.DARK
        saveThemeMode(app, themeMode)
        applyThemeMode(app, themeMode)
    }

    fun refresh() {
        // launch starts a coroutine: getIndex() waits for the network without freezing the screen.
        viewModelScope.launch {
            if (state is IndexState.Loaded) isRefreshing = true else state = IndexState.Loading
            try {
                val entries = fetchIndex(app)
                state = IndexState.Loaded(entries, updatedAt = LocalDateTime.now())
                updateWidgets(app) // show the newest headlines on the home screen too
            } catch (e: Exception) {
                // No internet, server error or broken JSON.
                // Keep a list we already show (marked offline), or fall back to the saved one.
                val current = state
                state = if (current is IndexState.Loaded) {
                    current.copy(offline = true)
                } else {
                    loadSavedIndex(app) ?: IndexState.Error("Couldn't load the briefings.\nCheck your internet connection.")
                }
            } finally {
                isRefreshing = false
            }
        }
    }
}

// Downloads index.json, keeps a copy on the phone and returns the briefings, newest first.
// Used by the app and the background check; the widget reads the saved copy.
suspend fun fetchIndex(context: Context): List<BriefingEntry> {
    val entries = briefingApi.getIndex().sortedByDescending { it.date } // newest first
    saveFile(context, INDEX_FILE, json.encodeToString(SavedIndex(LocalDateTime.now().toString(), entries)))
    return entries
}

// The list saved by fetchIndex, or null if there is none yet.
suspend fun loadSavedIndex(context: Context): IndexState.Loaded? {
    val text = readFile(context, INDEX_FILE) ?: return null
    return runCatching {
        val saved = json.decodeFromString<SavedIndex>(text)
        IndexState.Loaded(saved.entries, LocalDateTime.parse(saved.updatedAt), offline = true)
    }.getOrNull()
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
    val fileName = briefingFileName(url) // e.g. "2026-11-25.html"
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
