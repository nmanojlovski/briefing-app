package com.nmanojlovski.briefing

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nmanojlovski.briefing.ui.theme.Navy
import com.nmanojlovski.briefing.ui.theme.NavyLight
import com.nmanojlovski.briefing.ui.theme.Sun
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

// ===================== Screen 1: main menu =====================

// Greeting and date on a dark blue band, today's briefing in a big card, then the menu.
@Composable
fun HomeScreen(
    state: IndexState,
    onRetry: () -> Unit,
    onOpen: (BriefingEntry) -> Unit,
    onShowAll: () -> Unit,
    onOpenWheel: () -> Unit
) {
    // Surface paints the background and sets the matching text colour (light text in dark mode).
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
        ) {
            HomeHeader(offline = (state as? IndexState.Loaded)?.offline == true) {
                TodayCard(state, onRetry, onOpen)
            }
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text(
                    "Menu",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 12.dp, bottom = 12.dp)
                )
                MenuTile(
                    icon = Icons.AutoMirrored.Filled.List,
                    title = "Briefings",
                    subtitle = when (state) {
                        is IndexState.Loaded ->
                            "${countText(state.entries.size)} · updated ${state.updatedAt.format(UPDATED_FORMAT)}"
                        IndexState.Loading -> "Loading…"
                        is IndexState.Error -> "Browse all briefings"
                    },
                    onClick = onShowAll
                )
                Spacer(Modifier.height(12.dp))
                MenuTile(
                    icon = Icons.Default.Refresh,
                    title = "Spin the wheel",
                    subtitle = "Let the wheel pick a name",
                    onClick = onOpenWheel
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

// The dark blue band with a soft "rising sun". `card` is drawn so that its lower part
// hangs below the band, which makes it look like it floats on top.
@Composable
private fun HomeHeader(offline: Boolean, card: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val bandHeight = size.height - 80.dp.toPx()
                drawRect(
                    brush = Brush.verticalGradient(listOf(Navy, NavyLight), endY = bandHeight),
                    size = Size(size.width, bandHeight)
                )
                // The sun: a glow plus a solid circle, only inside the band.
                clipRect(bottom = bandHeight) {
                    val sunCenter = Offset(size.width - 60.dp.toPx(), 110.dp.toPx())
                    val glowRadius = 160.dp.toPx()
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(Sun.copy(alpha = 0.45f), Color.Transparent),
                            center = sunCenter,
                            radius = glowRadius
                        ),
                        radius = glowRadius,
                        center = sunCenter
                    )
                    drawCircle(color = Sun, radius = 28.dp.toPx(), center = sunCenter)
                }
            }
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(32.dp))
        Text(
            "DAILY BRIEFING",
            style = MaterialTheme.typography.labelLarge,
            color = Sun,
            letterSpacing = 2.sp
        )
        Spacer(Modifier.height(4.dp))
        Text(greeting(), style = MaterialTheme.typography.displaySmall, color = Color.White)
        Text(
            LocalDate.now().format(WEEKDAY_DATE),
            style = MaterialTheme.typography.titleMedium,
            fontFamily = FontFamily.Default,
            color = Color.White.copy(alpha = 0.8f)
        )
        if (offline) {
            Spacer(Modifier.height(12.dp))
            Text(
                "Offline · showing saved briefings",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
        Spacer(Modifier.height(28.dp))
        card()
    }
}

// The big card: the newest briefing with a "Read now" button.
@Composable
private fun TodayCard(state: IndexState, onRetry: () -> Unit, onOpen: (BriefingEntry) -> Unit) {
    ElevatedCard(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            when (state) {
                IndexState.Loading -> Box(
                    modifier = Modifier.fillMaxWidth().height(140.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                is IndexState.Error -> {
                    Text(state.message, style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onRetry) { Text("Try again") }
                }

                is IndexState.Loaded -> {
                    val latest = state.entries.firstOrNull()
                    if (latest == null) {
                        Text("No briefings yet.", style = MaterialTheme.typography.bodyLarge)
                    } else {
                        LatestBriefing(latest, onOpen)
                    }
                }
            }
        }
    }
}

@Composable
private fun LatestBriefing(entry: BriefingEntry, onOpen: (BriefingEntry) -> Unit) {
    val isToday = parseDate(entry.date) == LocalDate.now()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Pill(if (isToday) "TODAY'S BRIEFING" else "LATEST BRIEFING", highlighted = isToday)
        Spacer(Modifier.weight(1f))
        AppBadge()
    }
    Spacer(Modifier.height(12.dp))
    Text(entry.title, style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(4.dp))
    Text(
        friendlyDate(entry.date),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(20.dp))
    Button(
        onClick = { onOpen(entry) },
        colors = ButtonDefaults.buttonColors(containerColor = Sun, contentColor = Navy),
        contentPadding = PaddingValues(vertical = 14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Read now", fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(8.dp))
        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
    }
}

// The app icon (page + sun) in a small navy circle.
@Composable
private fun AppBadge() {
    Box(
        modifier = Modifier.size(56.dp).clip(CircleShape).background(Navy),
        contentAlignment = Alignment.Center
    ) {
        // The icon drawing has empty space around it, so draw it bigger than the circle.
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.requiredSize(90.dp)
        )
    }
}

