package com.github.phoswald.whereyoubeen.domain

import kotlinx.coroutines.flow.Flow
import java.time.Instant
import kotlin.time.Duration

data class GeoLocation(val latitude: Double, val longitude: Double, val time: Instant)

sealed interface LocationStatus {
    data object NoPermission : LocationStatus
    data object Disabled : LocationStatus
    data class Waiting(val time: Instant) : LocationStatus
    data class Available(val location: GeoLocation) : LocationStatus
}

interface LocationSource {

    /** Emits the current location status every [interval], as long as the flow is collected. */
    fun observe(interval: Duration): Flow<LocationStatus>
}
