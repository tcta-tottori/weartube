# WearTube 設計書

Pixel Watch（Wear OS）向け 個人利用専用 YouTube 再生アプリ

> 2026-09-15 改訂: 初版の矛盾（±15 秒と前後動画、ライブ時の非表示対象、リューズの割当）を統一し、
> Wear OS の WebView 可用性リスク、ライブ判定方法、API キーの渡し方、URL 入力再生を追記した。変更点は 9 章。
> 同日 2 版: スマホの設定アプリ（API キーとお気に入りを Data Layer で時計に同期）を追加した。10 章。

---

## 1. 目的とスコープ

| 項目 | 内容 |
|---|---|
| 目的 | Pixel Watch 単体で YouTube 動画を再生する |
| 構成 | **再生はウォッチ単体**。スマホ側は設定アプリのみ（API キー・お気に入りの入力と時計への同期。10 章） |
| 利用形態 | 個人利用のみ。Play ストア配信はしない（adb サイドロード） |
| 端末 | Pixel Watch（Wear OS 4 以降） |
| 開発環境 | Android Studio / Kotlin / Compose for Wear OS（Material3） |

### 非目標（やらないこと）

- 動画・音声ファイルのダウンロードおよび保存
- ストリーム URL の抽出（yt-dlp / NewPipeExtractor 系ライブラリの利用）
- 広告のブロック・スキップ
- スマホでの再生、スマホから時計への映像・音声の転送
- 一般公開・配布

> **規約上の前提**：再生は必ず YouTube 公式の IFrame Player を通して行う。上記の非目標は YouTube 利用規約違反にあたるため、個人利用でも実装しない。

---

## 2. 音声出力

### 2.1 出力先は2つのみ

Bluetooth オーディオの接続元は1台なので、**スマホにペアリング済みのイヤホンはウォッチの出力先として使えない**。モードA構成での出力先は次の2つに限られる。

| 出力先 | 条件 |
|---|---|
| ウォッチ本体スピーカー | 常時利用可 |
| BT イヤホン | **ウォッチに直接ペアリング**済みであること。マルチポイント対応機なら、スマホと繋いだまま切り替えて使える |

### 2.2 実装方針

- 出力デバイスは `AudioManager.getDevices(GET_DEVICES_OUTPUTS)` で検出し、`TYPE_BLUETOOTH_A2DP`（または `TYPE_BLE_HEADSET`）があればそちらを既定にする。
- 出力先の明示的な切替 UI は置かず、**接続状態に追従する**（BT 接続中は自動でイヤホン、切断でスピーカー）。`AudioDeviceCallback` で変化を追い、現在の出力先はプレーヤーのオーバーレイにアイコンで表示するのみ。
- BT 未接続かつメディア音量 0 の場合は、再生開始時に1行で警告を出す（3 秒で消える。再生は止めない）。
- 再生開始時に `AudioManager.requestAudioFocus(AUDIOFOCUS_GAIN)` を取得し、フォーカス喪失（一時的なものを含む）で一時停止する。自動再開はしない。

---

## 3. 機能要件

### MVP

| # | 機能 | 内容 |
|---|---|---|
| F-01 | お気に入り一覧 | 登録済み動画をリスト表示。タップで再生。長押しで削除 |
| F-02 | 音声検索 | ウォッチの音声入力 → YouTube Data API v3 で検索 → 結果リスト |
| F-03 | 再生 | WebView + IFrame Player API。再生/一時停止/前後の動画/シーク/音量 |
| F-04 | お気に入り登録 | 検索結果（長押し）・再生中の動画（オーバーレイの★）を保存／削除 |
| F-05 | URL・ID 入力再生 | キーボード入力した YouTube URL または動画 ID を単独再生。API キーが無くても動く補助経路 |
| F-06 | スマホ設定アプリ | API キーの入力、お気に入りの追加（YouTube アプリの共有・URL 貼り付け）と削除を行い、時計に同期する（10 章） |

### 将来拡張（MVP 後）

- 再生履歴、再生位置の記憶
- タイル／コンプリケーションからの即再生
- 回転リューズの割当切替（音量 ⇔ シーク）。MVP ではリューズは音量固定

