package com.example.data.remote

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import org.json.JSONArray
import org.json.JSONObject

enum class FirestoreOperationType {
    CREATE, UPDATE, DELETE, LIST, GET, WRITE
}

fun handleFirestoreError(
    error: Throwable,
    operationType: FirestoreOperationType,
    path: String
): Nothing {
    val auth: FirebaseAuth? = runCatching {
        Firebase.auth
    }.getOrNull()
    val currentUser = auth?.currentUser
    val code = (error as? FirebaseFirestoreException)?.code?.name ?: "UNKNOWN"

    val providerInfo = JSONArray().apply {
        currentUser?.providerData?.forEach { info ->
            put(JSONObject().apply {
                put("providerId", info.providerId)
                put("displayName", info.displayName ?: JSONObject.NULL)
                put("email", info.email ?: JSONObject.NULL)
            })
        }
    }

    val payload = JSONObject().apply {
        put("error", error.message ?: "Unknown Firestore error")
        put("code", code)
        put("operationType", operationType.name.lowercase())
        put("path", path)
        put("authInfo", JSONObject().apply {
            put("userId", currentUser?.uid ?: JSONObject.NULL)
            put("email", currentUser?.email ?: JSONObject.NULL)
            put("providerInfo", providerInfo)
        })
    }

    Log.e("FirestoreError", payload.toString())
    throw IllegalStateException("FirestoreError: $payload", error)
}
