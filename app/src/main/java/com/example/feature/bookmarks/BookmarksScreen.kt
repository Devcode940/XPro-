package com.example.feature.bookmarks

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.designsystem.AppTopBar
import com.example.core.designsystem.rememberIsKeyboardOpen
import com.example.core.scroll.AutoHideHeaderLayout
import com.example.data.model.BookmarkFolder
import com.example.data.model.BookmarkItemType
import com.example.data.model.PostItem
import com.example.data.model.TypedBookmarkItem
import com.example.data.model.UserProfile
import com.example.ui.components.PostCard
import com.example.ui.theme.MonoTabularStyle

@Composable
fun XtraBookmarksScreen(
    bookmarksViewModel: BookmarksViewModel,
    userProfile: UserProfile,
    onOpenDrawer: () -> Unit,
    onBack: () -> Unit,
    onOpenAuthorProfile: (String) -> Unit,
    onLike: (String) -> Unit,
    onRepost: (String) -> Unit,
    onReply: (PostItem) -> Unit,
    onBookmarkPost: (PostItem) -> Unit,
    onVotePoll: (String, String) -> Unit,
    onOpenReaderMode: (PostItem) -> Unit,
    onOpenEdit: (PostItem) -> Unit,
    onOpenEditHistory: (PostItem) -> Unit,
    onToggleHighlight: (String) -> Unit,
    onDeletePost: (String) -> Unit,
    onOpenMediaLightbox: (PostItem) -> Unit,
    onToggleSaveForOffline: (String) -> Unit,
    onToggleOfflineRead: (String) -> Unit,
    onOpenVideoUrl: (String) -> Unit,
    onSignInClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val state by bookmarksViewModel.uiState.collectAsStateWithLifecycle()
    var showNewCollectionDialog by rememberSaveable { mutableStateOf(false) }
    var showAddBookmarkDialog by rememberSaveable { mutableStateOf(false) }
    val isKeyboardOpen by rememberIsKeyboardOpen()
    var searchFocused by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val readLaterListState = rememberLazyListState()
    val activeListState = if (state.selectedCategory == BookmarkFilterCategory.READ_LATER) {
        readLaterListState
    } else {
        listState
    }

    val folderNameMap = remember(state.folders) { state.folders.associate { it.id to it.name } }

    AutoHideHeaderLayout(
        modifier = modifier.testTag("xtra_bookmarks_screen"),
        listState = activeListState,
        lockBarsVisible = isKeyboardOpen || searchFocused || showNewCollectionDialog || showAddBookmarkDialog,
        header = {
            AppTopBar(
                title = "Bookmarks Vault",
                subtitle = "${state.typedBookmarks.size} items · ${state.folders.size} collections",
                userName = userProfile.name,
                showBack = true,
                onBack = onBack,
                onOpenDrawer = onOpenDrawer,
                actions = {
                    IconButton(
                        onClick = { showAddBookmarkDialog = true },
                        modifier = Modifier.testTag("add_typed_bookmark_btn")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Save media or link bookmark", tint = colorScheme.primary)
                    }
                    IconButton(
                        onClick = { showNewCollectionDialog = true },
                        modifier = Modifier.testTag("new_collection_btn")
                    ) {
                        Icon(Icons.Filled.CreateNewFolder, contentDescription = "New collection", tint = colorScheme.onSurface)
                    }
                    IconButton(
                        onClick = {
                            if (state.isAuthenticated) bookmarksViewModel.pushAllLocalToFirestoreNow() else onSignInClick()
                        },
                        modifier = Modifier.testTag("sync_bookmarks_btn")
                    ) {
                        Icon(
                            imageVector = if (state.isAuthenticated) Icons.Filled.Sync else Icons.Filled.CloudOff,
                            contentDescription = if (state.isAuthenticated) "Sync bookmarks with Firestore" else "Sign in to sync",
                            tint = if (state.isAuthenticated) colorScheme.tertiary else colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    ) { scrollModifier, contentPadding ->
        LazyColumn(
            state = activeListState,
            modifier = scrollModifier,
            contentPadding = contentPadding
        ) {
            item(key = "bookmarks_controls_header") {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Cloud Sync Status Strip
                    Surface(
                        color = if (state.isAuthenticated) colorScheme.tertiaryContainer else colorScheme.surfaceContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = if (state.isAuthenticated) Icons.Filled.CloudDone else Icons.Filled.CloudOff,
                                    contentDescription = null,
                                    tint = if (state.isAuthenticated) colorScheme.tertiary else colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = when {
                                        state.syncError != null -> "Sync warning: ${state.syncError}"
                                        state.isSyncing -> "Synchronizing bookmarks with Firestore…"
                                        state.isAuthenticated -> "Room local cache + Firestore cloud sync active"
                                        else -> "Room local vault active · Sign in for cloud sync"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurface
                                )
                            }
                            if (!state.isAuthenticated) {
                                TextButton(onClick = onSignInClick) {
                                    Text("Sign In", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }

                    state.statusBanner?.let { banner ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(banner, style = MaterialTheme.typography.bodySmall, color = colorScheme.primary)
                            IconButton(onClick = bookmarksViewModel::dismissBanner, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Filled.Close, contentDescription = "Dismiss", modifier = Modifier.size(14.dp))
                            }
                        }
                    }

                    // Search bookmarks
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = bookmarksViewModel::updateSearchQuery,
                        placeholder = { Text("Search saved posts, videos, images, links…") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        trailingIcon = {
                            if (state.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { bookmarksViewModel.updateSearchQuery("") }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .onFocusChanged { searchFocused = it.isFocused }
                            .testTag("bookmarks_search_input")
                    )

                    // Category Filter Row: All, Posts, Videos, Images, Links, Read Later
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BookmarkFilterCategory.entries.forEach { cat ->
                            FilterChip(
                                selected = state.selectedCategory == cat,
                                onClick = { bookmarksViewModel.selectCategory(cat) },
                                label = { Text(cat.label) },
                                modifier = Modifier.testTag("bookmark_cat_${cat.name}")
                            )
                        }
                    }

                    // Collections Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = state.selectedFolderId == null,
                            onClick = { bookmarksViewModel.selectFolder(null) },
                            label = { Text("All Collections") }
                        )
                        state.folders.forEach { folder ->
                            val accent = Color(folder.colorHex)
                            FilterChip(
                                selected = state.selectedFolderId == folder.id,
                                onClick = { bookmarksViewModel.selectFolder(folder.id) },
                                label = { Text("${folder.name} (${folder.count})") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = accent.copy(alpha = 0.18f),
                                    selectedLabelColor = colorScheme.onSurface
                                ),
                                trailingIcon = if (state.selectedFolderId == folder.id) {
                                    {
                                        Icon(
                                            imageVector = Icons.Filled.DeleteOutline,
                                            contentDescription = "Delete collection",
                                            tint = colorScheme.error,
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clickable { bookmarksViewModel.deleteCollection(folder.id) }
                                        )
                                    }
                                } else null
                            )
                        }
                    }
                    HorizontalDivider(color = colorScheme.outline, thickness = 0.8.dp)
                }
            }

            if (state.selectedCategory == BookmarkFilterCategory.READ_LATER) {
                if (state.readLaterPosts.isEmpty()) {
                    item(key = "read_later_empty") {
                        EmptyBookmarksPlaceholder("No posts cached for Read Later", "Save threads or articles offline to read anywhere.")
                    }
                } else {
                    items(state.readLaterPosts, key = { it.id }) { post ->
                        PostCard(
                            post = post,
                            showEditHistoryLinks = userProfile.showEditHistoryLinks,
                            folderName = post.bookmarkFolderId?.let { folderNameMap[it] },
                            onLike = { onLike(post.id) },
                            onRepost = { onRepost(post.id) },
                            onReply = { onReply(post) },
                            onBookmark = { onBookmarkPost(post) },
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
            } else if (state.typedBookmarks.isEmpty() && state.savedPosts.isEmpty()) {
                item(key = "bookmarks_empty") {
                    EmptyBookmarksPlaceholder(
                        title = "No matching bookmarks",
                        subtitle = "Save posts, videos, images, or links and organize them into collections."
                    )
                }
            } else {
                items(state.typedBookmarks, key = { it.id }) { bookmark ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)) {
                        TypedBookmarkCard(
                            item = bookmark,
                            folderName = bookmark.folderId?.let { folderNameMap[it] },
                            folders = state.folders,
                            onMoveToFolder = { targetFolderId ->
                                bookmarksViewModel.moveBookmarkToFolder(bookmark, targetFolderId)
                            },
                            onRemove = { bookmarksViewModel.removeTypedBookmark(bookmark) },
                            onOpenMedia = {
                                val targetUrl = bookmark.mediaUrl ?: bookmark.linkUrl
                                if (!targetUrl.isNullOrBlank()) {
                                    onOpenVideoUrl(targetUrl)
                                }
                            }
                        )
                    }
                }

                if (state.selectedCategory == BookmarkFilterCategory.ALL || state.selectedCategory == BookmarkFilterCategory.POSTS) {
                    val unmirroredPosts = state.savedPosts.filter { post ->
                        state.typedBookmarks.none { it.postId == post.id }
                    }
                    items(unmirroredPosts, key = { "post_${it.id}" }) { post ->
                        PostCard(
                            post = post,
                            showEditHistoryLinks = userProfile.showEditHistoryLinks,
                            folderName = post.bookmarkFolderId?.let { folderNameMap[it] },
                            onLike = { onLike(post.id) },
                            onRepost = { onRepost(post.id) },
                            onReply = { onReply(post) },
                            onBookmark = { onBookmarkPost(post) },
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

    if (showNewCollectionDialog) {
        var name by rememberSaveable { mutableStateOf("") }
        var desc by rememberSaveable { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewCollectionDialog = false },
            title = { Text("Create Collection") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Collection Name") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("Description") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            bookmarksViewModel.createCollection(name, desc, 0xFF1D9BF0)
                            showNewCollectionDialog = false
                        }
                    }
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showNewCollectionDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showAddBookmarkDialog) {
        var type by rememberSaveable { mutableStateOf(BookmarkItemType.LINK) }
        var title by rememberSaveable { mutableStateOf("") }
        var content by rememberSaveable { mutableStateOf("") }
        var url by rememberSaveable { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddBookmarkDialog = false },
            title = { Text("Add Bookmark to Vault") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        BookmarkItemType.entries.forEach { entry ->
                            FilterChip(
                                selected = type == entry,
                                onClick = { type = entry },
                                label = { Text(entry.label) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("Notes / Summary") },
                        maxLines = 3
                    )
                    if (type != BookmarkItemType.POST) {
                        OutlinedTextField(
                            value = url,
                            onValueChange = { url = it },
                            label = { Text("URL") },
                            singleLine = true
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank() || content.isNotBlank()) {
                            bookmarksViewModel.addTypedBookmark(
                                itemType = type,
                                title = title,
                                content = content,
                                mediaUrl = if (type == BookmarkItemType.VIDEO || type == BookmarkItemType.IMAGE) url else null,
                                linkUrl = if (type == BookmarkItemType.LINK) url else null,
                                folderId = state.selectedFolderId
                            )
                            showAddBookmarkDialog = false
                        }
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showAddBookmarkDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun TypedBookmarkCard(
    item: TypedBookmarkItem,
    folderName: String?,
    folders: List<BookmarkFolder>,
    onMoveToFolder: (String?) -> Unit,
    onRemove: () -> Unit,
    onOpenMedia: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    var showFolderMenu by remember { mutableStateOf(false) }

    val (icon, tint) = when (item.itemType) {
        BookmarkItemType.POST -> Icons.Filled.Article to colorScheme.primary
        BookmarkItemType.VIDEO -> Icons.Filled.PlayCircle to colorScheme.tertiary
        BookmarkItemType.IMAGE -> Icons.Filled.Image to colorScheme.secondary
        BookmarkItemType.LINK -> Icons.Filled.Link to colorScheme.primary
    }

    Surface(
        color = colorScheme.surfaceContainer,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = item.mediaUrl != null || item.linkUrl != null, onClick = onOpenMedia)
            .testTag("typed_bookmark_${item.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = item.itemType.label, tint = tint, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.itemType.label.uppercase(),
                        style = MonoTabularStyle.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold),
                        color = tint
                    )
                    if (!folderName.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = colorScheme.primaryContainer,
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Text(
                                text = folderName,
                                style = MaterialTheme.typography.labelSmall,
                                color = colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        IconButton(onClick = { showFolderMenu = true }, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Filled.DriveFileMove, contentDescription = "Move to collection", tint = colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        }
                        DropdownMenu(expanded = showFolderMenu, onDismissRequest = { showFolderMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("No Collection") },
                                onClick = {
                                    showFolderMenu = false
                                    onMoveToFolder(null)
                                }
                            )
                            folders.forEach { folder ->
                                DropdownMenuItem(
                                    text = { Text(folder.name) },
                                    onClick = {
                                        showFolderMenu = false
                                        onMoveToFolder(folder.id)
                                    }
                                )
                            }
                        }
                    }
                    IconButton(onClick = onRemove, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Filled.BookmarkRemove, contentDescription = "Remove bookmark", tint = colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.content,
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            val displayUrl = item.mediaUrl ?: item.linkUrl
            if (!displayUrl.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = displayUrl,
                    style = MonoTabularStyle.copy(fontSize = 11.sp),
                    color = colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${item.authorName} (${item.authorHandle})",
                style = MaterialTheme.typography.labelSmall,
                color = colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyBookmarksPlaceholder(title: String, subtitle: String) {
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.Bookmarks, null, tint = colorScheme.onSurfaceVariant, modifier = Modifier.size(44.dp))
            Spacer(modifier = Modifier.height(10.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, color = colorScheme.onSurface)
            Spacer(modifier = Modifier.height(4.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = colorScheme.onSurfaceVariant)
        }
    }
}
