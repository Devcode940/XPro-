package com.example.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.VerificationBadge
import kotlinx.coroutines.flow.Flow

@Dao
interface XProDao {

    @Query("SELECT * FROM posts ORDER BY createdAtMillis DESC")
    fun observeAllPosts(): Flow<List<PostEntity>>

    @Query(
        """
        SELECT * FROM posts
        WHERE parentPostId IS NULL
          AND (:excludeSponsored = 0 OR isSponsored = 0)
          AND (:followingOnly = 0 OR isFollowedAuthor = 1 OR isCurrentUser = 1)
          AND (:readLaterOnly = 0 OR isSavedForOffline = 1)
          AND (:mediaOnly = 0 OR mediaResKey IS NOT NULL OR mediaUri IS NOT NULL)
          AND (:verifiedOnly = 0 OR authorBadge != 'NONE')
          AND (:articlesOnly = 0 OR isLongForm = 1)
        ORDER BY createdAtMillis DESC
        """
    )
    fun pagingFeedPosts(
        excludeSponsored: Boolean,
        followingOnly: Boolean,
        readLaterOnly: Boolean,
        mediaOnly: Boolean,
        verifiedOnly: Boolean,
        articlesOnly: Boolean
    ): PagingSource<Int, PostEntity>

    @Query("SELECT * FROM posts WHERE authorId = :authorId ORDER BY createdAtMillis DESC")
    fun observePostsByAuthor(authorId: String): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE isSavedForOffline = 1 ORDER BY offlineCachedAtMillis DESC")
    fun observeReadLaterPosts(): Flow<List<PostEntity>>

    @Query("SELECT COUNT(*) FROM posts")
    suspend fun getPostCount(): Int

    @Query("SELECT COUNT(*) FROM posts WHERE isSavedForOffline = 1")
    fun observeOfflineCachedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM posts WHERE isSavedForOffline = 1 AND isOfflineRead = 0")
    fun observeOfflineUnreadCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM posts WHERE isSavedForOffline = 1")
    suspend fun getOfflineSavedCount(): Int

