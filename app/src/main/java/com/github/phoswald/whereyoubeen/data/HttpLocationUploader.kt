package com.github.phoswald.whereyoubeen.data

import android.util.Log
import com.github.phoswald.whereyoubeen.domain.GeoLocation
import com.github.phoswald.whereyoubeen.domain.LocationUploader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private const val UPLOAD_URL = "https://phoswald.ch/rstm/app/rest/locations"
private const val TIMEOUT_MILLIS = 10_000
private const val TAG = "HttpLocationUploader"

private val timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC)

/** Posts locations to the backend, authenticated with a Google ID token. */
class HttpLocationUploader : LocationUploader {

    override suspend fun upload(location: GeoLocation, idToken: String): Int = withContext(Dispatchers.IO) {
        try {
            val status = post(toJson(location), idToken)
            if (status !in 200..299) {
                Log.w(TAG, "upload failed: HTTP $status")
            }
            status
        } catch (e: IOException) {
            Log.w(TAG, "upload failed: $e")
            throw e
        }
    }

    private fun toJson(location: GeoLocation): ByteArray =
        JSONObject()
            .put("latitude", location.latitude)
            .put("longitude", location.longitude)
            .put("timestamp", timeFormatter.format(location.time))
            .toString()
            .toByteArray()

    private fun post(body: ByteArray, idToken: String): Int {
        val connection = URL(UPLOAD_URL).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = TIMEOUT_MILLIS
            connection.readTimeout = TIMEOUT_MILLIS
            connection.doOutput = true
            connection.setFixedLengthStreamingMode(body.size)
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $idToken")
            connection.outputStream.use { it.write(body) }
            return connection.responseCode
        } finally {
            connection.disconnect()
        }
    }
}
