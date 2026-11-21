package com.example.feature.profile

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.data.model.PostItem
import com.example.data.model.ProfileTab
import com.example.ui.screens.ProfileScreen
import com.example.viewmodel.ProfileUiState

@Composable
fun XtraProfileRouteScreen(
    profileState: ProfileUiState,
    onSelectProfileTab: (ProfileTab) -> Unit,
    onRetryProfile: () -> Unit,
    onBackToOwnProfile: () -> Unit,
    onBackToHome: () -> Unit,
    onToggleFollowViewedUser: () -> Unit,
    onOpenPremiumStudio: () -> Unit,
    onOpenAppSettings: () -> Unit = onOpenPremiumStudio,
    onOpenGrok: () -> Unit = {},
    onOpenBookmarks: () -> Unit,
    onOpenArticles: () -> Unit,
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
    ProfileScreen(
        profileState = profileState,
        onSelectProfileTab = onSelectProfileTab,
        onRetryProfile = onRetryProfile,
        onBackToOwnProfile = onBackToOwnProfile,
        onBackToHome = onBackToHome,
        onToggleFollowViewedUser = onToggleFollowViewedUser,
        onOpenPremiumStudio = onOpenPremiumStudio,
        onOpenAppSettings = onOpenAppSettings,
        onOpenGrok = onOpenGrok,
        onOpenBookmarks = onOpenBookmarks,
        onOpenArticles = onOpenArticles,
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
        modifier = modifier.fillMaxSize()
    )
}
