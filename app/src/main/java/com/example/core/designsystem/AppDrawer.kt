package com.example.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FeaturedPlayList
import androidx.compose.material.icons.automirrored.filled.HelpCenter
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.outlined.Login
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.navigation.XtraRoutes
import com.example.data.model.UserProfile

@Composable
fun AppDrawerContent(
    currentRoute: String?,
    userProfile: UserProfile,
    isAuthenticated: Boolean,
    authEmail: String?,
    unreadNotificationsCount: Int,
    unreadMessagesCount: Int,
    activeDownloadsCount: Int,
    onNavigate: (String) -> Unit,
    onSwitchAccount: () -> Unit,
    onAddAccount: () -> Unit,
    onSignInClick: () -> Unit,
    onLogOutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    var socialExpanded by rememberSaveable { mutableStateOf(true) }
    var mediaExpanded by rememberSaveable { mutableStateOf(true) }
    var toolsExpanded by rememberSaveable { mutableStateOf(false) }
    var settingsExpanded by rememberSaveable { mutableStateOf(false) }

    ModalDrawerSheet(
        drawerContainerColor = colorScheme.surface,
        drawerContentColor = colorScheme.onSurface,
        windowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier
            .widthIn(max = 330.dp)
            .fillMaxHeight()
            .testTag("app_drawer_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colorScheme.surface)
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Top + WindowInsetsSides.Bottom + WindowInsetsSides.Start
                    )
                )
                .verticalScroll(rememberScrollState())
                .testTag("app_drawer_scroll")
        ) {
            DrawerHeader(
                userProfile = userProfile,
                isAuthenticated = isAuthenticated,
                authEmail = authEmail,
                onViewProfile = { onNavigate(XtraRoutes.profile("current_user")) },
                onSwitchAccount = onSwitchAccount,
                onAddAccount = onAddAccount
            )
            HorizontalDivider(color = colorScheme.outline, thickness = 0.8.dp)

            // SOCIAL SECTION
            DrawerSection(
                title = "Social",
                expanded = socialExpanded,
                onToggleExpanded = { socialExpanded = !socialExpanded },
                testTagId = "drawer_section_social"
            ) {
                DrawerMenuItem(
                    title = "Home",
                    icon = Icons.Filled.Home,
                    selected = currentRoute == XtraRoutes.HOME,
                    onClick = { onNavigate(XtraRoutes.HOME) },
                    testTagId = "drawer_item_home"
                )
                DrawerMenuItem(
                    title = "Explore",
                    icon = Icons.Filled.Explore,
                    selected = currentRoute == XtraRoutes.EXPLORE,
                    onClick = { onNavigate(XtraRoutes.EXPLORE) },
                    testTagId = "drawer_item_explore"
                )
                DrawerMenuItem(
                    title = "Search",
                    icon = Icons.Filled.Search,
                    selected = currentRoute == XtraRoutes.SEARCH,
                    onClick = { onNavigate(XtraRoutes.SEARCH) },
                    testTagId = "drawer_item_search"
                )
                DrawerMenuItem(
                    title = "Notifications",
                    icon = Icons.Filled.Notifications,
                    selected = currentRoute == XtraRoutes.NOTIFICATIONS,
                    badgeText = unreadNotificationsCount.takeIf { it > 0 }?.toString(),
                    onClick = { onNavigate(XtraRoutes.NOTIFICATIONS) },
                    testTagId = "drawer_item_notifications"
                )
                DrawerMenuItem(
                    title = "Messages",
                    icon = Icons.Filled.Mail,
                    selected = currentRoute == XtraRoutes.MESSAGES || currentRoute?.startsWith("chat/") == true,
                    badgeText = unreadMessagesCount.takeIf { it > 0 }?.toString(),
                    onClick = { onNavigate(XtraRoutes.MESSAGES) },
                    testTagId = "drawer_item_messages"
                )
                DrawerMenuItem(
                    title = "Bookmarks",
                    icon = Icons.Filled.Bookmark,
                    selected = currentRoute == XtraRoutes.BOOKMARKS,
                    onClick = { onNavigate(XtraRoutes.BOOKMARKS) },
                    testTagId = "drawer_item_bookmarks"
                )
                DrawerMenuItem(
                    title = "Lists",
                    icon = Icons.AutoMirrored.Filled.ListAlt,
                    selected = currentRoute == XtraRoutes.LISTS,
                    onClick = { onNavigate(XtraRoutes.LISTS) },
                    testTagId = "drawer_item_lists"
                )
                DrawerMenuItem(
                    title = "Communities",
                    icon = Icons.Filled.Groups,
                    selected = currentRoute == XtraRoutes.COMMUNITIES,
                    onClick = { onNavigate(XtraRoutes.COMMUNITIES) },
                    testTagId = "drawer_item_communities"
                )
                DrawerMenuItem(
                    title = "Trending",
                    icon = Icons.Filled.TrendingUp,
                    selected = currentRoute == XtraRoutes.TRENDING,
                    onClick = { onNavigate(XtraRoutes.TRENDING) },
                    testTagId = "drawer_item_trending"
                )
                DrawerMenuItem(
                    title = "Articles & Threads",
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    selected = currentRoute == XtraRoutes.ARTICLES,
                    onClick = { onNavigate(XtraRoutes.ARTICLES) },
                    testTagId = "drawer_item_articles"
                )
                DrawerMenuItem(
                    title = "Grok AI (Powered by X)",
                    icon = Icons.Filled.AutoAwesome,
                    selected = currentRoute == XtraRoutes.GROK,
                    onClick = { onNavigate(XtraRoutes.GROK) },
                    testTagId = "drawer_item_grok"
                )
            }

            HorizontalDivider(
                color = colorScheme.outline,
                thickness = 0.8.dp,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // MEDIA CENTER
            DrawerSection(
                title = "Media Center",
                expanded = mediaExpanded,
                onToggleExpanded = { mediaExpanded = !mediaExpanded },
                testTagId = "drawer_section_media"
            ) {
                DrawerMenuItem(
                    title = "Media Library",
                    icon = Icons.Filled.VideoLibrary,
                    selected = currentRoute == XtraRoutes.MEDIA_LIBRARY,
                    onClick = { onNavigate(XtraRoutes.MEDIA_LIBRARY) },
                    testTagId = "drawer_item_media_library"
                )
                DrawerMenuItem(
                    title = "Downloads",
                    icon = Icons.Filled.Download,
                    selected = currentRoute == XtraRoutes.DOWNLOADS,
                    badgeText = activeDownloadsCount.takeIf { it > 0 }?.toString(),
                    onClick = { onNavigate(XtraRoutes.DOWNLOADS) },
                    testTagId = "drawer_item_downloads"
                )
                DrawerMenuItem(
                    title = "Download Manager",
                    icon = Icons.Filled.CloudDownload,
                    selected = currentRoute == XtraRoutes.DOWNLOAD_MANAGER,
                    onClick = { onNavigate(XtraRoutes.DOWNLOAD_MANAGER) },
                    testTagId = "drawer_item_download_manager"
                )
                DrawerMenuItem(
                    title = "Video Player",
                    icon = Icons.Filled.PlayCircle,
                    selected = currentRoute == XtraRoutes.VIDEO_PLAYER,
                    onClick = { onNavigate(XtraRoutes.VIDEO_PLAYER) },
                    testTagId = "drawer_item_video_player"
                )
                DrawerMenuItem(
                    title = "Audio Player",
                    icon = Icons.Filled.MusicNote,
                    selected = currentRoute == XtraRoutes.AUDIO_PLAYER,
                    onClick = { onNavigate(XtraRoutes.AUDIO_PLAYER) },
                    testTagId = "drawer_item_audio_player"
                )
                DrawerMenuItem(
                    title = "Playlists",
                    icon = Icons.AutoMirrored.Filled.FeaturedPlayList,
                    selected = currentRoute == XtraRoutes.PLAYLISTS,
                    onClick = { onNavigate(XtraRoutes.PLAYLISTS) },
                    testTagId = "drawer_item_playlists"
                )
                DrawerMenuItem(
                    title = "Recently Played",
                    icon = Icons.Filled.QueueMusic,
                    selected = currentRoute == XtraRoutes.RECENTLY_PLAYED,
                    onClick = { onNavigate(XtraRoutes.RECENTLY_PLAYED) },
                    testTagId = "drawer_item_recently_played"
                )
                DrawerMenuItem(
                    title = "Favorites",
                    icon = Icons.Filled.Favorite,
                    selected = currentRoute == XtraRoutes.FAVORITES,
                    onClick = { onNavigate(XtraRoutes.FAVORITES) },
                    testTagId = "drawer_item_favorites"
                )
            }

            HorizontalDivider(
                color = colorScheme.outline,
                thickness = 0.8.dp,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // APPLICATION TOOLS
            DrawerSection(
                title = "Application Tools",
                expanded = toolsExpanded,
                onToggleExpanded = { toolsExpanded = !toolsExpanded },
                testTagId = "drawer_section_tools"
            ) {
                DrawerMenuItem(
                    title = "QR Code Scanner",
                    icon = Icons.Filled.QrCodeScanner,
                    selected = currentRoute == XtraRoutes.QR_SCANNER,
                    onClick = { onNavigate(XtraRoutes.QR_SCANNER) },
                    testTagId = "drawer_item_qr_scanner"
                )
                DrawerMenuItem(
                    title = "Saved Pages",
                    icon = Icons.Filled.BookmarkAdded,
                    selected = currentRoute == XtraRoutes.SAVED_PAGES,
                    onClick = { onNavigate(XtraRoutes.SAVED_PAGES) },
                    testTagId = "drawer_item_saved_pages"
                )
                DrawerMenuItem(
                    title = "Reading List",
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    selected = currentRoute == XtraRoutes.READING_LIST,
                    onClick = { onNavigate(XtraRoutes.READING_LIST) },
                    testTagId = "drawer_item_reading_list"
                )
                DrawerMenuItem(
                    title = "Browser History",
                    icon = Icons.Filled.History,
                    selected = currentRoute == XtraRoutes.BROWSER_HISTORY,
                    onClick = { onNavigate(XtraRoutes.BROWSER_HISTORY) },
                    testTagId = "drawer_item_browser_history"
                )
                DrawerMenuItem(
                    title = "Private Browsing",
                    icon = Icons.Filled.VisibilityOff,
                    selected = currentRoute == XtraRoutes.PRIVATE_BROWSING,
                    onClick = { onNavigate(XtraRoutes.PRIVATE_BROWSING) },
                    testTagId = "drawer_item_private_browsing"
                )
                DrawerMenuItem(
                    title = "Media Sharing",
                    icon = Icons.Filled.Share,
                    selected = currentRoute == XtraRoutes.MEDIA_SHARING,
                    onClick = { onNavigate(XtraRoutes.MEDIA_SHARING) },
                    testTagId = "drawer_item_media_sharing"
                )
            }

            HorizontalDivider(
                color = colorScheme.outline,
                thickness = 0.8.dp,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // SETTINGS & SUPPORT
            DrawerSection(
                title = "Settings & Support",
                expanded = settingsExpanded,
                onToggleExpanded = { settingsExpanded = !settingsExpanded },
                testTagId = "drawer_section_settings"
            ) {
                DrawerMenuItem(
                    title = "Settings and Privacy",
                    icon = Icons.Filled.Settings,
                    selected = currentRoute == XtraRoutes.settings(XtraRoutes.SETTINGS_OVERVIEW),
                    onClick = { onNavigate(XtraRoutes.settings(XtraRoutes.SETTINGS_OVERVIEW)) },
                    testTagId = "drawer_item_settings_privacy"
                )
                DrawerMenuItem(
                    title = "Account Settings",
                    icon = Icons.Filled.ManageAccounts,
                    selected = currentRoute == XtraRoutes.settings(XtraRoutes.SETTINGS_ACCOUNT),
                    onClick = { onNavigate(XtraRoutes.settings(XtraRoutes.SETTINGS_ACCOUNT)) },
                    testTagId = "drawer_item_account_settings"
                )
                DrawerMenuItem(
                    title = "Appearance and Themes",
                    icon = Icons.Filled.Palette,
                    selected = currentRoute == XtraRoutes.settings(XtraRoutes.SETTINGS_APPEARANCE),
                    onClick = { onNavigate(XtraRoutes.settings(XtraRoutes.SETTINGS_APPEARANCE)) },
                    testTagId = "drawer_item_appearance"
                )
                DrawerMenuItem(
                    title = "Notification Settings",
                    icon = Icons.Filled.NotificationsActive,
                    selected = currentRoute == XtraRoutes.settings(XtraRoutes.SETTINGS_NOTIFICATIONS),
                    onClick = { onNavigate(XtraRoutes.settings(XtraRoutes.SETTINGS_NOTIFICATIONS)) },
                    testTagId = "drawer_item_notification_settings"
                )
                DrawerMenuItem(
                    title = "Privacy and Security",
                    icon = Icons.Filled.Security,
                    selected = currentRoute == XtraRoutes.settings(XtraRoutes.SETTINGS_PRIVACY),
                    onClick = { onNavigate(XtraRoutes.settings(XtraRoutes.SETTINGS_PRIVACY)) },
                    testTagId = "drawer_item_privacy_security"
                )
                DrawerMenuItem(
                    title = "Data Usage",
                    icon = Icons.Filled.DataUsage,
                    selected = currentRoute == XtraRoutes.settings(XtraRoutes.SETTINGS_DATA_USAGE),
                    onClick = { onNavigate(XtraRoutes.settings(XtraRoutes.SETTINGS_DATA_USAGE)) },
                    testTagId = "drawer_item_data_usage"
                )
                DrawerMenuItem(
                    title = "Storage Management",
                    icon = Icons.Filled.Storage,
                    selected = currentRoute == XtraRoutes.settings(XtraRoutes.SETTINGS_STORAGE),
                    onClick = { onNavigate(XtraRoutes.settings(XtraRoutes.SETTINGS_STORAGE)) },
                    testTagId = "drawer_item_storage_management"
                )
                DrawerMenuItem(
                    title = "Language",
                    icon = Icons.Filled.Language,
                    selected = currentRoute == XtraRoutes.settings(XtraRoutes.SETTINGS_LANGUAGE),
                    onClick = { onNavigate(XtraRoutes.settings(XtraRoutes.SETTINGS_LANGUAGE)) },
                    testTagId = "drawer_item_language"
                )
                DrawerMenuItem(
                    title = "Accessibility",
                    icon = Icons.Filled.AccessibilityNew,
                    selected = currentRoute == XtraRoutes.settings(XtraRoutes.SETTINGS_ACCESSIBILITY),
                    onClick = { onNavigate(XtraRoutes.settings(XtraRoutes.SETTINGS_ACCESSIBILITY)) },
                    testTagId = "drawer_item_accessibility"
                )
                DrawerMenuItem(
                    title = "Help Center",
                    icon = Icons.AutoMirrored.Filled.HelpCenter,
                    selected = currentRoute == XtraRoutes.HELP_CENTER,
                    onClick = { onNavigate(XtraRoutes.HELP_CENTER) },
                    testTagId = "drawer_item_help_center"
                )
                DrawerMenuItem(
                    title = "About XTRA",
                    icon = Icons.Filled.Info,
                    selected = currentRoute == XtraRoutes.ABOUT_XTRA,
                    onClick = { onNavigate(XtraRoutes.ABOUT_XTRA) },
                    testTagId = "drawer_item_about_xtra"
                )
                DrawerMenuItem(
                    title = "Send Feedback",
                    icon = Icons.Filled.Feedback,
                    selected = currentRoute == XtraRoutes.SEND_FEEDBACK,
                    onClick = { onNavigate(XtraRoutes.SEND_FEEDBACK) },
                    testTagId = "drawer_item_send_feedback"
                )
            }

            HorizontalDivider(
                color = colorScheme.outline,
                thickness = 0.8.dp,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // ACCOUNT ACTIONS
            Text(
                text = "ACCOUNT ACTIONS",
                style = MaterialTheme.typography.labelSmall,
                color = colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)
            )
            if (!isAuthenticated) {
                DrawerMenuItem(
                    title = "Guest Account · Sign In with Google",
                    icon = Icons.Outlined.Login,
                    selected = false,
                    onClick = onSignInClick,
                    tintOverride = colorScheme.primary,
                    testTagId = "drawer_action_sign_in"
                )
            } else {
                DrawerMenuItem(
                    title = "Signed In · Switch Google Account",
                    icon = Icons.Filled.AccountCircle,
                    selected = false,
                    onClick = onSignInClick,
                    tintOverride = colorScheme.tertiary,
                    testTagId = "drawer_action_signed_in"
                )
            }
            DrawerMenuItem(
                title = "Log Out",
                icon = Icons.AutoMirrored.Filled.Logout,
                selected = false,
                onClick = onLogOutClick,
                tintOverride = colorScheme.error,
                testTagId = "drawer_action_logout"
            )
            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}
