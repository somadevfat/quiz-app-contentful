package com.example.contentful_javasilver;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.contentful_javasilver.data.QuizDao;
import com.example.contentful_javasilver.data.QuizDatabase;
import com.example.contentful_javasilver.databinding.FragmentQuestionCategoryBinding;
import com.example.contentful_javasilver.databinding.ItemQuestionCategoryBinding;
import com.example.contentful_javasilver.decoration.VerticalSpaceItemDecoration;
import com.example.contentful_javasilver.viewmodels.QuizViewModel;

import java.util.ArrayList;
import java.util.List;

import kotlin.Unit;

import com.example.contentful_javasilver.DatabaseHelperCoroutines.QuestionCategoryItem;
import androidx.navigation.Navigation;

public class QuestionCategoryFragment extends Fragment {
    private static final String TAG = "QuestionCategoryFragment";
    private FragmentQuestionCategoryBinding binding;
    private QuestionCategoryAdapter adapter;
    private QuizDao quizDao;
    private DatabaseHelperCoroutines databaseHelper;
    private QuizViewModel quizViewModel;
    private String selectedCategory;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        quizViewModel = new ViewModelProvider(requireActivity()).get(QuizViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentQuestionCategoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            selectedCategory = getArguments().getString("categoryName");
        }

        if (selectedCategory == null) {
            Log.e(TAG, "Category name argument is missing or null!");
            Toast.makeText(requireContext(), "カテゴリ情報の取得に失敗しました", Toast.LENGTH_SHORT).show();
            return;
        }

        QuizDatabase db = QuizDatabase.getDatabase(requireContext());
        quizDao = db.quizDao();
        databaseHelper = new DatabaseHelperCoroutines();

        setupHeader();
        setupRecyclerView();
        setupBannerActions();
        observeViewModel();
        loadQuestionCategories();
        quizViewModel.loadStatisticsData();
    }

    private void setupHeader() {
        if (binding != null) {
            // ヘッダーカードにカテゴリタイトルと説明を設定 (元のIDに戻す)
            binding.categoryTitleText.setText(selectedCategory);
            binding.categoryDescriptionText.setText(selectedCategory + " の問題カテゴリ一覧");
        }
    }

    private void setupRecyclerView() {
        binding.questionCategoryRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new QuestionCategoryAdapter(new ArrayList<>());
        binding.questionCategoryRecyclerView.setAdapter(adapter);
    }

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

    private void loadQuestionCategories() {
        showLoading(true);
        databaseHelper.loadQuestionCategoriesAsync(
            selectedCategory,
            quizDao,
            questionCategories -> {
                if (getView() != null) {
                    requireActivity().runOnUiThread(() -> {
                        if (questionCategories != null && !questionCategories.isEmpty()) {
                            adapter.updateQuestionCategories(questionCategories);
                            showLoading(false);
                        } else {
                            showError("このカテゴリには問題が見つかりませんでした。");
                        }
                    });
                }
                return Unit.INSTANCE;
            },
            error -> {
                if (getView() != null) {
                    requireActivity().runOnUiThread(() -> {
                        showError("問題カテゴリの読み込みに失敗しました: " + error);
                    });
                }
                return Unit.INSTANCE;
            }
        );
    }

    private void showLoading(boolean isLoading) {
        if (binding != null) {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            binding.questionCategoryRecyclerView.setVisibility(isLoading ? View.GONE : View.VISIBLE);
            binding.errorMessage.setVisibility(View.GONE);
        }
    }

    private void showError(String message) {
        if (binding != null) {
            binding.progressBar.setVisibility(View.GONE);
            binding.questionCategoryRecyclerView.setVisibility(View.GONE);
            binding.errorMessage.setVisibility(View.VISIBLE);
            binding.errorMessage.setText(message);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private class QuestionCategoryAdapter extends RecyclerView.Adapter<QuestionCategoryAdapter.ViewHolder> {
        private List<QuestionCategoryItem> items;

        QuestionCategoryAdapter(List<QuestionCategoryItem> items) {
            this.items = new ArrayList<>(items);
        }

        public void updateQuestionCategories(List<QuestionCategoryItem> newItems) {
            this.items.clear();
            this.items.addAll(newItems);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_question_category, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            QuestionCategoryItem item = items.get(position);
            boolean isLastItem = (position == items.size() - 1);
            holder.bind(item, isLastItem);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView categoryIcon;
            TextView itemNumberText;
            TextView questionCategoryTitle;
            View divider;
            ImageView completionCheckmark;

            ViewHolder(View itemView) {
                super(itemView);
                categoryIcon = itemView.findViewById(R.id.categoryIcon);
                itemNumberText = itemView.findViewById(R.id.itemNumberText);
                questionCategoryTitle = itemView.findViewById(R.id.questionCategoryTitle);
                divider = itemView.findViewById(R.id.divider);
                completionCheckmark = itemView.findViewById(R.id.completionCheckmark);
            }

            void bind(QuestionCategoryItem item, boolean isLastItem) {
                itemNumberText.setText(itemView.getContext().getString(R.string.question_item_number_format, item.getQid()));
                questionCategoryTitle.setText(item.getQuestionCategory());
                
                categoryIcon.setImageResource(R.drawable.outline_assignment_24);
                completionCheckmark.setVisibility(item.isCompleted() ? View.VISIBLE : View.GONE);
                
                divider.setVisibility(isLastItem ? View.GONE : View.VISIBLE);

                itemView.setOnClickListener(v -> {
                    String qid = item.getQid(); // Get qid

                    // Check if qid is valid before logging and navigating
                    if (qid == null || qid.isEmpty() || qid.equals("UNKNOWN_QID")) {
                        Log.e(TAG, "Invalid QID for navigation: " + qid + " for item: " + item.getQuestionCategory());
                        Toast.makeText(itemView.getContext(), "問題IDが無効または見つかりません", Toast.LENGTH_SHORT).show();
                        return; // Stop navigation
                    }

                    Log.d(TAG, "Navigating to QuizFragment with qid: " + qid);

                    // Add navigation logic here
                    Bundle bundle = new Bundle();
                    bundle.putString("qid", qid); // Pass the valid qid
                    bundle.putBoolean("isRandomMode", false); // Explicitly pass isRandomMode
                    try {
                        Navigation.findNavController(v).navigate(R.id.action_questionCategoryFragment_to_quizFragment, bundle);
                    } catch (IllegalArgumentException e) {
                        Log.e(TAG, "Navigation failed (QuestionCategory -> Quiz): ", e);
                        Toast.makeText(itemView.getContext(), "画面遷移に失敗しました", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }
}
