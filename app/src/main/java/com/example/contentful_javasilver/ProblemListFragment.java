package com.example.contentful_javasilver;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.contentful_javasilver.adapter.UnitLessonAdapter;
import com.example.contentful_javasilver.data.QuizDao;
import com.example.contentful_javasilver.data.QuizDatabase;
import com.example.contentful_javasilver.data.QuizEntity;
import com.example.contentful_javasilver.databinding.FragmentProblemListBinding;
import com.example.contentful_javasilver.model.UnitLessonItem;
import com.example.contentful_javasilver.viewmodels.QuizViewModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.lifecycle.Lifecycle;
import android.widget.TextView;

public class ProblemListFragment extends Fragment implements UnitLessonAdapter.OnLessonClickListener, MenuProvider {
    private static final String TAG = "ProblemListFragment";

    private FragmentProblemListBinding binding;
    private QuizViewModel quizViewModel;
    private UnitLessonAdapter adapter;
    private MenuProvider menuProvider;
    private QuizDao quizDao;
    private final Map<String, Boolean> completionStatusCache = new HashMap<>();
    private final Executor executor = Executors.newSingleThreadExecutor();

    // ユニットのタイトルと説明
    private final String[] unitTitles = {
            "Javaの基礎とプログラムの開発",
            "データ型と文字列操作",
            "演算子と制御構造",
            "クラス定義とオブジェクト指向設計",
            "継承とインタフェースの実装",
            "例外処理とデバッグ"
    };

    private final String[] unitDescriptions = {
            "Javaプログラムの基本構造とコンパイル・実行プロセスを学びます",
            "変数、基本データ型、配列、および文字列操作のテクニックを習得します",
            "条件分岐、ループ、演算子を使ったプログラム制御フローを学びます",
            "オブジェクト指向プログラミングの原則とクラス設計の基礎を学びます",
            "継承、抽象化、インタフェースを活用した拡張性の高いコードの書き方を学びます",
            "効果的な例外処理のパターンとデバッグ技術を習得します"
    };

    // レッスンリスト（各ユニットごと）
    private final String[][] lessonTitles = {
            // ユニット1のレッスン
            {
                "Javaプログラムの構造と実行可能クラスの作成",
                "コンパイル・実行プロセスとクラスパス",
                "パッケージの設計とインポート最適化"
            },
            // ユニット2のレッスン
            {
                "変数のスコープとライフサイクル",
                "型推論とvarの適切な使用法",
                "テキストブロックと効率的な文字列処理",
                "多次元配列の操作とメモリ管理",
                "コレクションフレームワークとArrayList"
            },
            // ユニット3のレッスン
            {
                "条件分岐とフロー制御の最適化",
                "効率的なループ構造の選択と実装",
                "break・continueによる制御フロー最適化",
                "パターンマッチングとswitch式の活用"
            },
            // ユニット4のレッスン
            {
                "クラス設計の原則とオブジェクトライフサイクル",
                "メソッド設計とコンストラクタパターン",
                "メソッドオーバーロードと引数の取り扱い",
                "static要素とシングルトンパターン",
                "アクセス修飾子とカプセル化の実践",
                "型チェックとパターンマッチング",
                "レコードクラスとイミュータブル設計"
            },
            // ユニット5のレッスン
            {
                "継承階層の設計と実装",
                "抽象クラスによるフレームワーク設計",
                "メソッドオーバーライドとポリモーフィズム",
                "instanceof演算子と安全なキャスト",
                "finalクラスによる継承制限",
                "インタフェース設計とマーカーインタフェース",
                "デフォルト・プライベートメソッドの活用",
                "シールドクラスと継承コントロール"
            },
            // ユニット6のレッスン
            {
                "例外の種類と例外階層",
                "try-catchブロックと例外処理戦略",
                "try-with-resourcesによる自動リソース管理",
                "カスタム例外クラスの設計と活用",
                "マルチキャッチと効率的なエラーハンドリング"
            }
    };

