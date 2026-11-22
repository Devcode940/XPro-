package com.example.data.remote

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestoreException

enum class FirestoreOperationType {
    CREATE, UPDATE, DELETE, LIST, GET, WRITE
}

/**
 * Logs a sanitized diagnostic line for a Firestore failure.
 *
 * Deliberately excludes anything derived from the authenticated user (uid, email,
 * provider info): Android logcat is readable by other apps on compromised devices and
 * logs have a way of ending up in bug reports. For diagnostics, the operation type,
 * target collection and Firestore error code are sufficient to triage.
 * The `users/{uid}` path segment is redacted here so callers can pass the raw path.
 */
fun logFirestoreError(
    error: Throwable,
    operationType: FirestoreOperationType,
    path: String
) {
    val code = (error as? FirebaseFirestoreException)?.code?.name ?: "UNKNOWN"
    val redactedPath = path.replace(Regex("users/[^/]+"), "users/***")
    Log.e("FirestoreError", "operation=${operationType.name.lowercase()} path=$redactedPath code=$code message=${error.message}")
}

/** Exception message that carries no PII, safe to surface in UI error states. */
fun sanitizedFirestoreMessage(error: Throwable, fallback: String): String {
    val code = (error as? FirebaseFirestoreException)?.code?.name
    return if (code != null) "$fallback ($code)" else fallback
}
