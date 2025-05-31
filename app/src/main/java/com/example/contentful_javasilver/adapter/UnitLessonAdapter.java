package com.example.contentful_javasilver.adapter;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.contentful_javasilver.R;
import com.example.contentful_javasilver.model.UnitLessonItem;

import java.util.ArrayList;
import java.util.List;

public class UnitLessonAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_UNIT = 0;
    private static final int VIEW_TYPE_LESSON = 1;

    private final List<UnitLessonItem> items;
    private final OnLessonClickListener lessonClickListener;

    public UnitLessonAdapter(List<UnitLessonItem> items, OnLessonClickListener lessonClickListener) {
        this.items = items;
        this.lessonClickListener = lessonClickListener;
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).isUnitHeader() ? VIEW_TYPE_UNIT : VIEW_TYPE_LESSON;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_UNIT) {
            View view = inflater.inflate(R.layout.item_unit_header, parent, false);
            return new UnitViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_lesson, parent, false);
            return new LessonViewHolder(view, lessonClickListener);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        UnitLessonItem item = items.get(position);
        if (holder instanceof UnitViewHolder) {
            ((UnitViewHolder) holder).bind(item);
        } else if (holder instanceof LessonViewHolder) {
            ((LessonViewHolder) holder).bind(item);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void updateItems(List<UnitLessonItem> newItems) {
        this.items.clear();
        this.items.addAll(newItems);
        notifyDataSetChanged();
    }

    /**
     * 現在のアイテムリストを取得します
     * @return 現在のアイテムリスト
     */
    public List<UnitLessonItem> getItems() {
        return new ArrayList<>(items); // 防御的コピーを返す
    }

    static class UnitViewHolder extends RecyclerView.ViewHolder {
        private final TextView unitButton;
        private final TextView lessonsProgress;
        private final TextView topicTitle;
        private final TextView topicDescription;
        private final View unitDivider;

        public UnitViewHolder(@NonNull View itemView) {
            super(itemView);
            unitButton = itemView.findViewById(R.id.unit_button);
            lessonsProgress = itemView.findViewById(R.id.lessons_progress);
            topicTitle = itemView.findViewById(R.id.topic_title);
            topicDescription = itemView.findViewById(R.id.topic_description);
            unitDivider = itemView.findViewById(R.id.unit_divider);
        }

        public void bind(UnitLessonItem item) {
            Context context = itemView.getContext();
            unitButton.setText(context.getString(R.string.unit_number_format, item.getUnitNumber()));
            lessonsProgress.setText(context.getString(
                    R.string.lessons_progress_format,
                    item.getCompletedLessons(),
                    item.getTotalLessons())
            );
            topicTitle.setText(item.getTitle());
            topicDescription.setText(item.getDescription());

            // ユニット番号に応じた色とDrawableリソースIDを取得
            int unitColor;
            int unitButtonBgResId;
            switch (item.getUnitNumber()) {
                case 1:
                    unitColor = ContextCompat.getColor(context, R.color.unit_1_color);
                    unitButtonBgResId = R.drawable.unit_button_bg_1; // Get drawable ID
                    break;
                case 2:
                    unitColor = ContextCompat.getColor(context, R.color.unit_2_color);
                    unitButtonBgResId = R.drawable.unit_button_bg_2;
                    break;
                case 3:
                    unitColor = ContextCompat.getColor(context, R.color.unit_3_color);
                    unitButtonBgResId = R.drawable.unit_button_bg_3;
                    break;
                case 4:
                    unitColor = ContextCompat.getColor(context, R.color.unit_4_color);
                    unitButtonBgResId = R.drawable.unit_button_bg_4;
                    break;
                case 5:
                    unitColor = ContextCompat.getColor(context, R.color.unit_5_color);
                    unitButtonBgResId = R.drawable.unit_button_bg_5;
                    break;
                case 6:
                    unitColor = ContextCompat.getColor(context, R.color.unit_6_color);
                    unitButtonBgResId = R.drawable.unit_button_bg_6;
                    break;
                default:
                    unitColor = ContextCompat.getColor(context, R.color.unit_default_color);
                    unitButtonBgResId = R.drawable.unit_button_bg; // Default drawable ID
                    break;
            }

            // 背景Drawableを取得し、色を設定 (テーマの影響を避ける)
            Drawable buttonBackground = ContextCompat.getDrawable(context, R.drawable.unit_button_bg); // Use the base drawable
            if (buttonBackground != null) {
                Drawable mutableBackground = buttonBackground.mutate(); // Create a mutable copy
                // Apply the specific unit color using SRC_IN to color the shape
                mutableBackground.setColorFilter(unitColor, PorterDuff.Mode.SRC_IN);
                unitButton.setBackground(mutableBackground);
            } else {
                 // Fallback: Set background color directly if drawable is null (less ideal)
                 unitButton.setBackgroundColor(unitColor);
            }

            // 最後のユニットの場合は区切り線を非表示にする
            if (item.isLastUnit()) {
                unitDivider.setVisibility(View.GONE);
            } else {
                unitDivider.setVisibility(View.VISIBLE);
            }
        }
    }

    static class LessonViewHolder extends RecyclerView.ViewHolder {
        private final ImageView lessonIcon;
        private final TextView lessonNumber;
        private final TextView lessonTitle;
        private final TextView lessonCategory;
        private final ImageView completionCheckmark;
        private final OnLessonClickListener listener;

        public LessonViewHolder(@NonNull View itemView, OnLessonClickListener listener) {
            super(itemView);
            lessonIcon = itemView.findViewById(R.id.lesson_icon);
            lessonNumber = itemView.findViewById(R.id.lesson_number);
            lessonTitle = itemView.findViewById(R.id.lesson_title);
            lessonCategory = itemView.findViewById(R.id.lesson_category);
            completionCheckmark = itemView.findViewById(R.id.completion_checkmark);
            this.listener = listener;
        }

        public void bind(UnitLessonItem item) {
            Context context = itemView.getContext();
            lessonIcon.setImageResource(item.getIconResId());
            String lessonQidText = "Lesson " + item.getUnitNumber() + "-" + item.getLessonNumber();
            lessonNumber.setText(lessonQidText);
            lessonTitle.setText(item.getTitle());
            lessonCategory.setText(item.getCategory());

            // ユニット番号に応じたアイコン色を設定
            int iconTintColor;
            switch (item.getUnitNumber()) {
                case 1:
                    iconTintColor = ContextCompat.getColor(context, R.color.unit_1_color);
                    break;
                case 2:
                    iconTintColor = ContextCompat.getColor(context, R.color.unit_2_color);
                    break;
                case 3:
                    iconTintColor = ContextCompat.getColor(context, R.color.unit_3_color);
                    break;
                case 4:
                    iconTintColor = ContextCompat.getColor(context, R.color.unit_4_color);
                    break;
                case 5:
                    iconTintColor = ContextCompat.getColor(context, R.color.unit_5_color);
                    break;
                case 6:
                    iconTintColor = ContextCompat.getColor(context, R.color.unit_6_color);
                    break;
                default:
                    iconTintColor = ContextCompat.getColor(context, R.color.unit_default_color);
                    break;
            }
            // PorterDuff.Mode.SRC_IN ensures the icon shape is filled with the tint color
            lessonIcon.setColorFilter(iconTintColor, PorterDuff.Mode.SRC_IN);

            if (item.isCompleted()) {
                completionCheckmark.setVisibility(View.VISIBLE);
            } else {
                completionCheckmark.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onLessonClick(item);
                }
            });
        }
    }

    public interface OnLessonClickListener {
        void onLessonClick(UnitLessonItem item);
    }
} 