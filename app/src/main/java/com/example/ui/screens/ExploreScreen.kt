package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.ui.focus.onFocusChanged
import com.example.core.designsystem.rememberIsKeyboardOpen
import com.example.core.scroll.AutoHideHeaderLayout
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PostItem
import com.example.data.model.SuggestedUser
import com.example.ui.components.AdShieldPill
import com.example.ui.components.PostCard
import com.example.ui.components.TopRightMenuButton
import com.example.ui.components.UserAvatar
import com.example.ui.components.VerificationBadgeIcon
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.GoldOrg
import com.example.ui.theme.HairlineDivider
import com.example.ui.theme.MonoTabularStyle
import com.example.ui.theme.OledBlack
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.XBlue
import com.example.viewmodel.XProUiState

@Composable
fun ExploreScreen(
    uiState: XProUiState,
    onSearchQueryChange: (String) -> Unit,
    onOpenPremiumStudio: () -> Unit,
    onOpenSettings: () -> Unit = onOpenPremiumStudio,
    onOpenDrawer: (() -> Unit)? = null,
    onOpenGrok: () -> Unit = {},
    onOpenBookmarks: () -> Unit = {},
    onOpenArticles: () -> Unit = {},
    onOpenMediaDownloader: () -> Unit = {},
    onToggleFollowUser: (SuggestedUser) -> Unit,
    onOpenProfile: () -> Unit = {},
    onOpenAuthorProfile: (String) -> Unit = {},
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
    val listState = rememberLazyListState()
    val isKeyboardOpen by rememberIsKeyboardOpen()
    var searchFocused by remember { mutableStateOf(false) }
    val folderNameMap = uiState.bookmarkFolders.associate { it.id to it.name }

    AutoHideHeaderLayout(
        modifier = modifier,
        listState = listState,
        lockBarsVisible = isKeyboardOpen || searchFocused,
        header = {
            // Search Bar Header
            Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onOpenDrawer ?: onOpenProfile,
                modifier = Modifier.testTag("explore_profile_avatar")
            ) {
                UserAvatar(
                    name = uiState.userProfile.name,
                    isCurrentUser = true,
                    avatarColorHex = 0xFF1D9BF0,
                    size = 32.dp
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search XTRA posts, topics, authors…", color = colorScheme.onSurfaceVariant) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = colorScheme.onSurfaceVariant) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear search", tint = colorScheme.onSurfaceVariant)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(999.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = colorScheme.surfaceContainerHigh,
                    focusedBorderColor = colorScheme.primary,
                    unfocusedBorderColor = colorScheme.outlineVariant,
                    focusedTextColor = colorScheme.onSurface,
                    unfocusedTextColor = colorScheme.onSurface
                ),
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { searchFocused = it.hasFocus }
                    .testTag("explore_search_input")
            )
            Spacer(modifier = Modifier.width(6.dp))
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
        HorizontalDivider(color = colorScheme.outline, thickness = 0.8.dp)
        }
    ) { scrollModifier, contentPadding ->
        LazyColumn(
            state = listState,
            modifier = scrollModifier.testTag("explore_feed_list"),
            contentPadding = contentPadding
        ) {
            if (uiState.searchQuery.isBlank()) {
                // Trends Section
                item {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.TrendingUp, null, tint = colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("What's Happening", style = MaterialTheme.typography.titleMedium, color = colorScheme.onSurface)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = colorScheme.surfaceContainer,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, colorScheme.outline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                uiState.visibleTrends.forEachIndexed { idx, trend ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onSearchQueryChange(trend.topic.removePrefix("#")) }
                                            .padding(horizontal = 14.dp, vertical = 12.dp)
                                    ) {
                                        Text(
                                            text = trend.category,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (trend.isPromoted) GoldOrg else colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = trend.topic,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = trend.postCountLabel,
                                            style = MonoTabularStyle.copy(fontSize = 11.sp),
                                            color = colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (idx < uiState.visibleTrends.lastIndex) {
                                        HorizontalDivider(color = colorScheme.outlineVariant, thickness = 0.8.dp)
                                    }
                                }
                            }
                        }
                    }
                }

                // Who to Follow Section
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        Text("Who to Follow", style = MaterialTheme.typography.titleMedium, color = colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = colorScheme.surfaceContainer,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, colorScheme.outline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                uiState.suggestedUsers.forEachIndexed { idx, user ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onOpenAuthorProfile(user.id) }
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        UserAvatar(
                                            name = user.name,
                                            isCurrentUser = false,
                                            avatarColorHex = user.avatarColorHex,
                                            size = 40.dp
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = user.name,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                VerificationBadgeIcon(badge = user.badge, size = 15.dp)
                                            }
                                            Text(user.handle, style = MonoTabularStyle, color = colorScheme.onSurfaceVariant)
                                            Text(
                                                text = user.bio,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        if (user.isFollowing) {
                                            OutlinedButton(
                                                onClick = { onToggleFollowUser(user) },
                                                shape = RoundedCornerShape(999.dp),
                                                border = BorderStroke(1.dp, colorScheme.outline)
                                            ) {
                                                Text("Following", color = colorScheme.onSurface, style = MaterialTheme.typography.labelMedium)
                                            }
                                        } else {
                                            Button(
                                                onClick = { onToggleFollowUser(user) },
                                                colors = ButtonDefaults.buttonColors(containerColor = colorScheme.onSurface, contentColor = colorScheme.background),
                                                shape = RoundedCornerShape(999.dp)
                                            ) {
                                                Text("Follow", style = MaterialTheme.typography.labelMedium)
                                            }
                                        }
                                    }
                                    if (idx < uiState.suggestedUsers.lastIndex) {
                                        HorizontalDivider(color = colorScheme.outlineVariant, thickness = 0.8.dp)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text("Discover Discussions", style = MaterialTheme.typography.titleMedium, color = colorScheme.onSurface)
                    }
                }
            } else {
                item {
                    Text(
                        text = "${uiState.explorePosts.size} matching results for \"${uiState.searchQuery}\"",
                        style = MonoTabularStyle,
                        color = TextMuted,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }

            items(uiState.explorePosts, key = { it.id }) { post ->
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
        }
    }
}
