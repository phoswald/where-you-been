package com.github.phoswald.whereyoubeen.domain

import kotlinx.coroutines.CancellationException
import java.time.Instant

private const val HTTP_UNAUTHORIZED = 401

sealed interface SyncState {
    data object None : SyncState
    /** [time] is the time of the uploaded location. */
    data class Synced(val time: Instant) : SyncState
    data class Failed(val time: Instant, val reason: String) : SyncState
}

/** Posts locations to the backend. */
interface LocationUploader {

    /** Returns the HTTP status code; throws [java.io.IOException] if no response was received. */
    suspend fun upload(location: GeoLocation, idToken: String): Int
}

/**
 * Uploads a location on behalf of the signed-in user. Renews the ID token silently when it is
 * about to expire or the backend rejects it, since uploads also run while the app is not visible.
 */
class SyncLocationUseCase(
    private val authRepository: AuthRepository,
    private val uploader: LocationUploader,
    private val clock: () -> Instant = Instant::now,
) {

    /** Returns null if nobody is signed in (nothing is uploaded then). */
    suspend operator fun invoke(location: GeoLocation): SyncState? {
        var user = authRepository.currentUser.value
        if (user == null) {
            return null
        }
        return try {
            if (user.expiresSoon(clock())) {
                user = authRepository.refreshSilently() ?: return reSignInFailed(location)
            }
            var status = uploader.upload(location, user.idToken)
            if (status == HTTP_UNAUTHORIZED) {
                user = authRepository.refreshSilently() ?: return reSignInFailed(location)
                status = uploader.upload(location, user.idToken)
            }
            if (status in 200..299) {
                SyncState.Synced(location.time)
            } else {
                SyncState.Failed(location.time, "HTTP $status")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            SyncState.Failed(location.time, e.message ?: e.toString())
        }
    }

    private fun reSignInFailed(location: GeoLocation) =
        SyncState.Failed(location.time, "token expired, re-sign-in failed")
}
