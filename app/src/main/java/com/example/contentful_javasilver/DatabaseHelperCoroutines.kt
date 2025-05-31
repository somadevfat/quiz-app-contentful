package com.example.contentful_javasilver

import android.util.Log
import kotlinx.coroutines.*
import com.example.contentful_javasilver.data.QuizDao
import com.example.contentful_javasilver.data.QuizEntity
import kotlin.Unit

class DatabaseHelperCoroutines {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val TAG = "DatabaseHelper"

    fun loadCategoriesAsync(
        chapterNumber: Int,
        quizDao: QuizDao,
        onSuccess: (List<String>) -> Unit,
        onError: (String) -> Unit
    ) {
        scope.launch {
            try {
                Log.d(TAG, "Loading categories for chapter number: $chapterNumber")

                // 正しい章名文字列を生成 (例: "1章")
                val chapterName = "${chapterNumber}章"
                Log.d(TAG, "Using chapter name for exact match: $chapterName")

                // DAOメソッドを呼び出してカテゴリを直接取得 (メソッド名はそのまま)
                val categories = quizDao.getCategoriesByChapterPattern(chapterName)
                
                Log.d(TAG, "Loaded ${categories.size} distinct categories for chapter $chapterName directly from DAO: $categories")
                
                withContext(Dispatchers.Main) {
                    if (categories.isEmpty()) {
                        Log.w(TAG, "No categories found for chapter $chapterName using exact match.")
                    }
                    onSuccess(categories)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading categories for chapter number $chapterNumber", e)
                withContext(Dispatchers.Main) {
                    onError("カテゴリーの読み込みに失敗しました: ${e.message}")
                }
            }
        }
    }

    /**
     * 文字列から数値を抽出するヘルパーメソッド
     */
    private fun extractNumberFromString(str: String?): Int {
        if (str.isNullOrBlank()) return -1
        
        return try {
            // 1. 数字のみの場合
            if (str.matches(Regex("^\\d+$"))) {
                return str.toInt()
            }
            
            // 2. "X章" パターンの場合
            if (str.contains("章")) {
                val number = str.replace("章", "").trim()
                if (number.matches(Regex("^\\d+$"))) {
                    return number.toInt()
                }
            }
            
            // 3. その他のケース: 数字以外の文字を除去
            val numberOnly = str.replace(Regex("[^0-9]"), "")
            if (numberOnly.isNotEmpty()) {
                numberOnly.toInt()
            } else {
                -1
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting number from $str", e)
            -1
        }
    }

    fun getQuizCountForCategoryAsync(
        category: String,
        quizDao: QuizDao,
        onSuccess: (Int) -> Unit,
        onError: (String) -> Unit
    ) {
        scope.launch {
            try {
                val quizzes = quizDao.getAllQuizzes()
                val count = quizzes.count { quiz -> quiz.category == category }
                withContext(Dispatchers.Main) {
                    onSuccess(count)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onError("問題数の取得に失敗しました")
                }
            }
        }
    }

    fun loadQuestionCategoriesAsync(
        selectedCategory: String,
        quizDao: QuizDao,
        onSuccess: (List<QuestionCategoryItem>) -> Unit,
        onError: (String) -> Unit
    ) {
        scope.launch {
            try {
                val allQuizzes = quizDao.getAllQuizzes()
                
                val quizzesInCategory = allQuizzes.filter { it.category == selectedCategory }
                
                val questionCategoryItems = quizzesInCategory
                    .filter { !it.questionCategory.isNullOrBlank() } 
                    .groupBy { it.questionCategory!! } 
                    .map { (questionCategoryName, quizzesInGroup) ->
                        // グループ内の最初のクイズのqidを取得 (nullでないことを期待)
                        val representativeQid = quizzesInGroup.firstOrNull()?.qid ?: "UNKNOWN_QID"
                        
                        // 問題が解かれたかどうかを確認
                        val isCompleted = quizDao.isProblemAnswered(representativeQid)
                        
                        QuestionCategoryItem(
                            qid = representativeQid, // 代表qidを追加
                            questionCategory = questionCategoryName,
                            totalQuestionCount = quizzesInGroup.size,
                            isCompleted = isCompleted // 追加: 解答済みかどうか
                        )
                    }
                    // .sortedBy { it.questionCategory } // 既存のソートをコメントアウトまたは削除
                    .sortedWith(compareBy<QuestionCategoryItem> {
                        val parts = it.qid.split('-')
                        parts.getOrNull(0)?.toIntOrNull() ?: Int.MAX_VALUE // major 番号でソート (エラー時は最後に)
                    }.thenBy {
                        val parts = it.qid.split('-')
                        parts.getOrNull(1)?.toIntOrNull() ?: Int.MAX_VALUE // minor 番号でソート (エラー時は最後に)
                    })

                withContext(Dispatchers.Main) {
                    if (questionCategoryItems.isEmpty()) {
                        Log.w(TAG, "No question categories found for category: $selectedCategory")
                    }
                    onSuccess(questionCategoryItems)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading question categories for $selectedCategory", e)
                withContext(Dispatchers.Main) {
                    onError("問題カテゴリの読み込みに失敗しました: ${e.message}")
                }
            }
        }
    }

    fun loadAllQidsAsync(
        quizDao: QuizDao,
        onSuccess: (List<String>) -> Unit,
        onError: (String) -> Unit
    ) {
        scope.launch {
            try {
                val allQuizzes = quizDao.getAllQuizzes() // シンプルに全件取得
                val allQids = allQuizzes.mapNotNull { it.qid } // qidのみを抽出 (nullを除外)
                                       .filter { it.isNotBlank() } // 空文字を除外
                                       .distinct() // 重複を除外
                
                Log.d(TAG, "Loaded ${allQids.size} distinct QIDs")

                withContext(Dispatchers.Main) {
                    onSuccess(allQids)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading all QIDs", e)
                withContext(Dispatchers.Main) {
                    onError("全問題IDの読み込みに失敗しました: ${e.message}")
                }
            }
        }
    }

    fun getQuizByQidAsync(
        qid: String,
        quizDao: QuizDao,
        onSuccess: (QuizEntity?) -> Unit, // Nullable QuizEntity を返すように変更
        onError: (String) -> Unit
    ) {
        scope.launch {
            var quiz: QuizEntity? = null // 変数を try の外で宣言
            try {
                // getQuizzesByQid は List を返すため、firstOrNull を使う
                quiz = quizDao.getQuizzesByQid(qid).firstOrNull()
                
                withContext(Dispatchers.Main) {
                    onSuccess(quiz) // 見つかった QuizEntity または null を渡す
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting quiz by QID $qid", e)
                withContext(Dispatchers.Main) {
                    onError("問題の取得に失敗しました (ID: $qid): ${e.message}")
                }
            }
        }
    }

    data class QuestionCategoryItem(
        val qid: String,
        val questionCategory: String,
        val totalQuestionCount: Int,
        val isCompleted: Boolean
    )

    fun cleanup() {
        scope.cancel()
    }
} 