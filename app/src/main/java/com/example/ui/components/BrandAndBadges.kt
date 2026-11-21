package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.VerificationBadge
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.EmeraldAdShield
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.FounderPrism
import com.example.ui.theme.GoldOrg
import com.example.ui.theme.GrayOfficial
import com.example.ui.theme.HairlineDivider
import com.example.ui.theme.MonoTabularStyle
import com.example.ui.theme.OledBlack
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.XBlue
import com.example.ui.theme.XBlueContainer
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

@Composable
fun XProLogo(
    modifier: Modifier = Modifier,
    size: Dp = 26.dp,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    Canvas(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = "X Pro logo" }
    ) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.085f

        // Double-line stylized X monogram
        drawLine(
            color = tint,
            start = Offset(w * 0.16f, h * 0.14f),
            end = Offset(w * 0.68f, h * 0.86f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.32f, h * 0.14f),
            end = Offset(w * 0.84f, h * 0.86f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.82f, h * 0.14f),
            end = Offset(w * 0.18f, h * 0.86f),
            strokeWidth = stroke * 1.05f,
            cap = StrokeCap.Round
        )
        // Subtle electric blue pro spark at top right
        drawCircle(
            color = XBlue,
            radius = w * 0.09f,
            center = Offset(w * 0.86f, h * 0.18f)
        )
    }
}

@Composable
fun VerificationBadgeIcon(
    badge: VerificationBadge,
    modifier: Modifier = Modifier,
    size: Dp = 17.dp
) {
    if (badge == VerificationBadge.NONE) return
    val desc = badge.label

    when (badge) {
        VerificationBadge.GOLD -> {
            Box(
                modifier = modifier
                    .size(size)
                    .clip(RoundedCornerShape(4.dp))
                    .background(GoldOrg)
                    .semantics { contentDescription = desc },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = OledBlack,
                    modifier = Modifier.size(size * 0.72f)
                )
            }
        }
        VerificationBadge.FOUNDER -> {
            Box(
                modifier = modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(XBlue, FounderPrism, GoldOrg)
                        )
                    )
                    .semantics { contentDescription = desc },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = TextPrimary,
                    modifier = Modifier.size(size * 0.7f)
                )
            }
        }
        VerificationBadge.BLUE, VerificationBadge.GRAY -> {
            val fillColor = if (badge == VerificationBadge.BLUE) XBlue else GrayOfficial
            Canvas(
                modifier = modifier
                    .size(size)
                    .semantics { contentDescription = desc }
            ) {
                val r = this.size.minDimension / 2f
                val cx = center.x
                val cy = center.y
                // 8-point subtle scalloped seal
                val path = Path()
                val points = 16
                for (i in 0 until points) {
                    val angle = (i * 2.0 * Math.PI / points)
                    val radius = if (i % 2 == 0) r else r * 0.84f
                    val x = cx + (radius * kotlin.math.cos(angle)).toFloat()
                    val y = cy + (radius * kotlin.math.sin(angle)).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
                drawPath(path = path, color = fillColor)
                // Crisp checkmark
                val check = Path().apply {
                    moveTo(cx - r * 0.36f, cy + r * 0.02f)
                    lineTo(cx - r * 0.08f, cy + r * 0.30f)
                    lineTo(cx + r * 0.38f, cy - r * 0.24f)
                }
                drawPath(
                    path = check,
                    color = OledBlack,
                    style = Stroke(width = r * 0.22f, cap = StrokeCap.Round)
                )
            }
        }
        VerificationBadge.NONE -> Unit
    }
}

@Composable
fun UserAvatar(
    name: String,
    isCurrentUser: Boolean,
    avatarColorHex: Long,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp
) {
    if (isCurrentUser) {
        Image(
            painter = painterResource(id = R.drawable.img_user_avatar),
            contentDescription = "$name avatar",
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .border(1.dp, HairlineDivider, CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        val initials = name.trim()
            .split(" ")
            .filter { it.isNotEmpty() }
            .take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }
            .ifEmpty { "X" }
        val accent = Color(avatarColorHex)
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(accent.copy(alpha = 0.28f), MaterialTheme.colorScheme.surfaceContainerHigh)
                    )
                )
                .border(1.dp, accent.copy(alpha = 0.55f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = (size.value * 0.34f).sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun AdShieldPill(
    adFreeEnabled: Boolean,
    blockedCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val bgColor = if (adFreeEnabled) EmeraldContainer else colorScheme.surfaceContainerHigh
    val borderColor = if (adFreeEnabled) EmeraldAdShield.copy(alpha = 0.45f) else colorScheme.outlineVariant
    val contentColor = if (adFreeEnabled) EmeraldAdShield else colorScheme.onSurfaceVariant

    Surface(
        modifier = modifier
            .testTag("ad_shield_pill")
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick),
        color = bgColor,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (adFreeEnabled) Icons.Filled.Shield else Icons.Outlined.Shield,
                contentDescription = if (adFreeEnabled) "Ad-Free Mode Active" else "Ad-Free Mode Paused",
                tint = contentColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = if (adFreeEnabled) "Ad-Free · $blockedCount" else "Ads On",
                style = MonoTabularStyle.copy(fontSize = 11.5.sp),
                color = contentColor
            )
        }
    }
}

