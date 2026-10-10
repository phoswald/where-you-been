package com.github.phoswald.whereyoubeen.ui

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.phoswald.whereyoubeen.domain.AuthRepository
import com.github.phoswald.whereyoubeen.domain.User
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

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    // The repository outlives the activity while tracking keeps the process alive
    private val _state = MutableStateFlow(toState(authRepository.currentUser.value))
    val state: StateFlow<AuthState> = _state.asStateFlow()

    fun signIn(activity: Activity) {
        _state.value = AuthState.Loading
        viewModelScope.launch {
            _state.value = try {
                toState(authRepository.signIn(activity))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AuthState.SignedOut(error = e.message ?: e.toString())
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _state.value = AuthState.SignedOut()
        }
    }

    private fun toState(user: User?): AuthState =
        if (user != null) AuthState.SignedIn(user) else AuthState.SignedOut()
}
