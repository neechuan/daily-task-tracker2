package com.example.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AuthRepositoryImpl
import com.example.data.AuthState
import com.example.data.FakeAuthApiService

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
class AuthViewModelTest {


    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: Context
    private lateinit var authRepository: AuthRepositoryImpl
    private lateinit var viewModel: AuthViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE).edit().clear().apply()
        context.getSharedPreferences("registered_users", Context.MODE_PRIVATE).edit().clear().apply()

        authRepository = AuthRepositoryImpl(

            context = context,
            authApiService = FakeAuthApiService(),
            ioDispatcher = testDispatcher
        )
        viewModel = AuthViewModel(authRepository)
    }



    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialValues_areCorrect() {
        assertEquals("", viewModel.email.value)
        assertEquals("", viewModel.password.value)
        assertEquals("", viewModel.displayName.value)
        assertFalse(viewModel.isSignUpMode.value)
        assertFalse(viewModel.isPasswordVisible.value)
        assertNull(viewModel.errorMessage.value)
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun toggleSignUpMode_togglesValue() {
        viewModel.toggleSignUpMode()
        assertTrue(viewModel.isSignUpMode.value)

        viewModel.toggleSignUpMode()
        assertFalse(viewModel.isSignUpMode.value)
    }

    @Test
    fun togglePasswordVisibility_togglesValue() {
        viewModel.togglePasswordVisibility()
        assertTrue(viewModel.isPasswordVisible.value)

        viewModel.togglePasswordVisibility()
        assertFalse(viewModel.isPasswordVisible.value)
    }

    @Test
    fun performLogin_withEmptyFields_setsErrorMessage() {
        viewModel.onEmailChange("")
        viewModel.onPasswordChange("")
        viewModel.performLogin()

        assertEquals("Email is required.", viewModel.errorMessage.value)
    }

    @Test
    fun performSignUp_withShortPassword_setsErrorMessage() {
        viewModel.toggleSignUpMode()
        viewModel.onDisplayNameChange("Jane Doe")
        viewModel.onEmailChange("jane@example.com")
        viewModel.onPasswordChange("12345")
        viewModel.performSignUp()

        assertEquals("Password must be at least 6 characters.", viewModel.errorMessage.value)
    }

    @Test
    fun guestLogin_triggersAuthenticatedState() = runTest {
        viewModel.guestLogin()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.authState.value
        assertTrue(state is AuthState.Authenticated)
        assertEquals("guest@example.com", (state as AuthState.Authenticated).user.email)
    }
}
