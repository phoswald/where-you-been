package com.github.phoswald.whereyoubeen.ui

import com.github.phoswald.whereyoubeen.FakeAuthRepository
import com.github.phoswald.whereyoubeen.FakeLocationSource
import com.github.phoswald.whereyoubeen.FakeLocationUploader
import com.github.phoswald.whereyoubeen.domain.LocationStatus
import com.github.phoswald.whereyoubeen.domain.SyncLocationUseCase
import com.github.phoswald.whereyoubeen.domain.SyncState
import com.github.phoswald.whereyoubeen.testLocation
import com.github.phoswald.whereyoubeen.testUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LocationViewModelTest {

    private val source = FakeLocationSource()
    private val auth = FakeAuthRepository()
    private val uploader = FakeLocationUploader()
    /** Runs coroutines eagerly, so each assertion sees the effect of the preceding line. */
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        // viewModelScope runs on Dispatchers.Main, which does not exist on the JVM
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun newViewModel() = LocationViewModel(source, SyncLocationUseCase(auth, uploader))

    @Test
    fun location_mirrorsSource() = runTest(dispatcher) {
        val viewModel = newViewModel()
        backgroundScope.launch { viewModel.location.collect {} } // simulates the visible UI

        source.status.value = LocationStatus.Disabled
        assertEquals(LocationStatus.Disabled, viewModel.location.value)

        source.status.value = LocationStatus.Available(testLocation)
        assertEquals(LocationStatus.Available(testLocation), viewModel.location.value)
    }

    @Test
    fun sync_signedOut_uploadsNothing() = runTest(dispatcher) {
        val viewModel = newViewModel()
        backgroundScope.launch { viewModel.sync.collect {} }

        source.status.value = LocationStatus.Available(testLocation)

        assertEquals(SyncState.None, viewModel.sync.value)
        assertTrue(uploader.uploads.isEmpty())
    }

    @Test
    fun sync_signedIn_uploadsAvailableLocationsOnly() = runTest(dispatcher) {
        auth.currentUser.value = testUser
        val viewModel = newViewModel()
        backgroundScope.launch { viewModel.sync.collect {} }

        source.status.value = LocationStatus.Disabled
        assertTrue(uploader.uploads.isEmpty())

        source.status.value = LocationStatus.Available(testLocation)
        assertEquals(SyncState.Synced(testLocation.time), viewModel.sync.value)
        assertEquals(listOf(testLocation to testUser.idToken), uploader.uploads)
    }
}