---

## 4. 画面設計

Pixel Watch は円形（41mm: 384×384px / 45mm: 450×450px）。dp 基準でレイアウトし、円の外周で見切れる前提で組む。

### 4.1 画面遷移

```
[ホーム]
 ├─ 🎤 音声で検索（先頭の固定チップ。API キー未設定なら無効）
 ├─ お気に入り一覧（ScalingLazyColumn）
 ├─ 🔗 URL・IDで再生
 └─ ⚙ 設定（APIキー）
       ↓ タップ
[検索結果] → [プレーヤー]
```

### 4.2 プレーヤー画面（本アプリの中心）

全面黒背景に動画を画面幅いっぱいで中央配置する。コントロールは常時表示せず、オーバーレイで出し入れする。

#### 通常時（コントロール非表示）

```
┌───────────────────────┐
│           10:09               │  ← システムの時刻表示は残す
│ ╭───────────────────╮ │
│ │                           │ │
│ │      動画（16:9）           │ │  ← 幅100%、角丸12dp
│ │                           │ │     左右は円形画面で切れる
│ ╰───────────────────╯ │
│                               │
└───────────────────────┘
```

| 項目 | 値 |
|---|---|
| 背景色 | `#000000`（全面。AMOLED の消灯画素を活かす） |
| 動画幅 | 画面幅の 100%（左右マージン 0） |
| 動画高さ | 幅 × 9/16 |
| 配置 | 垂直中央 |
| 角丸 | 12dp |
| 時刻表示 | 残す（没入表示にしない） |

#### 操作時（コントロール表示）

```
┌───────────────────────┐
│           10:09               │
│    スイス・アルプスの絶景 │ 4K …  │  ← タイトル1行・中央・省略記号
│ ╭───────────────────╮ │
│ │   ⬤       ⬤⬤       ⬤    │ │  ← 半透明の円ボタン3つ
│ │  ⏮        ▶/⏸       ⏭    │ │
│ │                           │ │
│ │ ━━━━●━━━━━━━━━ │ │  ← シークバー（動画下部に重ねる）
│ ╰───────────────────╯ │
│    🔊 03:21 / 12:34 ☆        │  ← 動画外の黒帯に中央揃え
└───────────────────────┘
```

| 要素 | 仕様 |
|---|---|
| 全面スクリム | **敷かない**。動画はそのままの明るさを保つ |
| ボタン背景 | 白 35% の円。アイコンは白・不透明 |
| 中央ボタン径 | 画面幅の約 21% |
| 左右ボタン径 | 画面幅の約 15%（中央の約 0.7 倍）。タップ領域は 48dp を確保する |
| ボタン水平位置 | 画面幅の 22% / 50% / 78% |
| ボタン垂直位置 | 画面の垂直中央 |
| タイトル | 動画上端の黒帯。中央揃え・1行・末尾省略。白 |
| シークバー | 動画の下端付近に重ねる。左右マージン 8%。進捗＝青、残り＝白 40%、ノブ＝白の円（径 14dp） |
| 時間表示 | `経過 / 全体`。シークバー下、動画外の黒帯に中央揃え。左に出力先アイコン（🔊 / BT）、右にお気に入り ★ |

#### 操作

| 操作 | 割当 |
|---|---|
| 画面タップ | コントロールの表示／非表示 |
| 中央 ▶/⏸ | 再生・一時停止（終了後は先頭から再生） |
| ⏮ / ⏭ | 再生元リスト内の前／次の動画。動画終了時は自動で次へ |
| シークバー | タップ／ドラッグでシーク（ライブ配信時は非表示） |
| ★ | お気に入りの登録／解除 |
| 回転リューズ | メディア音量（時計回りで上げる） |
| 右スワイプ | 前画面へ戻る（Wear OS 標準） |

- コントロールは既定で**非表示**。画面タップでトグルし、**3秒無操作で自動フェードアウト**（200ms）。
- リストの先頭／末尾では該当する ⏮ ／ ⏭ を 30% 不透明にして無効表示にする。
- ライブ配信ではシークバーと時間表示を出さず、時間の位置に「ライブ」と表示する。

