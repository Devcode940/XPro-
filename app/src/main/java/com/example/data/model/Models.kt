package com.example.data.model

enum class VerificationBadge(val label: String, val subtitle: String) {
    BLUE("Blue Verified", "Classic X Premium individual checkmark"),
    GOLD("Gold Organization", "Verified business & enterprise badge"),
    GRAY("Gray Official", "Government, institutional & protocol badge"),
    FOUNDER("Founder Prism", "Exclusive X Pro unlocked prism tier"),
    NONE("Standard", "No verification badge displayed")
}

enum class FeedTab(val title: String) {
    FOR_YOU("For You"),
    FOLLOWING("Following")
}

enum class FeedFilter(val title: String) {
    ALL("All"),
    READ_LATER("Read Later ⚡"),
    MEDIA("Media Only"),
    VERIFIED("Verified Only"),
    ARTICLES("Long Articles")
}

enum class ProfileTab(val title: String) {
    POSTS("Posts"),
    REPLIES("Replies"),
    HIGHLIGHTS("Highlights"),
    ARTICLES("Articles"),
    MEDIA("Media")
}

enum class AppDestination(val label: String) {
    HOME("Home"),
    GROK("Grok"),
    EXPLORE("Explore"),
    BOOKMARKS("Bookmarks"),
    ARTICLES("Articles"),
    PROFILE("Profile");

    companion object {
        val bottomNavItems: List<AppDestination> = listOf(HOME, GROK, EXPLORE)
    }
}

enum class ReaderTypography {
    SANS,
    SERIF,
    MONO
}

enum class BookmarkItemType(val label: String) {
    POST("Posts"),
    VIDEO("Videos"),
    IMAGE("Images"),
    LINK("Links")
}

enum class NotificationKind(val label: String) {
    LIKE("Like"),
    COMMENT("Comment"),
    MESSAGE("Message"),
    FOLLOWER("Follower"),
    DOWNLOAD("Download"),
    SYSTEM("System")
}

enum class LibraryMediaKind(val label: String) {
    VIDEO("Video"),
    AUDIO("Audio")
}

data class PostRevision(
    val content: String,
    val editedAtMillis: Long,
    val reason: String = "Updated post"
)

data class PollOption(
    val id: String,
    val label: String,
    val votes: Int
)

data class PollData(
    val question: String,
    val options: List<PollOption>,
    val selectedOptionId: String? = null,
    val durationLabel: String = "24h remaining"
)

data class PostItem(
    val id: String,
    val authorId: String,
    val authorName: String,
    val authorHandle: String,
    val authorBio: String,
    val authorBadge: VerificationBadge,
    val authorAvatarColorHex: Long,
    val isCurrentUser: Boolean = false,
    val isFollowedAuthor: Boolean = true,
    val content: String,
    val articleTitle: String? = null,
    val articleSubtitle: String? = null,
    val createdAtMillis: Long,
    val editedAtMillis: Long? = null,
    val likes: Int,
    val reposts: Int,
    val replies: Int,
    val views: Int,
    val isLiked: Boolean = false,
    val isReposted: Boolean = false,
    val isBookmarked: Boolean = false,
    val bookmarkFolderId: String? = null,
    val mediaResKey: String? = null,
    val mediaUri: String? = null,
    val isLongForm: Boolean = false,
    val isHighlight: Boolean = false,
    val isSponsored: Boolean = false,
    val sponsorLabel: String? = null,
    val audience: String = "Everyone",
    val parentPostId: String? = null,
    val threadPosts: List<String> = emptyList(),
    val revisions: List<PostRevision> = emptyList(),
    val poll: PollData? = null,
    val isSavedForOffline: Boolean = false,
    val offlineCachedAtMillis: Long? = null,
    val isOfflineRead: Boolean = false,
    val offlineNotes: String? = null
)

