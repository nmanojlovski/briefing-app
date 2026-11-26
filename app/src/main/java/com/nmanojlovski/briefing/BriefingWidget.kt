package com.nmanojlovski.briefing

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.nmanojlovski.briefing.ui.theme.Navy
import com.nmanojlovski.briefing.ui.theme.Sun
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// ===================== Home screen widget =====================
// A widget isn't drawn by the app: the home screen (launcher) shows it, even when the app is closed.
// Glance lets us describe it in a Compose-like way, but with its own, smaller set of building blocks:
// note that Text, Column, ... below come from androidx.glance, not androidx.compose.

// Tells the widget to redraw itself with the saved list. Called after the app or the
// background check downloads index.json. A widget problem must never break those, hence runCatching.
suspend fun updateWidgets(context: Context) {
    runCatching { BriefingWidget().updateAll(context) }
}

class BriefingWidget : GlanceAppWidget() {
    // Runs whenever the widget needs to be drawn. It only reads the list saved on the phone,
    // so it's fast and works offline.
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val latest = loadSavedIndex(context)?.entries?.firstOrNull()
        provideContent { WidgetContent(latest) }
    }
}

// Android finds the widget through this receiver (see <receiver> in AndroidManifest.xml).
class BriefingWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BriefingWidget()
}

// Navy card: "DAILY BRIEFING", the date, and the top 3 headlines. Tapping it opens the briefing.
@Composable
private fun WidgetContent(entry: BriefingEntry?) {
    val context = LocalContext.current
    // Same extras as the notification, so MainActivity opens this briefing straight away.
    val open = Intent(context, MainActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    if (entry != null) {
        open.putExtra(EXTRA_TITLE, entry.title).putExtra(EXTRA_URL, entry.fullUrl)
    }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Navy)
            .cornerRadius(24.dp)
            .padding(16.dp)
            .clickable(actionStartActivity(open))
    ) {
        Text("DAILY BRIEFING", style = TextStyle(color = fixed(Sun), fontSize = 12.sp, fontWeight = FontWeight.Bold))
        Spacer(GlanceModifier.height(2.dp))
        when {
            entry == null ->
                Text("Open the app once to load your briefings.", style = TextStyle(color = fixed(Color.White), fontSize = 14.sp))

            // Older briefings have no headlines: show the title instead.
            entry.headlines.isEmpty() -> {
                WidgetDate(entry)
                Text(entry.title, style = TextStyle(color = fixed(Color.White), fontSize = 15.sp, fontWeight = FontWeight.Bold))
            }

            else -> {
                WidgetDate(entry)
                entry.headlines.take(3).forEach { headline ->
                    Text(
                        "• $headline",
                        style = TextStyle(color = fixed(Color.White), fontSize = 14.sp),
                        maxLines = 2,
                        modifier = GlanceModifier.padding(bottom = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun WidgetDate(entry: BriefingEntry) {
    Text(
        widgetDate(entry.date),
        style = TextStyle(color = fixed(Color.White.copy(alpha = 0.8f)), fontSize = 13.sp)
    )
    Spacer(GlanceModifier.height(8.dp))
}

// Glance needs colours as a ColorProvider: one colour for light mode and one for dark mode.
// The widget is always navy, so both are the same.
private fun fixed(color: Color) = ColorProvider(color, color)

private val WIDGET_DATE = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.ENGLISH) // Thursday, 26 November

// A date, not "Today": the widget only redraws when it's told to, so "Today" could be stale tomorrow.
private fun widgetDate(isoDate: String): String =
    runCatching { LocalDate.parse(isoDate).format(WIDGET_DATE) }.getOrDefault(isoDate)
