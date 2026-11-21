package dev.neonlauncher.core

import android.content.Context
import android.content.SharedPreferences

/**
 * Installs an uncaught exception handler that persists crash timestamps synchronously
 * using [SharedPreferences.Editor.commit] and disables `.HomeAlias` after 3 crashes
 * within 2 minutes so the TV remains accessible.
 */
object SafeModeManager {
    private const val PREFS_NAME = "neon_safe_mode"
    private const val KEY_CRASH_TIMESTAMPS = "crash_timestamps"
    private const val KEY_SAFE_MODE_PENDING = "safe_mode_pending"
    private const val KEY_CRASH_SUMMARY = "last_crash_summary"
    private const val MAX_SUMMARY_LENGTH = 280

    @Volatile
    private var installed = false

    /** Installs the chained [Thread.UncaughtExceptionHandler]. Safe to call multiple times. */
    fun install(context: Context) {
        if (installed) return
        synchronized(this) {
            if (installed) return
            val appContext = context.applicationContext
            val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                try {
                    recordUncaughtCrash(appContext, throwable)
                } catch (_: Throwable) {
                    // Never mask the original crash if recovery bookkeeping itself fails.
                } finally {
                    previousHandler?.uncaughtException(thread, throwable)
                }
            }
            installed = true
        }
    }

    fun isSafeModePending(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SAFE_MODE_PENDING, false)

    fun getCrashSummary(context: Context): String? =
        prefs(context).getString(KEY_CRASH_SUMMARY, null)?.takeIf { it.isNotBlank() }

    /** Clears pending Safe Mode state and crash history synchronously. */
    fun clearSafeMode(context: Context) {
        prefs(context).edit()
            .remove(KEY_SAFE_MODE_PENDING)
            .remove(KEY_CRASH_TIMESTAMPS)
            .remove(KEY_CRASH_SUMMARY)
            .commit()
    }

    private fun recordUncaughtCrash(context: Context, throwable: Throwable) {
        val sp = prefs(context)
        val existing = sp.getString(KEY_CRASH_TIMESTAMPS, null)
            ?.split(',')
            ?.mapNotNull { it.trim().toLongOrNull() }
            .orEmpty()

        val (updated, triggeredSafeMode) = SafeModeTracker.evaluate(
            existingTimestamps = existing,
            nowMs = System.currentTimeMillis(),
        )
        val summary = formatCrashSummary(throwable)

        val editor = sp.edit()
            .putString(KEY_CRASH_SUMMARY, summary)

        if (triggeredSafeMode) {
            editor
                .putBoolean(KEY_SAFE_MODE_PENDING, true)
                .remove(KEY_CRASH_TIMESTAMPS)
            editor.commit()
            runCatching { HomeAliasManager.setHomeAliasEnabled(context, false) }
        } else {
            editor
                .putBoolean(KEY_SAFE_MODE_PENDING, sp.getBoolean(KEY_SAFE_MODE_PENDING, false))
                .putString(KEY_CRASH_TIMESTAMPS, updated.joinToString(","))
            editor.commit()
        }
    }

    private fun formatCrashSummary(throwable: Throwable): String {
        val type = throwable::class.java.simpleName.ifBlank { "Exception" }
        val message = throwable.message?.trim()?.takeIf { it.isNotEmpty() }
        val raw = if (message != null) "$type: $message" else type
        return if (raw.length > MAX_SUMMARY_LENGTH) {
            raw.take(MAX_SUMMARY_LENGTH - 1) + "…"
        } else {
            raw
        }
    }

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