### 4.3 一覧（ホーム・検索結果）

- 1 行 = サムネイル（16:9、幅 56dp）+ タイトル 2 行 + チャンネル名。ライブは「LIVE」を赤で表示。
- タップで再生（その一覧が再生元リストになる）。長押しでお気に入りの登録／解除（Toast で 1 行通知）。
- 検索画面は開いた直後に音声入力を起動する。「もう一度」で再入力。

---

## 5. アーキテクチャ

### 5.1 モジュール構成

```
weartube/
├── shared/                 Android ライブラリ（wear と mobile で共有）
│   └── kotlin/com/kazuya/weartube/
│       ├── data/           VideoItem、DataStore（お気に入り・API キー）、YouTube Data API クライアント
│       └── sync/           Data Layer 同期（SyncPayload、WearSync）
├── wear/                   Wear OS アプリ（applicationId com.kazuya.weartube）
│   └── src/main/
│       ├── kotlin/com/kazuya/weartube/
│       │   ├── ui/         Compose 画面（home / search / player / settings / navigation / common）
│       │   ├── player/     WebView ラッパー（PlayerController）、JS ブリッジ（PlayerBridge）
│       │   ├── audio/      出力デバイス検出、オーディオフォーカス、リューズ音量
│       │   ├── data/       再生キュー
│       │   ├── net/        ネットワーク接続の確認
│       │   ├── sync/       スマホからの受信サービス
│       │   ├── AppContainer.kt   手動 DI
│       │   └── MainActivity.kt
│       └── assets/player.html
└── mobile/                 スマホの設定アプリ（同じ applicationId・同じ署名）
    └── kotlin/com/kazuya/weartube/mobile/
        ├── ui/             設定画面（API キー、同期状態、お気に入り）
        ├── sync/           時計からの受信サービス
        └── MainActivity.kt 共有インテント（ACTION_SEND）の受け口
```

### 5.2 技術スタック

| レイヤ | 採用 |
|---|---|
| 言語 | Kotlin 2.4 |
| UI | Compose for Wear OS Material3（`androidx.wear.compose:compose-material3`）+ `compose-navigation` |
| 再生 | WebView + YouTube IFrame Player API |
| 検索 | YouTube Data API v3（Retrofit + kotlinx.serialization） |
| サムネイル | Coil 3 |
| 永続化 | DataStore（Preferences） |
| スマホ ⇔ 時計 | Wearable Data Layer API（play-services-wearable）。設定とお気に入りのみ |
| スマホ UI | Jetpack Compose Material3（1 画面） |
| DI | 手動 DI（`AppContainer`。規模的に Hilt 不要） |
| ビルド | AGP 9.1 / Gradle 9.5 / JDK 17。TimTra と同じ構成で GitHub Actions がビルド |
| minSdk / targetSdk / compileSdk | 30 / 35 / 37 |

### 5.3 再生方式

`assets/player.html` に IFrame Player を置き、`loadDataWithBaseURL("https://www.youtube.com", ...)` で読み込む（origin 検証を通すため必須）。

WebView 必須設定：

```kotlin
settings.javaScriptEnabled = true
settings.domStorageEnabled = true
settings.mediaPlaybackRequiresUserGesture = false   // 自動再生に必要
```

プレーヤーパラメータ：`playsinline=1`, `enablejsapi=1`, `controls=0`, `rel=0`, `fs=0`

