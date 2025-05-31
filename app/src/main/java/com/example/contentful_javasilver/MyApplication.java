package com.example.contentful_javasilver;

import android.app.Application;
import dagger.hilt.android.HiltAndroidApp;

@HiltAndroidApp
public class MyApplication extends Application {
    // 必要に応じて、onCreate() などで追加の初期化処理を記述できます
    @Override
    public void onCreate() {
        super.onCreate();
        // アプリケーションレベルの初期化処理
    }
} 