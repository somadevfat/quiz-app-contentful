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
import com.example.contentful_javasilver.ui.RegisterScreen
import com.example.contentful_javasilver.ui.theme.ContentfulJavasilverTheme
import com.example.contentful_javasilver.viewmodels.AuthViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RegisterFragment : Fragment() {

    private val authViewModel: AuthViewModel by viewModels()

    // ActivityResultLauncher for Google Sign-In
    private lateinit var googleSignInLauncher: ActivityResultLauncher<android.content.Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize the launcher in onCreate (same as LoginFragment)
        googleSignInLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(ApiException::class.java)!!
                    Log.d("RegisterFragment", "Google Sign In successful, attempting Firebase Auth with token: ${account.idToken}")
                    authViewModel.firebaseAuthWithGoogle(account.idToken!!) // Register/Sign in with Google
                } catch (e: ApiException) {
                    Log.w("RegisterFragment", "Google sign in failed", e)
                    authViewModel.setLoginError("Google 登録/ログインに失敗しました: ${e.statusCode}")
                }
            } else {
                Log.w("RegisterFragment", "Google sign in cancelled or failed with code: ${result.resultCode}")
                authViewModel.setLoginError("Google 登録/ログインがキャンセルされました")
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
                    RegisterScreen(
                        viewModel = authViewModel,
                        onRegisterSuccess = {
                            val navOptions = NavOptions.Builder()
                                .setPopUpTo(R.id.nav_graph, true)
                                .build()
                            findNavController().navigate(R.id.action_registerFragment_to_loadingFragment, null, navOptions)
                        },
                        onNavigateBackToLogin = {
                            findNavController().popBackStack()
                        },
                        // Pass the Google Sign-In Intent launch logic
                        onGoogleSignInClick = {
                            Log.d("RegisterFragment", "Google Sign In button clicked")
                            val signInIntent = authViewModel.getGoogleSignInIntent()
                            googleSignInLauncher.launch(signInIntent)
                        }
                    )
                }
            }
        }
    }
} 