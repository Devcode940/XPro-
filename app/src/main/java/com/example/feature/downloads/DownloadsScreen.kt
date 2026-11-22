package com.example.feature.downloads

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devcode.xpro.mediadownloader.DownloadStatus
import com.devcode.xpro.mediadownloader.MediaCategory
import com.devcode.xpro.mediadownloader.MediaDownloadItem
import com.example.core.designsystem.AppTopBar
import com.example.core.designsystem.rememberIsKeyboardOpen
import com.example.core.scroll.AutoHideHeaderLayout
import com.example.data.model.UserProfile
import com.example.ui.theme.MonoTabularStyle
import java.io.File

@Composable
fun DownloadsScreen(
    downloadsViewModel: DownloadsViewModel,
    userProfile: UserProfile,
    showUrlComposerExpanded: Boolean = true,
    initialUrl: String? = null,
    onOpenDrawer: () -> Unit,
    onBack: () -> Unit,
    onOpenInPlayer: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val state by downloadsViewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val isKeyboardOpen by rememberIsKeyboardOpen()
    var urlFocused by remember { mutableStateOf(false) }

    val hasActiveDownloads = remember(state.allDownloads) {
        state.allDownloads.any { it.status == DownloadStatus.QUEUED || it.status == DownloadStatus.DOWNLOADING }
    }

    var hasNotifPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= 33) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }
    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotifPermission = granted
    }

    LaunchedEffect(initialUrl) {
        if (!initialUrl.isNullOrBlank()) {
            downloadsViewModel.onUrlChanged(initialUrl)
        }
    }

    AutoHideHeaderLayout(
        modifier = modifier.testTag("downloads_screen"),
        listState = listState,
        lockBarsVisible = isKeyboardOpen || urlFocused || hasActiveDownloads,
        header = {
            Column(modifier = Modifier.fillMaxWidth()) {
                AppTopBar(
                    title = if (showUrlComposerExpanded) "Download Manager" else "Downloads",
                    subtitle = "${state.allDownloads.size} items · Wi-Fi Only: ${if (state.settings.wifiOnlyDownloads) "On" else "Off"}",
                    userName = userProfile.name,
                    showBack = true,
                    onBack = onBack,
                    onOpenDrawer = onOpenDrawer,
                    actions = {
                        IconButton(onClick = downloadsViewModel::refreshAll) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh queue")
                        }
                        if (state.allDownloads.isNotEmpty()) {
                            TextButton(onClick = downloadsViewModel::clearAll) {
                                Text("Clear")
                            }
                        }
                    }
                )
                // Tabs: All, Downloading, Completed, Failed
                TabRow(
                    selectedTabIndex = state.selectedTab.ordinal,
                    containerColor = colorScheme.background,
                    contentColor = colorScheme.primary
                ) {
                    DownloadsFilterTab.entries.forEach { tab ->
                        Tab(
                            selected = state.selectedTab == tab,
                            onClick = { downloadsViewModel.selectTab(tab) },
                            text = { Text(tab.label, maxLines = 1) },
                            modifier = Modifier.testTag("downloads_tab_${tab.name}")
                        )
                    }
                }
                HorizontalDivider(color = colorScheme.outline, thickness = 0.8.dp)
            }
        }
    ) { scrollModifier, contentPadding ->
        LazyColumn(
            state = listState,
            modifier = scrollModifier,
            contentPadding = PaddingValues(
                start = contentPadding.calculateLeftPadding(androidx.compose.ui.platform.LocalLayoutDirection.current),
                end = contentPadding.calculateRightPadding(androidx.compose.ui.platform.LocalLayoutDirection.current),
                top = contentPadding.calculateTopPadding() + 8.dp,
                bottom = contentPadding.calculateBottomPadding()
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!hasNotifPermission && Build.VERSION.SDK_INT >= 33) {
                item(key = "notif_permission_banner") {
                    Surface(
                        color = colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Filled.NotificationsActive, null, tint = colorScheme.secondary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Enable notifications for background download alerts",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurface
                                )
                            }
                            TextButton(onClick = { notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                                Text("Allow")
                            }
                        }
                    }
                }
            }

            state.statusBanner?.let { banner ->
                item(key = "status_banner") {
                    Surface(
                        color = if (state.isErrorBanner) colorScheme.errorContainer else colorScheme.primaryContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = banner,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (state.isErrorBanner) colorScheme.error else colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = downloadsViewModel::dismissBanner, modifier = Modifier.size(26.dp)) {
                                Icon(Icons.Filled.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            item(key = "direct_download_composer") {
                Surface(
                    color = colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, colorScheme.outline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        OutlinedTextField(
                            value = state.urlInput,
                            onValueChange = downloadsViewModel::onUrlChanged,
                            placeholder = { Text("https://example.com/video.mp4 or .mp3") },
                            leadingIcon = { Icon(Icons.Filled.Link, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { urlFocused = it.isFocused }
                                .testTag("downloads_url_input")
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AssistChip(
                                onClick = { downloadsViewModel.onUrlChanged("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4") },
                                label = { Text("MP4 Video") }
                            )
                            AssistChip(
                                onClick = { downloadsViewModel.onUrlChanged("https://dl8.webmfiles.org/big-buck-bunny_trailer.webm") },
                                label = { Text("WebM Video") }
                            )
                            AssistChip(
                                onClick = { downloadsViewModel.onUrlChanged("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3") },
                                label = { Text("MP3 Audio") }
                            )
                        }
                        state.validationResult?.let { validation ->
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (validation.isValid && validation.mediaType != null) {
                                    "Ready: ${validation.mediaType!!.displayName} -> ${validation.suggestedFileName}"
                                } else {
                                    validation.errorMessage ?: "Invalid URL"
                                },
                                style = MonoTabularStyle.copy(fontSize = 11.sp),
                                color = if (validation.isValid) colorScheme.tertiary else colorScheme.error
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = downloadsViewModel::startDownload,
                            enabled = state.urlInput.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("downloads_start_btn")
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Enqueue Download")
                        }
                    }
                }
            }

            if (state.visibleDownloads.isEmpty()) {
                item(key = "downloads_empty_state") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp)
                            .testTag("downloads_empty_state"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.Download, contentDescription = null, tint = colorScheme.onSurfaceVariant, modifier = Modifier.size(44.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No ${state.selectedTab.label.lowercase()} downloads",
                                style = MaterialTheme.typography.titleMedium,
                                color = colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Paste a direct HTTPS media link above to manage background downloads with Android DownloadManager.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(state.visibleDownloads, key = { "${it.downloadId}_${it.createdAtMillis}" }) { item ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        DownloadManagementCard(
                            item = item,
                            onPause = { downloadsViewModel.pauseDownload(item.downloadId) },
                            onResume = { downloadsViewModel.resumeDownload(item.downloadId) },
                            onCancel = { downloadsViewModel.cancelDownload(item.downloadId) },
                            onRetry = { downloadsViewModel.retryDownload(item.downloadId) },
                            onOpen = {
                                val playable = item.localUri ?: item.url
                                onOpenInPlayer(playable, item.mediaType.category == MediaCategory.VIDEO)
                            },
                            onShare = { shareDownloadedMedia(context, item) },
                            onDelete = { downloadsViewModel.deleteDownload(item.downloadId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadManagementCard(
    item: MediaDownloadItem,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val statusColor = when (item.status) {
        DownloadStatus.SUCCEEDED -> colorScheme.tertiary
        DownloadStatus.FAILED -> colorScheme.error
        DownloadStatus.PAUSED -> colorScheme.secondary
        DownloadStatus.QUEUED, DownloadStatus.DOWNLOADING -> colorScheme.primary
        DownloadStatus.CANCELLED -> colorScheme.onSurfaceVariant
    }

    val progress = if (item.bytesTotal > 0L) {
        (item.bytesDownloaded.toFloat() / item.bytesTotal.toFloat()).coerceIn(0f, 1f)
    } else if (item.status == DownloadStatus.SUCCEEDED) 1f else 0f

    val sizeLabel = if (item.bytesTotal > 0L) {
        "${formatDownloadBytes(item.bytesDownloaded)} / ${formatDownloadBytes(item.bytesTotal)}"
    } else if (item.bytesDownloaded > 0L) {
        formatDownloadBytes(item.bytesDownloaded)
    } else {
        "Size pending"
    }

    Surface(
        color = colorScheme.surfaceContainer,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("download_card_${item.downloadId}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Media Thumbnail Preview Box
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(statusColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (item.mediaType.category == MediaCategory.VIDEO) Icons.Filled.VideoFile else Icons.Filled.AudioFile,
                        contentDescription = item.mediaType.displayName,
                        tint = statusColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.fileName,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${item.mediaType.displayName} · ${item.resolution} · $sizeLabel",
                        style = MonoTabularStyle.copy(fontSize = 11.sp),
                        color = colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Status: ${item.status.label}",
                        style = MonoTabularStyle.copy(fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold),
                        color = statusColor
                    )
                }
            }

            if (item.status == DownloadStatus.QUEUED || item.status == DownloadStatus.DOWNLOADING || item.status == DownloadStatus.PAUSED) {
                Spacer(modifier = Modifier.height(10.dp))
                if (item.status == DownloadStatus.PAUSED) {
                    Text(
                        text = "DownloadManager can't pause; resume re-downloads from the start.",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
                if (item.bytesTotal > 0L) {
                    LinearProgressIndicator(
                        progress = { progress },
                        color = statusColor,
                        trackColor = colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    LinearProgressIndicator(
                        color = statusColor,
                        trackColor = colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item.errorReason?.let { err ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(err, style = MaterialTheme.typography.bodySmall, color = colorScheme.error)
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (item.status == DownloadStatus.QUEUED || item.status == DownloadStatus.DOWNLOADING) {
                    OutlinedButton(onClick = onPause) {
                        Icon(Icons.Filled.Pause, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pause")
                    }
                    OutlinedButton(onClick = onCancel) {
                        Icon(Icons.Filled.Cancel, null, modifier = Modifier.size(16.dp), tint = colorScheme.error)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cancel", color = colorScheme.error)
                    }
                }
                if (item.status == DownloadStatus.PAUSED) {
                    Button(onClick = onResume) {
                        Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Resume from start")
                    }
                    OutlinedButton(onClick = onCancel) {
                        Icon(Icons.Filled.Cancel, null, modifier = Modifier.size(16.dp), tint = colorScheme.error)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cancel", color = colorScheme.error)
                    }
                }
                if (item.status == DownloadStatus.FAILED) {
                    Button(onClick = onRetry) {
                        Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Retry")
                    }
                }
                if (item.status == DownloadStatus.SUCCEEDED) {
                    Button(onClick = onOpen) {
                        Icon(Icons.Filled.OpenInNew, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Open")
                    }
                }
                OutlinedButton(onClick = onShare) {
                    Icon(Icons.Filled.Share, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Share")
                }
                OutlinedButton(onClick = onDelete) {
                    Icon(Icons.Filled.DeleteOutline, null, modifier = Modifier.size(16.dp), tint = colorScheme.error)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete", color = colorScheme.error)
                }
            }
        }
    }
}

private fun shareDownloadedMedia(context: Context, item: MediaDownloadItem) {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        val localUriStr = item.localUri
        if (localUriStr != null && localUriStr.startsWith("file://")) {
            val file = Uri.parse(localUriStr).path?.let { File(it) }
            if (file != null && file.exists()) {
                val contentUri = runCatching {
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                }.getOrNull()
                if (contentUri != null) {
                    type = item.mediaType.mimeType
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } else {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "${item.fileName}\n${item.url}")
                }
            } else {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "${item.fileName}\n${item.url}")
            }
        } else {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "${item.fileName}\n${item.url}")
        }
    }
    runCatching {
        context.startActivity(Intent.createChooser(sendIntent, "Share ${item.fileName}"))
    }
}

private fun formatDownloadBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return if (mb >= 1.0) String.format("%.1f MB", mb) else String.format("%.0f KB", kb)
}
