package com.example.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.User
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AuthRepositoryTest {

    private lateinit var context: Context

    private lateinit var authRepository: AuthRepositoryImpl

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        // Clear prefs before each test
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE).edit().clear().apply()
        context.getSharedPreferences("registered_users", Context.MODE_PRIVATE).edit().clear().apply()

        authRepository = AuthRepositoryImpl(context, ioDispatcher = UnconfinedTestDispatcher())
    }


    @Test
    fun initialAuthState_isUnauthenticated() {
        val currentState = authRepository.authState.value
        assertTrue(currentState is AuthState.Unauthenticated)
        assertNull(authRepository.getCurrentUser())
    }

    @Test
    fun signUp_createsUserAndAuthenticates() = runTest {
        val result = authRepository.signUp("test@example.com", "password123", "Test User")
        assertTrue(result.isSuccess)

        val user = result.getOrNull()
        assertNotNull(user)
        assertEquals("test@example.com", user?.email)
        assertEquals("Test User", user?.displayName)

        val currentState = authRepository.authState.value
        assertTrue(currentState is AuthState.Authenticated)
        assertEquals(user, (currentState as AuthState.Authenticated).user)
    }

    @Test
    fun login_withEmptyEmail_returnsFailure() = runTest {
        val result = authRepository.login("", "password")
        assertTrue(result.isFailure)
        assertTrue(authRepository.authState.value is AuthState.Error)
    }

    @Test
    fun login_withRegisteredUser_succeeds() = runTest {
        authRepository.signUp("user@test.com", "secure123", "Alice")
        authRepository.logout()

        val loginResult = authRepository.login("user@test.com", "secure123")
        assertTrue(loginResult.isSuccess)

        val user = loginResult.getOrNull()
        assertEquals("Alice", user?.displayName)
        assertTrue(authRepository.authState.value is AuthState.Authenticated)
    }

    @Test
    fun guestLogin_authenticatesAsGuest() = runTest {
        val guestUser = authRepository.guestLogin()
        assertEquals("guest@example.com", guestUser.email)
        assertEquals("Guest User", guestUser.displayName)

        val state = authRepository.authState.value
        assertTrue(state is AuthState.Authenticated)
    }

    @Test
    fun logout_clearsSession() = runTest {
        authRepository.guestLogin()
        assertTrue(authRepository.authState.value is AuthState.Authenticated)

        authRepository.logout()
        assertTrue(authRepository.authState.value is AuthState.Unauthenticated)
        assertNull(authRepository.getCurrentUser())
    }
}
