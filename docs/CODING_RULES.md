# Kotlin コーディングルール

このプロジェクトにおける Kotlin のコーディングルールです。保守性・可読性の高いコードを目指します。

## 参照ガイドライン

-   **Kotlin 公式スタイルガイド:** [https://kotlinlang.org/docs/coding-conventions.html](https://kotlinlang.org/docs/coding-conventions.html)
-   **Android Kotlin スタイルガイド:** [https://developer.android.com/kotlin/style-guide?hl=ja](https://developer.android.com/kotlin/style-guide?hl=ja)
-   **(詳細版参考):** [CODING_CONVENTIONS.md](./CODING_CONVENTIONS.md) (より詳細な規約はこちらを参照)

## 主要ライブラリドキュメント (随時追加)

-   **(例) Jetpack Compose:** [https://developer.android.com/jetpack/compose/documentation?hl=ja](https://developer.android.com/jetpack/compose/documentation?hl=ja)
-   **(例) Hilt:** [https://developer.android.com/training/dependency-injection/hilt-android?hl=ja](https://developer.android.com/training/dependency-injection/hilt-android?hl=ja)
-   **(例) Room:** [https://developer.android.com/training/data-storage/room?hl=ja](https://developer.android.com/training/data-storage/room?hl=ja)
-   ... (その他、ViewModel, LiveData/Flow, Coroutines, Contentful SDK など)

---

## 1. 構造と設計 (MVVMベース)

-   **基本アーキテクチャ:** MVVM (Model-View-ViewModel) パターンに従う。
    -   **View (Activity/Fragment/Composable):** UI表示と入力受付に専念。ViewModelを監視。
    -   **ViewModel:** UI状態保持とUIロジック担当。UseCase/Repositoryと連携。Androidフレームワーク依存は最小限に。
    -   **UseCase (推奨):** 複雑なビジネスロジックを担当。
    -   **Repository:** データアクセスを抽象化。インターフェースと実装を分離。
    -   **Model:** データ構造定義 (`data class`)。
-   **単一責任の原則 (SRP):** 1クラス/1関数は1責務。クラスは300行、関数は30行を目安に。
-   **依存性の注入 (DI):** **Hilt** を使用。インターフェースに依存 (`constructor injection` 推奨)。
-   **ネスト深度:** 最大3段階までを目安に。

## 2. 命名規則

-   **基本:** [1. 構造と設計](#1-構造と設計-mvvmベース) の各コンポーネント名規則に従う。
    -   クラス/インターフェース/Composable関数: `UpperCamelCase`
    -   関数/変数: `lowerCamelCase`
    -   定数 (`const val`): `UPPER_SNAKE_CASE`
    -   パッケージ: `com.example.appname.feature`, `data`, `ui`, `util` など（小文字）。
-   **ViewModel:** `XxxViewModel`
-   **UseCase:** `VerbNounUseCase`
-   **Repository:** `XxxRepository` (IF), `XxxRepositoryImpl` (Impl)
-   **Composable:** `XxxScreen`, `XxxComponent`, または具体的な名前。
-   **Boolean:** `isXxx`, `hasXxx`, `shouldXxx` など。
-   **リソース:** （別項参照）

## 3. 可読性と保守性

-   **不変性:** `val` と読み取り専用コレクション (`List`, `Set`, `Map`) を最大限利用する。
-   **Null安全:**
    -   Null許容型 (`?`) の使用は必要最小限に。
    -   **`!!` (Non-null assertion) は原則禁止。** `?.`, `?:`, `let`, `requireNotNull` 等で安全に扱う。
-   **スコープ関数 (`let`, `run`, `apply` 等):** 可読性が向上する場合にのみ、適切に選択して使用する。ネストしすぎない。
-   **型推論:** ローカル変数では活用するが、関数の戻り値やAPI境界では型を明記する。
-   **コメント:** 「なぜ (Why)」コードだけでは分かりにくい部分に記述。公開APIには **KDoc** を記述。`TODO:` は具体的に。
-   **ログ:** **Timber** ライブラリの使用を推奨。デバッグログはリリースビルドで出力しない。機密情報はログ出力禁止。
-   **エラーハンドリング:** `try-catch` または **Kotlin Result API (`runCatching`)** を活用。例外を握りつぶさない。コルーチンでは `CoroutineExceptionHandler`, `Flow.catch` 等を適切に使用。ユーザーには分かりやすいエラー表示を。
-   **マジックナンバー/文字列禁止:** 定数 (`const val`) やリソースファイル (`strings.xml`, `dimens.xml`) で定義する。
-   **静的解析:** **ktlint** (+ 推奨: detekt) を導入し、指摘は原則修正する。

## 4. テスト

-   **ユニットテスト:** ViewModel, UseCase, Repository のpublic関数は原則テストを作成 (JUnit, Turbine, MockK)。
-   **UIテスト (推奨):** 主要画面・機能は Compose Testing ライブラリでテストを作成。
-   **テスト容易性:** DIを活用し、モック可能に設計する。

## 5. デザイン・リソース規約 (Compose主体)

-   **ハードコーディング禁止:** 文字列 (`stringResource`)、寸法 (`dimensionResource`)、色 (`MaterialTheme.colorScheme`) はリソース経由で参照。
-   **Compose Theme:** デザインシステムは `ui/theme/` 配下で定義 (`Color.kt`, `Type.kt`, `Shape.kt`, `Theme.kt`)。UI要素の色・タイポグラフィ・形状は `MaterialTheme` 経由でアクセスする。
-   **黒ベース・シックなテーマ:**
    -   カラースキーマは `Color.kt` で定義し、`Theme.kt` の `darkColorScheme` で適用。
    -   コントラスト比、一貫性に配慮する。
    -   コンポーネントデザイン（エレベーション、角丸、アニメーション）も一貫性を保つ。
-   **リソース命名:**
    -   Drawable: `ic_[name]`, `bg_[name]`
    -   String: `feature_description_text` (例: `login_button_text`)
    -   Dimen: `spacing_medium`, `font_size_large`
    -   Color (XML用): `brand_primary`, `text_secondary` (ComposeではTheme経由)

## 6. セキュリティ

-   **機密情報:** ソースコードに記述禁止 (`local.properties` + BuildConfig または Keystore/EncryptedPrefs)。Gitにもコミットしない。
-   **ネットワーク:** HTTPS必須。OkHttp/Retrofit推奨。
-   **データ保存:** SharedPreferencesは設定等に。機密情報は `EncryptedSharedPreferences`。
-   **WebView:** 使用は慎重に。設定は最小限に。`addJavascriptInterface` は避ける。
-   **パーミッション:** 必要最小限を要求。Dangerous Permissionは実行直前に要求。

## 7. アクセシビリティ (A11y)

-   **セマンティクス (`Modifier.semantics`):** 意味情報 (`contentDescription` 等) を付与。
-   **タッチターゲットサイズ:** 最低 48dp x 48dp を確保 (`Modifier.minimumTouchTargetSize()`)。
-   **コントラスト比:** WCAG基準を満たす。
-   **フォーカス制御:** スクリーンリーダーで論理的な順序になるように。
-   **テスト:** TalkBackで確認。
