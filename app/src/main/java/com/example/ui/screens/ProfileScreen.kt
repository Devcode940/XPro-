package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import com.example.core.scroll.AutoHideHeaderLayout
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.PostItem
import com.example.data.model.ProfileTab
import com.example.ui.components.AdShieldPill
import com.example.ui.components.PostCard
import com.example.ui.components.TopRightMenuButton
import com.example.ui.components.UserAvatar
import com.example.ui.components.VerificationBadgeIcon
import com.example.ui.components.formatCompactCount
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.DangerUndo
import com.example.ui.theme.EmeraldAdShield
import com.example.ui.theme.HairlineDivider
import com.example.ui.theme.MonoTabularStyle
import com.example.ui.theme.OledBlack
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.XBlue
import com.example.ui.theme.XBlueContainer
import com.example.viewmodel.ProfileUiState

import androidx.compose.runtime.saveable.rememberSaveable

@Composable
fun ProfileScreen(
    profileState: ProfileUiState,
    onSelectProfileTab: (ProfileTab) -> Unit,
    onRetryProfile: () -> Unit,
    onBackToOwnProfile: () -> Unit,
    onBackToHome: () -> Unit = {},
    onToggleFollowViewedUser: () -> Unit,
    onOpenPremiumStudio: () -> Unit,
    onOpenAppSettings: () -> Unit = onOpenPremiumStudio,
    onOpenGrok: () -> Unit = {},
    onOpenBookmarks: () -> Unit = {},
    onOpenArticles: () -> Unit = {},
    onOpenMediaDownloader: () -> Unit,
    onToggleAdFree: (Boolean) -> Unit,
    onUpdateUndoDelay: (Int) -> Unit,
    onToggleEditHistoryLinks: (Boolean) -> Unit,
    onToggleAutoExpandThreads: (Boolean) -> Unit,
    onToggleSaveForOffline: (String) -> Unit = {},
    onToggleOfflineRead: (String) -> Unit = {},
    onLike: (String) -> Unit,
    onRepost: (String) -> Unit,
    onReply: (PostItem) -> Unit,
    onBookmark: (PostItem) -> Unit,
    onVotePoll: (String, String) -> Unit,
    onOpenReaderMode: (PostItem) -> Unit,
    onOpenEdit: (PostItem) -> Unit,
    onOpenEditHistory: (PostItem) -> Unit,
    onToggleHighlight: (String) -> Unit,
    onDeletePost: (String) -> Unit,
    onOpenMediaLightbox: (PostItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    var showSettingsExpanded by rememberSaveable { mutableStateOf(false) }

    val onTopBarBack: () -> Unit = when (profileState) {
        is ProfileUiState.Success -> if (profileState.isCurrentUser) onBackToHome else onBackToOwnProfile
        is ProfileUiState.Error -> if (profileState.userId == "current_user") onBackToHome else onBackToOwnProfile
        is ProfileUiState.Loading -> onBackToHome
    }

    if (profileState is ProfileUiState.Success) {
        if (!profileState.isCurrentUser) {
            BackHandler(onBack = onBackToOwnProfile)
        }
        val listState = rememberLazyListState()
        AutoHideHeaderLayout(
            modifier = modifier,
            listState = listState,
            header = {
                ProfileTopBar(
                    profileState = profileState,
                    settingsExpanded = showSettingsExpanded,
                    onBack = onTopBarBack,
                    onRefresh = onRetryProfile,
                    onOpenSettings = onOpenAppSettings,
                    onOpenGrok = onOpenGrok,
                    onOpenProfile = onBackToOwnProfile,
                    onOpenBookmarks = onOpenBookmarks,
                    onOpenArticles = onOpenArticles,
                    onOpenMediaDownloader = onOpenMediaDownloader,
                    onToggleSettings = { showSettingsExpanded = !showSettingsExpanded }
                )
                HorizontalDivider(color = colorScheme.outline, thickness = 0.8.dp)
            }
        ) { scrollModifier, contentPadding ->
            ProfileSuccessContent(
                state = profileState,
                listState = listState,
                contentPadding = contentPadding,
                showSettingsExpanded = showSettingsExpanded,
                onToggleSettingsExpanded = { showSettingsExpanded = !showSettingsExpanded },
                onSelectProfileTab = onSelectProfileTab,
                onToggleFollowViewedUser = onToggleFollowViewedUser,
                onOpenPremiumStudio = onOpenPremiumStudio,
                onOpenMediaDownloader = onOpenMediaDownloader,
                onToggleAdFree = onToggleAdFree,
                onUpdateUndoDelay = onUpdateUndoDelay,
                onToggleEditHistoryLinks = onToggleEditHistoryLinks,
                onToggleAutoExpandThreads = onToggleAutoExpandThreads,
                onToggleSaveForOffline = onToggleSaveForOffline,
                onToggleOfflineRead = onToggleOfflineRead,
                onLike = onLike,
                onRepost = onRepost,
                onReply = onReply,
                onBookmark = onBookmark,
                onVotePoll = onVotePoll,
                onOpenReaderMode = onOpenReaderMode,
                onOpenEdit = onOpenEdit,
                onOpenEditHistory = onOpenEditHistory,
                onToggleHighlight = onToggleHighlight,
                onDeletePost = onDeletePost,
                onOpenMediaLightbox = onOpenMediaLightbox,
                modifier = scrollModifier
            )
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(colorScheme.background)
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            ProfileTopBar(
                profileState = profileState,
                settingsExpanded = showSettingsExpanded,
                onBack = onTopBarBack,
                onRefresh = onRetryProfile,
                onOpenSettings = onOpenAppSettings,
                onOpenGrok = onOpenGrok,
                onOpenProfile = onBackToOwnProfile,
                onOpenBookmarks = onOpenBookmarks,
                onOpenArticles = onOpenArticles,
                onOpenMediaDownloader = onOpenMediaDownloader,
                onToggleSettings = { showSettingsExpanded = !showSettingsExpanded }
            )
            HorizontalDivider(color = colorScheme.outline, thickness = 0.8.dp)

            when (profileState) {
                is ProfileUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .testTag("profile_loading"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = XBlue)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Loading profile…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted
                            )
                        }
                    }
                }

                is ProfileUiState.Error -> {
                    BackHandler(onBack = onTopBarBack)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(32.dp)
                            .testTag("profile_error"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Filled.ErrorOutline,
                                contentDescription = "Profile Error",
                                tint = DangerUndo,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Profile Unavailable",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = profileState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(
                                    onClick = onRetryProfile,
                                    colors = ButtonDefaults.buttonColors(containerColor = XBlue, contentColor = TextPrimary),
                                    modifier = Modifier.testTag("profile_retry_btn")
                                ) {
                                    Text("Retry")
                                }
                                OutlinedButton(
                                    onClick = onBackToOwnProfile,
                                    border = BorderStroke(1.dp, HairlineDivider)
                                ) {
                                    Text("My Profile", color = TextPrimary)
                                }
                            }
                        }
                    }
                }
                is ProfileUiState.Success -> Unit
            }
        }
    }
}

