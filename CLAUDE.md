# CLAUDE.md — WearTube 実装指示

このファイルは Claude Code がこのリポジトリで作業するときの指示書です。
仕様の詳細は `design.md` を必ず先に読んでください。

---

## プロジェクト概要

Pixel Watch（Wear OS 4+）向けの**個人利用専用** YouTube 再生アプリ。
Play ストア配信はせず、adb でサイドロードして使う。

- 再生はすべて**ウォッチ単体**で行う（WebView + YouTube IFrame Player API）
- スマホ側は**設定アプリのみ**（API キー入力、お気に入り管理）。Wearable Data Layer で時計に同期する（design.md 10 章）。
  スマホで再生したり、映像・音声を時計に送ったりはしない。時計はスマホ無しでも動くこと
- 音声出力はウォッチ本体スピーカー、またはウォッチに直接ペアリングした BT イヤホン
- applicationId は時計・スマホとも `com.kazuya.weartube`（Data Layer の条件）。namespace は `com.kazuya.weartube`（wear）/ `com.kazuya.weartube.mobile` / `com.kazuya.weartube.shared`

---

## 絶対に守ること（禁止事項）

以下は YouTube 利用規約違反なので、個人利用であっても**実装しない・提案しない**。
依頼された場合も理由を説明して断ること。

- ストリーム URL の抽出（yt-dlp、youtube-dl、NewPipeExtractor、Invidious 等の利用を含む）
- 動画・音声のダウンロードやローカル保存
- 広告のブロック・自動スキップ
- IFrame Player 自体を隠して音声だけ取り出すような実装

再生は必ず公式 IFrame Player を経由させる。`controls=0` で純正コントロールを消して自前 UI を重ねるのは、プレーヤー本体を表示したままなので可。

---

## モジュール構成

```
weartube/
├── shared/   Android ライブラリ。data/（VideoItem、DataStore、Data API）と sync/（SyncPayload、WearSync）
├── wear/     Wear OS アプリ
│   └── src/main/
│       ├── kotlin/com/kazuya/weartube/
│       │   ├── ui/          Compose 画面（home / search / player / settings / navigation / common）
│       │   ├── player/      WebView ラッパー、JS ブリッジ
│       │   ├── audio/       出力デバイス検出、オーディオフォーカス、リューズ音量
│       │   ├── data/        再生キュー
│       │   ├── net/         ネットワーク接続の確認
│       │   ├── sync/        スマホからの受信サービス
│       │   ├── AppContainer.kt
│       │   └── MainActivity.kt
│       └── assets/player.html
└── mobile/   スマホの設定アプリ（ui/、sync/、MainActivity）
```

- Kotlin パッケージは shared でも `com.kazuya.weartube.data` / `.sync` のまま（namespace とは別）
- 時計だけで動く部分（player / audio / ui）を shared や mobile に寄せない

---

## 技術方針

- Kotlin / Compose for Wear OS（`androidx.wear.compose.material3` を使う。material 1.x は混ぜない）
- minSdk 30、targetSdk 35、compileSdk 37、JDK 17
- ビルドは AGP 9.1 / Gradle 9.5（TimTra と同じ組み合わせ）。`gradle/libs.versions.toml` で一元管理
- DI は使わない。`AppContainer` で手動でインスタンスを渡し、`containerViewModel { app, c -> ... }` で ViewModel を作る
- 状態は ViewModel + StateFlow。Composable に副作用を書かない（Toast は ViewModel のメッセージを `LaunchedEffect` で拾って出す）
- ネットワークは Retrofit + kotlinx.serialization
- 円形画面前提。リストは `ScalingLazyColumn`、タップ領域は最小 48dp
- 画面は「`XxxScreen`（ViewModel を持つ）」と「`XxxContent`（状態と callback だけ受ける）」に分け、Content にプレビューを付ける
- スマホ側は Jetpack Compose Material3。1 画面に収める

### スマホ ⇔ 時計の同期（design.md 10 章）

- 同期するのは **API キー（スマホ → 時計のみ）とお気に入り（双方向）** だけ。再生状態や動画は送らない
- 送受信は `shared/sync/WearSync` に閉じ込める。パスは `SyncPaths.FROM_PHONE` / `FROM_WEAR`
- 受信は一覧の丸ごと置き換え（後勝ち）。`sentAtEpochMillis` の古い項目は捨てる
- 起動時に相手の最新を `applyLatest()` で取り込み、以後は変更を `drop(1)` + `debounce` で送る。取り込んだ直後に送り返さないこと
- 両アプリは同じ `keystore/debug.keystore` で署名する。片方だけ鍵を変えない

