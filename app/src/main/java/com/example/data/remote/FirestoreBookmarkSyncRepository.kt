package com.example.data.remote

import android.content.Context
import androidx.annotation.Keep
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await

@Keep
@IgnoreExtraProperties
data class SyncedBookmarkDoc(
    @com.google.firebase.firestore.DocumentId
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val content: String = "",
    val authorName: String = "",
    val authorHandle: String = "",
    val itemType: String = "POST",
    val mediaUrl: String? = null,
    val linkUrl: String? = null,
    val folderId: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SyncedBookmarkFolderDoc(
    @com.google.firebase.firestore.DocumentId
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val description: String = "",
    val colorHex: Long = 0xFF1D9BF0L,
    val iconKey: String = "folder",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

sealed interface FirestoreSyncResult<out T> {
    data object Loading : FirestoreSyncResult<Nothing>
    data class Success<T>(val data: T) : FirestoreSyncResult<T>
    data class Error(val message: String, val cause: Throwable) : FirestoreSyncResult<Nothing>
}

class FirestoreBookmarkSyncRepository(private val context: Context) {

    private val appContext = context.applicationContext
    private val safeIdRegex = Regex("^[a-zA-Z0-9_\\-]+$")

    private fun firestoreOrNull(): FirebaseFirestore? {
        return try {
            if (FirebaseApp.getApps(appContext).isEmpty()) {
                FirebaseApp.initializeApp(appContext)
            }
            if (FirebaseApp.getApps(appContext).isEmpty()) return null
            val dbId = appContext.getString(R.string.firestore_database_id)
            if (dbId.isBlank()) null else FirebaseFirestore.getInstance(dbId)
        } catch (_: Exception) {
            null
        }
    }

    private fun authenticatedUidOrNull(): String? {
        return try {
            if (FirebaseApp.getApps(appContext).isEmpty()) null else Firebase.auth.currentUser?.uid
        } catch (_: Exception) {
            null
        }
    }

    fun observeRemoteBookmarks(uid: String): Flow<FirestoreSyncResult<List<SyncedBookmarkDoc>>> {
        if (uid.isBlank() || authenticatedUidOrNull() != uid) {
            return flowOf(FirestoreSyncResult.Success(emptyList()))
        }
        val db = firestoreOrNull() ?: return flowOf(FirestoreSyncResult.Success(emptyList()))
        val path = "users/$uid/bookmarks"
        return callbackFlow {
            trySend(FirestoreSyncResult.Loading)
            val registration = db.collection(path)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        runCatching {
                            handleFirestoreError(error, FirestoreOperationType.LIST, path)
                        }.onFailure { wrapped ->
                            trySend(
                                FirestoreSyncResult.Error(
                                    error.localizedMessage ?: "Failed to sync cloud bookmarks",
                                    wrapped
                                )
                            )
                        }
                        return@addSnapshotListener
                    }
                    val docs = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject(
                            SyncedBookmarkDoc::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } ?: emptyList()
                    trySend(FirestoreSyncResult.Success(docs))
                }
            awaitClose { registration.remove() }
        }
    }

    fun observeRemoteFolders(uid: String): Flow<FirestoreSyncResult<List<SyncedBookmarkFolderDoc>>> {
        if (uid.isBlank() || authenticatedUidOrNull() != uid) {
            return flowOf(FirestoreSyncResult.Success(emptyList()))
        }
        val db = firestoreOrNull() ?: return flowOf(FirestoreSyncResult.Success(emptyList()))
        val path = "users/$uid/bookmark_folders"
        return callbackFlow {
            trySend(FirestoreSyncResult.Loading)
            val registration = db.collection(path)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        runCatching {
                            handleFirestoreError(error, FirestoreOperationType.LIST, path)
                        }.onFailure { wrapped ->
                            trySend(
                                FirestoreSyncResult.Error(
                                    error.localizedMessage ?: "Failed to sync bookmark folders",
                                    wrapped
                                )
                            )
                        }
                        return@addSnapshotListener
                    }
                    val docs = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject(
                            SyncedBookmarkFolderDoc::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } ?: emptyList()
                    trySend(FirestoreSyncResult.Success(docs))
                }
            awaitClose { registration.remove() }
        }
    }

    suspend fun upsertBookmark(
        uid: String,
        bookmarkId: String,
        title: String,
        content: String,
        authorName: String,
        authorHandle: String,
        itemType: String,
        mediaUrl: String?,
        linkUrl: String?,
        folderId: String?
    ): Result<Unit> {
        if (uid.isBlank() || authenticatedUidOrNull() != uid) {
            return Result.success(Unit)
        }
        val db = firestoreOrNull() ?: return Result.success(Unit)
        val sanitizedId = sanitizeId(bookmarkId)
        val path = "users/$uid/bookmarks/$sanitizedId"
        val validItemType = itemType.uppercase().takeIf { it in setOf("POST", "VIDEO", "IMAGE", "LINK") } ?: "POST"
        val sanitizedFolderId = folderId?.takeIf { it.isNotBlank() && safeIdRegex.matches(it) && it.length <= 128 }
        val nowTs = Timestamp.now()

        return try {
            val docRef = db.collection("users").document(uid).collection("bookmarks").document(sanitizedId)
            val existing = docRef.get().await()
            if (existing.exists()) {
                val updates = mutableMapOf<String, Any?>(
                    "title" to title.take(300),
                    "content" to content.ifBlank { title.ifBlank { "Saved bookmark" } }.take(5000),
                    "itemType" to validItemType,
                    "mediaUrl" to mediaUrl?.take(1000),
                    "linkUrl" to linkUrl?.take(1000),
                    "folderId" to sanitizedFolderId,
                    "updatedAt" to nowTs
                ).filterValues { it != null }
                docRef.set(updates, SetOptions.merge()).await()
            } else {
                val createMap = mutableMapOf<String, Any?>(
                    "userId" to uid,
                    "title" to title.take(300),
                    "content" to content.ifBlank { title.ifBlank { "Saved bookmark" } }.take(5000),
                    "authorName" to authorName.ifBlank { "XTRA User" }.take(120),
                    "authorHandle" to authorHandle.ifBlank { "@user" }.take(80),
                    "itemType" to validItemType,
                    "mediaUrl" to mediaUrl?.take(1000),
                    "linkUrl" to linkUrl?.take(1000),
                    "folderId" to sanitizedFolderId,
                    "createdAt" to nowTs,
                    "updatedAt" to nowTs
                ).filterValues { it != null }
                docRef.set(createMap).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            runCatching { handleFirestoreError(e, FirestoreOperationType.WRITE, path) }
            Result.failure(e)
        }
    }

    suspend fun deleteBookmark(uid: String, bookmarkId: String): Result<Unit> {
        if (uid.isBlank() || authenticatedUidOrNull() != uid) {
            return Result.success(Unit)
        }
        val db = firestoreOrNull() ?: return Result.success(Unit)
        val sanitizedId = sanitizeId(bookmarkId)
        val path = "users/$uid/bookmarks/$sanitizedId"
        return try {
            db.collection("users").document(uid).collection("bookmarks").document(sanitizedId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            runCatching { handleFirestoreError(e, FirestoreOperationType.DELETE, path) }
            Result.failure(e)
        }
    }

    suspend fun upsertFolder(
        uid: String,
        folderId: String,
        name: String,
        description: String,
        colorHex: Long,
        iconKey: String
    ): Result<Unit> {
        if (uid.isBlank() || authenticatedUidOrNull() != uid) {
            return Result.success(Unit)
        }
        val db = firestoreOrNull() ?: return Result.success(Unit)
        val sanitizedId = sanitizeId(folderId)
        val path = "users/$uid/bookmark_folders/$sanitizedId"
        val nowTs = Timestamp.now()

        return try {
            val docRef = db.collection("users").document(uid).collection("bookmark_folders").document(sanitizedId)
            val existing = docRef.get().await()
            if (existing.exists()) {
                val updates = mapOf(
                    "name" to name.ifBlank { "Collection" }.take(60),
                    "description" to description.take(200),
                    "colorHex" to colorHex,
                    "iconKey" to iconKey.ifBlank { "folder" }.take(40),
                    "updatedAt" to nowTs
                )
                docRef.set(updates, SetOptions.merge()).await()
            } else {
                val createMap = mapOf(
                    "userId" to uid,
                    "name" to name.ifBlank { "Collection" }.take(60),
                    "description" to description.take(200),
                    "colorHex" to colorHex,
                    "iconKey" to iconKey.ifBlank { "folder" }.take(40),
                    "createdAt" to nowTs,
                    "updatedAt" to nowTs
                )
                docRef.set(createMap).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            runCatching { handleFirestoreError(e, FirestoreOperationType.WRITE, path) }
            Result.failure(e)
        }
    }

    suspend fun deleteFolder(uid: String, folderId: String): Result<Unit> {
        if (uid.isBlank() || authenticatedUidOrNull() != uid) {
            return Result.success(Unit)
        }
        val db = firestoreOrNull() ?: return Result.success(Unit)
        val sanitizedId = sanitizeId(folderId)
        val path = "users/$uid/bookmark_folders/$sanitizedId"
        return try {
            db.collection("users").document(uid).collection("bookmark_folders").document(sanitizedId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            runCatching { handleFirestoreError(e, FirestoreOperationType.DELETE, path) }
            Result.failure(e)
        }
    }

    private fun sanitizeId(raw: String): String {
        val cleaned = raw.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(128)
        return cleaned.ifBlank { "item_${System.currentTimeMillis()}" }
    }
}
