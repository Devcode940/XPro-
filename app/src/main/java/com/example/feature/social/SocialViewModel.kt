package com.example.feature.social

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.XProRepository
import com.example.data.model.CommunityItem
import com.example.data.model.DirectMessage
import com.example.data.model.MessageConversation
import com.example.data.model.NotificationItem
import com.example.data.model.PostItem
import com.example.data.model.SuggestedUser
import com.example.data.model.TrendItem
import com.example.data.model.XListItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SocialUiState(
    val searchQuery: String = "",
    val unreadOnlyNotifications: Boolean = false,
    val notifications: List<NotificationItem> = emptyList(),
    val conversations: List<MessageConversation> = emptyList(),
    val lists: List<XListItem> = emptyList(),
    val communities: List<CommunityItem> = emptyList(),
    val trends: List<TrendItem> = emptyList(),
    val posts: List<PostItem> = emptyList(),
    val suggestedUsers: List<SuggestedUser> = emptyList(),
    val adFreeEnabled: Boolean = true,
    val statusBanner: String? = null
) {
    val unreadNotificationsCount: Int
        get() = notifications.count { !it.isRead }

    val unreadMessagesCount: Int
        get() = conversations.sumOf { it.unreadCount }

    val visibleNotifications: List<NotificationItem>
        get() = if (unreadOnlyNotifications) notifications.filter { !it.isRead } else notifications

    val visibleTrends: List<TrendItem>
        get() = if (adFreeEnabled) trends.filterNot { it.isPromoted } else trends

    val filteredPosts: List<PostItem>
        get() {
            val base = if (adFreeEnabled) posts.filterNot { it.isSponsored } else posts
            val q = searchQuery.trim()
            if (q.isEmpty()) return base
            return base.filter {
                it.content.contains(q, ignoreCase = true) ||
                    it.authorName.contains(q, ignoreCase = true) ||
                    it.authorHandle.contains(q, ignoreCase = true) ||
                    (it.articleTitle?.contains(q, ignoreCase = true) == true)
            }
        }

    val filteredCreators: List<SuggestedUser>
        get() {
            val q = searchQuery.trim()
            if (q.isEmpty()) return suggestedUsers
            return suggestedUsers.filter {
                it.name.contains(q, ignoreCase = true) ||
                    it.handle.contains(q, ignoreCase = true) ||
                    it.bio.contains(q, ignoreCase = true)
            }
        }

    val filteredTrends: List<TrendItem>
        get() {
            val q = searchQuery.trim()
            if (q.isEmpty()) return visibleTrends
            return visibleTrends.filter {
                it.topic.contains(q, ignoreCase = true) ||
                    it.category.contains(q, ignoreCase = true)
            }
        }
}

class SocialViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = XProRepository.getInstance(application)

    private val searchQuery = MutableStateFlow("")
    private val unreadOnly = MutableStateFlow(false)
    private val bannerState = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch { repo.ensureSeeded() }
    }

    val uiState: StateFlow<SocialUiState> = combine(
        searchQuery,
        unreadOnly,
        repo.notifications,
        repo.conversations,
        repo.xLists,
        repo.communities,
        repo.trends,
        repo.allPosts,
        repo.suggestedUsers,
        repo.userProfile,
        bannerState
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        SocialUiState(
            searchQuery = values[0] as String,
            unreadOnlyNotifications = values[1] as Boolean,
            notifications = values[2] as List<NotificationItem>,
            conversations = values[3] as List<MessageConversation>,
            lists = values[4] as List<XListItem>,
            communities = values[5] as List<CommunityItem>,
            trends = values[6] as List<TrendItem>,
            posts = values[7] as List<PostItem>,
            suggestedUsers = values[8] as List<SuggestedUser>,
            adFreeEnabled = (values[9] as com.example.data.model.UserProfile).adFreeEnabled,
            statusBanner = values[10] as String?
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SocialUiState()
    )

    fun observeMessages(conversationId: String): Flow<List<DirectMessage>> =
        repo.observeMessagesForConversation(conversationId)

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun toggleUnreadNotificationsOnly(onlyUnread: Boolean) {
        unreadOnly.value = onlyUnread
    }

    fun markNotificationRead(id: String) {
        viewModelScope.launch { repo.markNotificationRead(id) }
    }

    fun markAllNotificationsRead() {
        viewModelScope.launch {
            repo.markAllNotificationsRead()
            bannerState.value = "All notifications marked as read"
        }
    }

    fun openConversation(conversationId: String) {
        viewModelScope.launch { repo.clearConversationUnread(conversationId) }
    }

    fun sendDirectMessage(conversation: MessageConversation, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            repo.sendDirectMessage(conversation.id, trimmed)
            repo.updateConversationPreview(conversation, trimmed)
        }
    }

    fun createXList(name: String, description: String, isPrivate: Boolean) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repo.createXList(name, description, isPrivate)
            bannerState.value = "Created list \"$name\""
        }
    }

    fun toggleListJoined(listItem: XListItem) {
        viewModelScope.launch { repo.toggleXListJoined(listItem) }
    }

    fun toggleCommunityJoined(community: CommunityItem) {
        viewModelScope.launch { repo.toggleCommunityJoined(community) }
    }

    fun toggleFollowUser(user: SuggestedUser) {
        viewModelScope.launch { repo.setSuggestedUserFollow(user.id, user.isFollowing) }
    }

    fun dismissBanner() {
        bannerState.value = null
    }
}
