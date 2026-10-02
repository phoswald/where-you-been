package com.github.phoswald.whereyoubeen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.phoswald.whereyoubeen.ui.theme.WhereYouBeenTheme

class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        authViewModel.signInSilently(this)
        setContent {
            WhereYouBeenTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val state by authViewModel.state.collectAsStateWithLifecycle()
                    MainScreen(
                        state = state,
                        onSignIn = { authViewModel.signIn(this) },
                        onSignOut = authViewModel::signOut,
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
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when (state) {
            AuthState.Loading -> CircularProgressIndicator()
            is AuthState.SignedOut -> {
                Greeting(name = stringResource(R.string.anonymous_name))
                state.error?.let { Text(text = it, color = MaterialTheme.colorScheme.error) }
                Button(onClick = onSignIn) { Text(stringResource(R.string.sign_in)) }
            }
            is AuthState.SignedIn -> {
                Greeting(name = state.user.displayName ?: state.user.email)
                Text(text = state.user.email)
                Button(onClick = onSignOut) { Text(stringResource(R.string.sign_out)) }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.greeting, name),
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    WhereYouBeenTheme {
        Greeting("Android")
    }
}

@Preview(showBackground = true)
@Composable
fun SignedInPreview() {
    WhereYouBeenTheme {
        MainScreen(
            state = AuthState.SignedIn(User("Jane Doe", "jane@example.com", "")),
            onSignIn = {},
            onSignOut = {}
        )
    }
}
