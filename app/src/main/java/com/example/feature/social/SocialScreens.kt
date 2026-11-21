package com.example.feature.social

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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalLayoutDirection
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
import com.example.data.model.CommunityItem
import com.example.data.model.DirectMessage
import com.example.data.model.MessageConversation
import com.example.data.model.NotificationItem
import com.example.data.model.NotificationKind
import com.example.data.model.PostItem
import com.example.data.model.UserProfile
import com.example.data.model.XListItem
import com.example.ui.components.PostCard
import com.example.ui.components.UserAvatar
import com.example.ui.components.VerificationBadgeIcon
import com.example.ui.components.formatRelativeTime
import com.example.ui.theme.MonoTabularStyle

private enum class SearchScope(val label: String) {
    ALL("All"),
    POSTS("Posts"),
    CREATORS("Creators"),
    TRENDS("Trends")
}

@Composable
fun SearchScreen(
    socialViewModel: SocialViewModel,
    userProfile: UserProfile,
    folders: List<BookmarkFolder>,
    onOpenDrawer: () -> Unit,
    onBack: () -> Unit,
    onOpenAuthorProfile: (String) -> Unit,
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
    onToggleSaveForOffline: (String) -> Unit,
    onToggleOfflineRead: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val state by socialViewModel.uiState.collectAsStateWithLifecycle()
    var scope by rememberSaveable { mutableStateOf(SearchScope.ALL) }
    val listState = rememberLazyListState()
    val isKeyboardOpen by rememberIsKeyboardOpen()
    var searchFocused by remember { mutableStateOf(false) }
    val folderMap = remember(folders) { folders.associate { it.id to it.name } }

    val posts = if (scope == SearchScope.ALL || scope == SearchScope.POSTS) state.filteredPosts else emptyList()
    val creators = if (scope == SearchScope.ALL || scope == SearchScope.CREATORS) state.filteredCreators else emptyList()
    val trends = if (scope == SearchScope.ALL || scope == SearchScope.TRENDS) state.filteredTrends else emptyList()

    AutoHideHeaderLayout(
        modifier = modifier.testTag("search_screen"),
        listState = listState,
        lockBarsVisible = isKeyboardOpen || searchFocused,
        header = {
            AppTopBar(
                title = "Search XTRA",
                subtitle = "Posts, creators & live trends",
                userName = userProfile.name,
                showBack = true,
                onBack = onBack,
                onOpenDrawer = onOpenDrawer
            )
        }
    ) { scrollModifier, contentPadding ->
        LazyColumn(
            state = listState,
            modifier = scrollModifier,
            contentPadding = contentPadding
        ) {
            item(key = "search_controls_header") {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = socialViewModel::updateSearchQuery,
                        placeholder = { Text("Search posts, @handles, #topics...") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        trailingIcon = {
                            if (state.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { socialViewModel.updateSearchQuery("") }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .onFocusChanged { searchFocused = it.isFocused }
                            .testTag("search_screen_input")
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SearchScope.entries.forEach { item ->
                            val selected = scope == item
                            FilterChip(
                                selected = selected,
                                onClick = { scope = item },
                                label = { Text(item.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = colorScheme.primaryContainer,
                                    selectedLabelColor = colorScheme.primary
                                )
                            )
                        }
                    }
                    HorizontalDivider(color = colorScheme.outline, thickness = 0.8.dp)
                }
            }

            if (posts.isEmpty() && creators.isEmpty() && trends.isEmpty()) {
                item(key = "search_empty") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.Search, null, tint = colorScheme.onSurfaceVariant, modifier = Modifier.size(44.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("No matching results", style = MaterialTheme.typography.titleMedium, color = colorScheme.onSurface)
                            Text(
                                "Try a broader keyword or clear filters.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                if (trends.isNotEmpty()) {
                    item(key = "trends_heading") {
                        Text(
                            text = "Matching Topics",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }
                    items(trends, key = { it.id }) { trend ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { socialViewModel.updateSearchQuery(trend.topic.removePrefix("#")) }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.TrendingUp, null, tint = colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(trend.topic, style = MaterialTheme.typography.titleSmall, color = colorScheme.onSurface)
                                Text("${trend.category} · ${trend.postCountLabel}", style = MonoTabularStyle, color = colorScheme.onSurfaceVariant)
                            }
                        }
                        HorizontalDivider(color = colorScheme.outlineVariant, thickness = 0.6.dp)
                    }
                }

                if (creators.isNotEmpty()) {
                    item(key = "creators_heading") {
                        Text(
                            text = "Creators",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }
                    items(creators, key = { it.id }) { user ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenAuthorProfile(user.id) }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            UserAvatar(name = user.name, isCurrentUser = false, avatarColorHex = user.avatarColorHex, size = 40.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(user.name, style = MaterialTheme.typography.titleSmall, color = colorScheme.onSurface)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    VerificationBadgeIcon(badge = user.badge, size = 14.dp)
                                }
                                Text("${user.handle} · ${user.bio}", style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            if (user.isFollowing) {
                                OutlinedButton(onClick = { socialViewModel.toggleFollowUser(user) }) { Text("Following") }
                            } else {
                                Button(onClick = { socialViewModel.toggleFollowUser(user) }) { Text("Follow") }
                            }
                        }
                        HorizontalDivider(color = colorScheme.outlineVariant, thickness = 0.6.dp)
                    }
                }

                if (posts.isNotEmpty()) {
                    item(key = "posts_heading") {
                        Text(
                            text = "Posts",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }
                    items(posts, key = { it.id }) { post ->
                        PostCard(
                            post = post,
                            showEditHistoryLinks = userProfile.showEditHistoryLinks,
                            folderName = post.bookmarkFolderId?.let { folderMap[it] },
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
}

@Composable
fun NotificationsScreen(
    socialViewModel: SocialViewModel,
    userProfile: UserProfile,
    onOpenDrawer: () -> Unit,
    onOpenPostById: (String) -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenMessages: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val state by socialViewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    AutoHideHeaderLayout(
        modifier = modifier.testTag("notifications_screen"),
        listState = listState,
        header = {
            Column(modifier = Modifier.fillMaxWidth()) {
                AppTopBar(
                    title = "Notifications",
                    subtitle = "${state.unreadNotificationsCount} unread",
                    userName = userProfile.name,
                    showBack = false,
                    onOpenDrawer = onOpenDrawer,
                    actions = {
                        if (state.unreadNotificationsCount > 0) {
                            TextButton(
                                onClick = socialViewModel::markAllNotificationsRead,
                                modifier = Modifier.testTag("mark_all_notifications_read_btn")
                            ) {
                                Icon(Icons.Filled.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Mark all")
                            }
                        }
                    }
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !state.unreadOnlyNotifications,
                        onClick = { socialViewModel.toggleUnreadNotificationsOnly(false) },
                        label = { Text("All (${state.notifications.size})") }
                    )
                    FilterChip(
                        selected = state.unreadOnlyNotifications,
                        onClick = { socialViewModel.toggleUnreadNotificationsOnly(true) },
                        label = { Text("Unread (${state.unreadNotificationsCount})") }
                    )
                }
                HorizontalDivider(color = colorScheme.outline, thickness = 0.8.dp)
            }
        }
    ) { scrollModifier, contentPadding ->
        if (state.visibleNotifications.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Notifications, null, tint = colorScheme.onSurfaceVariant, modifier = Modifier.size(44.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("You're all caught up", style = MaterialTheme.typography.titleMedium, color = colorScheme.onSurface)
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = scrollModifier,
                contentPadding = contentPadding
            ) {
                items(state.visibleNotifications, key = { it.id }) { item ->
                    NotificationRow(
                        item = item,
                        onClick = {
                            socialViewModel.markNotificationRead(item.id)
                            when {
                                item.targetPostId != null -> onOpenPostById(item.targetPostId)
                                item.kind == NotificationKind.DOWNLOAD -> onOpenDownloads()
                                item.kind == NotificationKind.MESSAGE -> onOpenMessages()
                            }
                        }
                    )
                    HorizontalDivider(color = colorScheme.outlineVariant, thickness = 0.7.dp)
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(item: NotificationItem, onClick: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    val (icon, tint) = when (item.kind) {
        NotificationKind.LIKE -> Icons.Filled.Favorite to Color(0xFFF91880)
        NotificationKind.COMMENT -> Icons.Filled.AlternateEmail to colorScheme.primary
        NotificationKind.MESSAGE -> Icons.Filled.Mail to colorScheme.primary
        NotificationKind.FOLLOWER -> Icons.Filled.PersonAdd to colorScheme.tertiary
        NotificationKind.DOWNLOAD -> Icons.Filled.DownloadDone to colorScheme.tertiary
        NotificationKind.SYSTEM -> Icons.Filled.Shield to colorScheme.secondary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (!item.isRead) colorScheme.primaryContainer.copy(alpha = 0.28f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(icon, contentDescription = item.kind.label, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(12.dp))
        UserAvatar(name = item.actorName, isCurrentUser = false, avatarColorHex = item.actorAvatarHex, size = 36.dp)
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.actorName,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = formatRelativeTime(item.createdAtMillis),
                    style = MonoTabularStyle,
                    color = colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = item.message,
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurface
            )
        }
        if (!item.isRead) {
            Box(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(colorScheme.primary)
            )
        }
    }
}

@Composable
fun MessagesScreen(
    socialViewModel: SocialViewModel,
    userProfile: UserProfile,
    onOpenDrawer: () -> Unit,
    onOpenConversation: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val state by socialViewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    AutoHideHeaderLayout(
        modifier = modifier.testTag("messages_screen"),
        listState = listState,
        header = {
            AppTopBar(
                title = "Messages",
                subtitle = "Encrypted direct messaging · ${state.unreadMessagesCount} unread",
                userName = userProfile.name,
                showBack = false,
                onOpenDrawer = onOpenDrawer
            )
        }
    ) { scrollModifier, contentPadding ->
        if (state.conversations.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No conversations yet", style = MaterialTheme.typography.titleMedium, color = colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = scrollModifier,
                contentPadding = contentPadding
            ) {
                items(state.conversations, key = { it.id }) { conv ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                socialViewModel.openConversation(conv.id)
                                onOpenConversation(conv.id)
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .testTag("conversation_${conv.id}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UserAvatar(
                            name = conv.participantName,
                            isCurrentUser = false,
                            avatarColorHex = conv.avatarColorHex,
                            size = 44.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = conv.participantName,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                VerificationBadgeIcon(badge = conv.participantBadge, size = 14.dp)
                                if (conv.isEncrypted) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(Icons.Filled.Lock, contentDescription = "Encrypted", tint = colorScheme.tertiary, modifier = Modifier.size(12.dp))
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = formatRelativeTime(conv.updatedAtMillis),
                                    style = MonoTabularStyle,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = conv.lastMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (conv.unreadCount > 0) colorScheme.onSurface else colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (conv.unreadCount > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Badge(containerColor = colorScheme.primary, contentColor = colorScheme.onPrimary) {
                                Text(conv.unreadCount.toString())
                            }
                        }
                    }
                    HorizontalDivider(color = colorScheme.outlineVariant, thickness = 0.7.dp)
                }
            }
        }
    }
}

@Composable
fun ChatThreadScreen(
    conversationId: String,
    socialViewModel: SocialViewModel,
    userProfile: UserProfile,
    onBack: () -> Unit,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val state by socialViewModel.uiState.collectAsStateWithLifecycle()
    val conversation = state.conversations.firstOrNull { it.id == conversationId }
    val messages by socialViewModel.observeMessages(conversationId).collectAsStateWithLifecycle(initialValue = emptyList())
    var draft by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(conversationId) {
        socialViewModel.openConversation(conversationId)
    }
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .testTag("chat_thread_screen")
    ) {
        AppTopBar(
            title = conversation?.participantName ?: "Direct Message",
            subtitle = conversation?.let { "${it.participantHandle} · Encrypted" } ?: "Encrypted chat",
            userName = userProfile.name,
            showBack = true,
            onBack = onBack,
            onOpenDrawer = onOpenDrawer,
            modifier = Modifier.windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
            )
        )

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages, key = { it.id }) { msg: DirectMessage ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (msg.senderIsCurrentUser) Arrangement.End else Arrangement.Start
                ) {
                    Surface(
                        color = if (msg.senderIsCurrentUser) colorScheme.primary else colorScheme.surfaceContainer,
                        shape = RoundedCornerShape(16.dp),
                        border = if (msg.senderIsCurrentUser) null else BorderStroke(1.dp, colorScheme.outline),
                        modifier = Modifier.widthIn(max = 290.dp)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                            Text(
                                text = msg.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (msg.senderIsCurrentUser) colorScheme.onPrimary else colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = formatRelativeTime(msg.sentAtMillis),
                                style = MonoTabularStyle.copy(fontSize = 10.sp),
                                color = if (msg.senderIsCurrentUser) colorScheme.onPrimary.copy(alpha = 0.78f) else colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = colorScheme.outline, thickness = 0.8.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colorScheme.background)
                .windowInsetsPadding(
                    WindowInsets.navigationBars
                        .union(WindowInsets.ime)
                        .union(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = { Text("Write an encrypted message...") },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_message_input")
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    val target = conversation ?: return@Button
                    val text = draft.trim()
                    if (text.isNotEmpty()) {
                        draft = ""
                        socialViewModel.sendDirectMessage(target, text)
                    }
                },
                enabled = draft.isNotBlank() && conversation != null,
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .size(48.dp)
                    .testTag("chat_send_btn")
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send message")
            }
        }
    }
}

@Composable
fun ListsScreen(
    socialViewModel: SocialViewModel,
    userProfile: UserProfile,
    onOpenDrawer: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val state by socialViewModel.uiState.collectAsStateWithLifecycle()
    var showCreate by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val layoutDirection = LocalLayoutDirection.current

    AutoHideHeaderLayout(
        modifier = modifier.testTag("lists_screen"),
        listState = listState,
        lockBarsVisible = showCreate,
        header = {
            AppTopBar(
                title = "Lists",
                subtitle = "${state.lists.size} curated feeds",
                userName = userProfile.name,
                showBack = true,
                onBack = onBack,
                onOpenDrawer = onOpenDrawer,
                actions = {
                    IconButton(onClick = { showCreate = true }, modifier = Modifier.testTag("create_list_btn")) {
                        Icon(Icons.Filled.Add, contentDescription = "Create list", tint = colorScheme.primary)
                    }
                }
            )
        }
    ) { scrollModifier, contentPadding ->
        LazyColumn(
            state = listState,
            modifier = scrollModifier,
            contentPadding = PaddingValues(
                start = contentPadding.calculateStartPadding(layoutDirection) + 16.dp,
                end = contentPadding.calculateEndPadding(layoutDirection) + 16.dp,
                top = contentPadding.calculateTopPadding() + 12.dp,
                bottom = contentPadding.calculateBottomPadding()
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(state.lists, key = { it.id }) { item: XListItem ->
                Surface(
                    color = colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ListAlt, null, tint = colorScheme.primary, modifier = Modifier.size(26.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(item.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = colorScheme.onSurface)
                                if (item.isPrivate) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(Icons.Filled.Lock, contentDescription = "Private list", tint = colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                                }
                            }
                            Text(item.description, style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
                            Text("${item.memberCount} members · ${item.subscriberCount} subscribers", style = MonoTabularStyle, color = colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        if (item.isJoined) {
                            OutlinedButton(onClick = { socialViewModel.toggleListJoined(item) }) {
                                Text("Subscribed")
                            }
                        } else {
                            Button(onClick = { socialViewModel.toggleListJoined(item) }) {
                                Text("Subscribe")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreate) {
        var name by rememberSaveable { mutableStateOf("") }
        var desc by rememberSaveable { mutableStateOf("") }
        var isPrivate by rememberSaveable { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text("Create XTRA List") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("List name") }, singleLine = true)
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") })
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isPrivate, onCheckedChange = { isPrivate = it })
                        Text("Make private")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        socialViewModel.createXList(name, desc, isPrivate)
                        showCreate = false
                    },
                    enabled = name.isNotBlank()
                ) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showCreate = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun CommunitiesScreen(
    socialViewModel: SocialViewModel,
    userProfile: UserProfile,
    onOpenDrawer: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val state by socialViewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val layoutDirection = LocalLayoutDirection.current

    AutoHideHeaderLayout(
        modifier = modifier.testTag("communities_screen"),
        listState = listState,
        header = {
            AppTopBar(
                title = "Communities",
                subtitle = "${state.communities.count { it.isJoined }} joined",
                userName = userProfile.name,
                showBack = true,
                onBack = onBack,
                onOpenDrawer = onOpenDrawer
            )
        }
    ) { scrollModifier, contentPadding ->
        LazyColumn(
            state = listState,
            modifier = scrollModifier,
            contentPadding = PaddingValues(
                start = contentPadding.calculateStartPadding(layoutDirection) + 16.dp,
                end = contentPadding.calculateEndPadding(layoutDirection) + 16.dp,
                top = contentPadding.calculateTopPadding() + 12.dp,
                bottom = contentPadding.calculateBottomPadding()
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(state.communities, key = { it.id }) { community: CommunityItem ->
                val accent = Color(community.accentHex)
                Surface(
                    color = colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(accent.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Groups, contentDescription = null, tint = accent)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(community.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = colorScheme.onSurface)
                                Text("${community.category} · ${community.memberCount} members", style = MonoTabularStyle, color = accent)
                            }
                            if (community.isJoined) {
                                OutlinedButton(onClick = { socialViewModel.toggleCommunityJoined(community) }) {
                                    Text("Joined")
                                }
                            } else {
                                Button(onClick = { socialViewModel.toggleCommunityJoined(community) }) {
                                    Text("Join")
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(community.description, style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun TrendingScreen(
    socialViewModel: SocialViewModel,
    userProfile: UserProfile,
    onOpenDrawer: () -> Unit,
    onBack: () -> Unit,
    onSelectTopic: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val state by socialViewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val layoutDirection = LocalLayoutDirection.current

    AutoHideHeaderLayout(
        modifier = modifier.testTag("trending_screen"),
        listState = listState,
        header = {
            AppTopBar(
                title = "Trending",
                subtitle = if (state.adFreeEnabled) "Ad-Free Mode · Sponsored trends hidden" else "All trends",
                userName = userProfile.name,
                showBack = true,
                onBack = onBack,
                onOpenDrawer = onOpenDrawer
            )
        }
    ) { scrollModifier, contentPadding ->
        LazyColumn(
            state = listState,
            modifier = scrollModifier,
            contentPadding = PaddingValues(
                start = contentPadding.calculateStartPadding(layoutDirection) + 16.dp,
                end = contentPadding.calculateEndPadding(layoutDirection) + 16.dp,
                top = contentPadding.calculateTopPadding() + 12.dp,
                bottom = contentPadding.calculateBottomPadding()
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(state.visibleTrends, key = { _, t -> t.id }) { idx, trend ->
                Surface(
                    color = colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, colorScheme.outline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSelectTopic(trend.topic.removePrefix("#")) }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "#${idx + 1}",
                            style = MonoTabularStyle.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                            color = colorScheme.primary,
                            modifier = Modifier.width(34.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(trend.category, style = MaterialTheme.typography.labelSmall, color = colorScheme.onSurfaceVariant)
                            Text(trend.topic, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = colorScheme.onSurface)
                            Text(trend.postCountLabel, style = MonoTabularStyle, color = colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Filled.TrendingUp, contentDescription = null, tint = colorScheme.primary)
                    }
                }
            }
        }
    }
}
