package com.nmanojlovski.briefing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.abs

// Sunrise, sunset and the sun/moon in the main menu. Right-click this file → Run.
// The expected times are Skopje's published sunrise/sunset times; the formula is allowed
// to be a few minutes off.
class SkyTest {
    private val skopje = ZoneId.of("Europe/Skopje")

    private fun assertAbout(expectedMinutes: Int, actualMinutes: Long, tolerance: Int) {
        assertTrue(
            "expected about $expectedMinutes minutes, got $actualMinutes",
            abs(actualMinutes - expectedMinutes) <= tolerance
        )
    }

    private fun minutesOfDay(time: LocalTime): Long = time.toSecondOfDay() / 60L

    @Test
    fun shortestAndLongestDay() {
        val winter = sunTimes(LocalDate.of(2026, 12, 21), skopje)
        val summer = sunTimes(LocalDate.of(2026, 6, 21), skopje)
        assertAbout(9 * 60 + 10, Duration.between(winter.sunrise, winter.sunset).toMinutes(), tolerance = 10)
        assertAbout(15 * 60 + 11, Duration.between(summer.sunrise, summer.sunset).toMinutes(), tolerance = 10)
    }

    @Test
    fun lateNovember_sunriseBeforeSeven_sunsetAroundFour() {
        val times = sunTimes(LocalDate.of(2026, 11, 26), skopje)
        assertAbout(6 * 60 + 44, minutesOfDay(times.sunrise), tolerance = 15)
        assertAbout(16 * 60 + 4, minutesOfDay(times.sunset), tolerance = 15)
    }

    @Test
    fun summerTimeIsIncluded() {
        // In June the clocks are one hour ahead, so sunset is around 20:20.
        val times = sunTimes(LocalDate.of(2026, 6, 21), skopje)
        assertAbout(20 * 60 + 20, minutesOfDay(times.sunset), tolerance = 15)
    }

    @Test
    fun brightestAtOnePm_dimmerBeforeAndAfter() {
        fun brightness(hour: Int) = skyAt(LocalDateTime.of(2026, 11, 26, hour, 0), skopje).sunBrightness
        assertEquals(1f, brightness(13), 0.001f)
        assertTrue(brightness(9) < brightness(11))
        assertTrue(brightness(11) < brightness(13))
        assertTrue(brightness(15) < brightness(13))
        assertTrue(brightness(15) > 0f)
    }

    @Test
    fun moonAtNight_sunInTheDay() {
        assertTrue(skyAt(LocalDateTime.of(2026, 11, 26, 18, 30), skopje).night) // dark in November
        assertTrue(skyAt(LocalDateTime.of(2026, 11, 26, 3, 0), skopje).night)
        assertFalse(skyAt(LocalDateTime.of(2026, 11, 26, 12, 0), skopje).night)
        assertFalse(skyAt(LocalDateTime.of(2026, 6, 21, 20, 0), skopje).night)  // still light in June
    }
}
