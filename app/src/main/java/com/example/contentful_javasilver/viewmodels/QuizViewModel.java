package com.example.contentful_javasilver.viewmodels;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelKt;

import com.contentful.java.cda.CDAEntry;
import com.example.contentful_javasilver.AsyncHelperCoroutines;
import com.example.contentful_javasilver.ContentfulGetApi;
import com.example.contentful_javasilver.DatabaseHelperCoroutines;
import com.example.contentful_javasilver.data.QuizDao;
import com.example.contentful_javasilver.data.QuizDatabase;
import com.example.contentful_javasilver.data.QuizEntity;
import com.example.contentful_javasilver.data.QuizHistory;
import com.example.contentful_javasilver.utils.SecurePreferences;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import kotlin.Unit;
import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.CoroutineStart;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.flow.MutableStateFlow;
import kotlinx.coroutines.flow.StateFlow;
import kotlinx.coroutines.flow.StateFlowKt;

/**
 * クイズデータを管理するViewModel
 */
public class QuizViewModel extends AndroidViewModel {
    private static final String TAG = "QuizViewModel";
    private static final String PREF_BEST_STREAK = "best_streak"; // Key for SharedPreferences
    private static final String PREF_WEEKLY_GOAL = "weekly_goal_preference"; // Key for weekly goal
    private static final String PREF_LAST_SOLVED_QID = "last_solved_qid"; // Key for last solved qid
    private static final String PREF_TODAY_STUDY_TIME_MILLIS = "today_study_time_millis"; // Key for today's study time
    private static final String PREF_TODAY_STUDY_DATE = "today_study_date"; // Key for the date of today's study time
    private static final int DEFAULT_WEEKLY_GOAL = 50; // Default goal value
    private final QuizDatabase database;
    private final QuizDao quizDao; // Add QuizDao field
    private final DatabaseHelperCoroutines databaseHelper;
    private final AsyncHelperCoroutines asyncHelper;
    private final ContentfulGetApi contentfulApi;
    private final SharedPreferences prefs; // SharedPreferences for best streak

    // Get viewModelScope for Java
    private final CoroutineScope viewModelScope = ViewModelKt.getViewModelScope(this);

    // 表示中のクイズリスト（通常は1件）
    private final MutableLiveData<List<QuizEntity>> loadedQuizzes = new MutableLiveData<>();
    // エラーメッセージ
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    // 正解数 (シーケンシャルモードではあまり意味がないが、一応残す)
    private final MutableLiveData<Integer> correctAnswerCount = new MutableLiveData<>(0);
    // データロード中フラグ
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    // クイズ終了通知用は削除
    // private final MutableLiveData<Boolean> quizFinished = new MutableLiveData<>(false);

    // 現在のクイズを取得するためのMediatorLiveData
    private final MediatorLiveData<QuizEntity> currentQuiz = new MediatorLiveData<>();
    // ランダムに取得したqid
    private final MutableLiveData<String> randomQuizId = new MutableLiveData<>();
    
    // 最後に解いた問題ID
    private final MutableLiveData<String> lastSolvedQuizId = new MutableLiveData<>();

    // --- Problem List Screen ---
    // 全問題リスト
    private final MutableLiveData<List<QuizEntity>> allQuizzes = new MutableLiveData<>();
    // 検索クエリ
    private final MutableLiveData<String> searchQuery = new MutableLiveData<>(""); // Default to empty string
    // グルーピングされた表示用リスト (ヘッダー含む)
    private final MediatorLiveData<List<Object>> groupedProblemList = new MediatorLiveData<>();
    // --- End Problem List Screen ---

    // 削除: chapterProgress LiveData
    // private final MutableLiveData<List<ChapterProgressItem>> chapterProgress = new MutableLiveData<>();

    // 追加: ホーム画面用統計情報 LiveData
    private final MutableLiveData<Integer> weeklyAnswersCount = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> weeklyGoal; // Remove initializer here
    private final MutableLiveData<Integer> totalAnswersCount = new MutableLiveData<>(0);
    // ★★★ streakInfo の型を androidx.core.util.Pair に ★★★
    private final MutableLiveData<androidx.core.util.Pair<Integer, Integer>> streakInfo = new MutableLiveData<>(new androidx.core.util.Pair<>(0, 0));
    private final MutableLiveData<int[]> weeklyDailyAnswerCounts = new MutableLiveData<>(new int[7]);
    // 追加: 今日の学習時間 LiveData (表示用文字列)
    private final MutableLiveData<String> todayStudyTimeLiveData = new MutableLiveData<>("今日の学習時間: 0分");

    // ★★★ 同期された学習情報 LiveData ★★★
    private final MutableLiveData<Date> syncedLastStudyDate = new MutableLiveData<>();
    private final MutableLiveData<Long> syncedContinuousStudyDays = new MutableLiveData<>(0L);

    // --- ランダム出題用の修正 --- 
    private List<String> allQids = null; // 全てのQIDリスト (nullで未ロードを示す)
    private final Random random = new Random(); // ランダム選択用
    // --- 修正ここまで ---

    // Data class to hold answer result details (can be defined as inner class or separate file)
    public static class AnswerResult {
        public final boolean isCorrect;
        public final Set<Integer> correctAnswers; // Set of correct answer indices (0-based)
        public final Set<Integer> userSelections; // Set of user selected indices (0-based)

        public AnswerResult(boolean isCorrect, Set<Integer> correctAnswers, Set<Integer> userSelections) {
            this.isCorrect = isCorrect;
            this.correctAnswers = correctAnswers != null ? Collections.unmodifiableSet(new HashSet<>(correctAnswers)) : Collections.emptySet();
            this.userSelections = userSelections != null ? Collections.unmodifiableSet(new HashSet<>(userSelections)) : Collections.emptySet();
        }
    }

    // --- StateFlows for Compose UI ---
    // Use MutableStateFlow for internal state management
    private final MutableStateFlow<Boolean> _isAnswered =
            StateFlowKt.MutableStateFlow(false);
    // Directly expose MutableStateFlow as StateFlow (since MutableStateFlow implements StateFlow)
    public final StateFlow<Boolean> isAnswered = _isAnswered;

    private final MutableStateFlow<Set<Integer>> _userSelections =
            StateFlowKt.MutableStateFlow(Collections.emptySet());
    // Directly expose MutableStateFlow as StateFlow
    public final StateFlow<Set<Integer>> userSelections = _userSelections;

    // StateFlow for the answer result
    private final MutableStateFlow<AnswerResult> _answerResult =
            StateFlowKt.MutableStateFlow(null);
    // Directly expose MutableStateFlow as StateFlow
    public final StateFlow<AnswerResult> answerResult = _answerResult;

    // --- End StateFlows ---

    // ★★★ Firestore インスタンスを追加 ★★★
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // ★★★ Chapter進捗情報 LiveData (Use androidx.core.util.Pair) ★★★
    // ★★★ 型パラメータの Pair を androidx.core.util.Pair に ★★★
    private final MutableLiveData<Map<Integer, androidx.core.util.Pair<Integer, Integer>>> chapterProgressMapLiveData = new MutableLiveData<>();
    // ★★★ ゲッターの戻り値の型パラメータも androidx.core.util.Pair に ★★★
    public LiveData<Map<Integer, androidx.core.util.Pair<Integer, Integer>>> getChapterProgressMapLiveData() { 
        return chapterProgressMapLiveData; 
    }

    public QuizViewModel(@NonNull Application application) {
        super(application);
        database = QuizDatabase.getDatabase(application);
        quizDao = database.quizDao(); // Initialize QuizDao
        prefs = PreferenceManager.getDefaultSharedPreferences(application); // Init SharedPreferences

        // Initialize weeklyGoal by reading from SharedPreferences
        int savedGoal = prefs.getInt(PREF_WEEKLY_GOAL, DEFAULT_WEEKLY_GOAL);
        weeklyGoal = new MutableLiveData<>(savedGoal);
        
        // Load last solved QID from SharedPreferences
        String savedLastSolvedQid = prefs.getString(PREF_LAST_SOLVED_QID, null);
        lastSolvedQuizId.setValue(savedLastSolvedQid); // Initialize LiveData

        // 安全にAPIキーを取得
        String apiKey = SecurePreferences.getContentfulApiKey(application);
        String spaceId = SecurePreferences.getContentfulSpaceId(application);

        contentfulApi = new ContentfulGetApi(spaceId, apiKey);
        databaseHelper = new DatabaseHelperCoroutines();
        asyncHelper = new AsyncHelperCoroutines(contentfulApi);

        // MediatorLiveDataにソースを追加
        currentQuiz.addSource(loadedQuizzes, quizzes -> updateCurrentQuiz());

        // --- Problem List Screen Sources ---
        groupedProblemList.addSource(allQuizzes, quizzes -> updateGroupedProblemList());
        groupedProblemList.addSource(searchQuery, query -> updateGroupedProblemList());
        // --- End Problem List Screen Sources ---

        // currentQidIndex は不要になったため削除
        // currentQuiz.addSource(currentQidIndex, index -> updateCurrentQuiz());

        // resetAllBookmarkStates(); // Removed: Do not reset bookmarks on every ViewModel creation

        // 変更: 学習統計データと今日の学習時間をロード
        loadStatisticsData();
        loadTodayStudyTime(); // 今日の学習時間をロード
        loadAllQids(); // ViewModel 初期化時に全 QID をロード開始
    }

