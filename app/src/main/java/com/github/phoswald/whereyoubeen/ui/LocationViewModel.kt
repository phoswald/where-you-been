package com.github.phoswald.whereyoubeen.ui

import androidx.lifecycle.ViewModel
import com.github.phoswald.whereyoubeen.domain.LocationStatus
import com.github.phoswald.whereyoubeen.domain.LocationTracker
import com.github.phoswald.whereyoubeen.domain.SyncState
import kotlinx.coroutines.flow.StateFlow

/** The UI's view of [LocationTracker], which keeps running while the UI is not visible. */
class LocationViewModel(private val tracker: LocationTracker) : ViewModel() {

    val trackingEnabled: StateFlow<Boolean> = tracker.enabled
    val location: StateFlow<LocationStatus> = tracker.location
    val sync: StateFlow<SyncState> = tracker.sync

    fun setTrackingEnabled(enabled: Boolean) {
        tracker.setEnabled(enabled)
    }
}
