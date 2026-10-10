package com.github.phoswald.whereyoubeen.domain

import android.content.Context
import kotlinx.coroutines.flow.StateFlow

data class User(
    val displayName: String?,
    val email: String,
    /** Long-lived access token (JWT) issued by the backend in exchange for the Google ID token. */
    val accessToken: String,
)

/** The app-wide sign-in session, shared by all ViewModels. */
interface AuthRepository {

    /** The signed-in user, or null while signed out. */
    val currentUser: StateFlow<User?>

    /**
     * Shows the "Sign in with Google" flow and exchanges the ID token for an access token.
     * Returns null if the user cancelled, throws if the exchange failed.
     */
    suspend fun signIn(context: Context): User?

    suspend fun signOut()
}
