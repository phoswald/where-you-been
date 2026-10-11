package com.github.phoswald.whereyoubeen.ui

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.phoswald.whereyoubeen.domain.AuthRepository
import com.github.phoswald.whereyoubeen.domain.User
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AuthState {
    data object Loading : AuthState
    data class SignedOut(val error: String? = null) : AuthState
    data class SignedIn(val user: User) : AuthState
}

/** The signed-in user comes from [AuthRepository]; only the sign-in progress and error are local. */
class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val signingIn = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)

    val state: StateFlow<AuthState> =
        combine(authRepository.currentUser, signingIn, error, ::toState)
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                toState(authRepository.currentUser.value, signingIn.value, error.value)
            )

    fun signIn(activity: Activity) {
        error.value = null
        signingIn.value = true
        viewModelScope.launch {
            try {
                authRepository.signIn(activity)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error.value = e.message ?: e.toString()
            } finally {
                signingIn.value = false
            }
        }
    }

    fun signOut() {
        error.value = null
        authRepository.signOut()
    }

    private fun toState(user: User?, signingIn: Boolean, error: String?): AuthState = when {
        signingIn -> AuthState.Loading
        user != null -> AuthState.SignedIn(user)
        else -> AuthState.SignedOut(error)
    }
}
