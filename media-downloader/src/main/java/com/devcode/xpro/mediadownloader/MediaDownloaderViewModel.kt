package com.devcode.xpro.mediadownloader

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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

data class MediaDownloaderUiState(
    val urlInput: String = "",
    val validationResult: UrlValidationResult? = null,
    val downloads: List<MediaDownloadItem> = emptyList(),
    val statusBanner: String? = null,
    val isErrorBanner: Boolean = false
)

class MediaDownloaderViewModel(application: Application) : AndroidViewModel(application) {

    private val downloader = SystemMediaDownloader(application)
    private val urlInputState = MutableStateFlow("")
    private val validationState = MutableStateFlow<UrlValidationResult?>(null)
    private val bannerState = MutableStateFlow<Pair<String?, Boolean>>(null to false)

    init {
        downloader.setCallback(object : DownloadCallback {
            override fun onDownloadStarted(item: MediaDownloadItem) {
                bannerState.value = "DownloadManager queued ${item.fileName} (#${item.downloadId})" to false
            }

            override fun onDownloadSucceeded(item: MediaDownloadItem) {
                bannerState.value = "Download complete: ${item.fileName}" to false
            }

            override fun onDownloadFailed(item: MediaDownloadItem, reason: String) {
                bannerState.value = "Download failed (${item.fileName}): $reason" to true
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

    val uiState: StateFlow<MediaDownloaderUiState> = combine(
        urlInputState,
        validationState,
        downloader.downloads,
        bannerState
    ) { url, validation, downloads, banner ->
        MediaDownloaderUiState(
            urlInput = url,
            validationResult = validation,
            downloads = downloads,
            statusBanner = banner.first,
            isErrorBanner = banner.second
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MediaDownloaderUiState()
    )

    fun onUrlChanged(input: String) {
        urlInputState.value = input
        validationState.value = if (input.isBlank()) null else MediaUrlValidator.validateAndDetect(input)
    }

    fun selectPresetUrl(sampleUrl: String) {
        urlInputState.value = sampleUrl
        validationState.value = MediaUrlValidator.validateAndDetect(sampleUrl)
        bannerState.value = null to false
    }

    fun startDownload(
        resolution: String = "1080p HD",
        wifiOnly: Boolean = false,
        showNotifications: Boolean = true
    ) {
        val validation = MediaUrlValidator.validateAndDetect(urlInputState.value)
        validationState.value = validation
        if (!validation.isValid || validation.mediaType == null) {
            bannerState.value = (validation.errorMessage ?: "Invalid media URL") to true
            return
        }

        val result = downloader.enqueueDownload(
            url = validation.normalizedUrl,
            mediaType = validation.mediaType,
            fileName = validation.suggestedFileName,
            resolution = if (validation.mediaType.category == MediaCategory.VIDEO) resolution else "320 kbps",
            wifiOnly = wifiOnly,
            showNotifications = showNotifications
        )
        result.onFailure { err ->
            bannerState.value = (err.message ?: "DownloadManager rejected request") to true
        }
    }

    fun pauseDownload(downloadId: Long) {
        downloader.pauseDownload(downloadId)
        bannerState.value = "Download paused" to false
    }

    fun resumeDownload(downloadId: Long, wifiOnly: Boolean = false, showNotifications: Boolean = true) {
        downloader.resumeDownload(downloadId, wifiOnly, showNotifications).onFailure { err ->
            bannerState.value = (err.message ?: "Unable to resume download") to true
        }
    }

    fun retryDownload(downloadId: Long, wifiOnly: Boolean = false, showNotifications: Boolean = true) {
        downloader.retryDownload(downloadId, wifiOnly, showNotifications).onFailure { err ->
            bannerState.value = (err.message ?: "Unable to retry download") to true
        }
    }

    fun cancelDownload(downloadId: Long) {
        downloader.cancelDownload(downloadId)
        bannerState.value = "Download cancelled" to false
    }

    fun deleteDownload(downloadId: Long, deleteLocalFile: Boolean = false) {
        downloader.deleteDownload(downloadId, deleteLocalFile)
        bannerState.value = "Download removed" to false
    }

    fun refreshStatus(downloadId: Long) {
        downloader.refreshDownloadStatus(downloadId)
    }

    fun dismissBanner() {
        bannerState.value = null to false
    }

    fun clearHistory() {
        downloader.clearHistory()
        bannerState.value = "Download queue cleared" to false
    }

    override fun onCleared() {
        downloader.cleanup()
        super.onCleared()
    }
}
