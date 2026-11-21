package com.example.feature.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

private val Context.xtraSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "xtra_settings_prefs"
)

class SettingsRepository(private val context: Context) {

    private val appContext = context.applicationContext
    private val dataStore = appContext.xtraSettingsDataStore

    val settingsFlow: Flow<UserSettingsState> = dataStore.data.map { prefs ->
        UserSettingsState(
            themeMode = prefs[KEY_THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.DARK,
            accentColor = prefs[KEY_ACCENT_COLOR]?.let { runCatching { AccentColorOption.valueOf(it) }.getOrNull() }
                ?: AccentColorOption.X_BLUE,
            fontSize = prefs[KEY_FONT_SIZE]?.let { runCatching { FontSizePreset.valueOf(it) }.getOrNull() }
                ?: FontSizePreset.MEDIUM,
            displayDensity = prefs[KEY_DISPLAY_DENSITY]?.let { runCatching { DisplayDensity.valueOf(it) }.getOrNull() }
                ?: DisplayDensity.STANDARD,
            privateAccount = prefs[KEY_PRIVATE_ACCOUNT] ?: false,
            blockedAccounts = prefs[KEY_BLOCKED_ACCOUNTS]
                ?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                ?: listOf("@spam_bot_99"),
            mutedAccounts = prefs[KEY_MUTED_ACCOUNTS]
                ?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                ?: listOf("@crypto_promo_daily"),
            pushNotifications = prefs[KEY_PUSH_NOTIFS] ?: true,
            notifyLikes = prefs[KEY_NOTIFY_LIKES] ?: true,
            notifyComments = prefs[KEY_NOTIFY_COMMENTS] ?: true,
            notifyMessages = prefs[KEY_NOTIFY_MESSAGES] ?: true,
            notifyFollowers = prefs[KEY_NOTIFY_FOLLOWERS] ?: true,
            notifyDownloads = prefs[KEY_NOTIFY_DOWNLOADS] ?: true,
            downloadLocation = prefs[KEY_DOWNLOAD_LOCATION] ?: "Downloads/XPro",
            preferredVideoQuality = prefs[KEY_VIDEO_QUALITY] ?: "1080p HD",
            preferredAudioQuality = prefs[KEY_AUDIO_QUALITY] ?: "320 kbps",
            wifiOnlyDownloads = prefs[KEY_WIFI_ONLY] ?: false,
            backgroundDownloads = prefs[KEY_BACKGROUND_DOWNLOADS] ?: true,
            autoMediaDetection = prefs[KEY_AUTO_MEDIA_DETECT] ?: true,
            autoPlayVideos = prefs[KEY_AUTOPLAY_VIDEO] ?: true,
            subtitlesEnabled = prefs[KEY_SUBTITLES] ?: true,
            dataSaverEnabled = prefs[KEY_DATA_SAVER] ?: false,
            appLanguage = prefs[KEY_APP_LANGUAGE] ?: "English",
            largerText = prefs[KEY_LARGER_TEXT] ?: false,
            reducedMotion = prefs[KEY_REDUCED_MOTION] ?: false,
            screenReaderSupport = prefs[KEY_SCREEN_READER] ?: true,
            improvedContrast = prefs[KEY_IMPROVED_CONTRAST] ?: false
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) = update { it[KEY_THEME_MODE] = mode.name }
    suspend fun setAccentColor(accent: AccentColorOption) = update { it[KEY_ACCENT_COLOR] = accent.name }
    suspend fun setFontSize(size: FontSizePreset) = update { it[KEY_FONT_SIZE] = size.name }
    suspend fun setDisplayDensity(density: DisplayDensity) = update { it[KEY_DISPLAY_DENSITY] = density.name }

    suspend fun setPrivateAccount(enabled: Boolean) = update { it[KEY_PRIVATE_ACCOUNT] = enabled }
    suspend fun addBlockedAccount(handle: String) = updateList(KEY_BLOCKED_ACCOUNTS, handle)
    suspend fun removeBlockedAccount(handle: String) = removeFromList(KEY_BLOCKED_ACCOUNTS, handle)
    suspend fun addMutedAccount(handle: String) = updateList(KEY_MUTED_ACCOUNTS, handle)
    suspend fun removeMutedAccount(handle: String) = removeFromList(KEY_MUTED_ACCOUNTS, handle)

    suspend fun setPushNotifications(enabled: Boolean) = update { it[KEY_PUSH_NOTIFS] = enabled }
    suspend fun setNotifyLikes(enabled: Boolean) = update { it[KEY_NOTIFY_LIKES] = enabled }
    suspend fun setNotifyComments(enabled: Boolean) = update { it[KEY_NOTIFY_COMMENTS] = enabled }
    suspend fun setNotifyMessages(enabled: Boolean) = update { it[KEY_NOTIFY_MESSAGES] = enabled }
    suspend fun setNotifyFollowers(enabled: Boolean) = update { it[KEY_NOTIFY_FOLLOWERS] = enabled }
    suspend fun setNotifyDownloads(enabled: Boolean) = update { it[KEY_NOTIFY_DOWNLOADS] = enabled }

    suspend fun setDownloadLocation(location: String) = update { it[KEY_DOWNLOAD_LOCATION] = location }
    suspend fun setPreferredVideoQuality(quality: String) = update { it[KEY_VIDEO_QUALITY] = quality }
    suspend fun setPreferredAudioQuality(quality: String) = update { it[KEY_AUDIO_QUALITY] = quality }
    suspend fun setWifiOnlyDownloads(enabled: Boolean) = update { it[KEY_WIFI_ONLY] = enabled }
    suspend fun setBackgroundDownloads(enabled: Boolean) = update { it[KEY_BACKGROUND_DOWNLOADS] = enabled }
    suspend fun setAutoMediaDetection(enabled: Boolean) = update { it[KEY_AUTO_MEDIA_DETECT] = enabled }
    suspend fun setAutoPlayVideos(enabled: Boolean) = update { it[KEY_AUTOPLAY_VIDEO] = enabled }
    suspend fun setSubtitlesEnabled(enabled: Boolean) = update { it[KEY_SUBTITLES] = enabled }

    suspend fun setDataSaverEnabled(enabled: Boolean) = update { it[KEY_DATA_SAVER] = enabled }
    suspend fun setAppLanguage(language: String) = update { it[KEY_APP_LANGUAGE] = language }

    suspend fun setLargerText(enabled: Boolean) = update { it[KEY_LARGER_TEXT] = enabled }
    suspend fun setReducedMotion(enabled: Boolean) = update { it[KEY_REDUCED_MOTION] = enabled }
    suspend fun setScreenReaderSupport(enabled: Boolean) = update { it[KEY_SCREEN_READER] = enabled }
    suspend fun setImprovedContrast(enabled: Boolean) = update { it[KEY_IMPROVED_CONTRAST] = enabled }

    suspend fun computeCacheBytes(): Long = withContext(Dispatchers.IO) {
        directorySize(appContext.cacheDir) + (appContext.externalCacheDir?.let { directorySize(it) } ?: 0L)
    }

    suspend fun computeDatabaseBytes(): Long = withContext(Dispatchers.IO) {
        val dbFile = appContext.getDatabasePath("xpro_unlocked.db")
        val walFile = File("${dbFile.path}-wal")
        val shmFile = File("${dbFile.path}-shm")
        (if (dbFile.exists()) dbFile.length() else 0L) +
            (if (walFile.exists()) walFile.length() else 0L) +
            (if (shmFile.exists()) shmFile.length() else 0L)
    }

    suspend fun clearCacheDirectory(): Long = withContext(Dispatchers.IO) {
        appContext.cacheDir?.listFiles()?.forEach { it.deleteRecursively() }
        appContext.externalCacheDir?.listFiles()?.forEach { it.deleteRecursively() }
        computeCacheBytes()
    }

    private suspend fun update(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        dataStore.edit(block)
    }

    private suspend fun updateList(key: Preferences.Key<String>, handle: String) {
        val normalized = handle.trim().let { if (it.startsWith("@")) it else "@$it" }
        if (normalized.length <= 1) return
        dataStore.edit { prefs ->
            val current = prefs[key]?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }?.toMutableList()
                ?: mutableListOf()
            if (normalized !in current) {
                current.add(normalized)
            }
            prefs[key] = current.joinToString(",")
        }
    }

    private suspend fun removeFromList(key: Preferences.Key<String>, handle: String) {
        dataStore.edit { prefs ->
            val current = prefs[key]?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() && it != handle }
                ?: emptyList()
            prefs[key] = current.joinToString(",")
        }
    }

