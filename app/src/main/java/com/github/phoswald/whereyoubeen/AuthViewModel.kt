package com.github.phoswald.whereyoubeen

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AuthState {
    data object Loading : AuthState
    data class SignedOut(val error: String? = null) : AuthState
    data class SignedIn(val user: User) : AuthState
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val auth = GoogleAuth(application)
    private val _state = MutableStateFlow<AuthState>(AuthState.Loading)
    val state: StateFlow<AuthState> = _state.asStateFlow()
    private var triedSilentSignIn = false

    /** Called once from the activity; no-op on configuration changes. */
    fun signInSilently(activity: Activity) {
        if (triedSilentSignIn) return
        triedSilentSignIn = true
        runSignIn { auth.signInSilently(activity) }
    }

    fun signIn(activity: Activity) = runSignIn { auth.signInInteractive(activity) }

    fun signOut() {
        viewModelScope.launch {
            auth.signOut()
            _state.value = AuthState.SignedOut()
        }
    }

    private fun runSignIn(block: suspend () -> User?) {
        _state.value = AuthState.Loading
        viewModelScope.launch {
            _state.value = try {
                block()?.let { AuthState.SignedIn(it) } ?: AuthState.SignedOut()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AuthState.SignedOut(error = e.message ?: e.toString())
            }
        }
    }
}
