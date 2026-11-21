package com.example.core.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserProfile
import com.example.ui.components.UserAvatar
import com.example.ui.components.VerificationBadgeIcon
import com.example.ui.components.formatCompactCount
import com.example.ui.theme.MonoTabularStyle

@Composable
fun DrawerHeader(
    userProfile: UserProfile,
    isAuthenticated: Boolean,
    authEmail: String?,
    onViewProfile: () -> Unit,
    onSwitchAccount: () -> Unit,
    onAddAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colorScheme.surfaceContainerLow)
            .padding(horizontal = 18.dp, vertical = 16.dp)
            .testTag("drawer_header")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            UserAvatar(
                name = userProfile.name,
                isCurrentUser = true,
                avatarColorHex = 0xFF1D9BF0,
                size = 52.dp,
                modifier = Modifier
                    .clickable(onClick = onViewProfile)
                    .testTag("drawer_profile_avatar")
            )
            Surface(
                color = if (isAuthenticated) colorScheme.tertiaryContainer else colorScheme.primaryContainer,
                shape = RoundedCornerShape(999.dp),
                border = BorderStroke(
                    1.dp,
                    if (isAuthenticated) colorScheme.tertiary.copy(alpha = 0.45f) else colorScheme.primary.copy(alpha = 0.45f)
                )
            ) {
                Text(
                    text = if (isAuthenticated) "Cloud Synced" else "Guest Mode",
                    style = MonoTabularStyle.copy(fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold),
                    color = if (isAuthenticated) colorScheme.tertiary else colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = userProfile.name,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(modifier = Modifier.width(6.dp))
            VerificationBadgeIcon(badge = userProfile.badge, size = 16.dp)
        }

        Text(
            text = if (isAuthenticated && !authEmail.isNullOrBlank()) {
                "${userProfile.handle} · $authEmail"
            } else {
                userProfile.handle
            },
            style = MonoTabularStyle.copy(fontSize = 12.sp),
            color = colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.clickable(onClick = onViewProfile),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatCompactCount(userProfile.followingCount),
                    style = MonoTabularStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                    color = colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Following",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
            }
            Row(
                modifier = Modifier.clickable(onClick = onViewProfile),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatCompactCount(userProfile.followersCount),
                    style = MonoTabularStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                    color = colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Followers",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AssistChip(
                onClick = onViewProfile,
                leadingIcon = {
                    Icon(Icons.Filled.AccountCircle, null, modifier = Modifier.size(15.dp))
                },
                label = { Text("Profile", style = MaterialTheme.typography.labelSmall) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = colorScheme.surfaceContainer,
                    labelColor = colorScheme.onSurface,
                    leadingIconContentColor = colorScheme.primary
                ),
                border = BorderStroke(1.dp, colorScheme.outline),
                modifier = Modifier.testTag("drawer_action_view_profile")
            )
            AssistChip(
                onClick = onSwitchAccount,
                leadingIcon = {
                    Icon(Icons.Filled.SwapHoriz, null, modifier = Modifier.size(15.dp))
                },
                label = { Text("Switch", style = MaterialTheme.typography.labelSmall) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = colorScheme.surfaceContainer,
                    labelColor = colorScheme.onSurface,
                    leadingIconContentColor = colorScheme.secondary
                ),
                border = BorderStroke(1.dp, colorScheme.outline),
                modifier = Modifier.testTag("drawer_action_switch_account")
            )
            AssistChip(
                onClick = onAddAccount,
                leadingIcon = {
                    Icon(Icons.Filled.PersonAdd, null, modifier = Modifier.size(15.dp))
                },
                label = { Text("Add", style = MaterialTheme.typography.labelSmall) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = colorScheme.surfaceContainer,
                    labelColor = colorScheme.onSurface,
                    leadingIconContentColor = colorScheme.tertiary
                ),
                border = BorderStroke(1.dp, colorScheme.outline),
                modifier = Modifier.testTag("drawer_action_add_account")
            )
        }
    }
}