    @Query("SELECT * FROM posts WHERE id = :postId LIMIT 1")
    suspend fun getPostById(postId: String): PostEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<PostEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: PostEntity)

    @Update
    suspend fun updatePost(post: PostEntity)

    @Query("UPDATE posts SET isSavedForOffline = :saved, offlineCachedAtMillis = :cachedAt WHERE id = :postId")
    suspend fun updatePostOfflineCache(postId: String, saved: Boolean, cachedAt: Long?)

    @Query("UPDATE posts SET isOfflineRead = :isRead WHERE id = :postId")
    suspend fun updatePostOfflineRead(postId: String, isRead: Boolean)

    @Query("UPDATE posts SET isSavedForOffline = 1, offlineCachedAtMillis = :cachedAt WHERE isLongForm = 1 OR threadPosts != '[]'")
    suspend fun cacheAllThreadsAndArticles(cachedAt: Long)

    @Query("UPDATE posts SET isSavedForOffline = 0, offlineCachedAtMillis = NULL, isOfflineRead = 0")
    suspend fun clearOfflineCache()

    @Query("DELETE FROM posts WHERE id = :postId")
    suspend fun deletePostById(postId: String)

    @Query(
        "UPDATE posts SET authorName = :name, authorHandle = :handle, authorBio = :bio, authorBadge = :badge WHERE isCurrentUser = 1"
    )
    suspend fun syncCurrentUserPostsProfile(
        name: String,
        handle: String,
        bio: String,
        badge: VerificationBadge
    )

    @Query("UPDATE posts SET isFollowedAuthor = :isFollowing WHERE authorId = :authorId")
    suspend fun updateFollowStatusForAuthor(authorId: String, isFollowing: Boolean)

    // Remote-delete tombstones (offline deletes replayed on next sync)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingRemoteDelete(item: PendingRemoteDeleteEntity)

    @Query("SELECT * FROM pending_remote_deletes ORDER BY createdAtMillis ASC")
    suspend fun getPendingRemoteDeletes(): List<PendingRemoteDeleteEntity>

    @Query("DELETE FROM pending_remote_deletes WHERE id = :id")
    suspend fun clearPendingRemoteDelete(id: String)

    @Query("SELECT EXISTS(SELECT 1 FROM pending_remote_deletes WHERE id = :id AND kind = :kind)")
    suspend fun isPendingRemoteDelete(id: String, kind: String): Boolean

    @Query("SELECT * FROM bookmark_folders WHERE id = :folderId LIMIT 1")
    suspend fun getBookmarkFolderById(folderId: String): BookmarkFolderEntity?

    @Query("SELECT * FROM bookmark_folders WHERE pendingSync = 1")
    suspend fun getPendingSyncFolders(): List<BookmarkFolderEntity>

    @Query("UPDATE bookmark_folders SET pendingSync = 0 WHERE id IN (:ids)")
    suspend fun clearPendingFolders(ids: List<String>)

    @Query("DELETE FROM bookmark_folders WHERE pendingSync = 0 AND updatedAtMillis > 0 AND id NOT IN (:remoteIds)")
    suspend fun deleteSyncedFoldersNotIn(remoteIds: List<String>)

    @Query("DELETE FROM bookmark_folders WHERE pendingSync = 0 AND updatedAtMillis > 0")
    suspend fun deleteAllSyncedFolders()

    @Query("SELECT * FROM bookmark_folders ORDER BY name ASC")
    fun observeBookmarkFolders(): Flow<List<BookmarkFolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolders(folders: List<BookmarkFolderEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: BookmarkFolderEntity)

    @Query("DELETE FROM bookmark_folders WHERE id = :folderId")
    suspend fun deleteFolderById(folderId: String)

    @Query("UPDATE posts SET bookmarkFolderId = NULL WHERE bookmarkFolderId = :folderId")
    suspend fun clearFolderFromPosts(folderId: String)

    // Typed Bookmarks Vault
    @Query("SELECT * FROM bookmarks_vault WHERE id = :bookmarkId LIMIT 1")
    suspend fun getTypedBookmarkById(bookmarkId: String): TypedBookmarkEntity?

    @Query("SELECT * FROM bookmarks_vault WHERE pendingSync = 1")
    suspend fun getPendingSyncTypedBookmarks(): List<TypedBookmarkEntity>

    @Query("UPDATE bookmarks_vault SET pendingSync = 0 WHERE id IN (:ids)")
    suspend fun clearPendingTypedBookmarks(ids: List<String>)

    @Query("DELETE FROM bookmarks_vault WHERE pendingSync = 0 AND updatedAtMillis > 0 AND id NOT IN (:remoteIds)")
    suspend fun deleteSyncedTypedBookmarksNotIn(remoteIds: List<String>)

    @Query("DELETE FROM bookmarks_vault WHERE pendingSync = 0 AND updatedAtMillis > 0")
    suspend fun deleteAllSyncedTypedBookmarks()

    @Query("SELECT * FROM bookmarks_vault ORDER BY createdAtMillis DESC")
    fun observeTypedBookmarks(): Flow<List<TypedBookmarkEntity>>

    @Query("SELECT COUNT(*) FROM bookmarks_vault")
    suspend fun getTypedBookmarkCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTypedBookmarks(items: List<TypedBookmarkEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTypedBookmark(item: TypedBookmarkEntity)

    @Query("UPDATE bookmarks_vault SET folderId = :folderId WHERE id = :bookmarkId")
    suspend fun updateTypedBookmarkFolder(bookmarkId: String, folderId: String?)

    @Query("UPDATE bookmarks_vault SET pendingSync = 1, updatedAtMillis = :updatedAtMillis WHERE id = :bookmarkId")
    suspend fun markTypedBookmarkPendingSync(bookmarkId: String, updatedAtMillis: Long)

    @Query("UPDATE bookmarks_vault SET folderId = NULL, pendingSync = 1, updatedAtMillis = :updatedAtMillis WHERE folderId = :folderId")
    suspend fun clearFolderFromTypedBookmarks(folderId: String, updatedAtMillis: Long)

    @Query("DELETE FROM bookmarks_vault WHERE id = :bookmarkId")
    suspend fun deleteTypedBookmarkById(bookmarkId: String)

    @Query("DELETE FROM bookmarks_vault WHERE postId = :postId")
    suspend fun deleteTypedBookmarkByPostId(postId: String)

    @Query("SELECT * FROM user_profile WHERE id = 'current_user' LIMIT 1")
    fun observeUserProfile(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = 'current_user' LIMIT 1")
    suspend fun getUserProfileOnce(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(profile: UserProfileEntity)

    @Query("SELECT * FROM trends")
    fun observeTrends(): Flow<List<TrendEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrends(trends: List<TrendEntity>)

    @Query("SELECT * FROM suggested_users")
    fun observeSuggestedUsers(): Flow<List<SuggestedUserEntity>>

    @Query("SELECT * FROM suggested_users WHERE id = :userId LIMIT 1")
    suspend fun getSuggestedUserById(userId: String): SuggestedUserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSuggestedUsers(users: List<SuggestedUserEntity>)

    @Query("UPDATE suggested_users SET isFollowing = :isFollowing WHERE id = :userId")
    suspend fun updateSuggestedUserFollow(userId: String, isFollowing: Boolean)

    // Notifications
    @Query("SELECT * FROM notifications ORDER BY createdAtMillis DESC")
    fun observeNotifications(): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications")
    suspend fun getNotificationCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(items: List<NotificationEntity>)

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markNotificationRead(id: String)

    @Query("UPDATE notifications SET isRead = 1")
    suspend fun markAllNotificationsRead()

    // Messages
    @Query("SELECT * FROM conversations ORDER BY updatedAtMillis DESC")
    fun observeConversations(): Flow<List<MessageConversationEntity>>

    @Query("SELECT COUNT(*) FROM conversations")
    suspend fun getConversationCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversations(items: List<MessageConversationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(item: MessageConversationEntity)

    @Query("SELECT * FROM direct_messages WHERE conversationId = :conversationId ORDER BY sentAtMillis ASC")
    fun observeMessagesForConversation(conversationId: String): Flow<List<DirectMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDirectMessages(messages: List<DirectMessageEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDirectMessage(message: DirectMessageEntity)

    @Query("UPDATE conversations SET unreadCount = 0 WHERE id = :conversationId")
    suspend fun clearConversationUnread(conversationId: String)

    // Lists & Communities
    @Query("SELECT * FROM x_lists ORDER BY subscriberCount DESC")
    fun observeXLists(): Flow<List<XListEntity>>

    @Query("SELECT COUNT(*) FROM x_lists")
    suspend fun getXListCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertXLists(lists: List<XListEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertXList(list: XListEntity)

    @Query("UPDATE x_lists SET isJoined = :joined WHERE id = :listId")
    suspend fun updateXListJoined(listId: String, joined: Boolean)

    @Query("SELECT * FROM communities ORDER BY memberCount DESC")
    fun observeCommunities(): Flow<List<CommunityEntity>>

    @Query("SELECT COUNT(*) FROM communities")
    suspend fun getCommunityCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommunities(items: List<CommunityEntity>)

    @Query("UPDATE communities SET isJoined = :joined, memberCount = memberCount + :delta WHERE id = :communityId")
    suspend fun updateCommunityMembership(communityId: String, joined: Boolean, delta: Int)

    // Media Center
    @Query("SELECT * FROM media_library ORDER BY title ASC")
    fun observeMediaLibrary(): Flow<List<MediaItemEntity>>

    @Query("SELECT COUNT(*) FROM media_library")
    suspend fun getMediaLibraryCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMediaItems(items: List<MediaItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMediaItem(item: MediaItemEntity)

    @Query("UPDATE media_library SET isFavorite = :favorite WHERE id = :mediaId")
    suspend fun updateMediaFavorite(mediaId: String, favorite: Boolean)

    @Query("UPDATE media_library SET lastPlayedMillis = :playedAt, playCount = playCount + 1 WHERE id = :mediaId")
    suspend fun recordMediaPlayed(mediaId: String, playedAt: Long)

    @Query("UPDATE media_library SET playlistId = :playlistId WHERE id = :mediaId")
    suspend fun assignMediaToPlaylist(mediaId: String, playlistId: String?)

    @Query("SELECT * FROM playlists ORDER BY createdAtMillis DESC")
    fun observePlaylists(): Flow<List<PlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylists(items: List<PlaylistEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(item: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylistById(playlistId: String)

    @Query("UPDATE media_library SET playlistId = NULL WHERE playlistId = :playlistId")
    suspend fun clearPlaylistFromMedia(playlistId: String)

    // Application Tools (Saved Pages, Reading List, Browser History, Feedback)
    @Query("SELECT * FROM saved_pages ORDER BY savedAtMillis DESC")
    fun observeSavedPages(): Flow<List<SavedPageEntity>>

    @Query("SELECT COUNT(*) FROM saved_pages")
    suspend fun getSavedPageCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedPages(pages: List<SavedPageEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedPage(page: SavedPageEntity)

    @Query("UPDATE saved_pages SET isInReadingList = :inList WHERE id = :pageId")
    suspend fun updatePageReadingList(pageId: String, inList: Boolean)

    @Query("UPDATE saved_pages SET isCompleted = :completed WHERE id = :pageId")
    suspend fun updatePageCompleted(pageId: String, completed: Boolean)

    @Query("DELETE FROM saved_pages WHERE id = :pageId")
    suspend fun deleteSavedPageById(pageId: String)

    @Query("SELECT * FROM browser_history ORDER BY visitedAtMillis DESC")
    fun observeBrowserHistory(): Flow<List<BrowserHistoryEntity>>

    @Query("SELECT COUNT(*) FROM browser_history")
    suspend fun getBrowserHistoryCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBrowserHistoryItems(items: List<BrowserHistoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBrowserHistoryItem(item: BrowserHistoryEntity)

    @Query("DELETE FROM browser_history WHERE id = :id")
    suspend fun deleteBrowserHistoryById(id: String)

    @Query("DELETE FROM browser_history")
    suspend fun clearBrowserHistory()

    @Query("SELECT * FROM feedback_submissions ORDER BY submittedAtMillis DESC")
    fun observeFeedback(): Flow<List<FeedbackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeedback(item: FeedbackEntity)
}
