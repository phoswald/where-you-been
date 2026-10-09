package com.github.phoswald.whereyoubeen.domain

import com.github.phoswald.whereyoubeen.FakeAuthRepository
import com.github.phoswald.whereyoubeen.FakeLocationUploader
import com.github.phoswald.whereyoubeen.renewedUser
import com.github.phoswald.whereyoubeen.testLocation
import com.github.phoswald.whereyoubeen.testUser
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.time.Instant

class SyncLocationUseCaseTest {

    private val uploader = FakeLocationUploader()
    /** Upload time; the test user's token is valid for another hour. */
    private val now = { testLocation.time }

    private fun newUseCase(auth: FakeAuthRepository, clock: () -> Instant = now) =
        SyncLocationUseCase(auth, uploader, clock)

    @Test
    fun signedOut_uploadsNothing() = runTest {
        val syncLocation = newUseCase(FakeAuthRepository(user = null))

        assertNull(syncLocation(testLocation))
        assertTrue(uploader.uploads.isEmpty())
    }

    @Test
    fun http200_synced() = runTest {
        val syncLocation = newUseCase(FakeAuthRepository(testUser))

        assertEquals(SyncState.Synced(testLocation.time), syncLocation(testLocation))
        assertEquals(listOf(testLocation to testUser.idToken), uploader.uploads)
    }

    @Test
    fun http500_failed() = runTest {
        uploader.respond = { 500 }
        val syncLocation = newUseCase(FakeAuthRepository(testUser))

        assertEquals(SyncState.Failed(testLocation.time, "HTTP 500"), syncLocation(testLocation))
    }

    @Test
    fun ioException_failed() = runTest {
        uploader.respond = { throw IOException("no network") }
        val syncLocation = newUseCase(FakeAuthRepository(testUser))

        assertEquals(SyncState.Failed(testLocation.time, "no network"), syncLocation(testLocation))
    }

    @Test
    fun tokenExpiresSoon_renewedBeforeUpload() = runTest {
        val auth = FakeAuthRepository(testUser)
        val syncLocation = newUseCase(auth, clock = { testUser.expiresAt.minusSeconds(60) })

        assertEquals(SyncState.Synced(testLocation.time), syncLocation(testLocation))
        assertEquals(1, auth.refreshes)
        assertEquals(listOf(testLocation to renewedUser.idToken), uploader.uploads)
    }

    @Test
    fun http401_renewedAndRetried() = runTest {
        uploader.respond = { token -> if (token == testUser.idToken) 401 else 200 }
        val auth = FakeAuthRepository(testUser)
        val syncLocation = newUseCase(auth)

        assertEquals(SyncState.Synced(testLocation.time), syncLocation(testLocation))
        assertEquals(1, auth.refreshes)
        assertEquals(
            listOf(testLocation to testUser.idToken, testLocation to renewedUser.idToken),
            uploader.uploads
        )
    }

    @Test
    fun http401_renewalFails_failed() = runTest {
        uploader.respond = { 401 }
        val syncLocation = newUseCase(FakeAuthRepository(testUser, renewed = null))

        assertEquals(
            SyncState.Failed(testLocation.time, "token expired, re-sign-in failed"),
            syncLocation(testLocation)
        )
        assertEquals(1, uploader.uploads.size)
    }

    @Test
    fun http401_afterRenewal_failed() = runTest {
        uploader.respond = { 401 }
        val auth = FakeAuthRepository(testUser)
        val syncLocation = newUseCase(auth)

        assertEquals(SyncState.Failed(testLocation.time, "HTTP 401"), syncLocation(testLocation))
        assertEquals(1, auth.refreshes) // no endless retries
    }
}
