package com.devcode.xpro.mediadownloader

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

enum class DownloadStatus(val label: String) {
    QUEUED("Queued"),
    DOWNLOADING("Downloading"),
    /** Android DownloadManager cannot pause; a "paused" item is removed and re-downloads from byte 0 on resume. */
    PAUSED("Paused"),
    SUCCEEDED("Completed"),
    FAILED("Failed"),
    CANCELLED("Cancelled")
}

data class MediaDownloadItem(
    val downloadId: Long,
    val url: String,
    val fileName: String,
    val mediaType: SupportedMediaType,
    val status: DownloadStatus,
    val localUri: String? = null,
    val errorReason: String? = null,
    val bytesDownloaded: Long = 0L,
    val bytesTotal: Long = -1L,
    val resolution: String = "Source",
    val wifiOnly: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis()
)

interface DownloadCallback {
    fun onDownloadStarted(item: MediaDownloadItem)
    fun onDownloadSucceeded(item: MediaDownloadItem)
    fun onDownloadFailed(item: MediaDownloadItem, reason: String)
}

class SystemMediaDownloader(private val context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val downloadManager: DownloadManager? =
        appContext.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager

    private val _downloads = MutableStateFlow<List<MediaDownloadItem>>(emptyList())
    val downloads: StateFlow<List<MediaDownloadItem>> = _downloads.asStateFlow()

    private var activeCallback: DownloadCallback? = null
    private var receiverRegistered = false

    private val completionReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (intent?.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return
            val completedId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
            if (completedId != -1L) {
                refreshDownloadStatus(completedId)
            }
        }
    }

    init {
        restorePersistedQueue()
        registerCompletionReceiver()
        refreshActiveDownloads()
    }

    fun setCallback(callback: DownloadCallback?) {
        activeCallback = callback
    }

    private fun registerCompletionReceiver() {
        if (receiverRegistered) return
        try {
            val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            ContextCompat.registerReceiver(
                appContext,
                completionReceiver,
                filter,
                ContextCompat.RECEIVER_EXPORTED
            )
            receiverRegistered = true
        } catch (_: Exception) {
            receiverRegistered = false
        }
    }

    fun enqueueDownload(
        url: String,
        mediaType: SupportedMediaType,
        fileName: String,
        resolution: String = if (mediaType.category == MediaCategory.VIDEO) "1080p HD" else "320 kbps",
        wifiOnly: Boolean = false,
        showNotifications: Boolean = true
    ): Result<MediaDownloadItem> {
        val dm = downloadManager ?: return Result.failure(
            IllegalStateException("Android DownloadManager service is unavailable on this device.")
        )

        return try {
            val uri = Uri.parse(url)
            val resolvedFileName = resolveUniqueFileName(fileName)
            val request = DownloadManager.Request(uri).apply {
                setTitle(resolvedFileName)
                setDescription("XTRA · ${mediaType.displayName}")
                setMimeType(mediaType.mimeType)
                setAllowedOverMetered(!wifiOnly)
                setAllowedOverRoaming(!wifiOnly)
                setNotificationVisibility(
                    if (showNotifications) {
                        DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                    } else {
                        DownloadManager.Request.VISIBILITY_HIDDEN
                    }
                )
                try {
                    setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "XPro/$resolvedFileName")
                } catch (_: Exception) {
                    setDestinationInExternalFilesDir(appContext, Environment.DIRECTORY_DOWNLOADS, resolvedFileName)
                }
            }

            val downloadId = dm.enqueue(request)
            val item = MediaDownloadItem(
                downloadId = downloadId,
                url = url,
                fileName = resolvedFileName,
                mediaType = mediaType,
                status = DownloadStatus.QUEUED,
                resolution = resolution,
                wifiOnly = wifiOnly
            )
            _downloads.update { current -> listOf(item) + current }
            persistQueue(_downloads.value)
            activeCallback?.onDownloadStarted(item)
            Result.success(item)
        } catch (e: SecurityException) {
            val reason = e.message ?: "Download permission or storage security failure"
            val failedItem = MediaDownloadItem(
                downloadId = -System.currentTimeMillis(),
                url = url,
                fileName = fileName,
                mediaType = mediaType,
                status = DownloadStatus.FAILED,
                errorReason = reason,
                resolution = resolution,
                wifiOnly = wifiOnly
            )
            _downloads.update { listOf(failedItem) + it }
            persistQueue(_downloads.value)
            activeCallback?.onDownloadFailed(failedItem, reason)
            Result.failure(e)
        } catch (e: IllegalArgumentException) {
            val reason = e.message ?: "Invalid download URI"
            activeCallback?.onDownloadFailed(
                MediaDownloadItem(-1L, url, fileName, mediaType, DownloadStatus.FAILED, errorReason = reason),
                reason
            )
            Result.failure(e)
        } catch (e: Exception) {
            val reason = e.message ?: "Failed to enqueue download"
            activeCallback?.onDownloadFailed(
                MediaDownloadItem(-1L, url, fileName, mediaType, DownloadStatus.FAILED, errorReason = reason),
                reason
            )
            Result.failure(e)
        }
    }

    /**
     * Android's DownloadManager has no pause API. This removes the in-progress download
     * (its partial file is discarded) and keeps it listed as PAUSED so the user can
     * re-enqueue it later; resuming re-downloads from byte 0.
     */
    fun pauseDownload(downloadId: Long) {
        val existing = _downloads.value.firstOrNull { it.downloadId == downloadId } ?: return
        if (existing.status != DownloadStatus.DOWNLOADING && existing.status != DownloadStatus.QUEUED) return
        if (downloadId >= 0L) {
            runCatching { downloadManager?.remove(downloadId) }
        }
        _downloads.update { list ->
            list.map {
                if (it.downloadId == downloadId) {
                    it.copy(status = DownloadStatus.PAUSED, errorReason = null)
                } else it
            }
        }
        persistQueue(_downloads.value)
    }

    fun resumeDownload(downloadId: Long, wifiOnly: Boolean = false, showNotifications: Boolean = true): Result<MediaDownloadItem> {
        val existing = _downloads.value.firstOrNull { it.downloadId == downloadId }
            ?: return Result.failure(IllegalArgumentException("Download item not found"))
        val dm = downloadManager ?: return Result.failure(IllegalStateException("DownloadManager unavailable"))
        return try {
            val uri = Uri.parse(existing.url)
            val request = DownloadManager.Request(uri).apply {
                setTitle(existing.fileName)
                setDescription("XTRA · Resumed ${existing.mediaType.displayName}")
                setMimeType(existing.mediaType.mimeType)
                setAllowedOverMetered(!wifiOnly)
                setAllowedOverRoaming(!wifiOnly)
                setNotificationVisibility(
                    if (showNotifications) {
                        DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                    } else {
                        DownloadManager.Request.VISIBILITY_HIDDEN
                    }
                )
                try {
                    setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "XPro/${existing.fileName}")
                } catch (_: Exception) {
                    setDestinationInExternalFilesDir(appContext, Environment.DIRECTORY_DOWNLOADS, existing.fileName)
                }
            }
            val newId = dm.enqueue(request)
            val resumed = existing.copy(
                downloadId = newId,
                status = DownloadStatus.QUEUED,
                errorReason = null,
                wifiOnly = wifiOnly
            )
            _downloads.update { list ->
                list.map { if (it.downloadId == downloadId) resumed else it }
            }
            persistQueue(_downloads.value)
            activeCallback?.onDownloadStarted(resumed)
            Result.success(resumed)
        } catch (e: Exception) {
            val failed = existing.copy(
                status = DownloadStatus.FAILED,
                errorReason = e.message ?: "Unable to resume download"
            )
            _downloads.update { list ->
                list.map { if (it.downloadId == downloadId) failed else it }
            }
            persistQueue(_downloads.value)
            Result.failure(e)
        }
    }

    fun retryDownload(downloadId: Long, wifiOnly: Boolean = false, showNotifications: Boolean = true): Result<MediaDownloadItem> {
        return resumeDownload(downloadId, wifiOnly, showNotifications)
    }

    fun cancelDownload(downloadId: Long) {
        if (downloadId >= 0L) {
            runCatching { downloadManager?.remove(downloadId) }
        }
        _downloads.update { list ->
            list.map {
                if (it.downloadId == downloadId) {
                    it.copy(status = DownloadStatus.CANCELLED, errorReason = "Cancelled by user")
                } else it
            }
        }
        persistQueue(_downloads.value)
    }

    /**
     * Removes the item from the queue (and asks DownloadManager to cancel/remove it).
     * When [deleteLocalFile] is set, also tries to delete the downloaded file; returns false
     * when that file could not be removed (it may still exist in Downloads).
     */
    fun deleteDownload(downloadId: Long, deleteLocalFile: Boolean = false): Boolean {
        val target = _downloads.value.firstOrNull { it.downloadId == downloadId }
        var fileDeleted = !deleteLocalFile
        if (downloadId >= 0L) {
            runCatching { downloadManager?.remove(downloadId) }
        }
        if (deleteLocalFile && target?.localUri != null) {
            fileDeleted = runCatching {
                val parsed = Uri.parse(target.localUri)
                parsed.scheme == "file" && parsed.path?.let { File(it).delete() } == true
            }.getOrDefault(false)
        }
        _downloads.update { list -> list.filterNot { it.downloadId == downloadId } }
        persistQueue(_downloads.value)
        return fileDeleted
    }

    fun refreshActiveDownloads() {
        val activeIds = _downloads.value
            .filter { it.status == DownloadStatus.QUEUED || it.status == DownloadStatus.DOWNLOADING }
            .map { it.downloadId }
            .filter { it >= 0L }
        activeIds.forEach { refreshDownloadStatus(it) }
    }

    fun refreshDownloadStatus(downloadId: Long) {
        val dm = downloadManager ?: return
        val existing = _downloads.value.firstOrNull { it.downloadId == downloadId } ?: return
        if (existing.status == DownloadStatus.PAUSED) return
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor = try {
            dm.query(query)
        } catch (_: Exception) {
            null
        } ?: return

        cursor.use { c ->
            if (!c.moveToFirst()) {
                // DownloadManager no longer knows this id (removed externally, rebooted device,
                // or cleared state). Surface it as failed instead of leaving a zombie QUEUED row.
                val missingItem = existing.copy(
                    status = DownloadStatus.FAILED,
                    errorReason = "Download is no longer registered with the system download service"
                )
                if (missingItem != existing) {
                    _downloads.update { list ->
                        list.map { if (it.downloadId == downloadId) missingItem else it }
                    }
                    persistQueue(_downloads.value)
                }
                return
            }
            val statusCol = c.getColumnIndex(DownloadManager.COLUMN_STATUS)
            val reasonCol = c.getColumnIndex(DownloadManager.COLUMN_REASON)
            val uriCol = c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
            val downloadedCol = c.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
            val totalCol = c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)

            val rawStatus = if (statusCol >= 0) c.getInt(statusCol) else DownloadManager.STATUS_PENDING
            val rawReason = if (reasonCol >= 0) c.getInt(reasonCol) else 0
            val localUri = if (uriCol >= 0) c.getString(uriCol) else null
            val bytesDownloaded = if (downloadedCol >= 0) c.getLong(downloadedCol) else 0L
            val bytesTotal = if (totalCol >= 0) c.getLong(totalCol) else -1L

            val mappedStatus = when (rawStatus) {
                DownloadManager.STATUS_SUCCESSFUL -> DownloadStatus.SUCCEEDED
                DownloadManager.STATUS_FAILED -> DownloadStatus.FAILED
                DownloadManager.STATUS_PAUSED -> DownloadStatus.PAUSED
                DownloadManager.STATUS_RUNNING -> DownloadStatus.DOWNLOADING
                else -> DownloadStatus.QUEUED
            }
            val errorText = if (mappedStatus == DownloadStatus.FAILED) mapFailureReason(rawReason) else null

            val updatedItem = existing.copy(
                status = mappedStatus,
                localUri = localUri,
                errorReason = errorText,
                bytesDownloaded = bytesDownloaded,
                bytesTotal = bytesTotal
            )
            // Persist only on actual change — this is called every poll tick (~750ms).
            if (updatedItem != existing) {
                _downloads.update { list ->
                    list.map { if (it.downloadId == downloadId) updatedItem else it }
                }
                persistQueue(_downloads.value)
            }

            if (mappedStatus == DownloadStatus.SUCCEEDED && existing.status != DownloadStatus.SUCCEEDED) {
                activeCallback?.onDownloadSucceeded(updatedItem)
            } else if (mappedStatus == DownloadStatus.FAILED && existing.status != DownloadStatus.FAILED) {
                activeCallback?.onDownloadFailed(updatedItem, errorText ?: "Download failed")
            }
        }
    }

    fun clearHistory() {
        _downloads.value.forEach {
            if (it.downloadId >= 0L && (it.status == DownloadStatus.QUEUED || it.status == DownloadStatus.DOWNLOADING)) {
                runCatching { downloadManager?.remove(it.downloadId) }
            }
        }
        _downloads.value = emptyList()
        prefs.edit().remove(KEY_QUEUE_JSON).apply()
    }

    fun totalDownloadedBytes(): Long {
        return _downloads.value
            .filter { it.status == DownloadStatus.SUCCEEDED }
            .sumOf { item ->
                if (item.bytesTotal > 0L) item.bytesTotal else item.bytesDownloaded.coerceAtLeast(0L)
            }
    }

    fun cleanup() {
        if (receiverRegistered) {
            try {
                appContext.unregisterReceiver(completionReceiver)
            } catch (_: Exception) {
            }
            receiverRegistered = false
        }
        activeCallback = null
    }

    private fun resolveUniqueFileName(requestedFileName: String): String {
        val existingNames = _downloads.value.map { it.fileName }.toSet()
        val base = requestedFileName.substringBeforeLast('.', requestedFileName)
        val ext = requestedFileName.substringAfterLast('.', "")
        val suffixExt = if (ext.isNotEmpty() && requestedFileName.contains('.')) ".$ext" else ""

        fun existsOnDisk(candidate: String): Boolean {
            return try {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                File(File(downloadsDir, "XPro"), candidate).exists()
            } catch (_: Exception) {
                false
            }
        }

        if (requestedFileName !in existingNames && !existsOnDisk(requestedFileName)) {
            return requestedFileName
        }

        var counter = 2
        while (counter < 10_000) {
            val candidate = "${base}_$counter$suffixExt"
            if (candidate !in existingNames && !existsOnDisk(candidate)) {
                return candidate
            }
            counter++
        }
        return "${base}_${System.currentTimeMillis()}$suffixExt"
    }

    private fun persistQueue(items: List<MediaDownloadItem>) {
        val array = JSONArray()
        items.take(50).forEach { item ->
            val obj = JSONObject().apply {
                put("downloadId", item.downloadId)
                put("url", item.url)
                put("fileName", item.fileName)
                put("mediaType", item.mediaType.name)
                put("status", item.status.name)
                put("localUri", item.localUri ?: JSONObject.NULL)
                put("errorReason", item.errorReason ?: JSONObject.NULL)
                put("bytesDownloaded", item.bytesDownloaded)
                put("bytesTotal", item.bytesTotal)
                put("resolution", item.resolution)
                put("wifiOnly", item.wifiOnly)
                put("createdAtMillis", item.createdAtMillis)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_QUEUE_JSON, array.toString()).apply()
    }

    private fun restorePersistedQueue() {
        val raw = prefs.getString(KEY_QUEUE_JSON, null) ?: return
        val parsed = try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val typeName = obj.optString("mediaType", "")
                    val statusName = obj.optString("status", DownloadStatus.QUEUED.name)
                    val type = runCatching { SupportedMediaType.valueOf(typeName) }.getOrNull() ?: continue
                    val status = runCatching { DownloadStatus.valueOf(statusName) }.getOrDefault(DownloadStatus.QUEUED)
                    add(
                        MediaDownloadItem(
                            downloadId = obj.optLong("downloadId", -1L),
                            url = obj.optString("url", ""),
                            fileName = obj.optString("fileName", "xpro_media.${type.extension}"),
                            mediaType = type,
                            status = status,
                            localUri = obj.optString("localUri").takeIf { it.isNotBlank() && it != "null" },
                            errorReason = obj.optString("errorReason").takeIf { it.isNotBlank() && it != "null" },
                            bytesDownloaded = obj.optLong("bytesDownloaded", 0L),
                            bytesTotal = obj.optLong("bytesTotal", -1L),
                            resolution = obj.optString("resolution", "Source"),
                            wifiOnly = obj.optBoolean("wifiOnly", false),
                            createdAtMillis = obj.optLong("createdAtMillis", System.currentTimeMillis())
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
        _downloads.value = parsed
    }

    companion object {
        private const val PREFS_NAME = "xpro_media_downloader_prefs"
        private const val KEY_QUEUE_JSON = "download_queue_json"
    }

    private fun mapFailureReason(reasonCode: Int): String {
        return when (reasonCode) {
            DownloadManager.ERROR_CANNOT_RESUME -> "Cannot resume interrupted download"
            DownloadManager.ERROR_DEVICE_NOT_FOUND -> "External storage device not found"
            DownloadManager.ERROR_FILE_ALREADY_EXISTS -> "Destination file already exists"
            DownloadManager.ERROR_FILE_ERROR -> "Storage file I/O error"
            DownloadManager.ERROR_HTTP_DATA_ERROR -> "HTTP data transfer error"
            DownloadManager.ERROR_INSUFFICIENT_SPACE -> "Insufficient storage space"
            DownloadManager.ERROR_TOO_MANY_REDIRECTS -> "Too many HTTP redirects"
            DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> "Unhandled HTTP status code"
            else -> "Download failed (code $reasonCode)"
        }
    }
}
