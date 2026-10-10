package com.github.phoswald.whereyoubeen.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

private const val TOKEN_URL = "https://phoswald.ch/rstm/oauth/token"
private const val TIMEOUT_MILLIS = 10_000

/** Exchanges a Google ID token for the backend's long-lived access token (RFC 8693). */
class TokenExchanger {

    /** Returns the access token; throws [IOException] if the exchange failed. */
    suspend fun exchange(idToken: String): String = withContext(Dispatchers.IO) {
        val body = formOf(
            "grant_type" to "urn:ietf:params:oauth:grant-type:token-exchange",
            "subject_token_type" to "urn:ietf:params:oauth:token-type:id_token",
            "subject_token" to idToken,
        )
        val connection = URL(TOKEN_URL).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = TIMEOUT_MILLIS
            connection.readTimeout = TIMEOUT_MILLIS
            connection.doOutput = true
            connection.setFixedLengthStreamingMode(body.size)
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            connection.outputStream.use { it.write(body) }
            val status = connection.responseCode
            if (status !in 200..299) {
                throw IOException("token exchange failed: HTTP $status")
            }
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            JSONObject(response).getString("access_token")
        } finally {
            connection.disconnect()
        }
    }

    private fun formOf(vararg params: Pair<String, String>): ByteArray =
        params.joinToString("&") { (name, value) -> "$name=${URLEncoder.encode(value, "UTF-8")}" }
            .toByteArray()
}
