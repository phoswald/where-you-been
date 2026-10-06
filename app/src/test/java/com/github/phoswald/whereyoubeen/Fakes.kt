package com.github.phoswald.whereyoubeen

import android.content.Context
import com.github.phoswald.whereyoubeen.domain.AuthRepository
import com.github.phoswald.whereyoubeen.domain.GeoLocation
import com.github.phoswald.whereyoubeen.domain.LocationSource
import com.github.phoswald.whereyoubeen.domain.LocationStatus
import com.github.phoswald.whereyoubeen.domain.LocationUploader
import com.github.phoswald.whereyoubeen.domain.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.Instant
import kotlin.time.Duration

val testUser = User("Jane Doe", "jane@example.com", "token-123")
val testLocation = GeoLocation(47.376887, 8.541694, Instant.parse("2026-10-07T12:00:00Z"))

class FakeAuthRepository(user: User? = null) : AuthRepository {
    override val currentUser = MutableStateFlow(user)
    override suspend fun signInSilently(context: Context): User? = currentUser.value
    override suspend fun signInInteractive(context: Context): User? = currentUser.value
    override suspend fun signOut() {
        currentUser.value = null
    }
}

/** Emits whatever the test puts into [status]. */
class FakeLocationSource : LocationSource {
    val status = MutableStateFlow<LocationStatus>(LocationStatus.Waiting(Instant.EPOCH))
    override fun observe(interval: Duration): Flow<LocationStatus> = status
}

/** Records uploads; [respond] decides the HTTP status (or throws). */
class FakeLocationUploader(var respond: () -> Int = { 200 }) : LocationUploader {
    val uploads = mutableListOf<Pair<GeoLocation, String>>()
    override suspend fun upload(location: GeoLocation, idToken: String): Int {
        uploads += location to idToken
        return respond()
    }
}
