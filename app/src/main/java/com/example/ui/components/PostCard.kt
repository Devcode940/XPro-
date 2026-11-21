package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.MarkChatUnread
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.PollData
import com.example.data.model.PostItem
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.DangerUndo
import com.example.ui.theme.EmeraldAdShield
import com.example.ui.theme.GoldOrg
import com.example.ui.theme.HairlineDivider
import com.example.ui.theme.HeartPink
import com.example.ui.theme.MonoTabularStyle
import com.example.ui.theme.OledBlack
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.XBlue
import com.example.ui.theme.XBlueContainer

@Composable
fun PostCard(
    post: PostItem,
    showEditHistoryLinks: Boolean,
    folderName: String?,
    onLike: () -> Unit,
    onRepost: () -> Unit,
    onReply: () -> Unit,
    onBookmark: () -> Unit,
    onVotePoll: (String) -> Unit,
    onOpenReaderMode: () -> Unit,
    onOpenEdit: () -> Unit,
    onOpenEditHistory: () -> Unit,
    onToggleHighlight: () -> Unit,
    onDeletePost: () -> Unit,
    onOpenMediaLightbox: () -> Unit,
    onToggleSaveForOffline: () -> Unit = {},
    onToggleOfflineRead: () -> Unit = {},
    onAuthorClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    var menuExpanded by remember { mutableStateOf(false) }
    var isTextExpanded by remember(post.id) { mutableStateOf(false) }
    val isLongText = !post.isLongForm && post.content.length > 260

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colorScheme.background)
            .testTag("post_card_${post.id}")
    ) {
        if (post.isSponsored) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 66.dp, top = 10.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = post.sponsorLabel ?: "Promoted Ad",
                    style = MonoTabularStyle.copy(fontSize = 11.sp),
                    color = GoldOrg
                )
            }
        } else if (post.isHighlight) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 44.dp, top = 10.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = GoldOrg,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Highlighted on Profile",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMuted
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                UserAvatar(
                    name = post.authorName,
                    isCurrentUser = post.isCurrentUser,
                    avatarColorHex = post.authorAvatarColorHex,
                    size = 42.dp,
                    modifier = Modifier.clickable(onClick = onAuthorClick)
                )
                if (post.threadPosts.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(48.dp)
                            .background(HairlineDivider)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onAuthorClick),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = post.authorName,
                            style = MaterialTheme.typography.titleSmall,
                            color = colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        VerificationBadgeIcon(badge = post.authorBadge, size = 15.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${post.authorHandle} · ${formatRelativeTime(post.createdAtMillis)}",
                            style = MonoTabularStyle.copy(fontSize = 12.5.sp),
                            color = colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("post_more_${post.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MoreHoriz,
                                contentDescription = "Post options",
                                tint = TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            containerColor = SurfaceElevated
                        ) {
                            if (post.isCurrentUser) {
                                DropdownMenuItem(
                                    text = { Text("Edit Post", color = TextPrimary) },
                                    leadingIcon = {
                                        Icon(Icons.Filled.Edit, null, tint = XBlue)
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onOpenEdit()
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (post.isHighlight) "Remove from Highlights" else "Highlight on Profile",
                                            color = TextPrimary
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            if (post.isHighlight) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                            null,
                                            tint = GoldOrg
                                        )
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onToggleHighlight()
                                    }
                                )
                            }
                            if (post.revisions.isNotEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("View Edit History", color = TextPrimary) },
                                    leadingIcon = {
                                        Icon(Icons.Filled.History, null, tint = XBlue)
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onOpenEditHistory()
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Save to Bookmark Folder", color = TextPrimary) },
                                leadingIcon = {
                                    Icon(Icons.Outlined.BookmarkBorder, null, tint = TextPrimary)
                                },
                                onClick = {
                                    menuExpanded = false
                                    onBookmark()
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (post.isSavedForOffline) "Remove from Read Later" else "Save for Offline (Read Later)",
                                        color = TextPrimary
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (post.isSavedForOffline) Icons.Filled.Bolt else Icons.Outlined.CloudDownload,
                                        contentDescription = null,
                                        tint = XBlue
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onToggleSaveForOffline()
                                }
                            )
                            if (post.isSavedForOffline) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (post.isOfflineRead) "Mark as Unread" else "Mark as Read",
                                            color = TextPrimary
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (post.isOfflineRead) Icons.Outlined.MarkChatUnread else Icons.Filled.CheckCircle,
                                            contentDescription = null,
                                            tint = EmeraldAdShield
                                        )
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onToggleOfflineRead()
                                    }
                                )
                            }
                            if (post.isLongForm || post.threadPosts.isNotEmpty() || post.content.length > 200) {
                                DropdownMenuItem(
                                    text = { Text("Open Reader Mode", color = TextPrimary) },
                                    leadingIcon = {
                                        Icon(Icons.AutoMirrored.Rounded.MenuBook, null, tint = XBlue)
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onOpenReaderMode()
                                    }
                                )
                            }
                            if (post.isCurrentUser) {
                                DropdownMenuItem(
                                    text = { Text("Delete Post", color = DangerUndo) },
                                    leadingIcon = {
                                        Icon(Icons.Filled.DeleteOutline, null, tint = DangerUndo)
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onDeletePost()
                                    }
                                )
                            }
                        }
                    }
                }

                if (post.isSavedForOffline) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        OfflineCachedBadge(
                            isRead = post.isOfflineRead,
                            onToggleRead = onToggleOfflineRead
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (post.threadPosts.isNotEmpty()) {
                                "Thread cached in Room DB (${post.threadPosts.size} posts)"
                            } else if (post.isLongForm) {
                                "Article cached in Room DB"
                            } else {
                                "Cached for offline reading"
                            },
                            style = MonoTabularStyle.copy(fontSize = 11.sp),
                            color = TextMuted
                        )
                    }
                }

                // Body: Long-Form Article Card vs Standard Post
                if (post.isLongForm && post.articleTitle != null) {
                    ArticlePreviewCard(
                        post = post,
                        onOpenReaderMode = onOpenReaderMode,
                        onOpenMediaLightbox = onOpenMediaLightbox
                    )
                } else {
                    val displayedText = if (isLongText && !isTextExpanded) {
                        post.content.take(245).trimEnd() + "…"
                    } else {
                        post.content
                    }
                    RichPostText(
                        text = displayedText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = colorScheme.onSurface
                    )
                    if (isLongText) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isTextExpanded) "Show less" else "Show more",
                            style = MaterialTheme.typography.labelLarge,
                            color = XBlue,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { isTextExpanded = !isTextExpanded }
                                .padding(vertical = 4.dp)
                        )
                    }
                }

                // Media Attachment (for non-article posts)
                if (!post.isLongForm && (post.mediaResKey != null || post.mediaUri != null)) {
                    Spacer(modifier = Modifier.height(10.dp))
                    PostMediaAttachment(
                        mediaResKey = post.mediaResKey,
                        mediaUri = post.mediaUri,
                        onClick = onOpenMediaLightbox
                    )
                }

                // Poll Attachment
                if (post.poll != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    PollAttachmentCard(
                        poll = post.poll,
                        onVote = onVotePoll
                    )
                }

                // Thread CTA
                if (post.threadPosts.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onOpenReaderMode)
                            .testTag("read_thread_${post.id}"),
                        color = XBlueContainer,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, XBlue.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.MenuBook,
                                    contentDescription = null,
                                    tint = XBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Show this thread · ${post.threadPosts.size} posts",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = XBlue
                                )
                            }
                            Text(
                                text = "Reader Mode",
                                style = MonoTabularStyle.copy(fontSize = 11.sp),
                                color = XBlue
                            )
                        }
                    }
                }

                // Edit History Link
                if (showEditHistoryLinks && post.editedAtMillis != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(onClick = onOpenEditHistory)
                            .padding(vertical = 2.dp)
                            .testTag("view_history_${post.id}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.History,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Last edited ${formatRelativeTime(post.editedAtMillis)} ago · ",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                        Text(
                            text = "View edit history (${post.revisions.size})",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = XBlue
                        )
                    }
                }

                // Bookmark Folder label if bookmarked in folder
                if (post.isBookmarked && folderName != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Saved in $folderName",
                        style = MonoTabularStyle.copy(fontSize = 11.sp),
                        color = XBlue
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Engagement Row
                EngagementBar(
                    post = post,
                    onReply = onReply,
                    onRepost = onRepost,
                    onLike = onLike,
                    onBookmark = onBookmark
                )
            }
        }
        HorizontalDivider(thickness = 0.8.dp, color = colorScheme.outline)
    }
}