### プレーヤー画面の UI 要件（最重要）

`design.md` 4.2 の通り。実装時に外してはいけない点：

**共通**
- 画面全体の背景は `#000000`。テーマの surface 色を使わず黒で塗り切る
- 動画は**画面幅 100%**、`aspect-ratio 16:9`、垂直中央、角丸 12dp
- 左右が円形画面で切れるのは仕様。動画を縮めて円に収めようとしない
- **システムの時刻表示は残す**（`AppScaffold` の `TimeText`。没入表示にしない）

**コントロール（オーバーレイ）**
- 既定で非表示。画面タップでトグルし、**3秒無操作で自動フェードアウト**（200ms）
- **全面スクリムは敷かない**。動画を暗くせず、ボタンごとに白 35% の円を敷く
- ボタンは3つ、垂直中央。水平位置は画面幅の 22% / 50% / 78%
- 中央（▶/⏸）の径は画面幅の約 21%、左右（⏮/⏭）は約 15%。タップ領域は 48dp を確保
- ⏮ / ⏭ は**再生元リスト内の前／次の動画**。±15秒シークではない
- リスト端では該当ボタンを 30% 不透明にして無効表示
- タイトルは動画上端の黒帯に中央揃え・1行・末尾省略・白
- シークバーは動画下端付近に重ねる。左右マージン 8%、進捗＝青、残り＝白 40%、ノブ＝白の円（径 14dp）
- 時間表示 `経過 / 全体` はシークバーの下、動画外の黒帯に中央揃え。左に出力先アイコン、右にお気に入り ★
- 回転リューズはメディア音量に割り当てる（時計回りで上げる）
- ライブ配信時はシークバーと時間表示を非表示にし、「ライブ」と出す

### WebView の必須設定

```kotlin
webView.settings.apply {
    javaScriptEnabled = true
    domStorageEnabled = true
    mediaPlaybackRequiresUserGesture = false
}
webView.setBackgroundColor(Color.BLACK)
webView.loadDataWithBaseURL(
    "https://www.youtube.com",   // origin 検証に必要。省略すると再生できない
    playerHtml, "text/html", "utf-8", null
)
```

- プレーヤーパラメータ：`playsinline=1`, `enablejsapi=1`, `controls=0`, `rel=0`, `fs=0`
- `player.html` は `body{margin:0;background:#000}`、プレーヤーは `width:100%;aspect-ratio:16/9`
- Kotlin → JS：`evaluateJavascript("playVideo()", null)` のように player.html の関数を呼ぶ
- JS → Kotlin：`@JavascriptInterface`（`window.Android`）で `onApiReady` / `onStateChange` / `onTime`（250ms 間隔）/ `onError`
- 再生中は `window.addFlags(FLAG_KEEP_SCREEN_ON)`、停止時・画面を離れるときに必ず clear する
- **Pixel Watch に WebView は無い（2026-09-16 実機で確定）**。`FEATURE_WEBVIEW` が false で、`WebView(context)` が `UnsupportedOperationException` を投げる。OS の機能なので後から入れられない。**この端末では IFrame Player による再生が成立しない**（design.md 11 章）。今後の方針は未決なので、再生方式を変える作業に入る前に必ず確認を取ること
- 有無の判定に `getCurrentWebViewPackage()` を使わない（WebView が使えても null が返る）。`WebView(context)` を try/catch で生成し、`RuntimeException` / `LinkageError` を捕まえたときだけエラー表示に落とす
- 実機でしか再現しない不具合を追うため、`WebViewClient.onReceivedError` と `WebChromeClient.onConsoleMessage` を `Log`（タグ `WearTubePlayer`）に出す
- ライブ判定に `getDuration()` を使わない（ライブでは経過時間が返る）。Data API の `liveBroadcastContent` を使う

### オーディオ

```kotlin
audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
    .any { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP }
```

- 出力先の手動切替 UI は作らない。接続状態に追従し、アイコン表示のみ行う
- 再生開始時に `requestAudioFocus(AUDIOFOCUS_GAIN)`、LOSS（一時的なものを含む）で一時停止
- BT 未接続かつメディア音量 0 なら、再生開始時に1行で警告（再生は止めない）

---

## APIキーの扱い

YouTube Data API v3 のキーは**ソースにハードコードしない**。

