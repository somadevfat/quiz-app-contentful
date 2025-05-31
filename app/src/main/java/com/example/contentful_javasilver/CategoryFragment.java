package com.example.contentful_javasilver;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.contentful_javasilver.adapter.CategoryAdapter;
import com.example.contentful_javasilver.data.QuizDao;
import com.example.contentful_javasilver.data.QuizDatabase;
import com.example.contentful_javasilver.data.QuizEntity;
import com.example.contentful_javasilver.databinding.FragmentCategoryBinding;
import com.example.contentful_javasilver.decoration.VerticalSpaceItemDecoration;
import com.example.contentful_javasilver.model.CategoryItem;
import com.example.contentful_javasilver.DatabaseHelperCoroutines;
import com.example.contentful_javasilver.viewmodels.QuizViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class CategoryFragment extends Fragment implements CategoryAdapter.CategoryClickListener {
    private static final String TAG = "CategoryFragment";
    private FragmentCategoryBinding binding;
    private CategoryAdapter categoryAdapter;
    private QuizDao quizDao;
    private DatabaseHelperCoroutines databaseHelper;
    private QuizViewModel quizViewModel;
    private int chapterNumber;
    private String chapterTitle;
    private String chapterDescription; // 章の説明を保持するフィールドを追加
    private final Executor executor = Executors.newSingleThreadExecutor();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        quizViewModel = new ViewModelProvider(requireActivity()).get(QuizViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCategoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Bundleからデータを取得 (章の説明も取得できるように仮定、なければデフォルト値)
        if (getArguments() != null) {
            chapterNumber = getArguments().getInt("chapterNumber", 1);
            chapterTitle = getArguments().getString("chapterTitle", "");
            // ChapterFragment から渡されるデータに description が含まれるように修正が必要
            // もしくは、ここで chapterNumber に応じて description を設定する
            chapterDescription = getChapterDescription(chapterNumber); // 説明取得メソッドを呼び出し
        }

        Log.d(TAG, "Chapter number: " + chapterNumber + ", title: " + chapterTitle);

        // データベースの初期化
        QuizDatabase db = QuizDatabase.getDatabase(requireContext());
        quizDao = db.quizDao();
        databaseHelper = new DatabaseHelperCoroutines();

        setupHeader(); // ヘッダー設定メソッドを呼び出し
        setupRecyclerView();
        setupBannerActions();
        observeViewModel();
        
        // カテゴリをロード
        showLoading(true);
        loadCategories();
        quizViewModel.loadStatisticsData();
    }

    // ヘッダーカードに Unit タイトルと説明を設定するメソッド
    private void setupHeader() {
        if (binding != null) {
            // ヘッダーカードに Unit タイトルと説明を設定 (元のIDに戻す)
            binding.unitTitleText.setText(getString(R.string.unit_format, chapterNumber) + ": " + chapterTitle);
            binding.unitDescriptionText.setText(chapterDescription);
        }
    }

    // 章番号に基づいて説明文を返すヘルパーメソッド（仮実装）
    private String getChapterDescription(int chapterNum) {
        // ChapterFragmentのchapterDescriptions配列と同様のロジックで取得
        String[] descriptions = {
            "Javaプログラムの基本構造とコンパイル・実行方法を学びます",
            "変数、データ型、配列、文字列操作の基本を習得します",
            "条件分岐や繰り返し処理など、プログラムの流れを制御する方法を学びます",
            "オブジェクト指向プログラミングの基礎となるクラスの定義と使用方法を学びます",
            "継承と多態性の概念を理解し、インタフェースを活用する方法を学びます",
            "例外処理の仕組みと実装方法についてマスターします"
        };
        if (chapterNum > 0 && chapterNum <= descriptions.length) {
            return descriptions[chapterNum - 1];
        }
        return ""; // デフォルトの説明
    }

    private void setupRecyclerView() {
        binding.categoryRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        categoryAdapter = new CategoryAdapter(this);
        binding.categoryRecyclerView.setAdapter(categoryAdapter);

        // ItemDecoration を追加してアイテム間のスペースを設定
        int spacingInPixels = getResources().getDimensionPixelSize(R.dimen.category_item_vertical_spacing);
        binding.categoryRecyclerView.addItemDecoration(new VerticalSpaceItemDecoration(spacingInPixels));
    }

    // バナーのボタンアクションを設定するメソッド
    private void setupBannerActions() {
        if (binding != null) {
            // プレミアムボタンの参照を削除
            // binding.includedAchievementsBanner.premiumButton.setOnClickListener(v -> {
            //     Toast.makeText(requireContext(), "プレミアム機能は現在開発中です", Toast.LENGTH_SHORT).show();
            // });
            // プロフィールボタンの参照を削除
            // binding.includedAchievementsBanner.profileButton.setOnClickListener(v -> {
            //     Toast.makeText(requireContext(), "プロフィール機能は現在開発中です", Toast.LENGTH_SHORT).show();
            // });
        }
    }

    // ViewModelを監視するメソッド
    private void observeViewModel() {
        if (quizViewModel != null && getViewLifecycleOwner() != null) {
            quizViewModel.getStreakInfo().observe(getViewLifecycleOwner(), streakPair -> {
                if (binding != null) {
                    int currentStreak = streakPair.first;
                    // fireAchievement の表示/非表示ロジックを修正 (XMLで制御するためJavaからは削除)
                    if (currentStreak > 0) {
                        // binding.includedAchievementsBanner.fireAchievement.setVisibility(View.VISIBLE);
                        binding.includedAchievementsBanner.fireCount.setText(String.valueOf(currentStreak));
                    } else {
                        // ストリーク0の場合、カウントを0に設定
                        binding.includedAchievementsBanner.fireCount.setText("0");
                        // binding.includedAchievementsBanner.fireAchievement.setVisibility(View.GONE);
                    }
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
    }

    @Override
    public void onCategoryClick(CategoryItem category) {
        // CategoryItem からカテゴリ名を取得してナビゲーション
        navigateToQuestionCategory(category.getCategoryName());
    }

    private void navigateToQuestionCategory(String categoryName) {
        if (categoryName == null || categoryName.isEmpty()) {
            Log.e(TAG, "Cannot navigate, category name is invalid.");
            return;
        }
        Log.d(TAG, "Navigating to QuestionCategoryFragment with category: " + categoryName);
        Bundle args = new Bundle();
        args.putString("categoryName", categoryName);
        args.putInt("chapterNumber", chapterNumber); // chapterNumber も渡す
        Navigation.findNavController(requireView()).navigate(R.id.action_categoryFragment_to_questionCategoryFragment, args);
    }
    
    private void showLoading(boolean isLoading) {
        if (binding != null) {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            binding.categoryRecyclerView.setVisibility(isLoading ? View.GONE : View.VISIBLE);
            binding.errorMessage.setVisibility(View.GONE);
        }
    }
    
    private void showError(String message) {
        if (binding != null) {
            binding.progressBar.setVisibility(View.GONE);
            binding.categoryRecyclerView.setVisibility(View.GONE);
            binding.errorMessage.setVisibility(View.VISIBLE);
            binding.errorMessage.setText(message);
        }
    }

    // カテゴリ読み込みロジックを修正
    private void loadCategories() {
        Log.d(TAG, "Loading categories for chapter: " + chapterNumber);
        
        databaseHelper.loadCategoriesAsync(
            chapterNumber,
            quizDao,
            categoryNames -> {
                // getView() が null でない場合にのみ UI スレッドで処理を実行
                if (getView() != null) { 
                                        requireActivity().runOnUiThread(() -> {
                        if (categoryNames != null && !categoryNames.isEmpty()) {
                            Log.d(TAG, "Categories loaded: " + categoryNames.size());
                            List<CategoryItem> categoryItems = convertToCategoryItems(categoryNames);
                            categoryAdapter.submitList(categoryItems);
                                                    showLoading(false);
                                        } else {
                            Log.w(TAG, "No categories found for chapter " + chapterNumber);
                            showError("このユニットにはカテゴリが見つかりませんでした。");
                                        }
                                    });
                            }
                return null; // Function1<List<String>, Unit> のUnitに対応
            },
            error -> {
                 // getView() が null でない場合にのみ UI スレッドで処理を実行
                 if (getView() != null) { 
                     Log.e(TAG, "Error loading categories for chapter " + chapterNumber + ": " + error);
                    requireActivity().runOnUiThread(() -> {
                         showError("カテゴリの読み込みに失敗しました: " + error);
                    });
                }
                 return null; // Function1<String, Unit> のUnitに対応
            }
        );
    }

    // StringリストをCategoryItemリストに変換するヘルパーメソッド
    private List<CategoryItem> convertToCategoryItems(List<String> categoryNames) {
        List<CategoryItem> items = new ArrayList<>();
        for (String name : categoryNames) {
            // 仮の進捗、アイコンを設定 (説明は削除)
            // String description = name + " の詳細説明（仮）"; // 削除
            int totalQuestions = (int) (Math.random() * 10) + 5; // 仮の総問題数 (5-14)
            int completedQuestions = (int) (Math.random() * (totalQuestions + 1)); // 仮の完了問題数
            int iconResId = R.drawable.ic_category_placeholder; // 仮のアイコン

            items.add(new CategoryItem(name, /* description, */ totalQuestions, completedQuestions, iconResId, chapterNumber)); // description 引数を削除
            }
        return items;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
