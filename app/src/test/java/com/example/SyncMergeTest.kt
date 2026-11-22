package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.XProDatabase
import com.example.data.local.XProRepository
import com.example.data.model.BookmarkItemType
import com.example.data.model.TypedBookmarkItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Sync/merge behaviour of the bookmark vault:
 * - remote deletes are applied to synced local rows,
 * - locally-pending rows always win over a remote overwrite,
 * - never-synced (seeded) rows are never reconciled away,
 * - tombstoned deletes prevent resurrection from a stale remote snapshot.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SyncMergeTest {

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

    @Test
    fun `remote delete removes a previously synced row but keeps local pending row`() = runBlocking {
        val now = System.currentTimeMillis()
        // Simulate a row that arrived from Firestore (synced, updatedAt > 0).
        repository.mergeRemoteBookmarks(
            listOf(
                TypedBookmarkItem(
                    id = "tb_remote_1",
                    itemType = BookmarkItemType.LINK,
                    title = "Remote synced",
                    content = "came from firestore",
                    authorName = "Remote Author",
                    authorHandle = "@remote",
                    createdAtMillis = now - 10_000,
                    updatedAtMillis = now - 5_000
                )
            )
        )
        assertTrue(repository.typedBookmarks.first().any { it.id == "tb_remote_1" })

        // A local-only row (pending sync, never pushed).
        repository.addTypedBookmark(
            itemType = BookmarkItemType.LINK,
            title = "Local only",
            content = "never pushed",
            authorName = "Local",
            authorHandle = "@local",
            customId = "tb_local_pending"
        )

        // Remote snapshot is now empty => remote delete must propagate, local pending row must survive.
        repository.mergeRemoteBookmarks(emptyList())

        val ids = repository.typedBookmarks.first().map { it.id }
        assertFalse("synced row should be deleted", "tb_remote_1" in ids)
        assertTrue("local pending row must survive", "tb_local_pending" in ids)
    }

    @Test
    fun `local pending row is not overwritten by remote snapshot`() = runBlocking {
        val now = System.currentTimeMillis()
        repository.addTypedBookmark(
            itemType = BookmarkItemType.LINK,
            title = "Local title",
            content = "local content",
            authorName = "Local",
            authorHandle = "@local",
            customId = "tb_pending_wins"
        )
        repository.mergeRemoteBookmarks(
            listOf(
                TypedBookmarkItem(
                    id = "tb_pending_wins",
                    itemType = BookmarkItemType.LINK,
                    title = "Remote title",
                    content = "remote content",
                    authorName = "Remote",
                    authorHandle = "@remote",
                    createdAtMillis = now - 10_000,
                    updatedAtMillis = now
                )
            )
        )
        val stored = repository.typedBookmarks.first().first { it.id == "tb_pending_wins" }
        assertEquals("Local title", stored.title)
    }

    @Test
    fun `empty remote snapshot keeps seeded never-synced rows`() = runBlocking {
        val seededIds = repository.typedBookmarks.first().map { it.id }
        assertTrue(seededIds.isNotEmpty())
        repository.mergeRemoteBookmarks(emptyList())
        assertEquals(seededIds.toSet(), repository.typedBookmarks.first().map { it.id }.toSet())
    }

    @Test
    fun `assignBookmark keeps original createdAt when re-saving same post`() = runBlocking {
        val first = repository.assignBookmark("post_poll_1", null)
            ?: error("assignBookmark returned null for seed post post_poll_1")
        Thread.sleep(5)
        repository.assignBookmark("post_poll_1", "folder_eng")
        val stored = repository.typedBookmarks.first().first { it.id == first.id }
        assertEquals(first.createdAtMillis, stored.createdAtMillis)
        assertEquals("folder_eng", stored.folderId)
    }

    @Test
    fun `deleting a playlist detaches media items that referenced it`() = runBlocking {
        repository.createPlaylist("Mixtape", "Test playlist")
        val playlist = repository.playlists.first().first { it.name == "Mixtape" }
        val media = repository.mediaLibrary.first().first()
        repository.assignMediaToPlaylist(media.id, playlist.id)
        assertEquals(playlist.id, repository.mediaLibrary.first().first { it.id == media.id }.playlistId)

        repository.deletePlaylist(playlist.id)

        assertNull(repository.mediaLibrary.first().first { it.id == media.id }.playlistId)
    }
}
