package com.nmanojlovski.briefing

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.format.TextStyle
import java.util.Locale

// ===================== Budget: charts =====================
// Both charts are drawn by hand on a Canvas, like the spin-the-wheel.

val IncomeColor = Color(0xFF2E9E5B) // green: money in
val SpentColor = Color(0xFFFB8500)  // orange: money out

// A ring with one coloured piece per category; the total in the middle.
// It grows around the circle when it first appears or the month changes.
@Composable
fun DonutChart(slices: List<Pair<Category, Long>>, total: Long, modifier: Modifier = Modifier) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(slices) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(durationMillis = 700))
    }
    Box(modifier = modifier.size(200.dp), contentAlignment = Alignment.Center) {
        val track = MaterialTheme.colorScheme.surfaceVariant
        Canvas(modifier = Modifier.size(200.dp)) {
            val ringWidth = 28.dp.toPx()
            // The ring is drawn in the middle of its width, so shrink the square by half of it.
            val inset = ringWidth / 2
            val arcSize = Size(size.width - ringWidth, size.height - ringWidth)
            val topLeft = Offset(inset, inset)
            drawArc(track, 0f, 360f, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(ringWidth))

            var start = -90f // start at the top, go clockwise
            slices.forEach { (category, amount) ->
                val sweep = 360f * amount / total * progress.value
                drawArc(
                    category.color,
                    startAngle = start,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(ringWidth)
                )
                start += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Spent", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatMkd(total), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

// Two bars per month: money in (green) and money out (orange), all on the same scale.
@Composable
fun MonthBars(months: List<MonthSummary>, modifier: Modifier = Modifier) {
    val highest = months.maxOfOrNull { maxOf(it.income, it.spent) } ?: 0L
    val baseline = MaterialTheme.colorScheme.outlineVariant
    val progress = remember { Animatable(0f) }
    LaunchedEffect(months) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(durationMillis = 600))
    }
    Column(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxWidth().height(140.dp)) {
            val groupWidth = size.width / months.size
            val barWidth = groupWidth * 0.28f
            val radius = CornerRadius(4.dp.toPx())
            months.forEachIndexed { i, month ->
                val groupLeft = i * groupWidth + (groupWidth - 2 * barWidth - 4.dp.toPx()) / 2
                listOf(month.income to IncomeColor, month.spent to SpentColor).forEachIndexed { j, (amount, color) ->
                    if (highest == 0L || amount == 0L) return@forEachIndexed
                    val barHeight = size.height * amount / highest * progress.value
                    drawRoundRect(
                        color,
                        topLeft = Offset(groupLeft + j * (barWidth + 4.dp.toPx()), size.height - barHeight),
                        size = Size(barWidth, barHeight),
                        cornerRadius = radius
                    )
                }
            }
            drawLine(baseline, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 1.dp.toPx())
        }
        // Month names under the bars, e.g. "Jun Jul Aug Sep Oct Nov".
        Row(modifier = Modifier.fillMaxWidth()) {
            months.forEach { month ->
                Text(
                    month.month.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
