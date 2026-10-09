package com.github.phoswald.whereyoubeen.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.phoswald.whereyoubeen.R
import com.github.phoswald.whereyoubeen.domain.GeoLocation
import com.github.phoswald.whereyoubeen.domain.LocationStatus
import com.github.phoswald.whereyoubeen.domain.SyncState
import com.github.phoswald.whereyoubeen.domain.User
import com.github.phoswald.whereyoubeen.ui.theme.WhereYouBeenTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    .withZone(ZoneId.systemDefault())

@Composable
fun MainScreen(
    auth: AuthState,
    trackingEnabled: Boolean,
    location: LocationStatus,
    sync: SyncState,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onTrackingEnabledChange: (Boolean) -> Unit,
    onRequestLocationPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Title()
        UserInfo(auth = auth, onSignIn = onSignIn, onSignOut = onSignOut)
        TrackingSwitch(enabled = trackingEnabled, onEnabledChange = onTrackingEnabledChange)
        LocationInfo(location = location, onRequestPermission = onRequestLocationPermission)
        SyncInfo(sync = sync)
        OsmMap(
            location = (location as? LocationStatus.Available)?.location,
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
fun UserInfo(auth: AuthState, onSignIn: () -> Unit, onSignOut: () -> Unit) {
    when (auth) {
        AuthState.Loading -> {
            CircularProgressIndicator()
        }
        is AuthState.SignedOut -> {
            if (auth.error != null) {
                Text(text = auth.error, color = MaterialTheme.colorScheme.error)
            }
            Button(onClick = onSignIn) { Text(stringResource(R.string.sign_in)) }
        }
        is AuthState.SignedIn -> {
            val name = auth.user.displayName ?: stringResource(R.string.no_name)
            Text(stringResource(R.string.signed_in, name, auth.user.email))
            Button(onClick = onSignOut) { Text(stringResource(R.string.sign_out)) }
        }
    }
}

@Composable
fun TrackingSwitch(enabled: Boolean, onEnabledChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(stringResource(R.string.tracking))
        Switch(checked = enabled, onCheckedChange = onEnabledChange)
    }
}

@Composable
fun LocationInfo(location: LocationStatus, onRequestPermission: () -> Unit) {
    when (location) {
        LocationStatus.Off -> {
            Text(stringResource(R.string.location_off))
        }
        LocationStatus.NoPermission -> {
            Text(stringResource(R.string.location_no_permission))
            Button(onClick = onRequestPermission) { Text(stringResource(R.string.location_allow)) }
        }
        LocationStatus.Disabled -> {
            Text(stringResource(R.string.location_disabled))
        }
        is LocationStatus.Waiting -> {
            val time = timeFormatter.format(location.time)
            Text(stringResource(R.string.location_waiting, time))
        }
        is LocationStatus.Available -> {
            val (latitude, longitude, time) = location.location
            Text(stringResource(R.string.location_available, latitude, longitude, timeFormatter.format(time)))
        }
    }
}

@Composable
fun SyncInfo(sync: SyncState) {
    when (sync) {
        SyncState.None -> {
            Text(stringResource(R.string.sync_none))
        }
        is SyncState.Synced -> {
            Text(stringResource(R.string.sync_ok, timeFormatter.format(sync.time)))
        }
        is SyncState.Failed -> {
            Text(
                text = stringResource(R.string.sync_failed, sync.reason, timeFormatter.format(sync.time)),
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SignedInPreview() {
    WhereYouBeenTheme {
        MainScreen(
            auth = AuthState.SignedIn(User("Jane Doe", "jane@example.com", "", Instant.MAX)),
            trackingEnabled = true,
            location = LocationStatus.Available(GeoLocation(47.376887, 8.541694, Instant.now())),
            sync = SyncState.Synced(Instant.now()),
            onSignIn = {},
            onSignOut = {},
            onTrackingEnabledChange = {},
            onRequestLocationPermission = {}
        )
    }
}
