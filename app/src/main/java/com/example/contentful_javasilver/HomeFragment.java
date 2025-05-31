package com.example.contentful_javasilver;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.contentful_javasilver.databinding.FragmentHomeBinding;
import com.example.contentful_javasilver.viewmodels.QuizViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Arrays;

/**
 * ホーム画面のフラグメント
 * - メインアクティビティからナビゲーションされる画面
 * - ランダム問題とチャプター選択への選択肢を提供
 */
public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment"; // TAG for logging
    private FragmentHomeBinding binding;
    private QuizViewModel quizViewModel; // Add ViewModel instance

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        quizViewModel = new ViewModelProvider(requireActivity()).get(QuizViewModel.class);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated");
        final NavController navController = Navigation.findNavController(view);

        // ViewModel を取得
        quizViewModel = new ViewModelProvider(requireActivity()).get(QuizViewModel.class);

        // ★★★ ブックマーク同期処理を追加 ★★★
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            Log.d(TAG, "User is logged in, attempting to sync bookmarks.");
            quizViewModel.syncBookmarksFromFirestore();
        } else {
            Log.d(TAG, "User is not logged in, skipping bookmark sync.");
            // オプション: ログインしていない場合、ローカルのブックマークをリセットするなどの処理も可能
        }
        // ★★★ 同期処理ここまで ★★★

        // Retrieve arguments
        HomeFragmentArgs args = HomeFragmentArgs.fromBundle(getArguments());
        String randomQuizId = args.getRandomQuizId();
        if (randomQuizId != null) {
            Log.i(TAG, "Received randomQuizId from LoadingFragment: " + randomQuizId);
            // TODO: Use the randomQuizId, e.g., pass it to a specific quiz button
            // For example, modify the randomExamButton listener:
            // 1. Check if randomQuizId is not null.
            // 2. If not null, navigate to QuizFragment with this specific qid.
            // 3. If null, navigate in random mode as it currently does.
        }

        // 統計データをロード（ストリーク情報を含む）
        quizViewModel.loadStatisticsData();

        // チャットバブルにアニメーションを適用
        applyAnimationsToChatBubbles();

        // ストリーク情報を監視して表示を更新
        quizViewModel.getStreakInfo().observe(getViewLifecycleOwner(), streakPair -> {
            int currentStreak = streakPair.first;
            int bestStreak = streakPair.second;
            Log.d(TAG, "Streak info observed: current=" + currentStreak + ", best=" + bestStreak);
            updateChatBubbleBasedOnStreak(currentStreak);
        });

        // --- isLoading の監視 (変更なし) ---
        quizViewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading != null) {
                Log.d(TAG, "isLoading changed: " + isLoading);
            }
        });
        // --- isLoading の監視 終了 ---

        // --- 統計情報の監視呼び出し ---
        observeStatistics(); // Keep call to observe, but method content is reduced
        // --- 監視 終了 ---

        // ランダム出題ボタンのクリックリスナー (修正)
        binding.randomExamButton.setOnClickListener(v -> {
            // ViewModel のメソッド呼び出しと LiveData 監視を削除
            // quizViewModel.loadRandomQuizId();
            // quizViewModel.getRandomQuizId().observe(getViewLifecycleOwner(), qid -> { ... });

            // QuizFragment へ直接ナビゲーションし、ランダムモードを指定
            Log.d(TAG, "Navigating to QuizFragment in random mode (isRandomMode=true, qid=null)");
            HomeFragmentDirections.ActionHomeFragmentToQuizFragment action =
                    HomeFragmentDirections.actionHomeFragmentToQuizFragment();
            action.setIsRandomMode(true);
            try {
                navController.navigate(action);
            } catch (Exception e) {
                // ナビゲーション失敗時のエラーハンドリング
                Log.e(TAG, "Navigation to QuizFragment failed", e);
                Toast.makeText(getContext(), "画面遷移に失敗しました", Toast.LENGTH_SHORT).show();
            }
        });

        // ViewModelのエラーメッセージを監視してToast表示 (変更なし)
        quizViewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            }
        });

        // 分野別に出題ボタンのクリックリスナー (変更なし)
        binding.categoryExamButton.setOnClickListener(v ->
            navController.navigate(R.id.action_homeFragment_to_chapterFragment)
        );

        // 問題一覧ボタンのクリックリスナー (遷移先を学習履歴に変更)
        binding.listExamButton.setOnClickListener(v ->
            navController.navigate(R.id.navigation_history) // 遷移先を学習履歴画面のIDに変更
        );

        // --- 削除: 目標変更ボタンのクリックリスナー ---
        /*
        binding.editGoalButton.setOnClickListener(v -> {
            showEditGoalDialog();
        });
        */

        // --- 削除: ViewModel LiveData の週次進捗関連の監視 ---
        /*
        quizViewModel.getWeeklyAnswersCount().observe(getViewLifecycleOwner(), answers -> {
            // binding.weeklyAnswersValueTextView.setText(String.valueOf(answers)); <-- DELETED
            quizViewModel.getWeeklyGoal().observe(getViewLifecycleOwner(), goal -> {
                 // binding.weeklyLearningTimeProgress... <-- DELETED
            });
        });
        quizViewModel.getWeeklyGoal().observe(getViewLifecycleOwner(), goal -> {
            // binding.weeklyGoalValueTextView... <-- DELETED
        });
        */
    }

    // --- 削除: UIの初期セットアップメソッド ---
    /*
    private void setupUI() {
        // binding.weeklyLearningTimeProgress... <-- DELETED
        // binding.weeklyAnswersValueTextView... <-- DELETED
        // binding.weeklyGoalValueTextView... <-- DELETED
        // setupActivityBars(new int[7]); <-- DELETED
    }
    */

    // --- 変更: 統計情報監視メソッド (ログ出力のみ残す) ---
    private void observeStatistics() {
        Log.d(TAG, "observeStatistics called (Observing data for logging)");

        // 今週の解答数を監視 (ログのみ)
        quizViewModel.getWeeklyAnswersCount().observe(getViewLifecycleOwner(), answers -> {
            Log.d(TAG, "[Observer] Weekly answers observed: " + answers);
            // updateCircularProgressBar(...) <-- DELETED
        });

        // 週目標を監視 (ログのみ)
        quizViewModel.getWeeklyGoal().observe(getViewLifecycleOwner(), goal -> {
            Log.d(TAG, "[Observer] Weekly goal observed: " + goal);
            // updateCircularProgressBar(...) <-- DELETED
            // setupActivityBars(...) <-- DELETED
        });

        // 曜日別解答数を監視 (ログのみ)
        quizViewModel.getWeeklyDailyAnswerCounts().observe(getViewLifecycleOwner(), dailyCounts -> {
            Log.d(TAG, "[Observer] Daily counts observed: " + Arrays.toString(dailyCounts));
            // setupActivityBars(dailyCounts); <-- DELETED
        });
    }

    // --- 削除: 円形プログレスバー更新メソッド ---
    /*
    private void updateCircularProgressBar(int currentAnswers, int targetGoal) {
        // binding.weeklyLearningTimeProgress... <-- DELETED
    }
    */

    // --- 削除: 曜日別棒グラフセットアップメソッド ---
    /*
    private void setupActivityBars(int[] dailyCounts) {
        // binding.mondayBar... etc. <-- DELETED
    }
    */

    // --- 削除: 目標変更ダイアログ表示メソッド ---
    /*
    private void showEditGoalDialog() {
        // AlertDialog, EditText, FrameLayout related code <-- DELETED
    }
    */

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView");
        binding = null; // Important to prevent memory leaks
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    // チャットバブルにアニメーションを適用するメソッド
    private void applyAnimationsToChatBubbles() {
        try {
            // チャットバブルコンテナを取得
            View rootView = binding.getRoot();
            LinearLayout chatContainer = (LinearLayout) rootView.findViewWithTag("chat_container");
            
            if (chatContainer != null && chatContainer.getChildCount() >= 2) {
                View firstBubble = chatContainer.getChildAt(0);
                View secondBubble = chatContainer.getChildAt(1);

                // アニメーションをロード
                Animation chatAnim1 = AnimationUtils.loadAnimation(getContext(), R.anim.chat_bubble_in);
                Animation chatAnim2 = AnimationUtils.loadAnimation(getContext(), R.anim.chat_bubble_in);
                
                // 2つ目のバブルのアニメーション開始を遅延させる (300ms -> 450ms)
                chatAnim2.setStartOffset(450);
                
                // アニメーションを開始
                firstBubble.startAnimation(chatAnim1);
                secondBubble.startAnimation(chatAnim2);
                
                Log.d(TAG, "チャットバブルにアニメーションを適用しました");
            } else {
                Log.e(TAG, "チャットバブルコンテナが見つからないか、十分な子要素がありません");
            }
        } catch (Exception e) {
            Log.e(TAG, "チャットバブルアニメーション適用中にエラー: " + e.getMessage());
        }
    }
    
    // 連続学習日数に基づいてチャットバブルのテキストを更新するメソッド
    private void updateChatBubbleBasedOnStreak(int streak) {
        String message;
        
        if (streak == 0) {
            message = "今日も一緒にJavaの知識を深めていきましょう！どの分野から学習しますか？";
        } else if (streak < 3) {
            message = "連続" + streak + "日目の学習です！その調子で続けましょう！";
        } else if (streak < 7) {
            message = "すごい！連続" + streak + "日目の学習です！習慣化できていますね！";
        } else {
            message = "素晴らしい！連続" + streak + "日も学習を続けています！あなたはJavaマスターへの道を着実に歩んでいます！";
        }
        
        // チャットバブルのテキストを更新
        binding.chatBubbleText.setText(message);
        
        // ストリークが長い場合、アニメーションを再生（既存のアニメーションを再利用）
        if (streak >= 7) {
            // 既存のanimation_coffeeを再利用
            binding.lottieAnimationView.setAnimation(R.raw.animation_coffee);
            binding.lottieAnimationView.playAnimation();
        }
    }
}
