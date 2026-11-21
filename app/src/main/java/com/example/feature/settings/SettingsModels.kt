package com.example.feature.settings

enum class ThemeMode(val label: String) {
    DARK("Dark (OLED)"),
    LIGHT("Light"),
    SYSTEM("System Default")
}

enum class AccentColorOption(val label: String, val hex: Long) {
    X_BLUE("X Blue", 0xFF1D9BF0),
    GOLD("Gold Pro", 0xFFE2B714),
    EMERALD("Emerald Shield", 0xFF00BA7C),
    FOUNDER_PURPLE("Founder Prism", 0xFFA855F7)
}

enum class FontSizePreset(val label: String, val scale: Float) {
    SMALL("Small", 0.90f),
    MEDIUM("Standard", 1.00f),
    LARGE("Large", 1.12f),
    XLARGE("Extra Large", 1.25f)
}

enum class DisplayDensity(val label: String, val spacingScale: Float) {
    COMPACT("Compact", 0.85f),
    STANDARD("Standard", 1.0f),
    COMFORTABLE("Comfortable", 1.18f)
}

data class ActiveSessionInfo(
    val id: String,
    val deviceName: String,
    val location: String,
    val lastActive: String,
    val isCurrent: Boolean
)

data class UserSettingsState(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val accentColor: AccentColorOption = AccentColorOption.X_BLUE,
    val fontSize: FontSizePreset = FontSizePreset.MEDIUM,
    val displayDensity: DisplayDensity = DisplayDensity.STANDARD,

    val privateAccount: Boolean = false,
    val blockedAccounts: List<String> = listOf("@spam_bot_99"),
    val mutedAccounts: List<String> = listOf("@crypto_promo_daily"),

    val pushNotifications: Boolean = true,
    val notifyLikes: Boolean = true,
    val notifyComments: Boolean = true,
    val notifyMessages: Boolean = true,
    val notifyFollowers: Boolean = true,
    val notifyDownloads: Boolean = true,

    val downloadLocation: String = "Downloads/XPro",
    val preferredVideoQuality: String = "1080p HD",
    val preferredAudioQuality: String = "320 kbps",
    val wifiOnlyDownloads: Boolean = false,
    val backgroundDownloads: Boolean = true,
    val autoMediaDetection: Boolean = true,
    val autoPlayVideos: Boolean = true,
    val subtitlesEnabled: Boolean = true,

    val dataSaverEnabled: Boolean = false,
    val appLanguage: String = "English",

    val largerText: Boolean = false,
    val reducedMotion: Boolean = false,
    val screenReaderSupport: Boolean = true,
    val improvedContrast: Boolean = false
)
