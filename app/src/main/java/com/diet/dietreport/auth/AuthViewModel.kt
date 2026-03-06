package com.diet.dietreport.auth

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diet.dietreport.AppError
import com.diet.dietreport.auth.data.AuthRepository
import com.diet.dietreport.auth.data.User
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val error: AppError? = null,
    val isInitialized: Boolean = false,
)

sealed class AuthNavEvent {
    data object ToHome : AuthNavEvent()
    data object ToSettings : AuthNavEvent()
    data object ToSignIn : AuthNavEvent()
}

class AuthViewModel(
    private val authService: AuthService,
    private val repository: AuthRepository,
    private val isOnboardingComplete: suspend () -> Boolean = { true },
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _navEvent = MutableSharedFlow<AuthNavEvent>(extraBufferCapacity = 1)
    val navEvent: SharedFlow<AuthNavEvent> = _navEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.user.collect { storedUser ->
                _uiState.update { it.copy(user = storedUser, isInitialized = true) }
            }
        }
    }

    fun signIn(context: Context) {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val user = authService.signIn(context)
                repository.saveUser(user)
                Log.d("DR/Auth", "Sign-in success: ${user.email}")
                _uiState.update { it.copy(isLoading = false) }
                val onboarded = isOnboardingComplete()
                _navEvent.emit(if (onboarded) AuthNavEvent.ToHome else AuthNavEvent.ToSettings)
            } catch (e: Exception) {
                Log.e("DR/Auth", "Sign-in failed", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = AppError.AuthError("Sign-in failed: ${e.message}", e),
                    )
                }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            repository.clearUser()
            Log.d("DR/Auth", "Sign-out")
            _navEvent.emit(AuthNavEvent.ToSignIn)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
