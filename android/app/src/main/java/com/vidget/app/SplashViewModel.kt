package com.vidget.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vidget.domain.repository.ThemePreferencesRepository
import com.vidget.ui.navigation.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

sealed interface SplashUiState {
    data object Loading : SplashUiState
    data class Success(val startDestination: Route) : SplashUiState
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val themePreferencesRepository: ThemePreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SplashUiState>(SplashUiState.Loading)
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    val onboardingDone: StateFlow<Boolean> = themePreferencesRepository.onboardingDone
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false
        )

    init {
        loadAppInitialization()
    }

    private fun loadAppInitialization() {
        viewModelScope.launch {
            // Wait for yt-dlp/FFmpeg initialization from VidGetApp with a maximum 2-second timeout
            withTimeoutOrNull(2000L) {
                VidGetApp.instance.initializationState.first { state ->
                    state is InitializationState.Ready || state is InitializationState.Error
                }
            }

            // Check if user has already completed onboarding
            val isCompleted = themePreferencesRepository.onboardingDone.first()
            val destination = if (isCompleted) Route.Home else Route.Onboarding

            _uiState.value = SplashUiState.Success(destination)
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            themePreferencesRepository.setOnboardingDone(true)
        }
    }
}
