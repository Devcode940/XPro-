package com.example

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.paging.PagingSource
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.devcode.xpro.mediadownloader.SupportedMediaType
import com.example.data.local.SeedData
import com.example.data.local.XProDatabase
import com.example.data.local.XProRepository
import com.example.data.model.BookmarkItemType
import com.example.data.model.VerificationBadge
import com.example.feature.bookmarks.BookmarkFilterCategory
import com.example.feature.bookmarks.BookmarksViewModel
import com.example.feature.downloads.DownloadsFilterTab
import com.example.feature.downloads.DownloadsViewModel
import com.example.feature.settings.SettingsViewModel
import com.example.feature.settings.ThemeMode
import com.example.ui.theme.XProTheme
import com.example.viewmodel.GrokMode
import com.example.viewmodel.GrokViewModel
import com.example.viewmodel.ProfileUiState
import com.example.viewmodel.ProfileViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var db: XProDatabase
    private lateinit var repository: XProRepository

    @Before
    fun setup() {
        XProDatabase.resetInstanceForTesting()
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, XProDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = XProRepository(db.xProDao())
        runBlocking { repository.ensureSeeded() }
    }

    @After
    fun teardown() {
        db.close()
        XProDatabase.resetInstanceForTesting()
    }

    /**
     * Waits for [tag] to exist instead of asserting straight after `waitForIdle()`.
     *
     * `waitForIdle()` only promises that Compose has no pending frame work — the tag can still be
     * missing while the seeding coroutine launched by the ViewModel is in flight, which made the
     * compose tests fail intermittently on CI (identical commit, different run).
     */
    private fun waitForTag(tag: String, timeoutMillis: Long = 10_000L) {
        composeTestRule.waitUntil(timeoutMillis = timeoutMillis) {
            Thread.sleep(10)
            composeTestRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun `app name matches XPro and applicationId is com devcode xpro`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("XPro", appName)
        assertEquals("com.devcode.xpro", BuildConfig.APPLICATION_ID)
    }

    @Test
    fun `ad free mode defaults to enabled and seed contains sponsored items to filter`() = runBlocking {
        val profile = repository.userProfile.first()
        assertTrue(profile.adFreeEnabled)

        val posts = repository.allPosts.first()
        val sponsoredCount = posts.count { it.isSponsored }
        assertTrue(sponsoredCount >= 3)

        val filteredPosts = if (profile.adFreeEnabled) posts.filterNot { it.isSponsored } else posts
        assertFalse(filteredPosts.any { it.isSponsored })

        val trends = repository.trends.first()
        val visibleTrends = if (profile.adFreeEnabled) trends.filterNot { it.isPromoted } else trends
        assertFalse(visibleTrends.any { it.isPromoted })
    }

    @Test
    fun `editing post preserves previous content in revision history`() = runBlocking {
        val originalPost = repository.allPosts.first().first { it.id == "post_user_2" }
        val initialRevisionCount = originalPost.revisions.size

        repository.editPost(
            postId = originalPost.id,
            newContent = "Updated bookmark folders guide with keyboard shortcuts!",
            reason = "Added shortcut tip"
        )

        val updated = repository.allPosts.first().first { it.id == originalPost.id }
        assertEquals("Updated bookmark folders guide with keyboard shortcuts!", updated.content)
        assertEquals(initialRevisionCount + 1, updated.revisions.size)
        assertEquals(originalPost.content, updated.revisions.last().content)
        assertEquals("Added shortcut tip", updated.revisions.last().reason)
    }

    @Test
    fun `creating bookmark folder and assigning post updates folder count`() = runBlocking {
        val folderId = repository.createBookmarkFolder(
            name = "Compilers",
            description = "LLVM and SSA notes",
            colorHex = 0xFF1D9BF0,
            iconKey = "code"
        )
        repository.assignBookmark("post_poll_1", folderId)

        val folders = repository.bookmarkFolders.first()
        val created = folders.first { it.id == folderId }
        assertEquals("Compilers", created.name)
        assertEquals(1, created.count)
    }

    @Test
    fun `updating verification badge syncs across user profile and posts`() = runBlocking {
        repository.updateUserProfile(
            name = SeedData.initialProfile.name,
            handle = SeedData.initialProfile.handle,
            bio = SeedData.initialProfile.bio,
            badge = VerificationBadge.GOLD,
            undoDelaySeconds = 10,
            adFreeEnabled = true,
            extendedPostsEnabled = true,
            showEditHistoryLinks = true,
            autoExpandThreads = false
        )

        val profile = repository.userProfile.first()
        assertEquals(VerificationBadge.GOLD, profile.badge)
        assertEquals(10, profile.undoDelaySeconds)

        val userPost = repository.allPosts.first().first { it.isCurrentUser }
        assertEquals(VerificationBadge.GOLD, userPost.authorBadge)
    }

    @Test
    fun `compose app renders ad shield pill and hides sponsored posts`() {
        composeTestRule.setContent {
            XProTheme {
                XProApp()
            }
        }
        composeTestRule.waitForIdle()
        waitForTag("ad_shield_pill")
        composeTestRule.onNodeWithTag("ad_shield_pill").assertIsDisplayed()
        composeTestRule.onNodeWithTag("post_card_post_ad_1").assertDoesNotExist()
    }

    @Test
    fun `room database caches posts and threads for offline read later and persists read status`() = runBlocking {
        val initialReadLater = repository.readLaterPosts.first()
        assertTrue(initialReadLater.isNotEmpty())
        assertTrue(initialReadLater.any { it.threadPosts.isNotEmpty() })

        val saved = repository.toggleSaveForOffline("post_user_2")
        assertTrue(saved)

        val updatedList = repository.readLaterPosts.first()
        val userPost = updatedList.first { it.id == "post_user_2" }
        assertTrue(userPost.isSavedForOffline)
        assertTrue((userPost.offlineCachedAtMillis ?: 0L) > 0L)
        assertFalse(userPost.isOfflineRead)

        val read = repository.toggleOfflineRead("post_user_2")
        assertTrue(read)
        val readPost = repository.readLaterPosts.first().first { it.id == "post_user_2" }
        assertTrue(readPost.isOfflineRead)

        val unread = repository.toggleOfflineRead("post_user_2")
        assertFalse(unread)
        val unreadPost = repository.readLaterPosts.first().first { it.id == "post_user_2" }
        assertFalse(unreadPost.isOfflineRead)

        val removed = repository.toggleSaveForOffline("post_user_2")
        assertFalse(removed)
        assertFalse(repository.readLaterPosts.first().any { it.id == "post_user_2" })
    }

    @Test
    fun `cache all threads and articles marks long-form articles and multi-part threads`() = runBlocking {
        repository.clearOfflineCache()
        assertEquals(0, repository.getOfflineSavedCount())

        repository.cacheAllThreadsAndArticlesForOffline()
        val cached = repository.readLaterPosts.first()
        assertTrue(cached.size >= 3)
        assertTrue(cached.any { it.isLongForm })
        assertTrue(cached.any { it.threadPosts.isNotEmpty() })
    }

    @Test
    fun `compose app renders offline indicator pill`() {
        composeTestRule.setContent {
            XProTheme {
                XProApp()
            }
        }
        composeTestRule.waitForIdle()
        waitForTag("offline_mode_pill")
        composeTestRule.onNodeWithTag("offline_mode_pill").assertIsDisplayed()
        waitForTag("open_media_downloader_btn")
        composeTestRule.onNodeWithTag("open_media_downloader_btn").assertIsDisplayed()
    }

    @Test
    fun `paging source loads followed users posts cleanly`() = runBlocking {
        val pagingSource = db.xProDao().pagingFeedPosts(
            excludeSponsored = true,
            followingOnly = true,
            readLaterOnly = false,
            mediaOnly = false,
            verifiedOnly = false,
            articlesOnly = false
        )
        val result = pagingSource.load(
            PagingSource.LoadParams.Refresh(
                key = null,
                loadSize = 5,
                placeholdersEnabled = false
            )
        )
        assertTrue(result is PagingSource.LoadResult.Page)
        val page = result as PagingSource.LoadResult.Page
        assertEquals(5, page.data.size)
        assertTrue(page.data.all { !it.isSponsored && (it.isFollowedAuthor || it.isCurrentUser) })

        val verifiedSource = db.xProDao().pagingFeedPosts(
            excludeSponsored = true,
            followingOnly = false,
            readLaterOnly = false,
            mediaOnly = false,
            verifiedOnly = true,
            articlesOnly = false
        )
        val verifiedPage = verifiedSource.load(
            PagingSource.LoadParams.Refresh(key = null, loadSize = 20, placeholdersEnabled = false)
        ) as PagingSource.LoadResult.Page
        assertTrue(verifiedPage.data.isNotEmpty())
        assertTrue(verifiedPage.data.all { it.authorBadge != VerificationBadge.NONE })

        val beforeCached = repository.offlineCachedCount.first()
        repository.cacheAllThreadsAndArticlesForOffline()
        val afterCached = repository.offlineCachedCount.first()
        assertTrue(afterCached >= beforeCached && afterCached > 0)
    }

    @Test
    fun `profile viewmodel handles current user, other user, error and retry states`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = ProfileViewModel(app)

        composeTestRule.setContent {
            val state by vm.uiState.collectAsState()
            androidx.compose.material3.Text(
                text = when (val s = state) {
                    is ProfileUiState.Loading -> "loading"
                    is ProfileUiState.Error -> "error:${s.message}"
                    is ProfileUiState.Success -> "success:${s.displayName}"
                }
            )
        }

        vm.loadProfile("author_kairo")
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            Thread.sleep(10)
            val s = vm.uiState.value as? ProfileUiState.Success
            s?.displayName == "Kairo Systems" && s.posts.isNotEmpty()
        }
        val authorState = vm.uiState.value as ProfileUiState.Success
        assertFalse(authorState.isCurrentUser)
        assertTrue(authorState.followersCount > 0)
        assertTrue(authorState.posts.isNotEmpty())

        vm.triggerErrorPreview()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            Thread.sleep(10)
            vm.uiState.value is ProfileUiState.Error
        }
        val errorState = vm.uiState.value as ProfileUiState.Error
        assertTrue(errorState.message.isNotBlank())

        vm.retry()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            Thread.sleep(10)
            (vm.uiState.value as? ProfileUiState.Success)?.displayName == "Kairo Systems"
        }
    }

    @Test
    fun `compose app navigates to downloads manager and drawer profile screen`() {
        composeTestRule.setContent {
            XProTheme {
                XProApp()
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("open_media_downloader_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("downloads_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("top_bar_back_btn").performClick()
        composeTestRule.waitForIdle()

        // Tapping top-left avatar opens the complete XTRA navigation drawer
        composeTestRule.onNodeWithTag("home_profile_avatar").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("app_drawer_sheet").assertIsDisplayed()
        composeTestRule.onNodeWithTag("drawer_action_view_profile").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            Thread.sleep(10)
            composeTestRule.onAllNodesWithTag("profile_screen_success").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("profile_screen_success").assertIsDisplayed()
        composeTestRule.onNodeWithTag("profile_top_bar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("profile_top_settings_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("profile_top_refresh_btn").assertIsDisplayed()

        // Navigate back to Home via Profile top bar back button, then open Profile from Explore menu
        composeTestRule.onNodeWithTag("profile_back_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("nav_EXPLORE").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("top_right_menu_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("menu_item_profile").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            Thread.sleep(10)
            composeTestRule.onAllNodesWithTag("profile_screen_success").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("profile_top_bar").assertIsDisplayed()
    }

    @Test
    fun `phone bottom nav displays Home Explore Notifications Messages and top right menu opens Settings`() {
        composeTestRule.setContent {
            XProTheme {
                XProApp()
            }
        }
        composeTestRule.waitForIdle()

        // Mobile bottom navigation has Home, Explore, Notifications, Messages
        composeTestRule.onNodeWithTag("nav_HOME").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_EXPLORE").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_NOTIFICATIONS").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_MESSAGES").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_BOOKMARKS").assertDoesNotExist()
        composeTestRule.onNodeWithTag("nav_PROFILE").assertDoesNotExist()

        // Ad Shield Pill still opens Premium Studio customization dialog
        composeTestRule.onNodeWithTag("ad_shield_pill").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("save_premium_studio_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("save_premium_studio_btn").performClick()
        composeTestRule.waitForIdle()

        // Top-right menu button opens the full Settings & Privacy dashboard
        composeTestRule.onNodeWithTag("top_right_menu_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("top_right_menu_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("menu_item_settings").assertIsDisplayed()
        composeTestRule.onNodeWithTag("menu_item_settings").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("settings_screen").assertIsDisplayed()
    }

    @Test
    fun `navigating to Grok via top right menu displays inbuilt Grok powered by X screen`() {
        composeTestRule.setContent {
            XProTheme {
                XProApp()
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("top_right_menu_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("menu_item_grok").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("grok_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("grok_top_bar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("grok_prompt_input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("top_right_menu_btn").assertIsDisplayed()
    }

    @Test
    fun `xtra drawer contains all sections and navigates to bookmarks and about xtra`() {
        composeTestRule.setContent {
            XProTheme {
                XProApp()
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("home_profile_avatar").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("drawer_header").assertIsDisplayed()
        composeTestRule.onNodeWithTag("drawer_section_social").assertIsDisplayed()
        composeTestRule.onNodeWithTag("drawer_section_media").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("drawer_section_tools").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("drawer_section_settings").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("drawer_action_sign_in").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("drawer_action_logout").performScrollTo().assertIsDisplayed()

        // Navigate to Bookmarks from drawer
        composeTestRule.onNodeWithTag("drawer_item_bookmarks").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("xtra_bookmarks_screen").assertIsDisplayed()

        // Return Home via top bar back button, then open About XTRA from Settings & Support drawer section
        composeTestRule.onNodeWithTag("top_bar_back_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("home_profile_avatar").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("drawer_section_settings").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("drawer_item_about_xtra").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("about_xtra_screen").assertIsDisplayed()
    }

    @Test
    fun `settings viewmodel persists theme, download preferences and accessibility flags`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = SettingsViewModel(app)

        composeTestRule.setContent {
            val state by vm.uiState.collectAsState()
            androidx.compose.material3.Text("${state.settings.themeMode.name}:${state.settings.improvedContrast}")
        }

        vm.setThemeMode(ThemeMode.LIGHT)
        vm.setImprovedContrast(true)
        vm.setWifiOnlyDownloads(false)

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            Thread.sleep(10)
            val s = vm.uiState.value.settings
            s.themeMode == ThemeMode.LIGHT && s.improvedContrast && !s.wifiOnlyDownloads
        }
        assertEquals(ThemeMode.LIGHT, vm.uiState.value.settings.themeMode)
        assertTrue(vm.uiState.value.settings.improvedContrast)
        assertFalse(vm.uiState.value.settings.wifiOnlyDownloads)
    }

    @Test
    fun `bookmarks viewmodel adds typed bookmarks filters categories and assigns collections`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = BookmarksViewModel(app)

        composeTestRule.setContent {
            val state by vm.uiState.collectAsState()
            androidx.compose.material3.Text("count=${state.typedBookmarks.size}")
        }

        vm.addTypedBookmark(
            itemType = BookmarkItemType.VIDEO,
            title = "Compose Navigation Deep Dive",
            content = "Multi-module nav architecture clip",
            mediaUrl = "https://cdn.example.com/nav_deep_dive.mp4",
            linkUrl = null,
            folderId = null
        )
        vm.selectCategory(BookmarkFilterCategory.VIDEOS)

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            Thread.sleep(10)
            vm.uiState.value.typedBookmarks.any { it.title == "Compose Navigation Deep Dive" }
        }
        val savedVideo = vm.uiState.value.typedBookmarks.first { it.title == "Compose Navigation Deep Dive" }
        assertEquals(BookmarkItemType.VIDEO, savedVideo.itemType)

        vm.removeTypedBookmark(savedVideo)
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            Thread.sleep(10)
            vm.uiState.value.typedBookmarks.none { it.id == savedVideo.id }
        }
    }

    @Test
    fun `downloads viewmodel validates media urls and updates filter tabs`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = DownloadsViewModel(app)

        composeTestRule.setContent {
            val state by vm.uiState.collectAsState()
            androidx.compose.material3.Text("${state.selectedTab.name}:${state.validationResult?.isValid}")
        }

        vm.onUrlChanged("https://cdn.example.com/demo_trailer.mp4")
        vm.selectTab(DownloadsFilterTab.COMPLETED)

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            Thread.sleep(10)
            val s = vm.uiState.value
            s.selectedTab == DownloadsFilterTab.COMPLETED && s.validationResult?.isValid == true
        }
        assertEquals(SupportedMediaType.MP4, vm.uiState.value.validationResult?.mediaType)
        assertEquals(DownloadsFilterTab.COMPLETED, vm.uiState.value.selectedTab)
    }

    @Test
    fun `grok viewmodel manages mode switching and feed context toggles`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val grokVm = GrokViewModel(app)

        assertEquals(GrokMode.REGULAR, grokVm.uiState.value.mode)
        assertTrue(grokVm.uiState.value.attachFeedContext)
        assertFalse(grokVm.uiState.value.isGenerating)

        grokVm.setMode(GrokMode.FUN)
        assertEquals(GrokMode.FUN, grokVm.uiState.value.mode)

        grokVm.toggleFeedContext(false)
        assertFalse(grokVm.uiState.value.attachFeedContext)
    }
}