- `controls=0` にして、操作は 4.2 の自前オーバーレイに寄せる（純正コントロールはウォッチ画面では小さすぎる）。
- Kotlin → JS は `evaluateJavascript("playVideo()")` 等、player.html 側に置いた関数を呼ぶ（`loadVideo` / `playVideo` / `pauseVideo` / `seekTo` / `stopVideo`）。
- JS → Kotlin は `@JavascriptInterface`（`window.Android`）で `onApiReady` / `onStateChange(state, title, duration)` / `onTime`（250ms 間隔）/ `onError(code)` を受け取る。
- HTML 側は `body{margin:0;background:#000}` とし、プレーヤーを `width:100%; aspect-ratio:16/9` で配置。ネイティブ側の黒背景と継ぎ目なく見えるようにする。
- WebView は Application Context で生成して ViewModel が持ち、画面を離れたら `destroy()` する。WebView へのタッチは通さず（`setOnTouchListener` で消費）、Compose 側の透明レイヤーでタップを受ける。
- **WebView の有無は生成の可否で判定する**。`getCurrentWebViewPackage()` は Wear OS では WebView が使えても null を返すため使わない。`WebView(context)` の生成で `RuntimeException` / `LinkageError` が出たときだけ「この端末には WebView がなく再生できません」と表示する（7 章）。
- JS 側の状況は `onReceivedError` と `onConsoleMessage` を Logcat（タグ `WearTubePlayer`）に出して追う。
- ライブ判定は IFrame API の `getDuration()` では行わない（ライブ時は経過時間を返す）。Data API の `liveBroadcastContent == "live"` を `VideoItem.isLive` に持たせて使う。
- 検索は `type=video&videoEmbeddable=true&videoSyndicated=true` で、IFrame で再生できない動画を最初から除く。

---

## 6. データ設計（DataStore）

| キー | 型 | 用途 |
|---|---|---|
| `favorites` | JSON 文字列 | `[{videoId, title, channelTitle, thumbnailUrl, isLive, addedAt}]`（新しい順） |
| `youtube_api_key` | String | 設定画面から入れたキー。空なら BuildConfig のキーを使う |
| `last_position` | JSON 文字列 | `{videoId: seconds}`（将来拡張用。未実装） |
| `last_sync_at` | Long | スマホ ⇔ 時計で最後に同期した時刻（設定画面の表示用） |

同じキー構成をスマホ側の DataStore にも持ち、10 章の同期で内容を揃える。

APIキーはソースにハードコードせず、次の順で解決する。

1. 設定画面で入力したキー（DataStore）。**スマホの設定アプリで入れたキーはここに同期される**
2. `BuildConfig.YOUTUBE_API_KEY` — `local.properties` の `YOUTUBE_API_KEY`、または CI の環境変数 `YOUTUBE_API_KEY`（GitHub Actions の secret）から注入

ウォッチで 39 文字のキーを打つのは現実的でないため、**スマホの設定アプリで貼り付ける**か、CI の secret に入れる。`local.properties` は `.gitignore` 済み。

---

## 7. 非機能要件・既知の制約

| 項目 | 内容・対策 |
|---|---|
| **WebView が使えない（2026-09-16 実機で確定）** | Pixel Watch は `PackageManager.FEATURE_WEBVIEW` を持たず、`WebView(context)` が `UnsupportedOperationException` を投げる（`feature=false` / `app=UnsupportedOperationException: null`）。WebView は OS の機能なので後から入れることはできない。**IFrame Player を使う本設計では動画を再生できない**（11 章） |
| 電池 | WebView + 画面点灯で消費が大きい。長時間視聴には向かない前提。設定画面に注意書きを置く |
| 画面消灯 | 画面が消えると再生が止まる。再生中は `FLAG_KEEP_SCREEN_ON` を立て、アンビエントモードには入らない。バックグラウンド音声のみ再生は成立しないと割り切る |
| 描画性能 | ウォッチの WebView は重く、コマ落ちする前提。画質は自動に任せ `vq` 指定はしない |
| 通信 | LTE モデルまたはウォッチの Wi-Fi 接続が必要。接続確認は `NET_CAPABILITY_INTERNET` + `VALIDATED` で行う（スマホ経由の BT プロキシも通るが、動画には帯域不足） |
| APIクォータ | Data API v3 の検索は 1回 100 units（無料枠 10,000/日）。検索中は再検索を受け付けず、`quotaExceeded` を受けたらアプリ再起動まで検索を止める |
| ライブ配信 | 再生可だがシーク不可。シークバーと時間表示を非表示にする |
| 年齢制限・埋め込み不可動画 | IFrame では再生できない（onError 101/150 等）。「この動画はウォッチで再生できません」と表示し、戻れるようにする |
| 音声入力 | `RecognizerIntent.ACTION_RECOGNIZE_SPEECH` を使う。応答するアプリが無い端末では「音声入力が使えません」と表示 |

