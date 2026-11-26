package com.nmanojlovski.briefing

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.nmanojlovski.briefing.ui.theme.Navy
import com.nmanojlovski.briefing.ui.theme.NavyLight
import com.nmanojlovski.briefing.ui.theme.Sun
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.roundToLong
import kotlin.math.sin

// ===================== The sky in the main menu =====================
// The header shows a sun during the day and a moon with stars at night. The sun is highest and
// brightest at 13:00, and sinks and fades towards sunrise and sunset. Sunrise and sunset are
// calculated for each day, so the moon comes out when it really gets dark outside.

// Where the sunrise and sunset are calculated for: Skopje. Change these for another city.
private const val LATITUDE = 41.9981
private const val LONGITUDE = 21.4254

private val BRIGHTEST = LocalTime.of(13, 0)

data class SunTimes(val sunrise: LocalTime, val sunset: LocalTime)

// Sunrise and sunset on `date`, in the phone's clock time (summer time included).
// This is a well-known approximation, accurate to a few minutes, which is plenty here.
fun sunTimes(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): SunTimes {
    val day = date.dayOfYear
    // How far north or south the sun is today, in degrees: +23.44 in June, -23.44 in December.
    val declination = Math.toRadians(-23.44 * cos(2 * PI / 365 * (day + 10)))
    val latitude = Math.toRadians(LATITUDE)
    // How long the sun is above the horizon before and after noon, as an angle (15° = 1 hour).
    // -0.833° is where the top of the sun touches the horizon (the air bends the light a little).
    val cosHalfDay = (sin(Math.toRadians(-0.833)) - sin(latitude) * sin(declination)) /
        (cos(latitude) * cos(declination))
    val halfDayMinutes = Math.toDegrees(acos(cosHalfDay.coerceIn(-1.0, 1.0))) * 4
    // "Equation of time": during the year the sun runs up to ~16 minutes early or late.
    val b = 2 * PI * (day - 81) / 364
    val equationOfTime = 9.87 * sin(2 * b) - 7.53 * cos(b) - 1.5 * sin(b)
    // Solar noon in clock time: 12:00 UTC, shifted 4 minutes per degree of longitude,
    // corrected by the equation of time, plus the time zone (+1 h in winter, +2 h in summer).
    val zoneMinutes = zone.rules.getOffset(date.atTime(12, 0)).totalSeconds / 60
    val noonMinutes = 720 - 4 * LONGITUDE - equationOfTime + zoneMinutes
    return SunTimes(clockTime(noonMinutes - halfDayMinutes), clockTime(noonMinutes + halfDayMinutes))
}

private fun clockTime(minutes: Double): LocalTime = LocalTime.MIDNIGHT.plusSeconds((minutes * 60).roundToLong())

// What the header shows: the moon at night, or the sun with a brightness from
// 0 (at sunrise and sunset) to 1 (at 13:00).
data class Sky(val night: Boolean, val sunBrightness: Float)

fun skyAt(now: LocalDateTime, zone: ZoneId = ZoneId.systemDefault()): Sky {
    val (sunrise, sunset) = sunTimes(now.toLocalDate(), zone)
    val time = now.toLocalTime()
    if (time < sunrise || time >= sunset) return Sky(night = true, sunBrightness = 0f)
    val brightness = if (time <= BRIGHTEST) {
        closeness(time, edge = sunrise, peak = BRIGHTEST) // morning: getting brighter
    } else {
        closeness(time, edge = sunset, peak = BRIGHTEST)  // afternoon: fading
    }
    return Sky(night = false, sunBrightness = brightness)
}

// How far `time` has got from `edge` (0) to `peak` (1).
private fun closeness(time: LocalTime, edge: LocalTime, peak: LocalTime): Float {
    val total = Duration.between(edge, peak).abs().seconds.toFloat()
    if (total == 0f) return 1f
    return (Duration.between(edge, time).abs().seconds / total).coerceIn(0f, 1f)
}

// ----- Drawing -----

private val SunsetOrange = Color(0xFFFB6A00)
private val NightTop = Color(0xFF07111F)
private val MoonColor = Color(0xFFF1F4FA)

// Stars: where they are (as a fraction of the band's width and height) and how big (dp).
private val STARS = listOf(
    Triple(0.50f, 0.20f, 1.5f), Triple(0.62f, 0.58f, 1.2f), Triple(0.71f, 0.16f, 2f),
    Triple(0.90f, 0.78f, 1.5f), Triple(0.45f, 0.74f, 1.2f), Triple(0.96f, 0.30f, 1.2f),
    Triple(0.80f, 0.50f, 1f), Triple(0.57f, 0.38f, 1f)
)

// The whole band: a blue gradient (darker at night) with the sun, or the moon and stars.
fun DrawScope.drawSky(sky: Sky) {
    val band = if (sky.night) listOf(NightTop, Navy) else listOf(Navy, NavyLight)
    drawRect(Brush.verticalGradient(band))
    clipRect { // nothing (like the sun's glow) may spill outside the band
        if (sky.night) drawMoonAndStars() else drawSun(sky.sunBrightness)
    }
}

private fun DrawScope.drawSun(brightness: Float) {
    val b = brightness * brightness * (3 - 2 * brightness) // smooth: changes slowly near 0 and 1
    // Low (partly set) at sunrise/sunset, high at 13:00; orange when low, yellow when high.
    val center = Offset(size.width - 60.dp.toPx(), mix(size.height - 10.dp.toPx(), 100.dp.toPx(), b))
    val color = lerp(SunsetOrange, Sun, b)
    val glowRadius = mix(100.dp.toPx(), 170.dp.toPx(), b)
    drawCircle(
        brush = Brush.radialGradient(listOf(color.copy(alpha = 0.2f + 0.3f * b), Color.Transparent), center, glowRadius),
        radius = glowRadius,
        center = center
    )
    drawCircle(color, radius = 28.dp.toPx(), center = center)
}

private fun DrawScope.drawMoonAndStars() {
    STARS.forEach { (x, y, radius) ->
        drawCircle(Color.White.copy(alpha = 0.7f), radius.dp.toPx(), Offset(size.width * x, size.height * y))
    }
    val center = Offset(size.width - 60.dp.toPx(), 100.dp.toPx())
    val radius = 26.dp.toPx()
    val glowRadius = 120.dp.toPx()
    drawCircle(
        brush = Brush.radialGradient(listOf(Color.White.copy(alpha = 0.16f), Color.Transparent), center, glowRadius),
        radius = glowRadius,
        center = center
    )
    // A crescent: draw the full moon on its own layer, then erase a circle shifted to the upper
    // right (BlendMode.DstOut = "remove what's already there"). The layer keeps the erasing to
    // the moon itself; without it, it would cut a hole through the sky too.
    drawContext.canvas.saveLayer(Rect(center, radius * 1.2f), Paint())
    drawCircle(MoonColor, radius, center)
    drawCircle(
        color = Color.Black,
        radius = radius * 0.85f,
        center = center + Offset(radius * 0.45f, -radius * 0.3f),
        blendMode = BlendMode.DstOut
    )
    drawContext.canvas.restore()
}

private fun mix(from: Float, to: Float, amount: Float) = from + (to - from) * amount
