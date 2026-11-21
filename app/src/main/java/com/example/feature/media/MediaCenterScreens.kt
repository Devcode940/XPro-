package com.example.feature.media

import android.content.res.Configuration
import android.media.MediaPlayer
import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FeaturedPlayList
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.designsystem.AppTopBar
import com.example.core.designsystem.ImmersiveVideoSystemBarsEffect
import com.example.core.scroll.AutoHideHeaderLayout
import com.example.data.model.LibraryMediaKind
import com.example.data.model.MediaLibraryItem
import com.example.data.model.PlaylistItem
import com.example.data.model.UserProfile
import com.example.ui.components.formatRelativeTime
import com.example.ui.theme.MonoTabularStyle
import kotlinx.coroutines.delay

@Composable
fun MediaLibraryScreen(
    mediaViewModel: MediaCenterViewModel,
    userProfile: UserProfile,
    onOpenDrawer: () -> Unit,
    onBack: () -> Unit,
    onPlayVideo: (MediaLibraryItem) -> Unit,
    onPlayAudio: (MediaLibraryItem) -> Unit,
    onSendToDownloader: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val state by mediaViewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    val gridState = rememberLazyGridState()
    val layoutDirection = LocalLayoutDirection.current

    AutoHideHeaderLayout(
        modifier = modifier.testTag("media_library_screen"),
        gridState = gridState,
        lockBarsVisible = showAddDialog,
        header = {
            Column(modifier = Modifier.fillMaxWidth()) {
                AppTopBar(
                    title = "Media Library",
                    subtitle = "${state.allItems.size} items · ${state.settings.preferredVideoQuality}",
                    userName = userProfile.name,
                    showBack = true,
                    onBack = onBack,
                    onOpenDrawer = onOpenDrawer,
                    actions = {
                        IconButton(onClick = { showAddDialog = true }, modifier = Modifier.testTag("media_library_add_btn")) {
                            Icon(Icons.Filled.Add, contentDescription = "Add media link", tint = colorScheme.primary)
                        }
                    }
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MediaFilterKind.entries.forEach { kind ->
                        FilterChip(
                            selected = state.filterKind == kind,
                            onClick = { mediaViewModel.setFilter(kind) },
                            label = { Text(kind.label) }
                        )
                    }
                }
                HorizontalDivider(color = colorScheme.outline, thickness = 0.8.dp)
            }
        }
    ) { scrollModifier, contentPadding ->
        if (state.visibleLibraryItems.isEmpty()) {
            EmptyMediaState(
                text = "No media items in this filter",
                tint = colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
            )
        } else {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Adaptive(minSize = 280.dp),
                modifier = scrollModifier,
                contentPadding = PaddingValues(
                    start = contentPadding.calculateStartPadding(layoutDirection) + 16.dp,
                    end = contentPadding.calculateEndPadding(layoutDirection) + 16.dp,
                    top = contentPadding.calculateTopPadding() + 12.dp,
                    bottom = contentPadding.calculateBottomPadding()
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.visibleLibraryItems, key = { it.id }) { item ->
                    MediaItemCard(
                        item = item,
                        playlists = state.playlists,
                        onPlay = {
                            mediaViewModel.selectMediaForPlayback(item)
                            if (item.mediaKind == LibraryMediaKind.VIDEO) onPlayVideo(item) else onPlayAudio(item)
                        },
                        onToggleFavorite = { mediaViewModel.toggleFavorite(item) },
                        onAssignPlaylist = { plId -> mediaViewModel.assignToPlaylist(item.id, plId) },
                        onDownload = { onSendToDownloader(item.sourceUrl) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        var title by rememberSaveable { mutableStateOf("") }
        var creator by rememberSaveable { mutableStateOf("") }
        var url by rememberSaveable { mutableStateOf("") }
        var kind by rememberSaveable { mutableStateOf(LibraryMediaKind.VIDEO) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Stream to Library") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, singleLine = true)
                    OutlinedTextField(value = creator, onValueChange = { creator = it }, label = { Text("Creator") }, singleLine = true)
                    OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("Direct HTTPS URL (.mp4 / .mp3)") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LibraryMediaKind.entries.forEach { k ->
                            FilterChip(selected = kind == k, onClick = { kind = k }, label = { Text(k.label) })
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        mediaViewModel.addMediaItem(title, creator, kind, url)
                        showAddDialog = false
                    },
                    enabled = url.startsWith("http")
                ) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun VideoPlayerScreen(
    mediaViewModel: MediaCenterViewModel,
    userProfile: UserProfile,
    onOpenDrawer: () -> Unit,
    onBack: () -> Unit,
    onSendToDownloader: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val state by mediaViewModel.uiState.collectAsStateWithLifecycle()
    val activeVideo = state.activeVideoItem
    val streamUrl = state.customStreamUrl ?: activeVideo?.sourceUrl
    var isBuffering by remember(streamUrl) { mutableStateOf(streamUrl != null) }
    var playbackError by remember(streamUrl) { mutableStateOf<String?>(null) }
    var isPlaying by remember(streamUrl) { mutableStateOf(state.settings.autoPlayVideos) }
    var userFullscreen by rememberSaveable { mutableStateOf(false) }
    var showOverlayControls by remember { mutableStateOf(true) }
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isImmersive = userFullscreen || isLandscape
    val isDarkTheme = colorScheme.background.luminance() < 0.5f

    BackHandler(enabled = userFullscreen) {
        userFullscreen = false
    }

    ImmersiveVideoSystemBarsEffect(
        immersiveActive = isImmersive,
        forceLandscape = userFullscreen,
        isDarkTheme = isDarkTheme
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(if (isImmersive) Color.Black else colorScheme.background)
            .testTag("video_player_screen")
    ) {
        if (!isImmersive) {
            AppTopBar(
                title = "Video Player",
                subtitle = activeVideo?.let { "${it.resolution} · ${it.creator}" } ?: "Stream ready",
                userName = userProfile.name,
                showBack = true,
                onBack = onBack,
                onOpenDrawer = onOpenDrawer,
                actions = {
                    if (streamUrl != null) {
                        IconButton(
                            onClick = { onSendToDownloader(streamUrl) },
                            modifier = Modifier.testTag("video_download_btn")
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = "Download video", tint = colorScheme.primary)
                        }
                    }
                },
                modifier = Modifier.windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
                )
            )
        }

        Box(
            modifier = Modifier
                .then(
                    if (isImmersive) {
                        Modifier.fillMaxSize()
                    } else {
                        Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                            .aspectRatio(16f / 9f)
                    }
                )
                .background(Color.Black)
                .clickable { showOverlayControls = !showOverlayControls },
            contentAlignment = Alignment.Center
        ) {
            if (streamUrl != null && playbackError == null) {
                AndroidView(
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            val controller = MediaController(ctx)
                            controller.setAnchorView(this)
                            setMediaController(controller)
                            setOnPreparedListener { mp ->
                                isBuffering = false
                                mp.isLooping = false
                                if (state.settings.autoPlayVideos) {
                                    start()
                                    isPlaying = true
                                } else {
                                    isPlaying = false
                                }
                            }
                            setOnCompletionListener {
                                isPlaying = false
                            }
                            setOnErrorListener { _, what, extra ->
                                isBuffering = false
                                isPlaying = false
                                playbackError = "Playback unavailable on this device/network (code $what/$extra)."
                                true
                            }
                            videoViewRef = this
                        }
                    },
                    update = { view ->
                        videoViewRef = view
                        if (view.tag != streamUrl) {
                            view.tag = streamUrl
                            isBuffering = true
                            playbackError = null
                            runCatching {
                                view.setVideoURI(Uri.parse(streamUrl))
                                if (state.settings.autoPlayVideos) {
                                    view.start()
                                    isPlaying = true
                                }
                            }.onFailure { err ->
                                isBuffering = false
                                isPlaying = false
                                playbackError = err.message ?: "Unable to initialize video surface"
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            if (isBuffering && playbackError == null) {
                CircularProgressIndicator(color = colorScheme.primary)
            }
            if (playbackError != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(16.dp)
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                ) {
                    Text(playbackError ?: "", style = MaterialTheme.typography.bodySmall, color = Color.White)
                    Spacer(modifier = Modifier.height(6.dp))
                    TextButton(onClick = { playbackError = null }) {
                        Text("Retry Stream", color = colorScheme.primary)
                    }
                }
            }

            // Cutout-safe interactive video HUD overlay (remains accessible whenever shown or fullscreen)
            androidx.compose.animation.AnimatedVisibility(
                visible = showOverlayControls || !isPlaying || playbackError != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(
                            WindowInsets.displayCutout.union(
                                WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                            )
                        )
                        .padding(8.dp)
                        .testTag("video_controls_overlay")
                ) {
                    if (isImmersive) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                                .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                IconButton(
                                    onClick = {
                                        if (userFullscreen) userFullscreen = false else onBack()
                                    }
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Exit fullscreen", tint = Color.White)
                                }
                                Text(
                                    text = activeVideo?.title ?: "Video Player",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (streamUrl != null) {
                                IconButton(onClick = { onSendToDownloader(streamUrl) }) {
                                    Icon(Icons.Filled.Download, contentDescription = "Download video", tint = colorScheme.primary)
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.62f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                            .testTag("video_player_controls_bar"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    val view = videoViewRef
                                    if (view != null) {
                                        runCatching {
                                            if (view.isPlaying) {
                                                view.pause()
                                                isPlaying = false
                                            } else {
                                                view.start()
                                                isPlaying = true
                                            }
                                        }
                                    } else {
                                        isPlaying = !isPlaying
                                    }
                                },
                                modifier = Modifier.testTag("video_play_pause_btn")
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause video" else "Play video",
                                    tint = Color.White
                                )
                            }
                            Text(
                                text = activeVideo?.resolution ?: "HD",
                                style = MonoTabularStyle.copy(fontSize = 11.sp),
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                        IconButton(
                            onClick = { userFullscreen = !userFullscreen },
                            modifier = Modifier.testTag("video_fullscreen_btn")
                        ) {
                            Icon(
                                imageVector = if (isImmersive) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                                contentDescription = if (isImmersive) "Exit fullscreen" else "Enter fullscreen landscape",
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            if (state.settings.subtitlesEnabled && playbackError == null) {
                Surface(
                    color = Color.Black.copy(alpha = 0.72f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 58.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Subtitles, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = activeVideo?.title ?: "Subtitles Enabled",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
            }
        }

        if (!isImmersive) {
            activeVideo?.let { video ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(video.title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = colorScheme.onSurface)
                        Text("${video.creator} · ${video.durationLabel} · ${video.resolution}", style = MonoTabularStyle, color = colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { mediaViewModel.toggleFavorite(video) }) {
                        Icon(
                            imageVector = if (video.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Favorite video",
                            tint = if (video.isFavorite) Color(0xFFF91880) else colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalDivider(color = colorScheme.outline, thickness = 0.8.dp)
            Text(
                text = "Up Next in Video Library",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = colorScheme.onSurface,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            )
            val navBottomPadding = WindowInsets.navigationBars.union(WindowInsets.displayCutout.only(WindowInsetsSides.Bottom)).asPaddingValues().calculateBottomPadding()
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = navBottomPadding + 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.videoItems, key = { it.id }) { item ->
                    MediaItemCard(
                        item = item,
                        playlists = state.playlists,
                        onPlay = { mediaViewModel.selectMediaForPlayback(item) },
                        onToggleFavorite = { mediaViewModel.toggleFavorite(item) },
                        onAssignPlaylist = { plId -> mediaViewModel.assignToPlaylist(item.id, plId) },
                        onDownload = { onSendToDownloader(item.sourceUrl) }
                    )
                }
            }
        }
    }
}

@Composable
fun AudioPlayerScreen(
    mediaViewModel: MediaCenterViewModel,
    userProfile: UserProfile,
    onOpenDrawer: () -> Unit,
    onBack: () -> Unit,
    onSendToDownloader: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val state by mediaViewModel.uiState.collectAsStateWithLifecycle()
    val tracks = state.audioItems
    val currentTrack = state.activeAudioItem
    val activeUrl = state.customStreamUrl ?: currentTrack?.sourceUrl

    var isPlaying by remember { mutableStateOf(false) }
    var isPreparing by remember { mutableStateOf(false) }
    var positionMs by remember { mutableIntStateOf(0) }
    var durationMs by remember { mutableIntStateOf(1) }
    var audioError by remember(activeUrl) { mutableStateOf<String?>(null) }

    val mediaPlayer = remember { MediaPlayer() }

    DisposableEffect(Unit) {
        onDispose {
            runCatching {
                if (mediaPlayer.isPlaying) mediaPlayer.stop()
                mediaPlayer.release()
            }
        }
    }

    LaunchedEffect(activeUrl) {
        isPlaying = false
        positionMs = 0
        durationMs = 1
        audioError = null
        if (activeUrl != null) {
            isPreparing = true
            runCatching {
                mediaPlayer.reset()
                mediaPlayer.setDataSource(activeUrl)
                mediaPlayer.setOnPreparedListener { mp ->
                    isPreparing = false
                    durationMs = mp.duration.coerceAtLeast(1)
                    mp.start()
                    isPlaying = true
                }
                mediaPlayer.setOnCompletionListener {
                    isPlaying = false
                    positionMs = 0
                }
                mediaPlayer.setOnErrorListener { _, what, extra ->
                    isPreparing = false
                    isPlaying = false
                    audioError = "Audio stream error ($what/$extra)"
                    true
                }
                mediaPlayer.prepareAsync()
            }.onFailure { err ->
                isPreparing = false
                audioError = err.message ?: "Unable to prepare audio stream"
            }
        }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            runCatching {
                if (mediaPlayer.isPlaying) {
                    positionMs = mediaPlayer.currentPosition
                }
            }
            delay(500L)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .testTag("audio_player_screen")
    ) {
        AppTopBar(
            title = "Audio Player",
            subtitle = state.settings.preferredAudioQuality,
            userName = userProfile.name,
            showBack = true,
            onBack = onBack,
            onOpenDrawer = onOpenDrawer,
            modifier = Modifier.windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
            )
        )

        Surface(
            color = colorScheme.surfaceContainer,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, colorScheme.outline),
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(74.dp)
                        .clip(CircleShape)
                        .background(colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    if (isPreparing) {
                        CircularProgressIndicator(color = colorScheme.primary, modifier = Modifier.size(30.dp))
                    } else {
                        Icon(Icons.Filled.MusicNote, contentDescription = null, tint = colorScheme.primary, modifier = Modifier.size(36.dp))
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = currentTrack?.title ?: "Select an audio track",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = currentTrack?.creator ?: "XTRA Spaces & Podcasts",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )

                audioError?.let { err ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(err, style = MaterialTheme.typography.bodySmall, color = colorScheme.error)
                }

                Spacer(modifier = Modifier.height(10.dp))
                Slider(
                    value = positionMs.toFloat().coerceIn(0f, durationMs.toFloat()),
                    onValueChange = { pos ->
                        positionMs = pos.toInt()
                        runCatching { mediaPlayer.seekTo(pos.toInt()) }
                    },
                    valueRange = 0f..durationMs.toFloat().coerceAtLeast(1f)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatAudioMillis(positionMs), style = MonoTabularStyle, color = colorScheme.onSurfaceVariant)
                    Text(currentTrack?.durationLabel ?: formatAudioMillis(durationMs), style = MonoTabularStyle, color = colorScheme.onSurfaceVariant)
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (tracks.isNotEmpty()) {
                                val idx = tracks.indexOfFirst { it.id == currentTrack?.id }.coerceAtLeast(0)
                                val prev = tracks[(idx - 1 + tracks.size) % tracks.size]
                                mediaViewModel.selectMediaForPlayback(prev)
                            }
                        }
                    ) {
                        Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous track")
                    }
                    Surface(
                        color = colorScheme.primary,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .clickable {
                                runCatching {
                                    if (mediaPlayer.isPlaying) {
                                        mediaPlayer.pause()
                                        isPlaying = false
                                    } else {
                                        mediaPlayer.start()
                                        isPlaying = true
                                    }
                                }
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = colorScheme.onPrimary,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            if (tracks.isNotEmpty()) {
                                val idx = tracks.indexOfFirst { it.id == currentTrack?.id }.coerceAtLeast(0)
                                val next = tracks[(idx + 1) % tracks.size]
                                mediaViewModel.selectMediaForPlayback(next)
                            }
                        }
                    ) {
                        Icon(Icons.Filled.SkipNext, contentDescription = "Next track")
                    }
                }
            }
        }

        val navBottomPadding = WindowInsets.navigationBars.union(WindowInsets.displayCutout.only(WindowInsetsSides.Bottom)).asPaddingValues().calculateBottomPadding()
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = navBottomPadding + 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(tracks, key = { it.id }) { item ->
                MediaItemCard(
                    item = item,
                    playlists = state.playlists,
                    onPlay = { mediaViewModel.selectMediaForPlayback(item) },
                    onToggleFavorite = { mediaViewModel.toggleFavorite(item) },
                    onAssignPlaylist = { plId -> mediaViewModel.assignToPlaylist(item.id, plId) },
                    onDownload = { onSendToDownloader(item.sourceUrl) }
                )
            }
        }
    }
}

@Composable
fun PlaylistsScreen(
    mediaViewModel: MediaCenterViewModel,
    userProfile: UserProfile,
    onOpenDrawer: () -> Unit,
    onBack: () -> Unit,
    onPlayItem: (MediaLibraryItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val state by mediaViewModel.uiState.collectAsStateWithLifecycle()
    var showCreate by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val layoutDirection = LocalLayoutDirection.current

    AutoHideHeaderLayout(
        modifier = modifier.testTag("playlists_screen"),
        listState = listState,
        lockBarsVisible = showCreate,
        header = {
            AppTopBar(
                title = "Playlists",
                subtitle = "${state.playlists.size} collections",
                userName = userProfile.name,
                showBack = true,
                onBack = onBack,
                onOpenDrawer = onOpenDrawer,
                actions = {
                    IconButton(onClick = { showCreate = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "New playlist", tint = colorScheme.primary)
                    }
                }
            )
        }
    ) { scrollModifier, contentPadding ->
        if (state.playlists.isEmpty()) {
            EmptyMediaState(
                text = "No playlists yet. Tap + to create one.",
                tint = colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = scrollModifier,
                contentPadding = PaddingValues(
                    start = contentPadding.calculateStartPadding(layoutDirection) + 16.dp,
                    end = contentPadding.calculateEndPadding(layoutDirection) + 16.dp,
                    top = contentPadding.calculateTopPadding() + 12.dp,
                    bottom = contentPadding.calculateBottomPadding()
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.playlists, key = { it.id }) { playlist: PlaylistItem ->
                    val tracks = state.allItems.filter { it.playlistId == playlist.id }
                    Surface(
                        color = colorScheme.surfaceContainer,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Filled.FeaturedPlayList, null, tint = colorScheme.primary)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(playlist.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = colorScheme.onSurface)
                                    Text("${playlist.description} · ${tracks.size} tracks", style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { mediaViewModel.deletePlaylist(playlist.id) }) {
                                    Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete playlist", tint = colorScheme.error)
                                }
                            }
                            if (tracks.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                tracks.forEach { track ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                mediaViewModel.selectMediaForPlayback(track)
                                                onPlayItem(track)
                                            }
                                            .padding(vertical = 6.dp, horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Filled.PlayArrow, null, tint = colorScheme.primary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(track.title, style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurface, modifier = Modifier.weight(1f))
                                        Text(track.durationLabel, style = MonoTabularStyle, color = colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreate) {
        var name by rememberSaveable { mutableStateOf("") }
        var desc by rememberSaveable { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text("New Playlist") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true)
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") })
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        mediaViewModel.createPlaylist(name, desc)
                        showCreate = false
                    },
                    enabled = name.isNotBlank()
                ) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showCreate = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun RecentlyPlayedScreen(
    mediaViewModel: MediaCenterViewModel,
    userProfile: UserProfile,
    onOpenDrawer: () -> Unit,
    onBack: () -> Unit,
    onPlayItem: (MediaLibraryItem) -> Unit,
    onSendToDownloader: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val state by mediaViewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val layoutDirection = LocalLayoutDirection.current

    AutoHideHeaderLayout(
        modifier = modifier.testTag("recently_played_screen"),
        listState = listState,
        header = {
            AppTopBar(
                title = "Recently Played",
                subtitle = "${state.recentlyPlayed.size} played items",
                userName = userProfile.name,
                showBack = true,
                onBack = onBack,
                onOpenDrawer = onOpenDrawer
            )
        }
    ) { scrollModifier, contentPadding ->
        if (state.recentlyPlayed.isEmpty()) {
            EmptyMediaState(
                text = "Nothing played yet",
                tint = colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = scrollModifier,
                contentPadding = PaddingValues(
                    start = contentPadding.calculateStartPadding(layoutDirection) + 16.dp,
                    end = contentPadding.calculateEndPadding(layoutDirection) + 16.dp,
                    top = contentPadding.calculateTopPadding() + 12.dp,
                    bottom = contentPadding.calculateBottomPadding()
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.recentlyPlayed, key = { it.id }) { item ->
                    MediaItemCard(
                        item = item,
                        playlists = state.playlists,
                        extraLabel = item.lastPlayedMillis?.let { "Played ${formatRelativeTime(it)} ago" },
                        onPlay = {
                            mediaViewModel.selectMediaForPlayback(item)
                            onPlayItem(item)
                        },
                        onToggleFavorite = { mediaViewModel.toggleFavorite(item) },
                        onAssignPlaylist = { plId -> mediaViewModel.assignToPlaylist(item.id, plId) },
                        onDownload = { onSendToDownloader(item.sourceUrl) }
                    )
                }
            }
        }
    }
}

@Composable
fun FavoritesMediaScreen(
    mediaViewModel: MediaCenterViewModel,
    userProfile: UserProfile,
    onOpenDrawer: () -> Unit,
    onBack: () -> Unit,
    onPlayItem: (MediaLibraryItem) -> Unit,
    onSendToDownloader: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val state by mediaViewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val layoutDirection = LocalLayoutDirection.current

    AutoHideHeaderLayout(
        modifier = modifier.testTag("favorites_media_screen"),
        listState = listState,
        header = {
            AppTopBar(
                title = "Favorites",
                subtitle = "${state.favorites.size} starred items",
                userName = userProfile.name,
                showBack = true,
                onBack = onBack,
                onOpenDrawer = onOpenDrawer
            )
        }
    ) { scrollModifier, contentPadding ->
        if (state.favorites.isEmpty()) {
            EmptyMediaState(
                text = "Tap the heart icon on any video or audio item to favorite it.",
                tint = colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = scrollModifier,
                contentPadding = PaddingValues(
                    start = contentPadding.calculateStartPadding(layoutDirection) + 16.dp,
                    end = contentPadding.calculateEndPadding(layoutDirection) + 16.dp,
                    top = contentPadding.calculateTopPadding() + 12.dp,
                    bottom = contentPadding.calculateBottomPadding()
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.favorites, key = { it.id }) { item ->
                    MediaItemCard(
                        item = item,
                        playlists = state.playlists,
                        onPlay = {
                            mediaViewModel.selectMediaForPlayback(item)
                            onPlayItem(item)
                        },
                        onToggleFavorite = { mediaViewModel.toggleFavorite(item) },
                        onAssignPlaylist = { plId -> mediaViewModel.assignToPlaylist(item.id, plId) },
                        onDownload = { onSendToDownloader(item.sourceUrl) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaItemCard(
    item: MediaLibraryItem,
    playlists: List<PlaylistItem>,
    extraLabel: String? = null,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAssignPlaylist: (String?) -> Unit,
    onDownload: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    var playlistMenuOpen by remember { mutableStateOf(false) }

    Surface(
        color = colorScheme.surfaceContainer,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onPlay)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (item.mediaKind == LibraryMediaKind.VIDEO) Icons.Filled.VideoLibrary else Icons.Filled.MusicNote,
                    contentDescription = item.mediaKind.label,
                    tint = colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${item.creator} · ${item.durationLabel} · ${item.resolution}", style = MonoTabularStyle, color = colorScheme.onSurfaceVariant)
                if (extraLabel != null) {
                    Text(extraLabel, style = MonoTabularStyle.copy(fontSize = 11.sp), color = colorScheme.primary)
                }
            }
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (item.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (item.isFavorite) Color(0xFFF91880) else colorScheme.onSurfaceVariant
                )
            }
            Box {
                IconButton(onClick = { playlistMenuOpen = true }) {
                    Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = "Add to playlist")
                }
                DropdownMenu(expanded = playlistMenuOpen, onDismissRequest = { playlistMenuOpen = false }) {
                    playlists.forEach { pl ->
                        DropdownMenuItem(
                            text = { Text(pl.name) },
                            onClick = {
                                playlistMenuOpen = false
                                onAssignPlaylist(pl.id)
                            }
                        )
                    }
                    if (item.playlistId != null) {
                        DropdownMenuItem(
                            text = { Text("Remove from playlist") },
                            onClick = {
                                playlistMenuOpen = false
                                onAssignPlaylist(null)
                            }
                        )
                    }
                }
            }
            IconButton(onClick = onDownload) {
                Icon(Icons.Filled.Download, contentDescription = "Download media", tint = colorScheme.primary)
            }
        }
    }
}

@Composable
private fun EmptyMediaState(text: String, tint: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.QueueMusic, contentDescription = null, tint = tint, modifier = Modifier.size(42.dp))
            Spacer(modifier = Modifier.height(10.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, color = tint)
        }
    }
}

private fun formatAudioMillis(ms: Int): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val min = totalSec / 60
    val sec = totalSec % 60
    return String.format("%d:%02d", min, sec)
}
