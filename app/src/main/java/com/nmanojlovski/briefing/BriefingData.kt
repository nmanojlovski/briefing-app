package com.nmanojlovski.briefing

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET

const val BASE_URL = "https://nmanojlovski.github.io/daily-briefing/"

// One entry of index.json: {"date": "2026-11-25", "title": "...", "url": "briefings/2026-11-25.html"}
// @Serializable lets kotlinx.serialization turn that JSON into this class.
@Serializable
data class BriefingEntry(val date: String, val title: String, val url: String) {
    val fullUrl: String get() = BASE_URL + url
}

// Retrofit writes the networking code for this interface:
// getIndex() downloads BASE_URL + "index.json" and decodes it into a list.
interface BriefingApi {
    @GET("index.json")
    suspend fun getIndex(): List<BriefingEntry>
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
    data class Loaded(val entries: List<BriefingEntry>) : IndexState
    data class Error(val message: String) : IndexState
}

// The ViewModel holds the list of briefings. It survives screen rotation,
// and both screens read the same `state`.
class BriefingViewModel : ViewModel() {
    var state by mutableStateOf<IndexState>(IndexState.Loading)
        private set

    fun refresh() {
        // launch starts a coroutine: getIndex() waits for the network without freezing the screen.
        viewModelScope.launch {
            val hadData = state is IndexState.Loaded
            if (!hadData) state = IndexState.Loading
            try {
                val entries = briefingApi.getIndex().sortedByDescending { it.date } // newest first
                state = IndexState.Loaded(entries)
            } catch (e: Exception) {
                // No internet, server error or broken JSON. If we already show a list, keep it.
                if (!hadData) {
                    state = IndexState.Error("Couldn't load the briefings.\nCheck your internet connection.")
                }
            }
        }
    }
}
