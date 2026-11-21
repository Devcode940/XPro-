package com.example.ui.modals

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.PostItem
import com.example.data.model.TrendItem
import com.example.data.model.UserProfile
import com.example.data.model.VerificationBadge
import com.example.ui.components.VerificationBadgeIcon
import com.example.ui.components.resolveBundledMedia
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.EmeraldAdShield
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.HairlineDivider
import com.example.ui.theme.MonoTabularStyle
import com.example.ui.theme.OledBlack
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.XBlue
import com.example.ui.theme.XBlueContainer

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PremiumStudioDialog(
    userProfile: UserProfile,
    blockedPostAds: List<PostItem>,
    blockedTrendAds: List<TrendItem>,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        handle: String,
        bio: String,
        badge: VerificationBadge,
        undoDelaySeconds: Int,
        adFreeEnabled: Boolean,
        extendedPostsEnabled: Boolean,
        showEditHistoryLinks: Boolean,
        autoExpandThreads: Boolean
    ) -> Unit
) {
    var name by remember(userProfile) { mutableStateOf(userProfile.name) }
    var handle by remember(userProfile) { mutableStateOf(userProfile.handle) }
    var bio by remember(userProfile) { mutableStateOf(userProfile.bio) }
    var badge by remember(userProfile) { mutableStateOf(userProfile.badge) }
    var undoDelay by remember(userProfile) { mutableIntStateOf(userProfile.undoDelaySeconds) }
    var adFreeEnabled by remember(userProfile) { mutableStateOf(userProfile.adFreeEnabled) }
    var extendedPosts by remember(userProfile) { mutableStateOf(userProfile.extendedPostsEnabled) }
    var showEditHistory by remember(userProfile) { mutableStateOf(userProfile.showEditHistoryLinks) }
    var autoExpandThreads by remember(userProfile) { mutableStateOf(userProfile.autoExpandThreads) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.union(WindowInsets.ime)),
            color = OledBlack
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = "Close studio", tint = TextPrimary)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text("X Pro Studio", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Text(
                                text = "All Premium+ capabilities unlocked",
                                style = MaterialTheme.typography.bodySmall,
                                color = XBlue
                            )
                        }
                    }
                    Button(
                        onClick = {
                            onSave(
                                name,
                                handle,
                                bio,
                                badge,
                                undoDelay,
                                adFreeEnabled,
                                extendedPosts,
                                showEditHistory,
                                autoExpandThreads
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = XBlue, contentColor = TextPrimary),
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier.testTag("save_premium_studio_btn")
                    ) {
                        Text("Save")
                    }
                }
                HorizontalDivider(color = HairlineDivider, thickness = 0.8.dp)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Ad-Free Browsing Shield Card
                    Surface(
                        color = if (adFreeEnabled) EmeraldContainer else SurfaceCard,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            1.dp,
                            if (adFreeEnabled) EmeraldAdShield.copy(alpha = 0.45f) else HairlineDivider
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(
                                        imageVector = Icons.Filled.Shield,
                                        contentDescription = null,
                                        tint = if (adFreeEnabled) EmeraldAdShield else TextMuted,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Ad-Free Browsing Mode",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = if (adFreeEnabled) {
                                                "Blocking ${blockedPostAds.size} promoted posts & ${blockedTrendAds.size} promoted trends"
                                            } else {
                                                "Ad blocker paused — sponsored items visible in feeds"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (adFreeEnabled) EmeraldAdShield else TextMuted
                                        )
                                    }
                                }
                                Switch(
                                    checked = adFreeEnabled,
                                    onCheckedChange = { adFreeEnabled = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = OledBlack,
                                        checkedTrackColor = EmeraldAdShield
                                    ),
                                    modifier = Modifier.testTag("ad_free_switch")
                                )
                            }
                            if (adFreeEnabled && (blockedPostAds.isNotEmpty() || blockedTrendAds.isNotEmpty())) {
                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = EmeraldAdShield.copy(alpha = 0.25f), thickness = 0.8.dp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Intercepted in Feeds & Trends:",
                                    style = MonoTabularStyle.copy(fontSize = 11.sp),
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                blockedPostAds.forEach { ad ->
                                    Text(
                                        text = "• ${ad.sponsorLabel ?: ad.authorName} (Timeline Ad Hidden)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted
                                    )
                                }
                                blockedTrendAds.forEach { trend ->
                                    Text(
                                        text = "• ${trend.topic} (Promoted Trend Hidden)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted
                                    )
                                }
                            }
                        }
                    }

                    // Verification Badge Tier Picker
                    Surface(
                        color = SurfaceCard,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, HairlineDivider),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Verified, null, tint = XBlue, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Verification Badge Tier", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            VerificationBadge.entries.forEach { tier ->
                                val selected = badge == tier
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { badge = tier }
                                        .testTag("badge_option_${tier.name}"),
                                    color = if (selected) XBlueContainer else SurfaceElevated,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, if (selected) XBlue else BorderSubtle)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        VerificationBadgeIcon(badge = tier, size = 19.dp)
                                        if (tier == VerificationBadge.NONE) {
                                            Spacer(modifier = Modifier.width(19.dp))
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(tier.label, style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                                            Text(tier.subtitle, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Undo Send Delay Selector
                    Surface(
                        color = SurfaceCard,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, HairlineDivider),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Undo Send Countdown", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            Text(
                                text = "Hold posts locally before committing so you can cancel or edit typos.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(0, 3, 5, 10, 15).forEach { secs ->
                                    val selected = undoDelay == secs
                                    FilterChip(
                                        selected = selected,
                                        onClick = { undoDelay = secs },
                                        label = {
                                            Text(
                                                text = if (secs == 0) "Instant" else "${secs}s",
                                                style = MonoTabularStyle
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
                            }
                        }
                    }

                    // Power Toggles
                    Surface(
                        color = SurfaceCard,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, HairlineDivider),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Unlocked Power Features", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            PowerToggleRow(
                                title = "Extended 25,000-Char Posts",
                                subtitle = "Remove 280-character cap in standard composer",
                                checked = extendedPosts,
                                onCheckedChange = { extendedPosts = it }
                            )
                            PowerToggleRow(
                                title = "Show Edit History Links",
                                subtitle = "Display clickable diff timeline on edited posts",
                                checked = showEditHistory,
                                onCheckedChange = { showEditHistory = it }
                            )
                            PowerToggleRow(
                                title = "Auto-Expand Thread Previews",
                                subtitle = "Show thread controls prominently in feeds",
                                checked = autoExpandThreads,
                                onCheckedChange = { autoExpandThreads = it }
                            )
                        }
                    }

                    // Profile Identity Editor
                    Surface(
                        color = SurfaceCard,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, HairlineDivider),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Profile Identity", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("Display Name") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = XBlue,
                                    unfocusedBorderColor = BorderSubtle,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = handle,
                                onValueChange = { handle = it },
                                label = { Text("Handle") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = XBlue,
                                    unfocusedBorderColor = BorderSubtle,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = bio,
                                onValueChange = { bio = it },
                                label = { Text("Bio") },
                                minLines = 3,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = XBlue,
                                    unfocusedBorderColor = BorderSubtle,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PowerToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = OledBlack,
                checkedTrackColor = XBlue
            )
        )
    }
}

@Composable
fun MediaLightboxDialog(
    mediaResKey: String?,
    mediaUri: String?,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(OledBlack.copy(alpha = 0.96f))
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            val bundled = resolveBundledMedia(mediaResKey)
            if (bundled != null) {
                Image(
                    painter = painterResource(id = bundled),
                    contentDescription = "Full media view",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Fit
                )
            } else if (mediaUri != null) {
                AsyncImage(
                    model = mediaUri,
                    contentDescription = "Full photo view",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Fit
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .clip(CircleShape)
                    .background(SurfaceElevated)
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Close lightbox", tint = TextPrimary)
            }
        }
    }
}
