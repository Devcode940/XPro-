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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PostItem
import com.example.ui.components.PostCard
import com.example.ui.components.TopRightMenuButton
import com.example.ui.components.UserAvatar
import com.example.ui.modals.resolveFolderIcon
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
import com.example.ui.theme.XBlue
import com.example.ui.theme.XBlueContainer
import com.example.viewmodel.XProUiState

@Composable
fun BookmarksScreen(
    uiState: XProUiState,
    onSelectFolder: (String?) -> Unit,
    onBookmarkSearchChange: (String) -> Unit,
    onOpenNewFolder: () -> Unit,
    onDeleteFolder: (String) -> Unit,
    onOpenProfile: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenArticles: () -> Unit = {},
    onOpenMediaDownloader: () -> Unit = {},
    onOpenAuthorProfile: (String) -> Unit = {},
    onToggleSaveForOffline: (String) -> Unit,
    onToggleOfflineRead: (String) -> Unit,
    onCacheAllThreads: () -> Unit,
    onClearOfflineCache: () -> Unit,
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
    val folderNameMap = uiState.bookmarkFolders.associate { it.id to it.name }
    val allBookmarkedCount = uiState.allPosts.count { it.isBookmarked }
    val selectedFolder = uiState.bookmarkFolders.find { it.id == uiState.selectedFolderId }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(OledBlack)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onOpenProfile,
                    modifier = Modifier.testTag("bookmarks_profile_avatar")
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
                    Text("Bookmark Folders", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    Text(
                        text = "$allBookmarkedCount saved posts · ${uiState.bookmarkFolders.size} folders",
                        style = MonoTabularStyle.copy(fontSize = 11.5.sp),
                        color = TextMuted
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = onOpenNewFolder,
                    modifier = Modifier.testTag("new_folder_button")
                ) {
                    Icon(Icons.Filled.CreateNewFolder, contentDescription = null, tint = XBlue, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Folder", color = XBlue)
                }
                TopRightMenuButton(
                    onOpenSettings = onOpenSettings,
                    onOpenProfile = onOpenProfile,
                    onOpenBookmarks = {},
                    onOpenArticles = onOpenArticles,
                    onOpenMediaDownloader = onOpenMediaDownloader
                )
            }
        }

        // Search saved bookmarks
        OutlinedTextField(
            value = uiState.bookmarkSearchQuery,
            onValueChange = onBookmarkSearchChange,
            placeholder = { Text("Filter saved posts…", color = TextMuted) },
            leadingIcon = { Icon(Icons.Filled.Search, null, tint = TextMuted) },
            trailingIcon = {
                if (uiState.bookmarkSearchQuery.isNotEmpty()) {
                    IconButton(onClick = { onBookmarkSearchChange("") }) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear", tint = TextMuted)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(999.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceElevated,
                unfocusedContainerColor = SurfaceElevated,
                focusedBorderColor = XBlue,
                unfocusedBorderColor = BorderSubtle,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 10.dp)
        )

        // Horizontal Folder Cards
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FolderChipCard(
                title = "All Saved",
                count = allBookmarkedCount,
                accent = XBlue,
                selected = uiState.selectedFolderId == null,
                onClick = { onSelectFolder(null) },
                icon = Icons.Outlined.Bookmarks
            )
            FolderChipCard(
                title = "Read Later ⚡",
                count = uiState.offlineCachedCount,
                accent = EmeraldAdShield,
                selected = uiState.selectedFolderId == "read_later",
                onClick = { onSelectFolder("read_later") },
                icon = Icons.Filled.Bolt
            )
            uiState.bookmarkFolders.forEach { folder ->
                FolderChipCard(
                    title = folder.name,
                    count = folder.count,
                    accent = Color(folder.colorHex),
                    selected = uiState.selectedFolderId == folder.id,
                    onClick = { onSelectFolder(folder.id) },
                    icon = resolveFolderIcon(folder.iconKey)
                )
            }
        }

        if (uiState.selectedFolderId == "read_later") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Room SQLite cache · ${uiState.offlineCachedCount} saved for offline",
                    style = MaterialTheme.typography.bodySmall,
                    color = EmeraldAdShield,
                    modifier = Modifier.weight(1f)
                )
                Row {
                    TextButton(onClick = onCacheAllThreads) {
                        Text("Cache Threads", color = XBlue, style = MaterialTheme.typography.labelMedium)
                    }
                    TextButton(onClick = onClearOfflineCache) {
                        Text("Clear", color = DangerUndo, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        } else if (selectedFolder != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = selectedFolder.description.ifBlank { selectedFolder.name },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { onDeleteFolder(selectedFolder.id) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete folder", tint = DangerUndo, modifier = Modifier.size(18.dp))
                }
            }
        }

        HorizontalDivider(color = HairlineDivider, thickness = 0.8.dp)

        if (uiState.bookmarkedPosts.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.Bookmarks, null, tint = TextMuted, modifier = Modifier.size(44.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (uiState.selectedFolderId == "read_later") {
                            "No posts saved for Read Later yet"
                        } else "No saved posts in this folder",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (uiState.selectedFolderId == "read_later") {
                            "Save any thread or article for offline reading via post options (···) or tap Cache Threads."
                        } else "Tap the Bookmark icon on any post to organize it here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                items(uiState.bookmarkedPosts, key = { it.id }) { post ->
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
}

@Composable
private fun FolderChipCard(
    title: String,
    count: Int,
    accent: Color,
    selected: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        color = if (selected) accent.copy(alpha = 0.16f) else SurfaceCard,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (selected) accent else BorderSubtle)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) TextPrimary else TextMuted
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "$count",
                style = MonoTabularStyle.copy(fontSize = 11.sp),
                color = accent
            )
        }
    }
}
