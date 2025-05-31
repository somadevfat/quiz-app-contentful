package com.example.contentful_javasilver.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.contentful_javasilver.R
import com.example.contentful_javasilver.viewmodels.AuthViewModel
import com.google.firebase.auth.FirebaseAuth

@Composable
fun LoginScreen(
    // HiltからViewModelを取得
    viewModel: AuthViewModel = hiltViewModel(),
    // TODO: ログイン成功時のナビゲーション処理を追加
    onLoginSuccess: () -> Unit = {},
    // TODO: 新規登録画面へのナビゲーション処理を追加
    onNavigateToRegister: () -> Unit = {},
    onGoogleSignInClick: () -> Unit = {}
) {
    // State for email and password input fields
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Observe the login state from the ViewModel
    val loginState by viewModel.loginState.collectAsStateWithLifecycle()

    // Handle side effects when login state changes to Success
    LaunchedEffect(loginState) {
        if (loginState is AuthViewModel.LoginState.Success) {
            onLoginSuccess() // Navigate away on success
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("ログイン", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))

        // Email Input
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("メールアドレス") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Password Input
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("パスワード") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(32.dp))

        // Login Button
        Button(
            onClick = { viewModel.signInWithEmailPassword(email, password) },
            modifier = Modifier.fillMaxWidth(),
            enabled = loginState !is AuthViewModel.LoginState.Loading // Disable if loading
        ) {
            Text("ログイン")
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Google Sign-In Button
        Button(
            onClick = onGoogleSignInClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = loginState !is AuthViewModel.LoginState.Loading,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_google_logo),
                contentDescription = "Google Logo",
                modifier = Modifier.size(ButtonDefaults.IconSize),
                tint = Color.Unspecified
            )
            Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
            Text("Googleでログイン", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Navigate to Register Screen
        TextButton(onClick = onNavigateToRegister) {
            Text("アカウントをお持ちでない場合はこちら")
        }

        // Loading Indicator
        if (loginState is AuthViewModel.LoginState.Loading) {
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator()
        }

        // Error Message
        if (loginState is AuthViewModel.LoginState.Error) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = (loginState as AuthViewModel.LoginState.Error).message,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    // Preview doesn't need a real ViewModel.
    // We'll just call the Composable with default values or simplified logic if needed.
    // Since the viewModel parameter has a default value, we can call it without it for preview.
    // However, we need to pass the lambda functions.
    LoginScreen(
        onLoginSuccess = {},
        onNavigateToRegister = {},
        onGoogleSignInClick = {}
        // viewModel = hiltViewModel() // Let the default handle it, or provide a simple mock if absolutely necessary
    )
}

// Remove or comment out the placeholder ViewModel class as it's causing issues in preview
/*
open class AuthViewModel_Preview_Placeholder : AuthViewModel(FirebaseAuth.getInstance()) {
    // Override states/methods if needed for preview scenarios
}
*/

// Remember to add Hilt annotations to your Application class and Activity
// Example Application class:
// @HiltAndroidApp
// class YourApplication : Application() { ... }

// Example Activity:
// @AndroidEntryPoint
// class MainActivity : ComponentActivity() { ... } 