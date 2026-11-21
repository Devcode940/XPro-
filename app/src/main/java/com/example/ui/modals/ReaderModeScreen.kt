package com.example.ui.modals

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.FavoriteBorder
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.PostItem
import com.example.data.model.ReaderTypography
import com.example.ui.components.MarkdownArticleBody
import com.example.ui.components.UserAvatar
import com.example.ui.components.VerificationBadgeIcon
import com.example.ui.components.estimateReadingStats
import com.example.ui.components.formatCompactCount
import com.example.ui.components.formatTimeClock
import com.example.ui.components.resolveBundledMedia
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.DmSansFontFamily
import com.example.ui.theme.HairlineDivider
import com.example.ui.theme.HeartPink
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MonoTabularStyle
import com.example.ui.theme.OledBlack
import com.example.ui.theme.ReaderWarmDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.XBlue
import com.example.ui.theme.XBlueContainer

@Composable
fun ReaderModeOverlay(
    post: PostItem,
    fontSizeSp: Float,
    typography: ReaderTypography,
    warmSurface: Boolean,
    onAdjustFontSize: (Float) -> Unit,
    onSelectTypography: (ReaderTypography) -> Unit,
    onToggleWarmSurface: () -> Unit,
    onToggleLike: () -> Unit,
    onOpenBookmark: () -> Unit,
    onToggleSaveForOffline: () -> Unit = {},
    onClose: () -> Unit
) {
    BackHandler(onBack = onClose)

    val canvasColor = if (warmSurface) ReaderWarmDark else OledBlack
    val chosenFamily = when (typography) {
        ReaderTypography.SANS -> DmSansFontFamily
        ReaderTypography.SERIF -> FontFamily.Serif
        ReaderTypography.MONO -> JetBrainsMonoFontFamily
    }
    val combinedText = if (post.threadPosts.isNotEmpty()) {
        post.threadPosts.joinToString("\n\n")
    } else post.content
    val (minutes, words) = remember(combinedText) { estimateReadingStats(combinedText) }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("reader_mode_screen"),
        color = canvasColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            // Top Reader Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onClose, modifier = Modifier.testTag("reader_close_btn")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Exit Reader Mode",
                            tint = TextPrimary
                        )
                    }
                    Column {
                        Text(
                            text = if (post.threadPosts.isNotEmpty()) "Thread Reader" else "Reader Mode",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary
                        )
                        Text(
                            text = "$minutes min read · $words words",
                            style = MonoTabularStyle.copy(fontSize = 11.sp),
                            color = XBlue
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { onAdjustFontSize(-1.5f) }) {
                        Text("A-", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                    }
                    TextButton(onClick = { onAdjustFontSize(1.5f) }) {
                        Text("A+", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                    }
                    IconButton(onClick = onToggleWarmSurface) {
                        Icon(
                            imageVector = Icons.Filled.Contrast,
                            contentDescription = "Toggle reader contrast",
                            tint = if (warmSurface) XBlue else TextMuted
                        )
                    }
                }
            }

            // Font family switcher strip & offline button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ReaderTypography.entries.forEach { option ->
                    val selected = typography == option
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectTypography(option) },
                        label = {
                            Text(
                                text = option.name.lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
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
                IconButton(onClick = onToggleSaveForOffline) {
                    Icon(
                        imageVector = if (post.isSavedForOffline) Icons.Filled.Bolt else Icons.Outlined.CloudDownload,
                        contentDescription = "Save for Offline",
                        tint = if (post.isSavedForOffline) XBlue else TextMuted
                    )
                }
                IconButton(onClick = onToggleLike) {
                    Icon(
                        imageVector = if (post.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (post.isLiked) HeartPink else TextMuted
                    )
                }
                IconButton(onClick = onOpenBookmark) {
                    Icon(
                        imageVector = if (post.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = if (post.isBookmarked) XBlue else TextMuted
                    )
                }
            }

            if (post.isSavedForOffline) {
                Surface(
                    color = XBlueContainer,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, XBlue.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Bolt, null, tint = XBlue, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Cached in local Room SQLite · Read later offline",
                                style = MonoTabularStyle.copy(fontSize = 11.sp),
                                color = XBlue
                            )
                        }
                        Text(
                            text = if (post.isOfflineRead) "Read ✓" else "Unread",
                            style = MonoTabularStyle.copy(fontSize = 11.sp),
                            color = TextPrimary
                        )
                    }
                }
            }

            HorizontalDivider(color = HairlineDivider, thickness = 0.8.dp)

            // Main Reading Canvas
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 680.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        UserAvatar(
                            name = post.authorName,
                            isCurrentUser = post.isCurrentUser,
                            avatarColorHex = post.authorAvatarColorHex,
                            size = 44.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(post.authorName, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                Spacer(modifier = Modifier.width(5.dp))
                                VerificationBadgeIcon(badge = post.authorBadge, size = 16.dp)
                            }
                            Text(
                                text = "${post.authorHandle} · ${formatTimeClock(post.createdAtMillis)}",
                                style = MonoTabularStyle.copy(fontSize = 11.5.sp),
                                color = TextMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    if (!post.articleTitle.isNullOrBlank()) {
                        Text(
                            text = post.articleTitle,
                            style = MaterialTheme.typography.displaySmall,
                            color = TextPrimary
                        )
                        if (!post.articleSubtitle.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = post.articleSubtitle,
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    val bundled = resolveBundledMedia(post.mediaResKey)
                    if (bundled != null) {
                        Image(
                            painter = painterResource(id = bundled),
                            contentDescription = post.articleTitle ?: "Cover",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.dp, HairlineDivider, RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                    } else if (post.mediaUri != null) {
                        AsyncImage(
                            model = post.mediaUri,
                            contentDescription = "Attachment",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                    }

                    if (post.threadPosts.isNotEmpty()) {
                        post.threadPosts.forEachIndexed { idx, part ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(XBlueContainer)
                                            .border(1.dp, XBlue.copy(alpha = 0.5f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${idx + 1}",
                                            style = MonoTabularStyle.copy(fontSize = 11.sp),
                                            color = XBlue
                                        )
                                    }
                                    if (idx < post.threadPosts.lastIndex) {
                                        Box(
                                            modifier = Modifier
                                                .width(2.dp)
                                                .height(48.dp)
                                                .background(HairlineDivider)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Text(
                                    text = part,
                                    style = TextStyle(
                                        fontFamily = chosenFamily,
                                        fontSize = fontSizeSp.sp,
                                        lineHeight = (fontSizeSp * 1.55f).sp,
                                        color = TextPrimary
                                    ),
                                    modifier = Modifier.padding(bottom = 18.dp)
                                )
                            }
                        }
                    } else {
                        MarkdownArticleBody(
                            markdown = post.content,
                            fontFamily = chosenFamily,
                            fontSizeSp = fontSizeSp
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = HairlineDivider, thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "${formatCompactCount(post.views)} views · ${formatCompactCount(post.likes)} likes · ${formatCompactCount(post.reposts)} reposts",
                        style = MonoTabularStyle,
                        color = TextMuted
                    )
                }
            }
        }
    }
}
