package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.SeedData
import com.example.data.local.XProRepository
import com.example.data.model.BookmarkFolder
import com.example.data.model.PostItem
import com.example.data.model.ProfileTab
import com.example.data.model.SuggestedUser
import com.example.data.model.UserProfile
import com.example.data.model.VerificationBadge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data class Error(val message: String, val userId: String) : ProfileUiState
    data class Success(
        val userId: String,
        val isCurrentUser: Boolean,
        val displayName: String,
        val handle: String,
        val bio: String,
        val badge: VerificationBadge,
        val avatarColorHex: Long,
        val joinedDate: String,
        val followersCount: Int,
        val followingCount: Int,
        val isFollowing: Boolean,
        val activeTab: ProfileTab,
        val posts: List<PostItem>,
        val currentUserSettings: UserProfile,
        val bookmarkFolders: List<BookmarkFolder>,
        val blockedAdsCount: Int
    ) : ProfileUiState
}

private data class ProfileControls(
    val userId: String = "current_user",
    val activeTab: ProfileTab = ProfileTab.POSTS,
    val isLoading: Boolean = false,
    val isInitialized: Boolean = false,
    val simulatedError: String? = null
)

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: XProRepository
    private val controls = MutableStateFlow(ProfileControls())

    init {
        repository = XProRepository.getInstance(application)
        viewModelScope.launch {
            repository.ensureSeeded()
            controls.value = controls.value.copy(isInitialized = true)
        }
    }

    val uiState: StateFlow<ProfileUiState> = combine(
        controls,
        repository.userProfile,
        repository.allPosts,
        repository.suggestedUsers,
        repository.bookmarkFolders,
        repository.trends
    ) { values ->
        val ctrl = values[0] as ProfileControls
        val currentProfile = values[1] as UserProfile
        @Suppress("UNCHECKED_CAST")
        val allPosts = values[2] as List<PostItem>
        @Suppress("UNCHECKED_CAST")
        val suggestedUsers = values[3] as List<SuggestedUser>
        @Suppress("UNCHECKED_CAST")
        val folders = values[4] as List<BookmarkFolder>
        @Suppress("UNCHECKED_CAST")
        val trends = values[5] as List<com.example.data.model.TrendItem>

        if (ctrl.isLoading || !ctrl.isInitialized) {
            return@combine ProfileUiState.Loading
        }
        ctrl.simulatedError?.let { errorMsg ->
            return@combine ProfileUiState.Error(message = errorMsg, userId = ctrl.userId)
        }

        val adFree = currentProfile.adFreeEnabled
        val visiblePosts = if (adFree) allPosts.filterNot { it.isSponsored } else allPosts
        val blockedAdsCount = if (adFree) {
            allPosts.count { it.isSponsored } + trends.count { it.isPromoted }
        } else 0

        if (ctrl.userId == "current_user") {
            val userPosts = visiblePosts.filter { it.isCurrentUser }
            val tabPosts = filterByProfileTab(userPosts, ctrl.activeTab)
            ProfileUiState.Success(
                userId = "current_user",
                isCurrentUser = true,
                displayName = currentProfile.name,
                handle = currentProfile.handle,
                bio = currentProfile.bio,
                badge = currentProfile.badge,
                avatarColorHex = 0xFF1D9BF0,
                joinedDate = currentProfile.joinedDate,
                followersCount = currentProfile.followersCount,
                followingCount = currentProfile.followingCount,
                isFollowing = false,
                activeTab = ctrl.activeTab,
                posts = tabPosts,
                currentUserSettings = currentProfile,
                bookmarkFolders = folders,
                blockedAdsCount = blockedAdsCount
            )
        } else {
            val suggested = suggestedUsers.firstOrNull { it.id == ctrl.userId }
            val authorPosts = visiblePosts.filter { it.authorId == ctrl.userId }
            val leadPost = authorPosts.firstOrNull()

            if (suggested == null && leadPost == null) {
                ProfileUiState.Error(
                    message = "Unable to locate profile \"${ctrl.userId}\". The account may be unavailable offline.",
                    userId = ctrl.userId
                )
            } else {
                val name = suggested?.name ?: leadPost?.authorName ?: "Creator"
                val handle = suggested?.handle ?: leadPost?.authorHandle ?: "@creator"
                val bio = suggested?.bio ?: leadPost?.authorBio ?: ""
                val badge = suggested?.badge ?: leadPost?.authorBadge ?: VerificationBadge.NONE
                val colorHex = suggested?.avatarColorHex ?: leadPost?.authorAvatarColorHex ?: 0xFF1D9BF0
                val isFollowing = suggested?.isFollowing ?: leadPost?.isFollowedAuthor ?: false
                val followersEstimate = estimateFollowersCount(ctrl.userId, isFollowing)
                val followingEstimate = estimateFollowingCount(ctrl.userId)

                ProfileUiState.Success(
                    userId = ctrl.userId,
                    isCurrentUser = false,
                    displayName = name,
                    handle = handle,
                    bio = bio,
                    badge = badge,
                    avatarColorHex = colorHex,
                    joinedDate = "Joined October 2020",
                    followersCount = followersEstimate,
                    followingCount = followingEstimate,
                    isFollowing = isFollowing,
                    activeTab = ctrl.activeTab,
                    posts = filterByProfileTab(authorPosts, ctrl.activeTab),
                    currentUserSettings = currentProfile,
                    bookmarkFolders = folders,
                    blockedAdsCount = blockedAdsCount
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProfileUiState.Loading
    )

    fun loadProfile(userId: String) {
        controls.value = controls.value.copy(
            userId = userId,
            activeTab = ProfileTab.POSTS,
            isLoading = false,
            simulatedError = null
        )
    }

    fun selectTab(tab: ProfileTab) {
        controls.value = controls.value.copy(activeTab = tab)
    }

    fun retry() {
        val currentId = controls.value.userId
        controls.value = controls.value.copy(isLoading = true, simulatedError = null)
        viewModelScope.launch {
            repository.ensureSeeded()
            controls.value = controls.value.copy(
                userId = currentId,
                isLoading = false,
                isInitialized = true,
                simulatedError = null
            )
        }
    }

    fun triggerErrorPreview() {
        controls.value = controls.value.copy(
            simulatedError = "Profile sync timed out while refreshing remote stats. Tap Retry to reload from local cache."
        )
    }

    fun toggleFollowViewedUser() {
        val state = uiState.value as? ProfileUiState.Success ?: return
        if (state.isCurrentUser) return
        viewModelScope.launch {
            repository.setSuggestedUserFollow(state.userId, state.isFollowing)
        }
    }

    fun updateUndoDelay(seconds: Int) {
        viewModelScope.launch {
            repository.updateAppSettings(undoDelaySeconds = seconds)
        }
    }

    fun toggleAdFree(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateAppSettings(adFreeEnabled = enabled)
        }
    }

    fun toggleEditHistoryLinks(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateAppSettings(showEditHistoryLinks = enabled)
        }
    }

    fun toggleAutoExpandThreads(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateAppSettings(autoExpandThreads = enabled)
        }
    }

    private fun filterByProfileTab(posts: List<PostItem>, tab: ProfileTab): List<PostItem> {
        return when (tab) {
            ProfileTab.POSTS -> posts.filter { it.parentPostId == null }
            ProfileTab.REPLIES -> posts.filter { it.parentPostId != null }
            ProfileTab.HIGHLIGHTS -> posts.filter { it.isHighlight }
            ProfileTab.ARTICLES -> posts.filter { it.isLongForm }
            ProfileTab.MEDIA -> posts.filter { it.mediaResKey != null || it.mediaUri != null }
        }
    }

    private fun estimateFollowersCount(userId: String, isFollowing: Boolean): Int {
        val base = when (userId) {
            "author_kairo" -> 142_300
            "author_mira" -> 64_800
            "author_protocol" -> 215_400
            "author_devon" -> 91_250
            else -> 12_400
        }
        return if (isFollowing) base + 1 else base
    }

    private fun estimateFollowingCount(userId: String): Int {
        return when (userId) {
            "author_kairo" -> 184
            "author_mira" -> 412
            "author_protocol" -> 42
            "author_devon" -> 618
            else -> 210
        }
    }
}
