package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        PostEntity::class,
        BookmarkFolderEntity::class,
        TypedBookmarkEntity::class,
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
    version = 3,
    exportSchema = false
)
@TypeConverters(XProTypeConverters::class)
abstract class XProDatabase : RoomDatabase() {
    abstract fun xProDao(): XProDao

    companion object {
        @Volatile
        private var INSTANCE: XProDatabase? = null

        fun getInstance(context: Context): XProDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    XProDatabase::class.java,
                    "xpro_unlocked.db"
                )
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
