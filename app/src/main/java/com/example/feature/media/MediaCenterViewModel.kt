package com.example.feature.media

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.XProRepository
import com.example.data.model.LibraryMediaKind
import com.example.data.model.MediaLibraryItem
import com.example.data.model.PlaylistItem
import com.example.feature.settings.SettingsRepository
import com.example.feature.settings.UserSettingsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class MediaFilterKind(val label: String) {
    ALL("All"),
    VIDEO("Video"),
    AUDIO("Audio")
}

private data class MediaCatalog(
    val filterKind: MediaFilterKind,
    val allItems: List<MediaLibraryItem>,
    val playlists: List<PlaylistItem>,
    val activeMediaId: String?
)

private data class MediaPlayback(
    val customStreamUrl: String?,
    val settings: UserSettingsState,
    val banner: String?
)

data class MediaCenterUiState(
    val filterKind: MediaFilterKind = MediaFilterKind.ALL,
    val allItems: List<MediaLibraryItem> = emptyList(),
    val playlists: List<PlaylistItem> = emptyList(),
    val activeMediaId: String? = null,
    val customStreamUrl: String? = null,
    val settings: UserSettingsState = UserSettingsState(),
    val statusBanner: String? = null
) {
    val visibleLibraryItems: List<MediaLibraryItem>
        get() = when (filterKind) {
            MediaFilterKind.ALL -> allItems
            MediaFilterKind.VIDEO -> allItems.filter { it.mediaKind == LibraryMediaKind.VIDEO }
            MediaFilterKind.AUDIO -> allItems.filter { it.mediaKind == LibraryMediaKind.AUDIO }
        }

    val videoItems: List<MediaLibraryItem>
        get() = allItems.filter { it.mediaKind == LibraryMediaKind.VIDEO }

    val audioItems: List<MediaLibraryItem>
        get() = allItems.filter { it.mediaKind == LibraryMediaKind.AUDIO }

    val recentlyPlayed: List<MediaLibraryItem>
        get() = allItems.filter { it.lastPlayedMillis != null }.sortedByDescending { it.lastPlayedMillis }

    val favorites: List<MediaLibraryItem>
        get() = allItems.filter { it.isFavorite }

    val activeVideoItem: MediaLibraryItem?
        get() = videoItems.firstOrNull { it.id == activeMediaId } ?: videoItems.firstOrNull()

    val activeAudioItem: MediaLibraryItem?
        get() = audioItems.firstOrNull { it.id == activeMediaId } ?: audioItems.firstOrNull()
}

class MediaCenterViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = XProRepository.getInstance(application)
    private val settingsRepo = SettingsRepository(application)

    private val filterKind = MutableStateFlow(MediaFilterKind.ALL)
    private val activeMediaId = MutableStateFlow<String?>(null)
    private val customStreamUrl = MutableStateFlow<String?>(null)
    private val bannerState = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch { repo.ensureSeeded() }
    }

    /** Grouped `combine` to keep element types — see BookmarksViewModel for the rationale. */
    val uiState: StateFlow<MediaCenterUiState> = combine(
        combine(
            filterKind,
            repo.mediaLibrary,
            repo.playlists,
            activeMediaId
        ) { kind, items, playlists, activeId ->
            MediaCatalog(
                filterKind = kind,
                allItems = items,
                playlists = playlists,
                activeMediaId = activeId
            )
        },
        combine(customStreamUrl, settingsRepo.settingsFlow, bannerState) { url, settings, banner ->
            MediaPlayback(customStreamUrl = url, settings = settings, banner = banner)
        }
    ) { catalog, playback ->
        MediaCenterUiState(
            filterKind = catalog.filterKind,
            allItems = catalog.allItems,
            playlists = catalog.playlists,
            activeMediaId = catalog.activeMediaId,
            customStreamUrl = playback.customStreamUrl,
            settings = playback.settings,
            statusBanner = playback.banner
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MediaCenterUiState()
    )

    fun setFilter(kind: MediaFilterKind) {
        filterKind.value = kind
    }

    fun selectMediaForPlayback(item: MediaLibraryItem) {
        customStreamUrl.value = null
        activeMediaId.value = item.id
        viewModelScope.launch {
            repo.recordMediaPlayed(item.id)
        }
    }

    fun setCustomStreamUrl(url: String) {
        customStreamUrl.value = url
    }

    fun toggleFavorite(item: MediaLibraryItem) {
        viewModelScope.launch {
            repo.toggleMediaFavorite(item)
        }
    }

    fun addMediaItem(
        title: String,
        creator: String,
        kind: LibraryMediaKind,
        url: String
    ) {
        if (url.isBlank()) return
        viewModelScope.launch {
            repo.addMediaItemToLibrary(
                title = title,
                creator = creator,
                kind = kind,
                sourceUrl = url,
                resolution = if (kind == LibraryMediaKind.VIDEO) "1080p HD" else "320 kbps"
            )
            bannerState.value = "Added \"$title\" to Media Library"
        }
    }

    fun createPlaylist(name: String, description: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repo.createPlaylist(name, description)
            bannerState.value = "Created playlist \"$name\""
        }
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            repo.deletePlaylist(playlistId)
            bannerState.value = "Playlist removed"
        }
    }

    fun assignToPlaylist(mediaId: String, playlistId: String?) {
        viewModelScope.launch {
            repo.assignMediaToPlaylist(mediaId, playlistId)
            bannerState.value = if (playlistId == null) "Removed from playlist" else "Added to playlist"
        }
    }

    fun dismissBanner() {
        bannerState.value = null
    }
}