@Composable
private fun ArticlePreviewCard(
    post: PostItem,
    onOpenReaderMode: () -> Unit,
    onOpenMediaLightbox: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val (readMins, words) = remember(post.content) { estimateReadingStats(post.content) }
    Spacer(modifier = Modifier.height(6.dp))
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onOpenReaderMode)
            .testTag("open_article_${post.id}"),
        color = colorScheme.surfaceContainer,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, colorScheme.outline)
    ) {
        Column {
            val bundledRes = resolveBundledMedia(post.mediaResKey)
            if (bundledRes != null) {
                Image(
                    painter = painterResource(id = bundledRes),
                    contentDescription = post.articleTitle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .clickable(onClick = onOpenMediaLightbox),
                    contentScale = ContentScale.Crop
                )
            } else if (post.mediaUri != null) {
                AsyncImage(
                    model = post.mediaUri,
                    contentDescription = post.articleTitle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .clickable(onClick = onOpenMediaLightbox),
                    contentScale = ContentScale.Crop
                )
            }
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "LONG-FORM ARTICLE · $readMins MIN READ · $words WORDS",
                    style = MonoTabularStyle.copy(fontSize = 10.5.sp),
                    color = colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = post.articleTitle.orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                    color = colorScheme.onSurface
                )
                if (!post.articleSubtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = post.articleSubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.MenuBook,
                        contentDescription = null,
                        tint = XBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Open Distraction-Free Reader Mode",
                        style = MaterialTheme.typography.labelLarge,
                        color = XBlue
                    )
                }
            }
        }
    }
}

