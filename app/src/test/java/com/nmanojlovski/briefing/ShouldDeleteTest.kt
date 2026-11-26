package com.nmanojlovski.briefing

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

// Which saved files the clean-up deletes. Right-click this file → Run.
class ShouldDeleteTest {
    private val today = LocalDate.of(2026, 11, 26)

    @Test
    fun keepsTheLastTenDays() {
        assertFalse(shouldDelete("2026-11-26.html", emptySet(), today))
        assertFalse(shouldDelete("2026-11-16.html", emptySet(), today)) // exactly 10 days old
    }

    @Test
    fun deletesOlderOnes() {
        assertTrue(shouldDelete("2026-11-15.html", emptySet(), today))
        assertTrue(shouldDelete("2025-01-01.html", emptySet(), today))
    }

    @Test
    fun keepsArchivedOnesForever() {
        assertFalse(shouldDelete("2025-01-01.html", setOf("2025-01-01.html"), today))
    }

    @Test
    fun leavesOtherFilesAlone() {
        assertFalse(shouldDelete("index.json", emptySet(), today))
        assertFalse(shouldDelete("notes.html", emptySet(), today))
    }
}