    // レッスンカテゴリーの配列
    private final String[][] lessonCategories = {
            // ユニット1のレッスンカテゴリー
            {"実装", "概念", "実装"},
            // ユニット2のレッスンカテゴリー
            {"概念", "実装", "実装", "実装", "実装"},
            // ユニット3のレッスンカテゴリー
            {"実装", "実装", "実装", "実装"},
            // ユニット4のレッスンカテゴリー
            {"概念", "実装", "実装", "実装", "概念", "実装", "実装"},
            // ユニット5のレッスンカテゴリー
            {"実装", "実装", "実装", "実装", "概念", "実装", "実装", "概念"},
            // ユニット6のレッスンカテゴリー
            {"概念", "実装", "実装", "実装", "実装"}
    };

    // レッスンのアイコンIDの配列
    private final int[][] lessonIcons = {
            // ユニット1のレッスンアイコン
            {R.drawable.ic_lesson_code, R.drawable.ic_lesson_code, R.drawable.ic_lesson_code},
            // ユニット2のレッスンアイコン
            {R.drawable.ic_lesson_datatype, R.drawable.ic_lesson_datatype, R.drawable.ic_lesson_datatype, R.drawable.ic_lesson_datatype, R.drawable.ic_lesson_datatype},
            // ユニット3のレッスンアイコン
            {R.drawable.ic_lesson_control, R.drawable.ic_lesson_control, R.drawable.ic_lesson_control, R.drawable.ic_lesson_control},
            // ユニット4のレッスンアイコン
            {R.drawable.ic_lesson_class, R.drawable.ic_lesson_class, R.drawable.ic_lesson_class, R.drawable.ic_lesson_class, R.drawable.ic_lesson_class, R.drawable.ic_lesson_class, R.drawable.ic_lesson_class},
            // ユニット5のレッスンアイコン
            {R.drawable.ic_lesson_inheritance, R.drawable.ic_lesson_inheritance, R.drawable.ic_lesson_inheritance, R.drawable.ic_lesson_inheritance, R.drawable.ic_lesson_inheritance, R.drawable.ic_lesson_inheritance, R.drawable.ic_lesson_inheritance, R.drawable.ic_lesson_inheritance},
            // ユニット6のレッスンアイコン
            {R.drawable.ic_lesson_exception, R.drawable.ic_lesson_exception, R.drawable.ic_lesson_exception, R.drawable.ic_lesson_exception, R.drawable.ic_lesson_exception}
    };

    // レッスン完了状態（サンプル）
    private final boolean[][] lessonCompleted = {
            // ユニット1のレッスン完了状態
            {true, true, false},
            // ユニット2のレッスン完了状態
            {false, false, false, false, false},
            // ユニット3のレッスン完了状態
            {false, false, false, false},
            // ユニット4のレッスン完了状態
            {false, false, false, false, false, false, false},
            // ユニット5のレッスン完了状態
            {false, false, false, false, false, false, false, false},
            // ユニット6のレッスン完了状態
            {false, false, false, false, false}
    };

    // ユニット名と説明（一時的にハードコード - ViewModelから取得したデータで補完）
    private final Map<Integer, String> unitTitlesMap = Map.of(
            1, "Javaの基礎とプログラムの開発",
            2, "データ型と文字列操作",
            3, "演算子と制御構造",
            4, "クラス定義とオブジェクト指向設計",
            5, "継承とインタフェースの実装",
            6, "例外処理とデバッグ"
    );
    private final Map<Integer, String> unitDescriptionsMap = Map.of(
            1, "Javaプログラムの基本構造とコンパイル・実行プロセスを学びます",
            2, "変数、基本データ型、配列、および文字列操作のテクニックを習得します",
            3, "条件分岐、ループ、演算子を使ったプログラム制御フローを学びます",
            4, "オブジェクト指向プログラミングの原則とクラス設計の基礎を学びます",
            5, "継承、抽象化、インタフェースを活用した拡張性の高いコードの書き方を学びます",
            6, "効果的な例外処理のパターンとデバッグ技術を習得します"
    );

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        quizViewModel = new ViewModelProvider(requireActivity()).get(QuizViewModel.class);
        QuizDatabase database = QuizDatabase.getDatabase(requireContext());
        quizDao = database.quizDao();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProblemListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MenuHost menuHost = requireActivity();
        menuProvider = this;
        menuHost.addMenuProvider(menuProvider, getViewLifecycleOwner(), Lifecycle.State.RESUMED);