    // currentQuizを更新するヘルパーメソッド
    private void updateCurrentQuiz() {
        resetQuizState(); // Reset answer state when quiz changes
        List<QuizEntity> quizzes = loadedQuizzes.getValue();
        // loadedQuizzes は常に1件のはずなので、最初の要素をセット
        if (quizzes != null && !quizzes.isEmpty()) {
            currentQuiz.setValue(quizzes.get(0));
            Log.d(TAG, "Setting current quiz: " + quizzes.get(0).getQid());
            // isLoading.setValue(false); // isLoading is now handled by the coroutine in loadQuizByQid
        } else {
            Log.w(TAG, "Quizzes list is null or empty in updateCurrentQuiz (possible error or initial state).");
            currentQuiz.setValue(null); // データがない場合はnullを設定
            // isLoading.setValue(false); // isLoading is now handled by the coroutine in loadQuizByQid
        }
    }

    /**
     * 次のクイズをロードする（ランダムモードかシーケンシャルモードかを判断）
     * @param isRandomMode ランダムモードの場合はtrue
     */
    public void loadNextQuiz(boolean isRandomMode) {
        if (isRandomMode) {
            // ランダムモードの場合は、新しいランダムなqidをロードする
            // loadRandomQuizId() は内部で loadedQuizzes も更新する
            loadRandomQuizId();
        } else {
            // シーケンシャルモードの場合は、次のqidに進む
            moveToNextQuiz();
        }
    }

    // qidリストとインデックスを更新するヘルパーメソッドは不要になったため削除
    // private void updateQidListAndIndex(List<QuizEntity> quizzes) { ... }


    // --- Problem List Screen Logic ---

    /**
     * 全ての問題をデータベースから非同期でロードする
     */
    public void loadAllProblems() {
        isLoading.setValue(true);
        BuildersKt.launch(viewModelScope, Dispatchers.getIO(), CoroutineStart.DEFAULT, (coroutineScope, continuation) -> {
            try {
                List<QuizEntity> quizzes = quizDao.getAllQuizzesSorted();
                allQuizzes.postValue(quizzes);
                Log.d(TAG, "Loaded " + (quizzes != null ? quizzes.size() : 0) + " problems for the list.");
                
                // ★★★ 問題ロード後に進捗計算をトリガー ★★★
                calculateAllChapterProgressAsync(); 
                
            } catch (Exception e) {
                Log.e(TAG, "Error loading all problems", e);
                errorMessage.postValue("問題リストの読み込みに失敗しました: " + e.getMessage());
                allQuizzes.postValue(Collections.emptyList());
            } finally {
                isLoading.postValue(false);
            }
            return Unit.INSTANCE;
        });
    }

    /**
     * 検索クエリを設定する
     * @param query 検索文字列
     */
    public void setSearchQuery(String query) {
        searchQuery.setValue(query == null ? "" : query.trim());
    }

    /**
     * allQuizzes と searchQuery に基づいて groupedProblemList を更新する
     */
    private void updateGroupedProblemList() {
        List<QuizEntity> currentAllQuizzes = allQuizzes.getValue();
        String currentQuery = searchQuery.getValue();

        if (currentAllQuizzes == null) {
            groupedProblemList.setValue(Collections.emptyList());
            return;
        }

        isLoading.setValue(true); // Start loading state for filtering/grouping

        // Replace executor with viewModelScope using BuildersKt.launch
        BuildersKt.launch(viewModelScope, Dispatchers.getIO(), CoroutineStart.DEFAULT, (coroutineScope, continuation) -> {
            List<QuizEntity> filteredList;
            // Filter based on search query
            if (currentQuery == null || currentQuery.isEmpty()) {
                filteredList = new ArrayList<>(currentAllQuizzes); // No filter, use all
            } else {
                String lowerCaseQuery = currentQuery.toLowerCase();
                filteredList = currentAllQuizzes.stream()
                        .filter(quiz -> (quiz.getQid() != null && quiz.getQid().toLowerCase().contains(lowerCaseQuery)) ||
                                        (quiz.getQuestionCategory() != null && quiz.getQuestionCategory().toLowerCase().contains(lowerCaseQuery)))
                        .collect(Collectors.toList());
            }

            // Sort filteredList numerically by qid before grouping
            Collections.sort(filteredList, (q1, q2) -> {
                String qid1 = q1.getQid();
                String qid2 = q2.getQid();
                if (qid1 == null || qid2 == null) return 0; // Handle null qids

                String[] parts1 = qid1.split("-");
                String[] parts2 = qid2.split("-");

                if (parts1.length != 2 || parts2.length != 2) {
                    // Fallback to string comparison for invalid formats
                    return qid1.compareTo(qid2);
                }

                try {
                    int chapter1 = Integer.parseInt(parts1[0]);
                    int num1 = Integer.parseInt(parts1[1]);
                    int chapter2 = Integer.parseInt(parts2[0]);
                    int num2 = Integer.parseInt(parts2[1]);

                    int chapterCompare = Integer.compare(chapter1, chapter2);
                    if (chapterCompare != 0) {
                        return chapterCompare;
                    }
                    return Integer.compare(num1, num2);
                } catch (NumberFormatException e) {
                    // Fallback to string comparison if parsing fails
                    return qid1.compareTo(qid2);
                }
            });


            // Group by chapter and add headers
            Map<String, List<QuizEntity>> groupedMap = new LinkedHashMap<>(); // Use LinkedHashMap to preserve chapter order
            for (QuizEntity quiz : filteredList) {
                // Ensure chapter is not null or empty before creating header
                String chapterStr = quiz.getChapter();
                if (chapterStr != null && !chapterStr.isEmpty()) {
                    String chapterHeader = "第" + chapterStr ; // Correctly format the header
                    groupedMap.computeIfAbsent(chapterHeader, k -> new ArrayList<>()).add(quiz);
                } else {
                    // Handle cases where chapter might be missing (e.g., group under "その他")
                    groupedMap.computeIfAbsent("その他", k -> new ArrayList<>()).add(quiz);
                }
            }

            List<Object> displayList = new ArrayList<>();
            for (Map.Entry<String, List<QuizEntity>> entry : groupedMap.entrySet()) {
                displayList.add(entry.getKey()); // Add header
                displayList.addAll(entry.getValue()); // Add problems for this chapter
            }

            groupedProblemList.postValue(displayList);
            isLoading.postValue(false); // End loading state
            Log.d(TAG, "Updated grouped list. Query: '" + currentQuery + "', Items: " + displayList.size());
            return Unit.INSTANCE; // Return Unit
        });
    }

    // --- End Problem List Screen Logic ---


