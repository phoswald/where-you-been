package com.github.phoswald.whereyoubeen

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.github.phoswald.whereyoubeen.data.GoogleAuthRepository
import com.github.phoswald.whereyoubeen.data.GpsLocationSource
import com.github.phoswald.whereyoubeen.data.HttpLocationUploader
import com.github.phoswald.whereyoubeen.domain.AuthRepository
import com.github.phoswald.whereyoubeen.domain.LocationSource
import com.github.phoswald.whereyoubeen.domain.LocationTracker
import com.github.phoswald.whereyoubeen.domain.SyncLocationUseCase
import com.github.phoswald.whereyoubeen.ui.AuthViewModel
import com.github.phoswald.whereyoubeen.ui.LocationViewModel

/** Manual dependency injection: creates the app-wide singletons and wires them together. */
class AppContainer(context: Context) {

    private val authRepository: AuthRepository = GoogleAuthRepository(context)
    private val locationSource: LocationSource = GpsLocationSource(context)
    private val syncLocation = SyncLocationUseCase(authRepository, HttpLocationUploader())
    val locationTracker = LocationTracker(locationSource, syncLocation)

    val viewModelFactory: ViewModelProvider.Factory = viewModelFactory {
        initializer { AuthViewModel(authRepository) }
        initializer { LocationViewModel(locationTracker) }
    }
}
