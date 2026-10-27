package com.avioiva.player.util

import java.util.Locale

/** Small formatting helpers shared across the UI. */
object Format {

    /** 201000 -> "3:21"  |  3_601_000 -> "1:00:01" */
    fun time(ms: Long): String {
        if (ms <= 0L) return "0:00"
        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%d:%02d", minutes, seconds)
        }
    }

    fun bytes(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return when {
            mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
            kb >= 1.0 -> String.format(Locale.US, "%.0f KB", kb)
            else -> "$bytes B"
        }
    }

    /** 60_000 -> "1 min", 90_000 -> "1.5 min" (sleep timer labels). */
    fun minutes(ms: Long): String =
        if (ms % 60_000L == 0L) "${ms / 60_000L} min"
        else String.format(Locale.US, "%.1f min", ms / 60_000.0)
}
