package com.example.core.navigation

import android.net.Uri
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController

object XtraRoutes {
    // Bottom Navigation & Core Social
    const val HOME = "home"
    const val EXPLORE = "explore"
    const val SEARCH = "search"
    const val NOTIFICATIONS = "notifications"
    const val MESSAGES = "messages"
    const val CHAT_THREAD = "chat/{conversationId}"
    const val BOOKMARKS = "bookmarks"
    const val LISTS = "lists"
    const val COMMUNITIES = "communities"
    const val TRENDING = "trending"
    const val ARTICLES = "articles"
    const val GROK = "grok"

    // Profile & Account
    const val PROFILE = "profile/{userId}"

    // Media Center
    const val MEDIA_LIBRARY = "media_library"
    const val DOWNLOADS = "downloads"
    const val DOWNLOAD_MANAGER = "download_manager"
    const val VIDEO_PLAYER = "video_player"
    const val AUDIO_PLAYER = "audio_player"
    const val PLAYLISTS = "playlists"
    const val RECENTLY_PLAYED = "recently_played"
    const val FAVORITES = "favorites"

    // Application Tools
    const val QR_SCANNER = "qr_scanner"
    const val SAVED_PAGES = "saved_pages"
    const val READING_LIST = "reading_list"
    const val BROWSER_HISTORY = "browser_history"
    const val PRIVATE_BROWSING = "private_browsing"
    const val MEDIA_SHARING = "media_sharing"

    // Settings & Support (deep-links into specific Settings sections)
    const val SETTINGS_ROUTE = "settings?section={section}"
    const val SETTINGS_OVERVIEW = "overview"
    const val SETTINGS_ACCOUNT = "account"
    const val SETTINGS_APPEARANCE = "appearance"
    const val SETTINGS_NOTIFICATIONS = "notifications"
    const val SETTINGS_PRIVACY = "privacy"
    const val SETTINGS_DATA_USAGE = "data_usage"
    const val SETTINGS_STORAGE = "storage"
    const val SETTINGS_LANGUAGE = "language"
    const val SETTINGS_ACCESSIBILITY = "accessibility"

    const val HELP_CENTER = "help_center"
    const val ABOUT_XTRA = "about_xtra"
    const val SEND_FEEDBACK = "send_feedback"

    /**
     * Dynamic route segments are percent-encoded: ids come from user data and deep links, and a
     * raw `/`, `?` or `#` in one would otherwise change which destination the route resolves to.
     */
    fun profile(userId: String = "current_user") = "profile/${Uri.encode(userId)}"
    fun chatThread(conversationId: String) = "chat/${Uri.encode(conversationId)}"
    fun settings(section: String = SETTINGS_OVERVIEW) = "settings?section=${Uri.encode(section)}"

    val topLevelTabRoutes = setOf(HOME, EXPLORE, NOTIFICATIONS, MESSAGES)
}

/**
 * Route ids are the only externally reachable arguments (`xtra://profile/{userId}`,
 * `xtra://chat/{conversationId}` are advertised by an exported activity), so anything that does
 * not look like an app id falls back to a safe default instead of being used as-is.
 */
private val ROUTE_ID_PATTERN = Regex("^[A-Za-z0-9._-]{1,64}$")

fun sanitizeRouteId(raw: String?, fallback: String): String =
    raw?.takeIf { ROUTE_ID_PATTERN.matches(it) } ?: fallback

fun NavHostController.navigateSingleTopTo(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
