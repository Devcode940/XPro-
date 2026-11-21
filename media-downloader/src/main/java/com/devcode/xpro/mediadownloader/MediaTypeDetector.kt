package com.devcode.xpro.mediadownloader

import java.net.URI
import java.util.Locale

enum class MediaCategory(val label: String) {
    VIDEO("Video"),
    AUDIO("Audio")
}

enum class SupportedMediaType(
    val extension: String,
    val mimeType: String,
    val displayName: String,
    val category: MediaCategory
) {
    MP4("mp4", "video/mp4", "MP4 Video", MediaCategory.VIDEO),
    WEBM("webm", "video/webm", "WebM Video", MediaCategory.VIDEO),
    MKV("mkv", "video/x-matroska", "Matroska Video", MediaCategory.VIDEO),
    MOV("mov", "video/quicktime", "QuickTime Video", MediaCategory.VIDEO),
    MP3("mp3", "audio/mpeg", "MP3 Audio", MediaCategory.AUDIO),
    M4A("m4a", "audio/mp4", "M4A Audio", MediaCategory.AUDIO),
    WAV("wav", "audio/wav", "WAV Audio", MediaCategory.AUDIO),
    OGG("ogg", "audio/ogg", "OGG Audio", MediaCategory.AUDIO),
    FLAC("flac", "audio/flac", "FLAC Audio", MediaCategory.AUDIO),
    AAC("aac", "audio/aac", "AAC Audio", MediaCategory.AUDIO);

    companion object {
        fun fromExtension(ext: String): SupportedMediaType? {
            val clean = ext.trim().lowercase(Locale.US).removePrefix(".")
            return entries.firstOrNull { it.extension == clean }
        }

        fun fromMimeType(mime: String): SupportedMediaType? {
            val clean = mime.trim().lowercase(Locale.US).substringBefore(";")
            return entries.firstOrNull { it.mimeType == clean }
        }
    }
}

data class UrlValidationResult(
    val isValid: Boolean,
    val normalizedUrl: String,
    val mediaType: SupportedMediaType?,
    val suggestedFileName: String,
    val errorMessage: String?
)

object MediaUrlValidator {

    private val hostRegex = Regex("^[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$|^\\d{1,3}(\\.\\d{1,3}){3}$")
    private val ipv4Regex = Regex("^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$")

    fun validateAndDetect(rawInput: String): UrlValidationResult {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) {
            return UrlValidationResult(
                isValid = false,
                normalizedUrl = "",
                mediaType = null,
                suggestedFileName = "",
                errorMessage = "Enter a direct https:// media URL (e.g. .mp4, .webm, .mp3)"
            )
        }

        val parsedUri = try {
            URI(trimmed)
        } catch (_: Exception) {
            return UrlValidationResult(
                isValid = false,
                normalizedUrl = trimmed,
                mediaType = null,
                suggestedFileName = "",
                errorMessage = "Malformed URL syntax. Include https://"
            )
        }

        val scheme = parsedUri.scheme?.lowercase(Locale.US)
        if (scheme != "https") {
            return UrlValidationResult(
                isValid = false,
                normalizedUrl = trimmed,
                mediaType = null,
                suggestedFileName = "",
                errorMessage = "Only secure https:// media URLs are supported."
            )
        }

        val host = parsedUri.host?.lowercase(Locale.US)
        if (host.isNullOrBlank() || isPrivateOrLoopbackHost(host) || !hostRegex.matches(host)) {
            return UrlValidationResult(
                isValid = false,
                normalizedUrl = trimmed,
                mediaType = null,
                suggestedFileName = "",
                errorMessage = "URL must target a valid public domain host"
            )
        }

        val detectedType = detectMediaType(parsedUri)
        if (detectedType == null) {
            val supportedList = SupportedMediaType.entries.joinToString(", ") { it.extension.uppercase(Locale.US) }
            return UrlValidationResult(
                isValid = false,
                normalizedUrl = trimmed,
                mediaType = null,
                suggestedFileName = "",
                errorMessage = "Unsupported or missing media extension. Supported: $supportedList"
            )
        }

        val fileName = buildFileName(parsedUri, detectedType)
        return UrlValidationResult(
            isValid = true,
            normalizedUrl = trimmed,
            mediaType = detectedType,
            suggestedFileName = fileName,
            errorMessage = null
        )
    }

    fun detectMediaType(uri: URI): SupportedMediaType? {
        val path = uri.path.orEmpty()
        val lastSegment = path.substringAfterLast('/', "")
        val extFromPath = lastSegment.substringAfterLast('.', "")
        if (extFromPath.isNotBlank() && lastSegment.contains('.')) {
            SupportedMediaType.fromExtension(extFromPath)?.let { return it }
        }

        // Fallback: inspect query parameters such as ?format=mp4 or ?mime=video/webm
        val query = uri.rawQuery.orEmpty()
        if (query.isNotBlank()) {
            query.split('&').forEach { pair ->
                val key = pair.substringBefore('=').lowercase(Locale.US)
                val value = pair.substringAfter('=', "").lowercase(Locale.US)
                if (key in setOf("format", "ext", "type", "container")) {
                    SupportedMediaType.fromExtension(value)?.let { return it }
                }
                if (key in setOf("mime", "mimetype", "content_type")) {
                    val decoded = value.replace("%2f", "/")
                    SupportedMediaType.fromMimeType(decoded)?.let { return it }
                }
            }
        }

        return null
    }

    private fun buildFileName(uri: URI, type: SupportedMediaType): String {
        val rawSegment = uri.path.orEmpty()
            .substringAfterLast('/', "")
            .replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val base = rawSegment.substringBeforeLast('.', rawSegment).ifBlank { "xpro_media_${System.currentTimeMillis()}" }
        return "$base.${type.extension}"
    }

    private fun isPrivateOrLoopbackHost(host: String): Boolean {
        if (host == "localhost" || host.endsWith(".local")) return true
        val match = ipv4Regex.matchEntire(host) ?: return false
        val octets = match.groupValues.drop(1).mapNotNull { it.toIntOrNull() }
        if (octets.size != 4 || octets.any { it !in 0..255 }) return true
        val (a, b, _, _) = octets
        return a == 0 ||
            a == 10 ||
            a == 127 ||
            (a == 169 && b == 254) ||
            (a == 172 && b in 16..31) ||
            (a == 192 && b == 168)
    }
}