@Composable
fun OfflineIndicatorPill(
    offlineActive: Boolean,
    cachedCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val bgColor = if (offlineActive) colorScheme.primaryContainer else colorScheme.surfaceContainerHigh
    val borderColor = if (offlineActive) colorScheme.primary.copy(alpha = 0.5f) else colorScheme.outlineVariant
    val contentColor = if (offlineActive) colorScheme.primary else colorScheme.onSurfaceVariant

    Surface(
        modifier = modifier
            .testTag("offline_mode_pill")
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick),
        color = bgColor,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (offlineActive) Icons.Filled.Bolt else Icons.Outlined.CloudOff,
                contentDescription = if (offlineActive) "Offline Mode Active" else "Toggle Offline Mode",
                tint = contentColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = if (offlineActive) "Offline · $cachedCount" else "Online",
                style = MonoTabularStyle.copy(fontSize = 11.5.sp),
                color = contentColor
            )
        }
    }
}

@Composable
fun OfflineCachedBadge(
    isRead: Boolean,
    onToggleRead: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(XBlueContainer)
            .border(1.dp, XBlue.copy(alpha = 0.35f), RoundedCornerShape(999.dp))
            .clickable(onClick = onToggleRead)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isRead) Icons.Filled.CheckCircle else Icons.Filled.Bolt,
            contentDescription = null,
            tint = if (isRead) EmeraldAdShield else XBlue,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = if (isRead) "Cached · Read" else "Cached · Unread",
            style = MonoTabularStyle.copy(fontSize = 10.5.sp),
            color = if (isRead) EmeraldAdShield else XBlue
        )
    }
}

@Composable
fun GrokLogoMark(
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    Canvas(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = "Grok powered by X" }
    ) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.11f
        // Outer rounded square / circle frame with iconic diagonal slash
        drawRoundRect(
            color = tint,
            cornerRadius = CornerRadius(w * 0.28f, w * 0.28f),
            style = Stroke(width = stroke)
        )
        drawLine(
            color = XBlue,
            start = Offset(w * 0.24f, h * 0.78f),
            end = Offset(w * 0.78f, h * 0.22f),
            strokeWidth = stroke * 1.15f,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun TopRightMenuButton(
    onOpenSettings: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenArticles: () -> Unit,
    onOpenMediaDownloader: () -> Unit,
    onOpenGrok: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.testTag("top_right_menu_btn")
        ) {
            Icon(
                imageVector = Icons.Filled.Menu,
                contentDescription = "Open menu",
                tint = colorScheme.onSurface
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = colorScheme.surfaceContainer,
            modifier = Modifier
                .border(1.dp, colorScheme.outline, RoundedCornerShape(12.dp))
                .testTag("top_right_dropdown_menu")
        ) {
            DropdownMenuItem(
                text = { Text("Settings", color = colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = null,
                        tint = colorScheme.primary
                    )
                },
                onClick = {
                    expanded = false
                    onOpenSettings()
                },
                modifier = Modifier.testTag("menu_item_settings")
            )
            DropdownMenuItem(
                text = { Text("Grok AI (Powered by X)", color = colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium) },
                leadingIcon = {
                    GrokLogoMark(size = 18.dp, tint = colorScheme.onSurface)
                },
                onClick = {
                    expanded = false
                    onOpenGrok()
                },
                modifier = Modifier.testTag("menu_item_grok")
            )
            DropdownMenuItem(
                text = { Text("Profile", color = colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        tint = colorScheme.onSurface
                    )
                },
                onClick = {
                    expanded = false
                    onOpenProfile()
                },
                modifier = Modifier.testTag("menu_item_profile")
            )
            DropdownMenuItem(
                text = { Text("Bookmark Folders", color = colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Bookmark,
                        contentDescription = null,
                        tint = GoldOrg
                    )
                },
                onClick = {
                    expanded = false
                    onOpenBookmarks()
                },
                modifier = Modifier.testTag("menu_item_bookmarks")
            )
            DropdownMenuItem(
                text = { Text("Articles & Threads", color = colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Article,
                        contentDescription = null,
                        tint = colorScheme.onSurface
                    )
                },
                onClick = {
                    expanded = false
                    onOpenArticles()
                },
                modifier = Modifier.testTag("menu_item_articles")
            )
            DropdownMenuItem(
                text = { Text("Media Downloader", color = colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Download,
                        contentDescription = null,
                        tint = EmeraldAdShield
                    )
                },
                onClick = {
                    expanded = false
                    onOpenMediaDownloader()
                },
                modifier = Modifier.testTag("menu_item_media_downloader")
            )
        }
    }
}

