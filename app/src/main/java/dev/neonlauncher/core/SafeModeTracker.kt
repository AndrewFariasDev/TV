package dev.neonlauncher.core

/**
 * Pure Kotlin sliding-window crash counter for Safe Mode detection.
 * Triggers Safe Mode when [threshold] crashes occur within [windowMs] milliseconds.
 */
class SafeModeTracker(
    private val windowMs: Long = DEFAULT_WINDOW_MS,
    private val threshold: Int = DEFAULT_THRESHOLD,
) {
    private val timestamps = mutableListOf<Long>()

    /** Records a crash at [nowMs] and returns `true` if Safe Mode should trigger. */
    fun recordCrash(nowMs: Long): Boolean {
        val (updated, triggered) = evaluate(timestamps, nowMs, windowMs, threshold)
        timestamps.clear()
        timestamps.addAll(updated)
        return triggered
    }

    /** Returns the number of crashes currently inside the active sliding window at [nowMs]. */
    fun recentCrashCount(nowMs: Long): Int {
        timestamps.removeAll { ts -> ts > nowMs || (nowMs - ts) > windowMs }
        return timestamps.size
    }

    /** Clears all tracked crash timestamps. */
    fun reset() {
        timestamps.clear()
    }

    companion object {
        const val DEFAULT_WINDOW_MS: Long = 120_000L
        const val DEFAULT_THRESHOLD: Int = 3

        /**
         * Stateless evaluation helper used by persisted crash tracking.
         * Returns the pruned+appended timestamps and whether [threshold] was reached.
         */
        fun evaluate(
            existingTimestamps: List<Long>,
            nowMs: Long,
            windowMs: Long = DEFAULT_WINDOW_MS,
            threshold: Int = DEFAULT_THRESHOLD,
        ): Pair<List<Long>, Boolean> {
            val active = existingTimestamps.filter { ts ->
                ts <= nowMs && (nowMs - ts) <= windowMs
            } + nowMs
            return active to (active.size >= threshold)
        }
    }
}