@Composable
private fun ProfileTopBar(
    profileState: ProfileUiState,
    settingsExpanded: Boolean,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenGrok: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenArticles: () -> Unit,
    onOpenMediaDownloader: () -> Unit,
    onToggleSettings: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag("profile_top_bar"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("profile_back_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = colorScheme.onSurface
                )
            }
            Column(modifier = Modifier.padding(start = 2.dp)) {
                when (profileState) {
                    is ProfileUiState.Loading -> {
                        Text("Profile", style = MaterialTheme.typography.titleMedium, color = colorScheme.onSurface)
                        Text("Loading account…", style = MonoTabularStyle.copy(fontSize = 11.sp), color = colorScheme.onSurfaceVariant)
                    }
                    is ProfileUiState.Error -> {
                        Text("Profile Unavailable", style = MaterialTheme.typography.titleMedium, color = DangerUndo)
                        Text("Tap Retry to reload", style = MonoTabularStyle.copy(fontSize = 11.sp), color = colorScheme.onSurfaceVariant)
                    }
                    is ProfileUiState.Success -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(profileState.displayName, style = MaterialTheme.typography.titleMedium, color = colorScheme.onSurface)
                            Spacer(modifier = Modifier.width(4.dp))
                            VerificationBadgeIcon(badge = profileState.badge, size = 15.dp)
                        }
                        Text(
                            text = "${profileState.posts.size} posts · ${profileState.activeTab.title}",
                            style = MonoTabularStyle.copy(fontSize = 11.sp),
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onRefresh,
                modifier = Modifier.testTag("profile_top_refresh_btn")
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "Refresh profile",
                    tint = TextMuted
                )
            }
            IconButton(
                onClick = onOpenMediaDownloader,
                modifier = Modifier.testTag("profile_top_media_btn")
            ) {
                Icon(
                    imageVector = Icons.Filled.Download,
                    contentDescription = "Media Downloader",
                    tint = EmeraldAdShield
                )
            }
            if (profileState is ProfileUiState.Success && profileState.isCurrentUser) {
                IconButton(
                    onClick = onToggleSettings,
                    modifier = Modifier.testTag("profile_top_settings_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "App settings customization",
                        tint = if (settingsExpanded) XBlue else TextPrimary
                    )
                }
            }
            TopRightMenuButton(
                onOpenSettings = onOpenSettings,
                onOpenProfile = onOpenProfile,
                onOpenBookmarks = onOpenBookmarks,
                onOpenArticles = onOpenArticles,
                onOpenMediaDownloader = onOpenMediaDownloader,
                onOpenGrok = onOpenGrok
            )
        }
    }
}