data class BookmarkFolder(
    val id: String,
    val name: String,
    val description: String,
    val colorHex: Long,
    val iconKey: String,
    val count: Int = 0
)

data class TypedBookmarkItem(
    val id: String,
    val itemType: BookmarkItemType,
    val title: String,
    val content: String,
    val authorName: String,
    val authorHandle: String,
    val mediaUrl: String? = null,
    val linkUrl: String? = null,
    val folderId: String? = null,
    val postId: String? = null,
    val createdAtMillis: Long
)

data class TrendItem(
    val id: String,
    val category: String,
    val topic: String,
    val postCountLabel: String,
    val isPromoted: Boolean = false
)

data class SuggestedUser(
    val id: String,
    val name: String,
    val handle: String,
    val bio: String,
    val badge: VerificationBadge,
    val isFollowing: Boolean,
    val avatarColorHex: Long
)

data class UserProfile(
    val id: String = "current_user",
    val name: String,
    val handle: String,
    val bio: String,
    val badge: VerificationBadge,
    val joinedDate: String,
    val followingCount: Int,
    val followersCount: Int,
    val undoDelaySeconds: Int = 5,
    val adFreeEnabled: Boolean = true,
    val extendedPostsEnabled: Boolean = true,
    val showEditHistoryLinks: Boolean = true,
    val autoExpandThreads: Boolean = false
)

data class PendingPost(
    val id: String,
    val content: String,
    val articleTitle: String? = null,
    val articleSubtitle: String? = null,
    val isLongForm: Boolean = false,
    val mediaResKey: String? = null,
    val mediaUri: String? = null,
    val pollOptions: List<String> = emptyList(),
    val audience: String = "Everyone",
    val parentPostId: String? = null,
    val remainingMillis: Long,
    val totalMillis: Long
)

data class NotificationItem(
    val id: String,
    val kind: NotificationKind,
    val actorName: String,
    val actorHandle: String,
    val actorAvatarHex: Long,
    val message: String,
    val targetPostId: String? = null,
    val createdAtMillis: Long,
    val isRead: Boolean
)

data class MessageConversation(
    val id: String,
    val participantName: String,
    val participantHandle: String,
    val participantBadge: VerificationBadge,
    val avatarColorHex: Long,
    val lastMessage: String,
    val updatedAtMillis: Long,
    val unreadCount: Int,
    val isEncrypted: Boolean = true
)

data class DirectMessage(
    val id: String,
    val conversationId: String,
    val senderIsCurrentUser: Boolean,
    val text: String,
    val sentAtMillis: Long
)

data class XListItem(
    val id: String,
    val name: String,
    val description: String,
    val memberCount: Int,
    val subscriberCount: Int,
    val isJoined: Boolean,
    val isPrivate: Boolean
)

data class CommunityItem(
    val id: String,
    val name: String,
    val category: String,
    val description: String,
    val memberCount: Int,
    val isJoined: Boolean,
    val accentHex: Long
)

data class MediaLibraryItem(
    val id: String,
    val title: String,
    val creator: String,
    val mediaKind: LibraryMediaKind,
    val sourceUrl: String,
    val durationLabel: String,
    val resolution: String,
    val sizeLabel: String,
    val isFavorite: Boolean,
    val lastPlayedMillis: Long?,
    val playCount: Int,
    val playlistId: String?
)

data class PlaylistItem(
    val id: String,
    val name: String,
    val description: String,
    val createdAtMillis: Long,
    val trackCount: Int = 0
)

data class SavedPageItem(
    val id: String,
    val title: String,
    val url: String,
    val summary: String,
    val savedAtMillis: Long,
    val isInReadingList: Boolean,
    val isCompleted: Boolean
)

data class BrowserHistoryItem(
    val id: String,
    val title: String,
    val url: String,
    val visitedAtMillis: Long
)

data class FeedbackSubmission(
    val id: String,
    val category: String,
    val message: String,
    val submittedAtMillis: Long
)
