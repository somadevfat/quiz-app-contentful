package com.example.contentful_javasilver.model;

/**
 * チャプター選択画面で表示する章の情報を保持するデータモデル
 */
public class ChapterItem {
    
    private final int chapterNumber;
    private final String title;
    private final String description;
    private final int totalCategories;
    private final int completedCategories;
    private final int colorResId;
    private final int iconResId;

    /**
     * チャプターアイテム用コンストラクタ
     */
    public ChapterItem(int chapterNumber, String title, String description, 
                  int totalCategories, int completedCategories, 
                  int colorResId, int iconResId) {
        this.chapterNumber = chapterNumber;
        this.title = title;
        this.description = description;
        this.totalCategories = totalCategories;
        this.completedCategories = completedCategories;
        this.colorResId = colorResId;
        this.iconResId = iconResId;
    }

    public int getChapterNumber() {
        return chapterNumber;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public int getTotalCategories() {
        return totalCategories;
    }

    public int getCompletedCategories() {
        return completedCategories;
    }

    public int getColorResId() {
        return colorResId;
    }

    public int getIconResId() {
        return iconResId;
    }
} 