package com.example.contentful_javasilver.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface QuizDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<QuizEntity> quizzes);

    @Query("SELECT * FROM quizzes")
    List<QuizEntity> getAllQuizzes();

    /**
     * 全てのクイズを章番号（数値として）とQID（文字列として）でソートして取得します。
     * @return ソートされたクイズエンティティのリスト
     */
    @Query("SELECT * FROM quizzes ORDER BY CAST(chapter AS INTEGER) ASC, qid ASC")
    List<QuizEntity> getAllQuizzesSorted();

    @Query("SELECT * FROM quizzes WHERE rowid IN (SELECT rowid FROM quizzes ORDER BY RANDOM() LIMIT :count)")
    LiveData<List<QuizEntity>> getRandomQuizzes(int count);

    @Query("SELECT * FROM quizzes WHERE rowid IN (SELECT rowid FROM quizzes ORDER BY RANDOM() LIMIT :count)")
    List<QuizEntity> getRandomQuizzesSync(int count);

    @Query("SELECT COUNT(*) FROM quizzes")
    LiveData<Integer> getQuizCount();

    @Query("SELECT COUNT(*) FROM quizzes")
    int getQuizCountSync();

    @Query("SELECT * FROM quizzes WHERE category = :category ORDER BY qid ASC")
    LiveData<List<QuizEntity>> getQuizzesByCategory(String category);

    @Query("SELECT * FROM quizzes WHERE category = :category ORDER BY RANDOM()") // カテゴリで同期的に取得し、ランダムに並び替え
    List<QuizEntity> getQuizzesByCategorySync(String category);

    @Query("SELECT * FROM quizzes WHERE category = :category ORDER BY RANDOM() LIMIT :count") // カテゴリでフィルタし、ランダムに指定件数取得
    List<QuizEntity> getRandomQuizzesByCategorySync(String category, int count);

    @Query("SELECT * FROM quizzes WHERE qid = :qid")
    LiveData<List<QuizEntity>> getQuizzesByQidLive(String qid);

    @Query("SELECT * FROM quizzes WHERE chapter = :chapter AND category = :category")
    LiveData<List<QuizEntity>> getQuizzesByChapterAndCategory(String chapter, String category);

    @Query("SELECT * FROM quizzes WHERE qid = :qid")
    List<QuizEntity> getQuizzesByQid(String qid);

    /**
     * 指定された章パターンに一致するカテゴリのリストを取得します。
     * 例: "Unit 1 / %" は Unit 1 のすべてのカテゴリを返します。
     * @param chapterPattern "Unit X / %" のようなパターン文字列 -> 修正: 正確な章名 (例: "1章")
     * @return 一致するカテゴリ名のリスト (重複なし、昇順ソート)
     */
    @Query("SELECT DISTINCT category FROM quizzes WHERE chapter = :chapterPattern ORDER BY category ASC")
    List<String> getCategoriesByChapterPattern(String chapterPattern);

    /**
     * Gets the qid of a single random quiz synchronously.
     * @return The qid of a random quiz, or null if the table is empty.
     */
    @Query("SELECT qid FROM quizzes ORDER BY RANDOM() LIMIT 1")
    String getRandomQuizIdSync();

    @Query("DELETE FROM quizzes")
    void deleteAll();

    // --- Methods for Bookmarks ---

    /**
     * Updates the bookmark status for a specific quiz.
     * @param qid The ID of the quiz to update.
     * @param isBookmarked The new bookmark status.
     */
    @Query("UPDATE quizzes SET isBookmarked = :isBookmarked WHERE qid = :qid")
    void updateBookmarkStatus(String qid, boolean isBookmarked);

    /**
     * Retrieves all bookmarked quizzes, ordered by QID.
     * @return A LiveData list of bookmarked QuizEntity objects.
     */
    @Query("SELECT * FROM quizzes WHERE isBookmarked = 1 ORDER BY qid ASC")
    LiveData<List<QuizEntity>> getBookmarkedQuizzes();

    /**
     * Resets the bookmark status for all quizzes to false.
     */
    @Query("UPDATE quizzes SET isBookmarked = 0")
    void resetAllBookmarks();

    // --- Methods for QuizHistory ---

    /**
     * Inserts a single quiz answer history record.
     * @param history The QuizHistory object to insert.
     */
    @Insert
    void insertHistory(QuizHistory history);

    /**
     * Retrieves all quiz history records, ordered by timestamp descending (newest first).
     * @return A LiveData list of all QuizHistory records.
     */
    @Query("SELECT * FROM quiz_history ORDER BY timestamp DESC")
    LiveData<List<QuizHistory>> getAllHistorySortedByTimestampDesc();

    /**
     * Retrieves all quiz history records synchronously.
     * @return A list of all QuizHistory records.
     */
    @Query("SELECT * FROM quiz_history")
    List<QuizHistory> getAllHistorySync();

    /**
     * Retrieves statistics for each problem (qid) based on the quiz history.
     * Calculates the count of correct and incorrect answers for each problemId.
     * @return A LiveData list of ProblemStats objects, ordered by problemId.
     */
    @Query("SELECT problemId, " +
           "SUM(CASE WHEN isCorrect = 1 THEN 1 ELSE 0 END) as correctCount, " +
           "SUM(CASE WHEN isCorrect = 0 THEN 1 ELSE 0 END) as incorrectCount " +
           "FROM quiz_history GROUP BY problemId ORDER BY problemId ASC")
    LiveData<List<ProblemStats>> getProblemStatistics();

    /**
     * Checks if a problem has been answered at least once.
     * @param problemId The ID of the problem (qid).
     * @return true if the problem has an entry in the history, false otherwise.
     */
    @Query("SELECT COUNT(*) > 0 FROM quiz_history WHERE problemId = :problemId")
    boolean isProblemAnswered(String problemId);

    // --- Methods for Home Screen Statistics ---

    /**
     * Gets the total number of answered questions from the history.
     * @return Total answer count.
     */
    @Query("SELECT COUNT(*) FROM quiz_history")
    int getTotalAnswerCountSync();

    /**
     * Gets the number of questions answered since the given timestamp.
     * @param startTimeMillis The start timestamp (e.g., start of the week).
     * @return Count of answers since the start time.
     */
    @Query("SELECT COUNT(*) FROM quiz_history WHERE timestamp >= :startTimeMillis")
    int getWeeklyAnswerCountSync(long startTimeMillis);

    /**
     * Gets the count of distinct days with answer history since the given timestamp.
     * @param startTimeMillis The start timestamp.
     * @return Count of distinct answer days.
     */
    @Query("SELECT COUNT(DISTINCT DATE(timestamp / 1000, 'unixepoch', 'localtime')) FROM quiz_history WHERE timestamp >= :startTimeMillis")
    int getDistinctAnswerDaysCount(long startTimeMillis);

    /**
     * Retrieves all distinct dates (YYYY-MM-DD) from the history, sorted descending.
     * Used for calculating the current learning streak.
     * @return A list of distinct dates as strings.
     */
    @Query("SELECT DISTINCT DATE(timestamp / 1000, 'unixepoch', 'localtime') as answerDate FROM quiz_history ORDER BY answerDate DESC")
    List<String> getAllAnswerDaysSortedDesc();

    /**
     * Gets the number of questions answered between the given timestamps (exclusive of end time).
     * Used for calculating daily counts.
     * @param startTimeMillis The start timestamp (inclusive).
     * @param endTimeMillis The end timestamp (exclusive).
     * @return Count of answers within the time range.
     */
    @Query("SELECT COUNT(*) FROM quiz_history WHERE timestamp >= :startTimeMillis AND timestamp < :endTimeMillis")
    int getWeeklyAnswerCountSync(long startTimeMillis, long endTimeMillis);

    // If needed, add methods to delete history, etc.
    // @Query("DELETE FROM quiz_history")
    // void deleteAllHistory();

    // ★★★ 全てのQIDを同期的に取得するメソッドを追加 ★★★
    @Query("SELECT qid FROM quizzes")
    List<String> getAllQidsSync();

    // ★★★ 指定されたQIDのQuizEntityを同期的に取得するメソッドを追加 (LIMIT 1) ★★★
    @Query("SELECT * FROM quizzes WHERE qid = :qid LIMIT 1")
    QuizEntity getQuizByQidSync(String qid);

    // --- クエリ追加ここから ---
    /**
     * 指定された章のユニークな questionCategory のリストを取得します。
     * @param chapter 章番号 (文字列)
     * @return ユニークなカテゴリ名のリスト
     */
    @Query("SELECT DISTINCT questionCategory FROM quizzes WHERE chapter = :chapter")
    List<String> getDistinctCategoriesForChapter(String chapter);

    /**
     * 指定された章で正解した問題のユニークな questionCategory のリストを取得します。
     * @param chapter 章番号 (文字列)
     * @return 完了したカテゴリ名のリスト
     */
    @Query("SELECT DISTINCT q.questionCategory FROM quizzes q JOIN quiz_history h ON q.qid = h.problemId WHERE q.chapter = :chapter AND h.isCorrect = 1")
    List<String> getCompletedCategoriesForChapter(String chapter);
    // --- クエリ追加ここまで ---
}
