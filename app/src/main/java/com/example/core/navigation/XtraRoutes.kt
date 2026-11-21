package com.example.core.navigation

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

    fun profile(userId: String = "current_user") = "profile/$userId"
    fun chatThread(conversationId: String) = "chat/$conversationId"
    fun settings(section: String = SETTINGS_OVERVIEW) = "settings?section=$section"

    val topLevelTabRoutes = setOf(HOME, EXPLORE, NOTIFICATIONS, MESSAGES)
}

fun NavHostController.navigateSingleTopTo(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
