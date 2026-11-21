package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SuggestedUser
import com.example.ui.components.UserAvatar
import com.example.ui.components.VerificationBadgeIcon
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.EmeraldAdShield
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.GoldOrg
import com.example.ui.theme.HairlineDivider
import com.example.ui.theme.MonoTabularStyle
import com.example.ui.theme.OledBlack
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.XBlue
import com.example.viewmodel.XProUiState

@Composable
fun TrendsSidebar(
    uiState: XProUiState,
    onToggleAdFree: (Boolean) -> Unit,
    onOpenPremiumStudio: () -> Unit,
    onOpenMediaDownloader: () -> Unit = {},
    onOpenAuthorProfile: (String) -> Unit = {},
    onSearchTrend: (String) -> Unit,
    onToggleFollowUser: (SuggestedUser) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.End))
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Premium Status Hub
        Surface(
            color = if (uiState.userProfile.adFreeEnabled) EmeraldContainer else SurfaceCard,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(
                1.dp,
                if (uiState.userProfile.adFreeEnabled) EmeraldAdShield.copy(alpha = 0.45f) else HairlineDivider
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Shield,
                            null,
                            tint = if (uiState.userProfile.adFreeEnabled) EmeraldAdShield else TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ad-Free Shield", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    }
                    Switch(
                        checked = uiState.userProfile.adFreeEnabled,
                        onCheckedChange = onToggleAdFree,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = OledBlack,
                            checkedTrackColor = EmeraldAdShield
                        )
                    )
                }
                Text(
                    text = if (uiState.userProfile.adFreeEnabled) {
                        "Active · ${uiState.blockedAdsCount} ads hidden across feeds & trends"
                    } else {
                        "Paused · Promoted items visible"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (uiState.userProfile.adFreeEnabled) EmeraldAdShield else TextMuted
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Open XPro Studio →",
                        style = MaterialTheme.typography.labelLarge,
                        color = XBlue,
                        modifier = Modifier.clickable(onClick = onOpenPremiumStudio)
                    )
                    Text(
                        text = "Media Downloader",
                        style = MaterialTheme.typography.labelLarge,
                        color = EmeraldAdShield,
                        modifier = Modifier.clickable(onClick = onOpenMediaDownloader)
                    )
                }
            }
        }

        // What's Happening
        Surface(
            color = colorScheme.surfaceContainer,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(
                    text = "What's Happening",
                    style = MaterialTheme.typography.titleMedium,
                    color = colorScheme.onSurface,
                    modifier = Modifier.padding(14.dp)
                )
                HorizontalDivider(color = colorScheme.outlineVariant, thickness = 0.8.dp)
                uiState.visibleTrends.take(5).forEachIndexed { idx, trend ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSearchTrend(trend.topic.removePrefix("#")) }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = trend.category,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (trend.isPromoted) GoldOrg else colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = trend.topic,
                            style = MaterialTheme.typography.titleSmall,
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = trend.postCountLabel,
                            style = MonoTabularStyle.copy(fontSize = 11.sp),
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                    if (idx < uiState.visibleTrends.take(5).lastIndex) {
                        HorizontalDivider(color = colorScheme.outlineVariant, thickness = 0.8.dp)
                    }
                }
            }
        }

        // Who to Follow
        Surface(
            color = colorScheme.surfaceContainer,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(
                    text = "Who to Follow",
                    style = MaterialTheme.typography.titleMedium,
                    color = colorScheme.onSurface,
                    modifier = Modifier.padding(14.dp)
                )
                HorizontalDivider(color = colorScheme.outlineVariant, thickness = 0.8.dp)
                uiState.suggestedUsers.take(3).forEachIndexed { idx, user ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenAuthorProfile(user.id) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UserAvatar(
                            name = user.name,
                            isCurrentUser = false,
                            avatarColorHex = user.avatarColorHex,
                            size = 34.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = user.name,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                VerificationBadgeIcon(badge = user.badge, size = 14.dp)
                            }
                            Text(user.handle, style = MonoTabularStyle.copy(fontSize = 11.sp), color = colorScheme.onSurfaceVariant)
                        }
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
                    if (idx < 2) {
                        HorizontalDivider(color = colorScheme.outlineVariant, thickness = 0.8.dp)
                    }
                }
            }
        }
    }
}
