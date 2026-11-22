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

    init {
        viewModelScope.launch {
            localRepo.ensureSeeded()
        }

        // Only attach Firestore listeners when authenticated (uid != null).
        // Guest mode remains strictly Room-local with zero Firestore network calls.
        // First: replay offline deletes, then push locally-pending writes made while signed out/offline.
        viewModelScope.launch {
            activeUid.collectLatest { uid ->
                if (!uid.isNullOrBlank()) {
                    pushPendingSync(uid)
                }
            }
        }

        activeUid.flatMapLatest { uid ->
            if (uid.isNullOrBlank()) {
                syncStatus.value = false to null
                flowOf(null to null)
            } else {
                combine(
                    firestoreRepo.observeRemoteBookmarks(uid),
                    firestoreRepo.observeRemoteFolders(uid)
                ) { bookmarksRes, foldersRes ->
                    bookmarksRes to foldersRes
                }
            }
        }.onEach { pair ->
            val bookmarksRes = pair.first
            val foldersRes = pair.second
            if (bookmarksRes is FirestoreSyncResult.Loading || foldersRes is FirestoreSyncResult.Loading) {
                syncStatus.value = true to null
            } else {
                val err = (bookmarksRes as? FirestoreSyncResult.Error)?.message
                    ?: (foldersRes as? FirestoreSyncResult.Error)?.message
                syncStatus.value = false to err
            }

            if (foldersRes is FirestoreSyncResult.Success) {
                val domainFolders = foldersRes.data.map { doc ->
                    BookmarkFolder(
                        id = doc.id,
                        name = doc.name,
                        description = doc.description,
                        colorHex = doc.colorHex,
                        iconKey = doc.iconKey
                    )
                }
                localRepo.mergeRemoteFolders(domainFolders)
            }
            if (bookmarksRes is FirestoreSyncResult.Success) {
                val now = System.currentTimeMillis()
                val domainBookmarks = bookmarksRes.data.map { doc ->
                    val created = doc.createdAt?.toDate()?.time ?: now
                    TypedBookmarkItem(
                        id = doc.id,
                        itemType = runCatching { BookmarkItemType.valueOf(doc.itemType) }.getOrDefault(BookmarkItemType.POST),
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
                localRepo.mergeRemoteBookmarks(domainBookmarks)
            }
        }.launchIn(viewModelScope)
    }

    /** Replays tombstoned deletes and pushes locally-pending writes; returns nothing but sets a banner on failure. */
    private suspend fun pushPendingSync(uid: String) {
        localRepo.getPendingRemoteDeletes().forEach { tombstone ->
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
        localRepo.getPendingSyncFolders().forEach { folder ->
            when (firestoreRepo.upsertFolder(uid, folder.id, folder.name, folder.description, folder.colorHex, folder.iconKey)) {
                SyncWriteResult.Synced -> localRepo.markFoldersSynced(listOf(folder.id))
                else -> failures++
            }
        }
        localRepo.getPendingSyncBookmarks().forEach { item ->
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
                SyncWriteResult.Synced -> localRepo.markBookmarksSynced(listOf(item.id))
                else -> failures++
            }
        }
        if (failures > 0) {
            bannerState.value = "$failures change(s) couldn't be synced — kept on device, will retry on next sync"
        }
    }

    val uiState: StateFlow<BookmarksUiState> = combine(
        selectedCategory,
        selectedFolderId,
        searchQuery,
        localRepo.bookmarkFolders,
        localRepo.typedBookmarks,
        localRepo.allPosts,
        localRepo.readLaterPosts,
        activeUid,
        syncStatus,
        bannerState
    ) { values ->
        val category = values[0] as BookmarkFilterCategory
        val folderId = values[1] as String?
        val query = values[2] as String
        @Suppress("UNCHECKED_CAST")
        val folders = values[3] as List<BookmarkFolder>
        @Suppress("UNCHECKED_CAST")
        val typed = values[4] as List<TypedBookmarkItem>
        @Suppress("UNCHECKED_CAST")
        val posts = values[5] as List<PostItem>
        @Suppress("UNCHECKED_CAST")
        val readLater = values[6] as List<PostItem>
        val uid = values[7] as String?
        @Suppress("UNCHECKED_CAST")
        val sync = values[8] as Pair<Boolean, String?>
        val banner = values[9] as String?

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
            isSyncing = sync.first,
            syncError = sync.second,
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
                    SyncWriteResult.Synced -> localRepo.markFoldersSynced(listOf(id))
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
                    SyncWriteResult.Synced -> localRepo.markBookmarksSynced(listOf(created.id))
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
                    SyncWriteResult.Synced -> localRepo.markBookmarksSynced(listOf(bookmark.id))
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
            localRepo.deleteTypedBookmark(bookmark.id, bookmark.postId)
            val uid = activeUid.value
            if (uid != null) {
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
            localRepo.deleteBookmarkFolder(folderId)
            if (selectedFolderId.value == folderId) {
                selectedFolderId.value = null
            }
            val uid = activeUid.value
            if (uid != null) {
                when (firestoreRepo.deleteFolder(uid, folderId)) {
                    SyncWriteResult.Synced -> localRepo.clearPendingRemoteDelete(folderId)
                    is SyncWriteResult.Failed -> Unit // tombstone kept; replayed on next sync
                    SyncWriteResult.GuestMode -> Unit
                }
            }
            bannerState.value = "Collection deleted"
        }
    }

    fun pushAllLocalToFirestoreNow() {
        val uid = activeUid.value
        if (uid.isNullOrBlank()) {
            bannerState.value = "Sign in with Google to synchronize collections with Firestore"
            return
        }
        viewModelScope.launch {
            syncStatus.value = true to null
            val current = uiState.value
            var synced = 0
            var failed = 0
            current.folders.forEach { folder ->
                when (firestoreRepo.upsertFolder(uid, folder.id, folder.name, folder.description, folder.colorHex, folder.iconKey)) {
                    SyncWriteResult.Synced -> {
                        synced++
                        localRepo.markFoldersSynced(listOf(folder.id))
                    }
                    else -> failed++
                }
            }
            current.typedBookmarks.forEach { item ->
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
                        synced++
                        localRepo.markBookmarksSynced(listOf(item.id))
                    }
                    else -> failed++
                }
            }
            syncStatus.value = false to null
            bannerState.value = if (failed == 0) {
                "Synchronized $synced item(s) with Firestore"
            } else {
                "Synced $synced item(s); $failed failed — check your connection and try again"
            }
        }
    }

    fun dismissBanner() {
        bannerState.value = null
    }
}