    /**
     * 指定されたカテゴリのクイズを取得 (ランダムに **1件** )
     * @param category カテゴリ
     */
    public void loadQuizzesByCategory(String category) {
        isLoading.setValue(true);
        correctAnswerCount.setValue(0); // 正解数をリセット
        loadedQuizzes.setValue(new ArrayList<>()); // 表示リストもクリア
        // Replace executor with viewModelScope using BuildersKt.launch
        BuildersKt.launch(viewModelScope, Dispatchers.getIO(), CoroutineStart.DEFAULT, (coroutineScope, continuation) -> {
            try {
                List<QuizEntity> quizzes = quizDao.getRandomQuizzesByCategorySync(category, 1); // Use quizDao field
                Log.d(TAG, "Loaded " + quizzes.size() + " random quiz for category: " + category);

                if (quizzes.isEmpty()) {
                    Log.d(TAG, "No quizzes found in DB for category: " + category + ". Fetching from Contentful.");
                    // Launch fetchFromContentful on Main dispatcher using BuildersKt.launch
                     BuildersKt.launch(viewModelScope, Dispatchers.getMain(), CoroutineStart.DEFAULT, (cScope, cont) -> {
                         fetchFromContentful(category);
                         return Unit.INSTANCE;
                     });
                } else {
                    loadedQuizzes.postValue(quizzes); // LiveDataを更新 (1件のリスト)
                    isLoading.postValue(false);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading quiz by category: " + category, e);
                errorMessage.postValue("データの読み込みに失敗しました: " + e.getMessage());
                isLoading.postValue(false);
            }
            return Unit.INSTANCE; // Return Unit
        });
    }

    /**
     * QIDでクイズを取得 (通常は1件のはず)
     * @param qid クイズID
     */
    public void loadQuizByQid(String qid) {
        Log.d(TAG, "[loadQuizByQid] Method Entered with QID: " + qid);

        if (qid == null || qid.isEmpty()) {
            Log.e(TAG, "[loadQuizByQid] Invalid QID provided: " + qid);
            errorMessage.postValue("無効な問題IDです。");
            return;
        }

        Log.d(TAG, "[loadQuizByQid] Proceeding with load. Setting isLoading to true for QID: " + qid);
        isLoading.setValue(true); // Set loading true only if we are proceeding

        // Use viewModelScope for database operations
        BuildersKt.launch(viewModelScope, Dispatchers.getIO(), CoroutineStart.DEFAULT, (coroutineScope, continuation) -> {
            List<QuizEntity> quizListResult = null; // Declare outside try
            QuizEntity quiz = null; // Declare outside try
            try {
                // ★★★ Add log at the beginning of the try block ★★★
                Log.d(TAG, "[loadQuizByQid IO] Entering try block for QID: " + qid);
                Log.d(TAG, "[loadQuizByQid IO] Querying DB for QID: " + qid);
                // Corrected method call: getQuizzesByQid returns a List
                quizListResult = quizDao.getQuizzesByQid(qid);
                Log.d(TAG, "[loadQuizByQid IO] DB query finished. Result list size: " + (quizListResult != null ? quizListResult.size() : "null"));
                
                if (quizListResult != null && !quizListResult.isEmpty()) {
                    quiz = quizListResult.get(0); // Get the first (and likely only) item
                    Log.d(TAG, "[loadQuizByQid IO] Quiz object retrieved from list: " + quiz.getQid()); // Log retrieved quiz
                } else {
                     Log.w(TAG, "[loadQuizByQid IO] Quiz list is null or empty.");
                }

                if (quiz != null) {
                    Log.d(TAG, "[loadQuizByQid IO] Quiz found in DB. Posting to loadedQuizzes.");
                    List<QuizEntity> quizList = new ArrayList<>();
                    quizList.add(quiz);
                    loadedQuizzes.postValue(quizList);
                } else {
                    Log.w(TAG, "[loadQuizByQid IO] Quiz was null after DB check. Posting error and empty list.");
                    errorMessage.postValue("問題が見つかりませんでした: " + qid);
                    loadedQuizzes.postValue(Collections.emptyList()); // Post empty list when not found
                }
            } catch (Exception e) {
                Log.e(TAG, "[loadQuizByQid IO] Error during DB query or processing for QID: " + qid, e);
                errorMessage.postValue("問題の読み込み中にエラーが発生しました。");
                loadedQuizzes.postValue(Collections.emptyList()); // Ensure list is empty on exception
            } finally {
                // ★★★ Add log at the beginning of the finally block ★★★
                Log.d(TAG, "[loadQuizByQid IO] Entering finally block. Setting isLoading to false for QID: " + qid);
                // Ensure isLoading is set to false AFTER processing, regardless of success/failure/exception
                isLoading.postValue(false);
            }
            return Unit.INSTANCE;
        });
    }

    /**
     * Contentfulからデータを取得し、DBに保存後、指定されたカテゴリ（またはランダム **1件** ）で再読み込み
     * @param categoryToLoadAfterFetch 取得後に読み込むカテゴリ (nullの場合はランダム1件)
     */
    private void fetchFromContentful(String categoryToLoadAfterFetch) {
        Log.d(TAG, "Fetching data from Contentful...");
        asyncHelper.fetchEntriesAsync("javaSilverQ",
            entries -> {
                Log.d(TAG, "Received " + entries.size() + " entries from Contentful");
                List<QuizEntity> entities = new ArrayList<>();
                for (CDAEntry entry : entries) {
                    try {
                        String qid = getField(entry, "qid");
                        String chapter = getField(entry, "chapter");
                        String category = getField(entry, "category");
                        String questionCategory = getField(entry, "questionCategory", getField(entry, "questioncategory", "")); // ネストして取得試行
                        String difficulty = getField(entry, "difficulty");
                        String code = getField(entry, "code");
                        String questionText = getField(entry, "questionText");
                        List<String> choices = entry.getField("choices");
                        List<Double> rawAnswers = entry.getField("answer");
                        List<Integer> intAnswers = new ArrayList<>();
                        if (rawAnswers != null) {
                            for (Double answer : rawAnswers) {
                                if (answer != null) intAnswers.add(answer.intValue());
                            }
                        }
                        String explanation = getField(entry, "explanation");

                        // 必須フィールドチェック
                        if (qid.isEmpty() || questionText.isEmpty() || choices == null || choices.isEmpty() || intAnswers.isEmpty()) {
                            Log.w(TAG, "Skipping entry due to missing required fields: " + entry.id());
                            continue;
                        }

                        // Create QuizEntity with default isBookmarked = false
                        QuizEntity entity = new QuizEntity(
                            qid, chapter, category, questionCategory, difficulty, code,
                            questionText, choices, intAnswers, explanation, false // Pass default bookmark status
                        );
                        entities.add(entity);
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing entry: " + entry.id(), e);
                    }
                }

                if (entities.isEmpty()) {
                    Log.w(TAG, "No valid entities were created from Contentful entries");
                    errorMessage.postValue("有効なデータが見つかりませんでした");
                    isLoading.postValue(false);
                    return Unit.INSTANCE;
                }

                Log.d(TAG, "Saving " + entities.size() + " entities to database");
                asyncHelper.insertQuizEntitiesAsync(database, entities,
                    () -> {
                        Log.d(TAG, "Entities saved successfully. Reloading quiz.");
                        // 保存完了後、指定されたカテゴリまたはランダムで再読み込み
                        if (categoryToLoadAfterFetch != null) {
                            loadQuizzesByCategory(categoryToLoadAfterFetch); // カテゴリ指定で1件読み込み
                        } else {
                            loadRandomQuizzes(1); // ランダムで1件読み込み
                        }
                        return Unit.INSTANCE;
                    },
                    error -> {
                        Log.e(TAG, "Error saving entities: " + error);
                        errorMessage.postValue(error);
                        isLoading.postValue(false);
                        return Unit.INSTANCE;
                    }
                );
                return Unit.INSTANCE;
            },
            error -> {
                Log.e(TAG, "Error fetching from Contentful: " + error);
                errorMessage.postValue(error);
                isLoading.postValue(false);
                return Unit.INSTANCE;
            }
        );
    }

    /**
     * ランダムに **1件** のクイズを取得
     * @param count 取得するクイズ数 (引数は残すが、内部で1に固定)
     */
    public void loadRandomQuizzes(int count) {
        isLoading.setValue(true);
        correctAnswerCount.setValue(0);
        loadedQuizzes.setValue(new ArrayList<>());
        // Replace executor with viewModelScope using BuildersKt.launch
        BuildersKt.launch(viewModelScope, Dispatchers.getIO(), CoroutineStart.DEFAULT, (coroutineScope, continuation) -> {
            try {
                List<QuizEntity> quizzes = quizDao.getRandomQuizzesSync(1); // Use quizDao field, always 1
                Log.d(TAG, "Loaded " + quizzes.size() + " random quiz.");
                if (quizzes.isEmpty()) {
                    Log.d(TAG, "No random quiz found in DB. Fetching from Contentful.");
                     // Launch fetchFromContentful on Main dispatcher using BuildersKt.launch
                     BuildersKt.launch(viewModelScope, Dispatchers.getMain(), CoroutineStart.DEFAULT, (cScope, cont) -> {
                         fetchFromContentful(null);
                         return Unit.INSTANCE;
                     });
                } else {
                    loadedQuizzes.postValue(quizzes); // LiveDataを更新 (1件のリスト)
                    isLoading.postValue(false);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading random quiz", e);
                errorMessage.postValue("データの読み込みに失敗しました: " + e.getMessage());
                isLoading.postValue(false);
            }
             return Unit.INSTANCE; // Return Unit
        });
    }


    /**
     * エントリからフィールドを安全に取得するヘルパーメソッド (デフォルト値付き)
     */
    private String getField(CDAEntry entry, String fieldName, String defaultValue) {
        try {
            Object value = entry.getField(fieldName);
            return value != null ? value.toString() : defaultValue;
        } catch (Exception e) {
            // Log.w(TAG, "Field not found or error getting field " + fieldName + ": " + e.getMessage());
            return defaultValue;
        }
    }
     /**
     * エントリからフィールドを安全に取得するヘルパーメソッド (デフォルト値付き、オーバーロード)
     */
    private String getField(CDAEntry entry, String fieldName) {
        return getField(entry, fieldName, "");
    }


    /**
     * 次のシーケンシャルなQIDのクイズに進む (内部利用メソッドに変更)
     */
    private void moveToNextQuiz() {
        QuizEntity current = currentQuiz.getValue();
        if (current == null || current.getQid() == null || current.getQid().isEmpty()) {
            Log.e(TAG, "Cannot move to next quiz, current quiz or qid is null/empty.");
            errorMessage.postValue("現在のクイズ情報を取得できませんでした。");
            return;
        }

        String currentQid = current.getQid();
        Log.d(TAG, "Current QID for sequential move: " + currentQid);

        // QIDを解析 (例: "1-10")
        String[] parts = currentQid.split("-");
        if (parts.length != 2) {
            Log.e(TAG, "Invalid QID format for sequential move: " + currentQid);
            errorMessage.postValue("無効な問題ID形式です: " + currentQid);
            return;
        }

        try {
            int chapter = Integer.parseInt(parts[0]);
            int questionNum = Integer.parseInt(parts[1]);

            // 次の問題番号を計算
            int nextQuestionNum = questionNum + 1;
            String nextQid = chapter + "-" + nextQuestionNum;
            Log.d(TAG, "Calculated next sequential QID: " + nextQid);

            // --- 修正ここから ---
            // allQids がロードされているか確認
            if (allQids == null || allQids.isEmpty()) {
                 Log.e(TAG, "Cannot check next QID, allQids is not loaded or empty.");
                 errorMessage.postValue("問題リストを読み込み中です。しばらく待ってからもう一度お試しください。");
                 return;
            }

            // 次のQIDが存在するか確認
            if (allQids.contains(nextQid)) {
                Log.d(TAG, "Next QID found: " + nextQid + ". Loading...");
                loadQuizByQid(nextQid);
            } else {
                // 次のQIDが存在しない場合、さらに次のQIDを試す
                Log.d(TAG, "Next QID not found: " + nextQid + ". Trying next-next QID.");
                int nextNextQuestionNum = nextQuestionNum + 1;
                String nextNextQid = chapter + "-" + nextNextQuestionNum;
                Log.d(TAG, "Calculated next-next sequential QID: " + nextNextQid);

                if (allQids.contains(nextNextQid)) {
                     Log.d(TAG, "Next-next QID found: " + nextNextQid + ". Loading...");
                     loadQuizByQid(nextNextQid);
                } else {
                    // 次の次も存在しない場合
                    Log.w(TAG, "Next QID (" + nextQid + ") and next-next QID (" + nextNextQid + ") not found.");
                    errorMessage.postValue("この章の次の問題が見つかりませんでした。"); // または、最初の問題に戻るなどの処理
                    // isLoading.postValue(false); // 必要に応じてローディング状態を解除
                }
            }
             // --- 修正ここまで ---

            // 元のロード処理は削除 (条件分岐内で実行されるため)
            // loadQuizByQid(nextQid);

        } catch (NumberFormatException e) {
            Log.e(TAG, "Error parsing QID for sequential move: " + currentQid, e);
            errorMessage.postValue("問題IDの解析に失敗しました: " + currentQid);
        }
    }


    /**
     * 正解をカウント
     */
    public void incrementCorrectAnswerCount() {
        Integer count = correctAnswerCount.getValue();
        if (count != null) {
            correctAnswerCount.setValue(count + 1);
        }
    }

    /**
     * 表示用のクイズリストLiveData (常に1件のはず)
     */
    public LiveData<List<QuizEntity>> getLoadedQuizzes() {
        return loadedQuizzes;
    }

    /**
     * 現在のクイズLiveData
     */
    public LiveData<QuizEntity> getCurrentQuiz() {
        return currentQuiz;
    }

    /**
     * 現在のクイズインデックスLiveData は不要
     */
     // public LiveData<Integer> getCurrentQuizIndex() { ... }

    // --- Problem List Screen LiveData ---
    /**
     * グルーピングされた表示用リストLiveData
     */
    public LiveData<List<Object>> getGroupedProblemList() {
        return groupedProblemList;
    }
    // --- End Problem List Screen LiveData ---

    /**
     * エラーメッセージLiveData
     */
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    /**
     * 正解数LiveData
     */
    public LiveData<Integer> getCorrectAnswerCount() {
        return correctAnswerCount;
    }

    /**
     * ローディング状態LiveData
     */
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    /**
     * クイズ終了通知LiveData は不要
     */
    // public LiveData<Boolean> getQuizFinished() { ... }

    /**
     * 現在のqidリストのサイズを取得 は不要 (常に1のはず)
     */
    // public int getTotalQuizCount() { ... }

    /**
     * ランダムなクイズIDを取得するためのLiveData
     */
    public LiveData<String> getRandomQuizId() {
        return randomQuizId;
    }

    /**
     * 全てのQIDを非同期でロードし、メモリに保持する
     */
    private void loadAllQids() {
        // Check if list already exists or is being loaded
        if (allQids != null) {
             Log.d(TAG, "[loadAllQids] Skipping load, allQids already exists (size: " + allQids.size() + ")");
             return; // Already loaded
        }
        // Log start
        Log.i(TAG, "[loadAllQids] Starting to load all QIDs from DB..."); // Changed log level

        databaseHelper.loadAllQidsAsync(
            quizDao,
            qids -> {
                 // Log callback start
                 Log.i(TAG, "[loadAllQids] Success callback received.");
                if (qids != null && !qids.isEmpty()) {
                    allQids = new ArrayList<>(qids); // Store the fetched list
                    // Changed log level and added details
                    Log.i(TAG, "[loadAllQids] Successfully loaded " + allQids.size() + " QIDs into memory.");
                } else {
                    // Changed log level
                    Log.w(TAG, "[loadAllQids] Success callback, but loaded QID list is null or empty.");
                    allQids = Collections.emptyList(); // Set empty list
                }
                // Changed log level and kept existing log
                Log.i(TAG, "[loadAllQids] Finished processing success callback. Final allQids size: " + (allQids != null ? allQids.size() : "null"));
                return Unit.INSTANCE; 
            },
            error -> {
                 // Log callback start and error message
                 Log.e(TAG, "[loadAllQids] Error callback received: " + error);
                errorMessage.postValue("問題IDリストの読み込みに失敗しました: " + error);
                allQids = Collections.emptyList(); // Set empty list on error
                 // Log state after error
                 Log.e(TAG, "[loadAllQids] Finished processing error callback. allQids set to empty list.");
                return Unit.INSTANCE;
            }
        );
    }

    /**
     * ランダムなクイズIDをメモリ上のリストから取得し、対応するクイズをロードする
     */
    public void loadRandomQuizId() {
        // ★★★ ログを更新して isLoading のチェック前の状態も見る ★★★
        Log.d(TAG, "[loadRandomQuizId] Called. Current isLoading before check: " + isLoading.getValue() + ", allQids loaded: " + (allQids != null));

        // 既にロード中なら何もしない (念のため)
        if (Boolean.TRUE.equals(isLoading.getValue())) {
            Log.d(TAG, "[loadRandomQuizId] Already loading, skipping request.");
            return;
        }

        // ★★★ ロード開始を通知 (ここに移動) ★★★
        isLoading.setValue(true); 
        Log.d(TAG, "[loadRandomQuizId] Set isLoading=true. Proceeding...");

        if (allQids == null) {
            Log.w(TAG, "[loadRandomQuizId] QID list not loaded yet. Cannot proceed.");
            errorMessage.postValue("問題リスト準備中です。もう一度試してください。");
            isLoading.postValue(false); // ★★★ isLoading を false に設定 ★★★
            return;
        }

        if (allQids.isEmpty()) {
            Log.w(TAG, "[loadRandomQuizId] QID list is empty. Cannot load random quiz.");
            errorMessage.postValue("出題できる問題がありません。");
            isLoading.postValue(false); // ★★★ isLoading を false に設定 ★★★
            return;
        }

        // メモリ上のリストからランダムにqidを選択
        String randomId = allQids.get(random.nextInt(allQids.size()));
        Log.d(TAG, "[loadRandomQuizId] Selected random QID from memory: " + randomId);

        // 選択したqidを使ってクイズをロード
        // loadQuizByQid が isLoading の true -> false を内部で行う
        Log.d(TAG, "[loadRandomQuizId] Calling loadQuizByQid with randomId: " + randomId);
        loadQuizByQid(randomId); // loadQuizByQid は最後に isLoading を false にする
    }

    /**
     * 解答履歴をDBに保存し、統計を更新する
     * ★★★ Firestoreへの保存処理を追加 ★★★
     * @param qid 問題ID
     * @param isCorrect 正解したかどうか
     */
    public void recordAnswerResult(String qid, boolean isCorrect) {
        Log.d(TAG, "recordAnswerResult started for qid: " + qid + ", isCorrect: " + isCorrect);
        
        // 最後に解いた問題IDを保存 (LiveData & SharedPreferences)
        lastSolvedQuizId.postValue(qid);
        // ★★★ ローカルの SharedPreferences への保存は維持 ★★★
        BuildersKt.launch(viewModelScope, Dispatchers.getIO(), CoroutineStart.DEFAULT, (cScope, cont) -> {
            prefs.edit().putString(PREF_LAST_SOLVED_QID, qid).apply();
            return Unit.INSTANCE;
        });
        
        // ★★★ ローカルDBへの保存 (変更なし) ★★★
        BuildersKt.launch(viewModelScope, Dispatchers.getIO(), CoroutineStart.DEFAULT, (coroutineScope, continuation) -> {
            try {
                QuizHistory history = new QuizHistory(qid, isCorrect, System.currentTimeMillis());
                quizDao.insertHistory(history);
                Log.d(TAG, "[recordAnswerResult] Inserted history for problem: " + qid + ", Correct: " + isCorrect + " to LOCAL DB.");

                // ★★★ ローカル統計の更新も維持 ★★★
                Log.d(TAG, "[recordAnswerResult] Calling loadStatisticsData (for local stats)...");
                loadStatisticsData();

                // ★★★ 解答記録後にも進捗再計算をトリガー ★★★
                calculateAllChapterProgressAsync();

                // ★★★ 統計データも再ロード（非同期実行）★★★
                 BuildersKt.launch(viewModelScope, Dispatchers.getMain(), CoroutineStart.DEFAULT, (mainScope, mainContinuation) -> {
                    loadStatisticsData();
                    return Unit.INSTANCE;
                });

            } catch (Exception e) {
                Log.e(TAG, "Error inserting quiz history for problem: " + qid + " to LOCAL DB", e);
                BuildersKt.launch(viewModelScope, Dispatchers.getMain(), CoroutineStart.DEFAULT, (cScope, cont) -> {
                    errorMessage.postValue("解答履歴のローカル保存に失敗しました: " + e.getMessage());
                    return Unit.INSTANCE;
                });
            }
            return Unit.INSTANCE;
        });

        // ★★★ Firestoreへの保存 (ログインユーザーのみ) ★★★
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            long timestamp = System.currentTimeMillis();

            // 1. 解答履歴を Firestore に保存
            Map<String, Object> historyData = new HashMap<>();
            historyData.put("qid", qid);
            historyData.put("isCorrect", isCorrect);
            historyData.put("timestamp", timestamp); // FieldValue.serverTimestamp() も利用可能

            db.collection("users").document(userId)
                .collection("quizHistory") // サブコレクションに保存
                .add(historyData) // 新しいドキュメントとして追加
                .addOnSuccessListener(documentReference ->
                    Log.i(TAG, "[Firestore] Quiz history added for user: " + userId + ", qid: " + qid))
                .addOnFailureListener(e ->
                    Log.e(TAG, "[Firestore] Error adding quiz history for user: " + userId, e));

            // ★★★ ユーザー統計情報 (最終学習日、連続日数) を更新 ★★★
            updateUserStatsInFirestore(userId);

        } else {
             Log.d(TAG, "[recordAnswerResult] User not logged in, skipping Firestore save.");
        }
    }

    /**
     * Toggles the bookmark status of the given quiz entity.
     * Updates the database and the currentQuiz LiveData.
     * ★★★ Firestoreへの保存処理を追加 ★★★
     * @param quiz The QuizEntity to toggle the bookmark status for.
     */
    public void toggleBookmarkStatus(QuizEntity quiz) {
        if (quiz == null || quiz.getQid() == null) {
            Log.w(TAG, "Cannot toggle bookmark status, quiz or qid is null.");
            return;
        }
        String qid = quiz.getQid();
        boolean currentStatus = quiz.isBookmarked();
        boolean newBookmarkStatus = !currentStatus;
        Log.d(TAG, "toggleBookmarkStatus called for qid: " + qid + ". Current: " + currentStatus + ", New: " + newBookmarkStatus);
        quiz.setBookmarked(newBookmarkStatus);

        // ★★★ ローカルDBの更新 (変更なし) ★★★
        BuildersKt.launch(viewModelScope, Dispatchers.getIO(), CoroutineStart.DEFAULT, (coroutineScope, continuation) -> {
            try {
                Log.d(TAG, "Executing LOCAL DB update for qid: " + qid + " with status: " + newBookmarkStatus);
                quizDao.updateBookmarkStatus(qid, newBookmarkStatus);
                Log.d(TAG, "LOCAL DB update successful for qid: " + qid);
                // Post update to LiveData on main thread
                BuildersKt.launch(viewModelScope, Dispatchers.getMain(), CoroutineStart.DEFAULT, (cScope, cont) -> {
                List<QuizEntity> currentList = loadedQuizzes.getValue();
                if (currentList != null && !currentList.isEmpty()) {
                         List<QuizEntity> updatedList = new ArrayList<>(currentList); // Create a mutable copy
                         // Find and update the specific quiz in the list (assuming list contains the current quiz)
                         for(int i=0; i<updatedList.size(); i++){
                             if(updatedList.get(i).getQid().equals(qid)){
                                 updatedList.set(i, quiz); // Update with the modified quiz object
                                 break;
                             }
                         }
                        loadedQuizzes.postValue(updatedList);
                         Log.d(TAG, "Posted updated list to LiveData after LOCAL DB update for qid: " + qid);
                } else {
                        Log.w(TAG, "loadedQuizzes was null or empty when trying to post update after LOCAL DB update for qid: " + qid);
                }
                    return Unit.INSTANCE;
                });
            } catch (Exception e) {
                Log.e(TAG, "Error updating bookmark status in LOCAL DB for qid: " + qid, e);
                 BuildersKt.launch(viewModelScope, Dispatchers.getMain(), CoroutineStart.DEFAULT, (cScope, cont) -> {
                     errorMessage.postValue("ブックマーク状態のローカル更新に失敗しました。");
                     // Revert local state if DB update failed
                     quiz.setBookmarked(currentStatus);
                     List<QuizEntity> currentListOnError = loadedQuizzes.getValue();
                     if (currentListOnError != null && !currentListOnError.isEmpty()) {
                           List<QuizEntity> revertedList = new ArrayList<>(currentListOnError);
                           for(int i=0; i<revertedList.size(); i++){
                               if(revertedList.get(i).getQid().equals(qid)){
                                   revertedList.set(i, quiz); // Update with the reverted quiz object
                                   break;
                               }
                           }
                         loadedQuizzes.postValue(revertedList);
                           Log.d(TAG, "Reverted local state and posted to LiveData due to LOCAL DB error for qid: " + qid);
                     }
                     return Unit.INSTANCE;
                 });
            }
            return Unit.INSTANCE;
        });

        // ★★★ Firestoreへの保存 (ログインユーザーのみ) ★★★
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            // ★★★ DocumentReference の取得を修正 ★★★
            com.google.firebase.firestore.DocumentReference userDocRef = db.collection("users").document(userId);

            FieldValue updateValue;
            if (newBookmarkStatus) {
                // ブックマーク追加
                updateValue = FieldValue.arrayUnion(qid);
                Log.d(TAG, "[Firestore] Adding bookmark for user: " + userId + ", qid: " + qid);
            } else {
                // ブックマーク削除
                updateValue = FieldValue.arrayRemove(qid);
                 Log.d(TAG, "[Firestore] Removing bookmark for user: " + userId + ", qid: " + qid);
            }

            // 'bookmarkedQids' フィールドを更新 (なければ作成)
            // ★★★ Map を使ってフィールド名を指定 ★★★
            Map<String, Object> updates = new HashMap<>();
            updates.put("bookmarkedQids", updateValue);

            userDocRef.set(updates, SetOptions.merge()) // Use the map here
                .addOnSuccessListener(aVoid ->
                    Log.i(TAG, "[Firestore] Bookmark update successful for user: " + userId + ", qid: " + qid))
                .addOnFailureListener(e -> { // ★★★ Log.e 修正 ★★★
                    Log.e(TAG, "[Firestore] Error updating bookmark for user: " + userId + ", qid: " + qid, e); // Pass exception 'e'
                     BuildersKt.launch(viewModelScope, Dispatchers.getMain(), CoroutineStart.DEFAULT, (cScope, cont) -> {
                         errorMessage.postValue("ブックマークのサーバー保存に失敗しました。");
                         return Unit.INSTANCE;
                     });
                });
        } else {
            Log.d(TAG, "[toggleBookmarkStatus] User not logged in, skipping Firestore update.");
        }
    }

