package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.core.scroll.AutoHideHeaderLayout
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PostItem
import com.example.ui.components.PostCard
import com.example.ui.components.TopRightMenuButton
import com.example.ui.components.UserAvatar
import com.example.ui.theme.HairlineDivider
import com.example.ui.theme.MonoTabularStyle
import com.example.ui.theme.OledBlack
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.XBlue
import com.example.viewmodel.XProUiState

@Composable
fun ArticlesScreen(
    uiState: XProUiState,
    onOpenArticleStudio: () -> Unit,
    onOpenProfile: () -> Unit = {},
    onOpenDrawer: () -> Unit = onOpenProfile,
    onOpenSettings: () -> Unit = {},
    onOpenGrok: () -> Unit = {},
    onOpenBookmarks: () -> Unit = {},
    onOpenMediaDownloader: () -> Unit = {},
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
    val folderNameMap = uiState.bookmarkFolders.associate { it.id to it.name }

    AutoHideHeaderLayout(
        modifier = modifier,
        listState = listState,
        header = {
            Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                IconButton(
                    onClick = onOpenDrawer,
                    modifier = Modifier.testTag("articles_profile_avatar")
                ) {
                    UserAvatar(
                        name = uiState.userProfile.name,
                        isCurrentUser = true,
                        avatarColorHex = 0xFF1D9BF0,
                        size = 32.dp
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text("Articles & Threads", style = MaterialTheme.typography.titleLarge, color = colorScheme.onSurface)
                    Text(
                        text = "Long-form essays up to 25,000 chars · Reader Mode",
                        style = MonoTabularStyle.copy(fontSize = 11.sp),
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onOpenArticleStudio,
                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary, contentColor = colorScheme.onPrimary),
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier.testTag("write_article_button")
                ) {
                    Icon(Icons.Filled.EditNote, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Write", style = MaterialTheme.typography.labelLarge)
                }
                TopRightMenuButton(
                    onOpenSettings = onOpenSettings,
                    onOpenProfile = onOpenProfile,
                    onOpenBookmarks = onOpenBookmarks,
                    onOpenArticles = {},
                    onOpenMediaDownloader = onOpenMediaDownloader,
                    onOpenGrok = onOpenGrok
                )
            }
        }
        HorizontalDivider(color = colorScheme.outline, thickness = 0.8.dp)
        }
    ) { scrollModifier, contentPadding ->
        LazyColumn(
            state = listState,
            modifier = scrollModifier.testTag("articles_feed_list"),
            contentPadding = contentPadding
        ) {
            items(uiState.articlePosts, key = { it.id }) { post ->
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
