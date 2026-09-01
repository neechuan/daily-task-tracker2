package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.data.AuthState
import com.example.ui.AuthViewModel
import com.example.ui.AuthViewModelFactory
import com.example.ui.LoginScreen
import com.example.ui.TaskScreen
import com.example.ui.TaskViewModel
import com.example.ui.TaskViewModelFactory
import com.example.ui.theme.MyApplicationTheme



class MainActivity : ComponentActivity() {

    private val taskViewModel: TaskViewModel by viewModels {
        val app = application as TaskApplication
        TaskViewModelFactory(app.repository)
    }

    private val authViewModel: AuthViewModel by viewModels {
        val app = application as TaskApplication
        AuthViewModelFactory(app.authRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by taskViewModel.themeMode.collectAsStateWithLifecycle()
            val authState by authViewModel.authState.collectAsStateWithLifecycle()

            MyApplicationTheme(themeMode = themeMode) {
                when (val state = authState) {
                    is AuthState.Authenticated -> {
                        TaskScreen(
                            viewModel = taskViewModel,
                            currentUser = state.user,
                            onLogout = { authViewModel.logout() }
                        )
                    }
                    is AuthState.Loading -> {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                    else -> {
                        LoginScreen(viewModel = authViewModel)
                    }
                }
            }
        }
    }
}


