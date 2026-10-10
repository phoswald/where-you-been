package com.github.phoswald.whereyoubeen.domain

import com.github.phoswald.whereyoubeen.FakeAuthRepository
import com.github.phoswald.whereyoubeen.FakeLocationSource
import com.github.phoswald.whereyoubeen.FakeLocationUploader
import com.github.phoswald.whereyoubeen.testLocation
import com.github.phoswald.whereyoubeen.testUser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LocationTrackerTest {

    private val source = FakeLocationSource()
    private val auth = FakeAuthRepository()
    private val uploader = FakeLocationUploader()
    /** Runs coroutines eagerly, so each assertion sees the effect of the preceding line. */
    private val dispatcher = UnconfinedTestDispatcher()
    private val tracker = LocationTracker(source, SyncLocationUseCase(auth, uploader))

    @Test
    fun location_offUntilRunning_thenMirrorsSource() = runTest(dispatcher) {
        assertEquals(LocationStatus.Off, tracker.location.value)

        backgroundScope.launch { tracker.run() }

        source.status.value = LocationStatus.Disabled
        assertEquals(LocationStatus.Disabled, tracker.location.value)

        source.status.value = LocationStatus.Available(testLocation)
        assertEquals(LocationStatus.Available(testLocation), tracker.location.value)
    }

    @Test
    fun sync_signedOut_uploadsNothing() = runTest(dispatcher) {
        backgroundScope.launch { tracker.run() }

        source.status.value = LocationStatus.Available(testLocation)

        assertEquals(SyncState.None, tracker.sync.value)
        assertTrue(uploader.uploads.isEmpty())
    }

    @Test
    fun sync_signedIn_uploadsAvailableLocationsOnly() = runTest(dispatcher) {
        auth.currentUser.value = testUser
        backgroundScope.launch { tracker.run() }

        source.status.value = LocationStatus.Disabled
        assertTrue(uploader.uploads.isEmpty())

        source.status.value = LocationStatus.Available(testLocation)
        assertEquals(SyncState.Synced(testLocation.time), tracker.sync.value)
        assertEquals(listOf(testLocation to testUser.accessToken), uploader.uploads)
    }

    @Test
    fun disable_stopsTracking() = runTest(dispatcher) {
        auth.currentUser.value = testUser
        val run = backgroundScope.launch { tracker.run() }

        tracker.setEnabled(false)

        assertTrue(run.isCompleted)
        assertEquals(LocationStatus.Off, tracker.location.value)
        source.status.value = LocationStatus.Available(testLocation)
        assertTrue(uploader.uploads.isEmpty())
    }

    @Test
    fun runWhileDisabled_returnsImmediately() = runTest(dispatcher) {
        tracker.setEnabled(false)

        tracker.run()

        assertEquals(LocationStatus.Off, tracker.location.value)
    }
}
