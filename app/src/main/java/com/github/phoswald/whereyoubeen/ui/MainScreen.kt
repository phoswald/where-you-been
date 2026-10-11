package com.github.phoswald.whereyoubeen.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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

private val cardShape = RoundedCornerShape(16.dp)

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
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Header()
        Card(
            shape = cardShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                UserInfo(auth = auth, onSignIn = onSignIn, onSignOut = onSignOut)
                HorizontalDivider()
                TrackingSwitch(enabled = trackingEnabled, onEnabledChange = onTrackingEnabledChange)
                LocationInfo(location = location, onRequestPermission = onRequestLocationPermission)
                SyncInfo(sync = sync)
            }
        }
        OsmMap(
            location = (location as? LocationStatus.Available)?.location,
            modifier = Modifier.fillMaxWidth().weight(1f).clip(cardShape)
        )
    }
}

@Composable
fun Header() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(R.drawable.ic_logo),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(32.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = stringResource(R.string.title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun UserInfo(auth: AuthState, onSignIn: () -> Unit, onSignOut: () -> Unit) {
    when (auth) {
        AuthState.Loading -> {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.size(24.dp))
            }
        }
        is AuthState.SignedOut -> {
            if (auth.error != null) {
                Text(
                    text = auth.error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            FilledTonalButton(onClick = onSignIn, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.sign_in))
            }
        }
        is AuthState.SignedIn -> {
            val name = auth.user.displayName ?: stringResource(R.string.no_name)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(name = name, pictureUrl = auth.user.pictureUrl)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = auth.user.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                TextButton(onClick = onSignOut) { Text(stringResource(R.string.sign_out)) }
            }
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
        Text(stringResource(R.string.tracking), style = MaterialTheme.typography.titleSmall)
        Switch(checked = enabled, onCheckedChange = onEnabledChange)
    }
}

@Composable
fun LocationInfo(location: LocationStatus, onRequestPermission: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    when (location) {
        LocationStatus.Off -> {
            StatusRow(R.drawable.ic_location, stringResource(R.string.location_off))
        }
        LocationStatus.NoPermission -> {
            StatusRow(R.drawable.ic_location, stringResource(R.string.location_no_permission), colors.error) {
                TextButton(onClick = onRequestPermission) { Text(stringResource(R.string.location_allow)) }
            }
        }
        LocationStatus.Disabled -> {
            StatusRow(R.drawable.ic_location, stringResource(R.string.location_disabled))
        }
        is LocationStatus.Waiting -> {
            val time = timeFormatter.format(location.time)
            StatusRow(R.drawable.ic_location, stringResource(R.string.location_waiting, time))
        }
        is LocationStatus.Inaccurate -> {
            val time = timeFormatter.format(location.time)
            StatusRow(R.drawable.ic_location, stringResource(R.string.location_inaccurate, location.accuracy, time), colors.error)
        }
        is LocationStatus.Available -> {
            val (latitude, longitude, time) = location.location
            val text = stringResource(R.string.location_available, latitude, longitude, timeFormatter.format(time))
            StatusRow(R.drawable.ic_location, text, colors.primary)
        }
    }
}

@Composable
fun SyncInfo(sync: SyncState) {
    val colors = MaterialTheme.colorScheme
    when (sync) {
        SyncState.None -> {
            StatusRow(R.drawable.ic_cloud, stringResource(R.string.sync_none))
        }
        is SyncState.Synced -> {
            StatusRow(R.drawable.ic_cloud_done, stringResource(R.string.sync_ok, timeFormatter.format(sync.time)), colors.primary)
        }
        is SyncState.Failed -> {
            val text = stringResource(R.string.sync_failed, sync.reason, timeFormatter.format(sync.time))
            StatusRow(R.drawable.ic_cloud_off, text, colors.error)
        }
    }
}

/** A compact status line: small tinted icon, small text, and an optional trailing action. */
@Composable
private fun StatusRow(
    @DrawableRes icon: Int,
    text: String,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    action: (@Composable () -> Unit)? = null
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = color,
            modifier = Modifier.weight(1f)
        )
        action?.invoke()
    }
}

@Preview(showBackground = true)
@Composable
fun SignedInPreview() {
    WhereYouBeenTheme {
        MainScreen(
            auth = AuthState.SignedIn(User("Jane Doe", "jane@example.com", "")),
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

@Preview(showBackground = true)
@Composable
fun SignedOutPreview() {
    WhereYouBeenTheme {
        MainScreen(
            auth = AuthState.SignedOut(),
            trackingEnabled = true,
            location = LocationStatus.Inaccurate(Instant.now(), 23.4f),
            sync = SyncState.Failed(Instant.now(), "not logged in"),
            onSignIn = {},
            onSignOut = {},
            onTrackingEnabledChange = {},
            onRequestLocationPermission = {}
        )
    }
}
