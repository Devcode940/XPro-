package com.example.data.remote

import android.content.Context
import androidx.annotation.Keep
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.IgnoreExtraProperties
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
    @com.google.firebase.firestore.ServerTimestamp val createdAt: Timestamp? = null,
    @com.google.firebase.firestore.ServerTimestamp val updatedAt: Timestamp? = null
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
    @com.google.firebase.firestore.ServerTimestamp val createdAt: Timestamp? = null,
    @com.google.firebase.firestore.ServerTimestamp val updatedAt: Timestamp? = null
)

sealed interface FirestoreSyncResult<out T> {
    data object Loading : FirestoreSyncResult<Nothing>

    /**
     * A result from the server (or a placeholder emitted because sync is unavailable).
     *
     * [isAuthoritative] distinguishes a real, server-confirmed snapshot from (a) a snapshot
     * served out of the local Firestore cache and (b) the empty placeholder returned when
     * Firebase is missing or the caller is not authenticated as [SyncedBookmarkDoc.userId].
     * Only authoritative snapshots may drive a destructive reconcile — treating a cache or
     * placeholder result as "the server says zero documents" would delete every synced row.
     */
    data class Success<T>(
        val data: T,
        val isAuthoritative: Boolean = false
    ) : FirestoreSyncResult<T>

    data class Error(val message: String, val cause: Throwable) : FirestoreSyncResult<Nothing>
}

/** Outcome of a one-shot write; lets the UI distinguish "cloud synced" from "local only". */
sealed interface SyncWriteResult {
    /** Written to Firestore. */
    data object Synced : SyncWriteResult

    /** Firebase is unavailable or the caller isn't authenticated as this uid; nothing was written remotely. */
    data object GuestMode : SyncWriteResult

