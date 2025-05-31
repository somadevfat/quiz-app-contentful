package com.example.contentful_javasilver.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.contentful_javasilver.data.repository.QuizRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoadingViewModel @Inject constructor(
    private val quizRepository: QuizRepository
) : ViewModel() {

    private val _randomQuizId = MutableLiveData<String?>()
    val randomQuizId: LiveData<String?> = _randomQuizId

    private val _isLoadingComplete = MutableLiveData<Boolean>(false)
    val isLoadingComplete: LiveData<Boolean> = _isLoadingComplete

    private val _loadingStatus = MutableLiveData<String?>()
    val loadingStatus: LiveData<String?> = _loadingStatus

    private val _errorOccurred = MutableLiveData<String?>()
    val errorOccurred: LiveData<String?> = _errorOccurred

    init {
        fetchRandomQuizId()
    }

    private fun fetchRandomQuizId() {
        viewModelScope.launch {
            try {
                val id = quizRepository.getRandomQuizId()
                _randomQuizId.postValue(id)
            } catch (e: Exception) {
                _randomQuizId.postValue(null)
                _errorOccurred.postValue("Error fetching random quiz ID: ${e.message}")
                println("Error fetching random quiz ID: ${e.message}")
            } finally {
            }
        }
    }

    fun setLoadingComplete() {
        _isLoadingComplete.postValue(true)
        _loadingStatus.postValue("準備完了")
        _errorOccurred.postValue(null)
    }

    fun setLoadingError(errorMessage: String) {
        _errorOccurred.postValue(errorMessage)
        _loadingStatus.postValue("エラーが発生しました")
        _isLoadingComplete.postValue(false)
    }

    fun updateLoadingStatus(status: String) {
        _loadingStatus.postValue(status)
    }
} 