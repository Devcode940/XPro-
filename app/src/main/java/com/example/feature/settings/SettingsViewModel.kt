package com.example.feature.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.devcode.xpro.mediadownloader.SystemMediaDownloader
import com.example.data.local.XProRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class StorageStats(
    val cacheBytes: Long = 0L,
    val downloadedBytes: Long = 0L,
    val databaseBytes: Long = 0L,
    val cachedPostsCount: Int = 0
)

data class SettingsDashboardState(
    val settings: UserSettingsState = UserSettingsState(),
    val storage: StorageStats = StorageStats(),
    val sessions: List<ActiveSessionInfo> = defaultSessions(),
    val statusMessage: String? = null
)

private fun defaultSessions(): List<ActiveSessionInfo> = listOf(
    ActiveSessionInfo("sess_1", "Current Android Device", "Local Session · Active", "Just now", true),
    ActiveSessionInfo("sess_2", "XTRA Web Client", "San Francisco, US", "2h ago", false),
    ActiveSessionInfo("sess_3", "Tablet Companion", "Seattle, US", "Yesterday", false)
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepo = SettingsRepository(application)
    private val xProRepo = XProRepository.getInstance(application)
    private val downloader = SystemMediaDownloader(application)

    private val storageState = MutableStateFlow(StorageStats())
    private val sessionsState = MutableStateFlow(defaultSessions())
    private val statusState = MutableStateFlow<String?>(null)

    init {
        refreshStorageStats()
    }

    val uiState: StateFlow<SettingsDashboardState> = combine(
        settingsRepo.settingsFlow,
        storageState,
        sessionsState,
        statusState
    ) { settings, storage, sessions, status ->
        SettingsDashboardState(
            settings = settings,
            storage = storage,
            sessions = sessions,
            statusMessage = status
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsDashboardState()
    )

    fun refreshStorageStats() {
        viewModelScope.launch {
            val cache = settingsRepo.computeCacheBytes()
            val db = settingsRepo.computeDatabaseBytes()
            val downloads = downloader.totalDownloadedBytes()
            val cachedCount = xProRepo.getOfflineSavedCount()
            storageState.value = StorageStats(
                cacheBytes = cache,
                downloadedBytes = downloads,
                databaseBytes = db,
                cachedPostsCount = cachedCount
            )
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            val remaining = settingsRepo.clearCacheDirectory()
            storageState.value = storageState.value.copy(cacheBytes = remaining)
            statusState.value = "App cache cleared"
        }
    }

    fun clearOfflinePostsCache() {
        viewModelScope.launch {
            xProRepo.clearOfflineCache()
            refreshStorageStats()
            statusState.value = "Offline post cache cleared"
        }
    }

    fun revokeSession(sessionId: String) {
        sessionsState.value = sessionsState.value.filterNot { it.id == sessionId && !it.isCurrent }
        statusState.value = "Remote session revoked"
    }

    fun dismissStatus() {
        statusState.value = null
    }

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { settingsRepo.setThemeMode(mode) }
    fun setAccentColor(accent: AccentColorOption) = viewModelScope.launch { settingsRepo.setAccentColor(accent) }
    fun setFontSize(size: FontSizePreset) = viewModelScope.launch { settingsRepo.setFontSize(size) }
    fun setDisplayDensity(density: DisplayDensity) = viewModelScope.launch { settingsRepo.setDisplayDensity(density) }

    fun setPrivateAccount(enabled: Boolean) = viewModelScope.launch { settingsRepo.setPrivateAccount(enabled) }
    fun addBlockedAccount(handle: String) = viewModelScope.launch { settingsRepo.addBlockedAccount(handle) }
    fun removeBlockedAccount(handle: String) = viewModelScope.launch { settingsRepo.removeBlockedAccount(handle) }
    fun addMutedAccount(handle: String) = viewModelScope.launch { settingsRepo.addMutedAccount(handle) }
    fun removeMutedAccount(handle: String) = viewModelScope.launch { settingsRepo.removeMutedAccount(handle) }

    fun setPushNotifications(enabled: Boolean) = viewModelScope.launch { settingsRepo.setPushNotifications(enabled) }
    fun setNotifyLikes(enabled: Boolean) = viewModelScope.launch { settingsRepo.setNotifyLikes(enabled) }
    fun setNotifyComments(enabled: Boolean) = viewModelScope.launch { settingsRepo.setNotifyComments(enabled) }
    fun setNotifyMessages(enabled: Boolean) = viewModelScope.launch { settingsRepo.setNotifyMessages(enabled) }
    fun setNotifyFollowers(enabled: Boolean) = viewModelScope.launch { settingsRepo.setNotifyFollowers(enabled) }
    fun setNotifyDownloads(enabled: Boolean) = viewModelScope.launch { settingsRepo.setNotifyDownloads(enabled) }

    fun setDownloadLocation(location: String) = viewModelScope.launch { settingsRepo.setDownloadLocation(location) }
    fun setPreferredVideoQuality(quality: String) = viewModelScope.launch { settingsRepo.setPreferredVideoQuality(quality) }
    fun setPreferredAudioQuality(quality: String) = viewModelScope.launch { settingsRepo.setPreferredAudioQuality(quality) }
    fun setWifiOnlyDownloads(enabled: Boolean) = viewModelScope.launch { settingsRepo.setWifiOnlyDownloads(enabled) }
    fun setBackgroundDownloads(enabled: Boolean) = viewModelScope.launch { settingsRepo.setBackgroundDownloads(enabled) }
    fun setAutoMediaDetection(enabled: Boolean) = viewModelScope.launch { settingsRepo.setAutoMediaDetection(enabled) }
    fun setAutoPlayVideos(enabled: Boolean) = viewModelScope.launch { settingsRepo.setAutoPlayVideos(enabled) }
    fun setSubtitlesEnabled(enabled: Boolean) = viewModelScope.launch { settingsRepo.setSubtitlesEnabled(enabled) }

    fun setDataSaverEnabled(enabled: Boolean) = viewModelScope.launch { settingsRepo.setDataSaverEnabled(enabled) }
    fun setAppLanguage(language: String) = viewModelScope.launch { settingsRepo.setAppLanguage(language) }

    fun setLargerText(enabled: Boolean) = viewModelScope.launch { settingsRepo.setLargerText(enabled) }
    fun setReducedMotion(enabled: Boolean) = viewModelScope.launch { settingsRepo.setReducedMotion(enabled) }
    fun setScreenReaderSupport(enabled: Boolean) = viewModelScope.launch { settingsRepo.setScreenReaderSupport(enabled) }
    fun setImprovedContrast(enabled: Boolean) = viewModelScope.launch { settingsRepo.setImprovedContrast(enabled) }

    override fun onCleared() {
        downloader.cleanup()
        super.onCleared()
    }
}
