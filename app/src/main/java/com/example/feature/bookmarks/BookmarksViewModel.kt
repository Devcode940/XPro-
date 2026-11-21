package com.example.feature.bookmarks

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.XProRepository
import com.example.data.model.BookmarkFolder
import com.example.data.model.BookmarkItemType
import com.example.data.model.PostItem
import com.example.data.model.TypedBookmarkItem
import com.example.data.remote.FirestoreBookmarkSyncRepository
import com.example.data.remote.FirestoreSyncResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
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

            if (foldersRes is FirestoreSyncResult.Success && foldersRes.data.isNotEmpty()) {
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
            if (bookmarksRes is FirestoreSyncResult.Success && bookmarksRes.data.isNotEmpty()) {
                val domainBookmarks = bookmarksRes.data.map { doc ->
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
                        createdAtMillis = doc.createdAt?.toDate()?.time ?: System.currentTimeMillis()
                    )
                }
                localRepo.mergeRemoteBookmarks(domainBookmarks)
            }
        }.launchIn(viewModelScope)
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
            activeUid.value?.let { uid ->
                firestoreRepo.upsertFolder(uid, id, name.trim(), description.trim(), colorHex, "folder")
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
            val created = localRepo.addTypedBookmark(
                itemType = itemType,
                title = title,
                content = content,
                authorName = "Elena Vance",
                authorHandle = "@elenavance",
                mediaUrl = mediaUrl,
                linkUrl = linkUrl,
                folderId = folderId
            )
            activeUid.value?.let { uid ->
                firestoreRepo.upsertBookmark(
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
                )
            }
            bannerState.value = "Saved ${itemType.label.lowercase()} bookmark"
        }
    }

    fun moveBookmarkToFolder(bookmark: TypedBookmarkItem, folderId: String?) {
        viewModelScope.launch {
            localRepo.assignTypedBookmarkFolder(bookmark.id, folderId)
            bookmark.postId?.let { postId -> localRepo.assignBookmark(postId, folderId) }
            activeUid.value?.let { uid ->
                firestoreRepo.upsertBookmark(
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
                )
            }
            bannerState.value = "Moved bookmark to collection"
        }
    }

    fun removeTypedBookmark(bookmark: TypedBookmarkItem) {
        viewModelScope.launch {
            localRepo.deleteTypedBookmark(bookmark.id, bookmark.postId)
            activeUid.value?.let { uid ->
                firestoreRepo.deleteBookmark(uid, bookmark.id)
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
            activeUid.value?.let { uid ->
                firestoreRepo.deleteFolder(uid, folderId)
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
            current.folders.forEach { folder ->
                firestoreRepo.upsertFolder(uid, folder.id, folder.name, folder.description, folder.colorHex, folder.iconKey)
            }
            current.typedBookmarks.forEach { item ->
                firestoreRepo.upsertBookmark(
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
                )
            }
            syncStatus.value = false to null
            bannerState.value = "Synchronized ${current.typedBookmarks.size} bookmarks & ${current.folders.size} collections with Firestore"
        }
    }

    fun dismissBanner() {
        bannerState.value = null
    }
}