---

## 8. 開発ステップ

1. **Step 1**：アプリの骨組み（Compose、ホーム画面、DataStore）
2. **Step 2**：再生（WebView + IFrame、再生/停止のみ）
3. **Step 3**：プレーヤー UI の作り込み（全面黒＋16:9＋自動フェードのオーバーレイ、シークバーの表示）
4. **Step 4**：出力デバイス検出、オーディオフォーカス、音量操作
5. **Step 5**：YouTube Data API 検索、お気に入り登録
6. **Step 6**：シーク動作・エラーハンドリング・実機での電池確認

各 Step の終わりに実機（adb over Wi-Fi）へインストールして動作確認する。
この環境では Android SDK を取得できないため、ビルドは GitHub Actions（`.github/workflows/build-apk.yml`）で行い、
プレリリース `dev` の APK を時計に入れて確認する。

---

## 9. 初版からの変更点

| 箇所 | 初版 | 改訂 | 理由 |
|---|---|---|---|
| F-03 | 「±15秒」 | 「前後の動画／シーク」 | 4.2 では ⏮/⏭ を前後の動画と定義しており矛盾していた |
| 7 章 ライブ配信 | 「±15秒ボタンとシークバーを非表示」 | 「シークバーと時間表示を非表示」 | ±15秒ボタンは存在しない |
| 3 章 将来拡張 | 「回転リューズでのシーク」 | 「リューズ割当の切替」 | 4.2 でリューズは音量に確定している |
| 5.3 | — | WebView 有無の確認、ライブ判定、`videoEmbeddable` | Wear OS の WebView 非搭載リスク、IFrame API の仕様 |
| 6 章 | — | キー解決順と CI secret | ウォッチでのキー入力が現実的でない |
| 3 章 F-05 | — | URL・ID 入力再生 | API キー無しでも再生経路を確保する |
| 4.2 | — | ★ と出力先アイコンの位置、終了時の自動次送り | F-04「再生中の動画を保存」の置き場が未定だった |
| 1 章・10 章（2 版） | 「スマホ連携はしない」 | 設定アプリ + Data Layer 同期 | 時計での API キー入力・検索クォータの不便を解消する。再生は引き続き時計単体 |

---

## 10. スマホ設定アプリと同期

### 10.1 役割

| 端末 | 役割 |
|---|---|
| スマホ（`mobile`） | API キーの貼り付け、お気に入りの追加（YouTube アプリの共有 / URL 貼り付け）と削除、同期状態の表示 |
| 時計（`wear`） | 再生。設定と URL 入力は残す（スマホが無くても動く） |

スマホでは再生しない。映像・音声を時計へ送ることもしない。

### 10.2 同期の仕組み（Wearable Data Layer）

- 両アプリは **同じ applicationId（`com.kazuya.weartube`）と同じ署名鍵** でビルドする（Data Layer の通信条件）。
- パスは方向ごとに分ける。`/weartube/from_phone`（スマホ → 時計）と `/weartube/from_wear`（時計 → スマホ）。
- 内容は `SyncPayload` の JSON 1 本。`apiKey`（スマホ → 時計のみ。null は変更なし、空文字は削除）、`favorites`（一覧を丸ごと）、`sentAtEpochMillis`。
- 受信側は **一覧を置き換える（後勝ち）**。`sentAtEpochMillis` が前回適用分より古い項目は捨てる。
- DataStore は内容が同じなら書き込まず Flow も流れないので、受け取った内容をそのまま送り返してもループしない。
- 送信タイミング: 起動時に相手の最新項目を取り込んだあと、自端末の変更を 500ms のデバウンスで送る。スマホには「今すぐ同期」ボタンも置く。
- 受信は `WearableListenerService`（`DATA_CHANGED`、`pathPrefix="/weartube/"`）。アプリが起動していなくても届く。
- 時計が Bluetooth で未接続のときは Play 開発者サービスが保留し、接続後に届く。

### 10.3 スマホ画面

