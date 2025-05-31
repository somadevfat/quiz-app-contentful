package com.example.contentful_javasilver.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onRegisterSuccess: () -> Unit = {},
    onNavigateBackToLogin: () -> Unit = {},
    onGoogleSignInClick: () -> Unit = {}
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf<String?>(null) }

val registerState by viewModel.loginState.collectAsStateWithLifecycle()

    LaunchedEffect(registerState) {
        if (registerState is AuthViewModel.LoginState.Success) {
            onRegisterSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("新規登録") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBackToLogin) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "戻る")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("メールアドレス") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    passwordError = if (it.length < 6) "パスワードは6文字以上必要です" else null
                },
                label = { Text("パスワード (6文字以上)") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                isError = passwordError != null
            )
            if (passwordError != null && password.isNotEmpty()) {
                Text(passwordError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("パスワード（確認用）") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                isError = password != confirmPassword && confirmPassword.isNotEmpty()
            )
            if (password != confirmPassword && confirmPassword.isNotEmpty()) {
                Text("パスワードが一致しません", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (password.length >= 6 && password == confirmPassword) {
                        viewModel.createUserWithEmailPassword(email, password)
                    } else if (password.length < 6) {
                        passwordError = "パスワードは6文字以上必要です"
                    } else {
                        // Show general mismatch error perhaps via ViewModel state if needed
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = registerState !is AuthViewModel.LoginState.Loading && email.isNotEmpty() && password.isNotEmpty() && confirmPassword.isNotEmpty() && password == confirmPassword && password.length >= 6
            ) {
                Text("登録する")
            }
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onGoogleSignInClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = registerState !is AuthViewModel.LoginState.Loading,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_google_logo),
                    contentDescription = "Google Logo",
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                    tint = Color.Unspecified
                )
                Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                Text("Googleで登録/ログイン", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (registerState is AuthViewModel.LoginState.Loading) {
                CircularProgressIndicator()
            }

            if (registerState is AuthViewModel.LoginState.Error) {
                Text(
                    text = (registerState as AuthViewModel.LoginState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RegisterScreenPreview() {
    RegisterScreen(
        onRegisterSuccess = {},
        onNavigateBackToLogin = {},
        onGoogleSignInClick = {}
    )
}

/*
class AuthViewModel_Preview_Placeholder_Register : AuthViewModel(FirebaseAuth.getInstance()) {
    // Override states/methods if needed for preview scenarios
}
*/ 