    /**
     * リソースをクリーンアップ
     */
    @Override
    protected void onCleared() {
        super.onCleared();
        databaseHelper.cleanup();
        asyncHelper.cleanup();
        // executorのシャットダウンも考慮 -> viewModelScope handles cancellation automatically
    }

    /**
     * Resets the bookmark state for all quizzes in the database to false.
     * This should typically be called once, e.g., during initial setup or from settings.
     */
    private void resetAllBookmarkStates() {
         // Replace executor with viewModelScope using BuildersKt.launch
        BuildersKt.launch(viewModelScope, Dispatchers.getIO(), CoroutineStart.DEFAULT, (coroutineScope, continuation) -> {
            try {
                Log.d(TAG, "Executing resetAllBookmarks in database...");
                quizDao.resetAllBookmarks();
                Log.d(TAG, "Database bookmark reset successful.");
            } catch (Exception e) {
                Log.e(TAG, "Error resetting bookmark states in database", e);
                 // Post error message on the main thread using BuildersKt.launch
                 BuildersKt.launch(viewModelScope, Dispatchers.getMain(), CoroutineStart.DEFAULT, (cScope, cont) -> {
                     errorMessage.postValue("ブックマーク状態のリセットに失敗しました。");
                     return Unit.INSTANCE;
                 });
            }
             return Unit.INSTANCE; // Return Unit
        });
    }

