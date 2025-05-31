package com.example.contentful_javasilver.viewmodels

import android.app.Application
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.example.contentful_javasilver.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

// Hiltを使ってFirebaseAuthをインジェクト
@HiltViewModel
open class AuthViewModel @Inject constructor(
    private val app: Application,
    private val auth: FirebaseAuth
) : ViewModel() {

    // ログイン状態 (例: Loading, Success, Error) を管理するStateFlow
    // sealed interface などで状態を定義するとより良い
    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState

    // 現在のログインユーザーを取得 (起動時などに確認)
    val currentUser = auth.currentUser // 同期的だが、リスナーを使う方が推奨される場合も

    // GoogleSignInClient (Lazily initialized)
    private val googleSignInClient: GoogleSignInClient by lazy {
        // Configure Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            // Request ID token. ID token is necessary for Firebase Auth.
            // Use your server's client ID generated for your web app.
            // This is typically found in the google-services.json file downloaded from Firebase.
            .requestIdToken(app.getString(R.string.default_web_client_id)) // Get from strings.xml
            .requestEmail()
            .build()
        GoogleSignIn.getClient(app, gso)
    }

    init {
        // TODO: ログイン状態の変化を監視するリスナーを設定する
        // auth.addAuthStateListener { ... }
    }

    // Method for the Fragment to get the Google Sign-In Intent
    fun getGoogleSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }

    // --- メール/パスワード ログイン ---
    fun signInWithEmailPassword(email: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                auth.signInWithEmailAndPassword(email, password).await() // kotlinx-coroutines-play-services が必要
                _loginState.value = LoginState.Success
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e.localizedMessage ?: "ログインエラーが発生しました")
            }
        }
    }

    // --- メール/パスワード 新規登録 ---
    fun createUserWithEmailPassword(email: String, password: String) {
         viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                auth.createUserWithEmailAndPassword(email, password).await() // kotlinx-coroutines-play-services が必要
                // 新規登録成功時は自動的にログイン状態になる
                _loginState.value = LoginState.Success
            } catch (e: Exception) {
                 _loginState.value = LoginState.Error(e.localizedMessage ?: "新規登録エラーが発生しました")
            }
        }
    }

    // --- Google ログイン ---
    // Renamed to clarify it handles the result (token)
    fun firebaseAuthWithGoogle(idToken: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                auth.signInWithCredential(credential).await()
                _loginState.value = LoginState.Success
            } catch (e: Exception) {
                _loginState.value = LoginState.Error("Google ログインエラー: ${e.localizedMessage}")
            }
        }
    }

    // --- ログアウト ---
    fun signOut() {
        viewModelScope.launch { // Launch coroutine for sign out
            googleSignInClient.signOut().await() // Sign out from Google also
            auth.signOut()
            _loginState.value = LoginState.Idle
        }
    }

    // --- エラー状態設定用メソッド ---
    fun setLoginError(errorMessage: String) {
        _loginState.value = LoginState.Error(errorMessage)
    }

    // ログイン状態を表すクラス (より詳細な状態を追加可能)
    sealed class LoginState {
        object Idle : LoginState()
        object Loading : LoginState()
        object Success : LoginState()
        data class Error(val message: String) : LoginState()
    }
}

// signInWithEmailAndPassword().await() などを使うために必要
// app/build.gradle の dependencies に追加:
// implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3' // Use appropriate version aligned with coroutines