package com.example.feature.bookmarks

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.XProRepository
import com.example.data.model.BookmarkFolder
import com.example.data.model.BookmarkItemType
import com.example.data.model.PostItem
import com.example.data.model.TypedBookmarkItem
import com.example.data.local.PendingRemoteDeleteKind
import com.example.data.remote.FirestoreBookmarkSyncRepository
import com.example.data.remote.FirestoreSyncResult
import com.example.data.remote.SyncWriteResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class BookmarkFilterCategory(val label: String) {
    ALL("All"),
    POSTS("Posts"),
    VIDEOS("Videos"),
    IMAGES("Images"),
    LINKS("Links"),
    READ_LATER("Read Later ⚡")
}

/**
 * One emission of the remote listeners, tagged with the account it belongs to. Keeping the uid
 * next to the payload is what makes reconcile safe: a snapshot can never be applied to a
 * different account than the one it was requested for.
 */
private data class RemoteState(
    val uid: String?,
    val bookmarks: FirestoreSyncResult<List<com.example.data.remote.SyncedBookmarkDoc>>?,
    val folders: FirestoreSyncResult<List<com.example.data.remote.SyncedBookmarkFolderDoc>>?
)

private data class BookmarkFilters(
    val category: BookmarkFilterCategory,
    val folderId: String?,
    val query: String
)

private data class BookmarkContent(
    val folders: List<BookmarkFolder>,
    val typed: List<TypedBookmarkItem>,
    val posts: List<PostItem>,
    val readLater: List<PostItem>
)

private data class BookmarkSync(
    val uid: String?,
    val isSyncing: Boolean,
    val syncError: String?,
    val banner: String?
)

