package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.data.network.AuthApiRequest
import com.example.data.network.AuthApiService
import com.example.data.network.AuthNetworkClient
import com.example.model.User
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.UUID

interface AuthRepository {
    val authState: StateFlow<AuthState>
    suspend fun login(email: String, password: String): Result<User>
    suspend fun signUp(email: String, password: String, displayName: String): Result<User>
    suspend fun guestLogin(): User
    suspend fun logout()
    fun getCurrentUser(): User?
}

class AuthRepositoryImpl(
    context: Context,
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE),
    private val authApiService: AuthApiService = AuthNetworkClient.apiService,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AuthRepository {

    companion object {
        private const val PREFS_NAME = "auth_prefs"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NAME = "user_name"
        private const val USERS_PREFS_NAME = "registered_users"
    }

    private val userAccountsPrefs: SharedPreferences =
        context.getSharedPreferences(USERS_PREFS_NAME, Context.MODE_PRIVATE)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        checkStoredSession()
    }

    private fun checkStoredSession() {
        val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        if (isLoggedIn) {
            val id = prefs.getString(KEY_USER_ID, null)
            val email = prefs.getString(KEY_USER_EMAIL, null)
            val name = prefs.getString(KEY_USER_NAME, null)

            if (!id.isNullOrBlank() && !email.isNullOrBlank() && !name.isNullOrBlank()) {
                val user = User(id = id, email = email, displayName = name)
                _authState.value = AuthState.Authenticated(user)
            } else {
                _authState.value = AuthState.Unauthenticated
            }
        } else {
            _authState.value = AuthState.Unauthenticated
        }
    }

    override suspend fun login(email: String, password: String): Result<User> = withContext(ioDispatcher) {
        val trimmedEmail = email.trim()
        val trimmedPassword = password.trim()

        if (trimmedEmail.isEmpty() || trimmedPassword.isEmpty()) {
            val err = "Email and password cannot be empty."
            _authState.value = AuthState.Error(err)
            return@withContext Result.failure(IllegalArgumentException(err))
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches() && !trimmedEmail.contains("@")) {
            val err = "Please enter a valid email address."
            _authState.value = AuthState.Error(err)
            return@withContext Result.failure(IllegalArgumentException(err))
        }

        // Perform HTTP POST request to jsonplaceholder API
        try {
            authApiService.mockLogin(
                AuthApiRequest(
                    title = "LOGIN",
                    body = "email=$trimmedEmail"
                )
            )
        } catch (e: Exception) {
            // Handle network/HTTP exception if needed, but continue gracefully for offline/demo if desired
        }

        val storedPassKey = "user_pass_$trimmedEmail"
        val storedNameKey = "user_name_$trimmedEmail"
        val storedIdKey = "user_id_$trimmedEmail"

        val storedPassword = userAccountsPrefs.getString(storedPassKey, null)
        val storedName = userAccountsPrefs.getString(storedNameKey, null)
        val storedId = userAccountsPrefs.getString(storedIdKey, null)

        val user: User = if (storedPassword != null && storedName != null && storedId != null) {
            if (storedPassword != trimmedPassword) {
                val err = "Incorrect password. Please try again."
                _authState.value = AuthState.Error(err)
                return@withContext Result.failure(IllegalArgumentException(err))
            }
            User(id = storedId, email = trimmedEmail, displayName = storedName)
        } else {
            // Default demo account or dynamic auto-creation for unrecognized email during login
            val defaultName = trimmedEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
            val newId = UUID.randomUUID().toString()
            User(id = newId, email = trimmedEmail, displayName = defaultName)
        }

        saveSession(user)
        _authState.value = AuthState.Authenticated(user)
        Result.success(user)
    }

    override suspend fun signUp(email: String, password: String, displayName: String): Result<User> = withContext(ioDispatcher) {
        val trimmedEmail = email.trim()
        val trimmedPassword = password.trim()
        val trimmedName = displayName.trim()

        if (trimmedEmail.isEmpty() || trimmedPassword.isEmpty() || trimmedName.isEmpty()) {
            val err = "All fields are required."
            _authState.value = AuthState.Error(err)
            return@withContext Result.failure(IllegalArgumentException(err))
        }

        if (trimmedPassword.length < 6) {
            val err = "Password must be at least 6 characters."
            _authState.value = AuthState.Error(err)
            return@withContext Result.failure(IllegalArgumentException(err))
        }

        // Perform HTTP POST request to jsonplaceholder API
        try {
            authApiService.mockSignUp(
                AuthApiRequest(
                    title = "REGISTER",
                    body = "email=$trimmedEmail, name=$trimmedName"
                )
            )
        } catch (e: Exception) {
            // Log/handle network exception gracefully
        }

        val newId = UUID.randomUUID().toString()
        userAccountsPrefs.edit()
            .putString("user_pass_$trimmedEmail", trimmedPassword)
            .putString("user_name_$trimmedEmail", trimmedName)
            .putString("user_id_$trimmedEmail", newId)
            .apply()

        val user = User(id = newId, email = trimmedEmail, displayName = trimmedName)
        saveSession(user)
        _authState.value = AuthState.Authenticated(user)
        Result.success(user)
    }

    override suspend fun guestLogin(): User = withContext(ioDispatcher) {
        val guestUser = User(
            id = "guest_" + UUID.randomUUID().toString().take(8),
            email = "guest@example.com",
            displayName = "Guest User"
        )
        saveSession(guestUser)
        _authState.value = AuthState.Authenticated(guestUser)
        guestUser
    }

    override suspend fun logout() = withContext(ioDispatcher) {
        val currentUser = getCurrentUser()
        try {
            authApiService.mockLogout(
                AuthApiRequest(
                    title = "LOGOUT",
                    body = "user=${currentUser?.email ?: "guest"}"
                )
            )
        } catch (e: Exception) {
            // Log/handle network exception gracefully
        }

        prefs.edit().clear().apply()
        _authState.value = AuthState.Unauthenticated
    }

    override fun getCurrentUser(): User? {
        val state = _authState.value
        return if (state is AuthState.Authenticated) state.user else null
    }

    private fun saveSession(user: User) {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_USER_ID, user.id)
            .putString(KEY_USER_EMAIL, user.email)
            .putString(KEY_USER_NAME, user.displayName)
            .apply()
    }
}
