package com.example.contentfuljavasilver.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class ThemeDataStore(private val context: Context) {

    companion object {
        val THEME_KEY = stringPreferencesKey("theme_preference")
    }

    val getTheme: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[THEME_KEY] ?: "default" // Provide default value
        }

    suspend fun saveTheme(theme: String) {
        context.dataStore.edit {
            settings -> settings[THEME_KEY] = theme
        }
    }
}