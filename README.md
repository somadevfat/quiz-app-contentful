﻿# JavaSilver 学習アプリ

## 概要
Java Silver認定試験合格を支援するAndroid学習ツール。
クイズ形式の実践演習、分野別学習、詳細な学習履歴・統計機能を提供。

## 主な機能
* **ランダム出題**: 全範囲からランダムに問題を出題し、総合理解度を確認。
* **分野別学習**: 試験範囲に基づき、章・カテゴリー別に問題を絞って集中的に学習。
* **問題一覧**: 全収録問題を一覧で参照し、解答状況を確認。
* **学習履歴**: 解答した問題の記録（ID、正誤、日時）を時系列で確認。
* **学習統計**: 問題ごとの正誤回数・正答率を分析し、弱点を把握。
* **ブックマーク**: 要復習問題や重要問題をマークし、簡単アクセス。
* **テーマ設定**: 複数カラーテーマから好みの学習環境を構築。
* **ユーザー認証**: Firebase認証で学習データをアカウント管理。デバイス間同期・データ永続化。
* **コンテンツ管理**: ヘッドレスCMS (Contentful) からクイズデータを取得。柔軟なコンテンツ更新。

## デモ・紹介GIF

JavaSilver学習アプリの主要機能をGIF形式でご覧ください。

<img src="README_introduction.gif" alt="アプリ紹介 GIF" width="300"/>

## スクリーンショット
アプリの主要な画面です。

| ホーム画面                                 | 問題画面                                     | カテゴリー選択画面                                   |
| :----------------------------------------: | :------------------------------------------: | :----------------------------------------------: |
| <img src="picture/画面スクショ/ホーム画面1.jpg" alt="ホーム画面" width="250"/> | <img src="picture/画面スクショ/問題画面.png" alt="問題画面" width="250"/> | <img src="picture/画面スクショ/カテゴリー選択画面.jpg" alt="カテゴリー選択画面" width="250"/> |
| **主な機能へのアクセスポイントです。**             | **クイズに挑戦し、解答と解説を確認できます。**    | **学習したい分野を選んで集中的に取り組めます。**          |

| 学習履歴・ブックマーク画面                        | 学習統計画面                                    |
| :------------------------------------------: | :------------------------------------------: |
| <img src="picture/画面スクショ/学習履歴-ブックマーク選択画面.jpg" alt="学習履歴・ブックマーク選択画面" width="250"/> | <img src="picture/画面スクショ/学習統計.jpg" alt="学習統計画面" width="250"/> |
| **過去の学習記録やブックマークした問題を確認。** | **正答率や解答傾向をグラフで可視化。**         |

## 成果・工夫点
* **開発動機**:
    * Java Silver資格取得を目指す自身の学習効率向上。
    * 同様の課題を持つ学習者のサポート。
* **最も苦労した点と解決方法**:
    * **ContentfulとRoomDB連携**:
        * 課題: ContentfulデータローカルDBへの効率的な保存・更新。差分更新、初回大量データ同期時のパフォーマンス。
        * 解決: Contentful Sync API活用検討。Roomトランザクション最適化。非同期処理(コルーチン)管理によるUI応答性維持。
    * **Jetpack ComposeとXML共存**:
        * 課題: 既存XML画面との連携、ナビゲーション、状態管理統一。
        * 解決: `AndroidViewBinding`活用。ViewModel共通化による段階的移行。
* **このプロジェクトで学んだこと**:
    * モダンAndroid開発技術(Jetpack Compose, Hilt, Coroutines, Room)の実践。
    * MVVMアーキテクチャによる保守性の高いコード設計。
    * ヘッドレスCMS (Contentful)連携の動的コンテンツ配信システム構築。
    * Firebase Authenticationによるユーザー認証実装。
    * 段階的技術導入と既存コードベースとの共存戦略。

## 技術スタック
主要な技術要素とその役割です。

* **言語**:
    * **Kotlin**: 主要開発言語。Null安全、コルーチン等でコード削減、可読性・安全性向上。
    * **Java**: 一部既存コード、ライブラリ連携で使用。将来的にKotlinへ移行検討。
