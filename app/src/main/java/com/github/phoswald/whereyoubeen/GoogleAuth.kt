package com.github.phoswald.whereyoubeen

import android.app.Activity
import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CredentialOption
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

data class User(
    val displayName: String?,
    val email: String,
    /** Google ID token (JWT, audience = Web client ID), to be verified by a backend. */
    val idToken: String,
)

class GoogleAuth(context: Context) {

    private val credentialManager = CredentialManager.create(context)
    private val serverClientId = context.getString(R.string.server_client_id)

    /** Signs in without UI if a previously authorized account exists, returns null otherwise. */
    suspend fun signInSilently(activity: Activity): User? = signIn(
        activity,
        GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setAutoSelectEnabled(true)
            .setServerClientId(serverClientId)
            .build()
    )

    /** Shows the "Sign in with Google" flow, returns null if the user cancelled. */
    suspend fun signInInteractive(activity: Activity): User? = signIn(
        activity,
        GetSignInWithGoogleOption.Builder(serverClientId).build()
    )

    suspend fun signOut() {
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
    }

    private suspend fun signIn(activity: Activity, option: CredentialOption): User? {
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val credential = try {
            credentialManager.getCredential(activity, request).credential
        } catch (_: NoCredentialException) {
            return null
        } catch (_: GetCredentialCancellationException) {
            return null
        }
        check(credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            "Unexpected credential type: ${credential.type}"
        }
        val google = GoogleIdTokenCredential.createFrom(credential.data)
        return User(displayName = google.displayName, email = google.id, idToken = google.idToken)
    }
}
