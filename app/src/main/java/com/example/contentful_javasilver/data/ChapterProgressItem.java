package com.example.contentful_javasilver.data;

/**
 * RecyclerView で章ごとの進捗を表示するためのデータクラス
 * (進捗率、解答済み問題数、章の総問題数を保持)
 */
public class ChapterProgressItem {
    private final String chapterName;
    private final int completionPercentage;
    private final int answeredCount;
    private final int totalProblemsInChapter;

    public ChapterProgressItem(String chapterName, int completionPercentage, int answeredCount, int totalProblemsInChapter) {
        this.chapterName = chapterName;
        this.completionPercentage = completionPercentage;
        this.answeredCount = answeredCount;
        this.totalProblemsInChapter = totalProblemsInChapter;
    }

    public String getChapterName() {
        return chapterName;
    }

    public int getCompletionPercentage() {
        return completionPercentage;
    }

    public int getAnsweredCount() {
        return answeredCount;
    }

    public int getTotalProblemsInChapter() {
        return totalProblemsInChapter;
    }
} 