    // 追加: 統計情報のLiveDataゲッター
    public LiveData<Integer> getWeeklyAnswersCount() { return weeklyAnswersCount; }
    public LiveData<Integer> getWeeklyGoal() { return weeklyGoal; }
    public LiveData<Integer> getTotalAnswersCount() { return totalAnswersCount; }
    public LiveData<androidx.core.util.Pair<Integer, Integer>> getStreakInfo() { return streakInfo; }
    public LiveData<int[]> getWeeklyDailyAnswerCounts() { return weeklyDailyAnswerCounts; }

    /**
     * Loads and calculates statistics data from the database asynchronously.
     */
    public void loadStatisticsData() {
        Log.d(TAG, "Loading statistics data...");
        BuildersKt.launch(viewModelScope, Dispatchers.getIO(), CoroutineStart.DEFAULT, (coroutineScope, continuation) -> {
            try {
                // 1. 週ごとの解答数を取得
                long startOfWeekMillis = getStartOfWeekMillis();
                int weeklyCount = quizDao.getWeeklyAnswerCountSync(startOfWeekMillis);
                weeklyAnswersCount.postValue(weeklyCount);
                Log.d(TAG, "Weekly answers count: " + weeklyCount);

                // 2. 合計解答数を取得
                int totalCount = quizDao.getTotalAnswerCountSync();
                totalAnswersCount.postValue(totalCount);
                Log.d(TAG, "Total answers count: " + totalCount);

                // 3. 連続学習日数情報を計算・更新
                List<String> distinctDates = quizDao.getAllAnswerDaysSortedDesc();
                Log.d(TAG, "Distinct answer dates: " + distinctDates);
                calculateAndUpdateStreakInfo(distinctDates);

                // 4. 週ごとの日別解答数を計算・更新
                int[] dailyCounts = calculateWeeklyDailyCounts(startOfWeekMillis);
                weeklyDailyAnswerCounts.postValue(dailyCounts);
                Log.d(TAG, "Weekly daily counts: " + Arrays.toString(dailyCounts));
                
                // 5. 今日の学習時間をロード (loadStatisticsData が呼ばれるたびに日付チェックも行う)
                loadTodayStudyTime(); 

            } catch (Exception e) {
                Log.e(TAG, "Error loading statistics data", e);
                errorMessage.postValue("統計データの読み込みに失敗しました: " + e.getMessage());
            }
                     return Unit.INSTANCE;
        });
    }