    private fun directorySize(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0L
        if (dir.isFile) return dir.length()
        return dir.listFiles()?.sumOf { directorySize(it) } ?: 0L
    }

    companion object {
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_ACCENT_COLOR = stringPreferencesKey("accent_color")
        private val KEY_FONT_SIZE = stringPreferencesKey("font_size")
        private val KEY_DISPLAY_DENSITY = stringPreferencesKey("display_density")

        private val KEY_PRIVATE_ACCOUNT = booleanPreferencesKey("private_account")
        private val KEY_BLOCKED_ACCOUNTS = stringPreferencesKey("blocked_accounts")
        private val KEY_MUTED_ACCOUNTS = stringPreferencesKey("muted_accounts")

        private val KEY_PUSH_NOTIFS = booleanPreferencesKey("push_notifs")
        private val KEY_NOTIFY_LIKES = booleanPreferencesKey("notify_likes")
        private val KEY_NOTIFY_COMMENTS = booleanPreferencesKey("notify_comments")
        private val KEY_NOTIFY_MESSAGES = booleanPreferencesKey("notify_messages")
        private val KEY_NOTIFY_FOLLOWERS = booleanPreferencesKey("notify_followers")
        private val KEY_NOTIFY_DOWNLOADS = booleanPreferencesKey("notify_downloads")

        private val KEY_DOWNLOAD_LOCATION = stringPreferencesKey("download_location")
        private val KEY_VIDEO_QUALITY = stringPreferencesKey("video_quality")
        private val KEY_AUDIO_QUALITY = stringPreferencesKey("audio_quality")
        private val KEY_WIFI_ONLY = booleanPreferencesKey("wifi_only_downloads")
        private val KEY_BACKGROUND_DOWNLOADS = booleanPreferencesKey("background_downloads")
        private val KEY_AUTO_MEDIA_DETECT = booleanPreferencesKey("auto_media_detect")
        private val KEY_AUTOPLAY_VIDEO = booleanPreferencesKey("autoplay_video")
        private val KEY_SUBTITLES = booleanPreferencesKey("subtitles_enabled")

        private val KEY_DATA_SAVER = booleanPreferencesKey("data_saver")
        private val KEY_APP_LANGUAGE = stringPreferencesKey("app_language")

        private val KEY_LARGER_TEXT = booleanPreferencesKey("larger_text")
        private val KEY_REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
        private val KEY_SCREEN_READER = booleanPreferencesKey("screen_reader")
        private val KEY_IMPROVED_CONTRAST = booleanPreferencesKey("improved_contrast")
    }
}
