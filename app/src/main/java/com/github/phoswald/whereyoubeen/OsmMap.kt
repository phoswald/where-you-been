package com.github.phoswald.whereyoubeen

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.util.Log
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView

/** Android's reserved host for app-local content; an https origin makes tile requests send a Referer. */
private const val BASE_URL = "https://appassets.androidplatform.net/"
private const val TAG = "OsmMap"

/** OpenStreetMap (Leaflet in a WebView) with a marker at [location]; the marker is removed while there is none. */
@Composable
fun OsmMap(location: LocationState.Available?, modifier: Modifier = Modifier) {
    if (LocalInspectionMode.current) {
        Box(modifier)
        return
    }
    val map = remember { MapPage() }
    AndroidView(
        factory = { context -> map.createWebView(context) },
        update = { map.setLocation(location) },
        onRelease = { it.destroy() },
        modifier = modifier,
    )
}

private class MapPage {
    private var location: LocationState.Available? = null
    private var webView: WebView? = null
    private var pageLoaded = false

    // JavaScript only runs our own page (Leaflet); links are opened outside the WebView
    @SuppressLint("SetJavaScriptEnabled")
    fun createWebView(context: Context): WebView = WebView(context).apply {
        webView = this
        pageLoaded = false
        // AndroidView defaults to WRAP_CONTENT, which makes the page's viewport 0px high
        layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        settings.javaScriptEnabled = true
        if (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            WebView.setWebContentsDebuggingEnabled(true) // chrome://inspect
        }
        webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                Log.d(TAG, "${message.messageLevel()} ${message.message()} " +
                        "(${message.sourceId()}:${message.lineNumber()})")
                return true
            }
        }
        webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String?) {
                Log.d(TAG, "page finished, view ${view.width}x${view.height}")
                pageLoaded = true
                updateLocation()
            }

            override fun onReceivedError(
                view: WebView, request: WebResourceRequest, error: WebResourceError
            ) {
                Log.w(TAG, "${request.url}: ${error.errorCode} ${error.description}")
            }

            override fun onReceivedHttpError(
                view: WebView, request: WebResourceRequest, response: WebResourceResponse
            ) {
                Log.w(TAG, "${request.url}: HTTP ${response.statusCode} ${response.reasonPhrase}")
            }

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                view.context.startActivity(Intent(Intent.ACTION_VIEW, request.url))
                return true
            }
        }
        val html = context.assets.open("osm_map.html").bufferedReader().use { it.readText() }
        loadDataWithBaseURL(BASE_URL, html, "text/html", "UTF-8", null)
    }

    fun setLocation(location: LocationState.Available?) {
        this.location = location
        updateLocation()
    }

    private fun updateLocation() {
        if (pageLoaded) {
            val location = this@MapPage.location
            val script = if (location != null) "setLocation(${location.latitude}, ${location.longitude})" else "clearLocation()"
            webView?.evaluateJavascript(script, null)
        }
    }
}
