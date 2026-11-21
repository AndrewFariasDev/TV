package dev.neonlauncher.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SafeModeTrackerTest {
    @Test
    fun fewerThanThreeCrashesDoNotTriggerSafeMode() {
        val tracker = SafeModeTracker()

        assertFalse(tracker.recordCrash(10_000L))
        assertEquals(1, tracker.recentCrashCount(10_000L))

        assertFalse(tracker.recordCrash(45_000L))
        assertEquals(2, tracker.recentCrashCount(45_000L))
    }

    @Test
    fun threeCrashesWithinTwoMinutesTriggerSafeMode() {
        val tracker = SafeModeTracker()

        assertFalse(tracker.recordCrash(1_000L))
        assertFalse(tracker.recordCrash(60_000L))
        assertTrue(tracker.recordCrash(121_000L))
    }

    @Test
    fun crashesSpacedMoreThanTwoMinutesExpireOldEntries() {
        val tracker = SafeModeTracker()

        assertFalse(tracker.recordCrash(0L))
        assertFalse(tracker.recordCrash(30_000L))

        // At 120_001L, crash at 0L is > 120_000ms old and expires; 30_000L remains.
        assertFalse(tracker.recordCrash(120_001L))
        assertEquals(2, tracker.recentCrashCount(120_001L))

        // Next crash at 150_001L expires 30_000L; 120_001L remains -> still only 2.
        assertFalse(tracker.recordCrash(150_001L))
        assertEquals(2, tracker.recentCrashCount(150_001L))

        // Third crash inside the active window triggers Safe Mode.
        assertTrue(tracker.recordCrash(160_000L))
    }

    @Test
    fun resetClearsTrackedCrashes() {
        val tracker = SafeModeTracker()

        assertFalse(tracker.recordCrash(1_000L))
        assertFalse(tracker.recordCrash(2_000L))
        tracker.reset()

        assertEquals(0, tracker.recentCrashCount(2_500L))
        assertFalse(tracker.recordCrash(3_000L))
        assertFalse(tracker.recordCrash(4_000L))
        assertTrue(tracker.recordCrash(5_000L))
    }

    @Test
    fun evaluateStatelessHelperPrunesExpiredTimestamps() {
        val (active, triggered) = SafeModeTracker.evaluate(
            existingTimestamps = listOf(1_000L, 50_000L),
            nowMs = 130_000L,
        )

        assertFalse(triggered)
        assertEquals(listOf(50_000L, 130_000L), active)
    }
}
