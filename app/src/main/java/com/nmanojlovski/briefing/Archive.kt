package com.nmanojlovski.briefing

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

// ===================== Archive =====================
// Saved briefings (the .html files in filesDir) are deleted after KEEP_DAYS days,
// unless you archived them with the ★ button. Archived ones stay until you uninstall
// the app or tap "Clear storage" in its Android settings.

const val KEEP_DAYS = 10L

private const val PREFS = "archive"
private const val KEY_FILES = "files"

// The name a briefing is saved under, e.g. ".../briefings/2026-11-26.html" -> "2026-11-26.html".
fun briefingFileName(url: String): String = url.substringAfterLast('/')

// The file names of all archived briefings.
fun loadArchived(context: Context): Set<String> =
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getStringSet(KEY_FILES, null)
        ?.toSet() // a copy: Android doesn't allow changing the set it returns
        ?: emptySet()

fun saveArchived(context: Context, files: Set<String>) {
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .edit()
        .putStringSet(KEY_FILES, files)
        .apply()
}

// Deletes old saved briefings that aren't archived. Runs when the app starts and
// after each background check. index.json and other files are left alone.
suspend fun cleanUpOldBriefings(context: Context) {
    withContext(Dispatchers.IO) { // file work happens on a background thread
        val archived = loadArchived(context)
        val today = LocalDate.now()
        context.filesDir.listFiles()?.forEach { file ->
            if (shouldDelete(file.name, archived, today)) file.delete()
        }
    }
}

// True for a saved briefing ("YYYY-MM-DD.html") more than KEEP_DAYS days old that isn't archived.
// Kept separate so it can be unit-tested without a phone.
fun shouldDelete(fileName: String, archived: Set<String>, today: LocalDate): Boolean {
    if (!fileName.endsWith(".html") || fileName in archived) return false
    val date = runCatching { LocalDate.parse(fileName.removeSuffix(".html")) }.getOrNull() ?: return false
    return date.isBefore(today.minusDays(KEEP_DAYS))
}
