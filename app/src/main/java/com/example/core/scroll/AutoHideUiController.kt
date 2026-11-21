package com.example.core.scroll

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalXtraDisplayConfig
import kotlin.math.abs

/**
 * Shared scroll-driven chrome controller managing independent Top App Bar
 * and Bottom Navigation visibility with anti-jitter hysteresis and lock overrides.
 */
@Stable
class AutoHideUiController(
    private val topHideThresholdPx: Float,
    private val bottomHideThresholdPx: Float,
    private val revealThresholdPx: Float,
    private val minJitterDeadzonePx: Float
) {
    var isTopBarVisible by mutableStateOf(true)
        private set

    var isBottomBarVisible by mutableStateOf(true)
        private set

    var isLockedVisible by mutableStateOf(false)
        private set

    private var accumulatedDy: Float = 0f
    private val maxAccumulationPx: Float =
        maxOf(topHideThresholdPx, bottomHideThresholdPx, revealThresholdPx) * 2.5f

    fun updateLock(locked: Boolean) {
        isLockedVisible = locked
        if (locked) {
            isTopBarVisible = true
            isBottomBarVisible = true
            accumulatedDy = 0f
        }
    }

    fun onAtTopChanged(atTop: Boolean) {
        if (atTop) {
            isTopBarVisible = true
            isBottomBarVisible = true
            accumulatedDy = 0f
        }
    }

    fun forceShowAll() {
        isTopBarVisible = true
        isBottomBarVisible = true
        accumulatedDy = 0f
    }

    /**
     * Handles scroll consumption deltas in Jetpack Compose screen coordinates:
     * negative [dyPx] indicates downward feed progression (user swiping up),
     * positive [dyPx] indicates upward feed progression (user swiping down).
     */
    fun onScrollDelta(dyPx: Float, isAtTop: Boolean) {
        if (isAtTop || isLockedVisible) {
            if (!isTopBarVisible) isTopBarVisible = true
            if (!isBottomBarVisible) isBottomBarVisible = true
            accumulatedDy = 0f
            return
        }
        if (abs(dyPx) < 0.5f) return

        // Suppress tiny opposing micro-movements to prevent bar flicker
        val isReversal = accumulatedDy != 0f &&
            ((dyPx < 0f && accumulatedDy > 0f) || (dyPx > 0f && accumulatedDy < 0f))
        if (isReversal) {
            if (abs(dyPx) < minJitterDeadzonePx) {
                return
            }
            accumulatedDy = 0f
        }

        // Require exceeding deadzone before starting an initial direction from rest
        if (accumulatedDy == 0f && abs(dyPx) < minJitterDeadzonePx) {
            accumulatedDy = dyPx
            return
        }

        accumulatedDy = (accumulatedDy + dyPx).coerceIn(-maxAccumulationPx, maxAccumulationPx)

        if (accumulatedDy <= -bottomHideThresholdPx && isBottomBarVisible) {
            isBottomBarVisible = false
        }
        if (accumulatedDy <= -topHideThresholdPx && isTopBarVisible) {
            isTopBarVisible = false
        }
        if (accumulatedDy >= revealThresholdPx) {
            if (!isBottomBarVisible) isBottomBarVisible = true
            if (!isTopBarVisible) isTopBarVisible = true
        }
    }
}

val LocalAutoHideUiController = staticCompositionLocalOf<AutoHideUiController?> { null }
val LocalBottomBarClearance = compositionLocalOf { 0.dp }
val LocalHasPhoneBottomBar = compositionLocalOf { false }

@Composable
fun rememberAutoHideUiController(
    topHideThresholdDp: Dp = 44.dp,
    bottomHideThresholdDp: Dp = 28.dp,
    revealThresholdDp: Dp = 22.dp,
    minJitterDeadzoneDp: Dp = 6.dp
): AutoHideUiController {
    val density = LocalDensity.current
    return remember(density, topHideThresholdDp, bottomHideThresholdDp, revealThresholdDp, minJitterDeadzoneDp) {
        with(density) {
            AutoHideUiController(
                topHideThresholdPx = topHideThresholdDp.toPx(),
                bottomHideThresholdPx = bottomHideThresholdDp.toPx(),
                revealThresholdPx = revealThresholdDp.toPx(),
                minJitterDeadzonePx = minJitterDeadzoneDp.toPx()
            )
        }
    }
}

@Composable
fun rememberAutoHideNestedScrollConnection(
    controller: AutoHideUiController,
    isAtTopProvider: () -> Boolean
): NestedScrollConnection {
    val controllerState by rememberUpdatedState(controller)
    val isAtTopState by rememberUpdatedState(isAtTopProvider)
    return remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
              ): Offset {
                if (consumed.y != 0f) {
                    controllerState.onScrollDelta(
                        dyPx = consumed.y,
                        isAtTop = isAtTopState()
                    )
                }
                return Offset.Zero
            }
        }
    }
}

