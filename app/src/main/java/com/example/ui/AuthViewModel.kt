package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AuthRepository
import com.example.data.AuthState
import com.example.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    val authState: StateFlow<AuthState> = authRepository.authState

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _displayName = MutableStateFlow("")
    val displayName: StateFlow<String> = _displayName.asStateFlow()

    private val _isSignUpMode = MutableStateFlow(false)
    val isSignUpMode: StateFlow<Boolean> = _isSignUpMode.asStateFlow()

    private val _isPasswordVisible = MutableStateFlow(false)
    val isPasswordVisible: StateFlow<Boolean> = _isPasswordVisible.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun onEmailChange(newEmail: String) {
        _email.value = newEmail
        _errorMessage.value = null
    }

    fun onPasswordChange(newPassword: String) {
        _password.value = newPassword
        _errorMessage.value = null
    }

    fun onDisplayNameChange(newName: String) {
        _displayName.value = newName
        _errorMessage.value = null
    }

    fun toggleSignUpMode() {
        _isSignUpMode.value = !_isSignUpMode.value
        _errorMessage.value = null
    }

    fun togglePasswordVisibility() {
        _isPasswordVisible.value = !_isPasswordVisible.value
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun submit() {
        if (_isSignUpMode.value) {
            performSignUp()
        } else {
            performLogin()
        }
    }

    fun performLogin() {
        val currentEmail = _email.value.trim()
        val currentPassword = _password.value.trim()

        if (currentEmail.isEmpty()) {
            _errorMessage.value = "Email is required."
            return
        }

        if (currentPassword.isEmpty()) {
            _errorMessage.value = "Password is required."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val result = authRepository.login(currentEmail, currentPassword)
            _isLoading.value = false
            result.onFailure { error ->
                _errorMessage.value = error.message ?: "Authentication failed."
            }
        }
    }

    fun performSignUp() {
        val currentEmail = _email.value.trim()
        val currentPassword = _password.value.trim()
        val currentName = _displayName.value.trim()

        if (currentName.isEmpty()) {
            _errorMessage.value = "Full Name is required."
            return
        }

        if (currentEmail.isEmpty()) {
            _errorMessage.value = "Email is required."
            return
        }

        if (currentPassword.length < 6) {
            _errorMessage.value = "Password must be at least 6 characters."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val result = authRepository.signUp(currentEmail, currentPassword, currentName)
            _isLoading.value = false
            result.onFailure { error ->
                _errorMessage.value = error.message ?: "Registration failed."
            }
        }
    }

    fun guestLogin() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            authRepository.guestLogin()
            _isLoading.value = false
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _email.value = ""
            _password.value = ""
            _displayName.value = ""
            _errorMessage.value = null
        }
    }
}

class AuthViewModelFactory(
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            return AuthViewModel(authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
