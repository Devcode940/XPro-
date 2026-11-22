package com.devcode.xpro.mediadownloader

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MediaDownloaderTest {

    @Test
    fun `validates and detects mp4 webm and audio media types`() {
        val mp4 = MediaUrlValidator.validateAndDetect("https://cdn.example.com/clips/launch_demo.mp4?token=abc")
        assertTrue(mp4.isValid)
        assertEquals(SupportedMediaType.MP4, mp4.mediaType)
        assertEquals("launch_demo.mp4", mp4.suggestedFileName)

        val webm = MediaUrlValidator.validateAndDetect("https://media.example.org/trailer.webm")
        assertTrue(webm.isValid)
        assertEquals(SupportedMediaType.WEBM, webm.mediaType)

        val mp3 = MediaUrlValidator.validateAndDetect("https://audio.example.net/podcast_ep4.mp3")
        assertTrue(mp3.isValid)
        assertEquals(SupportedMediaType.MP3, mp3.mediaType)
        assertEquals(MediaCategory.AUDIO, mp3.mediaType?.category)

        val queryHint = MediaUrlValidator.validateAndDetect("https://stream.example.com/asset?id=99&format=m4a")
        assertTrue(queryHint.isValid)
        assertEquals(SupportedMediaType.M4A, queryHint.mediaType)
    }

    @Test
    fun `rejects invalid urls cleartext http private hosts and unsupported extensions`() {
        val empty = MediaUrlValidator.validateAndDetect("   ")
        assertFalse(empty.isValid)
        assertNotNull(empty.errorMessage)

        val badScheme = MediaUrlValidator.validateAndDetect("ftp://example.com/video.mp4")
        assertFalse(badScheme.isValid)

        val cleartext = MediaUrlValidator.validateAndDetect("http://cdn.example.com/video.mp4")
        assertFalse(cleartext.isValid)

        val loopback = MediaUrlValidator.validateAndDetect("https://localhost/video.mp4")
        assertFalse(loopback.isValid)

        val privateIp = MediaUrlValidator.validateAndDetect("https://192.168.1.42/video.mp4")
        assertFalse(privateIp.isValid)

        val unsupported = MediaUrlValidator.validateAndDetect("https://example.com/page.html")
        assertFalse(unsupported.isValid)
        assertNull(unsupported.mediaType)
    }

    @Test
    fun `enqueues download deduplicates filename and persists queue across restarts`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        SystemMediaDownloader.resetInstanceForTesting()
        val downloader = SystemMediaDownloader.getInstance(context)
        downloader.clearHistory()

        var startedItem: MediaDownloadItem? = null
        downloader.setCallback(object : DownloadCallback {
            override fun onDownloadStarted(item: MediaDownloadItem) {
                startedItem = item
            }

            override fun onDownloadSucceeded(item: MediaDownloadItem) {}
            override fun onDownloadFailed(item: MediaDownloadItem, reason: String) {}
        })

        val first = downloader.enqueueDownload(
            url = "https://example.com/sample.mp4",
            mediaType = SupportedMediaType.MP4,
            fileName = "sample.mp4"
        )
        assertTrue(first.isSuccess)
        assertNotNull(startedItem)
        assertEquals("sample.mp4", startedItem?.fileName)
        assertEquals(DownloadStatus.QUEUED, startedItem?.status)

        val second = downloader.enqueueDownload(
            url = "https://example.com/sample.mp4",
            mediaType = SupportedMediaType.MP4,
            fileName = "sample.mp4"
        )
        assertTrue(second.isSuccess)
        assertEquals("sample_2.mp4", second.getOrNull()?.fileName)
        assertEquals(2, downloader.downloads.value.size)
        // A fresh instance reads the persisted queue back (process restart simulation).
        SystemMediaDownloader.resetInstanceForTesting()
        val restoredDownloader = SystemMediaDownloader.getInstance(context)
        assertEquals(2, restoredDownloader.downloads.value.size)
        restoredDownloader.clearHistory()
        assertTrue(restoredDownloader.downloads.value.isEmpty())
        SystemMediaDownloader.resetInstanceForTesting()
    }

    @Test
    fun `every caller shares one queue and one persistence writer`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        SystemMediaDownloader.resetInstanceForTesting()

        val downloadsScreen = SystemMediaDownloader.getInstance(context)
        val settingsScreen = SystemMediaDownloader.getInstance(context)
        downloadsScreen.clearHistory()

        assertTrue(
            "settings must observe the same queue as the downloads screen",
            downloadsScreen === settingsScreen
        )

        downloadsScreen.enqueueDownload(
            url = "https://example.com/shared.mp4",
            mediaType = SupportedMediaType.MP4,
            fileName = "shared.mp4"
        )
        assertEquals(1, settingsScreen.downloads.value.size)

        // A second writer used to clobber the shared prefs blob with its own stale list.
        settingsScreen.refreshActiveDownloads()
        assertEquals(1, downloadsScreen.downloads.value.size)

        SystemMediaDownloader.resetInstanceForTesting()
    }
}
