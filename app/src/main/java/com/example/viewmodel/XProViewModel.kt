package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.data.local.SeedData
import com.example.data.local.XProRepository
import com.example.data.model.AppDestination
import com.example.data.model.BookmarkFolder
import com.example.data.model.FeedFilter
import com.example.data.model.FeedTab
import com.example.data.model.PendingPost
import com.example.data.model.PostItem
import com.example.data.model.ProfileTab
import com.example.data.model.ReaderTypography
import com.example.data.model.SuggestedUser
import com.example.data.model.TrendItem
import com.example.data.model.TypedBookmarkItem
import com.example.data.model.UserProfile
import com.example.data.model.VerificationBadge
import com.example.data.remote.FirestoreBookmarkSyncRepository
import com.example.data.remote.SyncWriteResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class DraftToRestore(
    val content: String,
    val articleTitle: String?,
    val articleSubtitle: String?,
    val isLongForm: Boolean,
    val mediaResKey: String?,
    val mediaUri: String?,
    val pollOptions: List<String>,
    val audience: String,
    val parentPostId: String?
)

data class XProUiState(
    val isLoaded: Boolean = false,
    val destination: AppDestination = AppDestination.HOME,
    val feedTab: FeedTab = FeedTab.FOR_YOU,
    val feedFilter: FeedFilter = FeedFilter.ALL,
    val profileTab: ProfileTab = ProfileTab.POSTS,
    val searchQuery: String = "",
    val bookmarkSearchQuery: String = "",
    val selectedFolderId: String? = null,
    val offlineModeActive: Boolean = false,
    val showMediaDownloader: Boolean = false,
    val mediaDownloaderInitialUrl: String? = null,
    val userProfile: UserProfile = SeedData.initialProfile,
    val allPosts: List<PostItem> = emptyList(),
    val feedPosts: List<PostItem> = emptyList(),
    val articlePosts: List<PostItem> = emptyList(),
    val readLaterPosts: List<PostItem> = emptyList(),
    val bookmarkedPosts: List<PostItem> = emptyList(),
    val explorePosts: List<PostItem> = emptyList(),
    val bookmarkFolders: List<BookmarkFolder> = emptyList(),
    val visibleTrends: List<TrendItem> = emptyList(),
    val suggestedUsers: List<SuggestedUser> = emptyList(),
    val blockedPostAds: List<PostItem> = emptyList(),
    val blockedTrendAds: List<TrendItem> = emptyList(),
    val offlineCachedCount: Int = 0,
    val offlineUnreadCount: Int = 0,
    val pendingPost: PendingPost? = null,
    val restoredDraft: DraftToRestore? = null,
    val showComposerSheet: Boolean = false,
    val replyTargetPost: PostItem? = null,
    val showArticleStudio: Boolean = false,
    val editingPost: PostItem? = null,
    val historyPost: PostItem? = null,
    val readerPost: PostItem? = null,
    val bookmarkTargetPost: PostItem? = null,
    val showPremiumStudio: Boolean = false,
    val showNewFolderDialog: Boolean = false,
    val lightboxMediaResKey: String? = null,
    val lightboxMediaUri: String? = null,
    val readerFontSizeSp: Float = 17f,
    val readerTypography: ReaderTypography = ReaderTypography.SANS,
    val readerWarmSurface: Boolean = false,
    val toastMessage: String? = null
) {
    val blockedAdsCount: Int
        get() = if (userProfile.adFreeEnabled) blockedPostAds.size + blockedTrendAds.size else 0
}

private data class UiControlState(
    val isSeeded: Boolean = false,
    val destination: AppDestination = AppDestination.HOME,
    val feedTab: FeedTab = FeedTab.FOR_YOU,
    val feedFilter: FeedFilter = FeedFilter.ALL,
    val profileTab: ProfileTab = ProfileTab.POSTS,
    val searchQuery: String = "",
    val bookmarkSearchQuery: String = "",
    val selectedFolderId: String? = null,
    val offlineModeActive: Boolean = false,
    val showMediaDownloader: Boolean = false,
    val mediaDownloaderInitialUrl: String? = null,
    val pendingPost: PendingPost? = null,
    val restoredDraft: DraftToRestore? = null,
    val showComposerSheet: Boolean = false,
    val replyTargetPost: PostItem? = null,
    val showArticleStudio: Boolean = false,
    val editingPost: PostItem? = null,
    val historyPost: PostItem? = null,
    val readerPost: PostItem? = null,
    val bookmarkTargetPost: PostItem? = null,
    val showPremiumStudio: Boolean = false,
    val showNewFolderDialog: Boolean = false,
    val lightboxMediaResKey: String? = null,
    val lightboxMediaUri: String? = null,
    val readerFontSizeSp: Float = 17f,
    val readerTypography: ReaderTypography = ReaderTypography.SANS,
    val readerWarmSurface: Boolean = false,
    val toastMessage: String? = null
)

