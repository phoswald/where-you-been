package com.github.phoswald.whereyoubeen.domain

import com.github.phoswald.whereyoubeen.FakeAuthRepository
import com.github.phoswald.whereyoubeen.FakeLocationUploader
import com.github.phoswald.whereyoubeen.testLocation
import com.github.phoswald.whereyoubeen.testUser
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class SyncLocationUseCaseTest {

    private val uploader = FakeLocationUploader()

    @Test
    fun signedOut_uploadsNothing() = runTest {
        val syncLocation = SyncLocationUseCase(FakeAuthRepository(user = null), uploader)

        assertNull(syncLocation(testLocation))
        assertTrue(uploader.uploads.isEmpty())
    }

    @Test
    fun http200_synced() = runTest {
        val syncLocation = SyncLocationUseCase(FakeAuthRepository(testUser), uploader)

        assertEquals(SyncState.Synced(testLocation.time), syncLocation(testLocation))
        assertEquals(listOf(testLocation to testUser.accessToken), uploader.uploads)
    }

    @Test
    fun http500_failed() = runTest {
        uploader.respond = { 500 }
        val syncLocation = SyncLocationUseCase(FakeAuthRepository(testUser), uploader)

        assertEquals(SyncState.Failed(testLocation.time, "HTTP 500"), syncLocation(testLocation))
    }

    @Test
    fun http401_failedWithoutRetry() = runTest {
        uploader.respond = { 401 }
        val syncLocation = SyncLocationUseCase(FakeAuthRepository(testUser), uploader)

        assertEquals(SyncState.Failed(testLocation.time, "HTTP 401"), syncLocation(testLocation))
        assertEquals(1, uploader.uploads.size)
    }

    @Test
    fun ioException_failed() = runTest {
        uploader.respond = { throw IOException("no network") }
        val syncLocation = SyncLocationUseCase(FakeAuthRepository(testUser), uploader)

        assertEquals(SyncState.Failed(testLocation.time, "no network"), syncLocation(testLocation))
    }
}
