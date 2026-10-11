package com.github.phoswald.whereyoubeen.ui

import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

private const val TIMEOUT_MILLIS = 10_000
private const val TAG = "Avatar"

/** Round profile picture loaded from [pictureUrl]; shows the initial of [name] until (or unless) it is loaded. */
@Composable
fun Avatar(name: String, pictureUrl: String?, modifier: Modifier = Modifier) {
    val picture by produceState<ImageBitmap?>(null, pictureUrl) {
        value = pictureUrl?.let { loadImage(it) }
    }
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        val bitmap = picture
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = name.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

/** Returns null if the image could not be loaded or decoded. */
private suspend fun loadImage(url: String): ImageBitmap? = withContext(Dispatchers.IO) {
    val connection = URL(url).openConnection() as HttpURLConnection
    try {
        connection.connectTimeout = TIMEOUT_MILLIS
        connection.readTimeout = TIMEOUT_MILLIS
        connection.inputStream.use { BitmapFactory.decodeStream(it) }?.asImageBitmap()
    } catch (e: IOException) {
        Log.w(TAG, "loading profile picture failed: $e")
        null
    } finally {
        connection.disconnect()
    }
}
