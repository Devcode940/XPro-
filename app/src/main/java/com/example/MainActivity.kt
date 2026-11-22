package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import androidx.paging.compose.collectAsLazyPagingItems
import com.devcode.xpro.mediadownloader.DownloadStatus
import com.example.ai.configureAppCheck
import com.example.auth.AuthResultState
import com.example.auth.GoogleAuthHelper
import com.example.auth.SwitchableAccount
import com.example.core.designsystem.AppBottomNavigation
import com.example.core.designsystem.AppDrawerContent
import com.example.core.designsystem.AppNavigationRail
import com.example.core.designsystem.XtraSystemBarsThemeEffect
import com.example.core.designsystem.configureXtraEdgeToEdge
import com.example.core.designsystem.rememberIsKeyboardOpen
import com.example.core.navigation.XtraRoutes
import com.example.core.navigation.navigateSingleTopTo
import com.example.core.scroll.LocalAutoHideUiController
import com.example.core.scroll.LocalBottomBarClearance
import com.example.core.scroll.LocalHasPhoneBottomBar
import com.example.core.scroll.rememberAutoHideUiController
import com.example.data.model.LibraryMediaKind
import com.example.data.model.VerificationBadge
import com.example.feature.bookmarks.BookmarksViewModel
import com.example.feature.bookmarks.XtraBookmarksScreen
import com.example.feature.downloads.DownloadsScreen
import com.example.feature.downloads.DownloadsViewModel
import com.example.feature.media.AudioPlayerScreen
import com.example.feature.media.FavoritesMediaScreen
import com.example.feature.media.MediaCenterViewModel
import com.example.feature.media.MediaLibraryScreen
import com.example.feature.media.PlaylistsScreen
import com.example.feature.media.RecentlyPlayedScreen
import com.example.feature.media.VideoPlayerScreen
import com.example.feature.profile.XtraProfileRouteScreen
import com.example.feature.settings.SettingsScreen
import com.example.feature.settings.SettingsViewModel
import com.example.feature.settings.ThemeMode
import com.example.feature.social.ChatThreadScreen
import com.example.feature.social.CommunitiesScreen
import com.example.feature.social.ListsScreen
import com.example.feature.social.MessagesScreen
import com.example.feature.social.NotificationsScreen
import com.example.feature.social.SearchScreen
import com.example.feature.social.SocialViewModel
import com.example.feature.social.TrendingScreen
import com.example.feature.tools.AboutXtraScreen
import com.example.feature.tools.BrowserHistoryScreen
import com.example.feature.tools.HelpCenterScreen
import com.example.feature.tools.MediaSharingScreen
import com.example.feature.tools.PrivateBrowsingScreen
import com.example.feature.tools.QrScannerScreen
import com.example.feature.tools.SavedPagesScreen
import com.example.feature.tools.SendFeedbackScreen
import com.example.feature.tools.ToolsViewModel
import com.example.ui.components.UndoSendBar
import com.example.ui.components.UserAvatar
import com.example.ui.modals.ArticleStudioDialog
import com.example.ui.modals.ComposerDialog
import com.example.ui.modals.EditHistoryDialog
import com.example.ui.modals.EditPostDialog
import com.example.ui.modals.MediaLightboxDialog
import com.example.ui.modals.NewBookmarkFolderDialog
import com.example.ui.modals.PremiumStudioDialog
import com.example.ui.modals.ReaderModeOverlay
import com.example.ui.modals.SaveToFolderSheet
import com.example.ui.screens.ArticlesScreen
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.GrokScreen
import com.example.ui.screens.HomeFeedScreen
import com.example.ui.screens.TrendsSidebar
import com.example.ui.theme.MonoTabularStyle
import com.example.ui.theme.XProTheme
import com.example.viewmodel.GrokViewModel
import com.example.viewmodel.ProfileViewModel
import com.example.viewmodel.XProViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        configureXtraEdgeToEdge(isDarkTheme = true)
        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
            val settingsDashboard by settingsViewModel.uiState.collectAsStateWithLifecycle()
            val s = settingsDashboard.settings
            val systemDark = isSystemInDarkTheme()
            val effectiveDarkTheme = when (s.themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> systemDark
            }
            XtraSystemBarsThemeEffect(isDarkTheme = effectiveDarkTheme)
            XProTheme(
                themeMode = s.themeMode,
                accentOption = s.accentColor,
                fontSizePreset = s.fontSize,
                displayDensity = s.displayDensity,
                largerText = s.largerText,
                reducedMotion = s.reducedMotion,
                improvedContrast = s.improvedContrast
            ) {
                XProApp(settingsViewModel = settingsViewModel)
            }
        }
    }
}

