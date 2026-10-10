package com.github.phoswald.whereyoubeen.domain

import kotlinx.coroutines.CancellationException
import java.time.Instant

sealed interface SyncState {
    data object None : SyncState
    /** [time] is the time of the uploaded location. */
    data class Synced(val time: Instant) : SyncState
    data class Failed(val time: Instant, val reason: String) : SyncState
}

/** Posts locations to the backend. */
interface LocationUploader {

    /** Returns the HTTP status code; throws [java.io.IOException] if no response was received. */
    suspend fun upload(location: GeoLocation, accessToken: String): Int
}

/** Uploads a location on behalf of the signed-in user. */
class SyncLocationUseCase(
    private val authRepository: AuthRepository,
    private val uploader: LocationUploader,
) {

    /** Returns null if nobody is signed in (nothing is uploaded then). */
    suspend operator fun invoke(location: GeoLocation): SyncState? {
        val user = authRepository.currentUser.value ?: return null
        return try {
            val status = uploader.upload(location, user.accessToken)
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
}
