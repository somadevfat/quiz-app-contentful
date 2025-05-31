package com.example.contentful_javasilver;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager; // Use androidx.preference if available, but this works too
import android.util.Log;
import android.view.MenuItem; // Import MenuItem
import android.view.View;
import android.view.Menu; // Import Menu
import android.view.ViewGroup; // <<< Add this import
import android.graphics.PorterDuff; // Import PorterDuff for tinting
import androidx.core.content.ContextCompat; // Import ContextCompat
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets; // Correct import for Insets
import androidx.core.graphics.drawable.DrawableCompat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.NavGraph;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView; // Import NavigationView
import android.widget.TextView; // Import TextView
import com.example.contentful_javasilver.data.QuizDao;
import com.example.contentful_javasilver.data.QuizDatabase;
import com.example.contentful_javasilver.data.QuizEntity;
import com.example.contentful_javasilver.databinding.ActivityMainBinding;
import com.example.contentful_javasilver.utils.SecurePreferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet; // Import HashSet
import java.util.List;
import java.util.Set; // Import Set
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.graphics.drawable.Drawable;
import android.content.Intent;
import android.net.Uri;
import dagger.hilt.android.AndroidEntryPoint; // Import Hilt annotation

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private ActivityMainBinding binding;
    private NavController navController;
    private AppBarConfiguration appBarConfiguration; // Make it a field
    private DrawerLayout drawerLayout; // Add DrawerLayout field

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply theme before super.onCreate() and setContentView()
        applyAppTheme();

        // --- Enable Edge-to-Edge display --- (Moved before super.onCreate)
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // 背景アニメーションの適用
        View mainContainer = binding.mainContentContainer;
        Animation gradientAnimation = AnimationUtils.loadAnimation(this, R.anim.gradient_animation);
        mainContainer.startAnimation(gradientAnimation);

        // Apply insets listener to AppBarLayout to handle status bar overlap
        final AppBarLayout appBarLayout = binding.appBarLayout; // Target AppBarLayout
        final int originalPaddingTop = appBarLayout.getPaddingTop();
        final int originalPaddingLeft = appBarLayout.getPaddingLeft();
        final int originalPaddingRight = appBarLayout.getPaddingRight();
        final int originalPaddingBottom = appBarLayout.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(appBarLayout, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            // Apply the top inset as padding to the AppBarLayout
            v.setPadding(
                originalPaddingLeft + insets.left,
                originalPaddingTop + insets.top,
                originalPaddingRight + insets.right,
                originalPaddingBottom
            );
            // Return the insets for propagation
            return windowInsets;
        });

        // Apply insets listener to NavigationView to handle system bars overlap
        final int originalNavPaddingLeft = binding.navigationView.getPaddingLeft();
        final int originalNavPaddingTop = binding.navigationView.getPaddingTop();
        final int originalNavPaddingRight = binding.navigationView.getPaddingRight();
        final int originalNavPaddingBottom = binding.navigationView.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(binding.navigationView, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            // Apply system bar insets as padding to the NavigationView
            v.setPadding(
                originalNavPaddingLeft + insets.left,
                originalNavPaddingTop + insets.top,
                originalNavPaddingRight + insets.right,
                originalNavPaddingBottom + insets.bottom
            );
            return windowInsets;
        });

        // Apply insets listener to Bottom Navigation CardView to handle navigation bar overlap
        final View bottomNavCard = binding.bottomNavCard; // Target the CardView
        // Store original bottom padding/margin if needed, assuming 0 for now
        // If you have margin set in XML, get it here: e.g., ((ViewGroup.MarginLayoutParams) bottomNavCard.getLayoutParams()).bottomMargin;
        final int originalBottomNavMarginBottom = 0; 

        ViewCompat.setOnApplyWindowInsetsListener(bottomNavCard, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars()); // Use navigationBars() type
            // Apply the bottom inset as bottom margin to the CardView
            if (v.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                params.bottomMargin = originalBottomNavMarginBottom + insets.bottom;
                v.setLayoutParams(params);
            } else {
                // Fallback or log error if layout params are not MarginLayoutParams
                Log.w("MainActivity", "BottomNavCard LayoutParams are not MarginLayoutParams, cannot apply bottom margin.");
            }
            // Consume only the navigation bar insets, let others pass through
            // This prevents interfering with other inset handling (like status bar)
             return new WindowInsetsCompat.Builder(windowInsets)
                 .setInsets(WindowInsetsCompat.Type.navigationBars(), Insets.of(0, 0, 0, 0)) // Consume bottom inset
                 .build();
        });

        drawerLayout = binding.drawerLayout; // Get DrawerLayout
        NavigationView navigationView = binding.navigationView; // Re-enabled declaration

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        navController = navHostFragment.getNavController();

        // Toolbar setup (variable declaration removed as it's accessed via binding)
        setSupportActionBar(binding.toolbar);

        // Define top-level destinations based on Drawer menu and NavGraph fragment IDs
        Set<Integer> topLevelDestinations = new HashSet<>(Arrays.asList(
                R.id.homeFragment,         // Home Fragment ID
                // R.id.problemListFragment, // Problem List Fragment ID - Removed from top-level
                R.id.navigation_bookmark   // Bookmark Fragment ID (using existing ID)
                // R.id.settingsFragment is removed to show back button instead of drawer icon
        ));
        appBarConfiguration = new AppBarConfiguration.Builder(topLevelDestinations)
                .setOpenableLayout(drawerLayout) // Link DrawerLayout
                .build();

        // Setup ActionBar with NavController and DrawerLayout
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        // Setup NavigationView listener
        navigationView.setNavigationItemSelectedListener(this);

        // Disable default title setting by NavigationUI
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // --- Listener for hiding/showing UI elements and setting custom title --- (Modified)
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            int destId = destination.getId();
            String destLabel = destination.getLabel() != null ? destination.getLabel().toString() : "No Label";
            Log.d("MainActivity", "Navigating to: " + destLabel + " (" + destId + ")");

            TextView customTitle = binding.toolbarTitleCustom; // Get custom title TextView

            // Check if the current destination is a top-level destination
            boolean isTopLevelDestination = appBarConfiguration.getTopLevelDestinations().contains(destId);

            // Show/Hide Toolbar and Bottom Nav based on destination
            if (destId == R.id.startFragment || destId == R.id.loadingFragment) {
                Log.d("MainActivity", "Hiding UI for " + destLabel);
                binding.appBarLayout.setVisibility(View.GONE);
                customTitle.setVisibility(View.GONE); // Hide custom title as well
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED); // Lock drawer
                binding.bottomNavCard.setVisibility(View.GONE); // Hide bottom navigation
            } else {
                Log.d("MainActivity", "Showing Toolbar for " + destLabel);
                binding.appBarLayout.setVisibility(View.VISIBLE);
                customTitle.setVisibility(View.VISIBLE); // Show custom title
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED); // Unlock drawer

                // Hide bottom nav specifically for QuizFragment
                if (destId == R.id.quizFragment) {
                    Log.d("MainActivity", "Hiding BottomNav for QuizFragment");
                    binding.bottomNavCard.setVisibility(View.GONE);
                } else {
                    Log.d("MainActivity", "Showing BottomNav for " + destLabel);
                    binding.bottomNavCard.setVisibility(View.VISIBLE);
                }

                // Set the text of the custom title TextView
                if (destId == R.id.problemListFragment) {
                    // 問題一覧フラグメントのとき専用タイトル設定
                    customTitle.setText("");
                    customTitle.setVisibility(View.GONE); // Hide title for problem list
                    Log.d("MainActivity", "Hiding custom title for Problem List");
                } else if (destination.getLabel() != null) {
                    customTitle.setText(destination.getLabel());
                    customTitle.setVisibility(View.VISIBLE); // Show for other destinations
                    Log.d("MainActivity", "Setting custom title to: " + destination.getLabel());
                } else {
                    customTitle.setText(""); // Clear title if no label
                    customTitle.setVisibility(View.GONE); // Hide empty title
                    Log.d("MainActivity", "Setting empty custom title");
                }

                // Update toggle buttons based on destination
                updateToggleState(destId);

                // リーン画面のとき設定ボタンの表示を更新
                invalidateOptionsMenu();
            }
        });
        
        // Setup toggle buttons
        setupToggleButtons();

        // --- Removed BottomNavigationView listeners --- 

        String apiKey = BuildConfig.CONTENTFUL_ACCESS_TOKEN;
        String spaceId = BuildConfig.CONTENTFUL_SPACE_ID;
        SecurePreferences.initializeSecureKeys(getApplicationContext(), apiKey, spaceId);

        // Restore handleIntent call at the end of onCreate logic
        // Check if the app was launched from a Contentful link
        handleIntent(getIntent());
    }

    // ★ Restore empty handleIntent method definition ★
    private void handleIntent(Intent intent) {
        // TODO: Implement logic to handle intents (e.g., deep links) if needed.
        // Check if the intent has data (like a deep link)
        if (intent != null && intent.getData() != null) {
            Uri data = intent.getData();
            Log.d("MainActivity", "Handling intent data: " + data.toString());
            // Example: Navigate based on deep link path
            // List<String> pathSegments = data.getPathSegments();
            // if (pathSegments != null && pathSegments.size() > 0) {
            //     String firstSegment = pathSegments.get(0);
            //     if ("quiz".equals(firstSegment) && pathSegments.size() > 1) {
            //         String qid = pathSegments.get(1);
            //         navigateToQuizWithQid(qid);
            //     }
            // }
        }
    }

    // --- Inflate Toolbar Menu and Apply Tint --- (Added/Modified)
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu); // Corrected menu file name
        
        // リーン画面のときは設定ボタンを表示しない
        if (navController.getCurrentDestination() != null && 
            navController.getCurrentDestination().getId() == R.id.problemListFragment) {
            menu.findItem(R.id.action_settings).setVisible(false);
        } else {
            // Tint the settings icon white
            MenuItem settingsItem = menu.findItem(R.id.action_settings);
            if (settingsItem != null && settingsItem.getIcon() != null) {
                settingsItem.getIcon().setColorFilter(ContextCompat.getColor(this, android.R.color.white), PorterDuff.Mode.SRC_ATOP);
            }
        }
        return true;
    }

    // --- Handle Toolbar Item Selection --- (Optional, add if needed)
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            // Navigate to settings fragment
            navController.navigate(R.id.settingsFragment); // Ensure ID matches nav_graph
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // --- Handle Navigation Item Selection --- (Added)
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        // Close the drawer
        drawerLayout.closeDrawer(GravityCompat.START);

        // Handle navigation using NavController
        // Use NavOptions to pop up to the start destination (Home) to avoid deep back stack
        NavOptions navOptions = new NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setPopUpTo(R.id.nav_home, false) // Pop up to home, don't pop home itself
                .build();

        try {
            // Use NavigationUI.onNavDestinationSelected for standard behavior
            // or handle navigation manually for custom logic
            boolean handled = NavigationUI.onNavDestinationSelected(item, navController);
            if (handled) {
                // Standard navigation handled, just close the drawer
            } else {
                // Handle custom navigation or non-standard menu items if needed
                if (itemId == R.id.nav_home) { // Example: explicit handling for Home
                    navController.navigate(R.id.homeFragment, null, navOptions);
                } else if (itemId == R.id.nav_problem_list) { // Example: explicit handling for Problem List
                     navController.navigate(R.id.problemListFragment, null, navOptions);
                 } else if (itemId == R.id.nav_history) { // <<<--- Added this case
                     navController.navigate(R.id.navigation_history, null, navOptions);
                 } else if (itemId == R.id.navigation_bookmark) {
                     navController.navigate(R.id.navigation_bookmark, null, navOptions);
                 } else if (itemId == R.id.nav_settings) {
                    navController.navigate(R.id.settingsFragment, null, navOptions);
                 }
                // Add other custom handling here if NavigationUI doesn't cover it
            }
        } catch (IllegalArgumentException e) {
            Log.e("MainActivity", "Failed to navigate to destination ID from drawer: " + itemId, e);
            // Maybe handle specific cases like settings separately if they are not direct destinations
            if (itemId == R.id.nav_settings) {
                // Navigate to SettingsFragment (assuming its ID in nav_graph is settingsFragment)
                try {
                     navController.navigate(R.id.settingsFragment, null, navOptions);
                     return true;
                } catch (IllegalArgumentException ex) {
                     Log.e("MainActivity", "Failed to navigate to settingsFragment", ex);
                }
            }
            return false; // Indicate navigation failed or wasn't handled
        }
        return true; // Event handled
    }

    // --- Handle ActionBar Up button --- (Modified)
    @Override
    public boolean onSupportNavigateUp() {
        Log.d("MainActivity", "onSupportNavigateUp called.");
        // Let NavigationUI handle navigating Up or opening the drawer
        return NavigationUI.navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp();
    }

    // --- Handle Back button press to close drawer --- (Added)
    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private void applyAppTheme() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String themePref = prefs.getString(SettingsFragment.KEY_PREF_THEME, "default");
        int themeResId;
        switch (themePref) {
            case "terracotta":
                themeResId = R.style.Theme_ContentfulJavasilver_Terracotta;
                break;
            case "forest_green":
                themeResId = R.style.Theme_ContentfulJavasilver_ForestGreen;
                break;
            case "indigo":
                themeResId = R.style.Theme_ContentfulJavasilver_Indigo;
                break;
            case "slate":
                themeResId = R.style.Theme_ContentfulJavasilver_Slate;
                break;
            case "ocean_blue":
                themeResId = R.style.Theme_ContentfulJavasilver_OceanBlue;
                break;
            case "mint_green":
                themeResId = R.style.Theme_ContentfulJavasilver_MintGreen;
                break;
            case "default": // Fallthrough for default case
            default:
                themeResId = R.style.Theme_ContentfulJavasilver; // Base theme
                break;
        }
        Log.d("MainActivity", "Applying theme: " + themePref + " (Res ID: " + themeResId + ")");
        setTheme(themeResId);
    }

    // Helper method to set up toggle buttons
    private void setupToggleButtons() {
        binding.homeToggle.setOnClickListener(v -> {
            // Navigate to home if not already there
            if (navController.getCurrentDestination().getId() != R.id.homeFragment) {
                navController.navigate(R.id.homeFragment);
            }
        });
        
        binding.learnToggle.setOnClickListener(v -> {
            // Navigate to problem list if not already there
            if (navController.getCurrentDestination().getId() != R.id.problemListFragment) {
                navController.navigate(R.id.problemListFragment);
            }
        });
    }
    
    // Helper method to update toggle buttons based on current destination
    private void updateToggleState(int destinationId) {
        if (destinationId == R.id.homeFragment) {
            // Home screen is active
            binding.homeToggle.setChecked(true);
            binding.learnToggle.setChecked(false);
        } else if (destinationId == R.id.problemListFragment ||
                   destinationId == R.id.chapterFragment ||
                   destinationId == R.id.quizFragment) {
            // Learning-related screens are active
            binding.homeToggle.setChecked(false);
            binding.learnToggle.setChecked(true);
        }
    }
}
