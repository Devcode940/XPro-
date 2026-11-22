package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.BookmarkFolder
import com.example.data.model.BookmarkItemType
import com.example.data.model.BrowserHistoryItem
import com.example.data.model.CommunityItem
import com.example.data.model.DirectMessage
import com.example.data.model.FeedbackSubmission
import com.example.data.model.LibraryMediaKind
import com.example.data.model.MediaLibraryItem
import com.example.data.model.MessageConversation
import com.example.data.model.NotificationItem
import com.example.data.model.NotificationKind
import com.example.data.model.PlaylistItem
import com.example.data.model.PollData
import com.example.data.model.PostItem
import com.example.data.model.PostRevision
import com.example.data.model.SavedPageItem
import com.example.data.model.SuggestedUser
import com.example.data.model.TrendItem
import com.example.data.model.TypedBookmarkItem
import com.example.data.model.UserProfile
import com.example.data.model.VerificationBadge
import com.example.data.model.XListItem

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey val id: String,
    val authorId: String,
    val authorName: String,
    val authorHandle: String,
    val authorBio: String,
    val authorBadge: VerificationBadge,
    val authorAvatarColorHex: Long,
    val isCurrentUser: Boolean,
    val isFollowedAuthor: Boolean,
    val content: String,
    val articleTitle: String?,
    val articleSubtitle: String?,
    val createdAtMillis: Long,
    val editedAtMillis: Long?,
    val likes: Int,
    val reposts: Int,
    val replies: Int,
    val views: Int,
    val isLiked: Boolean,
    val isReposted: Boolean,
    val isBookmarked: Boolean,
    val bookmarkFolderId: String?,
    val mediaResKey: String?,
    val mediaUri: String?,
    val isLongForm: Boolean,
    val isHighlight: Boolean,
    val isSponsored: Boolean,
    val sponsorLabel: String?,
    val audience: String,
    val parentPostId: String?,
    val threadPosts: List<String>,
    val revisions: List<PostRevision>,
    val poll: PollData?,
    val isSavedForOffline: Boolean = false,
    val offlineCachedAtMillis: Long? = null,
    val isOfflineRead: Boolean = false,
    val offlineNotes: String? = null
)

/** Tombstone for items deleted locally while offline: replayed remotely on the next sync. */
@Entity(tableName = "pending_remote_deletes")
data class PendingRemoteDeleteEntity(
    @PrimaryKey val id: String,
    /** "bookmark" or "folder", see [PendingRemoteDeleteKind]. */
    val kind: String,
    val createdAtMillis: Long
)

object PendingRemoteDeleteKind {
    const val BOOKMARK = "bookmark"
    const val FOLDER = "folder"
}

@Entity(tableName = "bookmark_folders")
data class BookmarkFolderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val colorHex: Long,
    val iconKey: String,
    @ColumnInfo(defaultValue = "0") val updatedAtMillis: Long = 0L,
    /** true when this row has local changes not yet pushed to Firestore. */
    @ColumnInfo(defaultValue = "0") val pendingSync: Boolean = false
)

@Entity(tableName = "bookmarks_vault")
data class TypedBookmarkEntity(
    @PrimaryKey val id: String,
    val itemType: String,
    val title: String,
    val content: String,
    val authorName: String,
    val authorHandle: String,
    val mediaUrl: String?,
    val linkUrl: String?,
    val folderId: String?,
    val postId: String?,
    val createdAtMillis: Long,
    @ColumnInfo(defaultValue = "0") val updatedAtMillis: Long = 0L,
    /** true when this row has local changes not yet pushed to Firestore. */
    @ColumnInfo(defaultValue = "0") val pendingSync: Boolean = false
)

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val handle: String,
    val bio: String,
    val badge: VerificationBadge,
    val joinedDate: String,
    val followingCount: Int,
    val followersCount: Int,
    val undoDelaySeconds: Int,
    val adFreeEnabled: Boolean,
    val extendedPostsEnabled: Boolean,
    val showEditHistoryLinks: Boolean,
    val autoExpandThreads: Boolean
)

@Entity(tableName = "trends")
data class TrendEntity(
    @PrimaryKey val id: String,
    val category: String,
    val topic: String,
    val postCountLabel: String,
    val isPromoted: Boolean
)

