package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import com.example.core.scroll.AutoHideHeaderLayout
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.example.data.model.FeedFilter
import com.example.data.model.FeedTab
import com.example.data.model.PostItem
import com.example.ui.components.AdShieldPill
import com.example.ui.components.OfflineIndicatorPill
import com.example.ui.components.PostCard
import com.example.ui.components.TopRightMenuButton
import com.example.ui.components.UserAvatar
import com.example.ui.components.XProLogo
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.HairlineDivider
import com.example.ui.theme.MonoTabularStyle
import com.example.ui.theme.OledBlack
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.XBlue
import com.example.ui.theme.XBlueContainer
import com.example.viewmodel.XProUiState

@Composable
fun HomeFeedScreen(
    uiState: XProUiState,
    pagedPosts: LazyPagingItems<PostItem>,
    onSelectFeedTab: (FeedTab) -> Unit,
    onSelectFeedFilter: (FeedFilter) -> Unit,
    onOpenProfile: () -> Unit,
    onOpenAuthorProfile: (String) -> Unit,
    onOpenPremiumStudio: () -> Unit,
    onOpenSettings: () -> Unit = onOpenPremiumStudio,
    onOpenDrawer: () -> Unit = onOpenProfile,
    onOpenGrok: () -> Unit = {},
    onOpenBookmarks: () -> Unit = {},
    onOpenArticles: () -> Unit = {},
    onOpenMediaDownloader: () -> Unit,
    onToggleOfflineMode: () -> Unit,
    onToggleSaveForOffline: (String) -> Unit,
    onToggleOfflineRead: (String) -> Unit,
    onCacheAllThreads: () -> Unit,
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
    val listState: LazyListState = rememberLazyListState()
    val folderNameMap = remember(uiState.bookmarkFolders) {
        uiState.bookmarkFolders.associate { it.id to it.name }
    }

    AutoHideHeaderLayout(
        modifier = modifier,
        listState = listState,
        header = {
        // Top Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onOpenDrawer,
                    modifier = Modifier.testTag("home_profile_avatar")
                ) {
                    UserAvatar(
                        name = uiState.userProfile.name,
                        isCurrentUser = true,
                        avatarColorHex = 0xFF1D9BF0,
                        size = 32.dp
                    )
                }
                XProLogo(size = 22.dp, tint = colorScheme.onSurface)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onOpenMediaDownloader,
                    modifier = Modifier.testTag("open_media_downloader_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Download,
                        contentDescription = "Media Downloader",
                        tint = colorScheme.primary
                    )
                }
                OfflineIndicatorPill(
                    offlineActive = uiState.offlineModeActive,
                    cachedCount = uiState.offlineCachedCount,
                    onClick = onToggleOfflineMode
                )
                AdShieldPill(
                    adFreeEnabled = uiState.userProfile.adFreeEnabled,
                    blockedCount = uiState.blockedAdsCount,
                    onClick = onOpenPremiumStudio
                )
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

        // Offline Mode Status Banner
        if (uiState.offlineModeActive) {
            Surface(
                color = XBlueContainer,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, XBlue.copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Bolt, contentDescription = null, tint = XBlue, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Offline Mode Active (Room SQLite)",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary
                            )
                            Text(
                                text = "${uiState.offlineCachedCount} posts & threads cached · ${uiState.offlineUnreadCount} unread",
                                style = MonoTabularStyle.copy(fontSize = 11.sp),
                                color = XBlue
                            )
                        }
                    }
                    TextButton(onClick = onCacheAllThreads) {
                        Text("Cache All", color = XBlue, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        // For You / Following Tabs
        Row(modifier = Modifier.fillMaxWidth()) {
            FeedTab.entries.forEach { tab ->
                val selected = uiState.feedTab == tab
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelectFeedTab(tab) }
                        .testTag("feed_tab_${tab.name}"),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = tab.title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (selected) colorScheme.onSurface else colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                    Box(
                        modifier = Modifier
                            .width(64.dp)
                            .height(3.dp)
                            .background(
                                color = if (selected) colorScheme.primary else colorScheme.background,
                                shape = RoundedCornerShape(999.dp)
                            )
                    )
                }
            }
        }
        HorizontalDivider(color = colorScheme.outline, thickness = 0.8.dp)

        // Filter Pill Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FeedFilter.entries.forEach { filter ->
                val selected = uiState.feedFilter == filter
                FilterChip(
                    selected = selected,
                    onClick = { onSelectFeedFilter(filter) },
                    label = {
                        Text(
                            text = filter.title,
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colorScheme.primaryContainer,
                        selectedLabelColor = colorScheme.primary,
                        containerColor = colorScheme.surfaceContainer,
                        labelColor = colorScheme.onSurfaceVariant
                    ),
                    border = BorderStroke(1.dp, if (selected) colorScheme.primary else colorScheme.outlineVariant),
                    modifier = Modifier.testTag("feed_filter_${filter.name}")
                )
            }
        }
        HorizontalDivider(color = colorScheme.outline, thickness = 0.8.dp)
        }
    ) { scrollModifier, contentPadding ->
        val refreshState = pagedPosts.loadState.refresh
        when {
            refreshState is LoadState.Loading && pagedPosts.itemCount == 0 -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding)
                        .padding(32.dp)
                        .testTag("home_feed_loading"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = XBlue)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Loading feed…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    }
                }
            }
            refreshState is LoadState.Error && pagedPosts.itemCount == 0 -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding)
                        .padding(32.dp)
                        .testTag("home_feed_error"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.ErrorOutline,
                            contentDescription = null,
                            tint = com.example.ui.theme.DangerUndo,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Failed to load feed",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = refreshState.error.localizedMessage ?: "Unexpected paging error.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { pagedPosts.retry() },
                            colors = ButtonDefaults.buttonColors(containerColor = XBlue, contentColor = TextPrimary)
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }
            refreshState is LoadState.NotLoading && pagedPosts.itemCount == 0 -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding)
                        .padding(32.dp)
                        .testTag("home_feed_empty"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.Article,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No matching posts in this view",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Try switching back to All or follow more creators in Explore.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    state = listState,
                    modifier = scrollModifier.testTag("home_feed_list"),
                    contentPadding = contentPadding
                ) {
                    items(
                        count = pagedPosts.itemCount,
                        key = pagedPosts.itemKey { it.id },
                        contentType = pagedPosts.itemContentType { it.isLongForm }
                    ) { index ->
                        val post = pagedPosts[index] ?: return@items
                        PostCard(
                            post = post,
                            showEditHistoryLinks = uiState.userProfile.showEditHistoryLinks,
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
                            onToggleOfflineRead = { onToggleOfflineRead(post.id) },
                            onAuthorClick = { onOpenAuthorProfile(post.authorId) }
                        )
                    }

                    when (val appendState = pagedPosts.loadState.append) {
                        is LoadState.Loading -> {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = XBlue,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                        is LoadState.Error -> {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = appendState.error.localizedMessage ?: "Page load error",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    TextButton(onClick = { pagedPosts.retry() }) {
                                        Text("Retry", color = XBlue)
                                    }
                                }
                            }
                        }
                        else -> Unit
                    }
                }
            }
        }
    }
}
