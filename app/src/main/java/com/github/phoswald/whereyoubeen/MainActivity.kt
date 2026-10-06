package com.github.phoswald.whereyoubeen

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.phoswald.whereyoubeen.ui.theme.WhereYouBeenTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val locationPermissions = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    .withZone(ZoneId.systemDefault())

class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()
    private val locationViewModel: LocationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        authViewModel.signInSilently(this)
        setContent {
            WhereYouBeenTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val state by authViewModel.state.collectAsStateWithLifecycle()
                    val location by locationViewModel.state.collectAsStateWithLifecycle()
                    val syncStatus by locationViewModel.syncStatus.collectAsStateWithLifecycle()
                    // The location state re-checks the permission on its next poll
                    val permissionLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestMultiplePermissions()
                    ) {}
                    LaunchedEffect(state) {
                        locationViewModel.idToken = (state as? AuthState.SignedIn)?.user?.idToken
                    }
                    LaunchedEffect(Unit) {
                        if (savedInstanceState == null &&
                            checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) !=
                            PackageManager.PERMISSION_GRANTED
                        ) permissionLauncher.launch(locationPermissions)
                    }
                    MainScreen(
                        state = state,
                        location = location,
                        syncStatus = syncStatus,
                        onSignIn = { authViewModel.signIn(this) },
                        onSignOut = authViewModel::signOut,
                        onRequestLocationPermission = { permissionLauncher.launch(locationPermissions) },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun MainScreen(
    state: AuthState,
    location: LocationState,
    syncStatus: Int?,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onRequestLocationPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Title()
        UserInfo(state = state, onSignIn = onSignIn, onSignOut = onSignOut)
        LocationInfo(location = location, onRequestPermission = onRequestLocationPermission)
        SyncState(status = syncStatus, location = location)
        OsmMap(
            location = location as? LocationState.Available,
            modifier = Modifier.fillMaxWidth().weight(1f)
        )
    }
}

@Composable
fun Title() {
    Text(
        text = stringResource(R.string.title),
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun UserInfo(state: AuthState, onSignIn: () -> Unit, onSignOut: () -> Unit) {
    when (state) {
        AuthState.Loading -> CircularProgressIndicator()
        is AuthState.SignedOut -> {
            state.error?.let { Text(text = it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = onSignIn) { Text(stringResource(R.string.sign_in)) }
        }
        is AuthState.SignedIn -> {
            Text(stringResource(
                R.string.signed_in,
                state.user.displayName ?: stringResource(R.string.no_name),
                state.user.email
            ))
            Button(onClick = onSignOut) { Text(stringResource(R.string.sign_out)) }
        }
    }
}

@Composable
fun LocationInfo(location: LocationState, onRequestPermission: () -> Unit) {
    when (location) {
        LocationState.NoPermission -> {
            Text(stringResource(R.string.location_no_permission))
            Button(onClick = onRequestPermission) { Text(stringResource(R.string.location_allow)) }
        }
        LocationState.Disabled -> Text(stringResource(R.string.location_disabled))
        is LocationState.Waiting -> Text(stringResource(
            R.string.location_waiting,
            timeFormatter.format(location.time)
        ))
        is LocationState.Available -> Text(stringResource(
            R.string.location_available,
            location.latitude,
            location.longitude,
            timeFormatter.format(location.time)
        ))
    }
}

@Composable
fun SyncState(status: Int?, location: LocationState) {
    if (status != null && location is LocationState.Available) {
        Text(stringResource(R.string.sync_status, status, timeFormatter.format(location.time)))
    } else {
        Text(stringResource(R.string.sync_none))
    }
}

@Preview(showBackground = true)
@Composable
fun SignedInPreview() {
    WhereYouBeenTheme {
        MainScreen(
            state = AuthState.SignedIn(User("Jane Doe", "jane@example.com", "")),
            location = LocationState.Available(47.376887, 8.541694, Instant.now()),
            syncStatus = 200,
            onSignIn = {},
            onSignOut = {},
            onRequestLocationPermission = {}
        )
    }
}