@Entity(tableName = "suggested_users")
data class SuggestedUserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val handle: String,
    val bio: String,
    val badge: VerificationBadge,
    val isFollowing: Boolean,
    val avatarColorHex: Long
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val kind: String,
    val actorName: String,
    val actorHandle: String,
    val actorAvatarHex: Long,
    val message: String,
    val targetPostId: String?,
    val createdAtMillis: Long,
    val isRead: Boolean
)

@Entity(tableName = "conversations")
data class MessageConversationEntity(
    @PrimaryKey val id: String,
    val participantName: String,
    val participantHandle: String,
    val participantBadge: VerificationBadge,
    val avatarColorHex: Long,
    val lastMessage: String,
    val updatedAtMillis: Long,
    val unreadCount: Int,
    val isEncrypted: Boolean
)

@Entity(tableName = "direct_messages")
data class DirectMessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val senderIsCurrentUser: Boolean,
    val text: String,
    val sentAtMillis: Long
)

@Entity(tableName = "x_lists")
data class XListEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val memberCount: Int,
    val subscriberCount: Int,
    val isJoined: Boolean,
    val isPrivate: Boolean
)

@Entity(tableName = "communities")
data class CommunityEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val description: String,
    val memberCount: Int,
    val isJoined: Boolean,
    val accentHex: Long
)

@Entity(tableName = "media_library")
data class MediaItemEntity(
    @PrimaryKey val id: String,
    val title: String,
    val creator: String,
    val mediaKind: String,
    val sourceUrl: String,
    val durationLabel: String,
    val resolution: String,
    val sizeLabel: String,
    val isFavorite: Boolean,
    val lastPlayedMillis: Long?,
    val playCount: Int,
    val playlistId: String?
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val createdAtMillis: Long
)

@Entity(tableName = "saved_pages")
data class SavedPageEntity(
    @PrimaryKey val id: String,
    val title: String,
    val url: String,
    val summary: String,
    val savedAtMillis: Long,
    val isInReadingList: Boolean,
    val isCompleted: Boolean
)

@Entity(tableName = "browser_history")
data class BrowserHistoryEntity(
    @PrimaryKey val id: String,
    val title: String,
    val url: String,
    val visitedAtMillis: Long
)

@Entity(tableName = "feedback_submissions")
data class FeedbackEntity(
    @PrimaryKey val id: String,
    val category: String,
    val message: String,
    val submittedAtMillis: Long
)

fun PostEntity.toDomain() = PostItem(
    id = id,
    authorId = authorId,
    authorName = authorName,
    authorHandle = authorHandle,
    authorBio = authorBio,
    authorBadge = authorBadge,
    authorAvatarColorHex = authorAvatarColorHex,
    isCurrentUser = isCurrentUser,
    isFollowedAuthor = isFollowedAuthor,
    content = content,
    articleTitle = articleTitle,
    articleSubtitle = articleSubtitle,
    createdAtMillis = createdAtMillis,
    editedAtMillis = editedAtMillis,
    likes = likes,
    reposts = reposts,
    replies = replies,
    views = views,
    isLiked = isLiked,
    isReposted = isReposted,
    isBookmarked = isBookmarked,
    bookmarkFolderId = bookmarkFolderId,
    mediaResKey = mediaResKey,
    mediaUri = mediaUri,
    isLongForm = isLongForm,
    isHighlight = isHighlight,
    isSponsored = isSponsored,
    sponsorLabel = sponsorLabel,
    audience = audience,
    parentPostId = parentPostId,
    threadPosts = threadPosts,
    revisions = revisions,
    poll = poll,
    isSavedForOffline = isSavedForOffline,
    offlineCachedAtMillis = offlineCachedAtMillis,
    isOfflineRead = isOfflineRead,
    offlineNotes = offlineNotes
)

fun PostItem.toEntity() = PostEntity(
    id = id,
    authorId = authorId,
    authorName = authorName,
    authorHandle = authorHandle,
    authorBio = authorBio,
    authorBadge = authorBadge,
    authorAvatarColorHex = authorAvatarColorHex,
    isCurrentUser = isCurrentUser,
    isFollowedAuthor = isFollowedAuthor,
    content = content,
    articleTitle = articleTitle,
    articleSubtitle = articleSubtitle,
    createdAtMillis = createdAtMillis,
    editedAtMillis = editedAtMillis,
    likes = likes,
    reposts = reposts,
    replies = replies,
    views = views,
    isLiked = isLiked,
    isReposted = isReposted,
    isBookmarked = isBookmarked,
    bookmarkFolderId = bookmarkFolderId,
    mediaResKey = mediaResKey,
    mediaUri = mediaUri,
    isLongForm = isLongForm,
    isHighlight = isHighlight,
    isSponsored = isSponsored,
    sponsorLabel = sponsorLabel,
    audience = audience,
    parentPostId = parentPostId,
    threadPosts = threadPosts,
    revisions = revisions,
    poll = poll,
    isSavedForOffline = isSavedForOffline,
    offlineCachedAtMillis = offlineCachedAtMillis,
    isOfflineRead = isOfflineRead,
    offlineNotes = offlineNotes
)

