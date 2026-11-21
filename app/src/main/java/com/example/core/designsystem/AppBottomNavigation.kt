package com.example.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.navigation.XtraRoutes
import com.example.data.model.UserProfile
import com.example.ui.components.UserAvatar
import com.example.ui.components.XProLogo

data class PrimaryNavItem(
    val route: String,
    val label: String,
    val activeIcon: ImageVector,
    val inactiveIcon: ImageVector,
    val testTag: String,
    val badgeCount: Int = 0
)

@Composable
fun rememberPrimaryNavItems(
    unreadNotificationsCount: Int,
    unreadMessagesCount: Int
): List<PrimaryNavItem> = listOf(
    PrimaryNavItem(
        route = XtraRoutes.HOME,
        label = "Home",
        activeIcon = Icons.Filled.Home,
        inactiveIcon = Icons.Outlined.Home,
        testTag = "nav_HOME"
    ),
    PrimaryNavItem(
        route = XtraRoutes.EXPLORE,
        label = "Explore",
        activeIcon = Icons.Filled.Explore,
        inactiveIcon = Icons.Outlined.Explore,
        testTag = "nav_EXPLORE"
    ),
    PrimaryNavItem(
        route = XtraRoutes.NOTIFICATIONS,
        label = "Notifications",
        activeIcon = Icons.Filled.Notifications,
        inactiveIcon = Icons.Outlined.Notifications,
        testTag = "nav_NOTIFICATIONS",
        badgeCount = unreadNotificationsCount
    ),
    PrimaryNavItem(
        route = XtraRoutes.MESSAGES,
        label = "Messages",
        activeIcon = Icons.Filled.Mail,
        inactiveIcon = Icons.Outlined.Mail,
        testTag = "nav_MESSAGES",
        badgeCount = unreadMessagesCount
    )
)

@Composable
fun AppBottomNavigation(
    currentRoute: String?,
    unreadNotificationsCount: Int,
    unreadMessagesCount: Int,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val items = rememberPrimaryNavItems(unreadNotificationsCount, unreadMessagesCount)

    Column(modifier = modifier.background(colorScheme.background)) {
        HorizontalDivider(color = colorScheme.outline, thickness = 0.8.dp)
        NavigationBar(
            containerColor = colorScheme.background.copy(alpha = 0.96f),
            contentColor = colorScheme.onSurface,
            windowInsets = WindowInsets.navigationBars.union(
                WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
            )
        ) {
            items.forEach { item ->
                val selected = currentRoute == item.route ||
                    (item.route == XtraRoutes.MESSAGES && currentRoute?.startsWith("chat/") == true)
                NavigationBarItem(
                    selected = selected,
                    onClick = { onNavigate(item.route) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (item.badgeCount > 0) {
                                    Badge(
                                        containerColor = colorScheme.primary,
                                        contentColor = colorScheme.onPrimary
                                    ) {
                                        Text(item.badgeCount.coerceAtMost(99).toString())
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (selected) item.activeIcon else item.inactiveIcon,
                                contentDescription = item.label
                            )
                        }
                    },
                    label = {
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = colorScheme.primary,
                        selectedTextColor = colorScheme.primary,
                        unselectedIconColor = colorScheme.onSurfaceVariant,
                        unselectedTextColor = colorScheme.onSurfaceVariant,
                        indicatorColor = colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag(item.testTag)
                )
            }
        }
    }
}

@Composable
fun AppNavigationRail(
    currentRoute: String?,
    userProfile: UserProfile,
    unreadNotificationsCount: Int,
    unreadMessagesCount: Int,
    onNavigate: (String) -> Unit,
    onOpenDrawer: () -> Unit,
    onCompose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val items = rememberPrimaryNavItems(unreadNotificationsCount, unreadMessagesCount)

    NavigationRail(
        containerColor = colorScheme.background,
        contentColor = colorScheme.onSurface,
        windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.Start),
        modifier = modifier.testTag("app_navigation_rail"),
        header = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
            ) {
                IconButton(onClick = onOpenDrawer) {
                    Icon(Icons.Filled.Menu, contentDescription = "Open XTRA menu", tint = colorScheme.onSurface)
                }
                Spacer(modifier = Modifier.height(6.dp))
                XProLogo(size = 26.dp, tint = colorScheme.onSurface)
                Spacer(modifier = Modifier.height(12.dp))
                FloatingActionButton(
                    onClick = onCompose,
                    containerColor = colorScheme.primary,
                    contentColor = colorScheme.onPrimary,
                    shape = CircleShape,
                    modifier = Modifier.size(46.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Compose post")
                }
            }
        }
    ) {
        items.forEach { item ->
            val selected = currentRoute == item.route
            NavigationRailItem(
                selected = selected,
                onClick = { onNavigate(item.route) },
                icon = {
                    BadgedBox(
                        badge = {
                            if (item.badgeCount > 0) {
                                Badge { Text(item.badgeCount.coerceAtMost(99).toString()) }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (selected) item.activeIcon else item.inactiveIcon,
                            contentDescription = item.label
                        )
                    }
                },
                label = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = colorScheme.primary,
                    selectedTextColor = colorScheme.primary,
                    unselectedIconColor = colorScheme.onSurfaceVariant,
                    unselectedTextColor = colorScheme.onSurfaceVariant,
                    indicatorColor = colorScheme.primaryContainer
                )
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .padding(bottom = 16.dp)
                .clip(CircleShape)
                .clickable { onNavigate(XtraRoutes.profile("current_user")) }
        ) {
            UserAvatar(
                name = userProfile.name,
                isCurrentUser = true,
                avatarColorHex = 0xFF1D9BF0,
                size = 38.dp
            )
        }
    }
}
