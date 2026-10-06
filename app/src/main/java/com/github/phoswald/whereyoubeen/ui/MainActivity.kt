package com.github.phoswald.whereyoubeen.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.phoswald.whereyoubeen.WhereYouBeenApp
import com.github.phoswald.whereyoubeen.ui.theme.WhereYouBeenTheme

class MainActivity : ComponentActivity() {

    private val viewModelFactory get() = (application as WhereYouBeenApp).container.viewModelFactory
    private val authViewModel: AuthViewModel by viewModels { viewModelFactory }
    private val locationViewModel: LocationViewModel by viewModels { viewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        authViewModel.signInSilently(this)
        setContent {
            WhereYouBeenTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val auth by authViewModel.state.collectAsStateWithLifecycle()
                    val location by locationViewModel.location.collectAsStateWithLifecycle()
                    val sync by locationViewModel.sync.collectAsStateWithLifecycle()
                    val requestLocationPermission = rememberLocationPermissionRequest(
                        requestOnStart = savedInstanceState == null
                    )
                    MainScreen(
                        auth = auth,
                        location = location,
                        sync = sync,
                        onSignIn = { authViewModel.signIn(this) },
                        onSignOut = authViewModel::signOut,
                        onRequestLocationPermission = requestLocationPermission,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