fun BookmarkFolderEntity.toDomain(count: Int = 0) = BookmarkFolder(
    id = id,
    name = name,
    description = description,
    colorHex = colorHex,
    iconKey = iconKey,
    count = count
)

fun BookmarkFolder.toEntity() = BookmarkFolderEntity(
    id = id,
    name = name,
    description = description,
    colorHex = colorHex,
    iconKey = iconKey
)

fun TypedBookmarkEntity.toDomain() = TypedBookmarkItem(
    id = id,
    itemType = runCatching { BookmarkItemType.valueOf(itemType) }.getOrDefault(BookmarkItemType.POST),
    title = title,
    content = content,
    authorName = authorName,
    authorHandle = authorHandle,
    mediaUrl = mediaUrl,
    linkUrl = linkUrl,
    folderId = folderId,
    postId = postId,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis
)

fun TypedBookmarkItem.toEntity() = TypedBookmarkEntity(
    id = id,
    itemType = itemType.name,
    title = title,
    content = content,
    authorName = authorName,
    authorHandle = authorHandle,
    mediaUrl = mediaUrl,
    linkUrl = linkUrl,
    folderId = folderId,
    postId = postId,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis,
    pendingSync = false
)

fun UserProfileEntity.toDomain() = UserProfile(
    id = id,
    name = name,
    handle = handle,
    bio = bio,
    badge = badge,
    joinedDate = joinedDate,
    followingCount = followingCount,
    followersCount = followersCount,
    undoDelaySeconds = undoDelaySeconds,
    adFreeEnabled = adFreeEnabled,
    extendedPostsEnabled = extendedPostsEnabled,
    showEditHistoryLinks = showEditHistoryLinks,
    autoExpandThreads = autoExpandThreads
)

fun UserProfile.toEntity() = UserProfileEntity(
    id = id,
    name = name,
    handle = handle,
    bio = bio,
    badge = badge,
    joinedDate = joinedDate,
    followingCount = followingCount,
    followersCount = followersCount,
    undoDelaySeconds = undoDelaySeconds,
    adFreeEnabled = adFreeEnabled,
    extendedPostsEnabled = extendedPostsEnabled,
    showEditHistoryLinks = showEditHistoryLinks,
    autoExpandThreads = autoExpandThreads
)

fun TrendEntity.toDomain() = TrendItem(
    id = id,
    category = category,
    topic = topic,
    postCountLabel = postCountLabel,
    isPromoted = isPromoted
)

fun TrendItem.toEntity() = TrendEntity(
    id = id,
    category = category,
    topic = topic,
    postCountLabel = postCountLabel,
    isPromoted = isPromoted
)

fun SuggestedUserEntity.toDomain() = SuggestedUser(
    id = id,
    name = name,
    handle = handle,
    bio = bio,
    badge = badge,
    isFollowing = isFollowing,
    avatarColorHex = avatarColorHex
)

fun SuggestedUser.toEntity() = SuggestedUserEntity(
    id = id,
    name = name,
    handle = handle,
    bio = bio,
    badge = badge,
    isFollowing = isFollowing,
    avatarColorHex = avatarColorHex
)

fun NotificationEntity.toDomain() = NotificationItem(
    id = id,
    kind = runCatching { NotificationKind.valueOf(kind) }.getOrDefault(NotificationKind.SYSTEM),
    actorName = actorName,
    actorHandle = actorHandle,
    actorAvatarHex = actorAvatarHex,
    message = message,
    targetPostId = targetPostId,
    createdAtMillis = createdAtMillis,
    isRead = isRead
)

fun NotificationItem.toEntity() = NotificationEntity(
    id = id,
    kind = kind.name,
    actorName = actorName,
    actorHandle = actorHandle,
    actorAvatarHex = actorAvatarHex,
    message = message,
    targetPostId = targetPostId,
    createdAtMillis = createdAtMillis,
    isRead = isRead
)

