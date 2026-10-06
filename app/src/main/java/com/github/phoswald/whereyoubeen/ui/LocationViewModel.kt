package com.github.phoswald.whereyoubeen.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.phoswald.whereyoubeen.domain.LocationSource
import com.github.phoswald.whereyoubeen.domain.LocationStatus
import com.github.phoswald.whereyoubeen.domain.SyncLocationUseCase
import com.github.phoswald.whereyoubeen.domain.SyncState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

class LocationViewModel(
    locationSource: LocationSource,
    private val syncLocation: SyncLocationUseCase,
) : ViewModel() {

    /** Polls the location every [INTERVAL], only while collected (i.e. while the UI is visible). */
    val location: StateFlow<LocationStatus> = locationSource.observe(INTERVAL)
        .stateIn(viewModelScope, whileVisible, LocationStatus.Waiting(Instant.now()))

    /**
     * Result of uploading the latest available location. Runs in its own collector of [location],
     * so a slow server never delays polling; locations arriving during an upload are skipped.
     */
    val sync: StateFlow<SyncState> = location
        .filterIsInstance<LocationStatus.Available>()
        .map { syncLocation(it.location) }
        .filterNotNull() // null: signed out, nothing uploaded
        .stateIn(viewModelScope, whileVisible, SyncState.None)

    private companion object {
        val INTERVAL = 10.seconds
        /** Keeps polling across configuration changes (e.g. rotation). */
        val whileVisible = SharingStarted.WhileSubscribed(5_000)
    }
}