* **UIフレームワーク**:
    * **Jetpack Compose**: ログイン画面等一部に導入。宣言的UIで生産性向上、コード簡潔化。
    * **Android Views (XML)**: 大部分の画面で使用。安定性と既存リソース活用。
* **アーキテクチャ**:
    * **MVVM**: UI層、ビジネスロジック層、データ層を分離。テスト容易性、保守性、拡張性向上。
* **データ永続化**:
    * **Room Database**: SQLite抽象化ライブラリ。クイズデータ、学習履歴等をローカル保存。オフライン利用、高速アクセス。
* **リモートデータ取得**:
    * **Contentful CDA SDK**: Contentful API用SDK。クイズコンテンツを効率的にフェッチ。
* **認証**:
    * **Firebase Authentication**: メール/パスワード認証、Googleアカウント連携を安全に実装。
* **依存性注入 (DI)**:
    * **Hilt**: Android特化DIライブラリ。依存オブジェクト管理自動化。疎結合、テスト容易性向上。
* **非同期処理**:
    * **Kotlin Coroutines**: ネットワーク通信、DBアクセス等をノンブロッキング実行。UI応答性維持。
* **ナビゲーション**:
    * **Android Navigation Component**: 画面遷移管理。Safe Args等利用。
* **その他ライブラリ/ツール**:
    * **Lottie**: アニメーション表示。ローディング画面等。
    * **EncryptedSharedPreferences**: 機密情報（APIキー等）を暗号化保存。
    * **ktlint, detekt**: 静的解析ツール。コードスタイル維持、潜在的問題早期発見。

## 技術的な取り組み
* MVVM + Clean Architectureによる保守性の高い設計。
* Jetpack ComposeとXMLレイアウトの段階的移行。
* Coroutinesによる適切な非同期処理。
* Hiltによるテスタブルなコード。

## アーキテクチャと技術間の関連性 (Mermaid図)
主要コンポーネントと技術要素、データの流れを示します。

```mermaid
graph TD
    A[UI: Fragment / Compose] --> B(ViewModel);
    B --> C(Repository);
    C --> D[Room Database];
    C --> E[Contentful CDA SDK];
    B --> F[Firebase Authentication];
    F --> G[Firebase Backend];
    C --> G;
    E -- クイズデータ --> C;
    D -- ローカルデータ --> C;
    G -- ユーザーデータ<br/>学習履歴<br/>ブックマーク --> C;
    A -- ユーザー操作 --> B;
    B -- 状態更新 --> A;
    F -- 認証リクエスト --> G;
    G -- 認証結果 --> F;
    C -- データ保存 --> G;

    %% Styling
    style A fill:#bbf,stroke:#333,stroke-width:2px
    style B fill:#f9f,stroke:#333,stroke-width:2px
    style C fill:#ccf,stroke:#333,stroke-width:2px
    style D fill:#cfc,stroke:#333,stroke-width:2px
    style E fill:#ffc,stroke:#333,stroke-width:2px
    style F fill:#fcf,stroke:#333,stroke-width:2px
    style G fill:#cff,stroke:#333,stroke-width:2px

    %% Subgraphs (Optional, for grouping)
    subgraph Presentation Layer
        A
        B
    end

    subgraph Domain Layer
        C
    end

    subgraph Data Layer
        D
        E
        F
        G
    end

    %% Link styles (Optional, for clarity)
    linkStyle 0,1,2,3,4,5,6,7,8,9,10,11,12 stroke:#555
```

**図の説明：**
* **UI (Fragment / Compose)**: ユーザーインターフェース。入力受付、ViewModelへ伝達。ViewModelからデータ表示。
* **ViewModel**: UIデータ保持、UIロジック処理。Repository経由でデータアクセス。UI状態管理。
* **Repository**: データアクセス層API提供。複数データソース(Room, Contentful, Firebase)からのデータ処理をカプセル化。
* **Room Database**: ローカルDB。クイズデータ、学習履歴等永続化。
* **Contentful CDA SDK**: Contentfulからクイズコンテンツ取得。
* **Firebase Authentication**: ユーザー認証管理。ログイン、新規登録、ログアウト機能。
* **Firebase Backend**: Firestore等クラウドサービス。ユーザー別学習履歴、ブックマーク等保存。

