package com.example.contentful_javasilver.model;

import java.util.Objects;

/**
 * カテゴリ選択画面で表示するカテゴリの情報を保持するデータモデル
 */
public class CategoryItem {
    private final String categoryName;
    private final int totalQuestions;
    private final int completedQuestions;
    private final int iconResId; // カテゴリの種類を示すアイコン (仮)
    private final int chapterNumber; // どの章に属するか

    public CategoryItem(String categoryName, int totalQuestions, int completedQuestions, int iconResId, int chapterNumber) {
        this.categoryName = categoryName;
        this.totalQuestions = totalQuestions;
        this.completedQuestions = completedQuestions;
        this.iconResId = iconResId;
        this.chapterNumber = chapterNumber;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public int getTotalQuestions() {
        return totalQuestions;
    }

    public int getCompletedQuestions() {
        return completedQuestions;
    }

    public int getIconResId() {
        return iconResId;
    }
    
    public int getChapterNumber() {
        return chapterNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CategoryItem that = (CategoryItem) o;
        return totalQuestions == that.totalQuestions &&
                completedQuestions == that.completedQuestions &&
                iconResId == that.iconResId &&
                chapterNumber == that.chapterNumber &&
                Objects.equals(categoryName, that.categoryName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(categoryName, totalQuestions, completedQuestions, iconResId, chapterNumber);
    }
} 