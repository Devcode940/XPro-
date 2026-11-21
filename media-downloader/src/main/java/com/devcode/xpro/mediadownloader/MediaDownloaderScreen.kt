package com.devcode.xpro.mediadownloader

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

private data class SampleMediaPreset(
    val label: String,
    val url: String
)

private val samplePresets = listOf(
    SampleMediaPreset("MP4 Sample", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"),
    SampleMediaPreset("WebM Sample", "https://dl8.webmfiles.org/big-buck-bunny_trailer.webm"),
    SampleMediaPreset("MP3 Audio", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3")
)

@Composable
fun MediaDownloaderScreen(
    onBack: (() -> Unit)? = null,
    initialUrl: String? = null,
    viewModel: MediaDownloaderViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var consumedInitialUrl by rememberSaveable { mutableStateOf<String?>(null) }

    if (onBack != null) {
        BackHandler(onBack = onBack)
    }

    LaunchedEffect(initialUrl) {
        if (!initialUrl.isNullOrBlank() && consumedInitialUrl != initialUrl && uiState.urlInput.isEmpty()) {
            consumedInitialUrl = initialUrl
            viewModel.selectPresetUrl(initialUrl)
        }
    }

    val colorScheme = MaterialTheme.colorScheme

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("media_downloader_screen"),
        color = colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxSize()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                                if (onBack != null) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier.testTag("media_downloader_back_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = colorScheme.onSurface
                                )
                            }
                        }
                        Column(modifier = Modifier.padding(start = 4.dp)) {
                            Text(
                                text = "Media Downloader",
                                style = MaterialTheme.typography.titleLarge,
                                color = colorScheme.onSurface
                            )
                            Text(
                                text = "Android DownloadManager · MP4 / WebM / Audio",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = colorScheme.primary
                            )
                        }
                    }

                    if (uiState.downloads.isNotEmpty()) {
                        TextButton(onClick = viewModel::clearHistory) {
                            Text("Clear", color = colorScheme.onSurfaceVariant)
                        }
                    }
                }

                HorizontalDivider(color = colorScheme.outline, thickness = 0.8.dp)

                AnimatedVisibility(visible = uiState.statusBanner != null) {
                    uiState.statusBanner?.let { banner ->
                        val bg = if (uiState.isErrorBanner) colorScheme.errorContainer else colorScheme.tertiaryContainer
                        val fg = if (uiState.isErrorBanner) colorScheme.error else colorScheme.tertiary
                        Surface(
                            color = bg,
                            border = BorderStroke(1.dp, fg.copy(alpha = 0.45f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                                .testTag("media_status_banner")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (uiState.isErrorBanner) Icons.Filled.ErrorOutline else Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = fg,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = banner,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = viewModel::dismissBanner,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Filled.Close, contentDescription = "Dismiss", tint = colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Surface(
                            color = colorScheme.surfaceContainer,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, colorScheme.outline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Direct Media URL",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Paste any direct HTTPS MP4, WebM, MKV, MOV, MP3, M4A, WAV, OGG, or FLAC URL to validate format and enqueue with DownloadManager.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = uiState.urlInput,
                                    onValueChange = viewModel::onUrlChanged,
                                    placeholder = {
                                        Text("https://example.com/video.mp4", color = colorScheme.onSurfaceVariant)
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Filled.Link, contentDescription = null, tint = colorScheme.primary)
                                    },
                                    trailingIcon = {
                                        if (uiState.urlInput.isNotEmpty()) {
                                            IconButton(onClick = { viewModel.onUrlChanged("") }) {
                                                Icon(Icons.Filled.Close, contentDescription = "Clear URL", tint = colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = colorScheme.surfaceContainerHigh,
                                        unfocusedContainerColor = colorScheme.surfaceContainerHigh,
                                        focusedBorderColor = colorScheme.primary,
                                        unfocusedBorderColor = colorScheme.outline,
                                        focusedTextColor = colorScheme.onSurface,
                                        unfocusedTextColor = colorScheme.onSurface
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("media_url_input")
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // Quick sample presets
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    samplePresets.forEach { preset ->
                                        AssistChip(
                                            onClick = { viewModel.selectPresetUrl(preset.url) },
                                            label = {
                                                Text(preset.label, style = MaterialTheme.typography.labelMedium)
                                            },
                                            colors = AssistChipDefaults.assistChipColors(
                                                containerColor = colorScheme.surfaceContainerHigh,
                                                labelColor = colorScheme.onSurface
                                            ),
                                            border = BorderStroke(1.dp, colorScheme.outline)
                                        )
                                    }
                                }

                                // Real-time Validation & Media Detection Preview
                                uiState.validationResult?.let { validation ->
                                    Spacer(modifier = Modifier.height(10.dp))
                                    if (validation.isValid && validation.mediaType != null) {
                                        val type = validation.mediaType
                                        Surface(
                                            color = colorScheme.primaryContainer,
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.45f)),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("media_detected_badge")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = if (type.category == MediaCategory.VIDEO) Icons.Filled.VideoFile else Icons.Filled.AudioFile,
                                                    contentDescription = type.displayName,
                                                    tint = colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "Detected: ${type.displayName} (${type.mimeType})",
                                                        style = MaterialTheme.typography.labelLarge,
                                                        color = colorScheme.primary
                                                    )
                                                    Text(
                                                        text = "Save as: Downloads/XPro/${validation.suggestedFileName}",
                                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                                        color = colorScheme.onSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    } else if (!validation.errorMessage.isNullOrBlank()) {
                                        Surface(
                                            color = colorScheme.errorContainer,
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, colorScheme.error.copy(alpha = 0.45f)),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("media_validation_error")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.ErrorOutline,
                                                    contentDescription = null,
                                                    tint = colorScheme.error,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = validation.errorMessage,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Button(
                                    onClick = { viewModel.startDownload() },
                                    enabled = uiState.urlInput.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colorScheme.primary,
                                        contentColor = colorScheme.onPrimary,
                                        disabledContainerColor = colorScheme.surfaceContainerHigh,
                                        disabledContentColor = colorScheme.onSurfaceVariant
                                    ),
                                    shape = RoundedCornerShape(999.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("media_download_btn")
                                ) {
                                    Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Download Media",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Download Queue (${uiState.downloads.size})",
                            style = MaterialTheme.typography.titleSmall,
                            color = colorScheme.onSurface
                        )
                    }

                    if (uiState.downloads.isEmpty()) {
                        item {
                            Surface(
                                color = colorScheme.surfaceContainer,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, colorScheme.outline),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Download,
                                        contentDescription = null,
                                        tint = colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(34.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "No active media downloads yet",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Enter an MP4, WebM, or audio link above to save to your device Downloads folder.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(uiState.downloads, key = { "${it.downloadId}_${it.createdAtMillis}" }) { item ->
                            DownloadQueueRow(
                                item = item,
                                onRefresh = { viewModel.refreshStatus(item.downloadId) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadQueueRow(
    item: MediaDownloadItem,
    onRefresh: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val statusColor = when (item.status) {
        DownloadStatus.SUCCEEDED -> colorScheme.tertiary
        DownloadStatus.FAILED -> colorScheme.error
        DownloadStatus.PAUSED -> colorScheme.secondary
        DownloadStatus.DOWNLOADING, DownloadStatus.QUEUED -> colorScheme.primary
    }
    Surface(
        color = colorScheme.surfaceContainer,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("download_item_${item.downloadId}")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (item.mediaType.category == MediaCategory.VIDEO) Icons.Filled.VideoFile else Icons.Filled.AudioFile,
                contentDescription = item.mediaType.displayName,
                tint = statusColor,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.fileName,
                    style = MaterialTheme.typography.titleSmall,
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${item.mediaType.displayName} · ID #${item.downloadId} · ${item.status.label}",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                    color = statusColor
                )
                if (item.status == DownloadStatus.QUEUED || item.status == DownloadStatus.DOWNLOADING) {
                    Spacer(modifier = Modifier.height(6.dp))
                    if (item.bytesTotal > 0L) {
                        val progress = (item.bytesDownloaded.toFloat() / item.bytesTotal.toFloat()).coerceIn(0f, 1f)
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
                if (!item.errorReason.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.errorReason,
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.error
                    )
                } else if (!item.localUri.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.localUri,
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            IconButton(onClick = onRefresh) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "Refresh download status",
                    tint = colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
