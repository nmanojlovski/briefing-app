package com.nmanojlovski.briefing

import android.app.UiModeManager
import android.content.Context
import android.os.Build

// ===================== Light / dark mode =====================
// Chosen with the small sun/moon button in the corner of the main menu, and saved on the phone.
// Until you tap it the app follows the phone (SYSTEM); after that it stays light or dark.

enum class ThemeMode { SYSTEM, LIGHT, DARK }

private const val PREFS = "settings"
private const val KEY_THEME = "theme"

fun loadThemeMode(context: Context): ThemeMode {
    val name = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_THEME, null)
    return ThemeMode.entries.find { it.name == name } ?: ThemeMode.SYSTEM
}

fun saveThemeMode(context: Context, mode: ThemeMode) {
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_THEME, mode.name)
        .apply()
}

// The app's own screens follow ThemeMode through BriefingTheme (see MainActivity).
// Since Android 12 we can also tell Android itself which mode this app wants. Then the
// briefing pages switch too (they use the CSS prefers-color-scheme), and Android remembers it.
fun applyThemeMode(context: Context, mode: ThemeMode) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(UiModeManager::class.java).setApplicationNightMode(
            when (mode) {
                ThemeMode.SYSTEM -> UiModeManager.MODE_NIGHT_AUTO // follow the phone's setting
                ThemeMode.LIGHT -> UiModeManager.MODE_NIGHT_NO
                ThemeMode.DARK -> UiModeManager.MODE_NIGHT_YES
            }
        )
    }
}
