package com.example.contentful_javasilver;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock; // Import SystemClock
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
// Removed ProgressBar import
import android.widget.TextView; // Import TextView
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.os.BundleCompat;
import androidx.fragment.app.Fragment;
// Removed LifecycleOwner import as lifecycleScope is removed
import androidx.navigation.NavController; // Import NavController
import androidx.navigation.Navigation;
import androidx.lifecycle.ViewModelProvider; // Add ViewModelProvider import
// Removed lifecycleScope import
import dagger.hilt.android.AndroidEntryPoint; // Add Hilt entry point annotation

import com.contentful.java.cda.CDAEntry;
import com.example.contentful_javasilver.data.QuizDao; // Added explicit QuizDao import
import com.example.contentful_javasilver.data.QuizDatabase;
import com.example.contentful_javasilver.data.QuizEntity;
import com.example.contentful_javasilver.utils.SecurePreferences;
// Removed QuizViewModel import as it wasn't used directly here
// Removed DatabaseTransaction import as we use Room's runInTransaction directly
import com.example.contentful_javasilver.viewmodels.LoadingViewModel; // Import LoadingViewModel

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService; // Added ExecutorService import
import java.util.concurrent.Executors;   // Added Executors import
// Import SharedPreferences for local version storage
import android.content.SharedPreferences;

// Removed kotlin.Unit import
// Removed kotlinx.coroutines imports

@AndroidEntryPoint // Add this annotation for Hilt
public class LoadingFragment extends Fragment {

    private static final String TAG = "LoadingFragment";
    private static final long MIN_DISPLAY_TIME_MS = 3000; // 3 seconds minimum display time
    // Key for storing local data version in SharedPreferences
    private static final String KEY_LOCAL_DATA_VERSION = "local_data_version";
    private static final int DEFAULT_DATA_VERSION = -1; // Default value if no version is stored

    // Removed progressBar and statusText fields
    private final Handler handler = new Handler(Looper.getMainLooper()); // Handler for UI updates
    private long startTimeMs; // To track start time
    private boolean dataLoadComplete = false; // Flag to track data loading status - Can be removed if ViewModel fully controls state
    private TextView loadingStatusText; // Add TextView field

    private AsyncHelperCoroutines asyncHelper; // Keep this for Contentful fetching
    private QuizDatabase database;
    private ExecutorService databaseExecutor; // Executor for background DB tasks
    private ContentfulGetApi contentfulApi; // Make ContentfulApi accessible in methods
    private LoadingViewModel viewModel; // Add ViewModel field
    private String receivedRandomQuizId = null; // Store the random ID when received

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_loading, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize ViewModel using Hilt
        viewModel = new ViewModelProvider(this).get(LoadingViewModel.class);

        startTimeMs = SystemClock.elapsedRealtime(); // Record start time
        loadingStatusText = view.findViewById(R.id.loadingStatusText); // Initialize TextView

        // Initialize Database and Executor (Keep for now, might be moved to ViewModel/Repo later)
        database = QuizDatabase.getDatabase(requireContext());
        databaseExecutor = Executors.newSingleThreadExecutor(); // Create a single-threaded executor

        // APIキーの取得 (Keep for now)
        String apiKey = SecurePreferences.getContentfulApiKey(requireContext());
        String spaceId = SecurePreferences.getContentfulSpaceId(requireContext());

        // API初期化 (Keep for now)
        contentfulApi = new ContentfulGetApi(spaceId, apiKey); // Initialize here
        asyncHelper = new AsyncHelperCoroutines(contentfulApi);

        // データバージョンの確認とデータロードを開始 (Keep for now)
        checkVersionAndLoadData(); // This might trigger ViewModel loading later

