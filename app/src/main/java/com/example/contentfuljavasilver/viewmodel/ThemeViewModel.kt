package com.example.contentfuljavasilver.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.contentfuljavasilver.data.ThemeDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val themeDataStore: ThemeDataStore
) : ViewModel() {

    // データストアから現在のテーマを取得し、StateFlowとして公開します
    // これにより、UIはテーマの変更をリアクティブに監視できます
    val currentTheme: StateFlow<String> = themeDataStore.getTheme
        .map { themeName ->
            themeName ?: "default" // null の場合は "default" を返す
        }
        .stateIn(
            scope = viewModelScope, // ViewModelのライフサイクルに連動
            started = SharingStarted.WhileSubscribed(5000), // 画面が表示されている間アクティブ
            initialValue = "default" // 初期値も "default" に統一 (stateInの初期値は最初の値がemitされるまで使われる)
        )

    // 新しいテーマ名をデータストアに保存する関数
    fun changeTheme(themeName: String) {
        viewModelScope.launch { // 非同期で実行
            themeDataStore.saveTheme(themeName)
        }
    }
} 