package com.example.core.designsystem

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.graphics.Color as AndroidColor
import android.os.Build
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Configures true full-screen edge-to-edge drawing, transparent system bars,
 * display cutout short-edges handling, and system bar icon contrast for XTRA.
 */
fun ComponentActivity.configureXtraEdgeToEdge(isDarkTheme: Boolean) {
    WindowCompat.setDecorFitsSystemWindows(window, false)

    val transparentStyle = if (isDarkTheme) {
        SystemBarStyle.dark(AndroidColor.TRANSPARENT)
    } else {
        SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
    }
    enableEdgeToEdge(
        statusBarStyle = transparentStyle,
        navigationBarStyle = transparentStyle
    )

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        window.attributes = window.attributes.apply {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        window.isNavigationBarContrastEnforced = false
    }

    val controller = WindowCompat.getInsetsController(window, window.decorView)
    controller.isAppearanceLightStatusBars = !isDarkTheme
    controller.isAppearanceLightNavigationBars = !isDarkTheme
}

@Composable
fun XtraSystemBarsThemeEffect(isDarkTheme: Boolean) {
    val view = LocalView.current
    val activity = remember(view) { view.context.findActivityOrNull() }
    val darkThemeState by rememberUpdatedState(isDarkTheme)

    SideEffect {
        val window = activity?.window ?: return@SideEffect
        if (activity is ComponentActivity) {
            activity.configureXtraEdgeToEdge(darkThemeState)
        } else {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkThemeState
            controller.isAppearanceLightNavigationBars = !darkThemeState
        }
    }
}

/**
 * Hides system bars in immersive sticky mode and optionally switches orientation
 * for full-screen video playback, always restoring defaults on dispose.
 */
@Composable
fun ImmersiveVideoSystemBarsEffect(
    immersiveActive: Boolean,
    forceLandscape: Boolean,
    isDarkTheme: Boolean
) {
    val view = LocalView.current
    val activity = remember(view) { view.context.findActivityOrNull() }
    val darkThemeState by rememberUpdatedState(isDarkTheme)

    DisposableEffect(activity, immersiveActive, forceLandscape) {
        val window = activity?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, view)
            if (immersiveActive) {
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
                controller.isAppearanceLightStatusBars = !darkThemeState
                controller.isAppearanceLightNavigationBars = !darkThemeState
            }
        }
        if (activity != null) {
            activity.requestedOrientation = if (forceLandscape) {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            } else {
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }

        onDispose {
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.show(WindowInsetsCompat.Type.systemBars())
                controller.isAppearanceLightStatusBars = !darkThemeState
                controller.isAppearanceLightNavigationBars = !darkThemeState
            }
            if (activity != null) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }
}

@Composable
fun rememberIsKeyboardOpen(): State<Boolean> {
    val density = LocalDensity.current
    val imeInsets = WindowInsets.ime
    return remember(density, imeInsets) {
        derivedStateOf { imeInsets.getBottom(density) > 0 }
    }
}

tailrec fun Context.findActivityOrNull(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivityOrNull()
    else -> null
}
