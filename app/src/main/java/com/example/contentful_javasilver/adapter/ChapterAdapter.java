package com.example.contentful_javasilver.adapter;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.contentful_javasilver.R;
import com.example.contentful_javasilver.model.ChapterItem;

import java.util.List;

public class ChapterAdapter extends RecyclerView.Adapter<ChapterAdapter.ChapterViewHolder> {

    private final List<ChapterItem> chapters;
    private final OnChapterClickListener listener;

    public ChapterAdapter(List<ChapterItem> chapters, OnChapterClickListener listener) {
        this.chapters = chapters;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ChapterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chapter, parent, false);
        return new ChapterViewHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull ChapterViewHolder holder, int position) {
        holder.bind(chapters.get(position));
    }

    @Override
    public int getItemCount() {
        return chapters.size();
    }

    public void updateItems(List<ChapterItem> newChapters) {
        this.chapters.clear();
        this.chapters.addAll(newChapters);
        notifyDataSetChanged();
    }

    static class ChapterViewHolder extends RecyclerView.ViewHolder {
        private final TextView chapterButton;
        private final TextView chapterProgress;
        private final TextView chapterTopicTitle;
        private final TextView chapterTopicDescription;
        private final View chapterDivider;
        private final OnChapterClickListener listener;

        ChapterViewHolder(@NonNull View itemView, OnChapterClickListener listener) {
            super(itemView);
            chapterButton = itemView.findViewById(R.id.chapter_button);
            chapterProgress = itemView.findViewById(R.id.chapter_progress);
            chapterTopicTitle = itemView.findViewById(R.id.chapter_topic_title);
            chapterTopicDescription = itemView.findViewById(R.id.chapter_topic_description);
            chapterDivider = itemView.findViewById(R.id.chapter_divider);
            this.listener = listener;
        }

        void bind(ChapterItem chapter) {
            Context context = itemView.getContext();
            
            chapterButton.setText(context.getString(R.string.unit_format, chapter.getChapterNumber()));
            
            int unitButtonBgResId;
            switch (chapter.getChapterNumber()) {
                case 1:
                    unitButtonBgResId = R.drawable.unit_button_bg_1;
                    break;
                case 2:
                    unitButtonBgResId = R.drawable.unit_button_bg_2;
                    break;
                case 3:
                    unitButtonBgResId = R.drawable.unit_button_bg_3;
                    break;
                case 4:
                    unitButtonBgResId = R.drawable.unit_button_bg_4;
                    break;
                case 5:
                    unitButtonBgResId = R.drawable.unit_button_bg_5;
                    break;
                case 6:
                    unitButtonBgResId = R.drawable.unit_button_bg_6;
                    break;
                default:
                    unitButtonBgResId = R.drawable.unit_button_bg;
                    break;
            }

            chapterButton.setBackgroundResource(unitButtonBgResId);
            
            chapterProgress.setText(context.getString(
                    R.string.categories_progress_format,
                    chapter.getCompletedCategories(),
                    chapter.getTotalCategories())
            );
            
            chapterTopicTitle.setText(chapter.getTitle());
            chapterTopicDescription.setText(chapter.getDescription());
            
            chapterDivider.setVisibility(View.VISIBLE);
            
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onChapterClick(chapter);
                }
            });
        }
    }

    public interface OnChapterClickListener {
        void onChapterClick(ChapterItem chapter);
    }
} 