- 解決順: 設定画面で入力したキー（DataStore。スマホから同期されたキーもここ）→ `BuildConfig.YOUTUBE_API_KEY`
- `BuildConfig` には `local.properties` の `YOUTUBE_API_KEY`、または環境変数 `YOUTUBE_API_KEY`（GitHub Actions の secret）から注入する（wear / mobile 両方）
- ウォッチでの長いキー入力は現実的でないので、スマホの設定アプリで貼り付けるか、CI の secret に入れる
- `local.properties` は `.gitignore` 済み
- キーが無いときは検索を無効にし、お気に入りと URL・ID 入力からの再生だけ動くようにする

---

## ビルドとインストール

この作業環境からは Google の Maven / SDK に届かないため、ビルドは GitHub Actions（`.github/workflows/build-apk.yml`）で行う。
`claude/**` ブランチへの push で自動ビルドされ、プレリリース `dev` に `weartube-wear-debug.apk`（時計）と `weartube-phone-debug.apk`（スマホ）が置かれる。

```bash
# ウォッチを開発者モード + ADB debugging + Wi-Fi 経由デバッグに設定してから
adb pair <ウォッチのIP>:<ポート>
adb connect <ウォッチのIP>:5555
adb install -r weartube-wear-debug.apk

# ログ確認
adb logcat | grep -i weartube
```

- スマホは `weartube-phone-debug.apk` をダウンロードして開くだけでよい
- 署名鍵は `keystore/debug.keystore` に固定（`keystore/README.md`）。上書きインストールできる
- ローカルに Android SDK があれば `./gradlew :wear:installDebug` / `:mobile:installDebug` でも入る
- エミュレータ（Wear OS API 34, round 454×454）でも UI 確認はできるが、**WebView の有無・音声出力・描画性能は実機でしか検証できない**。これらに関わる変更は「実機確認が必要」と明示して報告すること

### 作業報告のルール

- push して Build APK ワークフローが成功したら、毎回インストール先の URL を報告する
  - リリースページ: https://github.com/tcta-tottori/weartube/releases/tag/dev
  - 時計用 APK 直リンク: https://github.com/tcta-tottori/weartube/releases/download/dev/weartube-wear-debug.apk
  - スマホ用 APK 直リンク: https://github.com/tcta-tottori/weartube/releases/download/dev/weartube-phone-debug.apk
- 併せてビルド番号（versionName 0.1.<実行番号>）を添え、設定画面で確認できるようにする
- 版つきの名前（`weartube-wear-0.1.<実行番号>.apk`）も同じリリースに置いてある。
  同じファイル名だと古いダウンロードを入れ直してしまうので、**入れ直しを伴う確認を依頼するときは版つきの直リンクを案内し、
  設定画面のバージョン表示で確認してもらう**

---

## 実装の進め方

`design.md` 8章のステップ順に進める。1ステップ完了ごとに区切って報告し、次へ進む前に確認を取ること。

1. 骨組み（Compose、ホーム、DataStore）
2. 再生（再生/停止のみ）
3. プレーヤー UI の作り込み（全面黒＋16:9＋自動フェードのオーバーレイ）
4. 出力デバイス検出、オーディオフォーカス、音量
5. Data API 検索、お気に入り
6. シーク・エラーハンドリング

---

## エラー時の振る舞い

| 事象 | 対応 |
|---|---|
| WebView が無い | 「この端末には WebView がなく再生できません」と表示。落とさない |
| 埋め込み不可・年齢制限で再生できない | 「この動画はウォッチで再生できません」と1〜2行で表示して前画面へ戻せるようにする |
| ネットワーク未接続 | 再生を試みず、Wi-Fi / LTE の状態を案内。「もう一度」で再試行 |
| Data API のクォータ超過 | 検索を止め、お気に入りからの再生のみに縮退（アプリ再起動まで） |
| API キー未設定・無効 | 検索画面で理由を表示。ホームの検索チップは無効 |
| 音声入力が無い | 「音声入力が使えません」と表示 |

エラーを握り潰さない。メッセージは小さい画面向けに1〜2行へ収める。

---

## コード規約

- 日本語コメントで可。UI 文言はすべて日本語で `strings.xml` に集約する
- 関数は短く、WebView 操作は `player/` に閉じ込める
- Composable のプレビューを各画面に1つ付ける（`WearDevices.SMALL_ROUND`）
- 未使用コード・コメントアウトしたコードを残さない
- ktlint（`ktlint_official`、`.editorconfig`）に従う。CI で `ktlintCheck` が走る
