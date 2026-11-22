package com.example.feature.tools

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.XProRepository
import com.example.data.model.BrowserHistoryItem
import com.example.data.model.FeedbackSubmission
import com.example.data.model.SavedPageItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private data class ToolsContent(
    val savedPages: List<SavedPageItem>,
    val browserHistory: List<BrowserHistoryItem>,
    val feedbackList: List<FeedbackSubmission>,
    val banner: String?
)

private data class ToolsBrowser(
    val query: String,
    val privateUrl: String
)

data class ToolsUiState(
    val savedPages: List<SavedPageItem> = emptyList(),
    val browserHistory: List<BrowserHistoryItem> = emptyList(),
    val historySearchQuery: String = "",
    val feedbackList: List<FeedbackSubmission> = emptyList(),
    val privateBrowserUrl: String = "https://developer.android.com",
    val statusBanner: String? = null
) {
    val readingListPages: List<SavedPageItem>
        get() = savedPages.filter { it.isInReadingList }

    val filteredHistory: List<BrowserHistoryItem>
        get() {
            val q = historySearchQuery.trim()
            if (q.isEmpty()) return browserHistory
            return browserHistory.filter {
                it.title.contains(q, ignoreCase = true) || it.url.contains(q, ignoreCase = true)
            }
        }
}

class ToolsViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = XProRepository.getInstance(application)

    private val historyQuery = MutableStateFlow("")
    private val privateUrl = MutableStateFlow("https://developer.android.com")
    private val bannerState = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch { repo.ensureSeeded() }
    }

    /** Grouped `combine` to keep element types — see BookmarksViewModel for the rationale. */
    val uiState: StateFlow<ToolsUiState> = combine(
        combine(
            repo.savedPages,
            repo.browserHistory,
            repo.feedbackSubmissions,
            bannerState
        ) { savedPages, browserHistory, feedback, banner ->
            ToolsContent(
                savedPages = savedPages,
                browserHistory = browserHistory,
                feedbackList = feedback,
                banner = banner
            )
        },
        combine(historyQuery, privateUrl) { query, url -> ToolsBrowser(query = query, privateUrl = url) }
    ) { content, browser ->
        ToolsUiState(
            savedPages = content.savedPages,
            browserHistory = content.browserHistory,
            historySearchQuery = browser.query,
            feedbackList = content.feedbackList,
            privateBrowserUrl = browser.privateUrl,
            statusBanner = content.banner
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ToolsUiState()
    )

    fun saveWebPage(title: String, url: String, summary: String, addToReadingList: Boolean) {
        if (url.isBlank()) return
        viewModelScope.launch {
            repo.saveWebPage(title, url, summary, addToReadingList)
            bannerState.value = if (addToReadingList) "Saved to Reading List" else "Web page saved"
        }
    }

    fun toggleReadingList(page: SavedPageItem) {
        viewModelScope.launch { repo.togglePageReadingList(page) }
    }

    fun togglePageCompleted(page: SavedPageItem) {
        viewModelScope.launch { repo.togglePageCompleted(page) }
    }

    fun deleteSavedPage(pageId: String) {
        viewModelScope.launch {
            repo.deleteSavedPage(pageId)
            bannerState.value = "Saved page removed"
        }
    }

    fun updateHistorySearch(query: String) {
        historyQuery.value = query
    }

    fun recordVisit(title: String, url: String) {
        viewModelScope.launch { repo.recordBrowserVisit(title, url) }
    }

    fun deleteHistoryItem(id: String) {
        viewModelScope.launch { repo.deleteBrowserHistoryItem(id) }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repo.clearBrowserHistory()
            bannerState.value = "Browser history cleared"
        }
    }

    fun openPrivateUrl(url: String) {
        val normalized = if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url"
        privateUrl.value = normalized
    }

    fun submitFeedback(category: String, message: String) {
        if (message.isBlank()) return
        viewModelScope.launch {
            repo.submitFeedback(category, message)
            bannerState.value = "Thank you! Feedback recorded locally."
        }
    }

    fun dismissBanner() {
        bannerState.value = null
    }
}
