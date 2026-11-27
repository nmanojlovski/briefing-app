package com.nmanojlovski.briefing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nmanojlovski.briefing.ui.theme.Navy
import com.nmanojlovski.briefing.ui.theme.Sun
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

// ===================== Screen 5: budget =====================

// One month at a time: balance, a donut of where the money went, the last 6 months as bars,
// and the month's entries. + adds an entry; tapping an entry edits or deletes it.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    viewModel: BudgetViewModel,
    onAdd: () -> Unit,
    onEdit: (BudgetEntry) -> Unit,
    onBack: () -> Unit
) {
    // collectAsState: redraws the screen whenever the database sends a new list.
    val entries by viewModel.entries.collectAsState()
    val month = viewModel.month
    val summary = summarize(entries, month)
    val monthEntries = entries.filter { it.date.startsWith(month.toString()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Budget") },
                navigationIcon = { BackButton(onBack) },
                colors = brandTopBarColors()
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd, containerColor = Sun, contentColor = Navy) {
                Icon(Icons.Default.Add, contentDescription = "Add an entry")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp), // room for +
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                MonthPicker(
                    month = month,
                    canGoForward = month < YearMonth.now(),
                    onPrevious = viewModel::previousMonth,
                    onNext = viewModel::nextMonth
                )
            }
            item { BalanceCard(summary) }
            item {
                BudgetCard(title = "Where the money went") {
                    if (summary.spent == 0L) {
                        Text("Nothing spent this month.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        DonutChart(
                            summary.byCategory,
                            total = summary.spent,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        Spacer(Modifier.height(16.dp))
                        summary.byCategory.forEach { (category, amount) ->
                            CategoryLegendRow(category, amount, percentOf(amount, summary.spent))
                        }
                    }
                }
            }
            item {
                BudgetCard(title = "Last 6 months") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LegendDot(IncomeColor)
                        Text(" In   ", style = MaterialTheme.typography.labelMedium)
                        LegendDot(SpentColor)
                        Text(" Out", style = MaterialTheme.typography.labelMedium)
                    }
                    Spacer(Modifier.height(12.dp))
                    MonthBars(lastMonths(entries, month))
                }
            }
            item {
                Text(
                    "Entries",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                )
            }
            if (monthEntries.isEmpty()) {
                item {
                    Text(
                        "No entries in ${month.format(MONTH_NAME)}. Tap + to add one.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
            items(monthEntries, key = { it.id }) { entry ->
                EntryRow(entry, onClick = { onEdit(entry) })
            }
        }
    }
}

// ‹ November 2026 ›  (no going past the current month)
@Composable
private fun MonthPicker(month: YearMonth, canGoForward: Boolean, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month")
        }
        Text(
            month.format(MONTH_NAME),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
        IconButton(onClick = onNext, enabled = canGoForward) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month")
        }
    }
}

// Balance in big numbers (green when positive, red when negative), with money in and out below.
@Composable
private fun BalanceCard(summary: MonthSummary) {
    BudgetCard(title = "Balance") {
        Text(
            (if (summary.balance > 0) "+" else "") + formatMkd(summary.balance),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = if (summary.balance < 0) Color(0xFFE63946) else IncomeColor
        )
        Spacer(Modifier.height(8.dp))
        Row {
            Text("In  ", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatMkd(summary.income), color = IncomeColor, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(20.dp))
            Text("Out  ", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatMkd(summary.spent), color = SpentColor, fontWeight = FontWeight.Bold)
        }
    }
}

// A white (dark grey in dark mode) card with a small title, like the rest of the app.
@Composable
private fun BudgetCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                title.uppercase(Locale.ENGLISH),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

// ● Food ............ 12,300 MKD  38%
@Composable
private fun CategoryLegendRow(category: Category, amount: Long, percent: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        LegendDot(category.color)
        Spacer(Modifier.width(10.dp))
        Text(category.label, modifier = Modifier.weight(1f))
        Text(formatMkd(amount), fontWeight = FontWeight.Bold)
        Text(
            "  $percent%",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(52.dp),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun LegendDot(color: Color) {
    Box(Modifier.size(12.dp).clip(CircleShape).background(color))
}

// One entry: category colour, note (or category name), date, and +/- amount.
@Composable
private fun EntryRow(entry: BudgetEntry, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(entry.category.color),
                contentAlignment = Alignment.Center
            ) {
                Text(entry.category.label.take(1), color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    entry.note.ifBlank { entry.category.label },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    entryDate(entry.date) + if (entry.note.isNotBlank()) " · ${entry.category.label}" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                (if (entry.isDeposit) "+" else "−") + formatMkd(entry.amount),
                fontWeight = FontWeight.Bold,
                color = if (entry.isDeposit) IncomeColor else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private val MONTH_NAME = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)  // November 2026
private val SHORT_DATE = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH)  // Fri 27 Nov

private fun entryDate(isoDate: String): String =
    runCatching { LocalDate.parse(isoDate).format(SHORT_DATE) }.getOrDefault(isoDate)
