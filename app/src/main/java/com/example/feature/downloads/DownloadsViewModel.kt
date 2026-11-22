package com.example.feature.downloads

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.devcode.xpro.mediadownloader.DownloadCallback
import com.devcode.xpro.mediadownloader.DownloadStatus
import com.devcode.xpro.mediadownloader.MediaDownloadItem
import com.devcode.xpro.mediadownloader.MediaUrlValidator
import com.devcode.xpro.mediadownloader.SystemMediaDownloader
import com.devcode.xpro.mediadownloader.UrlValidationResult
import com.example.feature.settings.SettingsRepository
import com.example.feature.settings.UserSettingsState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class DownloadsFilterTab(val label: String) {
    ALL("All"),
    DOWNLOADING("Downloading"),
    COMPLETED("Completed"),
    FAILED("Failed")
}

data class DownloadsUiState(
    val selectedTab: DownloadsFilterTab = DownloadsFilterTab.ALL,
    val urlInput: String = "",
    val validationResult: UrlValidationResult? = null,
    val allDownloads: List<MediaDownloadItem> = emptyList(),
    val visibleDownloads: List<MediaDownloadItem> = emptyList(),
    val settings: UserSettingsState = UserSettingsState(),
    val statusBanner: String? = null,
    val isErrorBanner: Boolean = false
)

class DownloadsViewModel(application: Application) : AndroidViewModel(application) {

    private val downloader = SystemMediaDownloader.getInstance(application)
    private val settingsRepo = SettingsRepository(application)

    private val selectedTab = MutableStateFlow(DownloadsFilterTab.ALL)
    private val urlInput = MutableStateFlow("")
    private val validation = MutableStateFlow<UrlValidationResult?>(null)
    private val banner = MutableStateFlow<Pair<String?, Boolean>>(null to false)

    init {
        downloader.setCallback(object : DownloadCallback {
            override fun onDownloadStarted(item: MediaDownloadItem) {
                banner.value = "Downloading ${item.fileName} (${item.resolution})" to false
            }

            override fun onDownloadSucceeded(item: MediaDownloadItem) {
                banner.value = "Completed: ${item.fileName}" to false
            }

            override fun onDownloadFailed(item: MediaDownloadItem, reason: String) {
                banner.value = "Failed (${item.fileName}): $reason" to true
            }
        })

        viewModelScope.launch {
            downloader.downloads
                .map { items -> items.any { it.status == DownloadStatus.QUEUED || it.status == DownloadStatus.DOWNLOADING } }
                .distinctUntilChanged()
                .collectLatest { hasActive ->
                    while (hasActive) {
                        downloader.refreshActiveDownloads()
                        delay(750L)
                    }
                }
        }
    }

    val uiState: StateFlow<DownloadsUiState> = combine(
        selectedTab,
        urlInput,
        validation,
        downloader.downloads,
        settingsRepo.settingsFlow,
        banner
    ) { values ->
        val tab = values[0] as DownloadsFilterTab
        val url = values[1] as String
        val valResult = values[2] as UrlValidationResult?
        @Suppress("UNCHECKED_CAST")
        val all = values[3] as List<MediaDownloadItem>
        val settings = values[4] as UserSettingsState
        @Suppress("UNCHECKED_CAST")
        val bannerPair = values[5] as Pair<String?, Boolean>

        val filtered = when (tab) {
            DownloadsFilterTab.ALL -> all
            DownloadsFilterTab.DOWNLOADING -> all.filter {
                it.status == DownloadStatus.QUEUED || it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.PAUSED
            }
            DownloadsFilterTab.COMPLETED -> all.filter { it.status == DownloadStatus.SUCCEEDED }
            // Cancelled downloads are grouped with failures so they don't silently disappear from every tab.
            DownloadsFilterTab.FAILED -> all.filter { it.status == DownloadStatus.FAILED || it.status == DownloadStatus.CANCELLED }
        }

        DownloadsUiState(
            selectedTab = tab,
            urlInput = url,
            validationResult = valResult,
            allDownloads = all,
            visibleDownloads = filtered,
            settings = settings,
            statusBanner = bannerPair.first,
            isErrorBanner = bannerPair.second
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DownloadsUiState()
    )

    fun selectTab(tab: DownloadsFilterTab) {
        selectedTab.value = tab
    }

    fun onUrlChanged(input: String) {
        urlInput.value = input
        validation.value = if (input.isBlank()) null else MediaUrlValidator.validateAndDetect(input)
    }

    fun startDownload() {
        val result = MediaUrlValidator.validateAndDetect(urlInput.value)
        validation.value = result
        val detectedType = result.mediaType
        if (!result.isValid || detectedType == null) {
            banner.value = (result.errorMessage ?: "Invalid media URL") to true
            return
        }
        val settings = uiState.value.settings
        downloader.enqueueDownload(
            url = result.normalizedUrl,
            mediaType = detectedType,
            fileName = result.suggestedFileName,
            resolution = settings.preferredVideoQuality,
            wifiOnly = settings.wifiOnlyDownloads,
            showNotifications = settings.notifyDownloads
        ).onFailure { err ->
            banner.value = (err.message ?: "Download failed") to true
        }
    }

    fun pauseDownload(downloadId: Long) {
        downloader.pauseDownload(downloadId)
        banner.value = "Paused — resuming re-downloads from the start" to false
    }

    fun resumeDownload(downloadId: Long) {
        val s = uiState.value.settings
        downloader.resumeDownload(downloadId, s.wifiOnlyDownloads, s.notifyDownloads).onFailure {
            banner.value = (it.message ?: "Unable to resume") to true
        }
    }

    fun retryDownload(downloadId: Long) {
        val s = uiState.value.settings
        downloader.retryDownload(downloadId, s.wifiOnlyDownloads, s.notifyDownloads).onFailure {
            banner.value = (it.message ?: "Unable to retry") to true
        }
    }

    fun cancelDownload(downloadId: Long) {
        downloader.cancelDownload(downloadId)
        banner.value = "Download cancelled" to false
    }

    fun deleteDownload(downloadId: Long) {
        downloader.deleteDownload(downloadId, deleteLocalFile = false)
        banner.value = "Download removed from the list" to false
    }

    fun refreshAll() {
        downloader.refreshActiveDownloads()
    }

    fun clearAll() {
        downloader.clearHistory()
        banner.value = "Download queue cleared" to false
    }

    fun dismissBanner() {
        banner.value = null to false
    }

    override fun onCleared() {
        // The downloader is process-scoped; detach this screen's callback instead of
        // unregistering the shared completion receiver.
        downloader.release()
        super.onCleared()
    }
}
