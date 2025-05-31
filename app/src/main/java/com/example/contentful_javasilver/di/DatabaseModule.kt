package com.example.contentful_javasilver.di

import android.content.Context
import androidx.room.Room
import com.example.contentful_javasilver.data.QuizDao
import com.example.contentful_javasilver.data.QuizDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideQuizDatabase(@ApplicationContext appContext: Context): QuizDatabase {
        return QuizDatabase.getDatabase(appContext)
        /* Alternative if using Room.databaseBuilder directly:
        return Room.databaseBuilder(
            appContext,
            QuizDatabase::class.java,
            "quiz_database" // Make sure the name matches your QuizDatabase class
        )
        // Add migrations if necessary
        .build()
        */
    }

    @Provides
    @Singleton // Assuming DAO should be singleton as well
    fun provideQuizDao(database: QuizDatabase): QuizDao {
        return database.quizDao()
    }
} 