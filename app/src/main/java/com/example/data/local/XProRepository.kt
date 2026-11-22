package com.example.data.local

import android.content.Context
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.example.BuildConfig
import com.example.data.model.BookmarkFolder
import com.example.data.model.BookmarkItemType
import com.example.data.model.BrowserHistoryItem
import com.example.data.model.CommunityItem
import com.example.data.model.DirectMessage
import com.example.data.model.FeedFilter
import com.example.data.model.FeedTab
import com.example.data.model.FeedbackSubmission
import com.example.data.model.LibraryMediaKind
import com.example.data.model.MediaLibraryItem
import com.example.data.model.MessageConversation
import com.example.data.model.NotificationItem
import com.example.data.model.PlaylistItem
import com.example.data.model.PollData
import com.example.data.model.PollOption
import com.example.data.model.PostItem
import com.example.data.model.PostRevision
import com.example.data.model.SavedPageItem
import com.example.data.model.SuggestedUser
import com.example.data.model.TrendItem
import com.example.data.model.TypedBookmarkItem
import com.example.data.model.UserProfile
import com.example.data.model.VerificationBadge
import com.example.data.model.XListItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

class XProRepository(private val dao: XProDao) {

    private val seedMutex = Mutex()
    @Volatile
    private var seeded = false

    /**
     * The cloud account currently signed in (null = guest). Every row created while this is set
     * is stamped with it so sync can never push — or reconcile away — another account's data.
     * Set from the UI layer (`BookmarksViewModel` / `XProViewModel`), never read from Firebase here.
     */
    @Volatile
    private var activeOwnerUid: String? = null

    fun setActiveOwner(uid: String?) {
        activeOwnerUid = uid?.takeIf { it.isNotBlank() }
    }

    private fun ownerStamp(): String? = activeOwnerUid

    val allPosts: Flow<List<PostItem>> = dao.observeAllPosts().map { entities ->
        entities.map { it.toDomain() }
    }

    val offlineCachedCount: Flow<Int> = dao.observeOfflineCachedCount()
    val offlineUnreadCount: Flow<Int> = dao.observeOfflineUnreadCount()

    val typedBookmarks: Flow<List<TypedBookmarkItem>> = dao.observeTypedBookmarks().map { list ->
        list.map { it.toDomain() }
    }

    val notifications: Flow<List<NotificationItem>> = dao.observeNotifications().map { list ->
        list.map { it.toDomain() }
    }

    val conversations: Flow<List<MessageConversation>> = dao.observeConversations().map { list ->
        list.map { it.toDomain() }
    }

    val xLists: Flow<List<XListItem>> = dao.observeXLists().map { list ->
        list.map { it.toDomain() }
    }

    val communities: Flow<List<CommunityItem>> = dao.observeCommunities().map { list ->
        list.map { it.toDomain() }
    }

    val mediaLibrary: Flow<List<MediaLibraryItem>> = dao.observeMediaLibrary().map { list ->
        list.map { it.toDomain() }
    }

    val playlists: Flow<List<PlaylistItem>> = combine(
        dao.observePlaylists(),
        dao.observeMediaLibrary()
    ) { pls, items ->
        pls.map { pl ->
            pl.toDomain(trackCount = items.count { it.playlistId == pl.id })
        }
    }

    val savedPages: Flow<List<SavedPageItem>> = dao.observeSavedPages().map { list ->
        list.map { it.toDomain() }
    }

    val browserHistory: Flow<List<BrowserHistoryItem>> = dao.observeBrowserHistory().map { list ->
        list.map { it.toDomain() }
    }

    val feedbackSubmissions: Flow<List<FeedbackSubmission>> = dao.observeFeedback().map { list ->
        list.map { it.toDomain() }
    }

    fun observeMessagesForConversation(conversationId: String): Flow<List<DirectMessage>> =
        dao.observeMessagesForConversation(conversationId).map { list -> list.map { it.toDomain() } }

