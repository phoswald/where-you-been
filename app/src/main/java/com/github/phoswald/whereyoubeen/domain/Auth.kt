package com.github.phoswald.whereyoubeen.domain

import android.content.Context
import kotlinx.coroutines.flow.StateFlow

data class User(
    val displayName: String?,
    val email: String,
    /** Google ID token (JWT, audience = Web client ID), to be verified by a backend. */
    val idToken: String,
)

/** The app-wide sign-in session, shared by all ViewModels. */
interface AuthRepository {

    /** The signed-in user, or null while signed out. */
    val currentUser: StateFlow<User?>

    /**
     * Signs in without UI if a previously authorized account exists, returns null otherwise.
     * [context] must be an Activity, since the credential provider may show UI.
     */
    suspend fun signInSilently(context: Context): User?

    /** Shows the "Sign in with Google" flow, returns null if the user cancelled. */
    suspend fun signInInteractive(context: Context): User?

    suspend fun signOut()
}