        // 引数から最後に解いた問題IDを取得 (nullの可能性あり)
        String navArgLastSolvedQuizId = null;
        if (getArguments() != null) {
            navArgLastSolvedQuizId = getArguments().getString("lastSolvedQuizId");
            Log.d(TAG, "Received lastSolvedQuizId from navigation args: " + navArgLastSolvedQuizId);
        }

        setupUI();
        setupRecyclerView();
        // observeViewModel に引数を渡して呼び出し
        observeViewModel(navArgLastSolvedQuizId);

        quizViewModel.loadAllProblems();

        // 統計データをロード（ストリーク情報を含む）
        quizViewModel.loadStatisticsData();
    }

    private void setupUI() {
        // プレミアムボタンのリスナー削除（ボタン自体を削除したため）
        // binding.includedAchievementsBanner.premiumButton.setOnClickListener(v -> {
        //     // プレミアム機能へのナビゲーション（未実装）
        //     Toast.makeText(requireContext(), "プレミアム機能は現在開発中です", Toast.LENGTH_SHORT).show();
        // });

        // プロフィールボタンのリスナーも削除 (ボタン自体を削除したため)
        // binding.includedAchievementsBanner.profileButton.setOnClickListener(v -> {
        //     // プロフィール画面へナビゲーション（未実装）
        //     Toast.makeText(requireContext(), "プロフィール機能は現在開発中です", Toast.LENGTH_SHORT).show();
        // });
        
        // 学習パス表示を設定 (元のIDに戻す)
        binding.pathTitle.setText(getString(R.string.java_learning_path));
        binding.pathDetails.setText("6 ユニット • Java Silver認定対応");
        
        // ストリーク情報を監視して表示を更新
        quizViewModel.getStreakInfo().observe(getViewLifecycleOwner(), streakPair -> {
            int currentStreak = streakPair.first;
            // fireAchievement の表示/非表示はXMLで制御するため、Javaコードからは削除
            if (currentStreak > 0) {
                // binding.includedAchievementsBanner.fireAchievement.setVisibility(View.VISIBLE);
                binding.includedAchievementsBanner.fireCount.setText(String.valueOf(currentStreak));
            } else {
                // ストリークが0の場合でも fireCount は非表示にするか、値を0にするべきか？
                // 今回はXMLで常に表示なので、カウントを0に設定するか、空にする。
                // 0を表示することにする
                binding.includedAchievementsBanner.fireCount.setText("0");
                // XMLで常に表示なので、View.GONEのロジックは不要
                // binding.includedAchievementsBanner.fireAchievement.setVisibility(View.GONE);
            }
        });
    }

    private void setupRecyclerView() {
        // Adapterの初期化 (空のリストで)
        adapter = new UnitLessonAdapter(new ArrayList<>(), this);
        binding.unitsLessonsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.unitsLessonsRecyclerView.setAdapter(adapter);
    }

    @Override
    public void onLessonClick(UnitLessonItem item) {
        // Check if the clicked item is a header or a lesson
        if (item.isUnitHeader()) {
            // It's a header, log it or handle if necessary (e.g., expand/collapse)
            Log.d(TAG, "Unit header clicked: " + item.getTitle());
            // Currently, do nothing when a header is clicked
        } else {
            // It's a lesson item, proceed with navigation check
            String qid = item.getQid(); // Get qid for the lesson

            // Check if qid is valid before logging and navigating
            if (qid == null || qid.isEmpty()) {
                Log.e(TAG, "Invalid QID for navigation (null or empty): " + item.getTitle());
                Toast.makeText(requireContext(), "問題IDが無効なため遷移できません", Toast.LENGTH_SHORT).show();
                return; // Stop navigation
            }

            Log.d(TAG, "Lesson clicked: " + item.getTitle() + " with qid: " + qid);
            navigateToQuiz(qid); // Navigate only if qid is valid
        }
    }

    private void navigateToQuiz(String qid) {
        // The check here is now redundant because it's done in onLessonClick,
        // but keeping it provides an extra layer of safety.
        if (qid == null || qid.isEmpty()) {
            Log.e(TAG, "navigateToQuiz called with invalid qid (null or empty).");
            Toast.makeText(requireContext(), "問題IDが無効です", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.d(TAG, "Navigating to QuizFragment with qid: " + qid);
        Bundle bundle = new Bundle();
        bundle.putString("qid", qid);
        bundle.putBoolean("isRandomMode", false); // Add isRandomMode for consistency
        NavController navController = NavHostFragment.findNavController(this);
        try { // Add try-catch for navigation
            navController.navigate(R.id.action_problemListFragment_to_quizFragment, bundle);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Navigation failed (ProblemList -> Quiz): ", e);
            Toast.makeText(requireContext(), "画面遷移に失敗しました", Toast.LENGTH_SHORT).show();
        }
    }

    // observeViewModel メソッドのシグネチャを変更し、引数を追加
    private void observeViewModel(String navArgLastSolvedQuizId) {
        // ViewModelのallQuizzesを監視
        quizViewModel.getAllQuizzesLiveData().observe(getViewLifecycleOwner(), problems -> {
            Log.d(TAG, "Observed quiz list change. Got " + (problems != null ? problems.size() : "null") + " problems.");
            if (problems != null && !problems.isEmpty()) {
                // データロード完了時に完了状態をプリロード (UI更新とスクロールはpreload完了後)
                preloadCompletionStatus(problems, navArgLastSolvedQuizId);
            } else {
                // problemsがnullまたは空の場合もUI更新
                updateUnitLessonList(problems);
            }
        });

        // (他のLiveDataの監視処理はそのまま)
         quizViewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            // ローディング表示の更新 (必要に応じて)
            // binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            Log.d(TAG, "isLoading changed: " + isLoading);
        });

        quizViewModel.getErrorMessage().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Log.e(TAG, "Error from ViewModel: " + errorMessage);
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
                // エラーメッセージをクリアする処理も必要かも
                // quizViewModel.clearErrorMessage();
            }
        });

        // --- 追加: 今日の学習時間を監視 ---
        quizViewModel.getTodayStudyTimeLiveData().observe(getViewLifecycleOwner(), timeString -> {
            if (binding != null && binding.includedAchievementsBanner != null) {
                binding.includedAchievementsBanner.todayLearningTimeText.setText(timeString);
            }
        });
        // --- 追加ここまで ---
    }

    // preloadCompletionStatus メソッドの最後に UI 更新とスクロール処理を移動
    // 引数に navArgLastSolvedQuizId を追加
    private void preloadCompletionStatus(List<QuizEntity> problems, String navArgLastSolvedQuizId) {
        executor.execute(() -> {
            try {
                completionStatusCache.clear();
                for (QuizEntity problem : problems) {
                    String qid = problem.getQid();
                    if (qid != null && !qid.isEmpty()) {
                        boolean isCompleted = quizDao.isProblemAnswered(qid);
                        completionStatusCache.put(qid, isCompleted);
                    }
                }

                // メインスレッドでUIを更新し、スクロール処理を呼び出す
                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (isAdded()) { // 再確認
                            // UIリストを更新
                            updateUnitLessonList(problems);

                            // スクロール対象IDを決定 (ナビゲーション引数を優先)
                            String targetQuizId = navArgLastSolvedQuizId;
                             if (targetQuizId == null || targetQuizId.isEmpty()) {
                                // 引数がなければViewModelから取得 (SharedPreferencesからロードされた値)
                                targetQuizId = quizViewModel.getLastSolvedQuizId().getValue();
                                Log.d(TAG, "Using lastSolvedQuizId from ViewModel: " + targetQuizId);
                            } else {
                                Log.d(TAG, "Using lastSolvedQuizId from NavArgs: " + targetQuizId);
                            }

                            // IDがあればスクロール
                            if (targetQuizId != null && !targetQuizId.isEmpty()) {
                                final String finalTargetQuizId = targetQuizId;
                                binding.unitsLessonsRecyclerView.post(() -> {
                                    Log.d(TAG, "Executing scrollToQuiz after preload and UI update for: " + finalTargetQuizId);
                                    scrollToQuiz(finalTargetQuizId);
                                });
                             }
                        }
                    });
                }
            } catch (Exception e) {
                 Log.e(TAG, "Error preloading completion status: " + e.getMessage());
                 // エラー時もUIは更新する
                 if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (isAdded()) {
                            updateUnitLessonList(problems);
                        }
                    });
                }
            }
        });
    }

    // 新しいメソッド: ViewModel から取得した QuizEntity リストを UI モデルに変換して表示
    private void updateUnitLessonList(List<QuizEntity> problems) {
        if (problems == null) {
            Log.w(TAG, "Received null problem list from ViewModel.");
            adapter.updateItems(new ArrayList<>()); // 空リストで更新
            return;
        }
        Log.d(TAG, "Updating UI with " + problems.size() + " problems.");

        List<UnitLessonItem> displayItems = new ArrayList<>();

        // 問題をユニット番号でグループ化 (qidの最初の数字)
        Map<Integer, List<QuizEntity>> groupedByUnit = problems.stream()
                .filter(p -> p.getQid() != null && p.getQid().contains("-")) // qid形式チェック
                .collect(Collectors.groupingBy(
                        p -> {
                            try {
                                return Integer.parseInt(p.getQid().split("-")[0]);
                            } catch (NumberFormatException e) {
                                return -1; // 不正な形式は無視
                            }
                        },
                        LinkedHashMap::new, // 順序を保持
                        Collectors.toList()
                ));

        groupedByUnit.remove(-1); // 不正な形式のグループを除去

        int totalUnits = unitTitlesMap.size(); // 仮。 groupedByUnit.keySet().stream().max(Integer::compare).orElse(0); などで取得推奨

        // 各ユニットについて処理 (ユニット番号でソート)
        List<Integer> sortedUnitNumbers = new ArrayList<>(groupedByUnit.keySet());
        Collections.sort(sortedUnitNumbers);

        for (Integer unitNumber : sortedUnitNumbers) {
            List<QuizEntity> lessonsInUnit = groupedByUnit.get(unitNumber);
            if (lessonsInUnit == null || lessonsInUnit.isEmpty()) continue;

            // ユニットヘッダーを追加
            boolean isLastUnit = Objects.equals(unitNumber, sortedUnitNumbers.get(sortedUnitNumbers.size() - 1));
            int completedLessonsInUnit = countCompletedLessonsInUnit(unitNumber, lessonsInUnit);
            int totalLessonsInUnit = lessonsInUnit.size();
            String unitTitle = unitNumber + "章";
            String unitDescription = unitDescriptionsMap.getOrDefault(unitNumber, "");

            displayItems.add(new UnitLessonItem(
                    unitNumber,
                    unitTitle,
                    unitDescription,
                    completedLessonsInUnit,
                    totalLessonsInUnit,
                    isLastUnit
            ));

            // そのユニットのレッスンを追加 (QuizEntity から UnitLessonItem へ変換)
            int lessonIndex = 0;
            lessonsInUnit.sort((q1, q2) -> {
                try {
                    int lNum1 = Integer.parseInt(q1.getQid().split("-")[1]);
                    int lNum2 = Integer.parseInt(q2.getQid().split("-")[1]);
                    return Integer.compare(lNum1, lNum2);
                } catch (Exception e) {
                    return q1.getQid().compareTo(q2.getQid()); // パース失敗時は文字列比較
                }
            });

            for (QuizEntity lesson : lessonsInUnit) {
                int lessonNumberInQid; // qidからレッスン番号を取得
                try {
                     lessonNumberInQid = Integer.parseInt(lesson.getQid().split("-")[1]);
                } catch (Exception e) {
                    lessonNumberInQid = lessonIndex + 1; // fallback
                }

                int iconResId = getLessonIcon(unitNumber, lessonIndex);
                boolean completed = isLessonCompleted(lesson.getQid());
                String lessonCategory = lesson.getQuestionCategory() != null ? lesson.getQuestionCategory() : "";

                displayItems.add(new UnitLessonItem(
                        unitNumber,
                        lessonNumberInQid,
                        lesson.getCategory(), // Lesson Title from Contentful
                        lessonCategory,     // Question Category from Contentful
                        iconResId,
                        completed,
                        lesson.getQid()
                ));
                lessonIndex++;
            }
        }

        adapter.updateItems(displayItems);
        Log.d(TAG, "RecyclerView updated with " + displayItems.size() + " items.");
    }

    // 指定されたユニットの完了レッスン数を計算 (DB連携実装)
    private int countCompletedLessonsInUnit(int unitNumber, List<QuizEntity> lessons) {
        int count = 0;
        for (QuizEntity lesson : lessons) {
            if (lesson.getQid() != null && !lesson.getQid().isEmpty()) {
                if (isLessonCompleted(lesson.getQid())) {
                    count++;
                }
            }
        }
        return count;
    }

    // レッスンアイコン取得ヘルパー（ユニット別）
    private int getLessonIcon(int unitNumber, int lessonIndex) {
        // ★ lessonIcons 配列を使用するように変更
        if (unitNumber > 0 && unitNumber <= lessonIcons.length) {
            // ユニットに対応するアイコン配列を取得
            int[] iconsInUnit = lessonIcons[unitNumber - 1];
            if (iconsInUnit.length > 0) {
                // ユニット内の最初のアイコン（＝そのユニットの統一アイコン）を返す
                // lessonIndex は使わない
                return iconsInUnit[0];
            }
        }
        // 不正な場合やアイコン配列が空の場合はデフォルトアイコンを返す
        return R.drawable.ic_lesson_icon_template; // デフォルトアイコン
    }

    // レッスン完了状態取得ヘルパー (キャッシュ対応版)
    private boolean isLessonCompleted(String qid) {
        if (qid == null || qid.isEmpty()) {
            return false;
        }
        
        // キャッシュから結果を取得
        if (completionStatusCache.containsKey(qid)) {
            return completionStatusCache.get(qid);
        }
        
        // キャッシュにない場合は安全のためfalseを返す
        // (preloadCompletionStatusで全ての問題がロードされているはず)
        return false;
    }

    @Override
    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.problem_list_menu, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        if (searchItem != null && searchItem.getIcon() != null) {
            searchItem.getIcon().setColorFilter(
                ContextCompat.getColor(requireContext(), android.R.color.white),
                android.graphics.PorterDuff.Mode.SRC_ATOP
            );
        }

        SearchView searchView = (SearchView) searchItem.getActionView();
        if (searchView != null) {
            // SearchViewをカスタマイズ (RestrictedApi Lintエラー回避のためコメントアウト)
            /*
            SearchView.SearchAutoComplete searchAutoComplete = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
            if (searchAutoComplete != null) {
                searchAutoComplete.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
                searchAutoComplete.setHintTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
                searchAutoComplete.setHint("レッスンを検索...");
            }
            */

            View closeButton = searchView.findViewById(androidx.appcompat.R.id.search_close_btn);
            if (closeButton != null) {
                ((android.widget.ImageView) closeButton).setColorFilter(
                    ContextCompat.getColor(requireContext(), android.R.color.white),
                    android.graphics.PorterDuff.Mode.SRC_ATOP
                );
            }

            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    return false;
                }
                @Override
                public boolean onQueryTextChange(String newText) {
                    quizViewModel.setSearchQuery(newText);
                    return true;
                }
            });

            searchView.setOnCloseListener(() -> {
                quizViewModel.setSearchQuery("");
                return false;
            });
        }
    }

    @Override
    public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
        return false;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (menuProvider != null) {
            ((MenuHost) requireActivity()).removeMenuProvider(menuProvider);
        }
        binding = null;
        Log.d(TAG, "onDestroyView called.");
    }
    
    /**
     * 指定された問題IDの位置にスクロールします
     *
     * @param quizId スクロール先の問題ID
     */
    private void scrollToQuiz(String quizId) {
        Log.d(TAG, "Attempting to scroll to quiz: " + quizId);
        if (adapter == null || quizId == null || quizId.isEmpty()) {
            Log.w(TAG, "Cannot scroll: adapter is null or quiz ID is invalid");
            return;
        }

        // まず指定された問題IDに対応するアイテムのポジションを探す
        List<UnitLessonItem> items = adapter.getItems();
        int targetPosition = -1;
        
        for (int i = 0; i < items.size(); i++) {
            UnitLessonItem item = items.get(i);
            if (!item.isUnitHeader() && quizId.equals(item.getQid())) {
                targetPosition = i;
                break;
            }
        }
        
        final int finalTargetPosition = targetPosition; // ラムダ式用にfinal変数にコピー
        
        if (finalTargetPosition != -1) {
            // 見つかった位置にスクロール
            Log.d(TAG, "Scrolling to position: " + finalTargetPosition);
            LinearLayoutManager layoutManager = (LinearLayoutManager) binding.unitsLessonsRecyclerView.getLayoutManager();
            if (layoutManager != null) {
                layoutManager.scrollToPositionWithOffset(finalTargetPosition, 20); // 上部に少し余白を持たせる
                
                // 任意: 項目をハイライト表示
                binding.unitsLessonsRecyclerView.postDelayed(() -> {
                    View itemView = layoutManager.findViewByPosition(finalTargetPosition);
                    if (itemView != null) {
                        itemView.setBackgroundResource(R.drawable.highlight_background);
                        // 数秒後にハイライトを消す
                        itemView.postDelayed(() -> 
                            itemView.setBackgroundResource(android.R.color.transparent), 2000);
                    }
                }, 100);
            }
        } else {
            // 問題IDから単位番号を抽出（例: "1-5"から"1"を取得）
            String unitPart = null;
            if (quizId.contains("-")) {
                unitPart = quizId.split("-")[0];
            }
            
            if (unitPart != null) {
                // 単位番号が一致するヘッダーを探す
                int unitNumber = Integer.parseInt(unitPart);
                for (int i = 0; i < items.size(); i++) {
                    UnitLessonItem item = items.get(i);
                    if (item.isUnitHeader() && item.getUnitNumber() == unitNumber) {
                        Log.d(TAG, "Quiz not found, scrolling to unit header: " + unitNumber);
                        LinearLayoutManager layoutManager = (LinearLayoutManager) binding.unitsLessonsRecyclerView.getLayoutManager();
                        if (layoutManager != null) {
                            layoutManager.scrollToPositionWithOffset(i, 0);
                        }
                        break;
                    }
                }
            }
        }
    }
}
