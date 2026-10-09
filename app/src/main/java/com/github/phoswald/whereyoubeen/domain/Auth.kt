package com.github.phoswald.whereyoubeen.domain

import android.content.Context
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

/** Renew the ID token this long before it expires, so an upload never races its expiry. */
private val REFRESH_MARGIN = 5.minutes.toJavaDuration()

data class User(
    val displayName: String?,
    val email: String,
    /** Google ID token (JWT, audience = Web client ID), to be verified by a backend. */
    val idToken: String,
    /** Expiry of [idToken] ("exp" claim), typically one hour after sign-in. */
    val expiresAt: Instant,
) {
    fun expiresSoon(now: Instant): Boolean = now + REFRESH_MARGIN >= expiresAt
}

/** The app-wide sign-in session, shared by all ViewModels. */
interface AuthRepository {

    /** The signed-in user, or null while signed out. */
    val currentUser: StateFlow<User?>

    /**
     * Signs in without UI if a previously authorized account exists, returns null otherwise.
     * [context] must be an Activity, since the credential provider may show UI.
     */
    suspend fun signInSilently(context: Context): User?

    /**
     * Like [signInSilently], but without an Activity, to renew the ID token in the background.
     * Returns null if that is not possible; the user then has to open the app.
     */
    suspend fun refreshSilently(): User?

    /** Shows the "Sign in with Google" flow, returns null if the user cancelled. */
    suspend fun signInInteractive(context: Context): User?

    suspend fun signOut()
}