@Composable
fun XProApp(
    viewModel: XProViewModel = viewModel(),
    profileViewModel: ProfileViewModel = viewModel(),
    grokViewModel: GrokViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel(),
    bookmarksViewModel: BookmarksViewModel = viewModel(),
    downloadsViewModel: DownloadsViewModel = viewModel(),
    socialViewModel: SocialViewModel = viewModel(),
    mediaViewModel: MediaCenterViewModel = viewModel(),
    toolsViewModel: ToolsViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val profileUiState by profileViewModel.uiState.collectAsStateWithLifecycle()
    val grokUiState by grokViewModel.uiState.collectAsStateWithLifecycle()
    val settingsDashboard by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val socialState by socialViewModel.uiState.collectAsStateWithLifecycle()
    val downloadsState by downloadsViewModel.uiState.collectAsStateWithLifecycle()
    val pagedFeedPosts = viewModel.pagedFeedPosts.collectAsLazyPagingItems()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // Auth state (Guest mode by default; Firestore bookmark sync gated behind Google Sign-In)
    var authUid by remember {
        mutableStateOf(GoogleAuthHelper.currentFirebaseUserOrNull(context)?.uid)
    }
    var authEmail by remember {
        mutableStateOf(GoogleAuthHelper.currentFirebaseUserOrNull(context)?.email)
    }

    val switchableAccounts = remember {
        mutableStateListOf(
            SwitchableAccount(
                id = "acct_elena",
                displayName = "Elena Vance",
                handle = "@elenavance",
                bio = "Systems architect & XTRA Creator · Building local-first Android experiences.",
                isGuest = true
            ),
            SwitchableAccount(
                id = "acct_sandbox",
                displayName = "XTRA Dev Sandbox",
                handle = "@xtra_dev",
                bio = "Testing XTRA Media Center, Offline Room cache & Compose M3 themes.",
                isGuest = true
            )
        )
    }
    var showSwitchAccountDialog by rememberSaveable { mutableStateOf(false) }
    var showAddAccountDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (authUid == null) {
            when (val res = GoogleAuthHelper.attemptAutoSignIn(context)) {
                is AuthResultState.Authenticated -> {
                    authUid = res.uid
                    authEmail = res.email
                }
                else -> Unit
            }
        }
    }

    LaunchedEffect(authUid) {
        bookmarksViewModel.setAuthenticatedUid(authUid)
        viewModel.setCloudSyncUid(authUid)
    }

    val activeDownloadsCount = downloadsState.allDownloads.count {
        it.status == DownloadStatus.QUEUED || it.status == DownloadStatus.DOWNLOADING
    }

    val navigateToRoute: (String) -> Unit = { route ->
        scope.launch {
            if (drawerState.isOpen) drawerState.close()
        }
        navController.navigateSingleTopTo(route)
    }

    val openDrawer: () -> Unit = {
        scope.launch { drawerState.open() }
    }

    val openOwnProfile: () -> Unit = {
        profileViewModel.loadProfile("current_user")
        navigateToRoute(XtraRoutes.profile("current_user"))
    }

    val openAuthorProfile: (String) -> Unit = { authorId ->
        profileViewModel.loadProfile(authorId)
        navigateToRoute(XtraRoutes.profile(authorId))
    }

    val triggerGoogleSignIn: () -> Unit = {
        scope.launch {
            if (drawerState.isOpen) drawerState.close()
            when (val result = GoogleAuthHelper.signInWithGoogle(context)) {
                is AuthResultState.Authenticated -> {
                    authUid = result.uid
                    authEmail = result.email
                    val name = result.displayName ?: result.email?.substringBefore("@") ?: "Google User"
                    viewModel.showToast("Signed in as ${result.email ?: name} · Cloud sync active")
                }
                is AuthResultState.Error -> viewModel.showToast(result.message)
                AuthResultState.Cancelled -> Unit
                AuthResultState.NoSavedCredential -> viewModel.showToast("No Google credential found on device")
            }
        }
    }

    val triggerLogOut: () -> Unit = {
        scope.launch {
            if (drawerState.isOpen) drawerState.close()
            GoogleAuthHelper.signOut(context)
            authUid = null
            authEmail = null
            viewModel.showToast("Logged out · Guest mode active")
            navController.navigateSingleTopTo(XtraRoutes.HOME)
        }
    }

    LaunchedEffect(uiState.toastMessage) {
        val msg = uiState.toastMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearToast()
        }
    }

    val colorScheme = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val autoHideController = rememberAutoHideUiController()
    val isKeyboardOpen by rememberIsKeyboardOpen()
    val reducedMotion = settingsDashboard.settings.reducedMotion
    var bottomBarHeightPx by remember { mutableIntStateOf(0) }
    val bottomBarClearanceDp = with(density) { bottomBarHeightPx.toDp() }

    val forceFixedBarsRoute = currentRoute == XtraRoutes.GROK ||
        currentRoute?.startsWith("chat/") == true ||
        currentRoute == XtraRoutes.VIDEO_PLAYER ||
        currentRoute == XtraRoutes.AUDIO_PLAYER ||
        currentRoute == XtraRoutes.PRIVATE_BROWSING ||
        currentRoute == XtraRoutes.QR_SCANNER ||
        currentRoute == XtraRoutes.DOWNLOADS ||
        currentRoute == XtraRoutes.DOWNLOAD_MANAGER

    val hasOpenOverlay = uiState.showComposerSheet ||
        uiState.showArticleStudio ||
        uiState.showPremiumStudio ||
        uiState.readerPost != null ||
        uiState.editingPost != null ||
        uiState.historyPost != null ||
        uiState.bookmarkTargetPost != null ||
        showSwitchAccountDialog ||
        showAddAccountDialog ||
        isKeyboardOpen

    LaunchedEffect(forceFixedBarsRoute, hasOpenOverlay) {
        if (forceFixedBarsRoute || hasOpenOverlay) {
            autoHideController.forceShowAll()
        }
    }

    LaunchedEffect(currentRoute) {
        autoHideController.forceShowAll()
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        val usePermanentDrawer = maxWidth >= 1024.dp
        val useRail = maxWidth >= 600.dp && !usePermanentDrawer
        val showThreeColumn = maxWidth >= 960.dp

        val isPhoneTab = currentRoute in XtraRoutes.topLevelTabRoutes
        val showPhoneBottomBar = !useRail && !usePermanentDrawer && (isPhoneTab || currentRoute == null)
        val showFab = !useRail && !usePermanentDrawer &&
            (currentRoute == XtraRoutes.HOME || currentRoute == XtraRoutes.EXPLORE || currentRoute == null)
        val animDuration = if (reducedMotion) 0 else 220

        val drawerContent: @Composable () -> Unit = {
            AppDrawerContent(
                currentRoute = currentRoute,
                userProfile = uiState.userProfile,
                isAuthenticated = !authUid.isNullOrBlank(),
                authEmail = authEmail,
                unreadNotificationsCount = socialState.unreadNotificationsCount,
                unreadMessagesCount = socialState.unreadMessagesCount,
                activeDownloadsCount = activeDownloadsCount,
                onNavigate = navigateToRoute,
                onSwitchAccount = {
                    scope.launch { drawerState.close() }
                    showSwitchAccountDialog = true
                },
                onAddAccount = {
                    scope.launch { drawerState.close() }
                    showAddAccountDialog = true
                },
                onSignInClick = triggerGoogleSignIn,
                onLogOutClick = triggerLogOut
            )
        }

        val mainScaffold: @Composable () -> Unit = {
            CompositionLocalProvider(
                LocalAutoHideUiController provides autoHideController,
                LocalHasPhoneBottomBar provides showPhoneBottomBar,
                LocalBottomBarClearance provides if (showPhoneBottomBar) bottomBarClearanceDp else 0.dp
            ) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = colorScheme.background,
                    contentWindowInsets = WindowInsets(0, 0, 0, 0)
                ) { _ ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (useRail) {
                                AppNavigationRail(
                                    currentRoute = currentRoute,
                                    userProfile = uiState.userProfile,
                                    unreadNotificationsCount = socialState.unreadNotificationsCount,
                                    unreadMessagesCount = socialState.unreadMessagesCount,
                                    onNavigate = navigateToRoute,
                                    onOpenDrawer = openDrawer,
                                    onCompose = { viewModel.openComposer() }
                                )
                                VerticalDivider(color = colorScheme.outline, thickness = 0.8.dp)
                            }

                        Box(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .widthIn(max = 680.dp)
                                .fillMaxHeight()
                        ) {
                            NavHost(
                                navController = navController,
                                startDestination = XtraRoutes.HOME,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                composable(
                                    route = XtraRoutes.HOME,
                                    deepLinks = listOf(navDeepLink { uriPattern = "xtra://home" })
                                ) {
                                    HomeFeedScreen(
                                        uiState = uiState,
                                        pagedPosts = pagedFeedPosts,
                                        onSelectFeedTab = viewModel::selectFeedTab,
                                        onSelectFeedFilter = viewModel::selectFeedFilter,
                                        onOpenProfile = openOwnProfile,
                                        onOpenAuthorProfile = openAuthorProfile,
                                        onOpenPremiumStudio = viewModel::openPremiumStudio,
                                        onOpenSettings = { navigateToRoute(XtraRoutes.settings()) },
                                        onOpenDrawer = openDrawer,
                                        onOpenGrok = { navigateToRoute(XtraRoutes.GROK) },
                                        onOpenBookmarks = { navigateToRoute(XtraRoutes.BOOKMARKS) },
                                        onOpenArticles = { navigateToRoute(XtraRoutes.ARTICLES) },
                                        onOpenMediaDownloader = { navigateToRoute(XtraRoutes.DOWNLOAD_MANAGER) },
                                        onToggleOfflineMode = viewModel::toggleOfflineMode,
                                        onToggleSaveForOffline = viewModel::toggleSaveForOffline,
                                        onToggleOfflineRead = viewModel::toggleOfflineRead,
                                        onCacheAllThreads = viewModel::cacheAllThreadsAndArticlesForOffline,
                                        onLike = viewModel::toggleLike,
                                        onRepost = viewModel::toggleRepost,
                                        onReply = { post -> viewModel.openComposer(replyTo = post) },
                                        onBookmark = viewModel::openBookmarkSheet,
                                        onVotePoll = viewModel::votePoll,
                                        onOpenReaderMode = viewModel::openReaderMode,
                                        onOpenEdit = viewModel::openEditPost,
                                        onOpenEditHistory = viewModel::openEditHistory,
                                        onToggleHighlight = viewModel::toggleHighlight,
                                        onDeletePost = viewModel::deletePost,
                                        onOpenMediaLightbox = { post -> viewModel.openLightbox(post.mediaResKey, post.mediaUri) }
                                    )
                                }

                                composable(
                                    route = XtraRoutes.EXPLORE,
                                    deepLinks = listOf(navDeepLink { uriPattern = "xtra://explore" })
                                ) {
                                    ExploreScreen(
                                        uiState = uiState,
                                        onSearchQueryChange = viewModel::updateSearchQuery,
                                        onOpenPremiumStudio = viewModel::openPremiumStudio,
                                        onOpenSettings = { navigateToRoute(XtraRoutes.settings()) },
                                        onOpenDrawer = openDrawer,
                                        onOpenGrok = { navigateToRoute(XtraRoutes.GROK) },
                                        onOpenBookmarks = { navigateToRoute(XtraRoutes.BOOKMARKS) },
                                        onOpenArticles = { navigateToRoute(XtraRoutes.ARTICLES) },
                                        onOpenMediaDownloader = { navigateToRoute(XtraRoutes.DOWNLOAD_MANAGER) },
                                        onToggleFollowUser = viewModel::toggleFollowUser,
                                        onOpenProfile = openOwnProfile,
                                        onOpenAuthorProfile = openAuthorProfile,
                                        onToggleSaveForOffline = viewModel::toggleSaveForOffline,
                                        onToggleOfflineRead = viewModel::toggleOfflineRead,
                                        onLike = viewModel::toggleLike,
                                        onRepost = viewModel::toggleRepost,
                                        onReply = { post -> viewModel.openComposer(replyTo = post) },
                                        onBookmark = viewModel::openBookmarkSheet,
                                        onVotePoll = viewModel::votePoll,
                                        onOpenReaderMode = viewModel::openReaderMode,
                                        onOpenEdit = viewModel::openEditPost,
                                        onOpenEditHistory = viewModel::openEditHistory,
                                        onToggleHighlight = viewModel::toggleHighlight,
                                        onDeletePost = viewModel::deletePost,
                                        onOpenMediaLightbox = { post -> viewModel.openLightbox(post.mediaResKey, post.mediaUri) }
                                    )
                                }

                                composable(
                                    route = XtraRoutes.SEARCH,
                                    deepLinks = listOf(navDeepLink { uriPattern = "xtra://search" })
                                ) {
                                    SearchScreen(
                                        socialViewModel = socialViewModel,
                                        userProfile = uiState.userProfile,
                                        folders = uiState.bookmarkFolders,
                                        onOpenDrawer = openDrawer,
                                        onBack = { navController.popBackStack() },
                                        onOpenAuthorProfile = openAuthorProfile,
                                        onLike = viewModel::toggleLike,
                                        onRepost = viewModel::toggleRepost,
                                        onReply = { post -> viewModel.openComposer(replyTo = post) },
                                        onBookmark = viewModel::openBookmarkSheet,
                                        onVotePoll = viewModel::votePoll,
                                        onOpenReaderMode = viewModel::openReaderMode,
                                        onOpenEdit = viewModel::openEditPost,
                                        onOpenEditHistory = viewModel::openEditHistory,
                                        onToggleHighlight = viewModel::toggleHighlight,
                                        onDeletePost = viewModel::deletePost,
                                        onOpenMediaLightbox = { post -> viewModel.openLightbox(post.mediaResKey, post.mediaUri) },
                                        onToggleSaveForOffline = viewModel::toggleSaveForOffline,
                                        onToggleOfflineRead = viewModel::toggleOfflineRead
                                    )
                                }

                                composable(
                                    route = XtraRoutes.NOTIFICATIONS,
                                    deepLinks = listOf(navDeepLink { uriPattern = "xtra://notifications" })
                                ) {
                                    NotificationsScreen(
                                        socialViewModel = socialViewModel,
                                        userProfile = uiState.userProfile,
                                        onOpenDrawer = openDrawer,
                                        onOpenPostById = { postId -> viewModel.openReaderByPostId(postId) },
                                        onOpenDownloads = { navigateToRoute(XtraRoutes.DOWNLOADS) },
                                        onOpenMessages = { navigateToRoute(XtraRoutes.MESSAGES) }
                                    )
                                }

                                composable(
                                    route = XtraRoutes.MESSAGES,
                                    deepLinks = listOf(navDeepLink { uriPattern = "xtra://messages" })
                                ) {
                                    MessagesScreen(
                                        socialViewModel = socialViewModel,
                                        userProfile = uiState.userProfile,
                                        onOpenDrawer = openDrawer,
                                        onOpenConversation = { convId ->
                                            navigateToRoute(XtraRoutes.chatThread(convId))
                                        }
                                    )
                                }

                                composable(
                                    route = XtraRoutes.CHAT_THREAD,
                                    arguments = listOf(navArgument("conversationId") { type = NavType.StringType }),
                                    deepLinks = listOf(navDeepLink { uriPattern = "xtra://chat/{conversationId}" })
                                ) { entry ->
                                    val convId = entry.arguments?.getString("conversationId") ?: "conv_kairo"
                                    ChatThreadScreen(
                                        conversationId = convId,
                                        socialViewModel = socialViewModel,
                                        userProfile = uiState.userProfile,
                                        onBack = { navController.popBackStack() },
                                        onOpenDrawer = openDrawer
                                    )
                                }

                                composable(
                                    route = XtraRoutes.BOOKMARKS,
                                    deepLinks = listOf(navDeepLink { uriPattern = "xtra://bookmarks" })
                                ) {
                                    key(authUid) {
                                        XtraBookmarksScreen(
                                            bookmarksViewModel = bookmarksViewModel,
                                            userProfile = uiState.userProfile,
                                            onOpenDrawer = openDrawer,
                                            onBack = { navController.popBackStack() },
                                            onOpenAuthorProfile = openAuthorProfile,
                                            onLike = viewModel::toggleLike,
                                            onRepost = viewModel::toggleRepost,
                                            onReply = { post -> viewModel.openComposer(replyTo = post) },
                                            onBookmarkPost = viewModel::openBookmarkSheet,
                                            onVotePoll = viewModel::votePoll,
                                            onOpenReaderMode = viewModel::openReaderMode,
                                            onOpenEdit = viewModel::openEditPost,
                                            onOpenEditHistory = viewModel::openEditHistory,
                                            onToggleHighlight = viewModel::toggleHighlight,
                                            onDeletePost = viewModel::deletePost,
                                            onOpenMediaLightbox = { post -> viewModel.openLightbox(post.mediaResKey, post.mediaUri) },
                                            onToggleSaveForOffline = viewModel::toggleSaveForOffline,
                                            onToggleOfflineRead = viewModel::toggleOfflineRead,
                                            onOpenVideoUrl = { url ->
                                                mediaViewModel.setCustomStreamUrl(url)
                                                navigateToRoute(XtraRoutes.VIDEO_PLAYER)
                                            },
                                            onSignInClick = triggerGoogleSignIn
                                        )
                                    }
                                }

                                composable(
                                    route = XtraRoutes.LISTS,
                                    deepLinks = listOf(navDeepLink { uriPattern = "xtra://lists" })
                                ) {
                                    ListsScreen(
                                        socialViewModel = socialViewModel,
                                        userProfile = uiState.userProfile,
                                        onOpenDrawer = openDrawer,
                                        onBack = { navController.popBackStack() }
                                    )
                                }

                                composable(
                                    route = XtraRoutes.COMMUNITIES,
                                    deepLinks = listOf(navDeepLink { uriPattern = "xtra://communities" })
                                ) {
                                    CommunitiesScreen(
                                        socialViewModel = socialViewModel,
                                        userProfile = uiState.userProfile,
                                        onOpenDrawer = openDrawer,
                                        onBack = { navController.popBackStack() }
                                    )
                                }

                                composable(
                                    route = XtraRoutes.TRENDING,
                                    deepLinks = listOf(navDeepLink { uriPattern = "xtra://trending" })
                                ) {
                                    TrendingScreen(
                                        socialViewModel = socialViewModel,
                                        userProfile = uiState.userProfile,
                                        onOpenDrawer = openDrawer,
                                        onBack = { navController.popBackStack() },
                                        onSelectTopic = { topic ->
                                            socialViewModel.updateSearchQuery(topic)
                                            navigateToRoute(XtraRoutes.SEARCH)
                                        }
                                    )
                                }

                                composable(
                                    route = XtraRoutes.ARTICLES,
                                    deepLinks = listOf(navDeepLink { uriPattern = "xtra://articles" })
                                ) {
                                    ArticlesScreen(
                                        uiState = uiState,
                                        onOpenArticleStudio = viewModel::openArticleStudio,
                                        onOpenProfile = openOwnProfile,
                                        onOpenDrawer = openDrawer,
                                        onOpenSettings = { navigateToRoute(XtraRoutes.settings()) },
                                        onOpenGrok = { navigateToRoute(XtraRoutes.GROK) },
                                        onOpenBookmarks = { navigateToRoute(XtraRoutes.BOOKMARKS) },
                                        onOpenMediaDownloader = { navigateToRoute(XtraRoutes.DOWNLOAD_MANAGER) },
                                        onOpenAuthorProfile = openAuthorProfile,
                                        onToggleSaveForOffline = viewModel::toggleSaveForOffline,
                                        onToggleOfflineRead = viewModel::toggleOfflineRead,
                                        onLike = viewModel::toggleLike,
                                        onRepost = viewModel::toggleRepost,
                                        onReply = { post -> viewModel.openComposer(replyTo = post) },
                                        onBookmark = viewModel::openBookmarkSheet,
                                        onVotePoll = viewModel::votePoll,
                                        onOpenReaderMode = viewModel::openReaderMode,
                                        onOpenEdit = viewModel::openEditPost,
                                        onOpenEditHistory = viewModel::openEditHistory,
                                        onToggleHighlight = viewModel::toggleHighlight,
                                        onDeletePost = viewModel::deletePost,
                                        onOpenMediaLightbox = { post -> viewModel.openLightbox(post.mediaResKey, post.mediaUri) }
                                    )
                                }

                                composable(
                                    route = XtraRoutes.GROK,
                                    deepLinks = listOf(navDeepLink { uriPattern = "xtra://grok" })
                                ) {
                                    GrokScreen(
                                        grokState = grokUiState,
                                        userProfile = uiState.userProfile,
                                        onSendMessage = { prompt -> grokViewModel.sendMessage(prompt) },
                                        onSelectMode = grokViewModel::setMode,
                                        onToggleFeedContext = grokViewModel::toggleFeedContext,
                                        onRetryLast = grokViewModel::retryLastMessage,
                                        onDismissError = grokViewModel::dismissError,
                                        onClearChat = grokViewModel::clearConversation,
                                        onCopiedToast = { viewModel.showToast("Grok response copied") },
                                        onOpenProfile = openOwnProfile,
                                        onOpenDrawer = openDrawer,
                                        onOpenSettings = { navigateToRoute(XtraRoutes.settings()) },
                                        onOpenBookmarks = { navigateToRoute(XtraRoutes.BOOKMARKS) },
                                        onOpenArticles = { navigateToRoute(XtraRoutes.ARTICLES) },
                                        onOpenMediaDownloader = { navigateToRoute(XtraRoutes.DOWNLOAD_MANAGER) }
                                    )
                                }

                                composable(
                                    route = XtraRoutes.PROFILE,
                                    arguments = listOf(navArgument("userId") { type = NavType.StringType }),
                                    deepLinks = listOf(navDeepLink { uriPattern = "xtra://profile/{userId}" })
                                ) { entry ->
                                    val userId = entry.arguments?.getString("userId") ?: "current_user"
                                    LaunchedEffect(userId) {
                                        profileViewModel.loadProfile(userId)
                                    }
                                    XtraProfileRouteScreen(
                                        profileState = profileUiState,
                                        onSelectProfileTab = profileViewModel::selectTab,
                                        onRetryProfile = profileViewModel::retry,
                                        onBackToOwnProfile = openOwnProfile,
                                        onBackToHome = { navigateToRoute(XtraRoutes.HOME) },
                                        onToggleFollowViewedUser = profileViewModel::toggleFollowViewedUser,
                                        onOpenPremiumStudio = viewModel::openPremiumStudio,
                                        onOpenAppSettings = { navigateToRoute(XtraRoutes.settings()) },
                                        onOpenGrok = { navigateToRoute(XtraRoutes.GROK) },
                                        onOpenBookmarks = { navigateToRoute(XtraRoutes.BOOKMARKS) },
                                        onOpenArticles = { navigateToRoute(XtraRoutes.ARTICLES) },
                                        onOpenMediaDownloader = { navigateToRoute(XtraRoutes.DOWNLOAD_MANAGER) },
                                        onToggleAdFree = profileViewModel::toggleAdFree,
                                        onUpdateUndoDelay = profileViewModel::updateUndoDelay,
                                        onToggleEditHistoryLinks = profileViewModel::toggleEditHistoryLinks,
                                        onToggleAutoExpandThreads = profileViewModel::toggleAutoExpandThreads,
                                        onToggleSaveForOffline = viewModel::toggleSaveForOffline,
                                        onToggleOfflineRead = viewModel::toggleOfflineRead,
                                        onLike = viewModel::toggleLike,
                                        onRepost = viewModel::toggleRepost,
                                        onReply = { post -> viewModel.openComposer(replyTo = post) },
                                        onBookmark = viewModel::openBookmarkSheet,
                                        onVotePoll = viewModel::votePoll,
                                        onOpenReaderMode = viewModel::openReaderMode,
                                        onOpenEdit = viewModel::openEditPost,
                                        onOpenEditHistory = viewModel::openEditHistory,
                                        onToggleHighlight = viewModel::toggleHighlight,
                                        onDeletePost = viewModel::deletePost,
                                        onOpenMediaLightbox = { post -> viewModel.openLightbox(post.mediaResKey, post.mediaUri) }
                                    )
                                }

                                // Media Center Destinations
                                composable(
                                    route = XtraRoutes.MEDIA_LIBRARY,
                                    deepLinks = listOf(navDeepLink { uriPattern = "xtra://media_library" })
                                ) {
                                    MediaLibraryScreen(
                                        mediaViewModel = mediaViewModel,
                                        userProfile = uiState.userProfile,
                                        onOpenDrawer = openDrawer,
                                        onBack = { navController.popBackStack() },
                                        onPlayVideo = { navigateToRoute(XtraRoutes.VIDEO_PLAYER) },
                                        onPlayAudio = { navigateToRoute(XtraRoutes.AUDIO_PLAYER) },
                                        onSendToDownloader = { url ->
                                            downloadsViewModel.onUrlChanged(url)
                                            navigateToRoute(XtraRoutes.DOWNLOAD_MANAGER)
                                        }
                                    )
                                }

                                composable(
                                    route = XtraRoutes.DOWNLOADS,
                                    deepLinks = listOf(navDeepLink { uriPattern = "xtra://downloads" })
                                ) {
                                    DownloadsScreen(
                                        downloadsViewModel = downloadsViewModel,
                                        userProfile = uiState.userProfile,
                                        showUrlComposerExpanded = false,
                                        onOpenDrawer = openDrawer,
                                        onBack = { navController.popBackStack() },
                                        onOpenInPlayer = { url, isVideo ->
                                            mediaViewModel.setCustomStreamUrl(url)
                                            navigateToRoute(if (isVideo) XtraRoutes.VIDEO_PLAYER else XtraRoutes.AUDIO_PLAYER)
                                        }
                                    )
                                }

                                composable(
                                    route = XtraRoutes.DOWNLOAD_MANAGER,
                                    deepLinks = listOf(navDeepLink { uriPattern = "xtra://download_manager" })
                                ) {
                                    DownloadsScreen(
                                        downloadsViewModel = downloadsViewModel,
                                        userProfile = uiState.userProfile,
                                        showUrlComposerExpanded = true,
                                        onOpenDrawer = openDrawer,
                                        onBack = { navController.popBackStack() },
                                        onOpenInPlayer = { url, isVideo ->
                                            mediaViewModel.setCustomStreamUrl(url)
                                            navigateToRoute(if (isVideo) XtraRoutes.VIDEO_PLAYER else XtraRoutes.AUDIO_PLAYER)
                                        }
                                    )
                                }

                                composable(
                                    route = XtraRoutes.VIDEO_PLAYER,
                                    deepLinks = listOf(navDeepLink { uriPattern = "xtra://video_player" })
                                ) {
                                    VideoPlayerScreen(
                                        mediaViewModel = mediaViewModel,
                                        userProfile = uiState.userProfile,
                                        onOpenDrawer = openDrawer,
                                        onBack = { navController.popBackStack() },
                                        onSendToDownloader = { url ->
                                            downloadsViewModel.onUrlChanged(url)
                                            navigateToRoute(XtraRoutes.DOWNLOAD_MANAGER)
                                        }
                                    )
                                }

                                composable(
                                    route = XtraRoutes.AUDIO_PLAYER,
                                    deepLinks = listOf(navDeepLink { uriPattern = "xtra://audio_player" })
                                ) {
                                    AudioPlayerScreen(
                                        mediaViewModel = mediaViewModel,
                                        userProfile = uiState.userProfile,
                                        onOpenDrawer = openDrawer,
                                        onBack = { navController.popBackStack() },
                                        onSendToDownloader = { url ->
                                            downloadsViewModel.onUrlChanged(url)
                                            navigateToRoute(XtraRoutes.DOWNLOAD_MANAGER)
                                        }
                                    )
                                }

                                composable(
                                    route = XtraRoutes.PLAYLISTS,
                                    deepLinks = listOf(navDeepLink { uriPattern = "xtra://playlists" })
                                ) {
                                    PlaylistsScreen(
                                        mediaViewModel = mediaViewModel,
                                        userProfile = uiState.userProfile,
                                        onOpenDrawer = openDrawer,
                                        onBack = { navController.popBackStack() },
                                        onPlayItem = { item ->
                                            navigateToRoute(
                                                if (item.mediaKind == LibraryMediaKind.VIDEO) XtraRoutes.VIDEO_PLAYER else XtraRoutes.AUDIO_PLAYER
                                            )
                                        }
                                    )
                                }

                                composable(
                                    route = XtraRoutes.RECENTLY_PLAYED,
                                    deepLinks = listOf(navDeepLink { uriPattern = "xtra://recently_played" })
                                ) {
                                    RecentlyPlayedScreen(
                                        mediaViewModel = mediaViewModel,
                                        userProfile = uiState.userProfile,
                                        onOpenDrawer = openDrawer,
                                        onBack = { navController.popBackStack() },
                                        onPlayItem = { item ->
                                            navigateToRoute(
                                                if (item.mediaKind == LibraryMediaKind.VIDEO) XtraRoutes.VIDEO_PLAYER else XtraRoutes.AUDIO_PLAYER
                                            )
                                        },
                                        onSendToDownloader = { url ->
                                            downloadsViewModel.onUrlChanged(url)
                                            navigateToRoute(XtraRoutes.DOWNLOAD_MANAGER)
                                        }
                                    )
                                }

                                composable(
                                    route = XtraRoutes.FAVORITES,
                                    deepLinks = listOf(navDeepLink { uriPattern = "xtra://favorites" })
                                ) {
                                    FavoritesMediaScreen(
                                        mediaViewModel = mediaViewModel,
                                        userProfile = uiState.userProfile,
                                        onOpenDrawer = openDrawer,
                                        onBack = { navController.popBackStack() },
                                        onPlayItem = { item ->
                                            navigateToRoute(
                                                if (item.mediaKind == LibraryMediaKind.VIDEO) XtraRoutes.VIDEO_PLAYER else XtraRoutes.AUDIO_PLAYER
                                            )
                                        },
                                        onSendToDownloader = { url ->
                                            downloadsViewModel.onUrlChanged(url)
                                            navigateToRoute(XtraRoutes.DOWNLOAD_MANAGER)
                                        }
                                    )
                                }

                                // Application Tools Destinations
                                composable(
                                    route = XtraRoutes.QR_SCANNER,
                                    deepLinks = listOf(navDeepLink { uriPattern = "xtra://qr_scanner" })
                                ) {
                                    QrScannerScreen(
                                        userProfile = uiState.userProfile,
                                        onOpenDrawer = openDrawer,
                                        onBack = { navController.popBackStack() },
                                        onNavigateRoute = navigateToRoute,
                                        onOpenPrivateUrl = { url ->
                                            toolsViewModel.openPrivateUrl(url)
                                            navigateToRoute(XtraRoutes.PRIVATE_BROWSING)
                                        }
                                    )
                                }

                                composable(
                                    route = XtraRoutes.SAVED_PAGES,
                                    deepLinks = listOf(navDeepLink { uriPattern = "xtra://saved_pages" })
                                ) {
                                    SavedPagesScreen(
                                        toolsViewModel = toolsViewModel,
                                        userProfile = uiState.userProfile,
                                        readingListOnly = false,
                                        onOpenDrawer = openDrawer,
                                        onBack = { navController.popBackStack() },
                                        onOpenInPrivateBrowser = { url ->
                                            toolsViewModel.openPrivateUrl(url)
                                            navigateToRoute(XtraRoutes.PRIVATE_BROWSING)
                                        }
                                    )
                                }

                                composable(
                                    route = XtraRoutes.READING_LIST,
                                    deepLinks = listOf(navDeepLink { uriPattern = "xtra://reading_list" })
                                ) {
                                    SavedPagesScreen(
                                        toolsViewModel = toolsViewModel,
                                        userProfile = uiState.userProfile,
                                        readingListOnly = true,
                                        onOpenDrawer = openDrawer,
                                        onBack = { navController.popBackStack() },
                                        onOpenInPrivateBrowser = { url ->
                                            toolsViewModel.openPrivateUrl(url)
                                            navigateToRoute(XtraRoutes.PRIVATE_BROWSING)
                                        }
                                    )
                                }

                                composable(
                                    route = XtraRoutes.BROWSER_HISTORY,
                                    deepLinks = listOf(navDeepLink { uriPattern = "xtra://browser_history" })
                                ) {
                                    BrowserHistoryScreen(
                                        toolsViewModel = toolsViewModel,
                                        userProfile = uiState.userProfile,
                                        onOpenDrawer = openDrawer,
                                        onBack = { navController.popBackStack() },
                                        onOpenInPrivateBrowser = { url ->
                                            toolsViewModel.openPrivateUrl(url)
                                            navigateToRoute(XtraRoutes.PRIVATE_BROWSING)
                                        }
                                    )
                                }

                                composable(
                                    route = XtraRoutes.PRIVATE_BROWSING,
                                    deepLinks = listOf(navDeepLink { uriPattern = "xtra://private_browsing" })
                                ) {
                                    PrivateBrowsingScreen(
                                        toolsViewModel = toolsViewModel,
                                        userProfile = uiState.userProfile,
                                        onOpenDrawer = openDrawer,
                                        onBack = { navController.popBackStack() }
                                    )
                                }

                                composable(
                                    route = XtraRoutes.MEDIA_SHARING,
                                    deepLinks = listOf(navDeepLink { uriPattern = "xtra://media_sharing" })
                                ) {
                                    MediaSharingScreen(
                                        userProfile = uiState.userProfile,
                                        onOpenDrawer = openDrawer,
                                        onBack = { navController.popBackStack() },
                                        onOpenComposerWithMedia = { uri ->
                                            viewModel.openComposerWithMedia(uri)
                                        }
                                    )
                                }

                                // Settings & Support Destinations
                                composable(
                                    route = XtraRoutes.SETTINGS_ROUTE,
                                    arguments = listOf(
                                        navArgument("section") {
                                            type = NavType.StringType
                                            defaultValue = XtraRoutes.SETTINGS_OVERVIEW
                                        }
                                    ),
                                    deepLinks = listOf(navDeepLink { uriPattern = "xtra://settings?section={section}" })
                                ) { entry ->
                                    val section = entry.arguments?.getString("section") ?: XtraRoutes.SETTINGS_OVERVIEW
                                    SettingsScreen(
                                        state = settingsDashboard,
                                        userProfile = uiState.userProfile,
                                        isAuthenticated = !authUid.isNullOrBlank(),
                                        authEmail = authEmail,
                                        initialSection = section,
                                        viewModel = settingsViewModel,
                                        onOpenDrawer = openDrawer,
                                        onBack = { navController.popBackStack() },
                                        onEditProfile = viewModel::openPremiumStudio,
                                        onSignInClick = triggerGoogleSignIn,
                                        onSignOutClick = triggerLogOut,
                                        onOpenDownloads = { navigateToRoute(XtraRoutes.DOWNLOAD_MANAGER) }
                                    )
                                }

                                composable(
                                    route = XtraRoutes.HELP_CENTER,
                                    deepLinks = listOf(navDeepLink { uriPattern = "xtra://help_center" })
                                ) {
                                    HelpCenterScreen(
                                        userProfile = uiState.userProfile,
                                        onOpenDrawer = openDrawer,
                                        onBack = { navController.popBackStack() },
                                        onOpenSendFeedback = { navigateToRoute(XtraRoutes.SEND_FEEDBACK) }
                                    )
                                }

                                composable(
                                    route = XtraRoutes.ABOUT_XTRA,
                                    deepLinks = listOf(navDeepLink { uriPattern = "xtra://about_xtra" })
                                ) {
                                    AboutXtraScreen(
                                        userProfile = uiState.userProfile,
                                        onOpenDrawer = openDrawer,
                                        onBack = { navController.popBackStack() },
                                        onOpenHelpCenter = { navigateToRoute(XtraRoutes.HELP_CENTER) },
                                        onOpenSendFeedback = { navigateToRoute(XtraRoutes.SEND_FEEDBACK) }
                                    )
                                }

                                composable(
                                    route = XtraRoutes.SEND_FEEDBACK,
                                    deepLinks = listOf(navDeepLink { uriPattern = "xtra://send_feedback" })
                                ) {
                                    SendFeedbackScreen(
                                        toolsViewModel = toolsViewModel,
                                        userProfile = uiState.userProfile,
                                        onOpenDrawer = openDrawer,
                                        onBack = { navController.popBackStack() }
                                    )
                                }
                            }
                        }

                        if (showThreeColumn) {
                            VerticalDivider(color = colorScheme.outline, thickness = 0.8.dp)
                            TrendsSidebar(
                                uiState = uiState,
                                onToggleAdFree = viewModel::toggleAdFreeMode,
                                onOpenPremiumStudio = viewModel::openPremiumStudio,
                                onOpenMediaDownloader = { navigateToRoute(XtraRoutes.DOWNLOAD_MANAGER) },
                                onOpenAuthorProfile = openAuthorProfile,
                                onSearchTrend = { topic ->
                                    socialViewModel.updateSearchQuery(topic)
                                    navigateToRoute(XtraRoutes.SEARCH)
                                },
                                onToggleFollowUser = viewModel::toggleFollowUser,
                                modifier = Modifier.width(320.dp)
                            )
                        }
                    }

                        // Floating auto-hiding Bottom Navigation (phone layout)
                        AnimatedVisibility(
                            visible = showPhoneBottomBar && autoHideController.isBottomBarVisible && !isKeyboardOpen,
                            enter = slideInVertically(
                                animationSpec = tween(durationMillis = animDuration, easing = FastOutSlowInEasing),
                                initialOffsetY = { it }
                            ) + fadeIn(animationSpec = tween(durationMillis = animDuration)),
                            exit = slideOutVertically(
                                animationSpec = tween(durationMillis = animDuration, easing = FastOutSlowInEasing),
                                targetOffsetY = { it }
                            ) + fadeOut(animationSpec = tween(durationMillis = animDuration)),
                            modifier = Modifier.align(Alignment.BottomCenter)
                        ) {
                            AppBottomNavigation(
                                currentRoute = currentRoute ?: XtraRoutes.HOME,
                                unreadNotificationsCount = socialState.unreadNotificationsCount,
                                unreadMessagesCount = socialState.unreadMessagesCount,
                                onNavigate = navigateToRoute,
                                modifier = Modifier.onSizeChanged {
                                    if (it.height > 0) {
                                        bottomBarHeightPx = it.height
                                    }
                                }
                            )
                        }

                        // Floating auto-hiding Compose FAB
                        val fabBottomModifier = if (showPhoneBottomBar && autoHideController.isBottomBarVisible && !isKeyboardOpen && bottomBarClearanceDp > 0.dp) {
                            Modifier
                                .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.End))
                                .padding(bottom = bottomBarClearanceDp + 16.dp, end = 16.dp)
                        } else {
                            Modifier
                                .windowInsetsPadding(
                                    WindowInsets.navigationBars
                                        .union(WindowInsets.ime)
                                        .union(WindowInsets.displayCutout.only(WindowInsetsSides.End + WindowInsetsSides.Bottom))
                                )
                                .padding(16.dp)
                        }
                        AnimatedVisibility(
                            visible = showFab && autoHideController.isBottomBarVisible && !isKeyboardOpen,
                            enter = slideInVertically(
                                animationSpec = tween(durationMillis = animDuration, easing = FastOutSlowInEasing),
                                initialOffsetY = { it }
                            ) + fadeIn(animationSpec = tween(durationMillis = animDuration)),
                            exit = slideOutVertically(
                                animationSpec = tween(durationMillis = animDuration, easing = FastOutSlowInEasing),
                                targetOffsetY = { it }
                            ) + fadeOut(animationSpec = tween(durationMillis = animDuration)),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .then(fabBottomModifier)
                        ) {
                            FloatingActionButton(
                                onClick = { viewModel.openComposer() },
                                containerColor = colorScheme.primary,
                                contentColor = colorScheme.onPrimary,
                                shape = CircleShape,
                                modifier = Modifier.testTag("fab_compose")
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = "Compose post")
                            }
                        }

                        val overlayBottomModifier = if (showPhoneBottomBar && autoHideController.isBottomBarVisible && !isKeyboardOpen && bottomBarClearanceDp > 0.dp) {
                            Modifier
                                .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
                                .padding(bottom = bottomBarClearanceDp + 8.dp)
                        } else {
                            Modifier
                                .windowInsetsPadding(
                                    WindowInsets.navigationBars
                                        .union(WindowInsets.ime)
                                        .union(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                                )
                                .padding(bottom = 8.dp)
                        }
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .widthIn(max = 560.dp)
                                .then(overlayBottomModifier),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            UndoSendBar(
                                pendingPost = uiState.pendingPost,
                                onUndo = viewModel::cancelPendingPost,
                                onSendNow = viewModel::sendPendingPostNow
                            )
                            SnackbarHost(hostState = snackbarHostState) { data ->
                                Snackbar(
                                    snackbarData = data,
                                    containerColor = colorScheme.surfaceContainerHigh,
                                    contentColor = colorScheme.onSurface,
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (usePermanentDrawer) {
            PermanentNavigationDrawer(
                drawerContent = drawerContent,
                content = mainScaffold
            )
        } else {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = drawerContent,
                content = mainScaffold
            )
        }

        // Modals & Overlays
        if (uiState.showComposerSheet) {
            ComposerDialog(
                userProfile = uiState.userProfile,
                replyTo = uiState.replyTargetPost,
                restoredDraft = uiState.restoredDraft,
                onConsumeRestoredDraft = viewModel::consumeRestoredDraft,
                onDismiss = viewModel::closeComposer,
                onPublish = { content, mediaResKey, mediaUri, pollOptions, audience, parentId ->
                    viewModel.submitPostWithUndo(
                        content = content,
                        isLongForm = false,
                        mediaResKey = mediaResKey,
                        mediaUri = mediaUri,
                        pollOptions = pollOptions,
                        audience = audience,
                        parentPostId = parentId
                    )
                }
            )
        }

        if (uiState.showArticleStudio) {
            ArticleStudioDialog(
                userProfile = uiState.userProfile,
                restoredDraft = uiState.restoredDraft,
                onConsumeRestoredDraft = viewModel::consumeRestoredDraft,
                onDismiss = viewModel::closeArticleStudio,
                onPublish = { title, subtitle, body, mediaResKey ->
                    viewModel.submitPostWithUndo(
                        content = body,
                        articleTitle = title,
                        articleSubtitle = subtitle,
                        isLongForm = true,
                        mediaResKey = mediaResKey
                    )
                }
            )
        }

        uiState.editingPost?.let { editing ->
            EditPostDialog(
                post = editing,
                onDismiss = viewModel::closeEditPost,
                onSave = viewModel::savePostEdit
            )
        }

        uiState.historyPost?.let { history ->
            EditHistoryDialog(
                post = history,
                onDismiss = viewModel::closeEditHistory
            )
        }

        uiState.bookmarkTargetPost?.let { target ->
            SaveToFolderSheet(
                post = target,
                folders = uiState.bookmarkFolders,
                onSelectFolder = { folderId -> viewModel.saveBookmarkToFolder(target.id, folderId) },
                onCreateNewFolder = viewModel::openNewFolderDialog,
                onRemoveBookmark = { viewModel.removeBookmark(target.id) },
                onDismiss = viewModel::closeBookmarkSheet
            )
        }

        if (uiState.showNewFolderDialog) {
            NewBookmarkFolderDialog(
                onDismiss = viewModel::closeNewFolderDialog,
                onCreate = viewModel::createBookmarkFolder
            )
        }

        if (uiState.showPremiumStudio) {
            PremiumStudioDialog(
                userProfile = uiState.userProfile,
                blockedPostAds = uiState.blockedPostAds,
                blockedTrendAds = uiState.blockedTrendAds,
                onDismiss = viewModel::closePremiumStudio,
                onSave = viewModel::saveProfileAndPremiumSettings
            )
        }

        if (uiState.lightboxMediaResKey != null || uiState.lightboxMediaUri != null) {
            MediaLightboxDialog(
                mediaResKey = uiState.lightboxMediaResKey,
                mediaUri = uiState.lightboxMediaUri,
                onDismiss = viewModel::closeLightbox
            )
        }

        AnimatedVisibility(
            visible = uiState.readerPost != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            uiState.readerPost?.let { readerPost ->
                ReaderModeOverlay(
                    post = readerPost,
                    fontSizeSp = uiState.readerFontSizeSp,
                    typography = uiState.readerTypography,
                    warmSurface = uiState.readerWarmSurface,
                    onAdjustFontSize = viewModel::updateReaderFontSize,
                    onSelectTypography = viewModel::setReaderTypography,
                    onToggleWarmSurface = viewModel::toggleReaderWarmSurface,
                    onToggleLike = { viewModel.toggleLike(readerPost.id) },
                    onOpenBookmark = { viewModel.openBookmarkSheet(readerPost) },
                    onToggleSaveForOffline = { viewModel.toggleSaveForOffline(readerPost.id) },
                    onClose = viewModel::closeReaderMode
                )
            }
        }

        if (showSwitchAccountDialog) {
            AlertDialog(
                onDismissRequest = { showSwitchAccountDialog = false },
                title = { Text("Switch Account") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        switchableAccounts.forEach { acct ->
                            val isCurrent = uiState.userProfile.handle.equals(acct.handle, ignoreCase = true)
                            Surface(
                                color = if (isCurrent) colorScheme.primaryContainer else colorScheme.surfaceContainer,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, if (isCurrent) colorScheme.primary else colorScheme.outline),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        viewModel.switchActiveAccount(
                                            name = acct.displayName,
                                            handle = acct.handle,
                                            bio = acct.bio,
                                            badge = VerificationBadge.BLUE
                                        )
                                        showSwitchAccountDialog = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    UserAvatar(name = acct.displayName, isCurrentUser = false, avatarColorHex = 0xFF1D9BF0, size = 34.dp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(acct.displayName, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                        Text(acct.handle, style = MonoTabularStyle, color = colorScheme.onSurfaceVariant)
                                    }
                                    if (isCurrent) {
                                        Icon(Icons.Filled.CheckCircle, contentDescription = "Active", tint = colorScheme.primary)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = {
                                showSwitchAccountDialog = false
                                triggerGoogleSignIn()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Sign in with Google Account")
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSwitchAccountDialog = false }) { Text("Close") }
                }
            )
        }

        if (showAddAccountDialog) {
            var name by rememberSaveable { mutableStateOf("") }
            var handle by rememberSaveable { mutableStateOf("") }
            var bio by rememberSaveable { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showAddAccountDialog = false },
                title = { Text("Add XTRA Account") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Display Name") }, singleLine = true)
                        OutlinedTextField(value = handle, onValueChange = { handle = it }, label = { Text("@handle") }, singleLine = true)
                        OutlinedTextField(value = bio, onValueChange = { bio = it }, label = { Text("Bio") })
                        OutlinedButton(
                            onClick = {
                                showAddAccountDialog = false
                                triggerGoogleSignIn()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Connect Google Account instead")
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val normalizedHandle = if (handle.trim().startsWith("@")) handle.trim() else "@${handle.trim()}"
                            val newAcct = SwitchableAccount(
                                id = "acct_${switchableAccounts.size + 1}",
                                displayName = name.trim(),
                                handle = normalizedHandle,
                                bio = bio.trim().ifBlank { "XTRA Creator" },
                                isGuest = true
                            )
                            switchableAccounts.add(newAcct)
                            viewModel.switchActiveAccount(newAcct.displayName, newAcct.handle, newAcct.bio, VerificationBadge.BLUE)
                            showAddAccountDialog = false
                        },
                        enabled = name.isNotBlank() && handle.isNotBlank()
                    ) {
                        Text("Create & Switch")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddAccountDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}