このアーキテクチャにより、各層が独立し、テスト・保守しやすい構造です。

## プロジェクト構造の概要
```
.
├── app
│   ├── build.gradle          # アプリモジュールのGradle設定
│   └── src
│       ├── androidTest       # Androidインストルメンテーションテスト
│       ├── main
│       │   ├── AndroidManifest.xml # アプリのマニフェストファイル
│       │   ├── java          # Java/Kotlinソースコード
│       │   │   └── com
│       │   │       └── example
│       │   │           └── contentful_javasilver # アプリケーションコード
│       │   │               ├── adapter         # RecyclerViewアダプター
│       │   │               ├── data            # データ関連クラス (Room Entity, DAO, Repository)
│       │   │               ├── decoration      # RecyclerViewデコレーション
│       │   │               ├── di              # Hiltモジュール
│       │   │               ├── model           # UI用データモデル
│       │   │               ├── models          # (Kotlin) データモデル
│       │   │               ├── ui              # Jetpack Compose UI
│       │   │               ├── utils           # ユーティリティクラス
│       │   │               └── viewmodels      # ViewModelクラス
│       │   └── res           # リソースファイル (layout, drawable, values, menu, navigationなど)
│       └── test              # ローカルユニットテスト
├── build.gradle              # プロジェクト全体のGradle設定
├── gradle.properties         # Gradleプロパティ
├── gradlew / gradlew.bat     # Gradleラッパースクリプト
├── settings.gradle / settings.gradle.kts # Gradle設定ファイル
└── docs                      # プロジェクトに関するドキュメント
    └── code-review.md        # コードレビューに関するメモ
```

## 始め方

### 前提条件
* Android Studio Hedgehog | 2023.1.1 以降
* JDK 17 以降
* Contentful Space ID, Access Token
* Firebase プロジェクト設定 (`google-services.json`), Web Client ID

### セットアップ
1.  リポジトリをクローン:
    ```bash
    git clone [リポジトリURL]
    cd [リポジトリ名]
    ```
2.  Android Studioでプロジェクトを開く。
3.  `local.properties` をプロジェクトルートに作成し、Contentful認証情報を追加:
    ```properties
    CONTENTFUL_SPACE_ID="あなたのContentful Space ID"
    CONTENTFUL_ACCESS_TOKEN="あなたのContentful Access Token"
    ```
4.  `google-services.json` (FirebaseからDL) を `app/` に配置。
5.  `app/src/main/res/values/strings.xml` にFirebase Web Client IDを追加:
    ```xml
    <string name="default_web_client_id" translatable="false">あなたのWeb Client ID</string>
    ```
6.  プロジェクトをビルドし実行。

## コードレビューに関するメモ
(特に変更なし)

## 貢献

貢献を歓迎します！コントリビューションの方法については、別途 `CONTRIBUTING.md` ファイルを作成する予定です。

## 今後の展望

* **CI/CD環境の構築**: Dockerを活用してビルド・テスト・デプロイプロセスの自動化を進め、開発効率を向上させます。
* **開発環境の簡易化**: Dockerを用いて、新規開発者が容易に開発環境を構築できる仕組みを整備します。
* **機能拡張**:
    * 模擬試験モードの追加
    * ユーザー間のランキング機能
    * オフラインでの問題解答機能の強化
* **技術的改善**:
    * Jetpack Composeへの完全移行
    * Kotlinへの完全移行（Javaコードの排除）
    * 完全なKotlin化の達成
    * テストカバレッジの向上（ユニットテスト、UIテストの拡充）
    * Contentful Sync APIの本格導入によるデータ同期の最適化

## ライセンス

ライセンスMIT

---
