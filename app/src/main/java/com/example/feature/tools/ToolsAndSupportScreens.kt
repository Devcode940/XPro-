package com.example.feature.tools

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.core.designsystem.AppTopBar
import com.example.core.designsystem.rememberIsKeyboardOpen
import com.example.core.navigation.XtraRoutes
import com.example.core.scroll.AutoHideHeaderLayout
import com.example.data.model.BrowserHistoryItem
import com.example.data.model.SavedPageItem
import com.example.data.model.UserProfile
import com.example.ui.components.XProLogo
import com.example.ui.components.formatRelativeTime
import com.example.ui.theme.MonoTabularStyle
import kotlin.math.abs

@Composable
fun QrScannerScreen(
    userProfile: UserProfile,
    onOpenDrawer: () -> Unit,
    onBack: () -> Unit,
    onNavigateRoute: (String) -> Unit,
    onOpenPrivateUrl: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val clipboard = LocalClipboardManager.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    var cameraStatus by remember { mutableStateOf<String?>(null) }
    var qrPayloadInput by rememberSaveable { mutableStateOf("") }
    var resolvedMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .testTag("qr_scanner_screen")
    ) {
        AppTopBar(
            title = "QR Code Scanner",
            subtitle = "Scan or resolve xtra:// & HTTPS links",
            userName = userProfile.name,
            showBack = true,
            onBack = onBack,
            onOpenDrawer = onOpenDrawer,
            modifier = Modifier.windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
            )
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(
                    WindowInsets.navigationBars
                        .union(WindowInsets.ime)
                        .union(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                color = colorScheme.surfaceContainer,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, colorScheme.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
            ) {
                if (hasCameraPermission) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        AndroidView(
                            factory = { ctx ->
                                val previewView = PreviewView(ctx)
                                runCatching {
                                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                    cameraProviderFuture.addListener({
                                        runCatching {
                                            val cameraProvider = cameraProviderFuture.get()
                                            val preview = Preview.Builder().build().also {
                                                it.surfaceProvider = previewView.surfaceProvider
                                            }
                                            cameraProvider.unbindAll()
                                            cameraProvider.bindToLifecycle(
                                                lifecycleOwner,
                                                CameraSelector.DEFAULT_BACK_CAMERA,
                                                preview
                                            )
                                            cameraStatus = "CameraX live viewfinder active"
                                        }.onFailure { err ->
                                            cameraStatus = "Camera unavailable: ${err.message ?: "no hardware camera"}"
                                        }
                                    }, ContextCompat.getMainExecutor(ctx))
                                }.onFailure { err ->
                                    cameraStatus = "Camera init skipped: ${err.message}"
                                }
                                previewView
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                        // Viewfinder framing overlay
                        Surface(
                            color = Color.Transparent,
                            border = BorderStroke(2.dp, colorScheme.primary),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.size(140.dp)
                        ) {}
                        cameraStatus?.let { msg ->
                            Surface(
                                color = Color.Black.copy(alpha = 0.72f),
                                shape = RoundedCornerShape(999.dp),
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = msg,
                                    style = MonoTabularStyle.copy(fontSize = 11.sp),
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Filled.CameraAlt, null, tint = colorScheme.primary, modifier = Modifier.size(38.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Camera permission needed for live QR viewfinder", style = MaterialTheme.typography.titleSmall, color = colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                            modifier = Modifier.testTag("qr_request_camera_btn")
                        ) {
                            Text("Enable Camera Viewfinder")
                        }
                    }
                }
            }

            // Paste / Resolve QR payload
            Surface(
                color = colorScheme.surfaceContainer,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Resolve QR Payload / Deep Link", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = colorScheme.onSurface)
                    OutlinedTextField(
                        value = qrPayloadInput,
                        onValueChange = { qrPayloadInput = it },
                        placeholder = { Text("xtra://bookmarks or https://...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                val pasted = clipboard.getText()?.text.orEmpty()
                                if (pasted.isNotBlank()) qrPayloadInput = pasted
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.ContentPaste, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Paste")
                        }
                        Button(
                            onClick = {
                                val payload = qrPayloadInput.trim()
                                when {
                                    payload.startsWith("xtra://") -> {
                                        val route = payload.removePrefix("xtra://").trim('/')
                                        resolvedMessage = "Opening route: $route"
                                        onNavigateRoute(route)
                                    }
                                    payload.startsWith("http://") || payload.startsWith("https://") -> {
                                        resolvedMessage = "Opening in Private Browsing"
                                        onOpenPrivateUrl(payload)
                                    }
                                    else -> {
                                        resolvedMessage = "Detected text payload: $payload"
                                    }
                                }
                            },
                            enabled = qrPayloadInput.isNotBlank(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.QrCodeScanner, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open Link")
                        }
                    }
                    resolvedMessage?.let {
                        Text(it, style = MonoTabularStyle, color = colorScheme.primary)
                    }
                }
            }

            // Shareable My Profile QR Card
            Surface(
                color = colorScheme.surfaceContainer,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DeterministicQrMatrix(
                        seed = "xtra://profile/${userProfile.handle}",
                        foreground = colorScheme.onSurface,
                        background = colorScheme.surface,
                        modifier = Modifier.size(84.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.QrCode2, null, tint = colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Your XTRA Profile QR", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = colorScheme.onSurface)
                        }
                        Text(userProfile.handle, style = MonoTabularStyle, color = colorScheme.primary)
                        Text("Deep link: xtra://profile/current_user", style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun DeterministicQrMatrix(
    seed: String,
    foreground: Color,
    background: Color,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .padding(6.dp)
    ) {
        val grid = 9
        val cell = size.minDimension / grid
        val hash = seed.hashCode()
        for (r in 0 until grid) {
            for (c in 0 until grid) {
                val isCorner = (r < 3 && c < 3) || (r < 3 && c >= grid - 3) || (r >= grid - 3 && c < 3)
                val bit = if (isCorner) {
                    !((r == 1 && c == 1) || (r == 1 && c == grid - 2) || (r == grid - 2 && c == 1))
                } else {
                    abs((hash xor (r * 31 + c * 17))) % 2 == 0
                }
                if (bit) {
                    drawRect(
                        color = foreground,
                        topLeft = Offset(c * cell, r * cell),
                        size = Size(cell * 0.88f, cell * 0.88f)
                    )
                }
            }
        }
    }
}

@Composable
fun SavedPagesScreen(
    toolsViewModel: ToolsViewModel,
    userProfile: UserProfile,
    readingListOnly: Boolean,
    onOpenDrawer: () -> Unit,
    onBack: () -> Unit,
    onOpenInPrivateBrowser: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val layoutDirection = LocalLayoutDirection.current
    val state by toolsViewModel.uiState.collectAsStateWithLifecycle()
    val itemsList = if (readingListOnly) state.readingListPages else state.savedPages
    var showAdd by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val isKeyboardOpen by rememberIsKeyboardOpen()

    AutoHideHeaderLayout(
        modifier = modifier
            .fillMaxSize()
            .testTag(if (readingListOnly) "reading_list_screen" else "saved_pages_screen"),
        listState = listState,
        lockBarsVisible = showAdd || isKeyboardOpen,
        header = {
            AppTopBar(
                title = if (readingListOnly) "Reading List" else "Saved Pages",
                subtitle = "${itemsList.size} items · Room persisted",
                userName = userProfile.name,
                showBack = true,
                onBack = onBack,
                onOpenDrawer = onOpenDrawer,
                actions = {
                    IconButton(onClick = { showAdd = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Save page", tint = colorScheme.primary)
                    }
                }
            )
        }
    ) { scrollModifier, contentPadding ->
        if (itemsList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (readingListOnly) "Your Reading List is empty." else "No saved pages yet.",
                    style = MaterialTheme.typography.titleMedium,
                    color = colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = scrollModifier,
                contentPadding = PaddingValues(
                    start = contentPadding.calculateStartPadding(layoutDirection) + 16.dp,
                    end = contentPadding.calculateEndPadding(layoutDirection) + 16.dp,
                    top = contentPadding.calculateTopPadding() + 16.dp,
                    bottom = contentPadding.calculateBottomPadding() + 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(itemsList, key = { it.id }) { page: SavedPageItem ->
                    Surface(
                        color = colorScheme.surfaceContainer,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (readingListOnly) Icons.AutoMirrored.Filled.MenuBook else Icons.Filled.BookmarkAdded,
                                    contentDescription = null,
                                    tint = colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(page.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = colorScheme.onSurface)
                                    Text(page.url, style = MonoTabularStyle, color = colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                IconButton(onClick = { toolsViewModel.togglePageCompleted(page) }) {
                                    Icon(
                                        imageVector = if (page.isCompleted) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                                        contentDescription = if (page.isCompleted) "Completed" else "Mark read",
                                        tint = if (page.isCompleted) colorScheme.tertiary else colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(page.summary, style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { onOpenInPrivateBrowser(page.url) }) {
                                    Icon(Icons.Filled.OpenInBrowser, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Open")
                                }
                                OutlinedButton(onClick = { toolsViewModel.toggleReadingList(page) }) {
                                    Text(if (page.isInReadingList) "In Reading List" else "Add to Reading List")
                                }
                                IconButton(onClick = { toolsViewModel.deleteSavedPage(page.id) }) {
                                    Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete saved page", tint = colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        var title by rememberSaveable { mutableStateOf("") }
        var url by rememberSaveable { mutableStateOf("") }
        var summary by rememberSaveable { mutableStateOf("") }
        var addToList by rememberSaveable { mutableStateOf(readingListOnly) }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("Save Web Page") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, singleLine = true)
                    OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("HTTPS URL") }, singleLine = true)
                    OutlinedTextField(value = summary, onValueChange = { summary = it }, label = { Text("Notes / summary") })
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = addToList, onCheckedChange = { addToList = it })
                        Text("Add to Reading List")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        toolsViewModel.saveWebPage(title, url, summary, addToList)
                        showAdd = false
                    },
                    enabled = url.isNotBlank()
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun BrowserHistoryScreen(
    toolsViewModel: ToolsViewModel,
    userProfile: UserProfile,
    onOpenDrawer: () -> Unit,
    onBack: () -> Unit,
    onOpenInPrivateBrowser: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val state by toolsViewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val isKeyboardOpen by rememberIsKeyboardOpen()
    var searchFocused by remember { mutableStateOf(false) }
    val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()

    AutoHideHeaderLayout(
        modifier = modifier
            .fillMaxSize()
            .testTag("browser_history_screen"),
        listState = listState,
        lockBarsVisible = isKeyboardOpen || searchFocused,
        header = {
            Column(modifier = Modifier.fillMaxWidth()) {
                AppTopBar(
                    title = "Browser History",
                    subtitle = "${state.browserHistory.size} entries",
                    userName = userProfile.name,
                    showBack = true,
                    onBack = onBack,
                    onOpenDrawer = onOpenDrawer,
                    actions = {
                        if (state.browserHistory.isNotEmpty()) {
                            IconButton(onClick = toolsViewModel::clearHistory) {
                                Icon(Icons.Filled.DeleteSweep, contentDescription = "Clear history", tint = colorScheme.error)
                            }
                        }
                    }
                )

                OutlinedTextField(
                    value = state.historySearchQuery,
                    onValueChange = toolsViewModel::updateHistorySearch,
                    placeholder = { Text("Filter history...") },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    singleLine = true,
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .onFocusChanged { searchFocused = it.isFocused }
                )
            }
        }
    ) { scrollModifier, contentPadding ->
        val effectivePadding = PaddingValues(
            start = contentPadding.calculateStartPadding(LocalLayoutDirection.current),
            end = contentPadding.calculateEndPadding(LocalLayoutDirection.current),
            top = contentPadding.calculateTopPadding(),
            bottom = maxOf(contentPadding.calculateBottomPadding(), imeBottom)
        )
        if (state.filteredHistory.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(effectivePadding)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.History, null, tint = colorScheme.onSurfaceVariant, modifier = Modifier.size(42.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No browser history", style = MaterialTheme.typography.titleMedium, color = colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = scrollModifier,
                contentPadding = effectivePadding
            ) {
                items(state.filteredHistory, key = { it.id }) { item: BrowserHistoryItem ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenInPrivateBrowser(item.url) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.History, null, tint = colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.title, style = MaterialTheme.typography.titleSmall, color = colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${item.url} · ${formatRelativeTime(item.visitedAtMillis)} ago", style = MonoTabularStyle, color = colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        IconButton(onClick = { toolsViewModel.deleteHistoryItem(item.id) }) {
                            Icon(Icons.Filled.Close, contentDescription = "Remove entry", tint = colorScheme.onSurfaceVariant)
                        }
                    }
                    HorizontalDivider(color = colorScheme.outlineVariant, thickness = 0.6.dp)
                }
            }
        }
    }
}

/**
 * Plain holder (not Compose state) for the WebView created by `AndroidView`, so assigning it does
 * not schedule a recomposition and `onDispose` can still reach the instance to destroy it.
 */
private class WebViewHandle {
    var webView: WebView? = null
}

/**
 * Private browsing accepts `https://` only. Anything else is either refused or upgraded, so a
 * pasted `http://`/`javascript:`/`intent://` string can never reach the WebView.
 */
private fun normalizeHttpsUrl(raw: String): String? {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return null
    return when {
        trimmed.startsWith("https://", ignoreCase = true) -> trimmed
        trimmed.startsWith("http://", ignoreCase = true) -> "https://" + trimmed.substring("http://".length)
        else -> "https://$trimmed"
    }
}

/** Only http(s) navigations are allowed inside the private WebView (blocks file:, intent:, javascript:). */
private fun isWebNavigationAllowed(url: String): Boolean {
    val scheme = runCatching { Uri.parse(url).scheme?.lowercase() }.getOrNull()
    return scheme == "https" || scheme == "http"
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PrivateBrowsingScreen(
    toolsViewModel: ToolsViewModel,
    userProfile: UserProfile,
    onOpenDrawer: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val state by toolsViewModel.uiState.collectAsStateWithLifecycle()
    var urlInput by rememberSaveable(state.privateBrowserUrl) { mutableStateOf(state.privateBrowserUrl) }
    var currentTitle by remember { mutableStateOf("Private Browsing") }
    val webViewHolder = remember { WebViewHandle() }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { CookieManager.getInstance().removeAllCookies(null) }
            runCatching { WebStorage.getInstance().deleteAllData() }
            webViewHolder.webView?.let { view ->
                runCatching {
                    view.stopLoading()
                    view.loadUrl("about:blank")
                    view.clearHistory()
                    view.clearCache(true)
                    view.clearFormData()
                    // Destroying the WebView is what releases its Activity context; without this
                    // the whole screen (and its native renderer) leaks for the process lifetime.
                    view.destroy()
                }
            }
            webViewHolder.webView = null
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .testTag("private_browsing_screen")
    ) {
        AppTopBar(
            title = "Private Browsing",
            subtitle = "Zero-history · Cookies disabled",
            userName = userProfile.name,
            showBack = true,
            onBack = onBack,
            onOpenDrawer = onOpenDrawer,
            actions = {
                IconButton(onClick = { webViewHolder.webView?.goBack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back page")
                }
                IconButton(onClick = { webViewHolder.webView?.goForward() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Forward page")
                }
                IconButton(onClick = { webViewHolder.webView?.reload() }) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Reload")
                }
            },
            modifier = Modifier.windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
            )
        )

        Surface(
            color = colorScheme.secondaryContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.VisibilityOff, null, tint = colorScheme.secondary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Incognito active: $currentTitle · Cache wiped on exit",
                    style = MonoTabularStyle.copy(fontSize = 11.sp),
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = urlInput,
                onValueChange = { urlInput = it },
                placeholder = { Text("https://...") },
                singleLine = true,
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    normalizeHttpsUrl(urlInput)?.let { target ->
                        toolsViewModel.openPrivateUrl(target)
                        webViewHolder.webView?.loadUrl(target)
                    }
                }
            ) {
                Text("Go")
            }
        }

        AndroidView(
            factory = { ctx ->
                runCatching { CookieManager.getInstance().setAcceptCookie(false) }
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = false
                    settings.cacheMode = WebSettings.LOAD_NO_CACHE
                    // minSdk 24: file access defaults to enabled on API 24–29, which lets a page
                    // reach local files from inside a JS-enabled WebView.
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    settings.setGeolocationEnabled(false)
                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        settings.safeBrowsingEnabled = true
                    }
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            val target = request?.url?.toString() ?: return true
                            return !isWebNavigationAllowed(target)
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            currentTitle = view?.title ?: url ?: "Private Tab"
                            if (url != null) urlInput = url
                        }
                    }
                    loadUrl(state.privateBrowserUrl)
                    webViewHolder.webView = this
                }
            },
            update = { view ->
                webViewHolder.webView = view
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .windowInsetsPadding(
                    WindowInsets.navigationBars
                        .union(WindowInsets.ime)
                        .union(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                )
        )
    }
}

@Composable
fun MediaSharingScreen(
    userProfile: UserProfile,
    onOpenDrawer: () -> Unit,
    onBack: () -> Unit,
    onOpenComposerWithMedia: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    var pickedUri by remember { mutableStateOf<Uri?>(null) }
    var caption by rememberSaveable { mutableStateOf("") }
    var linkToShare by rememberSaveable { mutableStateOf("https://xtra.app/u/${userProfile.handle.removePrefix("@")}") }

    val mediaPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) pickedUri = uri
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .testTag("media_sharing_screen")
    ) {
        AppTopBar(
            title = "Media Sharing",
            subtitle = "Zero-permission Photo Picker & System Share",
            userName = userProfile.name,
            showBack = true,
            onBack = onBack,
            onOpenDrawer = onOpenDrawer,
            modifier = Modifier.windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
            )
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(
                    WindowInsets.navigationBars
                        .union(WindowInsets.ime)
                        .union(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                color = colorScheme.surfaceContainer,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Pick Media to Share", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = colorScheme.onSurface)
                    Text(
                        text = pickedUri?.toString() ?: "No photo or video selected yet. Uses the Android Photo Picker without broad storage permissions.",
                        style = MonoTabularStyle,
                        color = if (pickedUri != null) colorScheme.primary else colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = {
                            mediaPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.PhotoLibrary, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Select Photo or Video")
                    }
                    if (pickedUri != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { onOpenComposerWithMedia(pickedUri.toString()) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Post to XTRA")
                            }
                            Button(
                                onClick = {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "*/*"
                                        putExtra(Intent.EXTRA_STREAM, pickedUri)
                                        putExtra(Intent.EXTRA_TEXT, caption)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    runCatching {
                                        context.startActivity(Intent.createChooser(shareIntent, "Share media"))
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Share, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share App")
                            }
                        }
                    }
                }
            }

            Surface(
                color = colorScheme.surfaceContainer,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Share Link or Message", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = colorScheme.onSurface)
                    OutlinedTextField(value = caption, onValueChange = { caption = it }, label = { Text("Caption") })
                    OutlinedTextField(value = linkToShare, onValueChange = { linkToShare = it }, label = { Text("URL") }, singleLine = true)
                    Button(
                        onClick = {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, listOf(caption, linkToShare).filter { it.isNotBlank() }.joinToString("\n"))
                            }
                            runCatching {
                                context.startActivity(Intent.createChooser(sendIntent, "Share via"))
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Share, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share Text / Link")
                    }
                }
            }
        }
    }
}

private data class HelpFaqItem(val title: String, val answer: String)

private val HelpFaqs = listOf(
    HelpFaqItem("How does Ad-Free Mode work?", "XTRA automatically filters promoted posts and sponsored trends at the Room & Paging 3 query level so feeds stay fast and clean."),
    HelpFaqItem("How do Bookmarks sync across devices?", "Bookmarks are stored locally in Room SQLite at all times. When signed in with Google, your typed bookmarks and collections sync automatically with Cloud Firestore."),
    HelpFaqItem("Can I pause, resume, or retry downloads?", "Yes! Open Media Center -> Downloads or Download Manager to enqueue MP4, WebM, or MP3 links with Wi-Fi-only filtering, Pause/Resume/Retry/Cancel, and Share."),
    HelpFaqItem("How does Private Browsing protect privacy?", "Private Browsing disables persistent cookies, disables DOM storage, sets LOAD_NO_CACHE, and wipes WebView history and cache immediately when leaving."),
    HelpFaqItem("How do I customize themes & accessibility?", "Open Settings & Support -> Appearance and Themes or Accessibility to switch between OLED Dark, Light, System theme, Accent colors, Display Density, and Larger Text.")
)

@Composable
fun HelpCenterScreen(
    userProfile: UserProfile,
    onOpenDrawer: () -> Unit,
    onBack: () -> Unit,
    onOpenSendFeedback: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val layoutDirection = LocalLayoutDirection.current
    var query by rememberSaveable { mutableStateOf("") }
    var expandedTitle by rememberSaveable { mutableStateOf<String?>(HelpFaqs.first().title) }
    val listState = rememberLazyListState()
    val isKeyboardOpen by rememberIsKeyboardOpen()
    var searchFocused by remember { mutableStateOf(false) }
    val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()

    val filtered = HelpFaqs.filter {
        query.isBlank() || it.title.contains(query, true) || it.answer.contains(query, true)
    }

    AutoHideHeaderLayout(
        modifier = modifier
            .fillMaxSize()
            .testTag("help_center_screen"),
        listState = listState,
        lockBarsVisible = isKeyboardOpen || searchFocused,
        header = {
            Column(modifier = Modifier.fillMaxWidth()) {
                AppTopBar(
                    title = "Help Center",
                    subtitle = "Guides & troubleshooting",
                    userName = userProfile.name,
                    showBack = true,
                    onBack = onBack,
                    onOpenDrawer = onOpenDrawer
                )
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search help topics...") },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    singleLine = true,
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .onFocusChanged { searchFocused = it.isFocused }
                )
            }
        }
    ) { scrollModifier, contentPadding ->
        LazyColumn(
            state = listState,
            modifier = scrollModifier,
            contentPadding = PaddingValues(
                start = contentPadding.calculateStartPadding(layoutDirection) + 16.dp,
                end = contentPadding.calculateEndPadding(layoutDirection) + 16.dp,
                top = contentPadding.calculateTopPadding() + 8.dp,
                bottom = maxOf(contentPadding.calculateBottomPadding(), imeBottom) + 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filtered, key = { it.title }) { faq ->
                val isOpen = expandedTitle == faq.title
                Surface(
                    color = colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, colorScheme.outline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { expandedTitle = if (isOpen) null else faq.title }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(faq.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = colorScheme.onSurface, modifier = Modifier.weight(1f))
                            Icon(if (isOpen) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null, tint = colorScheme.onSurfaceVariant)
                        }
                        if (isOpen) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(faq.answer, style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onOpenSendFeedback, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Feedback, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Still need help? Send Feedback")
                }
            }
        }
    }
}

@Composable
fun AboutXtraScreen(
    userProfile: UserProfile,
    onOpenDrawer: () -> Unit,
    onBack: () -> Unit,
    onOpenHelpCenter: () -> Unit,
    onOpenSendFeedback: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .testTag("about_xtra_screen")
    ) {
        AppTopBar(
            title = "About XTRA",
            subtitle = "XPro Unlocked · v${BuildConfig.VERSION_NAME}",
            userName = userProfile.name,
            showBack = true,
            onBack = onBack,
            onOpenDrawer = onOpenDrawer,
            modifier = Modifier.windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
            )
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(
                    WindowInsets.navigationBars.union(
                        WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                    )
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                color = colorScheme.surfaceContainer,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    XProLogo(size = 44.dp, tint = colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("XTRA for XPro", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = colorScheme.onSurface)
                    Text("Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", style = MonoTabularStyle, color = colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "An ad-free, offline-first Jetpack Compose X client with unlocked Premium capabilities, Grok AI, Media Downloader, Private Browsing, and Cloud Firestore bookmark sync.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }
            Surface(
                color = colorScheme.surfaceContainer,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Shield, null, tint = colorScheme.tertiary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Stack & Security", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = colorScheme.onSurface)
                    }
                    Text("• Package: ${BuildConfig.APPLICATION_ID}", style = MonoTabularStyle, color = colorScheme.onSurfaceVariant)
                    Text("• Architecture: Navigation Compose + MVVM + Room + Paging 3 + DataStore", style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
                    Text("• Cloud & AI: Firebase AI Logic (Grok), Firestore Sync & App Check", style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onOpenHelpCenter, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Info, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Help Center")
                }
                Button(onClick = onOpenSendFeedback, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Feedback, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Send Feedback")
                }
            }
        }
    }
}

@Composable
fun SendFeedbackScreen(
    toolsViewModel: ToolsViewModel,
    userProfile: UserProfile,
    onOpenDrawer: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val layoutDirection = LocalLayoutDirection.current
    val state by toolsViewModel.uiState.collectAsStateWithLifecycle()
    val categories = listOf("Feature Request", "Bug Report", "Media & Downloads", "Design & Themes")
    var selectedCategory by rememberSaveable { mutableStateOf(categories.first()) }
    var message by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    val isKeyboardOpen by rememberIsKeyboardOpen()
    var messageFocused by remember { mutableStateOf(false) }
    val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()

    AutoHideHeaderLayout(
        modifier = modifier
            .fillMaxSize()
            .testTag("send_feedback_screen"),
        listState = listState,
        lockBarsVisible = isKeyboardOpen || messageFocused,
        header = {
            AppTopBar(
                title = "Send Feedback",
                subtitle = "Help improve XTRA",
                userName = userProfile.name,
                showBack = true,
                onBack = onBack,
                onOpenDrawer = onOpenDrawer
            )
        }
    ) { scrollModifier, contentPadding ->
        LazyColumn(
            state = listState,
            modifier = scrollModifier,
            contentPadding = PaddingValues(
                start = contentPadding.calculateStartPadding(layoutDirection) + 16.dp,
                end = contentPadding.calculateEndPadding(layoutDirection) + 16.dp,
                top = contentPadding.calculateTopPadding() + 16.dp,
                bottom = maxOf(contentPadding.calculateBottomPadding(), imeBottom) + 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                state.statusBanner?.let { banner ->
                    Surface(
                        color = colorScheme.primaryContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(banner, style = MaterialTheme.typography.bodySmall, color = colorScheme.primary)
                            IconButton(onClick = toolsViewModel::dismissBanner, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Filled.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
                Text("Category", style = MaterialTheme.typography.labelLarge, color = colorScheme.onSurface)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Your feedback") },
                    placeholder = { Text("Describe the issue or idea...") },
                    minLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { messageFocused = it.isFocused }
                        .testTag("feedback_message_input")
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        toolsViewModel.submitFeedback(selectedCategory, message)
                        message = ""
                    },
                    enabled = message.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("feedback_submit_btn")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Submit Feedback")
                }
            }

            if (state.feedbackList.isNotEmpty()) {
                item {
                    HorizontalDivider(color = colorScheme.outline, thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Submitted History", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = colorScheme.onSurface)
                }
                items(state.feedbackList, key = { it.id }) { fb ->
                    Surface(
                        color = colorScheme.surfaceContainer,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(fb.category, style = MonoTabularStyle, color = colorScheme.primary)
                                Text(formatRelativeTime(fb.submittedAtMillis) + " ago", style = MonoTabularStyle, color = colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(fb.message, style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurface)
                        }
                    }
                }
            }
        }
    }
}
