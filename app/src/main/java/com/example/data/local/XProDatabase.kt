package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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
    version = 4,
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

        fun getInstance(context: Context): XProDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    XProDatabase::class.java,
                    "xpro_unlocked.db"
                )
                    .addMigrations(MIGRATION_3_4)
                    .fallbackToDestructiveMigration(dropAllTables = true)
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
