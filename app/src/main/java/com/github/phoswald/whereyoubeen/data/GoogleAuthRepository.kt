package com.github.phoswald.whereyoubeen.data

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.github.phoswald.whereyoubeen.R
import com.github.phoswald.whereyoubeen.domain.AuthRepository
import com.github.phoswald.whereyoubeen.domain.User
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Sign-in with Google via Credential Manager, followed by a token exchange with the backend. */
class GoogleAuthRepository(
    context: Context,
    private val tokenExchanger: TokenExchanger,
) : AuthRepository {

    private val credentialManager = CredentialManager.create(context)
    private val serverClientId = context.getString(R.string.server_client_id)

    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    override suspend fun signIn(context: Context): User? {
        val option = GetSignInWithGoogleOption.Builder(serverClientId).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val credential = try {
            credentialManager.getCredential(context, request).credential
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
        val user = User(
            displayName = google.displayName,
            email = google.id,
            accessToken = tokenExchanger.exchange(google.idToken),
        )
        _currentUser.value = user
        return user
    }

    override suspend fun signOut() {
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
        _currentUser.value = null
    }
}
