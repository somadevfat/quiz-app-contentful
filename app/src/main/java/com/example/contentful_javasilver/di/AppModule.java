package com.example.contentful_javasilver.di;

import com.google.firebase.auth.FirebaseAuth;
import javax.inject.Singleton;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class) // Applicationスコープでインスタンスを提供
public final class AppModule {

    @Provides
    @Singleton // アプリ全体で FirebaseAuth の単一インスタンスを使用
    public FirebaseAuth provideFirebaseAuth() {
        return FirebaseAuth.getInstance();
    }

    // 他の依存関係 (例: Room Database, Retrofit など) も
    // 必要に応じてこのモジュールに追加できます。

} 