// One row of the menu: coloured icon, title, subtitle and an arrow.
@Composable
private fun MenuTile(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(listOf(Navy, NavyLight))),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Sun)
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ===================== Screen 2: all briefings =====================

// Every briefing as a card, grouped by month. Pull down to reload the list.
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    state: IndexState,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onOpen: (BriefingEntry) -> Unit,
    onBack: () -> Unit
) {
    // The big title shrinks into a normal top bar as you scroll down.
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("All briefings") },
                navigationIcon = { BackButton(onBack) },
                colors = brandTopBarColors(),
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        IndexContent(state, onRetry = onRefresh, Modifier.padding(innerPadding)) { loaded ->
            PullToRefreshBox(isRefreshing = isRefreshing, onRefresh = onRefresh) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp)
                ) {
                    item {
                        Text(
                            "${countText(loaded.entries.size)} · updated ${loaded.updatedAt.format(UPDATED_FORMAT)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp, top = 16.dp)
                        )
                    }
                    // One group per month; the month name stays on top while you scroll through it.
                    loaded.entries
                        .groupBy { parseDate(it.date)?.format(MONTH_YEAR) ?: "Other" }
                        .forEach { (month, entries) ->
                            stickyHeader { MonthHeader(month) }
                            items(entries) { entry ->
                                BriefingCard(entry, onClick = { onOpen(entry) })
                                Spacer(Modifier.height(12.dp))
                            }
                        }
                }
            }
        }
    }
}

@Composable
private fun MonthHeader(month: String) {
    Text(
        month.uppercase(Locale.ENGLISH),
        style = MaterialTheme.typography.labelLarge,
        letterSpacing = 1.5.sp,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(start = 4.dp, top = 20.dp, bottom = 10.dp)
    )
}

// A briefing in the list: calendar-style date, title, "Today"/"Yesterday"/weekday.
// Today's briefing gets a sun-coloured border and a NEW tag.
@Composable
private fun BriefingCard(entry: BriefingEntry, onClick: () -> Unit) {
    val date = parseDate(entry.date)
    val isToday = date == LocalDate.now()
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = if (isToday) BorderStroke(2.dp, Sun) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            DateBadge(date)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    entry.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    friendlyDate(entry.date),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isToday) Pill("NEW", highlighted = true)
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Day number and month in a small navy square, like a calendar page.
@Composable
private fun DateBadge(date: LocalDate?) {
    Column(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(Navy, NavyLight))),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            date?.dayOfMonth?.toString() ?: "?",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            lineHeight = 24.sp
        )
        Text(
            date?.format(SHORT_MONTH)?.uppercase(Locale.ENGLISH) ?: "",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Sun
        )
    }
}

// ===================== Screen 3: one briefing =====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(title: String, url: String, onBack: () -> Unit) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { BackButton(onBack) },
                actions = {
                    IconButton(onClick = { shareBriefing(context, title, url) }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                },
                colors = brandTopBarColors()
            )
        }
    ) { innerPadding ->
        BriefingWebView(url = url, modifier = Modifier.padding(innerPadding))
    }
}

// ===================== Shared pieces =====================

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
        color = MaterialTheme.colorScheme.onTertiaryContainer,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .padding(8.dp)
    )
}

// A small rounded label, e.g. "NEW" or "TODAY'S BRIEFING".
@Composable
private fun Pill(text: String, highlighted: Boolean) {
    val colors = MaterialTheme.colorScheme
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = if (highlighted) colors.onTertiaryContainer else colors.onSecondaryContainer,
        modifier = Modifier
            .clip(CircleShape)
            .background(if (highlighted) colors.tertiaryContainer else colors.secondaryContainer)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

@Composable
fun BackButton(onBack: () -> Unit) {
    IconButton(onClick = onBack) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
    }
}

// Navy top bars with white text, in light and dark mode.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun brandTopBarColors() = TopAppBarDefaults.topAppBarColors(
    containerColor = Navy,
    scrolledContainerColor = Navy,
    titleContentColor = Color.White,
    navigationIconContentColor = Color.White,
    actionIconContentColor = Color.White
)

// Opens Android's share sheet (Viber, Gmail, ...) with the briefing's title and link.
private fun shareBriefing(context: Context, title: String, url: String) {
    val send = Intent(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_SUBJECT, title)
        .putExtra(Intent.EXTRA_TEXT, "$title\n$url")
    context.startActivity(Intent.createChooser(send, null))
}

// ----- Dates (shown in English, like the rest of the app) -----

private val WEEKDAY_DATE = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.ENGLISH)   // Thursday, 26 November
private val MONTH_YEAR = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)       // November 2026
private val SHORT_MONTH = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH)            // Nov
private val UPDATED_FORMAT = DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale.ENGLISH) // 26 Nov, 09:03

private fun parseDate(isoDate: String): LocalDate? = runCatching { LocalDate.parse(isoDate) }.getOrNull()

// "Today", "Yesterday", or e.g. "Tuesday, 24 November".
private fun friendlyDate(isoDate: String): String {
    val date = parseDate(isoDate) ?: return isoDate
    val today = LocalDate.now()
    return when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> date.format(WEEKDAY_DATE)
    }
}

private fun greeting(): String = when (LocalTime.now().hour) {
    in 5..11 -> "Good morning"
    in 12..17 -> "Good afternoon"
    else -> "Good evening"
}

private fun countText(count: Int) = if (count == 1) "1 briefing" else "$count briefings"
