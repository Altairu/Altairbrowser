# 🐸 Altair Browser

ローカルホストなどへの接続に特化した Android 自作ブラウザアプリです。  
アドレス＋ポート番号を入力するだけでアクセスでき、履歴機能で毎回入力する手間を省けます。

---

## 📱 機能

| 機能 | 詳細 |
|------|------|
| アドレス入力 | ホスト名/IP とポート番号を別フィールドで入力 |
| 履歴 | 最新20件を新しい順に表示。タップで即接続 |
| 履歴削除 | 個別削除（スワイプまたは×ボタン）・全件削除 |
| 全画面表示 | タブなし・ステータスバーなしの完全全画面 WebView |
| 戻るボタン | WebView 内を戻れる限り戻る → それ以上はホーム画面へ |
| HTTP 専用 | `http://` のみ対応（ローカルネットワーク専用） |
| エラー表示 | 接続失敗時にエラー画面を表示 |

---

## 🛠️ 開発環境

| ツール | バージョン |
|--------|-----------|
| 言語 | Kotlin |
| UI | Jetpack Compose |
| 最低 SDK | Android 7.0（API 24） |
| ターゲット SDK | Android 16（API 36） |
| ビルドツール | Gradle 9.x |
| JDK | Android Studio 付属 JBR（Java 17） |

---

## 🚀 ビルド方法

### 前提条件

- **Android Studio** がインストールされていること  
  → [https://developer.android.com/studio](https://developer.android.com/studio)
- **Android SDK** が `C:\Users\<ユーザー名>\AppData\Local\Android\Sdk` に存在すること

### 1. リポジトリをクローン

```bash
git clone https://github.com/106no/Altairbrowser.git
cd Altairbrowser
```

### 2. デバッグ APK をビルド

```powershell
# JDK のパスを設定（Android Studio 付属 JBR を使用）
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

# ビルド実行
.\gradlew.bat assembleDebug
```

ビルド成功後、APK は以下に生成されます：

```
app\build\outputs\apk\debug\app-debug.apk
```

### 3. リリース APK をビルド（署名なし）

```powershell
.\gradlew.bat assembleRelease
```

---

## 📲 インストール方法

### ADB 経由でインストール（USB デバッグ）

#### スマホの準備

1. **設定** → **端末情報** → **ビルド番号** を7回タップ  
   → 「開発者向けオプション」が有効になる
2. **設定** → **開発者向けオプション** → **USB デバッグ** をオン
3. PC と USB ケーブルで接続
4. スマホに「USB デバッグを許可しますか？」と表示されたら **許可** をタップ

#### PC からインストール

```powershell
# ADB のパスを設定
$env:PATH = "$env:USERPROFILE\AppData\Local\Android\Sdk\platform-tools;$env:PATH"

# 接続デバイスを確認
adb devices

# APK をインストール
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

出力に `Success` と表示されればインストール完了です。

### APK ファイルを直接転送する場合

1. `app-debug.apk` をスマホに転送（メール・Google ドライブなど）
2. スマホで APK ファイルを開く
3. **「提供元不明のアプリ」** のインストールを許可してインストール

---

## 📁 プロジェクト構成

```
Altairbrowser/
├── app/
│   └── src/main/
│       ├── AndroidManifest.xml          # パーミッション・テーマ設定
│       └── java/com/example/altairbrowser/
│           ├── MainActivity.kt          # エントリーポイント
│           ├── Navigation.kt            # 画面遷移（Home ⇔ Browser）
│           ├── NavigationKeys.kt        # ナビゲーションキー定義
│           ├── data/
│           │   └── DataRepository.kt    # 履歴管理（SharedPreferences）
│           ├── theme/
│           │   ├── Color.kt             # カラーパレット定義
│           │   ├── Theme.kt             # Material3 テーマ設定
│           │   └── Type.kt              # タイポグラフィ設定
│           └── ui/
│               ├── main/
│               │   └── MainScreen.kt    # ホーム画面（入力＋履歴）
│               └── browser/
│                   └── BrowserScreen.kt # ブラウザ画面（全画面 WebView）
├── Altair Browser.png                   # アプリアイコン元画像
└── README.md                            # このファイル
```

---

## 📝 コードの修正方法

### 接続先のデフォルト値を変更したい

[MainScreen.kt](app/src/main/java/com/example/altairbrowser/ui/main/MainScreen.kt) の以下の部分を編集：

```kotlin
var host by remember { mutableStateOf("127.0.0.1") }  // ← デフォルトホスト
var port by remember { mutableStateOf("8765") }        // ← デフォルトポート
```

### 履歴の保存件数を変更したい

[DataRepository.kt](app/src/main/java/com/example/altairbrowser/data/DataRepository.kt) の以下の部分を編集：

```kotlin
private val maxHistoryCount = 20  // ← 件数を変更
```

### HTTP 以外のプロトコルも使いたい

[AndroidManifest.xml](app/src/main/AndroidManifest.xml) で `usesCleartextTraffic="true"` が設定済みです。  
HTTPS を追加したい場合は Navigation.kt の URL 生成部分を修正：

```kotlin
// 現在: http://固定
backStack.add(Browser(url = "http://$host:$port"))

// HTTPS に変えたい場合
backStack.add(Browser(url = "https://$host:$port"))
```

---

## 🔄 アップデート手順

1. コードを修正
2. 再ビルド：`.\gradlew.bat assembleDebug`
3. 再インストール：`adb install -r app\build\outputs\apk\debug\app-debug.apk`

---

## 📄 ライセンス

Private / All rights reserved.
