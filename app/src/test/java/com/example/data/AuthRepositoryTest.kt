package com.example.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.network.AuthApiRequest
import com.example.data.network.AuthApiResponse
import com.example.data.network.AuthApiService
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

class FakeAuthApiService : AuthApiService {
    var mockLoginCalls = 0
    var mockSignUpCalls = 0
    var mockLogoutCalls = 0

    override suspend fun mockLogin(request: AuthApiRequest): AuthApiResponse {
        mockLoginCalls++
        return AuthApiResponse(id = 101, title = request.title, body = request.body, userId = request.userId)
    }

    override suspend fun mockSignUp(request: AuthApiRequest): AuthApiResponse {
        mockSignUpCalls++
        return AuthApiResponse(id = 102, title = request.title, body = request.body, userId = request.userId)
    }

    override suspend fun mockLogout(request: AuthApiRequest): AuthApiResponse {
        mockLogoutCalls++
        return AuthApiResponse(id = 103, title = request.title, body = request.body, userId = request.userId)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AuthRepositoryTest {

    private lateinit var context: Context
    private lateinit var fakeAuthApiService: FakeAuthApiService
    private lateinit var authRepository: AuthRepositoryImpl

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        // Clear prefs before each test
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE).edit().clear().apply()
        context.getSharedPreferences("registered_users", Context.MODE_PRIVATE).edit().clear().apply()

        fakeAuthApiService = FakeAuthApiService()
        authRepository = AuthRepositoryImpl(
            context = context,
            authApiService = fakeAuthApiService,
            ioDispatcher = UnconfinedTestDispatcher()
        )
    }

    @Test
    fun initialAuthState_isUnauthenticated() {
        val currentState = authRepository.authState.value
        assertTrue(currentState is AuthState.Unauthenticated)
        assertNull(authRepository.getCurrentUser())
    }

    @Test
    fun signUp_createsUserAuthenticatesAndTriggersNetworkRequest() = runTest {
        val result = authRepository.signUp("test@example.com", "password123", "Test User")
        assertTrue(result.isSuccess)
        assertEquals(1, fakeAuthApiService.mockSignUpCalls)

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
        assertEquals(0, fakeAuthApiService.mockLoginCalls)
    }

    @Test
    fun login_withRegisteredUser_succeedsAndTriggersNetworkRequest() = runTest {
        authRepository.signUp("user@test.com", "secure123", "Alice")
        authRepository.logout()

        val loginResult = authRepository.login("user@test.com", "secure123")
        assertTrue(loginResult.isSuccess)
        assertEquals(1, fakeAuthApiService.mockLoginCalls)

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
    fun logout_clearsSessionAndTriggersNetworkRequest() = runTest {
        authRepository.guestLogin()
        assertTrue(authRepository.authState.value is AuthState.Authenticated)

        authRepository.logout()
        assertEquals(1, fakeAuthApiService.mockLogoutCalls)
        assertTrue(authRepository.authState.value is AuthState.Unauthenticated)
        assertNull(authRepository.getCurrentUser())
    }
}
