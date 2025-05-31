package com.example.contentful_javasilver.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun StartScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToHome: () -> Unit // Assuming Home screen for non-logged-in users
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("ようこそ！", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(64.dp))

        Button(
            onClick = onNavigateToLogin,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("ログイン / 新規登録")
        }
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onNavigateToHome,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("ログインせずに利用する")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun StartScreenPreview() {
    StartScreen(onNavigateToLogin = {}, onNavigateToHome = {})
} 