    /**
     * Calculates the start of the current week (Monday 00:00:00) in milliseconds.
     */
    private long getStartOfWeekMillis() {
        Calendar calendar = Calendar.getInstance();
        // Set the first day of the week to Monday
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        // Set the calendar to the first day of the current week (Monday)
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        // Set time to the beginning of the day
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    /**
     * Calculates the current streak and updates the best streak based on history.
     * Uses Calendar API for compatibility with API level 24.
     * @param distinctDates List of distinct answer dates (YYYY-MM-DD) sorted descending.
     */
    private void calculateAndUpdateStreakInfo(List<String> distinctDates) {
        if (distinctDates == null || distinctDates.isEmpty()) {
             // ★★★ インスタンス化も androidx.core.util.Pair に ★★★
            streakInfo.postValue(new androidx.core.util.Pair<>(0, getBestStreakFromPrefs()));
            return;
        }

        int currentStreak = 0;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        Calendar todayCal = Calendar.getInstance();
        Calendar yesterdayCal = Calendar.getInstance();
        yesterdayCal.add(Calendar.DAY_OF_YEAR, -1);

        Calendar lastDateCal = Calendar.getInstance();
        try {
            Date lastDateParsed = sdf.parse(distinctDates.get(0));
            if (lastDateParsed == null) throw new ParseException("Parsing returned null", 0);
            lastDateCal.setTime(lastDateParsed);
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing last date: " + distinctDates.get(0), e);
            streakInfo.postValue(new androidx.core.util.Pair<>(0, getBestStreakFromPrefs()));
            return;
        }

        // Check if the most recent answer was today or yesterday
        if (isSameDay(lastDateCal, todayCal) || isSameDay(lastDateCal, yesterdayCal)) {
            currentStreak = 1;
            Calendar expectedDateCal = (Calendar) lastDateCal.clone(); // Start from the last valid date
            expectedDateCal.add(Calendar.DAY_OF_YEAR, -1); // Expect the day before

            for (int i = 1; i < distinctDates.size(); i++) {
                Calendar currentDateCal = Calendar.getInstance();
                try {
                    Date currentDateParsed = sdf.parse(distinctDates.get(i));
                    if (currentDateParsed == null) throw new ParseException("Parsing returned null", 0);
                    currentDateCal.setTime(currentDateParsed);
                } catch (ParseException e) {
                    Log.e(TAG, "Error parsing date in streak calculation: " + distinctDates.get(i), e);
                    break; // Stop calculating if a date is invalid
                }

                if (isSameDay(currentDateCal, expectedDateCal)) {
                    currentStreak++;
                    expectedDateCal.add(Calendar.DAY_OF_YEAR, -1); // Expect the day before this one
                } else {
                    break; // Streak broken
                }
            }
        }

        int bestStreak = getBestStreakFromPrefs();
        if (currentStreak > bestStreak) {
            bestStreak = currentStreak;
            saveBestStreakToPrefs(bestStreak);
        }

        streakInfo.postValue(new androidx.core.util.Pair<>(currentStreak, bestStreak));
    }

    /**
     * Helper method to check if two Calendar instances represent the same day.
     */
    private boolean isSameDay(Calendar cal1, Calendar cal2) {
        if (cal1 == null || cal2 == null) {
            return false;
        }
        return cal1.get(Calendar.ERA) == cal2.get(Calendar.ERA) &&
               cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    /**
     * Retrieves the best streak from SharedPreferences.
     */
    private int getBestStreakFromPrefs() {
        return prefs.getInt(PREF_BEST_STREAK, 0);
    }

    /**
     * Saves the best streak to SharedPreferences.
     */
    private void saveBestStreakToPrefs(int bestStreak) {
        prefs.edit().putInt(PREF_BEST_STREAK, bestStreak).apply();
    }

    /**
     * Calculates the daily answer counts for the current week (Mon-Sun).
     */
    private int[] calculateWeeklyDailyCounts(long startOfWeekMillis) {
        int[] dailyCounts = new int[7];
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(startOfWeekMillis);

        // Loop through Monday (0) to Sunday (6)
        for (int i = 0; i < 7; i++) {
            long dayStartMillis = calendar.getTimeInMillis();
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            long dayEndMillis = calendar.getTimeInMillis();

            // Query history count for this specific day
            // Note: This runs 7 separate queries. Could be optimized if needed.
            dailyCounts[i] = quizDao.getWeeklyAnswerCountSync(dayStartMillis, dayEndMillis); // Need to add this method to DAO
        }
        return dailyCounts;
    }

    // loadQuizData メソッドを追加
    public void loadQuizData(String qid) {
        // ... existing code ...
    }

    /**
     * Updates the weekly goal and saves it to SharedPreferences.
     * @param newGoal The new weekly goal to set.
     */
    public void updateWeeklyGoal(int newGoal) {
        if (newGoal > 0) { // Basic validation
            weeklyGoal.setValue(newGoal);
            // Replace executor with viewModelScope using BuildersKt.launch
            BuildersKt.launch(viewModelScope, Dispatchers.getIO(), CoroutineStart.DEFAULT, (coroutineScope, continuation) -> {
                prefs.edit().putInt(PREF_WEEKLY_GOAL, newGoal).apply();
                Log.d(TAG, "Weekly goal updated and saved: " + newGoal);
                 return Unit.INSTANCE; // Return Unit
            });
        } else {
            Log.w(TAG, "Attempted to set invalid weekly goal: " + newGoal);
            // Optionally, post an error message via LiveData
        }
    }

    // --- getter methods ---
    public LiveData<List<QuizEntity>> getAllQuizzesLiveData() { // 新しいゲッターを追加
        return allQuizzes;
    }
    // --- End getter methods ---

    // Reset state when loading a new quiz
    private void resetQuizState() {
        _isAnswered.setValue(false);
        _userSelections.setValue(Collections.emptySet());
        _answerResult.setValue(null);
    }

    // Placeholder for submitAnswer method
    public void submitAnswer(Set<Integer> selectedIndices) {
        Log.d(TAG, "submitAnswer called with indices: " + selectedIndices);
        QuizEntity quiz = currentQuiz.getValue();
        if (quiz == null || _isAnswered.getValue()) {
            Log.w(TAG, "submitAnswer called but quiz is null or already answered.");
            return; // Already answered or no quiz
        }

        // Update user selections state
        _userSelections.setValue(selectedIndices != null ? new HashSet<>(selectedIndices) : Collections.emptySet());

        // Perform answer checking
        List<Integer> correctAnswersFromEntity = quiz.getAnswer(); // Answers from entity are 1-based
        Set<Integer> correctAnswersZeroBased = new HashSet<>();
        if (correctAnswersFromEntity != null) {
            for (Integer answer : correctAnswersFromEntity) {
                if (answer != null && answer >= 0) { // Ensure answer is valid and non-negative
                    correctAnswersZeroBased.add(answer); // Remove '- 1'
                }
            }
        }

        boolean isCorrect = correctAnswersZeroBased.equals(_userSelections.getValue());
        Log.d(TAG, "Answer check - Correct indices (0-based): " + correctAnswersZeroBased + ", User indices: " + _userSelections.getValue() + ", Result: " + isCorrect);


        // Create AnswerResult
        AnswerResult result = new AnswerResult(isCorrect, correctAnswersZeroBased, _userSelections.getValue());
        _answerResult.setValue(result);

        // Mark as answered
        _isAnswered.setValue(true);

        // Record history (using existing method)
        recordAnswerResult(quiz.getQid(), isCorrect);

        // Optionally increment correct count (though maybe less relevant now)
        if (isCorrect) {
            incrementCorrectAnswerCount();
        }
    }

    // Overload for single choice submission
    public void submitAnswer(int selectedIndex) {
        Set<Integer> selections = new HashSet<>();
        selections.add(selectedIndex);
        submitAnswer(selections);
    }

    // 最後に解いた問題IDを取得
    public LiveData<String> getLastSolvedQuizId() {
        return lastSolvedQuizId;
    }

    /**
     * SharedPreferences から今日の学習時間を読み込み、日付が変わっていたらリセットする
     */
    private void loadTodayStudyTime() {
        String todayDateStr = getTodayDateString();
        String savedDateStr = prefs.getString(PREF_TODAY_STUDY_DATE, "");
        long todayStudyTimeMillis;

        if (todayDateStr.equals(savedDateStr)) {
            // 同じ日なら保存された時間を読み込む
            todayStudyTimeMillis = prefs.getLong(PREF_TODAY_STUDY_TIME_MILLIS, 0L);
        } else {
            // 日付が変わっていたらリセット
            todayStudyTimeMillis = 0L;
            // 新しい日付とリセットされた時間を保存
            prefs.edit()
                 .putString(PREF_TODAY_STUDY_DATE, todayDateStr)
                 .putLong(PREF_TODAY_STUDY_TIME_MILLIS, 0L)
                 .apply();
        }

        Log.d(TAG, "Loaded today's study time: " + todayStudyTimeMillis + "ms for date: " + todayDateStr);
        // LiveData を更新
        todayStudyTimeLiveData.postValue(formatDuration(todayStudyTimeMillis));
    }

    /**
     * 学習時間を追加し、SharedPreferences に保存して LiveData を更新する
     * @param durationMillis 追加する学習時間 (ミリ秒)
     */
    public void updateTodayStudyTime(long durationMillis) {
        if (durationMillis <= 0) return;

        String todayDateStr = getTodayDateString();
        String savedDateStr = prefs.getString(PREF_TODAY_STUDY_DATE, "");
        long currentTotalMillis;

        if (todayDateStr.equals(savedDateStr)) {
            // 同じ日なら現在の値に追加
            currentTotalMillis = prefs.getLong(PREF_TODAY_STUDY_TIME_MILLIS, 0L) + durationMillis;
        } else {
            // 日付が変わっていたら、新しい時間として記録し、日付も更新
            currentTotalMillis = durationMillis;
            prefs.edit().putString(PREF_TODAY_STUDY_DATE, todayDateStr).apply();
        }

        // 新しい合計時間を保存
        prefs.edit().putLong(PREF_TODAY_STUDY_TIME_MILLIS, currentTotalMillis).apply();
        Log.d(TAG, "Updated today's study time to: " + currentTotalMillis + "ms");

        // LiveData を更新
        todayStudyTimeLiveData.postValue(formatDuration(currentTotalMillis));
    }

    /**
     * ミリ秒を "今日の学習時間: X時間Y分" または "今日の学習時間: Y分" の形式にフォーマットする
     * @param durationMillis フォーマットする時間 (ミリ秒)
     * @return フォーマットされた文字列
     */
    private String formatDuration(long durationMillis) {
        long totalMinutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis);
        if (totalMinutes == 0 && durationMillis > 0) {
             // 1分未満の場合は秒数を表示（例: 30秒）
             long seconds = TimeUnit.MILLISECONDS.toSeconds(durationMillis);
             return String.format(Locale.getDefault(), "今日の学習時間: %d秒", seconds);
        } else if (totalMinutes < 60) {
            // 60分未満
            return String.format(Locale.getDefault(), "今日の学習時間: %d分", totalMinutes);
        } else {
            // 60分以上
            long hours = TimeUnit.MINUTES.toHours(totalMinutes);
            long minutes = totalMinutes % 60;
            return String.format(Locale.getDefault(), "今日の学習時間: %d時間%d分", hours, minutes);
        }
    }
    
    /**
     * 今日の日付を "yyyy-MM-dd" 形式の文字列で取得する
     * @return 今日の日付文字列
     */
    private String getTodayDateString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }
    
