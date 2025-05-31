package com.example.contentful_javasilver;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class StartFragment extends Fragment {

    private FirebaseAuth mAuth;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_start, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        NavController navController = Navigation.findNavController(view);
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            Log.d("StartFragment", "User already logged in. Navigating to HomeFragment.");
            try {
                navController.navigate(R.id.action_start_to_home);
            } catch (IllegalArgumentException e) {
                Log.e("StartFragment", "Navigation to home failed after checking login state.", e);
                setupButtons(view, navController);
            }
        } else {
            Log.d("StartFragment", "User not logged in. Setting up buttons.");
            setupButtons(view, navController);
        }
    }

    private void setupButtons(@NonNull View view, @NonNull NavController navController) {
        Button loginButton = view.findViewById(R.id.loginButton);
        Button skipLoginButton = view.findViewById(R.id.skipLoginButton);

        loginButton.setOnClickListener(v -> {
            try {
                navController.navigate(R.id.action_start_to_login);
            } catch (IllegalArgumentException e) {
                Log.e("StartFragment", "Navigation to login failed", e);
            }
        });

        skipLoginButton.setOnClickListener(v -> {
            try {
                navController.navigate(R.id.action_start_to_loading);
            } catch (IllegalArgumentException e) {
                 Log.e("StartFragment", "Navigation to loading (skip login) failed", e);
            }
        });
    }
}