private data class FeedQueryKey(
    val feedTab: FeedTab,
    val feedFilter: FeedFilter,
    val adFree: Boolean,
    val offlineMode: Boolean
)

@OptIn(ExperimentalCoroutinesApi::class)
class XProViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: XProRepository
    private val controls = MutableStateFlow(UiControlState())
    private var undoTimerJob: Job? = null

    // Cloud-sync write-through: repository writes stay Room-local; this layer mirrors them to Firestore.
    private val firestoreSync by lazy { FirestoreBookmarkSyncRepository(application) }
    @Volatile private var cloudUid: String? = null

    /**
     * Called by MainActivity whenever the signed-in Firebase uid changes (null = guest).
     *
     * Also tells the repository who owns new rows: writes are stamped with the active account, so
     * switching accounts cannot re-upload (or reconcile away) the previous account's bookmarks.
     */
    fun setCloudSyncUid(uid: String?) {
        cloudUid = uid
        repository.setActiveOwner(uid)
    }

    private fun pushBookmarkUpsert(item: TypedBookmarkItem) {
        val uid = cloudUid ?: return
        viewModelScope.launch {
            when (firestoreSync.upsertBookmark(
                uid = uid,
                bookmarkId = item.id,
                title = item.title,
                content = item.content,
                authorName = item.authorName,
                authorHandle = item.authorHandle,
                itemType = item.itemType.name,
                mediaUrl = item.mediaUrl,
                linkUrl = item.linkUrl,
                folderId = item.folderId
            )) {
                SyncWriteResult.Synced -> repository.markBookmarksSynced(uid, listOf(item.id))
                else -> Unit // stays pendingSync; retried on next sign-in/sync
            }
        }
    }

    private suspend fun pushBookmarkDelete(bookmarkId: String) {
        val uid = cloudUid ?: return
        when (firestoreSync.deleteBookmark(uid, bookmarkId)) {
            SyncWriteResult.Synced -> repository.clearPendingRemoteDelete(bookmarkId)
            else -> Unit // tombstone kept; replayed on next sign-in/sync
        }
    }

    private suspend fun pushFolderUpsert(id: String, name: String, description: String, colorHex: Long, iconKey: String) {
        val uid = cloudUid ?: return
        when (firestoreSync.upsertFolder(uid, id, name.trim(), description.trim(), colorHex, iconKey)) {
            SyncWriteResult.Synced -> repository.markFoldersSynced(uid, listOf(id))
            else -> Unit
        }
    }

    private suspend fun pushFolderDelete(id: String) {
        val uid = cloudUid ?: return
        when (firestoreSync.deleteFolder(uid, id)) {
            SyncWriteResult.Synced -> repository.clearPendingRemoteDelete(id)
            else -> Unit
        }
    }

    init {
        repository = XProRepository.getInstance(application)
        viewModelScope.launch {
            repository.ensureSeeded()
            controls.update { it.copy(isSeeded = true) }
        }
    }

    val pagedFeedPosts: Flow<PagingData<PostItem>> = combine(
        controls.map { Triple(it.feedTab, it.feedFilter, it.offlineModeActive) }.distinctUntilChanged(),
        repository.userProfile.map { it.adFreeEnabled }.distinctUntilChanged()
    ) { (tab, filter, offline), adFree ->
        FeedQueryKey(feedTab = tab, feedFilter = filter, adFree = adFree, offlineMode = offline)
    }.flatMapLatest { key ->
        repository.getPagedFeedPosts(
            feedTab = key.feedTab,
            filter = key.feedFilter,
            adFree = key.adFree,
            offlineModeActive = key.offlineMode
        )
    }.cachedIn(viewModelScope)

    val pagedFollowedPosts: Flow<PagingData<PostItem>> = repository.userProfile
        .map { it.adFreeEnabled }
        .distinctUntilChanged()
        .flatMapLatest { adFree ->
            repository.getFollowedUsersFeedPaged(adFree = adFree)
        }
        .cachedIn(viewModelScope)

    val uiState: StateFlow<XProUiState> = combine(
        controls,
        repository.userProfile,
        repository.allPosts,
        repository.bookmarkFolders,
        repository.trends,
        repository.suggestedUsers,
        repository.offlineCachedCount,
        repository.offlineUnreadCount
    ) { values ->
        val ctrl = values[0] as UiControlState
        val profile = values[1] as UserProfile
        @Suppress("UNCHECKED_CAST")
        val posts = values[2] as List<PostItem>
        @Suppress("UNCHECKED_CAST")
        val folders = values[3] as List<BookmarkFolder>
        @Suppress("UNCHECKED_CAST")
        val trends = values[4] as List<TrendItem>
        @Suppress("UNCHECKED_CAST")
        val suggested = values[5] as List<SuggestedUser>
        val cachedCount = values[6] as Int
        val unreadCount = values[7] as Int

        val adFree = profile.adFreeEnabled
        val adFilteredPosts = if (adFree) posts.filterNot { it.isSponsored } else posts
        val visibleTrends = if (adFree) trends.filterNot { it.isPromoted } else trends

        val tabPosts = when (ctrl.feedTab) {
            FeedTab.FOR_YOU -> adFilteredPosts.filter { it.parentPostId == null }
            FeedTab.FOLLOWING -> adFilteredPosts.filter {
                it.parentPostId == null && (it.isFollowedAuthor || it.isCurrentUser) && !it.isSponsored
            }
        }

        val readLaterPosts = adFilteredPosts.filter { it.isSavedForOffline }

        val feedPosts = when (ctrl.feedFilter) {
            FeedFilter.ALL -> if (ctrl.offlineModeActive) readLaterPosts.ifEmpty { tabPosts } else tabPosts
            FeedFilter.READ_LATER -> tabPosts.filter { it.isSavedForOffline }
            FeedFilter.MEDIA -> tabPosts.filter { it.mediaResKey != null || it.mediaUri != null }
            FeedFilter.VERIFIED -> tabPosts.filter { it.authorBadge != VerificationBadge.NONE }
            FeedFilter.ARTICLES -> tabPosts.filter { it.isLongForm }
        }

        val articlePosts = adFilteredPosts.filter { it.isLongForm || it.threadPosts.isNotEmpty() }

        val bookmarkedPosts = if (ctrl.selectedFolderId == "read_later") {
            readLaterPosts.filter { post ->
                ctrl.bookmarkSearchQuery.isBlank() ||
                    post.content.contains(ctrl.bookmarkSearchQuery, ignoreCase = true) ||
                    (post.articleTitle?.contains(ctrl.bookmarkSearchQuery, ignoreCase = true) == true) ||
                    post.authorName.contains(ctrl.bookmarkSearchQuery, ignoreCase = true)
            }
        } else {
            adFilteredPosts.filter { post ->
                post.isBookmarked &&
                    (ctrl.selectedFolderId == null || post.bookmarkFolderId == ctrl.selectedFolderId) &&
                    (ctrl.bookmarkSearchQuery.isBlank() ||
                        post.content.contains(ctrl.bookmarkSearchQuery, ignoreCase = true) ||
                        (post.articleTitle?.contains(ctrl.bookmarkSearchQuery, ignoreCase = true) == true) ||
                        post.authorName.contains(ctrl.bookmarkSearchQuery, ignoreCase = true))
            }
        }

        val explorePosts = if (ctrl.searchQuery.isBlank()) {
            adFilteredPosts.filter { it.parentPostId == null }
        } else {
            adFilteredPosts.filter {
                it.content.contains(ctrl.searchQuery, ignoreCase = true) ||
                    it.authorName.contains(ctrl.searchQuery, ignoreCase = true) ||
                    it.authorHandle.contains(ctrl.searchQuery, ignoreCase = true) ||
                    (it.articleTitle?.contains(ctrl.searchQuery, ignoreCase = true) == true)
            }
        }

        // Sync live post reference for open modals if updated in DB
        val liveReaderPost = ctrl.readerPost?.let { rp -> posts.find { it.id == rp.id } ?: rp }
        val liveHistoryPost = ctrl.historyPost?.let { hp -> posts.find { it.id == hp.id } ?: hp }
        val liveBookmarkTarget = ctrl.bookmarkTargetPost?.let { bp -> posts.find { it.id == bp.id } ?: bp }

        XProUiState(
            isLoaded = ctrl.isSeeded,
            destination = ctrl.destination,
            feedTab = ctrl.feedTab,
            feedFilter = ctrl.feedFilter,
            profileTab = ctrl.profileTab,
            searchQuery = ctrl.searchQuery,
            bookmarkSearchQuery = ctrl.bookmarkSearchQuery,
            selectedFolderId = ctrl.selectedFolderId,
            offlineModeActive = ctrl.offlineModeActive,
            showMediaDownloader = ctrl.showMediaDownloader,
            mediaDownloaderInitialUrl = ctrl.mediaDownloaderInitialUrl,
            userProfile = profile,
            allPosts = adFilteredPosts,
            feedPosts = feedPosts,
            articlePosts = articlePosts,
            readLaterPosts = readLaterPosts,
            bookmarkedPosts = bookmarkedPosts,
            explorePosts = explorePosts,
            bookmarkFolders = folders,
            visibleTrends = visibleTrends,
            suggestedUsers = suggested,
            blockedPostAds = posts.filter { it.isSponsored },
            blockedTrendAds = trends.filter { it.isPromoted },
            offlineCachedCount = cachedCount,
            offlineUnreadCount = unreadCount,
            pendingPost = ctrl.pendingPost,
            restoredDraft = ctrl.restoredDraft,
            showComposerSheet = ctrl.showComposerSheet,
            replyTargetPost = ctrl.replyTargetPost,
            showArticleStudio = ctrl.showArticleStudio,
            editingPost = ctrl.editingPost,
            historyPost = liveHistoryPost,
            readerPost = liveReaderPost,
            bookmarkTargetPost = liveBookmarkTarget,
            showPremiumStudio = ctrl.showPremiumStudio,
            showNewFolderDialog = ctrl.showNewFolderDialog,
            lightboxMediaResKey = ctrl.lightboxMediaResKey,
            lightboxMediaUri = ctrl.lightboxMediaUri,
            readerFontSizeSp = ctrl.readerFontSizeSp,
            readerTypography = ctrl.readerTypography,
            readerWarmSurface = ctrl.readerWarmSurface,
            toastMessage = ctrl.toastMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = XProUiState()
    )

    fun selectDestination(destination: AppDestination) {
        controls.update { it.copy(destination = destination) }
    }

    fun selectFeedTab(tab: FeedTab) {
        controls.update { it.copy(feedTab = tab) }
    }

    fun selectFeedFilter(filter: FeedFilter) {
        controls.update { it.copy(feedFilter = filter) }
    }

    fun selectProfileTab(tab: ProfileTab) {
        controls.update { it.copy(profileTab = tab) }
    }

    fun updateSearchQuery(query: String) {
        controls.update { it.copy(searchQuery = query) }
    }

    fun searchTopicInExplore(topic: String) {
        controls.update {
            it.copy(
                destination = AppDestination.EXPLORE,
                searchQuery = topic
            )
        }
    }

    fun updateBookmarkSearchQuery(query: String) {
        controls.update { it.copy(bookmarkSearchQuery = query) }
    }

    fun selectBookmarkFolder(folderId: String?) {
        controls.update { it.copy(selectedFolderId = folderId) }
    }

    fun openComposer(replyTo: PostItem? = null) {
        controls.update {
            it.copy(
                showComposerSheet = true,
                replyTargetPost = replyTo
            )
        }
    }

    fun closeComposer() {
        controls.update {
            it.copy(
                showComposerSheet = false,
                replyTargetPost = null,
                restoredDraft = null
            )
        }
    }

    fun openArticleStudio() {
        controls.update { it.copy(showArticleStudio = true) }
    }

    fun closeArticleStudio() {
        controls.update { it.copy(showArticleStudio = false, restoredDraft = null) }
    }

    fun consumeRestoredDraft() {
        controls.update { it.copy(restoredDraft = null) }
    }

    fun submitPostWithUndo(
        content: String,
        articleTitle: String? = null,
        articleSubtitle: String? = null,
        isLongForm: Boolean = false,
        mediaResKey: String? = null,
        mediaUri: String? = null,
        pollOptions: List<String> = emptyList(),
        audience: String = "Everyone",
        parentPostId: String? = null
    ) {
        val delaySeconds = uiState.value.userProfile.undoDelaySeconds
        controls.update {
            it.copy(
                showComposerSheet = false,
                showArticleStudio = false,
                replyTargetPost = null,
                restoredDraft = null
            )
        }

        if (delaySeconds <= 0) {
            viewModelScope.launch {
                repository.publishPost(
                    content = content,
                    articleTitle = articleTitle,
                    articleSubtitle = articleSubtitle,
                    isLongForm = isLongForm,
                    mediaResKey = mediaResKey,
                    mediaUri = mediaUri,
                    pollOptions = pollOptions,
                    audience = audience,
                    parentPostId = parentPostId
                )
                showToast(if (isLongForm) "Article published" else "Post published")
            }
            return
        }

        // Flush any already pending post immediately before queueing the new one
        controls.value.pendingPost?.let { existing ->
            undoTimerJob?.cancel()
            viewModelScope.launch { commitPending(existing) }
        }

        val totalMs = delaySeconds * 1000L
        val pending = PendingPost(
            id = UUID.randomUUID().toString(),
            content = content,
            articleTitle = articleTitle,
            articleSubtitle = articleSubtitle,
            isLongForm = isLongForm,
            mediaResKey = mediaResKey,
            mediaUri = mediaUri,
            pollOptions = pollOptions,
            audience = audience,
            parentPostId = parentPostId,
            remainingMillis = totalMs,
            totalMillis = totalMs
        )
        controls.update { it.copy(pendingPost = pending) }

        undoTimerJob?.cancel()
        val deadlineMillis = System.currentTimeMillis() + totalMs
        undoTimerJob = viewModelScope.launch {
            val stepMs = 100L
            while (true) {
                val remaining = (deadlineMillis - System.currentTimeMillis()).coerceAtLeast(0L)
                controls.update { state ->
                    val current = state.pendingPost
                    if (current?.id == pending.id) {
                        state.copy(pendingPost = current.copy(remainingMillis = remaining))
                    } else state
                }
                if (remaining <= 0L) break
                delay(stepMs)
            }
            val finalPending = controls.value.pendingPost
            if (finalPending?.id == pending.id) {
                controls.update { it.copy(pendingPost = null) }
                commitPending(finalPending)
                showToast(if (finalPending.isLongForm) "Article published" else "Post published")
            }
        }
    }

    private suspend fun commitPending(pending: PendingPost) {
        repository.publishPost(
            content = pending.content,
            articleTitle = pending.articleTitle,
            articleSubtitle = pending.articleSubtitle,
            isLongForm = pending.isLongForm,
            mediaResKey = pending.mediaResKey,
            mediaUri = pending.mediaUri,
            pollOptions = pending.pollOptions,
            audience = pending.audience,
            parentPostId = pending.parentPostId
        )
    }

    fun cancelPendingPost() {
        val pending = controls.value.pendingPost ?: return
        undoTimerJob?.cancel()
        val restored = DraftToRestore(
            content = pending.content,
            articleTitle = pending.articleTitle,
            articleSubtitle = pending.articleSubtitle,
            isLongForm = pending.isLongForm,
            mediaResKey = pending.mediaResKey,
            mediaUri = pending.mediaUri,
            pollOptions = pending.pollOptions,
            audience = pending.audience,
            parentPostId = pending.parentPostId
        )
        controls.update {
            it.copy(
                pendingPost = null,
                restoredDraft = restored,
                showArticleStudio = pending.isLongForm,
                showComposerSheet = !pending.isLongForm,
                toastMessage = "Send cancelled — draft restored"
            )
        }
    }

    fun sendPendingPostNow() {
        val pending = controls.value.pendingPost ?: return
        undoTimerJob?.cancel()
        controls.update { it.copy(pendingPost = null) }
        viewModelScope.launch {
            commitPending(pending)
            showToast(if (pending.isLongForm) "Article published" else "Post published")
        }
    }

    fun openEditPost(post: PostItem) {
        controls.update { it.copy(editingPost = post) }
    }

    fun closeEditPost() {
        controls.update { it.copy(editingPost = null) }
    }

    fun savePostEdit(postId: String, newContent: String, reason: String) {
        viewModelScope.launch {
            repository.editPost(postId, newContent, reason)
            controls.update { it.copy(editingPost = null) }
            showToast("Post updated · Revision saved")
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            repository.deletePost(postId)
            showToast("Post deleted")
        }
    }

    fun openEditHistory(post: PostItem) {
        controls.update { it.copy(historyPost = post) }
    }

    fun closeEditHistory() {
        controls.update { it.copy(historyPost = null) }
    }

    fun openReaderMode(post: PostItem) {
        controls.update { it.copy(readerPost = post) }
    }

    fun closeReaderMode() {
        controls.update { it.copy(readerPost = null) }
    }

    fun updateReaderFontSize(delta: Float) {
        controls.update {
            it.copy(readerFontSizeSp = (it.readerFontSizeSp + delta).coerceIn(13f, 24f))
        }
    }

    fun setReaderTypography(typography: ReaderTypography) {
        controls.update { it.copy(readerTypography = typography) }
    }

    fun toggleReaderWarmSurface() {
        controls.update { it.copy(readerWarmSurface = !it.readerWarmSurface) }
    }

    fun toggleLike(postId: String) {
        viewModelScope.launch { repository.toggleLike(postId) }
    }

    fun toggleRepost(postId: String) {
        viewModelScope.launch { repository.toggleRepost(postId) }
    }

    fun toggleHighlight(postId: String) {
        viewModelScope.launch {
            repository.toggleHighlight(postId)
            showToast("Profile Highlights updated")
        }
    }

    fun votePoll(postId: String, optionId: String) {
        viewModelScope.launch { repository.votePoll(postId, optionId) }
    }

    fun openBookmarkSheet(post: PostItem) {
        controls.update { it.copy(bookmarkTargetPost = post) }
    }

    fun closeBookmarkSheet() {
        controls.update { it.copy(bookmarkTargetPost = null) }
    }

    fun saveBookmarkToFolder(postId: String, folderId: String?) {
        viewModelScope.launch {
            val item = repository.assignBookmark(postId, folderId)
            if (item != null) pushBookmarkUpsert(item)
            controls.update { it.copy(bookmarkTargetPost = null) }
            showToast("Saved to bookmarks")
        }
    }

    fun removeBookmark(postId: String) {
        viewModelScope.launch {
            // Read the owner first: a row left over from another account must not delete this
            // account's document with the same id.
            val owner = repository.postBookmarkOwner(postId)
            repository.removeBookmark(postId)
            if (repository.isRemoteCopyOwnedBy(owner, cloudUid)) pushBookmarkDelete("tb_$postId")
            controls.update { it.copy(bookmarkTargetPost = null) }
            showToast("Removed from bookmarks")
        }
    }

    fun openNewFolderDialog() {
        controls.update { it.copy(showNewFolderDialog = true) }
    }

    fun closeNewFolderDialog() {
        controls.update { it.copy(showNewFolderDialog = false) }
    }

    fun createBookmarkFolder(name: String, description: String, colorHex: Long, iconKey: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val newId = repository.createBookmarkFolder(name, description, colorHex, iconKey)
            pushFolderUpsert(newId, name, description, colorHex, iconKey)
            val targetPost = controls.value.bookmarkTargetPost
            if (targetPost != null) {
                val item = repository.assignBookmark(targetPost.id, newId)
                if (item != null) pushBookmarkUpsert(item)
            }
            controls.update {
                it.copy(
                    showNewFolderDialog = false,
                    bookmarkTargetPost = null
                )
            }
            showToast("Created folder \"$name\"")
        }
    }

    fun deleteBookmarkFolder(folderId: String) {
        viewModelScope.launch {
            val owner = repository.folderOwner(folderId)
            repository.deleteBookmarkFolder(folderId)
            if (repository.isRemoteCopyOwnedBy(owner, cloudUid)) pushFolderDelete(folderId)
            if (controls.value.selectedFolderId == folderId) {
                controls.update { it.copy(selectedFolderId = null) }
            }
            showToast("Folder deleted")
        }
    }

    fun openPremiumStudio() {
        controls.update { it.copy(showPremiumStudio = true) }
    }

    fun closePremiumStudio() {
        controls.update { it.copy(showPremiumStudio = false) }
    }

    fun saveProfileAndPremiumSettings(
        name: String,
        handle: String,
        bio: String,
        badge: VerificationBadge,
        undoDelaySeconds: Int,
        adFreeEnabled: Boolean,
        extendedPostsEnabled: Boolean,
        showEditHistoryLinks: Boolean,
        autoExpandThreads: Boolean
    ) {
        viewModelScope.launch {
            repository.updateUserProfile(
                name = name,
                handle = handle,
                bio = bio,
                badge = badge,
                undoDelaySeconds = undoDelaySeconds,
                adFreeEnabled = adFreeEnabled,
                extendedPostsEnabled = extendedPostsEnabled,
                showEditHistoryLinks = showEditHistoryLinks,
                autoExpandThreads = autoExpandThreads
            )
            controls.update { it.copy(showPremiumStudio = false) }
            showToast("X Pro settings saved")
        }
    }

    fun toggleAdFreeMode(enabled: Boolean) {
        viewModelScope.launch {
            repository.setAdFreeMode(enabled)
            showToast(if (enabled) "Ad-Free Shield active" else "Ad-Free Shield paused")
        }
    }

    fun toggleFollowUser(user: SuggestedUser) {
        viewModelScope.launch {
            repository.setSuggestedUserFollow(user.id, user.isFollowing)
        }
    }

    fun openLightbox(mediaResKey: String?, mediaUri: String?) {
        controls.update {
            it.copy(lightboxMediaResKey = mediaResKey, lightboxMediaUri = mediaUri)
        }
    }

    fun closeLightbox() {
        controls.update { it.copy(lightboxMediaResKey = null, lightboxMediaUri = null) }
    }

    fun showToast(message: String) {
        controls.update { it.copy(toastMessage = message) }
    }

    fun clearToast() {
        controls.update { it.copy(toastMessage = null) }
    }

    fun toggleOfflineMode() {
        controls.update {
            val next = !it.offlineModeActive
            it.copy(
                offlineModeActive = next,
                feedFilter = if (next) FeedFilter.READ_LATER else FeedFilter.ALL,
                toastMessage = if (next) "Offline Mode active · Reading from Room DB cache" else "Online Mode restored"
            )
        }
    }

    fun toggleSaveForOffline(postId: String) {
        viewModelScope.launch {
            val saved = repository.toggleSaveForOffline(postId)
            showToast(if (saved) "Saved for offline reading (Room DB)" else "Removed from Read Later")
        }
    }

    fun toggleOfflineRead(postId: String) {
        viewModelScope.launch {
            val read = repository.toggleOfflineRead(postId)
            showToast(if (read) "Marked as read" else "Marked as unread")
        }
    }

    fun cacheAllThreadsAndArticlesForOffline() {
        viewModelScope.launch {
            repository.cacheAllThreadsAndArticlesForOffline()
            showToast("All threads & articles cached in Room DB for offline reading")
        }
    }

    fun clearOfflineCache() {
        viewModelScope.launch {
            repository.clearOfflineCache()
            showToast("Offline cache cleared")
        }
    }

    fun openMediaDownloader(initialUrl: String? = null) {
        controls.update {
            it.copy(
                showMediaDownloader = true,
                mediaDownloaderInitialUrl = initialUrl
            )
        }
    }

    fun closeMediaDownloader() {
        controls.update {
            it.copy(
                showMediaDownloader = false,
                mediaDownloaderInitialUrl = null
            )
        }
    }

    fun switchActiveAccount(
        name: String,
        handle: String,
        bio: String,
        badge: VerificationBadge
    ) {
        viewModelScope.launch {
            repository.switchActiveProfileDetails(name, handle, bio, badge)
            showToast("Active account: $handle")
        }
    }

    fun openComposerWithMedia(mediaUri: String) {
        controls.update {
            it.copy(
                restoredDraft = DraftToRestore(
                    content = "",
                    articleTitle = null,
                    articleSubtitle = null,
                    isLongForm = false,
                    mediaResKey = null,
                    mediaUri = mediaUri,
                    pollOptions = emptyList(),
                    audience = "Everyone",
                    parentPostId = null
                ),
                showComposerSheet = true
            )
        }
    }

    fun openReaderByPostId(postId: String) {
        val found = uiState.value.allPosts.firstOrNull { it.id == postId }
        if (found != null) {
            openReaderMode(found)
        }
    }
}
