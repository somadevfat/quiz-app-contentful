package com.example.contentful_javasilver.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.contentful_javasilver.R;
import com.example.contentful_javasilver.model.CategoryItem;

public class CategoryAdapter extends ListAdapter<CategoryItem, CategoryAdapter.CategoryViewHolder> {
    private final CategoryClickListener listener;

    public interface CategoryClickListener {
        void onCategoryClick(CategoryItem category);
    }

    public CategoryAdapter(CategoryClickListener listener) {
        super(new DiffUtil.ItemCallback<CategoryItem>() {
            @Override
            public boolean areItemsTheSame(@NonNull CategoryItem oldItem, @NonNull CategoryItem newItem) {
                // カテゴリ名と章番号で一意性を判断 (より厳密なIDがあればそれが望ましい)
                return oldItem.getCategoryName().equals(newItem.getCategoryName()) && 
                       oldItem.getChapterNumber() == newItem.getChapterNumber();
            }

            @Override
            public boolean areContentsTheSame(@NonNull CategoryItem oldItem, @NonNull CategoryItem newItem) {
                // オブジェクトの内容が変更されたか比較
                return oldItem.equals(newItem); // CategoryItemにequals()の実装が必要
            }
        });
        this.listener = listener;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // ItemCategoryBindingではなく、通常のLayoutInflaterを使用
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        CategoryItem category = getItem(position);
        // 最後のアイテムかどうかを判定し、区切り線の表示を制御
        boolean isLastItem = (position == getItemCount() - 1);
        holder.bind(category, listener, isLastItem);
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        // ビューへの参照を item_category.xml に合わせる
        private final ImageView categoryIcon;
        private final TextView categoryTitle;
        private final TextView categoryProgress;
        private final View categoryDivider;

        CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            // 新しいIDでビューを検索
            categoryIcon = itemView.findViewById(R.id.category_icon);
            categoryTitle = itemView.findViewById(R.id.category_title);
            categoryProgress = itemView.findViewById(R.id.category_progress);
            categoryDivider = itemView.findViewById(R.id.category_divider);
        }

        void bind(CategoryItem category, CategoryClickListener listener, boolean isLastItem) {
            Context context = itemView.getContext();
            
            // アイコンを outline_folder_24 に設定
            categoryIcon.setImageResource(R.drawable.outline_folder_24);
            
            // タイトルを設定
            categoryTitle.setText(category.getCategoryName());

            // categoryDescription を常に非表示にする
            // categoryDescription.setVisibility(View.GONE); // ここで非表示に設定
            
            // 進捗テキストを設定
            categoryProgress.setText(context.getString(
                R.string.questions_progress_format, 
                category.getCompletedQuestions(),
                category.getTotalQuestions()
            ));

            // 区切り線の表示/非表示を設定
            categoryDivider.setVisibility(isLastItem ? View.GONE : View.VISIBLE);

            // クリックリスナーを設定
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCategoryClick(category);
                }
            });
        }
    }
} 