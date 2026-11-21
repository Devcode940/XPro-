package com.example.feature.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.core.designsystem.AppTopBar
import com.example.core.navigation.XtraRoutes
import com.example.core.scroll.AutoHideHeaderLayout
import com.example.data.model.UserProfile
import com.example.ui.theme.MonoTabularStyle

@Composable
fun SettingsScreen(
    state: SettingsDashboardState,
    userProfile: UserProfile,
    isAuthenticated: Boolean,
    authEmail: String?,
    initialSection: String,
    viewModel: SettingsViewModel,
    onOpenDrawer: () -> Unit,
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    onSignInClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onOpenDownloads: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val settings = state.settings

    var blockInput by rememberSaveable { mutableStateOf("") }
    var muteInput by rememberSaveable { mutableStateOf("") }

    val cameraGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED
    val notificationsGranted = if (Build.VERSION.SDK_INT >= 33) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else true

    LaunchedEffect(initialSection) {
        val index = when (initialSection) {
            XtraRoutes.SETTINGS_ACCOUNT -> 0
            XtraRoutes.SETTINGS_APPEARANCE -> 1
            XtraRoutes.SETTINGS_PRIVACY -> 2
            XtraRoutes.SETTINGS_NOTIFICATIONS -> 3
            XtraRoutes.SETTINGS_DATA_USAGE -> 4
            XtraRoutes.SETTINGS_STORAGE -> 5
            XtraRoutes.SETTINGS_ACCESSIBILITY -> 6
            XtraRoutes.SETTINGS_LANGUAGE -> 7
            else -> 0
        }
        listState.animateScrollToItem(index)
    }

    AutoHideHeaderLayout(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        listState = listState,
        lockBarsVisible = true,
        header = {
            Column(modifier = Modifier.fillMaxWidth()) {
                AppTopBar(
                    title = "Settings & Privacy",
                    subtitle = "Account · Appearance · Storage · Accessibility",
                    userName = userProfile.name,
                    showBack = true,
                    onBack = onBack,
                    onOpenDrawer = onOpenDrawer
                )

                state.statusMessage?.let { msg ->
                    Surface(
                        color = colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(msg, style = MaterialTheme.typography.bodySmall, color = colorScheme.primary)
                            IconButton(onClick = viewModel::dismissStatus, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Filled.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    ) { scrollModifier, contentPadding ->
        val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
        LazyColumn(
            state = listState,
            modifier = scrollModifier,
            contentPadding = PaddingValues(
                start = contentPadding.calculateLeftPadding(androidx.compose.ui.unit.LayoutDirection.Ltr) + 16.dp,
                end = contentPadding.calculateRightPadding(androidx.compose.ui.unit.LayoutDirection.Ltr) + 16.dp,
                top = contentPadding.calculateTopPadding() + 16.dp,
                bottom = maxOf(contentPadding.calculateBottomPadding(), imeBottom) + 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. ACCOUNT
            item {
                SettingsSectionCard(
                    title = "Account",
                    icon = Icons.Filled.ManageAccounts,
                    highlighted = initialSection == XtraRoutes.SETTINGS_ACCOUNT,
                    testTag = "settings_section_account"
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(userProfile.name, style = MaterialTheme.typography.titleSmall, color = colorScheme.onSurface)
                            Text(
                                text = "${userProfile.handle} · ${if (isAuthenticated) authEmail ?: "Google Connected" else "Guest Local Profile"}",
                                style = MonoTabularStyle.copy(fontSize = 11.sp),
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(onClick = onEditProfile) {
                            Text("Edit Profile")
                        }
                    }

                    HorizontalDivider(color = colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        text = "Authentication & Connected Accounts",
                        style = MaterialTheme.typography.labelLarge,
                        color = colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isAuthenticated) {
                            "Signed in with Google (${authEmail ?: "Verified Firebase User"}). Bookmarks sync automatically with Firestore."
                        } else {
                            "Currently browsing in Guest Mode. Sign in with Google to enable cloud bookmark sync across devices."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onSignInClick,
                            colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary)
                        ) {
                            Icon(Icons.Filled.CloudDone, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isAuthenticated) "Switch Google Account" else "Sign In with Google")
                        }
                        OutlinedButton(onClick = onSignOutClick) {
                            Icon(Icons.AutoMirrored.Filled.Logout, null, modifier = Modifier.size(16.dp), tint = colorScheme.error)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sign Out", color = colorScheme.error)
                        }
                    }
                }
            }

            // 2. APPEARANCE
            item {
                SettingsSectionCard(
                    title = "Appearance & Themes",
                    icon = Icons.Filled.Palette,
                    highlighted = initialSection == XtraRoutes.SETTINGS_APPEARANCE,
                    testTag = "settings_section_appearance"
                ) {
                    Text("Theme Mode", style = MaterialTheme.typography.labelMedium, color = colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = settings.themeMode == mode,
                                onClick = { viewModel.setThemeMode(mode) },
                                label = { Text(mode.label) },
                                modifier = Modifier.testTag("theme_mode_${mode.name}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Accent Color", style = MaterialTheme.typography.labelMedium, color = colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AccentColorOption.entries.forEach { accent ->
                            FilterChip(
                                selected = settings.accentColor == accent,
                                onClick = { viewModel.setAccentColor(accent) },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(Color(accent.hex))
                                    )
                                },
                                label = { Text(accent.label) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Font Size", style = MaterialTheme.typography.labelMedium, color = colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FontSizePreset.entries.forEach { preset ->
                            FilterChip(
                                selected = settings.fontSize == preset,
                                onClick = { viewModel.setFontSize(preset) },
                                label = { Text(preset.label) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Display Density", style = MaterialTheme.typography.labelMedium, color = colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DisplayDensity.entries.forEach { density ->
                            FilterChip(
                                selected = settings.displayDensity == density,
                                onClick = { viewModel.setDisplayDensity(density) },
                                label = { Text(density.label) }
                            )
                        }
                    }
                }
            }

            // 3. PRIVACY AND SECURITY
            item {
                SettingsSectionCard(
                    title = "Privacy & Security",
                    icon = Icons.Filled.Security,
                    highlighted = initialSection == XtraRoutes.SETTINGS_PRIVACY,
                    testTag = "settings_section_privacy"
                ) {
                    SettingsSwitchItem(
                        title = "Private Account",
                        subtitle = "Require approval for new followers and hide activity status",
                        checked = settings.privateAccount,
                        onCheckedChange = viewModel::setPrivateAccount,
                        testTag = "switch_private_account"
                    )

                    HorizontalDivider(color = colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 8.dp))
                    Text("Blocked Accounts (${settings.blockedAccounts.size})", style = MaterialTheme.typography.labelLarge, color = colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = blockInput,
                            onValueChange = { blockInput = it },
                            placeholder = { Text("@handle to block") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (blockInput.isNotBlank()) {
                                    viewModel.addBlockedAccount(blockInput)
                                    blockInput = ""
                                }
                            }
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Block account", tint = colorScheme.primary)
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        settings.blockedAccounts.forEach { handle ->
                            FilterChip(
                                selected = false,
                                onClick = { viewModel.removeBlockedAccount(handle) },
                                label = { Text("$handle ✕", style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Muted Accounts (${settings.mutedAccounts.size})", style = MaterialTheme.typography.labelLarge, color = colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = muteInput,
                            onValueChange = { muteInput = it },
                            placeholder = { Text("@handle to mute") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (muteInput.isNotBlank()) {
                                    viewModel.addMutedAccount(muteInput)
                                    muteInput = ""
                                }
                            }
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Mute account", tint = colorScheme.primary)
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        settings.mutedAccounts.forEach { handle ->
                            FilterChip(
                                selected = false,
                                onClick = { viewModel.removeMutedAccount(handle) },
                                label = { Text("$handle ✕", style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    HorizontalDivider(color = colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Permission Management", style = MaterialTheme.typography.labelLarge, color = colorScheme.onSurface)
                            Text(
                                text = "Camera (QR): ${if (cameraGranted) "Granted" else "Not granted"} · Notifications: ${if (notificationsGranted) "Granted" else "Not granted"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(
                            onClick = {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                runCatching { context.startActivity(intent) }
                            }
                        ) {
                            Text("System Apps")
                        }
                    }

                    HorizontalDivider(color = colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 8.dp))
                    Text("Active Sessions (${state.sessions.size})", style = MaterialTheme.typography.labelLarge, color = colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(6.dp))
                    state.sessions.forEach { session ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(session.deviceName, style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurface)
                                Text(
                                    text = "${session.location} · ${session.lastActive}",
                                    style = MonoTabularStyle.copy(fontSize = 11.sp),
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                            if (session.isCurrent) {
                                Text("This device", style = MaterialTheme.typography.labelSmall, color = colorScheme.tertiary)
                            } else {
                                TextButton(onClick = { viewModel.revokeSession(session.id) }) {
                                    Text("Revoke", color = colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }

            // 4. NOTIFICATIONS
            item {
                SettingsSectionCard(
                    title = "Notifications",
                    icon = Icons.Filled.Notifications,
                    highlighted = initialSection == XtraRoutes.SETTINGS_NOTIFICATIONS,
                    testTag = "settings_section_notifications"
                ) {
                    SettingsSwitchItem("Push Notifications", "Master toggle for device alerts", settings.pushNotifications, viewModel::setPushNotifications)
                    SettingsSwitchItem("Likes", "Notify when creators like your posts", settings.notifyLikes, viewModel::setNotifyLikes)
                    SettingsSwitchItem("Comments & Replies", "Notify on thread replies", settings.notifyComments, viewModel::setNotifyComments)
                    SettingsSwitchItem("Direct Messages", "Notify on new encrypted DMs", settings.notifyMessages, viewModel::setNotifyMessages)
                    SettingsSwitchItem("New Followers", "Notify when accounts follow you", settings.notifyFollowers, viewModel::setNotifyFollowers)
                    SettingsSwitchItem("Download Notifications", "Show DownloadManager progress & completion", settings.notifyDownloads, viewModel::setNotifyDownloads)
                }
            }

            // 5. MEDIA, DOWNLOADS & DATA USAGE
            item {
                SettingsSectionCard(
                    title = "Media, Downloads & Data Usage",
                    icon = Icons.Filled.Download,
                    highlighted = initialSection == XtraRoutes.SETTINGS_DATA_USAGE,
                    testTag = "settings_section_media"
                ) {
                    Text(
                        text = "Default Download Location: ${settings.downloadLocation}",
                        style = MonoTabularStyle.copy(fontSize = 12.sp),
                        color = colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Preferred Video Quality", style = MaterialTheme.typography.labelMedium, color = colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("4K", "1080p HD", "720p", "480p").forEach { quality ->
                            FilterChip(
                                selected = settings.preferredVideoQuality == quality,
                                onClick = { viewModel.setPreferredVideoQuality(quality) },
                                label = { Text(quality) }
                            )
                        }
                    }

                    Text("Preferred Audio Quality", style = MaterialTheme.typography.labelMedium, color = colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("320 kbps", "256 kbps", "128 kbps").forEach { quality ->
                            FilterChip(
                                selected = settings.preferredAudioQuality == quality,
                                onClick = { viewModel.setPreferredAudioQuality(quality) },
                                label = { Text(quality) }
                            )
                        }
                    }

                    SettingsSwitchItem("Download Over Wi-Fi Only", "Prevent cellular data usage for media downloads", settings.wifiOnlyDownloads, viewModel::setWifiOnlyDownloads, testTag = "switch_wifi_only")
                    SettingsSwitchItem("Background Downloads", "Allow Android DownloadManager in background", settings.backgroundDownloads, viewModel::setBackgroundDownloads)
                    SettingsSwitchItem("Automatic Media Detection", "Auto-detect MP4, WebM, and audio URLs", settings.autoMediaDetection, viewModel::setAutoMediaDetection)
                    SettingsSwitchItem("Auto-Play Video", "Automatically start playback in player & feed", settings.autoPlayVideos, viewModel::setAutoPlayVideos)
                    SettingsSwitchItem("Subtitles & Captions", "Show subtitles when available in Video Player", settings.subtitlesEnabled, viewModel::setSubtitlesEnabled)
                    SettingsSwitchItem("Data Saver Mode", "Compress previews and defer heavy assets on metered connections", settings.dataSaverEnabled, viewModel::setDataSaverEnabled)
                }
            }

            // 6. STORAGE
            item {
                SettingsSectionCard(
                    title = "Storage Management",
                    icon = Icons.Filled.Storage,
                    highlighted = initialSection == XtraRoutes.SETTINGS_STORAGE,
                    testTag = "settings_section_storage"
                ) {
                    StorageStatRow("App Cache Usage", formatByteCount(state.storage.cacheBytes))
                    StorageStatRow("Downloaded Media Files", formatByteCount(state.storage.downloadedBytes))
                    StorageStatRow("Room SQLite Database", "${formatByteCount(state.storage.databaseBytes)} (${state.storage.cachedPostsCount} offline posts)")

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = viewModel::clearCache,
                            colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary),
                            modifier = Modifier.testTag("clear_cache_btn")
                        ) {
                            Icon(Icons.Filled.DeleteSweep, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Clear Cache")
                        }
                        OutlinedButton(onClick = onOpenDownloads) {
                            Text("Manage Storage")
                        }
                    }
                }
            }

            // 7. ACCESSIBILITY
            item {
                SettingsSectionCard(
                    title = "Accessibility",
                    icon = Icons.Filled.AccessibilityNew,
                    highlighted = initialSection == XtraRoutes.SETTINGS_ACCESSIBILITY,
                    testTag = "settings_section_accessibility"
                ) {
                    SettingsSwitchItem("Larger Text", "Boost typography scale by an extra 10%", settings.largerText, viewModel::setLargerText)
                    SettingsSwitchItem("Reduced Motion", "Disable expandable transitions & drawer animations", settings.reducedMotion, viewModel::setReducedMotion)
                    SettingsSwitchItem("Screen Reader Hints", "Provide verbose TalkBack labels across icons", settings.screenReaderSupport, viewModel::setScreenReaderSupport)
                    SettingsSwitchItem("Improved Contrast", "Maximize foreground/outline contrast on dark & light modes", settings.improvedContrast, viewModel::setImprovedContrast)
                }
            }

            // 8. LANGUAGE
            item {
                SettingsSectionCard(
                    title = "Language",
                    icon = Icons.Filled.Language,
                    highlighted = initialSection == XtraRoutes.SETTINGS_LANGUAGE,
                    testTag = "settings_section_language"
                ) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("English", "Español", "Français", "Deutsch", "日本語").forEach { lang ->
                            FilterChip(
                                selected = settings.appLanguage == lang,
                                onClick = { viewModel.setAppLanguage(lang) },
                                label = { Text(lang) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    icon: ImageVector,
    highlighted: Boolean,
    testTag: String,
    content: @Composable ColumnScope.() -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        color = colorScheme.surfaceContainer,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = if (highlighted) 1.5.dp else 1.dp,
            color = if (highlighted) colorScheme.primary else colorScheme.outline
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun SettingsSwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = if (testTag != null) Modifier.testTag(testTag) else Modifier
        )
    }
}

@Composable
private fun StorageStatRow(label: String, value: String) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
        Text(value, style = MonoTabularStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold), color = colorScheme.onSurface)
    }
}

private fun formatByteCount(bytes: Long): String {
    if (bytes <= 0L) return "0 KB"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return if (mb >= 1.0) {
        String.format("%.2f MB", mb)
    } else {
        String.format("%.1f KB", kb.coerceAtLeast(0.1))
    }
}
