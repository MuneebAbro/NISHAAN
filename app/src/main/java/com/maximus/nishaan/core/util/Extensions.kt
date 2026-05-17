package com.maximus.nishaan.core.util

import java.util.concurrent.TimeUnit

/** Common extension functions used across the app. */

/** Formats a timestamp (millis) as a human-readable "X ago" string. */
fun Long.toTimeAgo(): String {
    val now = System.currentTimeMillis()
    val diff = now - this
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    val days = TimeUnit.MILLISECONDS.toDays(diff)

    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "${minutes} min ago"
        hours < 24 -> "${hours} hr ago"
        else -> "${days} days ago"
    }
}

/** Formats a confidence score as a display string, e.g. "87%". */
fun Int.toConfidenceString(): String = "$this%"

/** Truncates a string to the given max length with ellipsis. */
fun String.truncate(maxLength: Int): String {
    return if (length <= maxLength) this else "${take(maxLength - 1)}…"
}

/** Returns the first 8 characters of a string (used for report ID display). */
fun String.toShortId(): String = take(8)
