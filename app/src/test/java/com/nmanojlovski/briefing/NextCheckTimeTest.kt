package com.nmanojlovski.briefing

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

// Unit tests run on your computer (no phone needed): right-click this file → Run.
class NextCheckTimeTest {
    private fun nov(day: Int, hour: Int, minute: Int = 0) = LocalDateTime.of(2026, 11, day, hour, minute)

    @Test
    fun beforeNine_firstCheckIsAtNine() {
        assertEquals(nov(26, 9), nextCheckTime(nov(26, 0, 5), foundToday = false))
        assertEquals(nov(26, 9), nextCheckTime(nov(26, 8, 59), foundToday = false))
    }

    @Test
    fun betweenNineAndOne_checksEveryFullHour() {
        assertEquals(nov(26, 10), nextCheckTime(nov(26, 9, 0), foundToday = false))
        assertEquals(nov(26, 11), nextCheckTime(nov(26, 10, 3), foundToday = false))
        assertEquals(nov(26, 13), nextCheckTime(nov(26, 12, 59), foundToday = false))
    }

    @Test
    fun afterOne_waitsUntilTomorrowAtNine() {
        assertEquals(nov(27, 9), nextCheckTime(nov(26, 13, 0), foundToday = false))
        assertEquals(nov(27, 9), nextCheckTime(nov(26, 23, 59), foundToday = false))
    }

    @Test
    fun foundToday_stopsUntilTomorrowAtNine() {
        assertEquals(nov(27, 9), nextCheckTime(nov(26, 9, 0), foundToday = true))
        assertEquals(nov(27, 9), nextCheckTime(nov(26, 11, 0), foundToday = true))
    }
}