    fun getPagedFeedPosts(
        feedTab: FeedTab,
        filter: FeedFilter,
        adFree: Boolean,
        offlineModeActive: Boolean
    ): Flow<PagingData<PostItem>> {
        val followingOnly = feedTab == FeedTab.FOLLOWING
        val readLaterOnly = filter == FeedFilter.READ_LATER || (offlineModeActive && filter == FeedFilter.ALL)
        val mediaOnly = filter == FeedFilter.MEDIA
        val verifiedOnly = filter == FeedFilter.VERIFIED
        val articlesOnly = filter == FeedFilter.ARTICLES

        return Pager(
            config = PagingConfig(
                pageSize = 5,
                initialLoadSize = 8,
                prefetchDistance = 2,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                dao.pagingFeedPosts(
                    excludeSponsored = adFree,
                    followingOnly = followingOnly,
                    readLaterOnly = readLaterOnly,
                    mediaOnly = mediaOnly,
                    verifiedOnly = verifiedOnly,
                    articlesOnly = articlesOnly
                )
            }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomain() }
        }
    }

    fun getFollowedUsersFeedPaged(adFree: Boolean = true): Flow<PagingData<PostItem>> =
        getPagedFeedPosts(
            feedTab = FeedTab.FOLLOWING,
            filter = FeedFilter.ALL,
            adFree = adFree,
            offlineModeActive = false
        )

    fun observePostsByAuthor(authorId: String): Flow<List<PostItem>> =
        dao.observePostsByAuthor(authorId).map { entities -> entities.map { it.toDomain() } }

    /** Profile used for authorship stamps; falls back to the seeded persona. */
    suspend fun currentProfile(): UserProfile = dao.getUserProfileOnce()?.toDomain() ?: SeedData.initialProfile

    suspend fun getSuggestedUserById(userId: String): SuggestedUser? =
        dao.getSuggestedUserById(userId)?.toDomain()

    val readLaterPosts: Flow<List<PostItem>> = dao.observeReadLaterPosts().map { entities ->
        entities.map { it.toDomain() }
    }

    val bookmarkFolders: Flow<List<BookmarkFolder>> = combine(
        dao.observeBookmarkFolders(),
        dao.observeAllPosts(),
        dao.observeTypedBookmarks()
    ) { folders, posts, typed ->
        val bookmarkedPosts = posts.filter { it.isBookmarked }
        folders.map { folder ->
            val postCount = bookmarkedPosts.count { it.bookmarkFolderId == folder.id }
            val extraTypedCount = typed.count {
                it.folderId == folder.id && (it.postId == null || bookmarkedPosts.none { p -> p.id == it.postId })
            }
            folder.toDomain(count = postCount + extraTypedCount)
        }
    }

    val userProfile: Flow<UserProfile> = dao.observeUserProfile().map { entity ->
        entity?.toDomain() ?: SeedData.initialProfile
    }

    val trends: Flow<List<TrendItem>> = dao.observeTrends().map { entities ->
        entities.map { it.toDomain() }
    }

    val suggestedUsers: Flow<List<SuggestedUser>> = dao.observeSuggestedUsers().map { entities ->
        entities.map { it.toDomain() }
    }

    /**
     * Seeds demo content (fake persona, posts, trends, …) on first launch — debug builds only.
     * Release installs start with an empty database; every read path must tolerate empty state.
     * (Unit tests run against the debug variant, so seeded test fixtures still work.)
     */
    suspend fun ensureSeeded() {
        if (seeded) return
        seedMutex.withLock {
            if (seeded) return
            if (!BuildConfig.DEBUG) {
                seeded = true
                return@withLock
            }
            if (dao.getPostCount() == 0) {
                dao.insertUserProfile(SeedData.initialProfile.toEntity())
                dao.insertFolders(SeedData.initialFolders.map { it.toEntity() })
                dao.insertTrends(SeedData.initialTrends.map { it.toEntity() })
                dao.insertPosts(SeedData.initialPosts.map { it.toEntity() })
                dao.insertSuggestedUsers(SeedData.initialSuggestedUsers.map { it.toEntity() })
            }
            if (dao.getTypedBookmarkCount() == 0) {
                dao.insertTypedBookmarks(SeedData.initialTypedBookmarks.map { it.toEntity() })
            }
            if (dao.getNotificationCount() == 0) {
                dao.insertNotifications(SeedData.initialNotifications.map { it.toEntity() })
            }
            if (dao.getConversationCount() == 0) {
                dao.insertConversations(SeedData.initialConversations.map { it.toEntity() })
                dao.insertDirectMessages(SeedData.initialDirectMessages.map { it.toEntity() })
            }
            if (dao.getXListCount() == 0) {
                dao.insertXLists(SeedData.initialXLists.map { it.toEntity() })
            }
            if (dao.getCommunityCount() == 0) {
                dao.insertCommunities(SeedData.initialCommunities.map { it.toEntity() })
            }
            if (dao.getMediaLibraryCount() == 0) {
                dao.insertPlaylists(SeedData.initialPlaylists.map { it.toEntity() })
                dao.insertMediaItems(SeedData.initialMediaLibrary.map { it.toEntity() })
            }
            if (dao.getSavedPageCount() == 0) {
                dao.insertSavedPages(SeedData.initialSavedPages.map { it.toEntity() })
            }
            if (dao.getBrowserHistoryCount() == 0) {
                dao.insertBrowserHistoryItems(SeedData.initialBrowserHistory.map { it.toEntity() })
            }
            seeded = true
        }
    }

    suspend fun publishPost(
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
        val profile = dao.getUserProfileOnce()?.toDomain() ?: SeedData.initialProfile
        val cleanPoll = if (pollOptions.count { it.isNotBlank() } >= 2) {
            PollData(
                question = "Community Poll",
                options = pollOptions.filter { it.isNotBlank() }.mapIndexed { idx, label ->
                    PollOption(id = "opt_${idx + 1}", label = label.trim(), votes = 1)
                },
                selectedOptionId = null,
                durationLabel = "24h remaining"
            )
        } else null

        val newPost = PostItem(
            id = "post_${UUID.randomUUID()}",
            authorId = "current_user",
            authorName = profile.name,
            authorHandle = profile.handle,
            authorBio = profile.bio,
            authorBadge = profile.badge,
            authorAvatarColorHex = 0xFF1D9BF0,
            isCurrentUser = true,
            isFollowedAuthor = true,
            content = content.trim(),
            articleTitle = articleTitle?.trim()?.takeIf { it.isNotEmpty() },
            articleSubtitle = articleSubtitle?.trim()?.takeIf { it.isNotEmpty() },
            createdAtMillis = System.currentTimeMillis(),
            editedAtMillis = null,
            likes = 1,
            reposts = 0,
            replies = 0,
            views = 12,
            isLiked = false,
            isReposted = false,
            isBookmarked = false,
            mediaResKey = mediaResKey,
            mediaUri = mediaUri,
            isLongForm = isLongForm,
            isHighlight = false,
            isSponsored = false,
            audience = audience,
            parentPostId = parentPostId,
            poll = cleanPoll
        )
        dao.insertPost(newPost.toEntity())

        if (parentPostId != null) {
            dao.getPostById(parentPostId)?.let { parent ->
                dao.updatePost(parent.copy(replies = parent.replies + 1))
            }
        }
    }

    suspend fun editPost(postId: String, newContent: String, reason: String) {
        val existing = dao.getPostById(postId)?.toDomain() ?: return
        val trimmed = newContent.trim()
        if (trimmed.isEmpty() || trimmed == existing.content) return
        val now = System.currentTimeMillis()
        val previousSnapshot = PostRevision(
            content = existing.content,
            editedAtMillis = existing.editedAtMillis ?: existing.createdAtMillis,
            reason = reason.ifBlank { "Post revision" }
        )
        val updatedRevisions = existing.revisions + previousSnapshot
        dao.updatePost(
            existing.copy(
                content = trimmed,
                editedAtMillis = now,
                revisions = updatedRevisions
            ).toEntity()
        )
    }

    suspend fun deletePost(postId: String) {
        dao.deletePostById(postId)
        dao.deleteTypedBookmarkByPostId(postId)
    }

    suspend fun toggleLike(postId: String) {
        val post = dao.getPostById(postId) ?: return
        val nextLiked = !post.isLiked
        dao.updatePost(
            post.copy(
                isLiked = nextLiked,
                likes = if (nextLiked) post.likes + 1 else (post.likes - 1).coerceAtLeast(0)
            )
        )
    }

    suspend fun toggleRepost(postId: String) {
        val post = dao.getPostById(postId) ?: return
        val nextReposted = !post.isReposted
        dao.updatePost(
            post.copy(
                isReposted = nextReposted,
                reposts = if (nextReposted) post.reposts + 1 else (post.reposts - 1).coerceAtLeast(0)
            )
        )
    }

    suspend fun toggleHighlight(postId: String) {
        val post = dao.getPostById(postId) ?: return
        dao.updatePost(post.copy(isHighlight = !post.isHighlight))
    }

    suspend fun votePoll(postId: String, optionId: String) {
        val post = dao.getPostById(postId)?.toDomain() ?: return
        val poll = post.poll ?: return
        if (poll.selectedOptionId == optionId) return
        val updatedOptions = poll.options.map { opt ->
            when {
                opt.id == optionId -> opt.copy(votes = opt.votes + 1)
                opt.id == poll.selectedOptionId -> opt.copy(votes = (opt.votes - 1).coerceAtLeast(0))
                else -> opt
            }
        }
        dao.updatePost(
            post.copy(poll = poll.copy(options = updatedOptions, selectedOptionId = optionId)).toEntity()
        )
    }

    suspend fun assignBookmark(postId: String, folderId: String?): TypedBookmarkItem? {
        val post = dao.getPostById(postId) ?: return null
        dao.updatePost(
            post.copy(
                isBookmarked = true,
                bookmarkFolderId = folderId
            )
        )
        val now = System.currentTimeMillis()
        val existingTb = dao.getTypedBookmarkById("tb_$postId")
        val item = TypedBookmarkItem(
            id = "tb_$postId",
            itemType = BookmarkItemType.POST,
            title = post.articleTitle ?: "${post.authorName} on X",
            content = post.content.take(500),
            authorName = post.authorName,
            authorHandle = post.authorHandle,
            mediaUrl = post.mediaUri ?: post.mediaResKey?.let { "drawable://$it" },
            linkUrl = null,
            folderId = folderId,
            postId = post.id,
            createdAtMillis = existingTb?.createdAtMillis ?: now,
            updatedAtMillis = now
        )
        dao.insertTypedBookmark(
            item.toEntity(pendingSync = true, ownerUid = existingTb?.ownerUid ?: ownerStamp())
        )
        // Re-bookmarking something previously deleted locally clears the tombstone so it can sync again.
        dao.clearPendingRemoteDelete(item.id)
        return item
    }

    suspend fun removeBookmark(postId: String) {
        val post = dao.getPostById(postId) ?: return
        val existing = dao.getTypedBookmarkByPostId(postId)
        dao.updatePost(
            post.copy(
                isBookmarked = false,
                bookmarkFolderId = null
            )
        )
        dao.deleteTypedBookmarkByPostId(postId)
        // Tombstone so the next sync deletes the remote doc instead of resurrecting it locally.
        // Owned by whoever owned the row: a guest-mode delete must not touch a signed-in account.
        // Rows that were never synced (seed/demo, updatedAtMillis = 0, not pending) have no remote
        // counterpart, so they are not tombstoned — otherwise a demo delete could delete a real
        // document that happens to share the generated id.
        if (existing == null || existing.pendingSync || existing.updatedAtMillis > 0L) {
            dao.insertPendingRemoteDelete(
                PendingRemoteDeleteEntity(
                    id = "tb_$postId",
                    kind = PendingRemoteDeleteKind.BOOKMARK,
                    createdAtMillis = System.currentTimeMillis(),
                    ownerUid = existing?.ownerUid ?: ownerStamp()
                )
            )
        }
    }

    suspend fun addTypedBookmark(
        itemType: BookmarkItemType,
        title: String,
        content: String,
        authorName: String,
        authorHandle: String,
        mediaUrl: String? = null,
        linkUrl: String? = null,
        folderId: String? = null,
        customId: String? = null
    ): TypedBookmarkItem {
        val now = System.currentTimeMillis()
        val item = TypedBookmarkItem(
            id = customId ?: "tb_${UUID.randomUUID().toString().replace("-", "_").take(16)}",
            itemType = itemType,
            title = title.trim().ifBlank { "${itemType.label} Bookmark" },
            content = content.trim().ifBlank { title.trim().ifBlank { "Saved item" } },
            authorName = authorName.ifBlank { "XTRA User" },
            authorHandle = authorHandle.ifBlank { "@xtra" },
            mediaUrl = mediaUrl?.takeIf { it.isNotBlank() },
            linkUrl = linkUrl?.takeIf { it.isNotBlank() },
            folderId = folderId,
            postId = null,
            createdAtMillis = now,
            updatedAtMillis = now
        )
        dao.insertTypedBookmark(item.toEntity(pendingSync = true, ownerUid = ownerStamp()))
        dao.clearPendingRemoteDelete(item.id)
        return item
    }

    /** Claims unowned (guest) rows for [ownerUid] before a push so the user's local data syncs. */
    suspend fun claimUnownedRows(ownerUid: String) {
        if (ownerUid.isBlank()) return
        dao.claimUnownedTypedBookmarks(ownerUid)
        dao.claimUnownedFolders(ownerUid)
        dao.claimUnownedPendingRemoteDeletes(ownerUid)
    }

    /**
     * Reconciles a *server* snapshot of [ownerUid]'s bookmarks into Room.
     *
     * Callers must only pass authoritative snapshots (not cache-sourced ones) — an empty list
     * here deletes every synced local row for that account. Rows owned by another account are
     * never touched.
     */
    suspend fun mergeRemoteBookmarks(ownerUid: String, remoteItems: List<TypedBookmarkItem>) {
        if (ownerUid.isBlank()) return // guest mode has no remote authority
        if (remoteItems.isEmpty()) {
            // Authoritative empty snapshot: drop synced local rows that no longer exist remotely.
            dao.deleteAllSyncedTypedBookmarks(ownerUid)
            return
        }
        for (item in remoteItems) {
            // Never resurrect an item we deleted locally and haven't replayed remotely yet.
            if (dao.isPendingRemoteDelete(item.id, PendingRemoteDeleteKind.BOOKMARK)) continue
            val existing = dao.getTypedBookmarkById(item.id)
            if (existing?.pendingSync == true) continue // local unsynced edits win; pushed on next sync
            if (existing?.ownerUid != null && existing.ownerUid != ownerUid) continue // other account
            val effectiveUpdated = if (item.updatedAtMillis > 0L) item.updatedAtMillis else item.createdAtMillis
            dao.insertTypedBookmark(
                item.toEntity(updatedAtMillis = effectiveUpdated, pendingSync = false, ownerUid = ownerUid)
            )
        }
        val remoteIds = remoteItems.map { it.id }.toHashSet()
        val staleIds = dao.getSyncedTypedBookmarkIds(ownerUid).filterNot { it in remoteIds }
        deleteInChunks(staleIds) { chunk -> dao.deleteTypedBookmarksByIds(ownerUid, chunk) }
    }

    suspend fun getPendingSyncBookmarks(ownerUid: String): List<TypedBookmarkItem> {
        if (ownerUid.isBlank()) return emptyList()
        return dao.getPendingSyncTypedBookmarks(ownerUid).map { it.toDomain() }
    }

    suspend fun markBookmarksSynced(ownerUid: String, ids: List<String>) {
        if (ownerUid.isNotBlank() && ids.isNotEmpty()) dao.clearPendingTypedBookmarks(ownerUid, ids)
    }

    suspend fun getPendingRemoteDeletes(ownerUid: String): List<PendingRemoteDeleteEntity> {
        if (ownerUid.isBlank()) return emptyList()
        return dao.getPendingRemoteDeletes(ownerUid)
    }

    suspend fun clearPendingRemoteDelete(id: String) = dao.clearPendingRemoteDelete(id)

    /**
     * Same contract as [mergeRemoteBookmarks]: authoritative server snapshots only. Remote
     * timestamps are preserved (they used to be replaced with `now`, which made folder
     * last-writer-wins impossible).
     */
    suspend fun mergeRemoteFolders(ownerUid: String, remoteFolders: List<BookmarkFolder>) {
        if (ownerUid.isBlank()) return
        if (remoteFolders.isEmpty()) {
            dao.deleteAllSyncedFolders(ownerUid)
            return
        }
        val now = System.currentTimeMillis()
        for (folder in remoteFolders) {
            if (dao.isPendingRemoteDelete(folder.id, PendingRemoteDeleteKind.FOLDER)) continue
            val existing = dao.getBookmarkFolderById(folder.id)
            if (existing?.pendingSync == true) continue
            if (existing?.ownerUid != null && existing.ownerUid != ownerUid) continue
            val updatedAt = folder.updatedAtMillis.takeIf { it > 0L } ?: now
            dao.insertFolder(
                folder.toEntity(ownerUid = ownerUid, pendingSync = false, updatedAtMillis = updatedAt)
            )
        }
        val remoteIds = remoteFolders.map { it.id }.toHashSet()
        val staleIds = dao.getSyncedFolderIds(ownerUid).filterNot { it in remoteIds }
        deleteInChunks(staleIds) { chunk -> dao.deleteFoldersByIds(ownerUid, chunk) }
    }

    suspend fun getPendingSyncFolders(ownerUid: String): List<BookmarkFolder> {
        if (ownerUid.isBlank()) return emptyList()
        return dao.getPendingSyncFolders(ownerUid).map { it.toDomain() }
    }

    suspend fun markFoldersSynced(ownerUid: String, ids: List<String>) {
        if (ownerUid.isNotBlank() && ids.isNotEmpty()) dao.clearPendingFolders(ownerUid, ids)
    }

    /**
     * True when [rowOwnerUid]'s remote copy may be written through [uid].
     *
     * Unowned rows (`null`) are device-local and are adopted on sign-in; a row owned by a
     * different account must never be pushed, updated or deleted on this account's behalf.
     */
    fun isRemoteCopyOwnedBy(rowOwnerUid: String?, uid: String?): Boolean =
        !uid.isNullOrBlank() && (rowOwnerUid == null || rowOwnerUid == uid)

    /** Owner of a vault row, read *before* deleting it so the caller can scope the remote delete. */
    suspend fun typedBookmarkOwner(bookmarkId: String): String? =
        dao.getTypedBookmarkById(bookmarkId)?.ownerUid

    /** Owner of the vault row linked to [postId], if any. */
    suspend fun postBookmarkOwner(postId: String): String? =
        dao.getTypedBookmarkByPostId(postId)?.ownerUid

    /** Owner of a folder row, read *before* deleting it. */
    suspend fun folderOwner(folderId: String): String? =
        dao.getBookmarkFolderById(folderId)?.ownerUid

    /** Keeps every SQL `IN (...)` under SQLite's bound-parameter limit on large libraries. */
    private suspend fun deleteInChunks(ids: List<String>, delete: suspend (List<String>) -> Unit) {
        ids.chunked(SQL_IN_CHUNK_SIZE).forEach { chunk -> delete(chunk) }
    }

    suspend fun assignTypedBookmarkFolder(bookmarkId: String, folderId: String?) {
        dao.updateTypedBookmarkFolder(bookmarkId, folderId)
        dao.markTypedBookmarkPendingSync(bookmarkId, System.currentTimeMillis())
    }

    suspend fun deleteTypedBookmark(bookmarkId: String, linkedPostId: String?) {
        val existing = dao.getTypedBookmarkById(bookmarkId)
        dao.deleteTypedBookmarkById(bookmarkId)
        // Tombstone carries the row's owner so a guest delete is never replayed into an account.
        if (existing == null || existing.pendingSync || existing.updatedAtMillis > 0L) {
            dao.insertPendingRemoteDelete(
                PendingRemoteDeleteEntity(
                    id = bookmarkId,
                    kind = PendingRemoteDeleteKind.BOOKMARK,
                    createdAtMillis = System.currentTimeMillis(),
                    ownerUid = existing?.ownerUid ?: ownerStamp()
                )
            )
        }
        if (linkedPostId != null) {
            dao.getPostById(linkedPostId)?.let { post ->
                dao.updatePost(post.copy(isBookmarked = false, bookmarkFolderId = null))
            }
        }
    }

    suspend fun createBookmarkFolder(name: String, description: String, colorHex: Long, iconKey: String): String {
        val id = "folder_${UUID.randomUUID().toString().replace("-", "_").take(12)}"
        dao.insertFolder(
            BookmarkFolderEntity(
                id = id,
                name = name.trim(),
                description = description.trim(),
                colorHex = colorHex,
                iconKey = iconKey,
                updatedAtMillis = System.currentTimeMillis(),
                pendingSync = true,
                ownerUid = ownerStamp()
            )
        )
        return id
    }

    suspend fun deleteBookmarkFolder(folderId: String) {
        val existing = dao.getBookmarkFolderById(folderId)
        dao.clearFolderFromPosts(folderId)
        dao.clearFolderFromTypedBookmarks(folderId, System.currentTimeMillis())
        dao.deleteFolderById(folderId)
        if (existing == null || existing.pendingSync || existing.updatedAtMillis > 0L) {
            dao.insertPendingRemoteDelete(
                PendingRemoteDeleteEntity(
                    id = folderId,
                    kind = PendingRemoteDeleteKind.FOLDER,
                    createdAtMillis = System.currentTimeMillis(),
                    ownerUid = existing?.ownerUid ?: ownerStamp()
                )
            )
        }
    }

    suspend fun updateUserProfile(
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
        val current = dao.getUserProfileOnce()?.toDomain() ?: SeedData.initialProfile
        val trimmedHandle = handle.trim().removePrefix("@")
        val normalizedHandle = if (trimmedHandle.isEmpty()) current.handle else "@$trimmedHandle"
        val updated = current.copy(
            name = name.trim().ifEmpty { current.name },
            handle = normalizedHandle,
            bio = bio.trim(),
            badge = badge,
            undoDelaySeconds = undoDelaySeconds,
            adFreeEnabled = adFreeEnabled,
            extendedPostsEnabled = extendedPostsEnabled,
            showEditHistoryLinks = showEditHistoryLinks,
            autoExpandThreads = autoExpandThreads
        )
        dao.insertUserProfile(updated.toEntity())
        dao.syncCurrentUserPostsProfile(updated.name, updated.handle, updated.bio, updated.badge)
    }

    suspend fun switchActiveProfileDetails(
        name: String,
        handle: String,
        bio: String,
        badge: VerificationBadge
    ) {
        val current = dao.getUserProfileOnce()?.toDomain() ?: SeedData.initialProfile
        val updated = current.copy(
            name = name.trim().ifBlank { current.name },
            handle = handle.trim().ifBlank { current.handle },
            bio = bio.trim().ifBlank { current.bio },
            badge = badge
        )
        dao.insertUserProfile(updated.toEntity())
        dao.syncCurrentUserPostsProfile(updated.name, updated.handle, updated.bio, updated.badge)
    }

    suspend fun setAdFreeMode(enabled: Boolean) {
        val current = dao.getUserProfileOnce()?.toDomain() ?: SeedData.initialProfile
        dao.insertUserProfile(current.copy(adFreeEnabled = enabled).toEntity())
    }

    suspend fun updateAppSettings(
        undoDelaySeconds: Int? = null,
        adFreeEnabled: Boolean? = null,
        extendedPostsEnabled: Boolean? = null,
        showEditHistoryLinks: Boolean? = null,
        autoExpandThreads: Boolean? = null
    ) {
        val current = dao.getUserProfileOnce()?.toDomain() ?: SeedData.initialProfile
        dao.insertUserProfile(
            current.copy(
                undoDelaySeconds = undoDelaySeconds ?: current.undoDelaySeconds,
                adFreeEnabled = adFreeEnabled ?: current.adFreeEnabled,
                extendedPostsEnabled = extendedPostsEnabled ?: current.extendedPostsEnabled,
                showEditHistoryLinks = showEditHistoryLinks ?: current.showEditHistoryLinks,
                autoExpandThreads = autoExpandThreads ?: current.autoExpandThreads
            ).toEntity()
        )
    }

    suspend fun setSuggestedUserFollow(userId: String, currentlyFollowing: Boolean) {
        val next = !currentlyFollowing
        dao.updateSuggestedUserFollow(userId, next)
        dao.updateFollowStatusForAuthor(userId, next)
        val profile = dao.getUserProfileOnce()?.toDomain() ?: SeedData.initialProfile
        val nextFollowingCount = if (next) profile.followingCount + 1 else (profile.followingCount - 1).coerceAtLeast(0)
        dao.insertUserProfile(profile.copy(followingCount = nextFollowingCount).toEntity())
    }

    suspend fun toggleSaveForOffline(postId: String): Boolean {
        val post = dao.getPostById(postId) ?: return false
        val nextSaved = !post.isSavedForOffline
        val cachedAt = if (nextSaved) System.currentTimeMillis() else null
        dao.updatePostOfflineCache(postId, nextSaved, cachedAt)
        return nextSaved
    }

    suspend fun toggleOfflineRead(postId: String): Boolean {
        val post = dao.getPostById(postId) ?: return false
        val nextRead = !post.isOfflineRead
        dao.updatePostOfflineRead(postId, nextRead)
        return nextRead
    }

    suspend fun cacheAllThreadsAndArticlesForOffline() {
        dao.cacheAllThreadsAndArticles(System.currentTimeMillis())
    }

    suspend fun clearOfflineCache() {
        dao.clearOfflineCache()
    }

    suspend fun getOfflineSavedCount(): Int = dao.getOfflineSavedCount()

    // Social Actions
    suspend fun markNotificationRead(id: String) = dao.markNotificationRead(id)
    suspend fun markAllNotificationsRead() = dao.markAllNotificationsRead()

    suspend fun sendDirectMessage(conversationId: String, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        val now = System.currentTimeMillis()
        dao.insertDirectMessage(
            DirectMessageEntity(
                id = "dm_${UUID.randomUUID()}",
                conversationId = conversationId,
                senderIsCurrentUser = true,
                text = trimmed,
                sentAtMillis = now
            )
        )
        dao.clearConversationUnread(conversationId)
    }

    suspend fun updateConversationPreview(conversation: MessageConversation, newText: String) {
        dao.insertConversation(
            conversation.copy(
                lastMessage = newText,
                updatedAtMillis = System.currentTimeMillis(),
                unreadCount = 0
            ).toEntity()
        )
    }

    suspend fun clearConversationUnread(conversationId: String) {
        dao.clearConversationUnread(conversationId)
    }

    suspend fun createXList(name: String, description: String, isPrivate: Boolean) {
        if (name.isBlank()) return
        dao.insertXList(
            XListEntity(
                id = "list_${UUID.randomUUID()}",
                name = name.trim(),
                description = description.trim().ifBlank { "Curated creator list" },
                memberCount = 1,
                subscriberCount = 1,
                isJoined = true,
                isPrivate = isPrivate
            )
        )
    }

    suspend fun toggleXListJoined(listItem: XListItem) {
        dao.updateXListJoined(listItem.id, !listItem.isJoined)
    }

    suspend fun toggleCommunityJoined(community: CommunityItem) {
        val nextJoined = !community.isJoined
        dao.updateCommunityMembership(community.id, nextJoined, if (nextJoined) 1 else -1)
    }

    // Media Center
    suspend fun toggleMediaFavorite(media: MediaLibraryItem) {
        dao.updateMediaFavorite(media.id, !media.isFavorite)
    }

    suspend fun recordMediaPlayed(mediaId: String) {
        dao.recordMediaPlayed(mediaId, System.currentTimeMillis())
    }

    suspend fun addMediaItemToLibrary(
        title: String,
        creator: String,
        kind: LibraryMediaKind,
        sourceUrl: String,
        resolution: String
    ) {
        dao.insertMediaItem(
            MediaItemEntity(
                id = "media_${UUID.randomUUID()}",
                title = title.trim().ifBlank { "Custom Media" },
                creator = creator.trim().ifBlank { "XTRA User" },
                mediaKind = kind.name,
                sourceUrl = sourceUrl.trim(),
                durationLabel = "Stream",
                resolution = resolution,
                sizeLabel = "Remote",
                isFavorite = false,
                lastPlayedMillis = null,
                playCount = 0,
                playlistId = null
            )
        )
    }

    suspend fun createPlaylist(name: String, description: String) {
        if (name.isBlank()) return
        dao.insertPlaylist(
            PlaylistEntity(
                id = "pl_${UUID.randomUUID()}",
                name = name.trim(),
                description = description.trim().ifBlank { "Custom XTRA playlist" },
                createdAtMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun deletePlaylist(playlistId: String) {
        // Media items keep a dangling playlistId otherwise; detach them first.
        dao.clearPlaylistFromMedia(playlistId)
        dao.deletePlaylistById(playlistId)
    }

    suspend fun assignMediaToPlaylist(mediaId: String, playlistId: String?) {
        dao.assignMediaToPlaylist(mediaId, playlistId)
    }

    // Application Tools & Support
    suspend fun saveWebPage(title: String, url: String, summary: String, addToReadingList: Boolean) {
        if (url.isBlank()) return
        dao.insertSavedPage(
            SavedPageEntity(
                id = "page_${UUID.randomUUID()}",
                title = title.trim().ifBlank { url.trim() },
                url = url.trim(),
                summary = summary.trim().ifBlank { "Saved via XTRA browser tools" },
                savedAtMillis = System.currentTimeMillis(),
                isInReadingList = addToReadingList,
                isCompleted = false
            )
        )
    }

    suspend fun togglePageReadingList(page: SavedPageItem) {
        dao.updatePageReadingList(page.id, !page.isInReadingList)
    }

    suspend fun togglePageCompleted(page: SavedPageItem) {
        dao.updatePageCompleted(page.id, !page.isCompleted)
    }

    suspend fun deleteSavedPage(pageId: String) {
        dao.deleteSavedPageById(pageId)
    }

    suspend fun recordBrowserVisit(title: String, url: String) {
        if (url.isBlank()) return
        dao.insertBrowserHistoryItem(
            BrowserHistoryEntity(
                id = "hist_${UUID.randomUUID()}",
                title = title.trim().ifBlank { url.trim() },
                url = url.trim(),
                visitedAtMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteBrowserHistoryItem(id: String) = dao.deleteBrowserHistoryById(id)
    suspend fun clearBrowserHistory() = dao.clearBrowserHistory()

    suspend fun submitFeedback(category: String, message: String) {
        if (message.isBlank()) return
        dao.insertFeedback(
            FeedbackEntity(
                id = "fb_${UUID.randomUUID()}",
                category = category,
                message = message.trim(),
                submittedAtMillis = System.currentTimeMillis()
            )
        )
    }

    companion object {
        /**
         * SQLite's historical bound-parameter ceiling is 999; stay well under it so a
         * reconcile of a large bookmark library cannot throw "too many SQL variables".
         */
        private const val SQL_IN_CHUNK_SIZE = 400

        @Volatile
        private var INSTANCE: XProRepository? = null

        fun getInstance(context: Context): XProRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: XProRepository(XProDatabase.getInstance(context).xProDao()).also {
                    INSTANCE = it
                }
            }
        }

        @androidx.annotation.VisibleForTesting
        internal fun resetInstanceForTesting() {
            synchronized(this) {
                INSTANCE = null
            }
        }
    }
}