    /**
     * 今日の学習時間の LiveData を返すゲッター
     */
    public LiveData<String> getTodayStudyTimeLiveData() {
        return todayStudyTimeLiveData;
    }

    // ★★★ ブックマーク同期メソッドを追加 ★★★
    public void syncBookmarksFromFirestore() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.d(TAG, "[syncBookmarks] User not logged in, skipping sync.");
            return;
        }
        String userId = currentUser.getUid();
        Log.i(TAG, "[syncBookmarks] Starting bookmark sync for user: " + userId);

        db.collection("users").document(userId).get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    // Firestoreからブックマークリストを取得 (存在しない場合は空リスト)
                    List<String> firestoreBookmarks = (List<String>) documentSnapshot.get("bookmarkedQids");
                    if (firestoreBookmarks == null) {
                        firestoreBookmarks = Collections.emptyList();
                    }
                    Log.d(TAG, "[syncBookmarks] Fetched " + firestoreBookmarks.size() + " bookmarks from Firestore.");

                    // 同時に最終学習日と連続学習日数も取得・更新
                    com.google.firebase.Timestamp lastTimestamp = documentSnapshot.getTimestamp("lastStudyDate");
                    Date lastDate = (lastTimestamp != null) ? lastTimestamp.toDate() : null;
                    Long continuousDays = documentSnapshot.getLong("continuousStudyDays");
                    if (continuousDays == null) {
                        continuousDays = 0L; // Firestore にない場合は 0
                    }
                    syncedLastStudyDate.postValue(lastDate);
                    syncedContinuousStudyDays.postValue(continuousDays);
                    Log.d(TAG, "[syncBookmarks] Fetched stats - LastStudy: " + (lastDate != null ? lastDate.toString() : "null") + ", Streak: " + continuousDays);
                    // バックグラウンドでローカルDBを更新
                    List<String> finalFirestoreBookmarks = firestoreBookmarks; // Effectively final for lambda
                    BuildersKt.launch(viewModelScope, Dispatchers.getIO(), CoroutineStart.DEFAULT, (cScope, cont) -> {
                        try {
                            Log.d(TAG, "[syncBookmarks IO] Updating local DB bookmarks...");
                            // 1. ローカルDBの全QIDを取得 (効率は良くないが、今回はこれで)
                            List<String> localQids = quizDao.getAllQidsSync(); // 全QIDを取得するDAOメソッドが必要
                            if (localQids == null) localQids = Collections.emptyList();

                            // 2. ローカルDBのブックマーク状態をFirestoreの状態に合わせる
                            int updatedCount = 0;
                            for (String localQid : localQids) {
                                boolean shouldBeBookmarked = finalFirestoreBookmarks.contains(localQid);
                                // 現状の isBookmarked を取得し、変更が必要か確認 (DBアクセス削減のため)
                                QuizEntity currentEntity = quizDao.getQuizByQidSync(localQid); // qidで取得するDAOメソッドが必要
                                if (currentEntity != null && currentEntity.isBookmarked() != shouldBeBookmarked) {
                                    quizDao.updateBookmarkStatus(localQid, shouldBeBookmarked);
                                    updatedCount++;
                                }
                            }
                            Log.i(TAG, "[syncBookmarks IO] Finished updating local DB. " + updatedCount + " bookmark statuses changed.");

                            // 3. (オプション) UI反映のためにLiveDataを更新
                            // 例: ブックマークリスト画面などがあれば、ここで再読み込みトリガー
                            // 今回はQuizScreenの表示には直接影響しないため省略

                        } catch (Exception e) {
                            Log.e(TAG, "[syncBookmarks IO] Error updating local DB bookmarks", e);
                        }
                        return Unit.INSTANCE;
                    });

                } else {
                    Log.w(TAG, "[syncBookmarks] User document not found in Firestore for user: " + userId + ". Assuming no bookmarks.");
                    // Firestoreにドキュメントがない = ブックマーク0件としてローカルを更新 (全解除)
                     BuildersKt.launch(viewModelScope, Dispatchers.getIO(), CoroutineStart.DEFAULT, (cScope, cont) -> {
                          try {
                              quizDao.resetAllBookmarks(); // void なので変数代入不要
                              Log.i(TAG, "[syncBookmarks IO] Reset all local bookmarks as Firestore doc was missing.");
                          } catch (Exception e) {
                              Log.e(TAG, "[syncBookmarks IO] Error resetting local bookmarks", e);
                          }
                          return Unit.INSTANCE;
                     });
                }
            })
            .addOnFailureListener(e -> { // ★★★ Log.e 修正 ★★★
                Log.e(TAG, "[syncBookmarks] Error fetching user document from Firestore for user: " + userId, e); // Pass exception 'e'
            });
    }

    /**
     * Firestore のユーザードキュメント内の最終学習日と連続学習日数を更新する
     * @param userId ユーザーID
     */
    private void updateUserStatsInFirestore(String userId) {
        DocumentReference userDocRef = db.collection("users").document(userId);

        userDocRef.get().addOnSuccessListener(documentSnapshot -> {
            long currentContinuousDays = 0;
            Date lastStudyDate = null;

            if (documentSnapshot.exists()) {
                // Firestoreから既存の値を取得 (存在しない/型が違う場合はデフォルト値)
                Number days = documentSnapshot.getLong("continuousStudyDays");
                if (days != null) {
                    currentContinuousDays = days.longValue();
                }
                com.google.firebase.Timestamp lastTimestamp = documentSnapshot.getTimestamp("lastStudyDate");
                if (lastTimestamp != null) {
                    lastStudyDate = lastTimestamp.toDate();
                }
                 Log.d(TAG, "[updateUserStats] Fetched from Firestore - LastStudy: " + (lastStudyDate != null ? lastStudyDate.toString() : "null") + ", CurrentStreak: " + currentContinuousDays);
            } else {
                Log.w(TAG, "[updateUserStats] User document doesn't exist for user: " + userId + ". Will create with new stats.");
            }

            // 連続日数を計算
            Calendar todayCal = Calendar.getInstance();
            Calendar lastStudyCal = null;
            if (lastStudyDate != null) {
                lastStudyCal = Calendar.getInstance();
                lastStudyCal.setTime(lastStudyDate);
            }

            long newContinuousDays = 1; // デフォルトは1 (今日学習したので)

            if (lastStudyCal != null) {
                if (isSameDay(todayCal, lastStudyCal)) {
                    // 今日すでに学習済みの場合 -> 日数は変えない
                    newContinuousDays = currentContinuousDays;
                     Log.d(TAG, "[updateUserStats] Same day study detected. Streak remains: " + newContinuousDays);
                } else {
                    // 昨日学習したかチェック
                    Calendar yesterdayCal = (Calendar) todayCal.clone();
                    yesterdayCal.add(Calendar.DAY_OF_YEAR, -1);
                    if (isSameDay(lastStudyCal, yesterdayCal)) {
                        // 昨日学習していた -> インクリメント
                        newContinuousDays = currentContinuousDays + 1;
                         Log.d(TAG, "[updateUserStats] Yesterday study detected. Streak incremented to: " + newContinuousDays);
                    } else {
                        // 連続が途切れた -> 1 にリセット
                         Log.d(TAG, "[updateUserStats] Streak broken. Resetting streak to 1.");
                        // newContinuousDays は既に 1 なので変更不要
                    }
                }
            } else {
                 Log.d(TAG, "[updateUserStats] No previous study date found. Starting streak at 1.");
                 // newContinuousDays は既に 1
            }

            // Firestoreに書き込むデータを準備
            Map<String, Object> updates = new HashMap<>();
            updates.put("lastStudyDate", FieldValue.serverTimestamp());
            updates.put("continuousStudyDays", newContinuousDays);

            // ★★★ final 変数にコピーしてラムダ式で使用 ★★★
            final long finalNewContinuousDays = newContinuousDays;

            // Firestoreに書き込み (merge オプション付き)
            userDocRef.set(updates, SetOptions.merge())
                .addOnSuccessListener(aVoid ->
                    Log.i(TAG, "[updateUserStats] Firestore update successful for user: " + userId + ". New streak: " + finalNewContinuousDays)) // final 変数を参照
                .addOnFailureListener(e ->
                    Log.e(TAG, "[updateUserStats] Error updating Firestore stats for user: " + userId, e));

        }).addOnFailureListener(e -> {
            Log.e(TAG, "[updateUserStats] Error fetching user document for user: " + userId, e);
            // 読み込みに失敗した場合のエラーハンドリング (例: エラーメッセージ表示)
        });
    }

    /**
     * 全てのチャプターのカテゴリ進捗（完了数/総数）を非同期で計算し、LiveDataを更新します。
     */
    private void calculateAllChapterProgressAsync() {
        Log.d(TAG, "[calculateProgress] Starting calculation..."); // 開始ログ
        BuildersKt.launch(viewModelScope, Dispatchers.getIO(), CoroutineStart.DEFAULT, (coroutineScope, continuation) -> {
            Map<Integer, androidx.core.util.Pair<Integer, Integer>> progressMap = new HashMap<>();
            try {
                Log.d(TAG, "[calculateProgress IO] Getting all quizzes from DAO...");
                List<QuizEntity> quizzes = quizDao.getAllQuizzesSorted();
                Log.d(TAG, "[calculateProgress IO] Got " + (quizzes != null ? quizzes.size() : "null") + " quizzes.");

                if (quizzes != null && !quizzes.isEmpty()) {
                    Set<Integer> chapters = null;
                    try {
                        chapters = quizzes.stream()
                                        .filter(q -> q.getChapter() != null && !q.getChapter().isEmpty())
                                        .map(q -> {
                                            // ★★★ "章" を取り除いてから parseInt する ★★★
                                            String chapterStr = q.getChapter();
                                            String numericPart = chapterStr.replace("章", "").trim(); // "章"を削除し、前後の空白も除去
                                            try {
                                                return Integer.parseInt(numericPart);
                                            } catch (NumberFormatException nfe) {
                                                Log.e(TAG, "[calculateProgress IO] Failed to parse chapter number: " + numericPart + " from original: " + chapterStr, nfe);
                                                return null; // パース失敗した場合は null を返す
                                            }
                                        })
                                        .filter(Objects::nonNull) // パース失敗(null)したものを除外
                                        .collect(Collectors.toSet());
                        Log.d(TAG, "[calculateProgress IO] Found distinct chapters after parsing: " + chapters);
                    } catch (Exception e) { // ストリーム処理全体のエラーキャッチ
                         Log.e(TAG, "[calculateProgress IO] Error processing quiz stream for chapters", e);
                         chapters = Collections.emptySet(); // エラー時は空セット
                    }

                    for (Integer chapter : chapters) {
                        String chapterStr = String.valueOf(chapter);
                        Log.d(TAG, "[calculateProgress IO] Processing chapter: " + chapterStr);
                        List<String> allCategories = null;
                        List<String> completedCategories = null;
                        try {
                            Log.d(TAG, "[calculateProgress IO] Getting distinct categories for chapter: " + chapterStr);
                            allCategories = quizDao.getDistinctCategoriesForChapter(chapterStr);
                            Log.d(TAG, "[calculateProgress IO] Distinct categories count: " + (allCategories != null ? allCategories.size() : "null"));

                            Log.d(TAG, "[calculateProgress IO] Getting completed categories for chapter: " + chapterStr);
                            completedCategories = quizDao.getCompletedCategoriesForChapter(chapterStr);
                            Log.d(TAG, "[calculateProgress IO] Completed categories count: " + (completedCategories != null ? completedCategories.size() : "null"));

                            int completedSize = (completedCategories != null) ? completedCategories.size() : 0;
                            int totalSize = (allCategories != null) ? allCategories.size() : 0;

                            progressMap.put(chapter, new androidx.core.util.Pair<>(completedSize, totalSize));
                            Log.d(TAG, "[calculateProgress IO] Chapter " + chapter + " Progress PUT: " + completedSize + "/" + totalSize);

                        } catch (Exception e) {
                            Log.e(TAG, "[calculateProgress IO] Error processing categories for chapter " + chapterStr, e);
                            // このチャプターの処理中にエラーが出ても、他のチャプターの処理は続ける
                            // エラーが出たチャプターは progressMap に追加されない
                        }
                    }
                } else {
                    Log.w(TAG, "[calculateProgress IO] No quizzes found to calculate chapter progress.");
                }
            } catch (Exception e) {
                // DAOアクセス自体やストリーム処理での予期せぬエラー
                Log.e(TAG, "[calculateProgress IO] Error calculating chapter progress (outer try-catch)", e);
                errorMessage.postValue("チャプター進捗の計算中に予期せぬエラーが発生しました。");
                progressMap.clear();
            } finally {
                Log.d(TAG, "[calculateProgress IO] Calculation finished. Final progressMap: " + progressMap); // 最終結果をログ出力
                chapterProgressMapLiveData.postValue(progressMap);
                Log.d(TAG, "[calculateProgress] Posted progressMap to LiveData.");
            }
            return Unit.INSTANCE;
        });
    }
}