@Composable
private fun PostMediaAttachment(
    mediaResKey: String?,
    mediaUri: String?,
    onClick: () -> Unit
) {
    val bundled = resolveBundledMedia(mediaResKey)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, HairlineDivider, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        if (bundled != null) {
            Image(
                painter = painterResource(id = bundled),
                contentDescription = "Post attachment",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp),
                contentScale = ContentScale.Crop
            )
        } else if (mediaUri != null) {
            AsyncImage(
                model = mediaUri,
                contentDescription = "Post photo attachment",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
private fun PollAttachmentCard(
    poll: PollData,
    onVote: (String) -> Unit
) {
    val totalVotes = poll.options.sumOf { it.votes }.coerceAtLeast(1)
    val hasVoted = poll.selectedOptionId != null
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
            .background(SurfaceCard)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        poll.options.forEach { option ->
            val isSelected = poll.selectedOptionId == option.id
            val pct = option.votes.toFloat() / totalVotes.toFloat()
            val animatedFraction by animateFloatAsState(
                targetValue = if (hasVoted) pct else 0f,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "poll_bar"
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .border(
                        width = 1.dp,
                        color = if (isSelected) XBlue else BorderSubtle,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable { onVote(option.id) }
                    .padding(1.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedFraction.coerceIn(0f, 1f))
                        .matchParentSize()
                        .background(
                            color = if (isSelected) XBlue.copy(alpha = 0.25f) else SurfaceElevated,
                            shape = RoundedCornerShape(9.dp)
                        )
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = TextPrimary
                        )
                        if (isSelected) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Selected option",
                                tint = XBlue,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                    if (hasVoted) {
                        Text(
                            text = "${(pct * 100).toInt()}%",
                            style = MonoTabularStyle,
                            color = if (isSelected) XBlue else TextPrimary
                        )
                    }
                }
            }
        }
        Text(
            text = "${formatCompactCount(totalVotes)} votes · ${poll.durationLabel}",
            style = MonoTabularStyle.copy(fontSize = 11.5.sp),
            color = TextMuted
        )
    }
}

@Composable
private fun EngagementBar(
    post: PostItem,
    onReply: () -> Unit,
    onRepost: () -> Unit,
    onLike: () -> Unit,
    onBookmark: () -> Unit
) {
    val likeColor by animateColorAsState(
        targetValue = if (post.isLiked) HeartPink else TextMuted,
        label = "like_color"
    )
    val likeScale by animateFloatAsState(
        targetValue = if (post.isLiked) 1.15f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "like_scale"
    )
    val repostColor by animateColorAsState(
        targetValue = if (post.isReposted) EmeraldAdShield else TextMuted,
        label = "repost_color"
    )
    val bookmarkColor by animateColorAsState(
        targetValue = if (post.isBookmarked) XBlue else TextMuted,
        label = "bookmark_color"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ActionMetricItem(
            icon = Icons.AutoMirrored.Outlined.Chat,
            countText = formatCompactCount(post.replies),
            tint = TextMuted,
            contentDesc = "Reply",
            onClick = onReply,
            modifier = Modifier.testTag("reply_btn_${post.id}")
        )
        ActionMetricItem(
            icon = Icons.Filled.Repeat,
            countText = formatCompactCount(post.reposts),
            tint = repostColor,
            contentDesc = "Repost",
            onClick = onRepost,
            modifier = Modifier.testTag("repost_btn_${post.id}")
        )
        ActionMetricItem(
            icon = if (post.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            countText = formatCompactCount(post.likes),
            tint = likeColor,
            contentDesc = "Like",
            onClick = onLike,
            iconScale = likeScale,
            modifier = Modifier.testTag("like_btn_${post.id}")
        )
        ActionMetricItem(
            icon = Icons.Outlined.BarChart,
            countText = formatCompactCount(post.views),
            tint = TextMuted,
            contentDesc = "Views",
            onClick = {},
            enabled = false
        )
        IconButton(
            onClick = onBookmark,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("bookmark_btn_${post.id}")
        ) {
            Icon(
                imageVector = if (post.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                contentDescription = "Save to bookmark folder",
                tint = bookmarkColor,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

@Composable
private fun ActionMetricItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    countText: String,
    tint: androidx.compose.ui.graphics.Color,
    contentDesc: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconScale: Float = 1f,
    enabled: Boolean = true
) {
    Row(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(999.dp))
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDesc,
            tint = tint,
            modifier = Modifier
                .size(18.dp)
                .scale(iconScale)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = countText,
            style = MonoTabularStyle.copy(fontSize = 12.sp),
            color = tint
        )
    }
}
