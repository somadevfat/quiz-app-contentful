package com.example.contentful_javasilver.data.repository

import androidx.lifecycle.LiveData
import com.example.contentful_javasilver.data.QuizDao
import com.example.contentful_javasilver.data.QuizEntity
import com.example.contentful_javasilver.data.QuizHistory
import com.example.contentful_javasilver.data.ProblemStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton // Or @ViewModelScoped, depending on lifecycle needs
class QuizRepository @Inject constructor(private val quizDao: QuizDao) {

    // Example: Get all quizzes sorted as a suspend function
    suspend fun getAllQuizzesSorted(): List<QuizEntity> = withContext(Dispatchers.IO) {
        quizDao.getAllQuizzesSorted()
    }

    // Example: Get random quizzes as LiveData
    fun getRandomQuizzes(count: Int): LiveData<List<QuizEntity>> {
        return quizDao.getRandomQuizzes(count)
    }

    // Get a single random quiz ID
    suspend fun getRandomQuizId(): String? = withContext(Dispatchers.IO) {
        quizDao.getRandomQuizIdSync()
    }

    // Example: Get quiz count as LiveData
    fun getQuizCount(): LiveData<Int> {
        return quizDao.getQuizCount()
    }

    // Example: Get quiz count synchronously (use with caution on main thread)
    suspend fun getQuizCountSync(): Int = withContext(Dispatchers.IO) {
        quizDao.getQuizCountSync()
    }

    // Add other necessary repository methods wrapping DAO calls...
    // For example:
    // suspend fun getQuizzesByCategory(category: String): List<QuizEntity> = withContext(Dispatchers.IO) {
    //     quizDao.getQuizzesByCategorySync(category)
    // }

    suspend fun updateBookmarkStatus(qid: String, isBookmarked: Boolean) = withContext(Dispatchers.IO) {
        quizDao.updateBookmarkStatus(qid, isBookmarked)
    }

    fun getBookmarkedQuizzes(): LiveData<List<QuizEntity>> {
        return quizDao.getBookmarkedQuizzes()
    }

    suspend fun resetAllBookmarks() = withContext(Dispatchers.IO) {
        quizDao.resetAllBookmarks()
    }

    suspend fun insertHistory(history: QuizHistory) = withContext(Dispatchers.IO) {
        quizDao.insertHistory(history)
    }

    fun getAllHistorySortedByTimestampDesc(): LiveData<List<QuizHistory>> {
        return quizDao.getAllHistorySortedByTimestampDesc()
    }

    fun getProblemStatistics(): LiveData<List<ProblemStats>> {
        return quizDao.getProblemStatistics()
    }

    suspend fun getTotalAnswerCountSync(): Int = withContext(Dispatchers.IO) {
        quizDao.getTotalAnswerCountSync()
    }

    suspend fun getWeeklyAnswerCountSync(startTimeMillis: Long): Int = withContext(Dispatchers.IO) {
        quizDao.getWeeklyAnswerCountSync(startTimeMillis)
    }

    suspend fun getDistinctAnswerDaysCount(startTimeMillis: Long): Int = withContext(Dispatchers.IO) {
        quizDao.getDistinctAnswerDaysCount(startTimeMillis)
    }

    suspend fun getAllAnswerDaysSortedDesc(): List<String> = withContext(Dispatchers.IO) {
        quizDao.getAllAnswerDaysSortedDesc()
    }

    suspend fun getWeeklyAnswerCountSync(startTimeMillis: Long, endTimeMillis: Long): Int = withContext(Dispatchers.IO) {
        quizDao.getWeeklyAnswerCountSync(startTimeMillis, endTimeMillis)
    }

    // Add wrappers for other DAO methods as needed

} 