1 画面のみ。上から「API キー」「時計との同期（接続状態・最終同期・今すぐ同期）」「お気に入り（URL 入力 + 一覧・削除）」。
YouTube アプリの共有シートからこのアプリを選ぶと URL が `ACTION_SEND` で届き、動画 ID を取り出して Data API（videos.list、1 unit）でタイトル・サムネイルを補って追加する。キーが無ければ ID だけで追加し、タイトルは時計の再生時に補われる。

### 10.4 アイコン

赤地に白の再生マーク（添付画像）を時計・スマホ共通で使う。adaptive icon はマスクで中央 72dp しか見えず ▶ が拡大されるため、前景は赤で塗った 108dp のキャンバスに画像を 72dp（2/3）で中央に置く。背景は画像の赤（`#DB1617`）。

---

## 11. WebView が無いという結論（2026-09-16）

### 確認したこと

Pixel Watch（Wear OS）実機で、プレーヤー画面が次を表示した。

```
0.1.11 feature=false app=UnsupportedOperationException: null / themed=UnsupportedOperationException: null
```

- `feature=false` … `PackageManager.hasSystemFeature(FEATURE_WEBVIEW)` が false
- `UnsupportedOperationException` … `WebViewFactory` がこの機能の無い端末で投げる例外

Application Context でもテーマ付き Context でも同じ結果で、**この端末に WebView は存在しない**。
WebView は OS の一部なので、アプリを入れて補うことはできない。

### 影響

design.md 5.3 の再生方式（WebView + 公式 IFrame Player）は、この端末では成立しない。
1 章の目的「Pixel Watch 単体で YouTube 動画を再生する」は、**現状の方式では達成できない**。

2 章の非目標（ストリーム URL の抽出、ダウンロード）は YouTube 利用規約違反なので、
回避策としても採らない。つまり「時計単体で動画を再生する」正規の手段が無い。

### 動いている部分

再生以外は実機で動作を確認済み。

- ホーム / 検索 / 設定の各画面、お気に入りの保存
- スマホ設定アプリでの API キー入力と、Data Layer 経由の時計への同期（「スマホと同期: 9/16 22:44」を確認）

### 残る選択肢

| # | 方針 | 内容 | 評価 |
|---|---|---|---|
| A | 時計は操作だけ、再生はスマホ | 時計から選んだ動画をスマホの YouTube アプリで開く。音は Wear OS 標準のメディア操作で制御できる | 確実に動く。ただし「時計単体」ではなくなる |
| B | 音声だけ YouTube Music | 公式の YouTube Music（Wear OS 版）を使う。開発は不要 | 動画は見られない。Premium が要る |
| C | GeckoView を同梱 | WebView の代わりに Mozilla の描画エンジンを積み、その中で公式 IFrame Player を動かす | 目的は保てるが、APK が 100MB を超え、時計の性能では実用になりにくい。成功する見込みは低い |
| D | 再生を諦める | お気に入り・検索の管理アプリとして残す | 目的を満たさない |

---

## 12. 再生方式の検証（GeckoView PoC、2026-09-16〜）

11 章の通り WebView が使えないため、**本番採用を前提にせず**、実機で Go / No-Go を判断するための検証を行う。
本番の再生画面（4.2、`ui/player`）はこの検証とは独立させ、壊さない。

### 12.1 やらないこと（再確認）

ストリーム URL の抽出、`googlevideo` の URL 取得、動画・音声のダウンロード、Media3 / ExoPlayer に
YouTube の生ストリームを流す実装は行わない。広告の非表示・回避、公式 player の UI を覆う・切り取る・
画面外へ追い出す実装も行わない。検証中は **YouTube 公式 player をそのまま表示する**。

### 12.2 構成

### 12.2 本体とは別アプリにする（2026-09-16 改）

GeckoView を本体に入れると APK が 212MB になり、実機に入らなかった。
そこで**検証を別アプリに分け、本体は元の大きさ（約 43MB）に戻す**。

| アプリ | applicationId | 中身 |
|---|---|---|
| 本体 | `com.kazuya.weartube` | 既存の機能。端末判定とブラウザ経路だけ持つ |
| 検証 | `com.kazuya.weartube.poc` | GeckoView と検証画面のみ。ABI ごとに APK を分ける |