@Composable
fun rememberLazyListAutoHideConnection(
    listState: LazyListState,
    controller: AutoHideUiController,
    lockVisible: Boolean = false
): NestedScrollConnection {
    val isAtTop by remember(listState) {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset <= 12
        }
    }

    LaunchedEffect(lockVisible) {
        controller.updateLock(lockVisible)
    }

    LaunchedEffect(isAtTop) {
        if (isAtTop) {
            controller.onAtTopChanged(true)
        }
    }

    DisposableEffect(controller) {
        onDispose {
            controller.updateLock(false)
        }
    }

    return rememberAutoHideNestedScrollConnection(
        controller = controller,
        isAtTopProvider = { isAtTop }
    )
}

@Composable
fun rememberLazyGridAutoHideConnection(
    gridState: LazyGridState,
    controller: AutoHideUiController,
    lockVisible: Boolean = false
): NestedScrollConnection {
    val isAtTop by remember(gridState) {
        derivedStateOf {
            gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset <= 12
        }
    }

    LaunchedEffect(lockVisible) {
        controller.updateLock(lockVisible)
    }

    LaunchedEffect(isAtTop) {
        if (isAtTop) {
            controller.onAtTopChanged(true)
        }
    }

    DisposableEffect(controller) {
        onDispose {
            controller.updateLock(false)
        }
    }

    return rememberAutoHideNestedScrollConnection(
        controller = controller,
        isAtTopProvider = { isAtTop }
    )
}

/**
 * Reusable full-bleed edge-to-edge screen container featuring:
 * - Floating auto-hiding top header with slide+fade animations and zero list jump
 * - Safe status bar + display cutout + horizontal waterfall padding without double-padding
 * - Translucent top status-bar protection scrim when the header slides away
 * - Automatic bottom navigation + gesture navigation bar clearance for scroll lists
 */
@Composable
fun AutoHideHeaderLayout(
    modifier: Modifier = Modifier,
    listState: LazyListState? = null,
    gridState: LazyGridState? = null,
    lockBarsVisible: Boolean = false,
    extraBottomPadding: Dp = 16.dp,
    header: @Composable () -> Unit,
    content: @Composable (scrollModifier: Modifier, contentPadding: PaddingValues) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val reducedMotion = LocalXtraDisplayConfig.current.reducedMotion
    val sharedController = LocalAutoHideUiController.current
    val localFallbackController = rememberAutoHideUiController()
    val controller = sharedController ?: localFallbackController

    val nestedScrollConnection = when {
        listState != null -> rememberLazyListAutoHideConnection(
            listState = listState,
            controller = controller,
            lockVisible = lockBarsVisible
        )
        gridState != null -> rememberLazyGridAutoHideConnection(
            gridState = gridState,
            controller = controller,
            lockVisible = lockBarsVisible
        )
        else -> {
            LaunchedEffect(lockBarsVisible) {
                controller.updateLock(lockBarsVisible)
            }
            rememberAutoHideNestedScrollConnection(controller = controller, isAtTopProvider = { false })
        }
    }

    var measuredHeaderHeightPx by remember { mutableIntStateOf(0) }
    val headerHeightDp = with(density) { measuredHeaderHeightPx.toDp() }

    val safeDrawingPadding = WindowInsets.safeDrawing.asPaddingValues()
    val bottomSystemAndNavPadding = WindowInsets.navigationBars
        .union(WindowInsets.displayCutout.only(WindowInsetsSides.Bottom))
        .asPaddingValues()
        .calculateBottomPadding()

    val hasBottomBar = LocalHasPhoneBottomBar.current
    val bottomBarClearance = LocalBottomBarClearance.current
    val totalBottomPadding = if (hasBottomBar && bottomBarClearance > 0.dp) {
        maxOf(bottomBarClearance, bottomSystemAndNavPadding) + extraBottomPadding
    } else {
        bottomSystemAndNavPadding + extraBottomPadding
    }

    val topPadding = maxOf(
        headerHeightDp,
        safeDrawingPadding.calculateTopPadding()
    )

    val listContentPadding = PaddingValues(
        start = safeDrawingPadding.calculateStartPadding(layoutDirection),
        end = safeDrawingPadding.calculateEndPadding(layoutDirection),
        top = topPadding,
        bottom = totalBottomPadding
    )

    val animDuration = if (reducedMotion) 0 else 220

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        content(
            Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection),
            listContentPadding
        )

        // Subtle status bar & notch protection scrim when content scrolls underneath
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars.union(WindowInsets.displayCutout))
                .background(colorScheme.background.copy(alpha = 0.78f))
                .align(Alignment.TopCenter)
        )

        AnimatedVisibility(
            visible = controller.isTopBarVisible || lockBarsVisible,
            enter = slideInVertically(
                animationSpec = tween(durationMillis = animDuration, easing = FastOutSlowInEasing),
                initialOffsetY = { -it }
            ) + fadeIn(animationSpec = tween(durationMillis = animDuration)),
            exit = slideOutVertically(
                animationSpec = tween(durationMillis = animDuration, easing = FastOutSlowInEasing),
                targetOffsetY = { -it }
            ) + fadeOut(animationSpec = tween(durationMillis = animDuration)),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colorScheme.background.copy(alpha = 0.96f))
                    .onSizeChanged { measuredHeaderHeightPx = it.height }
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
                    )
            ) {
                header()
            }
        }
    }
}