        // Observe ViewModel State
        observeViewModel();
    }

    // Updated to use LiveData.observe
    private void observeViewModel() {
        // Observe isLoadingComplete LiveData
        viewModel.isLoadingComplete().observe(getViewLifecycleOwner(), isComplete -> {
            if (isComplete != null && isComplete) {
                // Retrieve the ID stored when randomQuizId LiveData was observed
                tryNavigateToHome(receivedRandomQuizId);
            }
        });

        // Observe randomQuizId LiveData to store the ID when it becomes available
        viewModel.getRandomQuizId().observe(getViewLifecycleOwner(), qid -> {
             if (qid != null) {
                 Log.d(TAG, "Random Quiz ID received: " + qid);
                 receivedRandomQuizId = qid; // Store the received ID
             }
        });

        // Observe loadingStatus LiveData
        viewModel.getLoadingStatus().observe(getViewLifecycleOwner(), status -> {
            if (status != null && isAdded()) {
                updateStatusText(status);
            }
        });

        // Observe errorOccurred LiveData (optional: show error message)
        viewModel.getErrorOccurred().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty() && isAdded()) {
                // Optionally show error via Toast or update UI
                 showError("ViewModel Error: " + errorMessage); // Example usage
            }
        });
    }

    // Modified tryNavigateToHome to accept quizId
    private synchronized void tryNavigateToHome(@Nullable String randomQuizId) {
        Log.d(TAG, "Attempting navigation. Random ID: " + randomQuizId);
        long elapsedTimeMs = SystemClock.elapsedRealtime() - startTimeMs;
        long remainingTimeMs = MIN_DISPLAY_TIME_MS - elapsedTimeMs;

        // ViewModel now controls the trigger via the isLoadingComplete observer.
        // We just need to handle the minimum display time.
        if (remainingTimeMs <= 0) {
            navigateToHomeInternal(randomQuizId);
        } else {
             Log.d(TAG, "Delaying navigation by " + remainingTimeMs + " ms");
             handler.postDelayed(() -> {
                  if (isAdded()) {
                     navigateToHomeInternal(randomQuizId);
                  }
             }, remainingTimeMs);
        }
    }

    // Modified navigateToHomeInternal to use Bundle instead of Safe Args
    private void navigateToHomeInternal(@Nullable String randomQuizId) {
        if (isAdded() && getView() != null) {
            Log.i(TAG, "Navigating to HomeFragment with randomQuizId: " + randomQuizId);
            NavController navController = Navigation.findNavController(requireView());

            // Create Bundle to pass arguments
            Bundle args = new Bundle();
            args.putString("randomQuizId", randomQuizId); // Key must match the argument name in nav_graph

            try {
                // Navigate using action ID and Bundle
                navController.navigate(R.id.action_loading_to_home, args);
            } catch (IllegalStateException e) {
                Log.e(TAG, "Navigation failed: Could not find NavController", e);
                showError("画面遷移に失敗しました。");
            } catch (IllegalArgumentException e) {
                // This might happen if the action ID is wrong or destination requires arguments not provided
                Log.e(TAG, "Navigation failed: Action ID invalid or arguments missing", e);
                showError("画面遷移の設定に問題があります。ActionID: action_loading_to_home");
            }
        } else {
            Log.w(TAG, "Navigation skipped: Fragment not added or view destroyed.");
        }
    }

    // --- Methods below might be refactored/removed if logic moves to ViewModel/Repository ---
    // The current implementation keeps data loading logic here, but triggers ViewModel completion.

    private void checkVersionAndLoadData() {
        viewModel.updateLoadingStatus(getString(R.string.loading_status_checking)); // Use ViewModel status update
        Log.d(TAG, "Checking data version...");

        databaseExecutor.execute(() -> {
            int latestVersion = contentfulApi.fetchDataVersion();
            Log.d(TAG, "Latest data version from Contentful: " + latestVersion);

            SharedPreferences prefs = SecurePreferences.getSecurePreferences(requireContext());
            int localVersion = prefs.getInt(KEY_LOCAL_DATA_VERSION, DEFAULT_DATA_VERSION);
            Log.d(TAG, "Local data version: " + localVersion);

            boolean needsUpdate = latestVersion != DEFAULT_DATA_VERSION && latestVersion != localVersion;

            handler.post(() -> {
                if (!isAdded()) return;

                if (needsUpdate) {
                    Log.i(TAG, "Data version mismatch. Forcing refresh.");
                    clearDatabaseAndDownload(latestVersion);
                } else if (localVersion == DEFAULT_DATA_VERSION && latestVersion != DEFAULT_DATA_VERSION) {
                     Log.i(TAG, "First launch. Forcing refresh.");
                    clearDatabaseAndDownload(latestVersion);
                } else if (latestVersion == DEFAULT_DATA_VERSION) {
                     Log.w(TAG, "Could not fetch latest version. Checking existing data.");
                     checkExistingDataCount();
                 } else {
                    Log.d(TAG, "Data version up-to-date. Checking local data.");
                    checkExistingDataCount();
                }
            });
        });
    }

    private void clearDatabaseAndDownload(int newVersion) {
        viewModel.updateLoadingStatus("データベース初期化中...");
        Log.d(TAG, "Clearing database, version: " + newVersion);
        databaseExecutor.execute(() -> {
            try {
                database.quizDao().deleteAll();
                Log.i(TAG, "Database cleared.");

                SharedPreferences prefs = SecurePreferences.getSecurePreferences(requireContext());
                prefs.edit().putInt(KEY_LOCAL_DATA_VERSION, newVersion).apply();
                Log.i(TAG, "Local version updated: " + newVersion);

                handler.post(() -> {
                    if (isAdded()) {
                        viewModel.updateLoadingStatus(getString(R.string.loading_status_downloading));
                         downloadAllData();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Failed to clear database", e);
                handler.post(() -> {
                    if (isAdded()) {
                        viewModel.setLoadingError("DBクリア失敗: " + e.getMessage()); // Report error to ViewModel
                        // showError("DBクリア失敗: " + e.getMessage()); // Optionally still show toast
                        // checkExistingDataCount(); // Decide if we should proceed or stop on error
                    }
                });
            }
        });
    }

    // Check existing data count - triggers ViewModel completion if data exists
    private void checkExistingDataCount() {
        viewModel.updateLoadingStatus("既存データを確認中...");
        Log.d(TAG, "Checking existing data count...");

        databaseExecutor.execute(() -> {
            try {
                int quizCount = database.quizDao().getQuizCountSync();
                handler.post(() -> {
                    if (isAdded()) {
                        if (quizCount > 0) {
                            Log.d(TAG, "既存データあり (" + quizCount + "件)。ロード完了通知。");
                             viewModel.setLoadingComplete(); // Signal completion
                        } else {
                            Log.d(TAG, "データなし。ダウンロード開始。");
                            viewModel.updateLoadingStatus(getString(R.string.loading_status_downloading));
                            downloadAllData();
                        }
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "DBカウント取得失敗", e);
                handler.post(() -> {
                    if (isAdded()) {
                         viewModel.setLoadingError("データベース確認エラー");
                         // showError("DB確認エラー: " + e.getMessage());
                    }
                });
            }
        });
    }

    // Download data - triggers ViewModel completion on success
    private void downloadAllData() {
        viewModel.updateLoadingStatus(getString(R.string.loading_status_downloading)); // Update status via ViewModel

        asyncHelper.fetchAllEntriesAsync(
                "javaSilverQ",
                (progress, status) -> {
                    // Update status via ViewModel (runs on main thread due to handler.post in implementation)
                    viewModel.updateLoadingStatus(status);
                    return null; // Return null for Kotlin lambda expecting Unit
                },
                entries -> {
                    Log.d(TAG, entries.size() + "件ダウンロード完了。保存中...");
                    viewModel.updateLoadingStatus(getString(R.string.loading_status_saving));

                    List<QuizEntity> entities = convertToQuizEntities(entries);
                    saveToDatabase(entities);

                    return null; // Return null for Kotlin lambda expecting Unit
                },
                error -> {
                    Log.e(TAG, "ダウンロード失敗: " + error);
                    viewModel.setLoadingError("データダウンロード失敗: " + error);
                    // showError(error);
                    return null; // Return null for Kotlin lambda expecting Unit
                }
        );
    }

    @SuppressWarnings("unchecked")
    private List<QuizEntity> convertToQuizEntities(List<? extends CDAEntry> entries) {
        List<QuizEntity> entities = new ArrayList<>();
        for (CDAEntry entry : entries) {
            try {
                String qid = getFieldAsString(entry, "qid");
                // Null check for qid is important
                if (qid == null || qid.isEmpty()) {
                    Log.w(TAG, "Skipping entry due to missing or empty qid: " + entry.id());
                    continue; // Skip this entry if qid is missing
                }

                // Attempt to get List<Integer> for answer from Contentful field "answer"
                List<Integer> answerList = null;
                Object answerField = entry.getField("answer");
                if (answerField instanceof List) {
                    // Check if the list contains numbers (likely Doubles from JSON)
                    List<?> rawList = (List<?>) answerField;
                    if (!rawList.isEmpty() && rawList.get(0) instanceof Number) {
                        answerList = new ArrayList<>();
                        for (Object item : rawList) {
                            if (item instanceof Number) {
                                answerList.add(((Number) item).intValue());
                            }
                        }
                    }
                }

                // Assuming Contentful fields match QuizEntity constructor parameters
                entities.add(new QuizEntity(
                    qid, // @NonNull String qid
                    getFieldAsString(entry, "chapter"),          // String chapter
                    getFieldAsString(entry, "category"),         // String category
                    getFieldAsString(entry, "questionCategory"), // String questionCategory
                    getFieldAsString(entry, "difficulty"),       // String difficulty
                    getFieldAsString(entry, "code"),             // String code
                    getFieldAsString(entry, "questionText"),     // String questionText
                    (List<String>) entry.getField("choices"),       // List<String> choices
                    answerList,                                     // List<Integer> answer (converted above)
                    getFieldAsString(entry, "explanation"),      // String explanation
                    (entry.getField("isBookmarked") instanceof Boolean) ? (Boolean) entry.getField("isBookmarked") : false // boolean isBookmarked
                ));
            } catch (ClassCastException cce) {
                Log.e(TAG, "Type casting error converting entry: " + entry.id() + ". Check Contentful field types.", cce);
                // Skip entry on type mismatch
            } catch (Exception e) {
                Log.e(TAG, "Error converting entry: " + entry.id(), e);
                // Decide if one bad entry should stop the whole process or just be skipped
            }
        }
        return entities;
    }

    private String getFieldAsString(CDAEntry entry, String fieldName) {
        // Helper to safely get string fields, returning null if absent or not a string
        Object field = entry.getField(fieldName);
        return (field instanceof String) ? (String) field : null;
    }

    // Save to DB - triggers ViewModel completion on success
    private void saveToDatabase(List<QuizEntity> entities) {
        databaseExecutor.execute(() -> {
            try {
                database.runInTransaction(() -> {
                    database.quizDao().insertAll(entities);
                    return null;
                });
                Log.i(TAG, entities.size() + "件のデータをデータベースに保存しました");
                handler.post(() -> {
                    if (isAdded()) {
                        viewModel.setLoadingComplete(); // Signal completion
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "データベースへの保存に失敗", e);
                handler.post(() -> {
                    if (isAdded()) {
                        viewModel.setLoadingError("データ保存エラー: " + e.getMessage());
                        // showError("データ保存エラー: " + e.getMessage());
                    }
                });
            }
        });
    }

    private void showError(String message) {
        if (isAdded() && getContext() != null) {
            Log.e(TAG, "Error displayed: " + message);
            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
            // Optionally update status text to reflect the error state
            // updateStatusText("エラーが発生しました"); // ViewModel handles status now
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up resources
        if (databaseExecutor != null && !databaseExecutor.isShutdown()) {
            databaseExecutor.shutdown();
        }
        handler.removeCallbacksAndMessages(null); // Remove any pending posts
        loadingStatusText = null; // Avoid memory leaks
    }

    // Helper method to update status text view safely
    private void updateStatusText(String status) {
        if (loadingStatusText != null && isAdded()) {
            loadingStatusText.setText(status);
             Log.d(TAG, "Status updated: " + status);
        }
    }
}