`.so` は ABI ごとに 100MB 近いので、`splits.abi` で `armeabi-v7a` / `arm64-v8a` を別々の APK にする。
時計の ABI は `adb shell getprop ro.product.cpu.abilist` で分かる。
`useLegacyPackaging` は既定（false）のまま。インストール時に展開されず、必要な容量が減る。

```
playback/
├── PlaybackCapabilities.kt   端末の判定（WebView / ブラウザ / Custom Tabs / ABI / 画面）
├── PlaybackRoute.kt          経路の型（WatchBrowser / InAppGecko / Phone）と GeckoLoadMode
├── PlaybackRouter.kt         使える経路の一覧と、ブラウザへの受け渡し
ui/poc/PocScreen.kt           判定結果の表示と、ブラウザ経路の起動

geckopoc/（別アプリ）
├── PocLauncherActivity.kt    読み込み方式を選ぶだけの入口
├── GeckoPlayerActivity.kt    GeckoView で公式 player を出す
├── GeckoRuntimeHolder.kt     GeckoRuntime をプロセスに 1 つだけ持つ
├── WrapperServer.kt          ラッパーページを返す端末内 HTTP サーバー
└── assets/iframe_wrapper.html  IFrame Player API を使うラッパーページ
```

UI から GeckoView を直接呼ばず、`PlaybackRouter` を通す。将来
`Watch Browser / Custom Tabs → In-App GeckoView → Phone` を切り替えられるようにするための土台。

### 12.3 読み込みの 2 方式を比べる

`error 153`（埋め込み元の identity 不正）が出たとき、GeckoView 自体の問題なのか referer / origin の
問題なのかを切り分けるため、次の 2 つを別々に試せるようにする。

| モード | 読み込むもの | origin | referer |
|---|---|---|---|
| `DIRECT_EMBED` | `https://www.youtube.com/embed/<id>` | youtube.com | `GeckoSession.Loader.referrer()` で `https://www.youtube.com/` を付ける |
| `LOCAL_WRAPPER` | 端末内 HTTP サーバーの `http://127.0.0.1:<port>/player` | `http://127.0.0.1:<port>` | なし（IFrame API に `origin` を渡す） |

`file://` では origin を検証できないため使わない。

### 12.4 実機で取る値

ラッパーページの `console.log` は `GeckoRuntimeSettings.consoleOutput(true)` により Logcat に出る。
先頭に `WT_PoC` を付けてある。ネイティブ側は `Log`（タグ `WearTubePoC`）。

- `FEATURE_WEBVIEW` / `WATCH_BROWSER_HANDLER` / `CUSTOM_TABS_PROVIDER` / `HANDLER_PACKAGES`
- `ABIS` / `API_LEVEL` / `SCREEN_PX` / `DENSITY` / `SCREEN_MIN_CSS_PX`
- `GECKO_RUNTIME` / `GECKO_PAGE_START` / `GECKO_PAGE_STOP` / `GECKO_CONTENT`（crash / killed）
- `WT_PoC ORIGIN` / `UA` / `VIEWPORT`（`innerWidth`×`innerHeight`、`devicePixelRatio`）
- `WT_PoC RECT_*`（player の `getBoundingClientRect`）と `MEETS_200x200`
- `WT_PoC EVENT=onReady / onStateChange / onError / onAutoplayBlocked / onApiChange`

YouTube の player は **CSS px で 200×200 以上**が要る。丸型画面なので物理解像度ではなく
`RECT_*` の値で判断する。

### 12.5 Go / No-Go

GO は次をすべて満たすこと。GeckoView が安定起動し、IFrame Player が読み込め、通常動画と音声が再生でき、
`error 153` が出ないか正規の方法で解決でき、200×200 を満たし、YouTube 標準のコントロールが操作でき、
広告表示で破綻せず、画面の再生成・終了・再起動でクラッシュせず、セッションがリークせず、Wi-Fi で安定すること。

NO-GO なら**規約違反の回避策は実装せず止める**。10 章までの機能（お気に入り・検索・スマホ同期）はそのまま残す。
