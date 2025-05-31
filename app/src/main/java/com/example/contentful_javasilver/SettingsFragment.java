package com.example.contentful_javasilver;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import com.google.firebase.auth.FirebaseAuth;

public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "SettingsFragment";
    public static final String KEY_PREF_THEME = "theme_preference";
    public static final String KEY_PREF_NOTIFICATION = "notification_preference";
    public static final String KEY_PREF_PRIVACY_POLICY = "privacy_policy_preference";
    public static final String KEY_PREF_VERSION = "version_preference";
    public static final String KEY_PREF_LOGOUT = "logout_preference";

    private SharedPreferences sharedPreferences;
    private FirebaseAuth mAuth;

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
        mAuth = FirebaseAuth.getInstance();

        // Initialize summaries and listeners
        updateVersionSummary();
        setupPrivacyPolicyLink();
        setupLogoutPreference();
        updateThemeSummary(sharedPreferences.getString(KEY_PREF_THEME, "default"));

        // TODO: Add listener for notification switch if specific action is needed on change
        // SwitchPreferenceCompat notificationPref = findPreference(KEY_PREF_NOTIFICATION);
        // if (notificationPref != null) {
        //     // Add listener logic here
        // }
    }

    private void updateVersionSummary() {
        Preference versionPref = findPreference(KEY_PREF_VERSION);
        if (versionPref != null) {
            try {
                Context context = requireContext();
                PackageManager pm = context.getPackageManager();
                PackageInfo pInfo = pm.getPackageInfo(context.getPackageName(), 0);
                String version = pInfo.versionName;
                versionPref.setSummary(version); // Update summary directly
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "Could not get package version", e);
                versionPref.setSummary("N/A");
            }
        }
    }

    private void setupPrivacyPolicyLink() {
        Preference privacyPolicyPref = findPreference(KEY_PREF_PRIVACY_POLICY);
        if (privacyPolicyPref != null) {
            privacyPolicyPref.setOnPreferenceClickListener(preference -> {
                try {
                    NavController navController = NavHostFragment.findNavController(SettingsFragment.this);
                    // Ensure this action ID exists in your nav_graph.xml
                    navController.navigate(R.id.action_settingsFragment_to_privacyPolicyFragment);
                    return true;
                } catch (Exception e) {
                    Log.e(TAG, "Navigation to Privacy Policy failed", e);
                    // Optionally show a toast or message to the user
                    return false;
                }
            });
        }
    }

    private void setupLogoutPreference() {
        Preference logoutPref = findPreference(KEY_PREF_LOGOUT);
        if (logoutPref != null) {
            logoutPref.setOnPreferenceClickListener(preference -> {
                // Perform logout
                mAuth.signOut();

                // Navigate to Login screen
                try {
                    NavController navController = NavHostFragment.findNavController(SettingsFragment.this);
                    // Use the correct action ID from your nav_graph.xml
                    navController.navigate(R.id.action_settingsFragment_to_loginFragment);
                    return true;
                } catch (Exception e) {
                    Log.e(TAG, "Navigation to Login failed after logout", e);
                    // Optionally show a toast or message to the user
                    return false;
                }
            });
        }
    }

    private void updateThemeSummary(String themeValue) {
        ListPreference themePref = findPreference(KEY_PREF_THEME);
        if (themePref != null) {
            // Update the summary to show the currently selected theme name
            themePref.setSummary(themePref.getEntry());
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String key) {
        if (key != null && key.equals(KEY_PREF_THEME)) {
            String themeValue = sharedPreferences.getString(KEY_PREF_THEME, "default");
            Log.d(TAG, "Theme preference changed to: " + themeValue);
            updateThemeSummary(themeValue);
            // Recreate the activity to apply the new theme
            requireActivity().recreate();
        }
        // Handle other preference changes if needed
        // else if (key.equals(KEY_PREF_NOTIFICATION)) { ... }
    }

    @Override
    public void onResume() {
        super.onResume();
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        // Update summary in case the value was changed externally (less likely but good practice)
        updateThemeSummary(sharedPreferences.getString(KEY_PREF_THEME, "default"));
    }

    @Override
    public void onPause() {
        super.onPause();
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    // Remove onCreateView and onViewCreated as PreferenceFragmentCompat handles the view creation
}