data class BookmarksUiState(
    val selectedCategory: BookmarkFilterCategory = BookmarkFilterCategory.ALL,
    val selectedFolderId: String? = null,
    val searchQuery: String = "",
    val folders: List<BookmarkFolder> = emptyList(),
    val typedBookmarks: List<TypedBookmarkItem> = emptyList(),
    val savedPosts: List<PostItem> = emptyList(),
    val readLaterPosts: List<PostItem> = emptyList(),
    val isAuthenticated: Boolean = false,
    val isSyncing: Boolean = false,
    val syncError: String? = null,
    val statusBanner: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class BookmarksViewModel(application: Application) : AndroidViewModel(application) {

    private val localRepo = XProRepository.getInstance(application)
    private val firestoreRepo = FirestoreBookmarkSyncRepository(application)

    private val activeUid = MutableStateFlow<String?>(null)
    private val selectedCategory = MutableStateFlow(BookmarkFilterCategory.ALL)
    private val selectedFolderId = MutableStateFlow<String?>(null)
    private val searchQuery = MutableStateFlow("")
    private val syncStatus = MutableStateFlow<Pair<Boolean, String?>>(false to null)
    private val bannerState = MutableStateFlow<String?>(null)
    private val syncRetryTick = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            localRepo.ensureSeeded()
        }

        // Ownership + push. Runs on every account change *and* on every retry request, so a
        // failed push is no longer stranded until the next cold start.
        viewModelScope.launch {
            combine(activeUid, syncRetryTick) { uid, tick -> uid to tick }
                .collectLatest { (uid, tick) ->
                    localRepo.setActiveOwner(uid)
                    if (uid.isNullOrBlank()) {
                        syncStatus.value = false to null
                    } else {
                        // Only a user-initiated retry reports success; an automatic push at
                        // sign-in should stay quiet unless something failed.
                        pushPendingSync(uid, notifyOnSuccess = tick > 0)
                    }
                }
        }

        // Only attach Firestore listeners when authenticated (uid != null).
        // Guest mode remains strictly Room-local with zero Firestore network calls.
        activeUid.flatMapLatest { uid ->
            if (uid.isNullOrBlank()) {
                syncStatus.value = false to null
                flowOf(RemoteState(uid = null, bookmarks = null, folders = null))
            } else {
                combine(
                    firestoreRepo.observeRemoteBookmarks(uid),
                    firestoreRepo.observeRemoteFolders(uid)
                ) { bookmarksRes, foldersRes -> RemoteState(uid, bookmarksRes, foldersRes) }
            }
        }.onEach { state ->
            val bookmarksRes = state.bookmarks
            val foldersRes = state.folders
            if (bookmarksRes == null || foldersRes == null) {
                syncStatus.value = false to null
                return@onEach
            }
            syncStatus.value = if (
                bookmarksRes is FirestoreSyncResult.Loading || foldersRes is FirestoreSyncResult.Loading
            ) {
                true to null
            } else {
                false to (
                    (bookmarksRes as? FirestoreSyncResult.Error)?.message
                        ?: (foldersRes as? FirestoreSyncResult.Error)?.message
                    )
            }

            val uid = state.uid ?: return@onEach
            // Reconcile is destructive (it deletes local rows that are gone remotely), so it is
            // only ever applied to an authoritative server snapshot — never to a cache-sourced
            // one, and never when Firebase turned out to be unavailable.
            if (foldersRes is FirestoreSyncResult.Success && foldersRes.isAuthoritative) {
                localRepo.mergeRemoteFolders(
                    ownerUid = uid,
                    remoteFolders = foldersRes.data.map { doc ->
                        BookmarkFolder(
                            id = doc.id,
                            name = doc.name,
                            description = doc.description,
                            colorHex = doc.colorHex,
                            iconKey = doc.iconKey,
                            updatedAtMillis = doc.updatedAt?.toDate()?.time ?: 0L
                        )
                    }
                )
            }
            if (bookmarksRes is FirestoreSyncResult.Success && bookmarksRes.isAuthoritative) {
                val now = System.currentTimeMillis()
                localRepo.mergeRemoteBookmarks(
                    ownerUid = uid,
                    remoteItems = bookmarksRes.data.map { doc ->
                        val created = doc.createdAt?.toDate()?.time ?: now
                        TypedBookmarkItem(
                            id = doc.id,
                            itemType = runCatching { BookmarkItemType.valueOf(doc.itemType) }
                                .getOrDefault(BookmarkItemType.POST),
                            title = doc.title,
                            content = doc.content,
                            authorName = doc.authorName,
                            authorHandle = doc.authorHandle,
                            mediaUrl = doc.mediaUrl,
                            linkUrl = doc.linkUrl,
                            folderId = doc.folderId,
                            postId = null,
                            createdAtMillis = created,
                            updatedAtMillis = doc.updatedAt?.toDate()?.time ?: created
                        )
                    }
                )
            }
        }.launchIn(viewModelScope)
    }

    /**
     * Claims device-local rows for [uid], replays tombstoned deletes, then pushes pending
     * folders and bookmarks. Every query is scoped to [uid], so rows belonging to a previously
     * signed-in account are never uploaded or deleted on its behalf.
     */
    private suspend fun pushPendingSync(uid: String, notifyOnSuccess: Boolean = false) {
        localRepo.claimUnownedRows(uid)
        var pushed = 0

        localRepo.getPendingRemoteDeletes(uid).forEach { tombstone ->
            val res = when (tombstone.kind) {
                PendingRemoteDeleteKind.BOOKMARK -> firestoreRepo.deleteBookmark(uid, tombstone.id)
                PendingRemoteDeleteKind.FOLDER -> firestoreRepo.deleteFolder(uid, tombstone.id)
                else -> SyncWriteResult.GuestMode
            }
            if (res is SyncWriteResult.Synced) {
                localRepo.clearPendingRemoteDelete(tombstone.id)
            }
        }

        var failures = 0
        localRepo.getPendingSyncFolders(uid).forEach { folder ->
            when (firestoreRepo.upsertFolder(uid, folder.id, folder.name, folder.description, folder.colorHex, folder.iconKey)) {
                SyncWriteResult.Synced -> {
                    pushed++
                    localRepo.markFoldersSynced(uid, listOf(folder.id))
                }
                else -> failures++
            }
        }
        localRepo.getPendingSyncBookmarks(uid).forEach { item ->
            when (firestoreRepo.upsertBookmark(
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
                SyncWriteResult.Synced -> {
                    pushed++
                    localRepo.markBookmarksSynced(uid, listOf(item.id))
                }
                else -> failures++
            }
        }
        if (failures == 0) syncStatus.value = false to null
        when {
            failures > 0 -> bannerState.value =
                "$failures change(s) couldn't be synced — kept on this device. Tap Sync to retry."
            notifyOnSuccess && pushed > 0 -> bannerState.value = "Synced $pushed change(s) with Firestore"
            notifyOnSuccess -> bannerState.value = "Everything is already in sync"
        }
    }

    /**
     * Grouped `combine`, not a 10-arity vararg: the vararg overload erases the element types, and
     * indexing it with `values[n] as T` turns "someone reordered the flows" into a
     * ClassCastException at runtime instead of a compile error.
     */
    val uiState: StateFlow<BookmarksUiState> = combine(
        combine(selectedCategory, selectedFolderId, searchQuery) { category, folderId, query ->
            BookmarkFilters(category = category, folderId = folderId, query = query)
        },
        combine(
            localRepo.bookmarkFolders,
            localRepo.typedBookmarks,
            localRepo.allPosts,
            localRepo.readLaterPosts
        ) { folders, typed, posts, readLater ->
            BookmarkContent(folders = folders, typed = typed, posts = posts, readLater = readLater)
        },
        combine(activeUid, syncStatus, bannerState) { uid, sync, banner ->
            BookmarkSync(uid = uid, isSyncing = sync.first, syncError = sync.second, banner = banner)
        }
    ) { filters, content, sync ->
        val category = filters.category
        val folderId = filters.folderId
        val query = filters.query
        val folders = content.folders
        val typed = content.typed
        val posts = content.posts
        val readLater = content.readLater
        val uid = sync.uid
        val banner = sync.banner

        val filteredTyped = typed.filter { item ->
            val matchesCategory = when (category) {
                BookmarkFilterCategory.ALL -> true
                BookmarkFilterCategory.POSTS -> item.itemType == BookmarkItemType.POST
                BookmarkFilterCategory.VIDEOS -> item.itemType == BookmarkItemType.VIDEO
                BookmarkFilterCategory.IMAGES -> item.itemType == BookmarkItemType.IMAGE
                BookmarkFilterCategory.LINKS -> item.itemType == BookmarkItemType.LINK
                BookmarkFilterCategory.READ_LATER -> false
            }
            val matchesFolder = folderId == null || item.folderId == folderId
            val matchesQuery = query.isBlank() ||
                item.title.contains(query, ignoreCase = true) ||
                item.content.contains(query, ignoreCase = true) ||
                item.authorName.contains(query, ignoreCase = true)
            matchesCategory && matchesFolder && matchesQuery
        }

        val bookmarkedPosts = posts.filter { post ->
            !post.isSponsored && post.isBookmarked &&
                (folderId == null || post.bookmarkFolderId == folderId) &&
                (query.isBlank() ||
                    post.content.contains(query, ignoreCase = true) ||
                    post.authorName.contains(query, ignoreCase = true) ||
                    (post.articleTitle?.contains(query, ignoreCase = true) == true))
        }

        val filteredReadLater = readLater.filter { post ->
            query.isBlank() ||
                post.content.contains(query, ignoreCase = true) ||
                post.authorName.contains(query, ignoreCase = true)
        }

        BookmarksUiState(
            selectedCategory = category,
            selectedFolderId = folderId,
            searchQuery = query,
            folders = folders,
            typedBookmarks = filteredTyped,
            savedPosts = bookmarkedPosts,
            readLaterPosts = filteredReadLater,
            isAuthenticated = !uid.isNullOrBlank(),
            isSyncing = sync.isSyncing,
            syncError = sync.syncError,
            statusBanner = banner
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BookmarksUiState()
    )

    fun setAuthenticatedUid(uid: String?) {
        activeUid.value = uid
    }

    /** User-triggered sync: re-plays tombstones and pending writes for the current account. */
    fun retrySync() {
        if (activeUid.value.isNullOrBlank()) {
            bannerState.value = "Sign in with Google to synchronize collections with Firestore"
            return
        }
        syncRetryTick.update { it + 1 }
    }

    fun selectCategory(category: BookmarkFilterCategory) {
        selectedCategory.value = category
    }

    fun selectFolder(folderId: String?) {
        selectedFolderId.value = folderId
    }

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun createCollection(name: String, description: String, colorHex: Long) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = localRepo.createBookmarkFolder(name, description, colorHex, "folder")
            val uid = activeUid.value
            if (uid != null) {
                when (val res = firestoreRepo.upsertFolder(uid, id, name.trim(), description.trim(), colorHex, "folder")) {
                    SyncWriteResult.Synced -> localRepo.markFoldersSynced(uid, listOf(id))
                    is SyncWriteResult.Failed -> {
                        bannerState.value = "Collection saved on this device — sync failed: ${res.message}"
                        return@launch
                    }
                    SyncWriteResult.GuestMode -> Unit
                }
            }
            bannerState.value = "Created collection \"$name\""
        }
    }

    fun addTypedBookmark(
        itemType: BookmarkItemType,
        title: String,
        content: String,
        mediaUrl: String?,
        linkUrl: String?,
        folderId: String?
    ) {
        viewModelScope.launch {
            val profile = localRepo.currentProfile()
            val created = localRepo.addTypedBookmark(
                itemType = itemType,
                title = title,
                content = content,
                authorName = profile.name,
                authorHandle = profile.handle,
                mediaUrl = mediaUrl,
                linkUrl = linkUrl,
                folderId = folderId
            )
            val uid = activeUid.value
            if (uid != null) {
                when (val res = firestoreRepo.upsertBookmark(
                    uid = uid,
                    bookmarkId = created.id,
                    title = created.title,
                    content = created.content,
                    authorName = created.authorName,
                    authorHandle = created.authorHandle,
                    itemType = created.itemType.name,
                    mediaUrl = created.mediaUrl,
                    linkUrl = created.linkUrl,
                    folderId = created.folderId
                )) {
                    SyncWriteResult.Synced -> localRepo.markBookmarksSynced(uid, listOf(created.id))
                    is SyncWriteResult.Failed -> {
                        bannerState.value = "Saved on this device — sync failed: ${res.message}"
                        return@launch
                    }
                    SyncWriteResult.GuestMode -> Unit
                }
            }
            bannerState.value = "Saved ${itemType.label.lowercase()} bookmark"
        }
    }

    fun moveBookmarkToFolder(bookmark: TypedBookmarkItem, folderId: String?) {
        viewModelScope.launch {
            localRepo.assignTypedBookmarkFolder(bookmark.id, folderId)
            bookmark.postId?.let { postId -> localRepo.assignBookmark(postId, folderId) }
            val uid = activeUid.value
            if (uid != null) {
                when (val res = firestoreRepo.upsertBookmark(
                    uid = uid,
                    bookmarkId = bookmark.id,
                    title = bookmark.title,
                    content = bookmark.content,
                    authorName = bookmark.authorName,
                    authorHandle = bookmark.authorHandle,
                    itemType = bookmark.itemType.name,
                    mediaUrl = bookmark.mediaUrl,
                    linkUrl = bookmark.linkUrl,
                    folderId = folderId
                )) {
                    SyncWriteResult.Synced -> localRepo.markBookmarksSynced(uid, listOf(bookmark.id))
                    is SyncWriteResult.Failed -> {
                        bannerState.value = "Moved locally — sync failed: ${res.message}"
                        return@launch
                    }
                    SyncWriteResult.GuestMode -> Unit
                }
            }
            bannerState.value = "Moved bookmark to collection"
        }
    }

    fun removeTypedBookmark(bookmark: TypedBookmarkItem) {
        viewModelScope.launch {
            // Owner is read before the row disappears; rows owned by another account keep their
            // tombstone for that account's next sync instead of touching this account's cloud.
            val owner = localRepo.typedBookmarkOwner(bookmark.id)
            localRepo.deleteTypedBookmark(bookmark.id, bookmark.postId)
            val uid = activeUid.value
            if (uid != null && localRepo.isRemoteCopyOwnedBy(owner, uid)) {
                when (firestoreRepo.deleteBookmark(uid, bookmark.id)) {
                    SyncWriteResult.Synced -> localRepo.clearPendingRemoteDelete(bookmark.id)
                    is SyncWriteResult.Failed -> Unit // tombstone kept; replayed on next sync
                    SyncWriteResult.GuestMode -> Unit
                }
            }
            bannerState.value = "Bookmark removed"
        }
    }

    fun deleteCollection(folderId: String) {
        viewModelScope.launch {
            val owner = localRepo.folderOwner(folderId)
            localRepo.deleteBookmarkFolder(folderId)
            if (selectedFolderId.value == folderId) {
                selectedFolderId.value = null
            }
            val uid = activeUid.value
            if (uid != null && localRepo.isRemoteCopyOwnedBy(owner, uid)) {
                when (firestoreRepo.deleteFolder(uid, folderId)) {
                    SyncWriteResult.Synced -> localRepo.clearPendingRemoteDelete(folderId)
                    is SyncWriteResult.Failed -> Unit // tombstone kept; replayed on next sync
                    SyncWriteResult.GuestMode -> Unit
                }
            }
            bannerState.value = "Collection deleted"
        }
    }

    /**
     * Manual "sync now". Previously this pushed whatever the UI happened to be showing, which
     * included rows owned by other accounts; it now goes through the same scoped push path.
     */
    fun pushAllLocalToFirestoreNow() {
        if (activeUid.value.isNullOrBlank()) {
            bannerState.value = "Sign in with Google to synchronize collections with Firestore"
            return
        }
        viewModelScope.launch {
            syncStatus.value = true to null
            retrySync()
        }
    }

    fun dismissBanner() {
        bannerState.value = null
    }
}
