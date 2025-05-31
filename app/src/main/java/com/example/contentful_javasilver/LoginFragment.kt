package com.example.contentful_javasilver

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.contentful_javasilver.ui.LoginScreen
import com.example.contentful_javasilver.ui.theme.ContentfulJavasilverTheme
import com.example.contentful_javasilver.viewmodels.AuthViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private val authViewModel: AuthViewModel by viewModels()

    // ActivityResultLauncher for Google Sign-In
    private lateinit var googleSignInLauncher: ActivityResultLauncher<android.content.Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize the launcher in onCreate
        googleSignInLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult() // Standard contract
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // Handle the successful sign-in result
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    // Google Sign In was successful, authenticate with Firebase
                    val account = task.getResult(ApiException::class.java)!!
                    Log.d("LoginFragment", "Google Sign In successful, attempting Firebase Auth with token: ${account.idToken}")
                    authViewModel.firebaseAuthWithGoogle(account.idToken!!)
                } catch (e: ApiException) {
                    // Google Sign In failed, update UI appropriately
                    Log.w("LoginFragment", "Google sign in failed", e)
                    // Optionally show an error message to the user
                    authViewModel.setLoginError("Google ログインに失敗しました: ${e.statusCode}") // Add this method to ViewModel if needed
                }
            } else {
                // Handle sign-in cancellation or error
                Log.w("LoginFragment", "Google sign in cancelled or failed with code: ${result.resultCode}")
                 authViewModel.setLoginError("Google ログインがキャンセルされました")
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                ContentfulJavasilverTheme {
                    LoginScreen(
                        viewModel = authViewModel,
                        onLoginSuccess = {
                            val navOptions = NavOptions.Builder()
                                .setPopUpTo(R.id.nav_graph, true)
                                .build()
                            findNavController().navigate(R.id.action_loginFragment_to_loadingFragment, null, navOptions)
                        },
                        onNavigateToRegister = {
                            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
                        },
                        // Pass the Google Sign-In Intent launch logic
                        onGoogleSignInClick = {
                            Log.d("LoginFragment", "Google Sign In button clicked")
                            val signInIntent = authViewModel.getGoogleSignInIntent()
                            googleSignInLauncher.launch(signInIntent)
                        }
                    )
                }
            }
        }
    }
} 