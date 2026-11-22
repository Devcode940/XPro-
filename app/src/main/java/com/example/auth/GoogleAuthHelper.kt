package com.example.auth

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private const val TAG = "GoogleAuthHelper"

/**
 * The google-services plugin generates `default_web_client_id` only when a
 * google-services.json is present at build time. Reading it via resources.getIdentifier keeps
 * the app buildable/runnable without the file: sign-in reports "not configured" instead of
 * failing compilation.
 */
private fun defaultWebClientId(context: Context): String? {
    val resId = context.resources.getIdentifier(
        "default_web_client_id",
        "string",
        context.packageName
    )
    return if (resId != 0) context.getString(resId).takeIf { it.isNotBlank() } else null
}

sealed interface AuthResultState {
    data class Authenticated(
        val uid: String,
        val email: String?,
        val displayName: String?
    ) : AuthResultState
    data class Error(val message: String) : AuthResultState
    data object Cancelled : AuthResultState
    data object NoSavedCredential : AuthResultState
}

data class SwitchableAccount(
    val id: String,
    val displayName: String,
    val handle: String,
    val bio: String,
    val isGuest: Boolean,
    val isFirebaseGoogle: Boolean = false
)

object GoogleAuthHelper {

    fun currentFirebaseUserOrNull(context: Context): FirebaseUser? {
        return try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            if (FirebaseApp.getApps(context).isEmpty()) null else Firebase.auth.currentUser
        } catch (_: Exception) {
            null
        }
    }

    suspend fun attemptAutoSignIn(activityContext: Context): AuthResultState {
        return try {
            if (FirebaseApp.getApps(activityContext).isEmpty()) {
                FirebaseApp.initializeApp(activityContext)
            }
            val clientId = defaultWebClientId(activityContext)
                ?: return AuthResultState.NoSavedCredential
            val option = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)
                .setServerClientId(clientId)
                .setAutoSelectEnabled(true)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(option)
                .build()

            val credentialManager = CredentialManager.create(activityContext)
            val result = credentialManager.getCredential(
                context = activityContext,
                request = request
            )
            exchangeGoogleCredential(result.credential)
        } catch (_: NoCredentialException) {
            AuthResultState.NoSavedCredential
        } catch (_: GetCredentialCancellationException) {
            AuthResultState.Cancelled
        } catch (e: Exception) {
            Log.d(TAG, "Auto sign-in skipped: ${e.message}")
            AuthResultState.NoSavedCredential
        }
    }

    suspend fun signInWithGoogle(activityContext: Context): AuthResultState {
        return try {
            if (FirebaseApp.getApps(activityContext).isEmpty()) {
                FirebaseApp.initializeApp(activityContext)
            }
            val clientId = defaultWebClientId(activityContext)
                ?: return AuthResultState.Error(
                    "Google Sign-In is not configured in this build (missing google-services.json). Use Guest mode."
                )
            val signInOption = GetSignInWithGoogleOption.Builder(clientId).build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInOption)
                .build()

            val credentialManager = CredentialManager.create(activityContext)
            val result = credentialManager.getCredential(
                context = activityContext,
                request = request
            )
            exchangeGoogleCredential(result.credential)
        } catch (_: GetCredentialCancellationException) {
            AuthResultState.Cancelled
        } catch (_: NoCredentialException) {
            AuthResultState.Error("No Google account found on this device. Add one in Android Settings or continue in Guest mode.")
        } catch (e: Exception) {
            Log.w(TAG, "Google sign-in failed", e)
            AuthResultState.Error(e.message ?: "Google Sign-In failed. Check network connection.")
        }
    }

    private suspend fun exchangeGoogleCredential(credential: androidx.credentials.Credential): AuthResultState {
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
            val authResult = Firebase.auth.signInWithCredential(firebaseCredential).await()
            val user = authResult.user
            return if (user != null) {
                AuthResultState.Authenticated(
                    uid = user.uid,
                    email = user.email,
                    displayName = user.displayName
                )
            } else {
                AuthResultState.Error("Firebase sign-in returned an empty user.")
            }
        }
        return AuthResultState.Error("Unsupported credential type returned by Credential Manager.")
    }

    suspend fun signOut(context: Context) {
        withContext(NonCancellable) {
            runCatching {
                if (FirebaseApp.getApps(context).isNotEmpty()) {
                    Firebase.auth.signOut()
                }
            }
            runCatching {
                CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
            }
        }
    }
}
