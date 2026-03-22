package com.stockmarket.tutorials.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stockmarket.tutorials.data.model.User
import com.stockmarket.tutorials.data.model.UserRole
import com.stockmarket.tutorials.data.repository.AuthRepository
import com.stockmarket.tutorials.util.Resource
import com.stockmarket.tutorials.util.SecurityUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for authentication and user state management.
 * Handles login, session validation, and device binding.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    application: Application
) : AndroidViewModel(application) {

    // Auth state
    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    // Current user profile
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Error messages
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        checkExistingSession()
    }

    /**
     * Check if user has an existing valid session.
     */
    private fun checkExistingSession() {
        viewModelScope.launch {
            if (authRepository.isLoggedIn) {
                _isLoading.value = true
                when (val result = authRepository.getCurrentUserProfile()) {
                    is Resource.Success -> {
                        val user = result.data
                        if (user.isActive) {
                            _currentUser.value = user
                            _authState.value = if (user.role == UserRole.ADMIN) {
                                AuthState.AuthenticatedAdmin
                            } else {
                                AuthState.AuthenticatedUser
                            }
                        } else {
                            authRepository.signOut()
                            _authState.value = AuthState.NotAuthenticated
                        }
                    }
                    is Resource.Error -> {
                        authRepository.signOut()
                        _authState.value = AuthState.NotAuthenticated
                    }
                    is Resource.Loading -> {}
                }
                _isLoading.value = false
            } else {
                _authState.value = AuthState.NotAuthenticated
            }
        }
    }

    /**
     * Sign in with email and password.
     * Includes device binding validation.
     */
    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            if (email.isBlank() || password.isBlank()) {
                _errorMessage.value = "Please enter both email and password"
                _isLoading.value = false
                return@launch
            }

            val deviceId = SecurityUtils.getDeviceFingerprint(getApplication())

            when (val result = authRepository.signIn(email, password, deviceId)) {
                is Resource.Success -> {
                    val user = result.data
                    _currentUser.value = user
                    _authState.value = if (user.role == UserRole.ADMIN) {
                        AuthState.AuthenticatedAdmin
                    } else {
                        AuthState.AuthenticatedUser
                    }
                }
                is Resource.Error -> {
                    _errorMessage.value = result.message
                }
                is Resource.Loading -> {}
            }
            _isLoading.value = false
        }
    }

    /**
     * Sign out and clear all local state.
     */
    fun signOut() {
        authRepository.signOut()
        _currentUser.value = null
        _authState.value = AuthState.NotAuthenticated
        _errorMessage.value = null
    }

    /**
     * Clear error message.
     */
    fun clearError() {
        _errorMessage.value = null
    }
}

/**
 * Sealed class representing authentication states.
 */
sealed class AuthState {
    data object Initial : AuthState()
    data object NotAuthenticated : AuthState()
    data object AuthenticatedUser : AuthState()
    data object AuthenticatedAdmin : AuthState()
}
