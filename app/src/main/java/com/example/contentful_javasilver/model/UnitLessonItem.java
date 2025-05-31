package com.example.contentful_javasilver.model;

/**
 * ユニットヘッダーまたはレッスンアイテムを表すデータモデル
 */
public class UnitLessonItem {
    
    private final boolean isUnitHeader;
    private final int unitNumber;
    private final int lessonNumber;
    private final String title;
    private final String description;
    private final String category;
    private final int iconResId;
    private final boolean isCompleted;
    private final int completedLessons;
    private final int totalLessons;
    private final boolean isLastUnit;
    private final String qid; // クイズID（レッスンを開くときに使用）

    /**
     * ユニットヘッダー用コンストラクタ
     */
    public UnitLessonItem(int unitNumber, String title, String description,
                         int completedLessons, int totalLessons, boolean isLastUnit) {
        this.isUnitHeader = true;
        this.unitNumber = unitNumber;
        this.lessonNumber = 0;
        this.title = title;
        this.description = description;
        this.category = "";
        this.iconResId = 0;
        this.isCompleted = false;
        this.completedLessons = completedLessons;
        this.totalLessons = totalLessons;
        this.isLastUnit = isLastUnit;
        this.qid = "";
    }

    /**
     * レッスンアイテム用コンストラクタ
     */
    public UnitLessonItem(int unitNumber, int lessonNumber, String title, String category,
                         int iconResId, boolean isCompleted, String qid) {
        this.isUnitHeader = false;
        this.unitNumber = unitNumber;
        this.lessonNumber = lessonNumber;
        this.title = title;
        this.description = "";
        this.category = category;
        this.iconResId = iconResId;
        this.isCompleted = isCompleted;
        this.completedLessons = 0;
        this.totalLessons = 0;
        this.isLastUnit = false;
        this.qid = qid;
    }

    public boolean isUnitHeader() {
        return isUnitHeader;
    }

    public int getUnitNumber() {
        return unitNumber;
    }

    public int getLessonNumber() {
        return lessonNumber;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public int getIconResId() {
        return iconResId;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public int getCompletedLessons() {
        return completedLessons;
    }

    public int getTotalLessons() {
        return totalLessons;
    }

    public boolean isLastUnit() {
        return isLastUnit;
    }

    public String getQid() {
        return qid;
    }
} 