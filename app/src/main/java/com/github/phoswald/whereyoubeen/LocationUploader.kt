package com.github.phoswald.whereyoubeen

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private const val UPLOAD_URL = "https://phoswald.ch/rstm/app/rest/where-you-been"
private const val TIMEOUT_MILLIS = 10_000

private val timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC)

/** Posts locations to the backend, authenticated with a Google ID token. */
class LocationUploader {

    /** Returns the HTTP status code; throws [java.io.IOException] if no response was received. */
    suspend fun upload(location: LocationState.Available, idToken: String): Int = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("latitude", location.latitude)
            .put("longitude", location.longitude)
            .put("timestamp", timeFormatter.format(location.time))
            .toString()
            .toByteArray()
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
            connection.responseCode
        } finally {
            connection.disconnect()
        }
    }
}
