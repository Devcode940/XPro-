package com.example.ui.components

import androidx.annotation.DrawableRes
import com.example.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil

fun formatCompactCount(count: Int): String {
    return when {
        count >= 1_000_000 -> String.format(Locale.US, "%.1fM", count / 1_000_000f).replace(".0M", "M")
        count >= 1_000 -> String.format(Locale.US, "%.1fK", count / 1_000f).replace(".0K", "K")
        else -> count.toString()
    }
}

fun formatRelativeTime(timestampMillis: Long): String {
    val diff = (System.currentTimeMillis() - timestampMillis).coerceAtLeast(0L)
    val seconds = diff / 1000L
    val minutes = seconds / 60L
    val hours = minutes / 60L
    val days = hours / 24L
    return when {
        seconds < 60 -> "${seconds.coerceAtLeast(1)}s"
        minutes < 60 -> "${minutes}m"
        hours < 24 -> "${hours}h"
        days < 7 -> "${days}d"
        else -> SimpleDateFormat("MMM d", Locale.US).format(Date(timestampMillis))
    }
}

fun formatTimeClock(timestampMillis: Long): String {
    return SimpleDateFormat("h:mm a · MMM d", Locale.US).format(Date(timestampMillis))
}

fun estimateReadingStats(text: String): Pair<Int, Int> {
    val words = text.trim().split(Regex("\\s+")).count { it.isNotBlank() }
    val minutes = ceil(words / 200.0).toInt().coerceAtLeast(1)
    return minutes to words
}

@DrawableRes
fun resolveBundledMedia(key: String?): Int? {
    return when (key) {
        "article_cover_1" -> R.drawable.img_article_cover_1
        "media_post_1" -> R.drawable.img_media_post_1
        "profile_banner" -> R.drawable.img_profile_banner
        "user_avatar" -> R.drawable.img_user_avatar
        else -> null
    }
}