@Composable
private fun ProfileSuccessContent(
    state: ProfileUiState.Success,
    listState: LazyListState,
    contentPadding: PaddingValues,
    showSettingsExpanded: Boolean,
    onToggleSettingsExpanded: () -> Unit,
    onSelectProfileTab: (ProfileTab) -> Unit,
    onToggleFollowViewedUser: () -> Unit,
    onOpenPremiumStudio: () -> Unit,
    onOpenMediaDownloader: () -> Unit,
    onToggleAdFree: (Boolean) -> Unit,
    onUpdateUndoDelay: (Int) -> Unit,
    onToggleEditHistoryLinks: (Boolean) -> Unit,
    onToggleAutoExpandThreads: (Boolean) -> Unit,
    onToggleSaveForOffline: (String) -> Unit,
    onToggleOfflineRead: (String) -> Unit,
    onLike: (String) -> Unit,
    onRepost: (String) -> Unit,
    onReply: (PostItem) -> Unit,
    onBookmark: (PostItem) -> Unit,
    onVotePoll: (String, String) -> Unit,
    onOpenReaderMode: (PostItem) -> Unit,
    onOpenEdit: (PostItem) -> Unit,
    onOpenEditHistory: (PostItem) -> Unit,
    onToggleHighlight: (String) -> Unit,
    onDeletePost: (String) -> Unit,
    onOpenMediaLightbox: (PostItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val folderNameMap = remember(state.bookmarkFolders) {
        state.bookmarkFolders.associate { it.id to it.name }
    }
    val settings = state.currentUserSettings

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .testTag("profile_screen_success"),
        contentPadding = contentPadding
    ) {
        item {
            Box(modifier = Modifier.fillMaxWidth()) {
                Image(
                    painter = painterResource(id = R.drawable.img_profile_banner),
                    contentDescription = "Profile banner",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(145.dp),
                    contentScale = ContentScale.Crop
                )
                AdShieldPill(
                    adFreeEnabled = settings.adFreeEnabled,
                    blockedCount = state.blockedAdsCount,
                    onClick = onOpenPremiumStudio,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                if (state.isCurrentUser) {
                    Image(
                        painter = painterResource(id = R.drawable.img_user_avatar),
                        contentDescription = "${state.displayName} avatar",
                        modifier = Modifier
                            .offset(y = (-34).dp)
                            .size(76.dp)
                            .clip(CircleShape)
                            .border(3.5.dp, OledBlack, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .offset(y = (-34).dp)
                            .clip(CircleShape)
                            .border(3.5.dp, OledBlack, CircleShape)
                    ) {
                        UserAvatar(
                            name = state.displayName,
                            isCurrentUser = false,
                            avatarColorHex = state.avatarColorHex,
                            size = 76.dp
                        )
                    }
                }

                if (state.isCurrentUser) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onOpenMediaDownloader,
                            shape = RoundedCornerShape(999.dp),
                            border = BorderStroke(1.dp, HairlineDivider),
                            modifier = Modifier.testTag("profile_media_downloader_btn")
                        ) {
                            Icon(Icons.Filled.Download, null, tint = EmeraldAdShield, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Media", color = TextPrimary, style = MaterialTheme.typography.labelLarge)
                        }
                        OutlinedButton(
                            onClick = onOpenPremiumStudio,
                            shape = RoundedCornerShape(999.dp),
                            border = BorderStroke(1.dp, HairlineDivider),
                            modifier = Modifier.testTag("open_premium_studio_btn")
                        ) {
                            Icon(Icons.Filled.Tune, null, tint = XBlue, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Studio", color = TextPrimary, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                } else {
                    if (state.isFollowing) {
                        OutlinedButton(
                            onClick = onToggleFollowViewedUser,
                            shape = RoundedCornerShape(999.dp),
                            border = BorderStroke(1.dp, HairlineDivider),
                            modifier = Modifier
                                .padding(top = 10.dp)
                                .testTag("profile_follow_btn")
                        ) {
                            Text("Following", color = TextPrimary, style = MaterialTheme.typography.labelLarge)
                        }
                    } else {
                        Button(
                            onClick = onToggleFollowViewedUser,
                            colors = ButtonDefaults.buttonColors(containerColor = TextPrimary, contentColor = OledBlack),
                            shape = RoundedCornerShape(999.dp),
                            modifier = Modifier
                                .padding(top = 10.dp)
                                .testTag("profile_follow_btn")
                        ) {
                            Text("Follow", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .offset(y = (-14).dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = state.displayName,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        modifier = Modifier.testTag("profile_display_name")
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    VerificationBadgeIcon(badge = state.badge, size = 19.dp)
                }
                Text(
                    text = "${state.handle} · ${state.badge.label}",
                    style = MonoTabularStyle,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(state.bio, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Spacer(modifier = Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CalendarMonth, null, tint = TextMuted, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(state.joinedDate, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                }
                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(modifier = Modifier.testTag("profile_following_count")) {
                        Text(
                            text = formatCompactCount(state.followingCount),
                            style = MonoTabularStyle.copy(fontSize = 13.5.sp, fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Following", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    }
                    Row(modifier = Modifier.testTag("profile_followers_count")) {
                        Text(
                            text = formatCompactCount(state.followersCount),
                            style = MonoTabularStyle.copy(fontSize = 13.5.sp, fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Followers", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    }
                }

                if (state.isCurrentUser) {
                    Spacer(modifier = Modifier.height(12.dp))

                    // App Settings Customization Card
                    Surface(
                        color = SurfaceCard,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_settings_card")
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                    modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(onClick = onToggleSettingsExpanded)
                                    .testTag("profile_settings_header"),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Settings, null, tint = XBlue, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "App Settings Customization",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "Undo: ${settings.undoDelaySeconds}s · Ad-Free: ${if (settings.adFreeEnabled) "On" else "Off"}",
                                            style = MonoTabularStyle.copy(fontSize = 11.sp),
                                            color = TextMuted
                                        )
                                    }
                                }
                                Text(
                                    text = if (showSettingsExpanded) "Hide" else "Customize",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = XBlue
                                )
                            }

                            if (showSettingsExpanded) {
                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = BorderSubtle, thickness = 0.8.dp)
                                Spacer(modifier = Modifier.height(10.dp))

                                SettingsToggleRow(
                                    title = "Ad-Free Timeline & Trends",
                                    checked = settings.adFreeEnabled,
                                    onCheckedChange = onToggleAdFree
                                )
                                SettingsToggleRow(
                                    title = "Show Edit History Diff Links",
                                    checked = settings.showEditHistoryLinks,
                                    onCheckedChange = onToggleEditHistoryLinks
                                )
                                SettingsToggleRow(
                                    title = "Auto-Expand Thread Cards",
                                    checked = settings.autoExpandThreads,
                                    onCheckedChange = onToggleAutoExpandThreads
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Undo Send Countdown",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf(0, 5, 10).forEach { delaySec ->
                                        val selected = settings.undoDelaySeconds == delaySec
                                        FilterChip(
                                            selected = selected,
                                            onClick = { onUpdateUndoDelay(delaySec) },
                                            label = { Text("${delaySec}s") },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = XBlueContainer,
                                                selectedLabelColor = XBlue,
                                                containerColor = SurfaceElevated,
                                                labelColor = TextMuted
                                            ),
                                            border = BorderStroke(1.dp, if (selected) XBlue else BorderSubtle)
                                        )
                                    }
                                    Spacer(modifier = Modifier.weight(1f))
                                    TextButton(onClick = onOpenPremiumStudio) {
                                        Text("Edit Profile & Badge", color = XBlue, style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Profile Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                ProfileTab.entries.forEach { tab ->
                    val selected = state.activeTab == tab
                    Column(
                        modifier = Modifier
                            .clickable { onSelectProfileTab(tab) }
                            .padding(horizontal = 16.dp)
                            .testTag("profile_tab_${tab.name}"),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = tab.title,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (selected) TextPrimary else TextMuted,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                        Box(
                            modifier = Modifier
                                .width(44.dp)
                                .height(3.dp)
                                .background(
                                    color = if (selected) XBlue else OledBlack,
                                    shape = RoundedCornerShape(999.dp)
                                )
                        )
                    }
                }
            }
            HorizontalDivider(color = HairlineDivider, thickness = 0.8.dp)
        }

        if (state.posts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nothing in ${state.activeTab.title} yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
            }
        } else {
            items(state.posts, key = { it.id }) { post ->
                PostCard(
                    post = post,
                    showEditHistoryLinks = settings.showEditHistoryLinks,
                    folderName = post.bookmarkFolderId?.let { folderNameMap[it] },
                    onLike = { onLike(post.id) },
                    onRepost = { onRepost(post.id) },
                    onReply = { onReply(post) },
                    onBookmark = { onBookmark(post) },
                    onVotePoll = { optId -> onVotePoll(post.id, optId) },
                    onOpenReaderMode = { onOpenReaderMode(post) },
                    onOpenEdit = { onOpenEdit(post) },
                    onOpenEditHistory = { onOpenEditHistory(post) },
                    onToggleHighlight = { onToggleHighlight(post.id) },
                    onDeletePost = { onDeletePost(post.id) },
                    onOpenMediaLightbox = { onOpenMediaLightbox(post) },
                    onToggleSaveForOffline = { onToggleSaveForOffline(post.id) },
                    onToggleOfflineRead = { onToggleOfflineRead(post.id) }
                )
            }
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = TextPrimary
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = OledBlack,
                checkedTrackColor = XBlue
            )
        )
    }
}
