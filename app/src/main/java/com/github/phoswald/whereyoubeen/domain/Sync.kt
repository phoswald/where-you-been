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

    /**
     * Queues [location] and, given an [accessToken], uploads all queued locations in order.
     * Returns the HTTP status of the last request, or null if nothing was sent (no token);
     * throws [java.io.IOException] if no response was received.
     */
    suspend fun upload(location: GeoLocation, accessToken: String?): Int?
}

/** Uploads a location on behalf of the signed-in user. */
class SyncLocationUseCase(
    private val authRepository: AuthRepository,
    private val uploader: LocationUploader,
) {

    /** Returns null if nobody is signed in (the location is only queued then). */
    suspend operator fun invoke(location: GeoLocation): SyncState? {
        val accessToken = authRepository.currentUser.value?.accessToken
        return try {
            val status = uploader.upload(location, accessToken) ?: return null
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
