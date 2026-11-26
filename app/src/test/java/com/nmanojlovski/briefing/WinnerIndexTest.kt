package com.nmanojlovski.briefing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Checks which slice ends up under the pointer. Right-click this file → Run.
class WinnerIndexTest {
    @Test
    fun notTurned_firstSliceIsUnderPointer() {
        assertEquals(0, winnerIndex(0f, count = 4))
    }

    @Test
    fun turningClockwise_bringsEarlierSlicesUnderPointer() {
        // 4 names: each slice is 90°. Turning clockwise moves slice 3, then 2, ... under the pointer.
        assertEquals(3, winnerIndex(45f, count = 4))
        assertEquals(2, winnerIndex(135f, count = 4))
        assertEquals(1, winnerIndex(225f, count = 4))
        assertEquals(0, winnerIndex(359f, count = 4))
    }

    @Test
    fun fullTurnsDontMatter() {
        assertEquals(winnerIndex(45f, count = 4), winnerIndex(5 * 360f + 45f, count = 4))
        assertEquals(winnerIndex(100f, count = 7), winnerIndex(7 * 360f + 100f, count = 7))
    }

    @Test
    fun alwaysAValidIndex() {
        for (count in 2..20) {
            var angle = 0f
            while (angle < 720f) {
                val index = winnerIndex(angle, count)
                assertTrue("angle $angle, count $count gave $index", index in 0 until count)
                angle += 0.5f
            }
        }
    }
}
