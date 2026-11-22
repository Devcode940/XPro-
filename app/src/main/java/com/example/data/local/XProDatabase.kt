package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.BuildConfig

@Database(
    entities = [
        PostEntity::class,
        BookmarkFolderEntity::class,
        TypedBookmarkEntity::class,
        PendingRemoteDeleteEntity::class,
        UserProfileEntity::class,
        TrendEntity::class,
        SuggestedUserEntity::class,
        NotificationEntity::class,
        MessageConversationEntity::class,
        DirectMessageEntity::class,
        XListEntity::class,
        CommunityEntity::class,
        MediaItemEntity::class,
        PlaylistEntity::class,
        SavedPageEntity::class,
        BrowserHistoryEntity::class,
        FeedbackEntity::class
    ],
    version = 5,
    exportSchema = true
)
@TypeConverters(XProTypeConverters::class)
abstract class XProDatabase : RoomDatabase() {
    abstract fun xProDao(): XProDao

    companion object {
        @Volatile
        private var INSTANCE: XProDatabase? = null

        /**
         * v3 -> v4: adds sync bookkeeping for the bookmark vault and folders.
         *
         * Rows upgraded from v3 are flagged pendingSync = 1 so the first sync after upgrade
         * pushes them to Firestore instead of reconciling them away (local-only writes made
         * before this version existed were never pushed, and the reconciler would otherwise
         * delete them as "synced rows missing remotely").
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE bookmarks_vault ADD COLUMN updatedAtMillis INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE bookmarks_vault ADD COLUMN pendingSync INTEGER NOT NULL DEFAULT 1")
                // updatedAtMillis stays 0 => "never synced" marker: reconcile never deletes such rows.
                db.execSQL("ALTER TABLE bookmark_folders ADD COLUMN updatedAtMillis INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE bookmark_folders ADD COLUMN pendingSync INTEGER NOT NULL DEFAULT 1")
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS pending_remote_deletes (
                        id TEXT PRIMARY KEY NOT NULL,
                        kind TEXT NOT NULL,
                        createdAtMillis INTEGER NOT NULL
                    )""".trimIndent()
                )
            }
        }

        /**
         * v4 -> v5: scopes synced rows to the Google account that owns them.
         *
         * Nullable with no SQL default on purpose: `ALTER TABLE ... ADD COLUMN` needs no default
         * for a nullable column, and Room validates the declared schema against the migrated one.
         * Legacy rows come out unowned (device-local); [XProDao.claimUnownedTypedBookmarks] binds
         * them to the first account that signs in.
         *
         * The two `UPDATE`s undo an over-eager side effect of v3 -> v4, which flagged *every*
         * pre-existing row as `pendingSync = 1` — including seeded demo content. Rows with
         * `updatedAtMillis = 0` were never pushed anywhere, so they must not be uploaded to a
         * real account on first sign-in.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE bookmarks_vault ADD COLUMN ownerUid TEXT")
                db.execSQL("ALTER TABLE bookmark_folders ADD COLUMN ownerUid TEXT")
                db.execSQL("ALTER TABLE pending_remote_deletes ADD COLUMN ownerUid TEXT")
                db.execSQL("UPDATE bookmarks_vault SET pendingSync = 0 WHERE updatedAtMillis = 0")
                db.execSQL("UPDATE bookmark_folders SET pendingSync = 0 WHERE updatedAtMillis = 0")
            }
        }

        fun getInstance(context: Context): XProDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    XProDatabase::class.java,
                    "xpro_unlocked.db"
                )
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
                    // Destructive fallback is a debug-only convenience. Shipping it in release
                    // turns any forgotten migration into silent deletion of user bookmarks.
                    .apply {
                        if (BuildConfig.DEBUG) {
                            fallbackToDestructiveMigration(dropAllTables = true)
                        }
                    }
                    .build()
                    .also { INSTANCE = it }
            }
        }

        @androidx.annotation.VisibleForTesting
        internal fun resetInstanceForTesting() {
            XProRepository.resetInstanceForTesting()
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
            }
        }
    }
}