fun MessageConversationEntity.toDomain() = MessageConversation(
    id = id,
    participantName = participantName,
    participantHandle = participantHandle,
    participantBadge = participantBadge,
    avatarColorHex = avatarColorHex,
    lastMessage = lastMessage,
    updatedAtMillis = updatedAtMillis,
    unreadCount = unreadCount,
    isEncrypted = isEncrypted
)

fun MessageConversation.toEntity() = MessageConversationEntity(
    id = id,
    participantName = participantName,
    participantHandle = participantHandle,
    participantBadge = participantBadge,
    avatarColorHex = avatarColorHex,
    lastMessage = lastMessage,
    updatedAtMillis = updatedAtMillis,
    unreadCount = unreadCount,
    isEncrypted = isEncrypted
)

fun DirectMessageEntity.toDomain() = DirectMessage(
    id = id,
    conversationId = conversationId,
    senderIsCurrentUser = senderIsCurrentUser,
    text = text,
    sentAtMillis = sentAtMillis
)

fun DirectMessage.toEntity() = DirectMessageEntity(
    id = id,
    conversationId = conversationId,
    senderIsCurrentUser = senderIsCurrentUser,
    text = text,
    sentAtMillis = sentAtMillis
)

fun XListEntity.toDomain() = XListItem(
    id = id,
    name = name,
    description = description,
    memberCount = memberCount,
    subscriberCount = subscriberCount,
    isJoined = isJoined,
    isPrivate = isPrivate
)

fun XListItem.toEntity() = XListEntity(
    id = id,
    name = name,
    description = description,
    memberCount = memberCount,
    subscriberCount = subscriberCount,
    isJoined = isJoined,
    isPrivate = isPrivate
)

fun CommunityEntity.toDomain() = CommunityItem(
    id = id,
    name = name,
    category = category,
    description = description,
    memberCount = memberCount,
    isJoined = isJoined,
    accentHex = accentHex
)

fun CommunityItem.toEntity() = CommunityEntity(
    id = id,
    name = name,
    category = category,
    description = description,
    memberCount = memberCount,
    isJoined = isJoined,
    accentHex = accentHex
)

fun MediaItemEntity.toDomain() = MediaLibraryItem(
    id = id,
    title = title,
    creator = creator,
    mediaKind = runCatching { LibraryMediaKind.valueOf(mediaKind) }.getOrDefault(LibraryMediaKind.VIDEO),
    sourceUrl = sourceUrl,
    durationLabel = durationLabel,
    resolution = resolution,
    sizeLabel = sizeLabel,
    isFavorite = isFavorite,
    lastPlayedMillis = lastPlayedMillis,
    playCount = playCount,
    playlistId = playlistId
)

fun MediaLibraryItem.toEntity() = MediaItemEntity(
    id = id,
    title = title,
    creator = creator,
    mediaKind = mediaKind.name,
    sourceUrl = sourceUrl,
    durationLabel = durationLabel,
    resolution = resolution,
    sizeLabel = sizeLabel,
    isFavorite = isFavorite,
    lastPlayedMillis = lastPlayedMillis,
    playCount = playCount,
    playlistId = playlistId
)

fun PlaylistEntity.toDomain(trackCount: Int = 0) = PlaylistItem(
    id = id,
    name = name,
    description = description,
    createdAtMillis = createdAtMillis,
    trackCount = trackCount
)

fun PlaylistItem.toEntity() = PlaylistEntity(
    id = id,
    name = name,
    description = description,
    createdAtMillis = createdAtMillis
)

fun SavedPageEntity.toDomain() = SavedPageItem(
    id = id,
    title = title,
    url = url,
    summary = summary,
    savedAtMillis = savedAtMillis,
    isInReadingList = isInReadingList,
    isCompleted = isCompleted
)

fun SavedPageItem.toEntity() = SavedPageEntity(
    id = id,
    title = title,
    url = url,
    summary = summary,
    savedAtMillis = savedAtMillis,
    isInReadingList = isInReadingList,
    isCompleted = isCompleted
)

fun BrowserHistoryEntity.toDomain() = BrowserHistoryItem(
    id = id,
    title = title,
    url = url,
    visitedAtMillis = visitedAtMillis
)

fun BrowserHistoryItem.toEntity() = BrowserHistoryEntity(
    id = id,
    title = title,
    url = url,
    visitedAtMillis = visitedAtMillis
)

fun FeedbackEntity.toDomain() = FeedbackSubmission(
    id = id,
    category = category,
    message = message,
    submittedAtMillis = submittedAtMillis
)
