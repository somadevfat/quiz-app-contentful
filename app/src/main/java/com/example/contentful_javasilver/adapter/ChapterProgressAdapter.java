package com.example.contentful_javasilver.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.example.contentful_javasilver.R;
import com.example.contentful_javasilver.data.ChapterProgressItem;
import java.util.Locale;

public class ChapterProgressAdapter extends ListAdapter<ChapterProgressItem, ChapterProgressAdapter.ProgressViewHolder> {

    public ChapterProgressAdapter() {
        super(DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<ChapterProgressItem> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<ChapterProgressItem>() {
        @Override
        public boolean areItemsTheSame(@NonNull ChapterProgressItem oldItem, @NonNull ChapterProgressItem newItem) {
            // ユニークなIDがあればそれで比較、なければ名前で比較
            return oldItem.getChapterName().equals(newItem.getChapterName());
        }

        @Override
        public boolean areContentsTheSame(@NonNull ChapterProgressItem oldItem, @NonNull ChapterProgressItem newItem) {
            return oldItem.getCompletionPercentage() == newItem.getCompletionPercentage() &&
                   oldItem.getAnsweredCount() == newItem.getAnsweredCount() &&
                   oldItem.getTotalProblemsInChapter() == newItem.getTotalProblemsInChapter();
        }
    };

    @NonNull
    @Override
    public ProgressViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_chapter_progress, parent, false);
        return new ProgressViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ProgressViewHolder holder, int position) {
        ChapterProgressItem currentItem = getItem(position);
        holder.bind(currentItem);
    }

    static class ProgressViewHolder extends RecyclerView.ViewHolder {
        private final TextView chapterNameTextView;
        private final TextView accuracyPercentageTextView;
        private final ProgressBar chapterProgressBar;
        private final TextView progressCountTextView;

        public ProgressViewHolder(@NonNull View itemView) {
            super(itemView);
            chapterNameTextView = itemView.findViewById(R.id.chapterNameTextView);
            accuracyPercentageTextView = itemView.findViewById(R.id.accuracyPercentageTextView);
            chapterProgressBar = itemView.findViewById(R.id.chapterProgressBar);
            progressCountTextView = itemView.findViewById(R.id.progressCountTextView);
        }

        public void bind(ChapterProgressItem item) {
            chapterNameTextView.setText(item.getChapterName());
            accuracyPercentageTextView.setText(String.format(Locale.JAPAN, "%d%%", item.getCompletionPercentage()));
            chapterProgressBar.setProgress(item.getCompletionPercentage());
            progressCountTextView.setText(String.format(Locale.JAPAN, "%d/%d問", item.getAnsweredCount(), item.getTotalProblemsInChapter()));
        }
    }
} 