package com.example.contentfuljavasilver.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.example.contentfuljavasilver.data.ThemeDataStore
import com.example.contentfuljavasilver.data.dataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun providePreferencesDataStore(@ApplicationContext appContext: Context): DataStore<Preferences> {
        return appContext.dataStore
    }

    @Provides
    @Singleton
    fun provideThemeDataStore(@ApplicationContext context: Context): ThemeDataStore {
        return ThemeDataStore(context)
    }
} 