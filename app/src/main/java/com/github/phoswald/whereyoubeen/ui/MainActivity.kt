package com.github.phoswald.whereyoubeen.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.phoswald.whereyoubeen.WhereYouBeenApp
import com.github.phoswald.whereyoubeen.domain.LocationStatus
import com.github.phoswald.whereyoubeen.service.LocationTrackingService
import com.github.phoswald.whereyoubeen.ui.theme.WhereYouBeenTheme

class MainActivity : ComponentActivity() {

    private val viewModelFactory get() = (application as WhereYouBeenApp).container.viewModelFactory
    private val authViewModel: AuthViewModel by viewModels { viewModelFactory }
    private val locationViewModel: LocationViewModel by viewModels { viewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        authViewModel.signInSilently(this)
        if (savedInstanceState == null) {
            // Every app launch starts tracking, even if the process survived from an earlier launch
            locationViewModel.setTrackingEnabled(true)
        }
        setContent {
            WhereYouBeenTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val auth by authViewModel.state.collectAsStateWithLifecycle()
                    val trackingEnabled by locationViewModel.trackingEnabled.collectAsStateWithLifecycle()
                    val location by locationViewModel.location.collectAsStateWithLifecycle()
                    val sync by locationViewModel.sync.collectAsStateWithLifecycle()
                    val locationPermission = rememberLocationPermission(
                        requestOnStart = savedInstanceState == null
                    )
                    // The service stops itself when tracking is disabled
                    LaunchedEffect(locationPermission.granted, trackingEnabled) {
                        if (locationPermission.granted && trackingEnabled) {
                            LocationTrackingService.start(this@MainActivity)
                        }
                    }
                    MainScreen(
                        auth = auth,
                        trackingEnabled = trackingEnabled,
                        location = if (locationPermission.granted) location else LocationStatus.NoPermission,
                        sync = sync,
                        onSignIn = { authViewModel.signIn(this) },
                        onSignOut = authViewModel::signOut,
                        onTrackingEnabledChange = locationViewModel::setTrackingEnabled,
                        onRequestLocationPermission = locationPermission.request,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        authViewModel.refreshIfExpired(this)
    }
}