    /** Write was attempted and rejected/failed. */
    data class Failed(val message: String, val cause: Throwable?) : SyncWriteResult
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
                        logFirestoreError(error, FirestoreOperationType.LIST, path)
                        trySend(
                            FirestoreSyncResult.Error(
                                sanitizedFirestoreMessage(error, "Failed to sync cloud bookmarks"),
                                error
                            )
                        )
                        return@addSnapshotListener
                    }
                    val docs = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject(
                            SyncedBookmarkDoc::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } ?: emptyList()
                    // Cache-sourced snapshots are shown as-is but must never trigger a reconcile.
                    trySend(
                        FirestoreSyncResult.Success(
                            data = docs,
                            isAuthoritative = snapshot != null && !snapshot.metadata.isFromCache
                        )
                    )
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
                        logFirestoreError(error, FirestoreOperationType.LIST, path)
                        trySend(
                            FirestoreSyncResult.Error(
                                sanitizedFirestoreMessage(error, "Failed to sync bookmark folders"),
                                error
                            )
                        )
                        return@addSnapshotListener
                    }
                    val docs = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject(
                            SyncedBookmarkFolderDoc::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } ?: emptyList()
                    trySend(
                        FirestoreSyncResult.Success(
                            data = docs,
                            isAuthoritative = snapshot != null && !snapshot.metadata.isFromCache
                        )
                    )
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
    ): SyncWriteResult {
        if (uid.isBlank() || authenticatedUidOrNull() != uid) {
            return SyncWriteResult.GuestMode
        }
        val db = firestoreOrNull() ?: return SyncWriteResult.GuestMode
        val sanitizedId = sanitizeId(bookmarkId)
        val path = "users/$uid/bookmarks/$sanitizedId"
        val validItemType = itemType.uppercase().takeIf { it in setOf("POST", "VIDEO", "IMAGE", "LINK") } ?: "POST"
        val sanitizedFolderId = folderId?.takeIf { it.isNotBlank() && safeIdRegex.matches(it) && it.length <= 128 }
        val cleanContent = content.ifBlank { title.ifBlank { "Saved bookmark" } }.take(5000)

        return try {
            val docRef = db.collection("users").document(uid).collection("bookmarks").document(sanitizedId)
            val existing = docRef.get().await()
            if (existing.exists()) {
                val updates = mutableMapOf<String, Any>(
                    "title" to title.take(300),
                    "content" to cleanContent,
                    "itemType" to validItemType,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                // Null means "cleared locally": delete the remote key so it can't linger.
                updates["mediaUrl"] = mediaUrl?.take(1000) ?: FieldValue.delete()
                updates["linkUrl"] = linkUrl?.take(1000) ?: FieldValue.delete()
                updates["folderId"] = sanitizedFolderId ?: FieldValue.delete()
                docRef.update(updates).await()
            } else {
                val createMap = mutableMapOf<String, Any>(
                    "userId" to uid,
                    "title" to title.take(300),
                    "content" to cleanContent,
                    "authorName" to authorName.ifBlank { "XTRA User" }.take(120),
                    "authorHandle" to authorHandle.ifBlank { "@user" }.take(80),
                    "itemType" to validItemType,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                mediaUrl?.take(1000)?.let { createMap["mediaUrl"] = it }
                linkUrl?.take(1000)?.let { createMap["linkUrl"] = it }
                sanitizedFolderId?.let { createMap["folderId"] = it }
                docRef.set(createMap).await()
            }
            SyncWriteResult.Synced
        } catch (e: Exception) {
            logFirestoreError(e, FirestoreOperationType.WRITE, path)
            SyncWriteResult.Failed(sanitizedFirestoreMessage(e, "Bookmark sync failed"), e)
        }
    }

    suspend fun deleteBookmark(uid: String, bookmarkId: String): SyncWriteResult {
        if (uid.isBlank() || authenticatedUidOrNull() != uid) {
            return SyncWriteResult.GuestMode
        }
        val db = firestoreOrNull() ?: return SyncWriteResult.GuestMode
        val sanitizedId = sanitizeId(bookmarkId)
        val path = "users/$uid/bookmarks/$sanitizedId"
        return try {
            db.collection("users").document(uid).collection("bookmarks").document(sanitizedId)
                .delete()
                .await()
            SyncWriteResult.Synced
        } catch (e: Exception) {
            logFirestoreError(e, FirestoreOperationType.DELETE, path)
            SyncWriteResult.Failed(sanitizedFirestoreMessage(e, "Bookmark delete sync failed"), e)
        }
    }

    suspend fun upsertFolder(
        uid: String,
        folderId: String,
        name: String,
        description: String,
        colorHex: Long,
        iconKey: String
    ): SyncWriteResult {
        if (uid.isBlank() || authenticatedUidOrNull() != uid) {
            return SyncWriteResult.GuestMode
        }
        val db = firestoreOrNull() ?: return SyncWriteResult.GuestMode
        val sanitizedId = sanitizeId(folderId)
        val path = "users/$uid/bookmark_folders/$sanitizedId"

        return try {
            val docRef = db.collection("users").document(uid).collection("bookmark_folders").document(sanitizedId)
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "name" to name.ifBlank { "Collection" }.take(60),
                        "description" to description.take(200),
                        "colorHex" to colorHex,
                        "iconKey" to iconKey.ifBlank { "folder" }.take(40),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "name" to name.ifBlank { "Collection" }.take(60),
                        "description" to description.take(200),
                        "colorHex" to colorHex,
                        "iconKey" to iconKey.ifBlank { "folder" }.take(40),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            SyncWriteResult.Synced
        } catch (e: Exception) {
            logFirestoreError(e, FirestoreOperationType.WRITE, path)
            SyncWriteResult.Failed(sanitizedFirestoreMessage(e, "Folder sync failed"), e)
        }
    }

    suspend fun deleteFolder(uid: String, folderId: String): SyncWriteResult {
        if (uid.isBlank() || authenticatedUidOrNull() != uid) {
            return SyncWriteResult.GuestMode
        }
        val db = firestoreOrNull() ?: return SyncWriteResult.GuestMode
        val sanitizedId = sanitizeId(folderId)
        val path = "users/$uid/bookmark_folders/$sanitizedId"
        return try {
            db.collection("users").document(uid).collection("bookmark_folders").document(sanitizedId)
                .delete()
                .await()
            SyncWriteResult.Synced
        } catch (e: Exception) {
            logFirestoreError(e, FirestoreOperationType.DELETE, path)
            SyncWriteResult.Failed(sanitizedFirestoreMessage(e, "Folder delete sync failed"), e)
        }
    }

    private fun sanitizeId(raw: String): String {
        val cleaned = raw.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(128)
        return cleaned.ifBlank { "item_${System.currentTimeMillis()}" }
    }
}
